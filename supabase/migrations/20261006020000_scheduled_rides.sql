-- Rides booked for later (passenger mototaxi only). A cron job dispatches each one as a normal
-- request 10 minutes before its time, acting as the rider, so every rule of a live request applies:
-- server fare, cancellation pauses, request limit and one open ride per rider.

create table private.scheduled_rides (
  id uuid primary key default gen_random_uuid(),
  rider_id text not null,
  scheduled_for timestamptz not null,
  status text not null default 'scheduled' check (status in ('scheduled', 'dispatched', 'cancelled', 'failed')),
  preferred_vehicle_brand text check (preferred_vehicle_brand in ('honda', 'bajaj')),
  origin_lat double precision not null,
  origin_lng double precision not null,
  origin_address text not null,
  destination_lat double precision not null,
  destination_lng double precision not null,
  destination_address text not null,
  distance_meters integer not null check (distance_meters > 0),
  duration_seconds integer not null check (duration_seconds > 0),
  route_polyline text,
  payment_method text not null check (payment_method in ('efectivo', 'yape_plin')),
  passenger_name text,
  passenger_phone text,
  estimated_fare numeric(10,2) not null,
  ride_id uuid references public.rides(id) on delete set null,
  failure_reason text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check ((passenger_name is null) = (passenger_phone is null))
);
create index scheduled_rides_due_idx on private.scheduled_rides (scheduled_for) where status = 'scheduled';
create index scheduled_rides_rider_idx on private.scheduled_rides (rider_id, scheduled_for);
alter table private.scheduled_rides enable row level security;
revoke all on private.scheduled_rides from public, anon, authenticated;

create function private.scheduled_ride_json(r private.scheduled_rides)
returns jsonb language sql immutable set search_path = '' as $$
  select to_jsonb(r) - 'rider_id' - 'route_polyline'
$$;

create function private.schedule_ride(p_scheduled_for timestamptz,
  p_origin_lat double precision, p_origin_lng double precision, p_origin_address text,
  p_destination_lat double precision, p_destination_lng double precision, p_destination_address text,
  p_distance_meters integer, p_duration_seconds integer, p_route_polyline text,
  p_payment_method text, p_preferred_brand text, p_contact jsonb)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_uid text := private.requesting_uid();
  v_fare numeric;
  v_row private.scheduled_rides;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  perform private.assert_not_blocked(v_uid, 'rider');
  if p_scheduled_for is null or p_scheduled_for < now() + interval '20 minutes'
     or p_scheduled_for > now() + interval '7 days' then
    raise exception 'invalid_schedule_time' using errcode = '22023';
  end if;
  if coalesce(btrim(p_origin_address), '') = '' or coalesce(btrim(p_destination_address), '') = ''
     or coalesce(p_distance_meters, 0) <= 0 or coalesce(p_duration_seconds, 0) <= 0
     or p_payment_method not in ('efectivo', 'yape_plin')
     or (p_preferred_brand is not null and p_preferred_brand not in ('honda', 'bajaj')) then
    raise exception 'invalid_scheduled_ride' using errcode = '22023';
  end if;
  if p_contact is not null and (jsonb_typeof(p_contact) <> 'object'
     or coalesce(char_length(btrim(p_contact ->> 'name')), 0) not between 2 and 100
     or coalesce(p_contact ->> 'phone', '') !~ '^\+519[0-9]{8}$') then
    raise exception 'invalid_passenger_contact' using errcode = '22023';
  end if;
  -- One reservation at a time per rider, so parallel calls cannot exceed the limits below
  perform pg_advisory_xact_lock(hashtext('scheduled_rides:' || v_uid));
  if (select count(*) from private.scheduled_rides where rider_id = v_uid and status = 'scheduled') >= 3 then
    raise exception 'too_many_scheduled' using errcode = 'P0001';
  end if;
  if exists (select 1 from private.scheduled_rides where rider_id = v_uid and status = 'scheduled'
             and scheduled_for between p_scheduled_for - interval '30 minutes' and p_scheduled_for + interval '30 minutes') then
    raise exception 'schedule_conflict' using errcode = 'P0001';
  end if;
  v_fare := private.compute_fare('mototaxi', p_distance_meters, p_duration_seconds);
  if v_fare is null then raise exception 'vehicle_type_not_available' using errcode = '22023'; end if;
  if p_preferred_brand = 'honda' then v_fare := round(v_fare * 1.12, 1); end if;

  insert into private.scheduled_rides(rider_id, scheduled_for, preferred_vehicle_brand,
    origin_lat, origin_lng, origin_address, destination_lat, destination_lng, destination_address,
    distance_meters, duration_seconds, route_polyline, payment_method, passenger_name, passenger_phone, estimated_fare)
  values (v_uid, date_trunc('minute', p_scheduled_for), p_preferred_brand,
    p_origin_lat, p_origin_lng, left(p_origin_address, 300), p_destination_lat, p_destination_lng, left(p_destination_address, 300),
    p_distance_meters, p_duration_seconds, p_route_polyline, p_payment_method,
    nullif(btrim(p_contact ->> 'name'), ''), p_contact ->> 'phone', v_fare)
  returning * into v_row;
  return private.scheduled_ride_json(v_row);
