-- Passenger brand selection. NULL intentionally accepts any registered mototaxi brand.
alter table public.rides add column preferred_vehicle_brand text
  check (preferred_vehicle_brand in ('honda', 'bajaj')),
  add constraint rides_brand_only_for_passengers check (vehicle_type = 'mototaxi' or preferred_vehicle_brand is null);
grant insert (preferred_vehicle_brand) on public.rides to authenticated;

create function private.current_driver_vehicle_brand()
returns text language sql stable security definer set search_path = '' as $$
  select lower(trim(v.brand)) from public.vehicles v
  join public.drivers d on d.id = v.driver_id and d.status = 'approved'
  join public.vehicle_types t on t.code = v.vehicle_type and t.is_active
  where v.driver_id = private.requesting_uid() and v.is_active
  limit 1
$$;
revoke all on function private.current_driver_vehicle_brand() from public, anon;
grant execute on function private.current_driver_vehicle_brand() to authenticated;

alter policy "rides: participantes y conductores aprobados (solicitudes abier" on public.rides
  using (rider_id = (select private.requesting_uid()) or driver_id = (select private.requesting_uid())
    or (status = 'searching' and vehicle_type = (select private.current_driver_vehicle_type())
      and (preferred_vehicle_brand is null or preferred_vehicle_brand = (select private.current_driver_vehicle_brand()))));
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
revoke all on function public.accept_ride(uuid) from public, anon;
grant execute on function public.accept_ride(uuid) to authenticated;

