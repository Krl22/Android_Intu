-- Ratings visibility and cancellation policy, both controlled from the admin panel.
-- Everything starts off: installed apps keep today's behavior until an admin enables each part.
-- Reasons and outcomes are recorded from now on; strikes only count while penalties are enabled.

create table private.trip_policy_settings (
  singleton boolean primary key default true check (singleton),
  show_driver_rating boolean not null default false,
  show_rider_rating boolean not null default false,
  cancellation_penalties_enabled boolean not null default false,
  free_cancel_seconds integer not null default 120 check (free_cancel_seconds between 0 and 900),
  strike_limit integer not null default 3 check (strike_limit between 1 and 20),
  strike_window_days integer not null default 7 check (strike_window_days between 1 and 60),
  excused_limit integer not null default 2 check (excused_limit between 0 and 20),
  first_block_minutes integer not null default 30 check (first_block_minutes between 5 and 10080),
  repeat_block_minutes integer not null default 1440 check (repeat_block_minutes between 5 and 43200),
  no_show_wait_seconds integer not null default 300 check (no_show_wait_seconds between 60 and 1800),
  no_show_radius_m integer not null default 150 check (no_show_radius_m between 30 and 1000),
  max_requests_per_10_min integer not null default 6 check (max_requests_per_10_min between 1 and 60),
  updated_at timestamptz not null default now(),
  updated_by text
);
alter table private.trip_policy_settings enable row level security;
revoke all on private.trip_policy_settings from public, anon, authenticated;
insert into private.trip_policy_settings(singleton) values (true);

-- Copied at request/accept time like the other participant fields; null while hidden.
alter table public.rides
  add column rider_rating numeric(3,2),
  add column rider_rating_count integer,
  add column driver_rating numeric(3,2),
  add column driver_rating_count integer,
  add column accept_pickup_distance_m integer;

create table private.ride_cancellations (
  id bigint generated always as identity primary key,
  ride_id uuid not null references public.rides(id) on delete cascade,
  user_id text not null,
  role text not null check (role in ('rider', 'driver')),
  ride_status public.ride_status not null,
  reason_code text not null,
  note text,
  -- free: never counts · excused: accepted excuse within the quota · counted: a strike
  -- no_show: verified driver no-show release (the rider receives the counted row)
  outcome text not null check (outcome in ('free', 'excused', 'counted', 'no_show')),
  penalized boolean not null default false,
  reported_user_id text,
  created_at timestamptz not null default now()
);
create index ride_cancellations_user_idx on private.ride_cancellations (user_id, role, created_at desc);
create index ride_cancellations_ride_idx on private.ride_cancellations (ride_id);
create index ride_cancellations_reported_idx on private.ride_cancellations (reported_user_id, created_at desc)
  where reported_user_id is not null;
alter table private.ride_cancellations enable row level security;
revoke all on private.ride_cancellations from public, anon, authenticated;

create table private.cancellation_blocks (
  id bigint generated always as identity primary key,
  user_id text not null,
  role text not null check (role in ('rider', 'driver')),
  blocked_until timestamptz not null,
  created_at timestamptz not null default now(),
  lifted_at timestamptz,
  lifted_by text
);
create index cancellation_blocks_user_idx on private.cancellation_blocks (user_id, role, created_at desc);
alter table private.cancellation_blocks enable row level security;
revoke all on private.cancellation_blocks from public, anon, authenticated;

-- ---------------------------------------------------------------------
-- Policy helpers
-- ---------------------------------------------------------------------

-- Unknown or legacy reasons ('cancelled_by_rider', 'cancelled_by_driver', null) become 'other'.
create function private.normalize_cancel_reason(p_role text, p_reason text)
returns text language sql immutable set search_path = '' as $$
  select case
    when p_role = 'rider' and p_reason in ('search_too_long', 'changed_mind', 'wrong_address', 'other_transport',
      'driver_late', 'driver_not_moving', 'driver_asked', 'safety') then p_reason
    when p_role = 'driver' and p_reason in ('rider_no_show', 'rider_asked', 'vehicle_problem', 'unsafe_pickup',
      'accepted_by_mistake') then p_reason
    else 'other'
  end
$$;

