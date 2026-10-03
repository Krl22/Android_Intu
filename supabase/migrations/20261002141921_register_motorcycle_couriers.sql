-- Moto lineal can apply and be approved; ordering/delivery fares are not enabled yet.
-- Keep passenger capacity explicit instead of advertising a seat on a courier motorcycle.
alter table public.vehicle_types add column service_kind text not null default 'passenger'
  check (service_kind in ('passenger', 'delivery'));
alter table public.vehicle_types drop constraint vehicle_types_max_passengers_check;
alter table public.vehicle_types add constraint vehicle_types_capacity_check check (
  (service_kind = 'passenger' and max_passengers > 0) or
  (service_kind = 'delivery' and max_passengers = 0)
);
insert into public.vehicle_types
  (code, name, base_fare, per_km, per_minute, min_fare, max_passengers, is_active, service_kind)
values ('motorcycle', 'Moto lineal', 0, 0, 0, 0, 0, false, 'delivery');

-- Pending/inactive services cannot read open requests or publish driver availability.
create or replace function private.current_driver_vehicle_type()
returns text language sql stable security definer set search_path = '' as $$
  select v.vehicle_type
  from public.drivers d
  join public.vehicles v on v.driver_id = d.id and v.is_active
  join public.vehicle_types vt on vt.code = v.vehicle_type and vt.is_active
  where d.id = private.requesting_uid() and d.status = 'approved'
$$;
revoke all on function private.current_driver_vehicle_type() from public, anon;
grant execute on function private.current_driver_vehicle_type() to authenticated;

-- Vehicle approval belongs to the reviewed vehicle, not just to the account.
-- Only this internal trigger can reset approval; clients still cannot set drivers.status.
create or replace function private.vehicles_before_write()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
  v_status public.driver_status;
  v_changed boolean;
begin
  new.plate := upper(regexp_replace(new.plate, '\s', '', 'g'));
  if private.requesting_uid() is not null then
    if new.driver_id is distinct from private.requesting_uid() then
      raise exception 'not_vehicle_owner' using errcode = '42501';
    end if;
    select status into v_status from public.drivers where id = new.driver_id for update;
    if tg_op = 'INSERT' then
      v_changed := new.is_active;
    else
      v_changed := (new.vehicle_type, new.brand, new.model, new.year, new.plate, new.is_active)
        is distinct from (old.vehicle_type, old.brand, old.model, old.year, old.plate, old.is_active);
    end if;
    if v_changed and v_status = 'approved' then
      if exists (select 1 from public.rides where driver_id = new.driver_id
                   and status in ('accepted', 'arrived', 'in_progress')) then
        raise exception 'vehicle_change_during_ride' using errcode = 'P0001';
      end if;
      update public.drivers set status = 'pending', approved_at = null where id = new.driver_id;
      update public.profiles set driver_mode = false where id = new.driver_id;
      update public.driver_locations set is_available = false where driver_id = new.driver_id;
    end if;
  end if;
  return new;
end
$$;
revoke all on function private.vehicles_before_write() from public, anon, authenticated;

-- Atomic submission under existing ownership RLS. A plate/DNI conflict rolls back both records,
-- so a failed application does not leave a pending driver without a vehicle.
create or replace function public.submit_driver_application(
  p_document_number text, p_license_number text, p_vehicle_type text,
  p_brand text, p_model text, p_year integer, p_plate text
)
returns public.drivers language plpgsql security invoker set search_path = '' as $$
declare
  v_uid text := private.requesting_uid();
  v_vehicle_id uuid;
  v_driver public.drivers;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if p_vehicle_type not in ('mototaxi', 'motorcycle') or p_vehicle_type is null then
    raise exception 'vehicle_type_not_available' using errcode = '22023';
  end if;
  if nullif(trim(p_brand), '') is null or nullif(trim(p_model), '') is null
     or nullif(trim(p_license_number), '') is null or nullif(trim(p_plate), '') is null
     or p_year is null then
    raise exception 'invalid_driver_application' using errcode = '22023';
  end if;
  insert into public.drivers (id, document_type, document_number, license_number)
    values (v_uid, 'dni', trim(p_document_number), trim(p_license_number))
    on conflict (id) do update set document_type = excluded.document_type,
      document_number = excluded.document_number, license_number = excluded.license_number;
  select id into v_vehicle_id from public.vehicles where driver_id = v_uid and is_active for update;
  if v_vehicle_id is null then
    insert into public.vehicles (driver_id, vehicle_type, brand, model, year, plate, is_active)
      values (v_uid, p_vehicle_type, trim(p_brand), trim(p_model), p_year, trim(p_plate), true);
  else
    update public.vehicles set vehicle_type = p_vehicle_type, brand = trim(p_brand),
      model = trim(p_model), year = p_year, plate = trim(p_plate)
      where id = v_vehicle_id;
  end if;
  select * into v_driver from public.drivers where id = v_uid;
  return v_driver;
end
$$;
revoke all on function public.submit_driver_application(text, text, text, text, text, integer, text) from public, anon;
grant execute on function public.submit_driver_application(text, text, text, text, text, integer, text) to authenticated;

-- Apply the same active-service restriction to the nearby lookup.
create or replace function public.nearby_drivers(
  p_latitude double precision, p_longitude double precision,
  p_radius_m integer default 3000, p_vehicle_type text default null, p_limit integer default 20
)
returns table (driver_id text, latitude double precision, longitude double precision,
  heading real, vehicle_type text, distance_m double precision)
language sql stable security definer set search_path = '' as $$
  with p as (
    select extensions.st_setsrid(extensions.st_makepoint(p_longitude, p_latitude), 4326)::extensions.geography as pt
  )
  select dl.driver_id, dl.latitude, dl.longitude, dl.heading, v.vehicle_type,
    extensions.st_distance(dl.location, p.pt) as distance_m
  from public.driver_locations dl cross join p
  join public.drivers d on d.id = dl.driver_id and d.status = 'approved'
  join public.vehicles v on v.driver_id = dl.driver_id and v.is_active
  join public.vehicle_types vt on vt.code = v.vehicle_type and vt.is_active
  where private.requesting_uid() is not null and dl.is_available
    and dl.updated_at > now() - interval '2 minutes'
    and (p_vehicle_type is null or v.vehicle_type = p_vehicle_type)
    and extensions.st_dwithin(dl.location, p.pt, least(greatest(p_radius_m, 0), 10000))
    and not exists (select 1 from public.rides r where r.driver_id = dl.driver_id
                    and r.status in ('accepted', 'arrived', 'in_progress'))
  order by distance_m limit least(greatest(p_limit, 1), 50)
$$;
revoke all on function public.nearby_drivers(double precision, double precision, integer, text, integer) from public, anon;
grant execute on function public.nearby_drivers(double precision, double precision, integer, text, integer) to authenticated;

-- Lock approval against concurrent vehicle changes while accepting a request.
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
  join public.vehicle_types vt on vt.code = v.vehicle_type and vt.is_active
  where d.id = v_uid and d.status = 'approved'
  for update of d;
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
revoke all on function public.accept_ride(uuid) from public, anon;
grant execute on function public.accept_ride(uuid) to authenticated;