end $$;
revoke all on function private.schedule_ride(timestamptz, double precision, double precision, text, double precision,
  double precision, text, integer, integer, text, text, text, jsonb) from public, anon;
grant execute on function private.schedule_ride(timestamptz, double precision, double precision, text, double precision,
  double precision, text, integer, integer, text, text, text, jsonb) to authenticated;
create function public.schedule_ride(p_scheduled_for timestamptz,
  p_origin_lat double precision, p_origin_lng double precision, p_origin_address text,
  p_destination_lat double precision, p_destination_lng double precision, p_destination_address text,
  p_distance_meters integer, p_duration_seconds integer, p_route_polyline text default null,
  p_payment_method text default 'efectivo', p_preferred_brand text default null, p_contact jsonb default null)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.schedule_ride(p_scheduled_for, p_origin_lat, p_origin_lng, p_origin_address,
    p_destination_lat, p_destination_lng, p_destination_address, p_distance_meters, p_duration_seconds,
    p_route_polyline, p_payment_method, p_preferred_brand, p_contact)
$$;
revoke all on function public.schedule_ride(timestamptz, double precision, double precision, text, double precision,
  double precision, text, integer, integer, text, text, text, jsonb) from public, anon;
grant execute on function public.schedule_ride(timestamptz, double precision, double precision, text, double precision,
  double precision, text, integer, integer, text, text, text, jsonb) to authenticated;

-- Upcoming rides plus those that failed in the last day, so the rider learns why.
create function private.my_scheduled_rides()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare v_uid text := private.requesting_uid();
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  return coalesce((select jsonb_agg(private.scheduled_ride_json(r) order by r.scheduled_for)
    from private.scheduled_rides r
    where r.rider_id = v_uid and (r.status = 'scheduled'
      or (r.status = 'failed' and r.updated_at > now() - interval '1 day'))), '[]'::jsonb);
end $$;
revoke all on function private.my_scheduled_rides() from public, anon;
grant execute on function private.my_scheduled_rides() to authenticated;
create function public.my_scheduled_rides()
returns jsonb language sql stable security invoker set search_path = '' as $$
  select private.my_scheduled_rides()
$$;
revoke all on function public.my_scheduled_rides() from public, anon;
grant execute on function public.my_scheduled_rides() to authenticated;