create function private.cancellation_status(p_user_id text, p_role text)
returns jsonb language sql stable security definer set search_path = '' as $$
  with policy as (select * from private.trip_policy_settings where singleton),
  since as (
    select greatest(now() - make_interval(days => p.strike_window_days),
      coalesce((select max(b.created_at) from private.cancellation_blocks b
                where b.user_id = p_user_id and b.role = p_role), '-infinity'::timestamptz)) as at
    from policy p
  )
  select jsonb_build_object(
    'penalties_enabled', p.cancellation_penalties_enabled,
    'strikes', (select count(*) from private.ride_cancellations c, since s
                where c.user_id = p_user_id and c.role = p_role and c.penalized and c.created_at > s.at),
    'limit', p.strike_limit,
    'window_days', p.strike_window_days,
    'excused_left', greatest(0, p.excused_limit - (select count(*) from private.ride_cancellations c
                where c.user_id = p_user_id and c.role = p_role and c.outcome = 'excused'
                  and c.created_at > now() - make_interval(days => p.strike_window_days))),
    'blocked_until', (select max(b.blocked_until) from private.cancellation_blocks b
                where b.user_id = p_user_id and b.role = p_role and b.lifted_at is null and b.blocked_until > now()))
  from policy p
$$;
revoke all on function private.cancellation_status(text, text) from public, anon, authenticated;

