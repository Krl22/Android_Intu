-- Delivery recipient contacts are visible only to the sender and assigned courier.
-- They must not appear in the searching requests visible to all approved couriers.
alter table public.rides add column service_kind text not null default 'passenger'
  check (service_kind in ('passenger', 'delivery'));
create table public.delivery_details (
  ride_id uuid primary key references public.rides(id) on delete cascade,
  recipient_name text not null check (char_length(trim(recipient_name)) between 2 and 100),
  recipient_phone text not null check (recipient_phone ~ '^\+519[0-9]{8}$'),
  description text not null check (char_length(trim(description)) between 3 and 280),
  pickup_reference text not null default '' check (char_length(pickup_reference) <= 200),
  delivery_reference text not null default '' check (char_length(delivery_reference) <= 200),
  payer text not null check (payer in ('sender', 'recipient'))
);
alter table public.delivery_details enable row level security;
revoke all on public.delivery_details from public, anon, authenticated;
grant select on public.delivery_details to authenticated;
grant insert (ride_id, recipient_name, recipient_phone, description, pickup_reference, delivery_reference, payer)
  on public.delivery_details to authenticated;
create policy "delivery_details: participants read" on public.delivery_details for select to authenticated
  using (exists (select 1 from public.rides r where r.id = ride_id
    and (r.rider_id = (select private.requesting_uid()) or r.driver_id = (select private.requesting_uid()))));
create policy "delivery_details: sender creates" on public.delivery_details for insert to authenticated
  with check (exists (select 1 from public.rides r where r.id = ride_id and r.service_kind = 'delivery'
    and r.rider_id = (select private.requesting_uid()) and r.status = 'searching'));

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
  select nullif(trim(p.first_name || ' ' || p.last_name), ''), p.photo_url
    into new.rider_name, new.rider_photo_url from public.profiles p where p.id = new.rider_id;
  return new;
end
$$;
revoke all on function private.rides_before_insert() from public, anon, authenticated;

-- PostgREST direct rides INSERT cannot create an incomplete delivery. The RPC inserts both
-- records in the same transaction before this deferred check runs.
create function private.require_delivery_details()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if new.service_kind = 'delivery' and not exists (
       select 1 from public.delivery_details d where d.ride_id = new.id) then
    raise exception 'invalid_delivery_details' using errcode = '23514';
  end if;
  return null;
end
$$;
revoke all on function private.require_delivery_details() from public, anon, authenticated;
create constraint trigger rides_require_delivery_details
  after insert on public.rides deferrable initially deferred
  for each row execute function private.require_delivery_details();

create function public.create_delivery_request(
  p_origin_lat double precision, p_origin_lng double precision, p_origin_address text,
  p_destination_lat double precision, p_destination_lng double precision, p_destination_address text,
  p_distance_meters integer, p_duration_seconds integer, p_route_polyline text,
  p_payment_method text, p_details jsonb
)
returns public.rides language plpgsql security invoker set search_path = '' as $$
declare v_ride public.rides;
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if p_details is null or jsonb_typeof(p_details) <> 'object'
     or p_details ->> 'recipient_name' is null or p_details ->> 'recipient_phone' is null
     or p_details ->> 'description' is null or p_details ->> 'payer' is null then
    raise exception 'invalid_delivery_details' using errcode = '22023';
  end if;
  insert into public.rides (vehicle_type, origin_lat, origin_lng, origin_address,
    destination_lat, destination_lng, destination_address, distance_meters, duration_seconds,
    route_polyline, payment_method)
  values ('motorcycle', p_origin_lat, p_origin_lng, p_origin_address,
    p_destination_lat, p_destination_lng, p_destination_address, p_distance_meters, p_duration_seconds,
    p_route_polyline, p_payment_method)
  returning * into v_ride;
  insert into public.delivery_details (ride_id, recipient_name, recipient_phone, description,
    pickup_reference, delivery_reference, payer)
  values (v_ride.id, trim(p_details ->> 'recipient_name'), p_details ->> 'recipient_phone',
    trim(p_details ->> 'description'), trim(coalesce(p_details ->> 'pickup_reference', '')),
    trim(coalesce(p_details ->> 'delivery_reference', '')), p_details ->> 'payer');
  return v_ride;
end
$$;
revoke all on function public.create_delivery_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,jsonb)
  from public, anon;
grant execute on function public.create_delivery_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,jsonb)
  to authenticated;

-- Trial tariff authorized by Carlos: 20% below all passenger fare coefficients/minimum.
-- Activation follows the completed APK and confirmed delivery scope.
update public.vehicle_types set base_fare = 2.00, per_km = 0.80, per_minute = 0.08, min_fare = 3.20
  where code = 'motorcycle';
