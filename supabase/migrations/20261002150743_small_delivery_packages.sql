-- Initial courier service transports small parcels only, without shopping or product collection.
-- There are no deliveries before activation; the acknowledgement is required on every new order.
alter table public.delivery_details add column small_package_confirmed boolean not null
  check (small_package_confirmed = true);
grant insert (small_package_confirmed) on public.delivery_details to authenticated;
create or replace function public.create_delivery_request(
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
     or p_details ->> 'description' is null or p_details ->> 'payer' is null
     or (p_details -> 'small_package_confirmed') is distinct from 'true'::jsonb then
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
    pickup_reference, delivery_reference, payer, small_package_confirmed)
  values (v_ride.id, trim(p_details ->> 'recipient_name'), p_details ->> 'recipient_phone',
    trim(p_details ->> 'description'), trim(coalesce(p_details ->> 'pickup_reference', '')),
    trim(coalesce(p_details ->> 'delivery_reference', '')), p_details ->> 'payer', true);
  return v_ride;
end
$$;
revoke all on function public.create_delivery_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,jsonb)
  from public, anon;
grant execute on function public.create_delivery_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,jsonb)
  to authenticated;