create function private.cancel_scheduled_ride(p_id uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_row private.scheduled_rides;
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  update private.scheduled_rides set status = 'cancelled', updated_at = now()
   where id = p_id and rider_id = private.requesting_uid() and status in ('scheduled', 'failed')
  returning * into v_row;
  if not found then raise exception 'scheduled_ride_not_found' using errcode = 'P0002'; end if;
  return private.scheduled_ride_json(v_row);
end $$;
revoke all on function private.cancel_scheduled_ride(uuid) from public, anon;
grant execute on function private.cancel_scheduled_ride(uuid) to authenticated;
create function public.cancel_scheduled_ride(p_id uuid)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.cancel_scheduled_ride(p_id)
$$;
revoke all on function public.cancel_scheduled_ride(uuid) from public, anon;
grant execute on function public.cancel_scheduled_ride(uuid) to authenticated;

create function private.notify_scheduled_ride(p_user_id text, p_tag text, p_title text, p_body text)
returns void language plpgsql security definer set search_path = '' as $$
declare v_tokens text[]; v_secret text;
begin
  select array_agg(token) into v_tokens from public.device_tokens where user_id = p_user_id;
  select decrypted_secret into v_secret from vault.decrypted_secrets where name = 'push_webhook_secret';
  if v_tokens is null or v_secret is null then return; end if;
  perform net.http_post(
    url := 'https://us-central1-intu-e8403.cloudfunctions.net/ridePush',
    body := jsonb_build_object('tokens', to_jsonb(v_tokens), 'title', p_title, 'body', p_body,
                               'rideId', p_tag, 'status', 'scheduled'),
    headers := jsonb_build_object('Content-Type', 'application/json', 'X-Intu-Secret', v_secret),
    timeout_milliseconds := 8000);
exception when others then
  raise warning 'notify_scheduled_ride: %', sqlerrm;
end $$;
revoke all on function private.notify_scheduled_ride(text, text, text, text) from public, anon, authenticated;

create function private.dispatch_scheduled_rides()
returns void language plpgsql security definer set search_path = '' as $$
declare
  r private.scheduled_rides;
  v_ride public.rides;
  v_constraint text;
  v_reason text;
begin
  for r in select * from private.scheduled_rides
           where status = 'scheduled' and scheduled_for <= now() + interval '10 minutes'
           order by scheduled_for for update skip locked loop
    if r.scheduled_for < now() - interval '15 minutes' then
      update private.scheduled_rides set status = 'failed', failure_reason = 'expired', updated_at = now() where id = r.id;
      perform private.notify_scheduled_ride(r.rider_id, 'scheduled-' || r.id, 'No pudimos iniciar tu viaje programado',
        'Ya pasó la hora del viaje. Pide uno nuevo desde la app.');
      continue;
    end if;
    begin
      -- Acts as the rider: the insert triggers apply the same checks as a request from the app
      perform set_config('request.jwt.claims', jsonb_build_object('sub', r.rider_id, 'role', 'authenticated')::text, true);
      if r.passenger_name is not null then
        v_ride := private.create_guest_ride_request(r.origin_lat, r.origin_lng, r.origin_address,
          r.destination_lat, r.destination_lng, r.destination_address, r.distance_meters, r.duration_seconds,
          r.route_polyline, r.payment_method, r.preferred_vehicle_brand,
          jsonb_build_object('name', r.passenger_name, 'phone', r.passenger_phone));
      else
        insert into public.rides(vehicle_type, preferred_vehicle_brand, origin_lat, origin_lng, origin_address,
          destination_lat, destination_lng, destination_address, distance_meters, duration_seconds, route_polyline, payment_method)
        values ('mototaxi', r.preferred_vehicle_brand, r.origin_lat, r.origin_lng, r.origin_address,
          r.destination_lat, r.destination_lng, r.destination_address, r.distance_meters, r.duration_seconds,
          r.route_polyline, r.payment_method)
        returning * into v_ride;
      end if;
      update private.scheduled_rides set status = 'dispatched', ride_id = v_ride.id, updated_at = now() where id = r.id;
      perform private.notify_scheduled_ride(r.rider_id, v_ride.id::text,
        case when r.passenger_name is null then 'Buscando conductor para tu viaje programado'
             else 'Buscando conductor para ' || r.passenger_name end,
        'Recojo en ' || r.origin_address || '. Abre Intu para seguir el viaje.');
    exception when others then
      get stacked diagnostics v_constraint = constraint_name;
      v_reason := case
        when v_constraint = 'rides_one_open_per_rider' then 'open_ride'
        when sqlerrm = 'cancellation_block' then 'cancellation_block'
        when sqlerrm = 'too_many_requests' then 'too_many_requests'
        else 'error' end;
      update private.scheduled_rides set status = 'failed', failure_reason = v_reason, updated_at = now() where id = r.id;
      perform private.notify_scheduled_ride(r.rider_id, 'scheduled-' || r.id, 'No pudimos iniciar tu viaje programado',
        case v_reason
          when 'open_ride' then 'Tenías otro viaje en curso. Pide el viaje desde la app cuando termines.'
          when 'cancellation_block' then 'Tu cuenta estaba en pausa por cancelaciones.'
          else 'Pide el viaje desde la app.' end);
    end;
  end loop;
  perform set_config('request.jwt.claims', '', true);
end $$;
revoke all on function private.dispatch_scheduled_rides() from public, anon, authenticated;

select cron.schedule('dispatch-scheduled-rides', '* * * * *', $$select private.dispatch_scheduled_rides()$$);

-- Scheduled rides leave with the account too.
create or replace function private.profiles_before_delete_cleanup()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  delete from public.ride_messages where sender_id = old.id;
  delete from private.ride_cancellations where user_id = old.id;
  update private.ride_cancellations set reported_user_id = null where reported_user_id = old.id;
  delete from private.cancellation_blocks where user_id = old.id;
  delete from private.support_chat_usage where user_id = old.id;
  delete from private.scheduled_rides where rider_id = old.id;
  return old;
end $$;
