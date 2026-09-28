-- Conductor siempre en línea, como Uber: mientras lleva a un pasajero (in_progress) puede aceptar
-- el siguiente viaje. Antes solo podía tener un viaje abierto. Ahora puede tener como máximo uno
-- en curso y uno por recoger (accepted/arrived); nunca dos recojos a la vez.

drop index if exists public.rides_one_open_per_driver;
create unique index rides_one_pickup_per_driver on public.rides (driver_id)
  where status in ('accepted', 'arrived');
create unique index rides_one_trip_per_driver on public.rides (driver_id)
  where status = 'in_progress';

-- El pasajero del siguiente viaje ve que su conductor está terminando otro viaje antes de ir por él
alter table public.rides add column if not exists driver_on_other_trip boolean not null default false;

-- Disponible solo si el conductor ya no tiene viajes abiertos
create or replace function private.refresh_driver_availability(p_driver_id text)
returns void
language sql
security definer
set search_path = ''
as $$
  update public.driver_locations
     set is_available = not exists (
           select 1 from public.rides
           where driver_id = p_driver_id and status in ('accepted', 'arrived', 'in_progress'))
   where driver_id = p_driver_id
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
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  select v.* into v_vehicle
  from public.drivers d
  join public.vehicles v on v.driver_id = d.id and v.is_active
  where d.id = v_uid and d.status = 'approved';
  if not found then
    raise exception 'driver_not_approved' using errcode = '42501';
  end if;

  -- Ocupado solo si ya va a recoger a alguien; con un pasajero a bordo sí puede aceptar el siguiente
  if exists (select 1 from public.rides
             where driver_id = v_uid and status in ('accepted', 'arrived')) then
    raise exception 'driver_busy' using errcode = 'P0001';
  end if;

  select * into v_driver from public.profiles where id = v_uid;

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
                                        where o.driver_id = v_uid and o.status = 'in_progress')
   where r.id = p_ride_id
     and r.status = 'searching'
     and r.vehicle_type = v_vehicle.vehicle_type
     and r.rider_id <> v_uid
  returning * into v_ride;

  if not found then
    raise exception 'ride_not_available' using errcode = 'P0002';
  end if;

  update public.driver_locations set is_available = false where driver_id = v_uid;
  return v_ride;
end
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
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  -- No se va por el siguiente pasajero ni se inicia su viaje hasta terminar el actual
  if p_status in ('arrived', 'in_progress') and exists (
       select 1 from public.rides
       where driver_id = v_uid and status = 'in_progress' and id <> p_ride_id) then
    raise exception 'finish_current_trip' using errcode = 'P0001';
  end if;

  if p_status = 'in_progress' and exists (
       select 1 from private.ride_start_pins
       where ride_id = p_ride_id
         and (verified_at is null or driver_id is distinct from v_uid)) then
    raise exception 'pin_required' using errcode = 'P0001';
  end if;

  update public.rides
     set status = p_status,
         arrived_at   = case when p_status = 'arrived'     then now() else arrived_at end,
         started_at   = case when p_status = 'in_progress' then now() else started_at end,
         completed_at = case when p_status = 'completed'   then now() else completed_at end,
         final_fare   = case when p_status = 'completed'   then estimated_fare else final_fare end,
         payment_confirmed_at = case when p_status = 'completed' then now() else payment_confirmed_at end
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

create or replace function public.cancel_ride(p_ride_id uuid, p_reason text default null)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
  v_driver text;
begin
  select * into v_ride from public.rides where id = p_ride_id for update;

  if not found or v_uid is null or (v_ride.rider_id <> v_uid and v_ride.driver_id is distinct from v_uid) then
    raise exception 'ride_not_found' using errcode = 'P0002';
  end if;
  if v_ride.status not in ('searching', 'accepted', 'arrived') then
    raise exception 'invalid_transition' using errcode = 'P0001';
  end if;

  v_driver := v_ride.driver_id;

  if v_ride.rider_id = v_uid then
    update public.rides
       set status = 'cancelled', cancelled_at = now(), cancelled_by = 'rider', cancel_reason = left(p_reason, 500)
     where id = p_ride_id
    returning * into v_ride;
  else
    -- El conductor suelta el viaje: vuelve a buscar conductor
    update public.rides
       set status = 'searching', driver_id = null, vehicle_id = null, accepted_at = null, arrived_at = null,
           driver_name = null, driver_phone = null, driver_photo_url = null,
           vehicle_plate = null, vehicle_description = null, rider_phone = null,
           driver_on_other_trip = false
     where id = p_ride_id
    returning * into v_ride;
  end if;

  if v_driver is not null then
    perform private.refresh_driver_availability(v_driver);
  end if;
  return v_ride;
end
$$;
