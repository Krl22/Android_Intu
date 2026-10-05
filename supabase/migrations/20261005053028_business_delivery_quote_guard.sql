-- Fail closed if an administrator edits a pickup while a rider prepares a quote.
create or replace function private.create_business_delivery_request(p_ad_id uuid,p_destination_lat float8,p_destination_lng float8,
  p_destination_address text,p_distance_meters int,p_duration_seconds int,p_route_polyline text,p_payment_method text,p_details jsonb)
returns public.rides language plpgsql security definer set search_path = '' as $$
declare v_ad private.business_ads; v_ride public.rides; v_enabled boolean; v_uid text:=private.requesting_uid();
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode='28000'; end if;
  select enabled into v_enabled from private.business_delivery_settings where singleton for share;
  if not v_enabled then raise exception 'business_delivery_disabled' using errcode='P0001'; end if;
  select * into v_ad from private.business_ads where id=p_ad_id and published and archived_at is null for share;
  if not found or v_ad.updated_at is distinct from (p_details->>'business_ad_updated_at')::timestamptz then
    raise exception 'business_ad_unavailable' using errcode='P0001';
  end if;
  if not exists(select 1 from private.business_test_couriers t
    join public.drivers d on d.id=t.driver_id and d.status='approved' and d.id<>v_uid
    join public.vehicles v on v.driver_id=d.id and v.is_active and v.vehicle_type='motorcycle') then
    raise exception 'business_no_test_courier' using errcode='P0001';
  end if;
  if p_details is null or jsonb_typeof(p_details)<>'object' or p_details->>'recipient_name' is null
    or p_details->>'recipient_phone' is null or p_details->>'description' is null
    or (p_details->>'payer') is distinct from 'recipient'
    or (p_details->'small_package_confirmed') is distinct from 'true'::jsonb then
    raise exception 'invalid_delivery_details' using errcode='22023';
  end if;
  insert into public.rides(business_ad_id,vehicle_type,origin_lat,origin_lng,origin_address,
    destination_lat,destination_lng,destination_address,distance_meters,duration_seconds,route_polyline,payment_method)
  values(v_ad.id,'motorcycle',v_ad.lat,v_ad.lng,'[DEMO] '||v_ad.name||' · '||v_ad.address,
    p_destination_lat,p_destination_lng,p_destination_address,p_distance_meters,p_duration_seconds,p_route_polyline,p_payment_method)
  returning * into v_ride;
  insert into public.delivery_details(ride_id,recipient_name,recipient_phone,description,pickup_reference,
    delivery_reference,payer,small_package_confirmed,business_name)
  values(v_ride.id,trim(p_details->>'recipient_name'),p_details->>'recipient_phone',trim(p_details->>'description'),
    trim(coalesce(p_details->>'pickup_reference','')),trim(coalesce(p_details->>'delivery_reference','')),'recipient',true,v_ad.name);
  return v_ride;
end $$;

-- Explicit deny policies document that these private tables are RPC-only.
create policy no_direct_access on private.business_ads for all to authenticated using(false) with check(false);
create policy no_direct_access on private.business_delivery_settings for all to authenticated using(false) with check(false);
create policy no_direct_access on private.business_test_couriers for all to authenticated using(false) with check(false);
