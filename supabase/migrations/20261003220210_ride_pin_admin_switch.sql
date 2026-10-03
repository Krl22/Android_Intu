-- Default off for new services; existing requests keep their original PIN contract.
create table private.ride_security_settings (
  singleton boolean primary key default true check (singleton),
  pin_enabled boolean not null default false,
  updated_at timestamptz not null default now(),
  updated_by text
);
alter table private.ride_security_settings enable row level security;
revoke all on private.ride_security_settings from public, anon, authenticated;
insert into private.ride_security_settings(singleton, pin_enabled) values (true, false);

alter table public.rides add column start_pin_required boolean not null default false;
update public.rides r set start_pin_required = true
where r.status in ('searching','accepted','arrived')
  and exists (select 1 from private.ride_start_pins p where p.ride_id = r.id);

create function private.assign_ride_pin_requirement()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  select pin_enabled into new.start_pin_required from private.ride_security_settings where singleton;
  if new.start_pin_required is null then raise exception 'security_settings_missing'; end if;
  return new;
end $$;
revoke all on function private.assign_ride_pin_requirement() from public, anon, authenticated;
create trigger rides_assign_pin_requirement before insert on public.rides
for each row execute function private.assign_ride_pin_requirement();

create function private.get_ride_security_settings()
returns jsonb language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  return (select jsonb_build_object('pin_enabled', pin_enabled)
    from private.ride_security_settings where singleton);
end $$;
revoke all on function private.get_ride_security_settings() from public, anon;
grant execute on function private.get_ride_security_settings() to authenticated;
create function public.admin_get_ride_security_settings()
returns jsonb language sql security invoker set search_path = '' as $$
  select private.get_ride_security_settings()
$$;
revoke all on function public.admin_get_ride_security_settings() from public, anon;
grant execute on function public.admin_get_ride_security_settings() to authenticated;

create function private.set_ride_security_settings(p_pin_enabled boolean)
returns jsonb language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  if p_pin_enabled is null then raise exception 'invalid_preferences' using errcode = '22023'; end if;
  update private.ride_security_settings set pin_enabled = p_pin_enabled,
    updated_at = now(), updated_by = private.requesting_uid() where singleton;
  return private.get_ride_security_settings();
end $$;
revoke all on function private.set_ride_security_settings(boolean) from public, anon;
grant execute on function private.set_ride_security_settings(boolean) to authenticated;
create function public.admin_set_ride_security_settings(p_pin_enabled boolean)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.set_ride_security_settings(p_pin_enabled)
$$;
revoke all on function public.admin_set_ride_security_settings(boolean) from public, anon;
grant execute on function public.admin_set_ride_security_settings(boolean) to authenticated;

-- Only the two participants can query the immutable requirement of their own ride.
create function public.ride_pin_requirement(p_ride_id uuid)
returns jsonb language plpgsql stable security invoker set search_path = '' as $$
declare v_required boolean; v_uid text := private.requesting_uid();
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  select start_pin_required into v_required from public.rides
    where id = p_ride_id and (rider_id = v_uid or driver_id = v_uid);
  if not found then raise exception 'ride_not_found' using errcode = 'P0002'; end if;
  return jsonb_build_object('required', v_required);
end $$;
revoke all on function public.ride_pin_requirement(uuid) from public, anon;
grant execute on function public.ride_pin_requirement(uuid) to authenticated;

create or replace function public.ride_start_pin(p_ride_id uuid)
returns table (pin text) language sql stable security definer set search_path = '' as $$
  select p.pin from private.ride_start_pins p join public.rides r on r.id = p.ride_id
  where p.ride_id = p_ride_id and r.rider_id = private.requesting_uid()
    and r.start_pin_required and r.status in ('searching','accepted','arrived')
$$;

create or replace function public.advance_ride(p_ride_id uuid, p_status public.ride_status)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
  v_payment_collected_at timestamptz;
  v_payer text;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  select * into v_ride from public.rides where id = p_ride_id and driver_id = v_uid for update;
  if not found then raise exception 'invalid_transition' using errcode = 'P0001'; end if;
  if v_ride.service_kind = 'delivery' then
    select payer, payment_collected_at into v_payer, v_payment_collected_at
      from public.delivery_details where ride_id = p_ride_id;
    if v_payment_collected_at is null and
       (p_status = 'completed' or (p_status = 'in_progress' and v_payer = 'sender')) then
      raise exception 'delivery_payment_required' using errcode = 'P0001';
    end if;
  end if;
  -- No se va por el siguiente pasajero ni se inicia su viaje hasta terminar el actual
  if p_status in ('arrived', 'in_progress') and exists (
       select 1 from public.rides
       where driver_id = v_uid and status = 'in_progress' and id <> p_ride_id) then
    raise exception 'finish_current_trip' using errcode = 'P0001';
  end if;

  if p_status = 'in_progress' and v_ride.start_pin_required and not exists (
       select 1 from private.ride_start_pins
       where ride_id = p_ride_id and verified_at is not null and driver_id = v_uid) then
    raise exception 'pin_required' using errcode = 'P0001';
  end if;

  update public.rides
     set status = p_status,
         arrived_at   = case when p_status = 'arrived'     then now() else arrived_at end,
         started_at   = case when p_status = 'in_progress' then now() else started_at end,
         completed_at = case when p_status = 'completed'   then now() else completed_at end,
         final_fare   = case when p_status = 'completed'   then estimated_fare else final_fare end,
         payment_confirmed_at = case when p_status = 'completed' then coalesce(v_payment_collected_at, now()) else payment_confirmed_at end
   where id = p_ride_id
     and driver_id = v_uid
     and (   (status = 'accepted'    and p_status = 'arrived')
          or (status = 'arrived'     and p_status = 'in_progress')
          or (status = 'in_progress' and p_status = 'completed'))
  returning * into v_ride;

  if not found then
    raise exception 'invalid_transition' using errcode = 'P0001';
  end if;

  if p_status = 'completed' then
    -- El siguiente pasajero ya no espera a que termine otro viaje
    update public.rides set driver_on_other_trip = false
     where driver_id = v_uid and status in ('accepted', 'arrived') and driver_on_other_trip;
    perform private.refresh_driver_availability(v_uid);
  end if;
  return v_ride;
end
$$;

-- Verify sender PIN before collecting payment at pickup.
create or replace function private.collect_delivery_payment(p_ride_id uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
  v_detail public.delivery_details;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  select * into v_ride from public.rides where id = p_ride_id and driver_id = v_uid for update;
  if not found or v_ride.service_kind <> 'delivery' then
    raise exception 'ride_not_found' using errcode = 'P0002';
  end if;
  select * into v_detail from public.delivery_details where ride_id = p_ride_id;
  if v_detail.payment_collected_at is not null then
    return jsonb_build_object('confirmed', true);
  end if;
  if (v_detail.payer = 'sender' and v_ride.status <> 'arrived')
     or (v_detail.payer = 'recipient' and v_ride.status <> 'in_progress') then
    raise exception 'invalid_payment_stage' using errcode = 'P0001';
  end if;
  if v_detail.payer = 'sender' and v_ride.start_pin_required and not exists (select 1 from private.ride_start_pins
       where ride_id = p_ride_id and driver_id = v_uid and verified_at is not null) then
    raise exception 'pin_required' using errcode = 'P0001';
  end if;
  update public.delivery_details set payment_collected_at = now() where ride_id = p_ride_id;
  return jsonb_build_object('confirmed', true);
end
$$;