-- The UTC instant goes in DETAIL so the app can show the local day and time.
create function private.assert_not_blocked(p_user_id text, p_role text)
returns void language plpgsql stable security definer set search_path = '' as $$
declare v_until timestamptz;
begin
  if not (select cancellation_penalties_enabled from private.trip_policy_settings where singleton) then return; end if;
  select max(blocked_until) into v_until from private.cancellation_blocks
   where user_id = p_user_id and role = p_role and lifted_at is null and blocked_until > now();
  if v_until is not null then
    raise exception 'cancellation_block' using errcode = 'P0001',
      detail = to_char(v_until at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"');
  end if;
end $$;
revoke all on function private.assert_not_blocked(text, text) from public, anon, authenticated;

-- Decides whether a cancellation counts. Rider claims about the driver are checked against the
-- driver's live position; a driver's no-show needs the wait and the distance verified here.
create function private.cancellation_outcome(p_ride public.rides, p_user_id text, p_role text, p_reason text)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  v_policy private.trip_policy_settings;
  v_loc public.driver_locations;
  v_dist double precision;
  v_outcome text;
  v_reported text;
  v_wait integer := 0;
  v_excused integer;
begin
  select * into v_policy from private.trip_policy_settings where singleton;
  if p_ride.driver_id is not null then
    select * into v_loc from public.driver_locations where driver_id = p_ride.driver_id;
    v_dist := extensions.st_distance(v_loc.location, p_ride.origin);
  end if;

  if p_role = 'rider' then
    if p_reason in ('driver_asked', 'safety') then v_reported := p_ride.driver_id; end if;
    if p_ride.status = 'searching' then
      v_outcome := 'free';
    elsif p_ride.accepted_at is not null
          and now() - p_ride.accepted_at <= make_interval(secs => v_policy.free_cancel_seconds) then
      v_outcome := 'free';
    elsif p_ride.status = 'accepted' and p_ride.driver_on_other_trip then
      v_outcome := 'free';
    elsif p_reason in ('driver_late', 'driver_not_moving') and p_ride.status = 'accepted' and (
          -- Late: 5 minutes plus the pickup distance at about 11 km/h
          now() > p_ride.accepted_at + make_interval(secs => 300 + coalesce(p_ride.accept_pickup_distance_m, 2000) / 3.0)
          -- Not coming: after 3 minutes no fresh position, or no closer than at acceptance
          or (now() - p_ride.accepted_at >= interval '3 minutes' and (
                v_loc.updated_at is null or v_loc.updated_at < now() - interval '2 minutes'
                or v_dist is null or v_dist > coalesce(p_ride.accept_pickup_distance_m, 0) - 100))) then
      v_outcome := 'free';
    end if;
  else
    if p_reason in ('rider_asked', 'unsafe_pickup') then v_reported := p_ride.rider_id; end if;
    if p_reason = 'rider_no_show' then
      if p_ride.status <> 'arrived' or p_ride.arrived_at is null then
        v_outcome := 'no_show_unavailable';
      else
        v_wait := greatest(0, v_policy.no_show_wait_seconds - floor(extract(epoch from now() - p_ride.arrived_at))::integer);
        if v_wait > 0 then
          v_outcome := 'no_show_wait';
        elsif v_loc.updated_at is null or v_loc.updated_at < now() - interval '2 minutes'
              or v_dist is null or v_dist > v_policy.no_show_radius_m then
          v_outcome := 'no_show_far';
        else
          v_outcome := 'no_show';
        end if;
      end if;
    end if;
  end if;

  if v_outcome is null then
    if (p_role = 'rider' and p_reason in ('driver_asked', 'safety'))
       or (p_role = 'driver' and p_reason in ('rider_asked', 'vehicle_problem', 'unsafe_pickup', 'accepted_by_mistake')) then
      select count(*) into v_excused from private.ride_cancellations
       where user_id = p_user_id and role = p_role and outcome = 'excused'
         and created_at > now() - make_interval(days => v_policy.strike_window_days);
      v_outcome := case when v_excused < v_policy.excused_limit then 'excused' else 'counted' end;
    else
      v_outcome := 'counted';
    end if;
  end if;
  return jsonb_build_object('outcome', v_outcome, 'reported_user_id', v_reported, 'wait_seconds', v_wait);
end $$;
revoke all on function private.cancellation_outcome(public.rides, text, text, text) from public, anon, authenticated;

create function private.record_cancellation(p_ride public.rides, p_user_id text, p_role text, p_reason text,
  p_note text, p_outcome text, p_reported text)
returns void language plpgsql security definer set search_path = '' as $$
declare
  v_policy private.trip_policy_settings;
  v_penalized boolean;
  v_minutes integer;
begin
  select * into v_policy from private.trip_policy_settings where singleton;
  v_penalized := v_policy.cancellation_penalties_enabled and p_outcome = 'counted';
  insert into private.ride_cancellations(ride_id, user_id, role, ride_status, reason_code, note, outcome, penalized, reported_user_id)
  values (p_ride.id, p_user_id, p_role, p_ride.status, p_reason, nullif(left(trim(p_note), 300), ''),
          p_outcome, v_penalized, p_reported);
  if v_penalized and (private.cancellation_status(p_user_id, p_role)->>'strikes')::integer >= v_policy.strike_limit then
    -- A block resets the strike count; a second block within 30 days lasts longer
    v_minutes := case when exists (select 1 from private.cancellation_blocks
                                   where user_id = p_user_id and role = p_role and created_at > now() - interval '30 days')
                      then v_policy.repeat_block_minutes else v_policy.first_block_minutes end;
    insert into private.cancellation_blocks(user_id, role, blocked_until)
    values (p_user_id, p_role, now() + make_interval(mins => v_minutes));
  end if;
end $$;
revoke all on function private.record_cancellation(public.rides, text, text, text, text, text, text) from public, anon, authenticated;

-- ---------------------------------------------------------------------
-- Ride lifecycle
-- ---------------------------------------------------------------------

create or replace function private.rides_before_insert()
returns trigger language plpgsql security definer set search_path = '' as $$
declare v_policy private.trip_policy_settings;
begin
  if private.requesting_uid() is not null then
    new.rider_id := private.requesting_uid();
    new.status := 'searching';
    new.requested_at := now();
  end if;
  select * into v_policy from private.trip_policy_settings where singleton;
  if v_policy.cancellation_penalties_enabled then
    perform private.assert_not_blocked(new.rider_id, 'rider');
    if (select count(*) from public.rides
        where rider_id = new.rider_id and requested_at > now() - interval '10 minutes') >= v_policy.max_requests_per_10_min then
      raise exception 'too_many_requests' using errcode = 'P0001';
    end if;
  end if;
  select vt.service_kind into new.service_kind from public.vehicle_types vt where vt.code = new.vehicle_type;
  new.estimated_fare := private.compute_fare(new.vehicle_type, new.distance_meters, new.duration_seconds);
  if new.estimated_fare is null then
    raise exception 'vehicle_type_not_available' using errcode = '22023';
  end if;
  if new.preferred_vehicle_brand = 'honda' then
    new.estimated_fare := round(new.estimated_fare * 1.12, 1);
  end if;
  select nullif(trim(p.first_name || ' ' || p.last_name), ''), p.photo_url
    into new.rider_name, new.rider_photo_url from public.profiles p where p.id = new.rider_id;
  new.rider_rating := null;
  new.rider_rating_count := null;
  if v_policy.show_rider_rating then
    select round(avg(r.rating_for_rider), 2), count(r.rating_for_rider)
      into new.rider_rating, new.rider_rating_count
      from public.rides r where r.rider_id = new.rider_id and r.rating_for_rider is not null;
  end if;
  return new;
end
$$;

create or replace function public.accept_ride(p_ride_id uuid)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_vehicle public.vehicles;
  v_driver public.profiles;
  v_ride public.rides;
  v_show_rating boolean;
  v_rating numeric;
  v_rating_count integer;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  select v.* into v_vehicle
  from public.drivers d
  join public.vehicles v on v.driver_id = d.id and v.is_active
  join public.vehicle_types vt on vt.code = v.vehicle_type and vt.is_active
  where d.id = v_uid and d.status = 'approved'
  for update of d;
  if not found then
    raise exception 'driver_not_approved' using errcode = '42501';
  end if;

  perform private.assert_not_blocked(v_uid, 'driver');

  -- Ocupado solo si ya va a recoger a alguien; con un pasajero a bordo sí puede aceptar el siguiente
  if exists (select 1 from public.rides
             where driver_id = v_uid and status in ('accepted', 'arrived')) then
    raise exception 'driver_busy' using errcode = 'P0001';
  end if;

  select * into v_driver from public.profiles where id = v_uid;
  select show_driver_rating into v_show_rating from private.trip_policy_settings where singleton;
  select d.rating, d.rating_count into v_rating, v_rating_count from public.drivers d where d.id = v_uid;

  update public.rides r
     set status = 'accepted',
         driver_id = v_uid,
         vehicle_id = v_vehicle.id,
         accepted_at = now(),
         driver_name = nullif(trim(v_driver.first_name || ' ' || v_driver.last_name), ''),
         driver_phone = v_driver.phone,
         driver_photo_url = v_driver.photo_url,
         vehicle_plate = v_vehicle.plate,
         vehicle_description = concat_ws(' ', v_vehicle.brand, v_vehicle.model, v_vehicle.color),
         rider_phone = (select p.phone from public.profiles p where p.id = r.rider_id),
         driver_on_other_trip = exists (select 1 from public.rides o
                                        where o.driver_id = v_uid and o.status = 'in_progress'),
         driver_rating = case when v_show_rating then v_rating end,
         driver_rating_count = case when v_show_rating then v_rating_count end,
         accept_pickup_distance_m = (select round(extensions.st_distance(dl.location, r.origin))::integer
                                     from public.driver_locations dl where dl.driver_id = v_uid)
   where r.id = p_ride_id
     and r.status = 'searching'
     and r.vehicle_type = v_vehicle.vehicle_type
     and (r.preferred_vehicle_brand is null or r.preferred_vehicle_brand = lower(trim(v_vehicle.brand)))
     and r.rider_id <> v_uid
  returning * into v_ride;

  if not found then
    raise exception 'ride_not_available' using errcode = 'P0002';
  end if;

  update public.driver_locations set is_available = false where driver_id = v_uid;
  return v_ride;
end
$$;

-- Rider: the ride ends. Driver: the request goes back to searching, except a verified no-show,
-- which ends the ride and gives the strike to the rider.
create function public.cancel_ride_with_reason(p_ride_id uuid, p_reason text, p_note text default null)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
  v_driver text;
  v_role text;
  v_reason text;
  v_eval jsonb;
  v_outcome text;
begin
  select * into v_ride from public.rides where id = p_ride_id for update;

  if not found or v_uid is null or (v_ride.rider_id <> v_uid and v_ride.driver_id is distinct from v_uid) then
    raise exception 'ride_not_found' using errcode = 'P0002';
  end if;
  if v_ride.status not in ('searching', 'accepted', 'arrived') then
    raise exception 'invalid_transition' using errcode = 'P0001';
  end if;
  if v_ride.service_kind = 'delivery'
     and exists (select 1 from public.delivery_details where ride_id = p_ride_id and payment_collected_at is not null) then
    raise exception 'delivery_in_custody' using errcode = 'P0001';
  end if;

  v_role := case when v_ride.rider_id = v_uid then 'rider' else 'driver' end;
  v_reason := private.normalize_cancel_reason(v_role, p_reason);
  v_eval := private.cancellation_outcome(v_ride, v_uid, v_role, v_reason);
  v_outcome := v_eval->>'outcome';
  if v_outcome in ('no_show_unavailable', 'no_show_wait', 'no_show_far') then
    raise exception 'no_show_not_allowed' using errcode = 'P0001',
      detail = v_outcome || ':' || (v_eval->>'wait_seconds');
  end if;
  v_driver := v_ride.driver_id;
  perform private.record_cancellation(v_ride, v_uid, v_role, v_reason, p_note, v_outcome, v_eval->>'reported_user_id');

  if v_role = 'rider' then
    update public.rides
       set status = 'cancelled', cancelled_at = now(), cancelled_by = 'rider', cancel_reason = v_reason
     where id = p_ride_id
    returning * into v_ride;
  elsif v_outcome = 'no_show' then
    perform private.record_cancellation(v_ride, v_ride.rider_id, 'rider', 'no_show', null, 'counted', null);
    update public.rides
       set status = 'cancelled', cancelled_at = now(), cancelled_by = 'driver', cancel_reason = 'rider_no_show'
     where id = p_ride_id
    returning * into v_ride;
  else
    -- El conductor suelta el viaje: vuelve a buscar conductor
    update public.rides
       set status = 'searching', driver_id = null, vehicle_id = null, accepted_at = null, arrived_at = null,
           driver_name = null, driver_phone = null, driver_photo_url = null,
           vehicle_plate = null, vehicle_description = null, rider_phone = null,
           driver_on_other_trip = false, driver_rating = null, driver_rating_count = null,
           accept_pickup_distance_m = null
     where id = p_ride_id
    returning * into v_ride;
  end if;

  if v_driver is not null then
    perform private.refresh_driver_availability(v_driver);
  end if;
  return v_ride;
end
$$;
revoke all on function public.cancel_ride_with_reason(uuid, text, text) from public, anon;
grant execute on function public.cancel_ride_with_reason(uuid, text, text) to authenticated;

-- Installed apps keep calling the old signature; their reasons are recorded as 'other'.
create or replace function public.cancel_ride(p_ride_id uuid, p_reason text default null)
returns public.rides language sql security invoker set search_path = '' as $$
  select public.cancel_ride_with_reason(p_ride_id, p_reason, null)
$$;

-- What a cancellation would mean, before the user confirms it.
create function private.cancellation_preview(p_ride_id uuid, p_reason text)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
  v_role text;
  v_eval jsonb;
  v_status jsonb;
  v_counts boolean;
  v_policy private.trip_policy_settings;
begin
  select * into v_ride from public.rides where id = p_ride_id;
  if not found or v_uid is null or (v_ride.rider_id <> v_uid and v_ride.driver_id is distinct from v_uid) then
    raise exception 'ride_not_found' using errcode = 'P0002';
  end if;
  select * into v_policy from private.trip_policy_settings where singleton;
  v_role := case when v_ride.rider_id = v_uid then 'rider' else 'driver' end;
  v_eval := private.cancellation_outcome(v_ride, v_uid, v_role, private.normalize_cancel_reason(v_role, p_reason));
  v_status := private.cancellation_status(v_uid, v_role);
  v_counts := v_policy.cancellation_penalties_enabled and v_eval->>'outcome' = 'counted';
  return v_eval || jsonb_build_object(
    'role', v_role,
    'counts', v_counts,
    'penalties_enabled', v_policy.cancellation_penalties_enabled,
    'strikes', (v_status->>'strikes')::integer,
    'limit', v_policy.strike_limit,
    'window_days', v_policy.strike_window_days,
    'would_block', v_counts and (v_status->>'strikes')::integer + 1 >= v_policy.strike_limit,
    'block_minutes', case when exists (select 1 from private.cancellation_blocks
                                       where user_id = v_uid and role = v_role and created_at > now() - interval '30 days')
                          then v_policy.repeat_block_minutes else v_policy.first_block_minutes end,
    'no_show_wait_seconds', v_policy.no_show_wait_seconds);
end $$;
revoke all on function private.cancellation_preview(uuid, text) from public, anon;
grant execute on function private.cancellation_preview(uuid, text) to authenticated;
create function public.cancellation_preview(p_ride_id uuid, p_reason text)
returns jsonb language sql stable security invoker set search_path = '' as $$
  select private.cancellation_preview(p_ride_id, p_reason)
$$;
revoke all on function public.cancellation_preview(uuid, text) from public, anon;
grant execute on function public.cancellation_preview(uuid, text) to authenticated;

create function private.my_cancellation_status()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare v_uid text := private.requesting_uid();
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  return jsonb_build_object('rider', private.cancellation_status(v_uid, 'rider'),
                            'driver', private.cancellation_status(v_uid, 'driver'));
end $$;
revoke all on function private.my_cancellation_status() from public, anon;
grant execute on function private.my_cancellation_status() to authenticated;
create function public.my_cancellation_status()
returns jsonb language sql stable security invoker set search_path = '' as $$
  select private.my_cancellation_status()
$$;
revoke all on function public.my_cancellation_status() from public, anon;
grant execute on function public.my_cancellation_status() to authenticated;

-- ---------------------------------------------------------------------
-- Admin
-- ---------------------------------------------------------------------

create function private.get_trip_policy()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  return (select to_jsonb(s) - 'singleton' - 'updated_by' from private.trip_policy_settings s where singleton);
end $$;
revoke all on function private.get_trip_policy() from public, anon;
grant execute on function private.get_trip_policy() to authenticated;
create function public.admin_get_trip_policy()
returns jsonb language sql stable security invoker set search_path = '' as $$
  select private.get_trip_policy()
$$;
revoke all on function public.admin_get_trip_policy() from public, anon;
grant execute on function public.admin_get_trip_policy() to authenticated;

-- Only the keys present in p_settings change; the table checks reject out-of-range values.
create function private.set_trip_policy(p_settings jsonb)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare s jsonb := coalesce(p_settings, '{}'::jsonb);
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  if jsonb_typeof(s) <> 'object' or exists (select 1 from jsonb_each(s) e where jsonb_typeof(e.value) = 'null') then
    raise exception 'invalid_preferences' using errcode = '22023';
  end if;
  update private.trip_policy_settings set
    show_driver_rating = coalesce((s->>'show_driver_rating')::boolean, show_driver_rating),
    show_rider_rating = coalesce((s->>'show_rider_rating')::boolean, show_rider_rating),
    cancellation_penalties_enabled = coalesce((s->>'cancellation_penalties_enabled')::boolean, cancellation_penalties_enabled),
    free_cancel_seconds = coalesce((s->>'free_cancel_seconds')::integer, free_cancel_seconds),
    strike_limit = coalesce((s->>'strike_limit')::integer, strike_limit),
    strike_window_days = coalesce((s->>'strike_window_days')::integer, strike_window_days),
    excused_limit = coalesce((s->>'excused_limit')::integer, excused_limit),
    first_block_minutes = coalesce((s->>'first_block_minutes')::integer, first_block_minutes),
    repeat_block_minutes = coalesce((s->>'repeat_block_minutes')::integer, repeat_block_minutes),
    no_show_wait_seconds = coalesce((s->>'no_show_wait_seconds')::integer, no_show_wait_seconds),
    no_show_radius_m = coalesce((s->>'no_show_radius_m')::integer, no_show_radius_m),
    max_requests_per_10_min = coalesce((s->>'max_requests_per_10_min')::integer, max_requests_per_10_min),
    updated_at = now(), updated_by = private.requesting_uid()
  where singleton;
  return private.get_trip_policy();
end $$;
revoke all on function private.set_trip_policy(jsonb) from public, anon;
grant execute on function private.set_trip_policy(jsonb) to authenticated;
create function public.admin_set_trip_policy(p_settings jsonb)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.set_trip_policy(p_settings)
$$;
revoke all on function public.admin_set_trip_policy(jsonb) from public, anon;
grant execute on function public.admin_set_trip_policy(jsonb) to authenticated;

-- People with the most counted cancellations, reports against them and active blocks,
-- plus the latest cancellations where someone blamed the other participant.
create function private.cancellation_overview(p_days integer)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare v_since timestamptz := now() - make_interval(days => least(greatest(coalesce(p_days, 7), 1), 90));
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  return jsonb_build_object(
    'people', coalesce((select jsonb_agg(row_to_json(t)::jsonb order by t.counted desc, t.reports desc, t.total desc) from (
      select u.user_id, u.role,
             nullif(trim(coalesce(p.first_name, '') || ' ' || coalesce(p.last_name, '')), '') as name,
             count(c.id) filter (where c.outcome = 'counted') as counted,
             count(c.id) filter (where c.outcome = 'excused') as excused,
             count(c.id) as total,
             (select count(*) from private.ride_cancellations x
               where x.reported_user_id = u.user_id and x.created_at > v_since) as reports,
             (select max(b.blocked_until) from private.cancellation_blocks b
               where b.user_id = u.user_id and b.role = u.role and b.lifted_at is null and b.blocked_until > now()) as blocked_until
      from (select distinct user_id, role from private.ride_cancellations where created_at > v_since
            union select distinct reported_user_id, case when role = 'rider' then 'driver' else 'rider' end
            from private.ride_cancellations where reported_user_id is not null and created_at > v_since) u
      left join private.ride_cancellations c on c.user_id = u.user_id and c.role = u.role and c.created_at > v_since
      left join public.profiles p on p.id = u.user_id
      group by u.user_id, u.role, p.first_name, p.last_name
      order by count(c.id) filter (where c.outcome = 'counted') desc, count(c.id) desc
      limit 40) t), '[]'::jsonb),
    'reports', coalesce((select jsonb_agg(row_to_json(t)::jsonb order by t.created_at desc) from (
      select c.ride_id, c.created_at, c.role, c.reason_code, c.note, c.outcome,
             nullif(trim(coalesce(p.first_name, '') || ' ' || coalesce(p.last_name, '')), '') as user_name,
             nullif(trim(coalesce(q.first_name, '') || ' ' || coalesce(q.last_name, '')), '') as reported_name
      from private.ride_cancellations c
      left join public.profiles p on p.id = c.user_id
      left join public.profiles q on q.id = c.reported_user_id
      where c.reported_user_id is not null and c.created_at > v_since
      order by c.created_at desc limit 30) t), '[]'::jsonb));
end $$;
revoke all on function private.cancellation_overview(integer) from public, anon;
grant execute on function private.cancellation_overview(integer) to authenticated;
create function public.admin_cancellation_overview(p_days integer default 7)
returns jsonb language sql stable security invoker set search_path = '' as $$
  select private.cancellation_overview(p_days)
$$;
revoke all on function public.admin_cancellation_overview(integer) from public, anon;
grant execute on function public.admin_cancellation_overview(integer) to authenticated;

create function private.lift_cancellation_block(p_user_id text, p_role text)
returns jsonb language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  update private.cancellation_blocks set lifted_at = now(), lifted_by = private.requesting_uid()
   where user_id = p_user_id and role = p_role and lifted_at is null and blocked_until > now();
  return private.cancellation_status(p_user_id, p_role);
end $$;
revoke all on function private.lift_cancellation_block(text, text) from public, anon;
grant execute on function private.lift_cancellation_block(text, text) to authenticated;
create function public.admin_lift_cancellation_block(p_user_id text, p_role text)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.lift_cancellation_block(p_user_id, p_role)
$$;
revoke all on function public.admin_lift_cancellation_block(text, text) from public, anon;
grant execute on function public.admin_lift_cancellation_block(text, text) to authenticated;

-- ---------------------------------------------------------------------
-- Push: a verified no-show tells the rider why the ride ended
-- ---------------------------------------------------------------------

create or replace function private.notify_ride_update()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_to     text;
  v_title  text;
  v_body   text;
  v_tokens text[];
  v_secret text;
  v_plate  text := nullif(new.vehicle_plate, '');
  v_driver text := coalesce(nullif(new.driver_name, ''), case when new.service_kind = 'delivery' then 'Tu repartidor' else 'Tu conductor' end);
begin
  case new.status
    when 'accepted' then
      v_to := new.rider_id;
      v_title := case when new.service_kind = 'delivery' then 'Tu repartidor va en camino' else 'Tu conductor va en camino' end;
      v_body := concat_ws(' · ', v_driver, nullif(new.vehicle_description, ''), 'Placa ' || v_plate);
    when 'arrived' then
      v_to := new.rider_id;
      v_title := case when new.service_kind = 'delivery' then 'Tu repartidor llegó al recojo' else 'Tu conductor llegó' end;
      v_body := v_driver || ' te espera en el punto de recojo.' || coalesce(' Placa ' || v_plate || '.', '');
    when 'in_progress' then
      v_to := new.rider_id;
      v_title := case when new.service_kind = 'delivery' then 'Tu paquete está en camino' else 'Viaje iniciado' end;
      v_body := case when new.service_kind = 'delivery' then 'Tu paquete va hacia ' else 'Vas camino a ' end || new.destination_address || '.';
    when 'completed' then
      v_to := new.rider_id;
      v_title := case when new.service_kind = 'delivery' then 'Envío entregado' else 'Llegaste a tu destino' end;
      v_body := 'Total: S/ ' || to_char(coalesce(new.final_fare, new.estimated_fare), 'FM999990.00')
                || case when new.payment_method = 'yape_plin' then ' por Yape o Plin' else ' en efectivo' end
                || case when new.service_kind = 'delivery' then '. Califica a tu repartidor.' else '. Califica tu viaje.' end;
    when 'searching' then
      -- El conductor soltó el viaje (cancel_ride lo devuelve a la búsqueda)
      if old.status not in ('accepted', 'arrived') then
        return null;
      end if;
      v_to := new.rider_id;
      v_title := case when new.service_kind = 'delivery' then 'Tu repartidor canceló' else 'Tu conductor canceló' end;
      v_body := case when new.service_kind = 'delivery' then 'Estamos buscando otro repartidor para tu envío.' else 'Estamos buscando otro conductor para ti.' end;
    when 'cancelled' then
      if new.cancelled_by = 'rider' then
        v_to := old.driver_id;
        v_title := case when new.service_kind = 'delivery' then 'Quien envía canceló el pedido' else 'El pasajero canceló el viaje' end;
        v_body := 'Ya puedes recibir otras solicitudes.';
      elsif new.cancelled_by = 'driver' and new.cancel_reason = 'rider_no_show' then
        v_to := new.rider_id;
        v_title := case when new.service_kind = 'delivery' then 'Tu repartidor no te encontró' else 'Tu conductor no te encontró' end;
        v_body := v_driver || ' esperó en el punto de recojo y canceló el servicio.';
      elsif new.cancel_reason = 'no_driver_found' then
        v_to := new.rider_id;
        v_title := case when new.service_kind = 'delivery' then 'No encontramos repartidor' else 'No encontramos conductor' end;
        v_body := case when new.service_kind = 'delivery' then 'No hay repartidores disponibles cerca. Intenta de nuevo en unos minutos.' else 'No hay conductores disponibles cerca. Intenta de nuevo en unos minutos.' end;
      else
        v_to := new.rider_id;
        v_title := case when new.service_kind = 'delivery' then 'Tu envío fue cancelado' else 'Tu viaje fue cancelado' end;
        v_body := case when new.service_kind = 'delivery' then 'Puedes pedir otro envío cuando quieras.' else 'Puedes pedir otro viaje cuando quieras.' end;
      end if;
    else
      return null;
  end case;

  if v_to is null then
    return null;
  end if;
  select array_agg(token) into v_tokens from public.device_tokens where user_id = v_to;
  select decrypted_secret into v_secret from vault.decrypted_secrets where name = 'push_webhook_secret';
  if v_tokens is null or v_secret is null then
    return null;
  end if;

  perform net.http_post(
    url := 'https://us-central1-intu-e8403.cloudfunctions.net/ridePush',
    body := jsonb_build_object(
      'tokens', to_jsonb(v_tokens),
      'title', v_title,
      'body', v_body,
      'rideId', new.id,
      'status', new.status
    ),
    headers := jsonb_build_object('Content-Type', 'application/json', 'X-Intu-Secret', v_secret),
    timeout_milliseconds := 8000
  );
  return null;
exception when others then
  -- Un aviso que falla nunca debe impedir el cambio de estado del viaje
  raise warning 'notify_ride_update: %', sqlerrm;
  return null;
end
$$;
