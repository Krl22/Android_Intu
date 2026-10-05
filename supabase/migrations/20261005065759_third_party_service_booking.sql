-- Guest identity is separate from the account that owns, tracks and cancels the service.
create table public.ride_passengers (
  ride_id uuid primary key references public.rides(id) on delete cascade,
  name text not null check (char_length(trim(name)) between 2 and 100),
  phone text not null check (phone ~ '^\+519[0-9]{8}$')
);
alter table public.ride_passengers enable row level security;
revoke all on public.ride_passengers from anon, authenticated;
grant select on public.ride_passengers to authenticated;
create policy "ride_passengers: participants read" on public.ride_passengers
  for select to authenticated using (exists (
    select 1 from public.rides r where r.id = ride_passengers.ride_id
      and (r.rider_id = (select private.requesting_uid()) or r.driver_id = (select private.requesting_uid()))
  ));

alter table public.delivery_details add column sender_name text, add column sender_phone text,
  add constraint delivery_sender_contact_valid check (
    (sender_name is null and sender_phone is null) or
    (sender_name is not null and sender_phone is not null and char_length(trim(sender_name)) between 2 and 100 and sender_phone ~ '^\+519[0-9]{8}$')
  );
-- Existing INSERT privileges are column-specific; the new columns are RPC-only.
revoke insert (sender_name, sender_phone), update (sender_name, sender_phone) on public.delivery_details from anon, authenticated;

create function private.create_guest_ride_request(
  p_origin_lat double precision, p_origin_lng double precision, p_origin_address text,
  p_destination_lat double precision, p_destination_lng double precision, p_destination_address text,
  p_distance_meters integer, p_duration_seconds integer, p_route_polyline text,
  p_payment_method text, p_preferred_brand text, p_contact jsonb
) returns public.rides language plpgsql security definer set search_path = '' as $$
declare v_ride public.rides;
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode='28000'; end if;
  if p_contact is null or jsonb_typeof(p_contact) <> 'object'
     or coalesce(char_length(trim(p_contact ->> 'name')),0) not between 2 and 100
     or coalesce(p_contact ->> 'phone','') !~ '^\+519[0-9]{8}$'
     or (p_preferred_brand is not null and p_preferred_brand not in ('honda','bajaj')) then
    raise exception 'invalid_passenger_contact' using errcode='22023';
  end if;
  insert into public.rides(vehicle_type, preferred_vehicle_brand, origin_lat, origin_lng, origin_address,
    destination_lat, destination_lng, destination_address, distance_meters, duration_seconds, route_polyline, payment_method)
  values ('mototaxi',p_preferred_brand,p_origin_lat,p_origin_lng,p_origin_address,
    p_destination_lat,p_destination_lng,p_destination_address,p_distance_meters,p_duration_seconds,p_route_polyline,p_payment_method)
  returning * into v_ride;
  insert into public.ride_passengers(ride_id,name,phone)
  values(v_ride.id,trim(p_contact ->> 'name'),p_contact ->> 'phone');
  return v_ride;
end $$;
create function public.create_guest_ride_request(
  p_origin_lat double precision, p_origin_lng double precision, p_origin_address text,
  p_destination_lat double precision, p_destination_lng double precision, p_destination_address text,
  p_distance_meters integer, p_duration_seconds integer, p_route_polyline text,
  p_payment_method text, p_preferred_brand text, p_contact jsonb
) returns public.rides language sql security invoker set search_path = '' as $$
  select private.create_guest_ride_request(p_origin_lat,p_origin_lng,p_origin_address,
    p_destination_lat,p_destination_lng,p_destination_address,p_distance_meters,p_duration_seconds,
    p_route_polyline,p_payment_method,p_preferred_brand,p_contact);
$$;
revoke all on function public.create_guest_ride_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,text,jsonb) from public,anon;
revoke all on function private.create_guest_ride_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,text,jsonb) from public,anon;
grant execute on function public.create_guest_ride_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,text,jsonb) to authenticated;
grant execute on function private.create_guest_ride_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,text,jsonb) to authenticated;

create function private.create_contact_delivery_request(
  p_origin_lat double precision, p_origin_lng double precision, p_origin_address text,
  p_destination_lat double precision, p_destination_lng double precision, p_destination_address text,
  p_distance_meters integer, p_duration_seconds integer, p_route_polyline text,
  p_payment_method text, p_details jsonb
) returns public.rides language plpgsql security definer set search_path = '' as $$
declare v_ride public.rides;
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode='28000'; end if;
  if p_details is null or jsonb_typeof(p_details) <> 'object'
     or coalesce(char_length(trim(p_details ->> 'sender_name')),0) not between 2 and 100
     or coalesce(p_details ->> 'sender_phone','') !~ '^\+519[0-9]{8}$' then
    raise exception 'invalid_sender_contact' using errcode='22023';
  end if;
  -- Retain the parcel validation, canonical motorcycle tariff and payment lifecycle.
  v_ride := public.create_delivery_request(p_origin_lat,p_origin_lng,p_origin_address,
    p_destination_lat,p_destination_lng,p_destination_address,p_distance_meters,p_duration_seconds,
    p_route_polyline,p_payment_method,p_details);
  update public.delivery_details set sender_name=trim(p_details ->> 'sender_name'),sender_phone=p_details ->> 'sender_phone'
    where ride_id=v_ride.id;
  return v_ride;
end $$;
create function public.create_contact_delivery_request(
  p_origin_lat double precision, p_origin_lng double precision, p_origin_address text,
  p_destination_lat double precision, p_destination_lng double precision, p_destination_address text,
  p_distance_meters integer, p_duration_seconds integer, p_route_polyline text,
  p_payment_method text, p_details jsonb
) returns public.rides language sql security invoker set search_path = '' as $$
  select private.create_contact_delivery_request(p_origin_lat,p_origin_lng,p_origin_address,
    p_destination_lat,p_destination_lng,p_destination_address,p_distance_meters,p_duration_seconds,
    p_route_polyline,p_payment_method,p_details);
$$;
revoke all on function public.create_contact_delivery_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,jsonb) from public,anon;
revoke all on function private.create_contact_delivery_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,jsonb) from public,anon;
grant execute on function public.create_contact_delivery_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,jsonb) to authenticated;
grant execute on function private.create_contact_delivery_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,jsonb) to authenticated;
