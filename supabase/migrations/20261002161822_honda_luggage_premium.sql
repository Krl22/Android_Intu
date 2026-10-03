-- Honda mototaxis carry a rear luggage rack. Premium authorized by Carlos: 12% over the
-- rounded passenger fare, rounded again to 0.1 (same as ServiceFare.withBrandPremium in the app).
-- Bajaj and "any brand" keep the base fare.
create or replace function private.rides_before_insert()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is not null then
    new.rider_id := private.requesting_uid();
    new.status := 'searching';
    new.requested_at := now();
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
  return new;
end
$$;
revoke all on function private.rides_before_insert() from public, anon, authenticated;
