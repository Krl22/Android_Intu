-- Execute as postgres; every fixture, offer and preference is rolled back.
begin;
-- Deterministic defaults, independent of any admin edits made since deployment.
update public.vehicle_types set base_fare=1.5,per_km=1,per_minute=0.1,min_fare=3 where code='mototaxi';
update private.fare_settings set honda_premium_percent=12,rounding_step=0.1,driver_price_offers_enabled=false where singleton;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-fares-%') then raise exception 'Fixture collision'; end if;
  if has_function_privilege('anon','public.admin_set_fare_settings(jsonb)','execute')
    or has_function_privilege('anon','public.get_fare_settings()','execute')
    or has_function_privilege('anon','public.propose_ride_price(uuid,numeric)','execute')
    or has_function_privilege('anon','public.respond_ride_price_offer(uuid,boolean)','execute')
    or has_function_privilege('anon','public.my_ride_price_offers(uuid)','execute') then raise exception 'Anonymous access'; end if;
  if has_table_privilege('authenticated','private.fare_settings','update')
    or has_table_privilege('authenticated','private.ride_price_offers','select')
    or has_function_privilege('authenticated','private.accept_ride_for_driver(uuid,text)','execute')
    or has_column_privilege('authenticated','public.rides','estimated_fare','update') then raise exception 'Direct price access'; end if;
end $$;
insert into public.profiles(id,first_name,last_name)
select 'intu-qa-fares-' || n,'QA',n from unnest(array['admin','rider','rider2','stranger','honda','bajaj']) n;
insert into private.admins(user_id) values ('intu-qa-fares-admin');
insert into public.drivers(id,document_type,document_number,license_number,status) values
('intu-qa-fares-honda','dni','99108101','QA','approved'),('intu-qa-fares-bajaj','dni','99108102','QA','approved');
insert into public.vehicles(driver_id,vehicle_type,brand,model,year,plate) values
('intu-qa-fares-honda','mototaxi','Honda','QA',2024,'QAFH101'),('intu-qa-fares-bajaj','mototaxi','Bajaj','QA',2024,'QAFB101');
insert into public.driver_locations(driver_id,latitude,longitude,is_available) values
('intu-qa-fares-honda',-11.252,-74.637,true),('intu-qa-fares-bajaj',-11.252,-74.637,true);
update private.ride_security_settings set pin_enabled=false where singleton;
update private.trip_policy_settings set cancellation_penalties_enabled=false where singleton;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-rider","role":"authenticated"}',true);
do $$ begin
  if (public.get_fare_settings()->>'base_fare')::numeric <> 1.5 or (public.get_fare_settings()->>'min_fare')::numeric <> 3 then raise exception 'Defaults'; end if;
  begin perform public.admin_set_fare_settings('{}'); raise exception 'Non-admin wrote rates'; exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-admin","role":"authenticated"}',true);
select set_config('intu.qa_fares',public.get_fare_settings()::text,true);
do $$ declare initial jsonb := current_setting('intu.qa_fares')::jsonb; k text; begin
  foreach k in array array['base_fare','per_km','per_minute','min_fare','honda_premium_percent','rounding_step','driver_price_offers_enabled'] loop
    begin perform public.admin_set_fare_settings(initial - k); raise exception 'Missing field accepted: %',k; exception when invalid_parameter_value then null; end;
  end loop;
  begin perform public.admin_set_fare_settings(initial || '{"per_km":-1}'); raise exception 'Negative accepted'; exception when invalid_parameter_value then null; end;
  begin perform public.admin_set_fare_settings(initial || '{"base_fare":1.005}'); raise exception 'Precision accepted'; exception when invalid_parameter_value then null; end;
  begin perform public.admin_set_fare_settings(initial || '{"rounding_step":0}'); raise exception 'Zero step accepted'; exception when invalid_parameter_value then null; end;
  begin perform public.admin_set_fare_settings(initial || '{"honda_premium_percent":101}'); raise exception 'Premium accepted'; exception when invalid_parameter_value then null; end;
  perform public.admin_set_fare_settings('{"base_fare":2,"per_km":2,"per_minute":0.2,"min_fare":3.03,"honda_premium_percent":25,"rounding_step":0.5,"driver_price_offers_enabled":false}');
  if public.estimate_fare('mototaxi',3000,600) <> 10 then raise exception 'Editable formula'; end if;
  if public.estimate_fare('mototaxi',0,0) <> 3.5 then raise exception 'Minimum must not round down'; end if;
  if public.estimate_fare('motorcycle',3000,600) <> 5.2 then raise exception 'Delivery changed'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-rider2","role":"authenticated"}',true);
do $$ declare scheduled jsonb; begin
  scheduled := public.schedule_ride(now()+interval '1 hour',-11.252,-74.637,'QA',-11.25,-74.63,'QA',3000,600,null,'efectivo','honda',null);
  if (scheduled->>'estimated_fare')::numeric is distinct from 12.5 then raise exception 'Scheduled configurable premium'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-admin","role":"authenticated"}',true);
select public.admin_set_fare_settings(current_setting('intu.qa_fares')::jsonb || '{"driver_price_offers_enabled":false}');
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-rider","role":"authenticated"}',true);
with created as (
 insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
 values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',3000,600) returning id
) select set_config('intu.qa_fare_ride',id::text,true) from created;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-honda","role":"authenticated"}',true);
do $$ begin
  begin perform public.propose_ride_price(current_setting('intu.qa_fare_ride')::uuid,7.25); raise exception 'Disabled offer'; exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-admin","role":"authenticated"}',true);
select public.admin_set_fare_settings(current_setting('intu.qa_fares')::jsonb || '{"driver_price_offers_enabled":true}');
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-honda","role":"authenticated"}',true);
do $$ declare amount numeric; begin
  foreach amount in array array[0,-1,5.5,7.251,10000,'NaN'::numeric] loop
    begin perform public.propose_ride_price(current_setting('intu.qa_fare_ride')::uuid,amount); raise exception 'Invalid amount %',amount; exception when invalid_parameter_value then null; end;
  end loop;
end $$;
select set_config('intu.qa_honda_offer',(public.propose_ride_price(current_setting('intu.qa_fare_ride')::uuid,7.25)->>'id'),true);
do $$ begin
  begin perform public.propose_ride_price(current_setting('intu.qa_fare_ride')::uuid,8); raise exception 'Repeated offer'; exception when raise_exception then if sqlerrm <> 'offer_already_sent' then raise; end if; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-bajaj","role":"authenticated"}',true);
select set_config('intu.qa_bajaj_offer',(public.propose_ride_price(current_setting('intu.qa_fare_ride')::uuid,6)->>'id'),true);
do $$ begin
  if jsonb_array_length(public.my_ride_price_offers()) <> 1 then raise exception 'Driver sees other offers'; end if;
  begin perform public.respond_ride_price_offer(current_setting('intu.qa_honda_offer')::uuid,true); raise exception 'Driver accepted offer'; exception when no_data_found then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-stranger","role":"authenticated"}',true);
do $$ begin
  if jsonb_array_length(public.my_ride_price_offers(current_setting('intu.qa_fare_ride')::uuid)) <> 0 then raise exception 'Stranger sees offers'; end if;
  begin perform public.respond_ride_price_offer(current_setting('intu.qa_honda_offer')::uuid,true); raise exception 'Stranger accepted'; exception when no_data_found then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-rider","role":"authenticated"}',true);
do $$ begin
  if jsonb_array_length(public.my_ride_price_offers(current_setting('intu.qa_fare_ride')::uuid)) <> 2 then raise exception 'Rider missing offers'; end if;
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_fare_ride')::uuid and status='searching' and driver_id is null and estimated_fare=5.5) then raise exception 'Proposal assigned/changed fare early'; end if;
  perform public.respond_ride_price_offer(current_setting('intu.qa_bajaj_offer')::uuid,false);
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_fare_ride')::uuid and status='searching' and estimated_fare=5.5) then raise exception 'Reject changed ride'; end if;
  perform public.respond_ride_price_offer(current_setting('intu.qa_honda_offer')::uuid,true);
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_fare_ride')::uuid and status='accepted' and driver_id='intu-qa-fares-honda' and estimated_fare=7.25) then raise exception 'Accepted offer price/driver'; end if;
  begin perform public.respond_ride_price_offer(current_setting('intu.qa_bajaj_offer')::uuid,true); raise exception 'Second acceptance'; exception when no_data_found then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-admin","role":"authenticated"}',true);
select public.admin_set_fare_settings(current_setting('intu.qa_fares')::jsonb || '{"base_fare":9,"driver_price_offers_enabled":false}');
reset role;
do $$ begin
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_fare_ride')::uuid and estimated_fare=7.25) then raise exception 'Existing fare changed'; end if;
end $$;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-honda","role":"authenticated"}',true);
select public.advance_ride(current_setting('intu.qa_fare_ride')::uuid,'arrived');
select public.advance_ride(current_setting('intu.qa_fare_ride')::uuid,'in_progress');
select public.advance_ride(current_setting('intu.qa_fare_ride')::uuid,'completed');
reset role;
do $$ begin
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_fare_ride')::uuid and final_fare=7.25 and status='completed') then raise exception 'Final payment changed'; end if;
end $$;
-- Brand matching, unavailable drivers, switch-off and normal acceptance of a previously offered ride.
update public.driver_locations set is_available=true where driver_id='intu-qa-fares-honda';
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-admin","role":"authenticated"}',true);
select public.admin_set_fare_settings(current_setting('intu.qa_fares')::jsonb || '{"driver_price_offers_enabled":true}');
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-rider2","role":"authenticated"}',true);
with created as (
 insert into public.rides(vehicle_type,preferred_vehicle_brand,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
 values ('mototaxi','honda',-11.252,-74.637,'QA',-11.25,-74.63,'QA',3000,600) returning id
) select set_config('intu.qa_fare_second',id::text,true) from created;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-bajaj","role":"authenticated"}',true);
do $$ begin
  begin perform public.propose_ride_price(current_setting('intu.qa_fare_second')::uuid,8); raise exception 'Wrong brand proposed'; exception when no_data_found then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-honda","role":"authenticated"}',true);
select set_config('intu.qa_second_offer',(public.propose_ride_price(current_setting('intu.qa_fare_second')::uuid,8)->>'id'),true);
reset role;
update public.driver_locations set is_available=false where driver_id='intu-qa-fares-honda';
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-rider2","role":"authenticated"}',true);
do $$ begin
  if jsonb_array_length(public.my_ride_price_offers(current_setting('intu.qa_fare_second')::uuid)) <> 0 then raise exception 'Offline offer visible'; end if;
  begin perform public.respond_ride_price_offer(current_setting('intu.qa_second_offer')::uuid,true); raise exception 'Offline acceptance'; exception when raise_exception then if sqlerrm <> 'driver_unavailable' then raise; end if; end;
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_fare_second')::uuid and status='searching' and estimated_fare=6.2) then raise exception 'Failed acceptance changed fare'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-admin","role":"authenticated"}',true);
select public.admin_set_fare_settings(current_setting('intu.qa_fares')::jsonb || '{"driver_price_offers_enabled":false}');
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-rider2","role":"authenticated"}',true);
do $$ begin
  begin perform public.respond_ride_price_offer(current_setting('intu.qa_second_offer')::uuid,true); raise exception 'Disabled old acceptance'; exception when no_data_found then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-fares-honda","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_fare_second')::uuid);
reset role;
do $$ begin
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_fare_second')::uuid and status='accepted' and estimated_fare=6.2) then raise exception 'Normal acceptance applied proposal price'; end if;
  if not exists(select 1 from private.ride_price_offers where id=current_setting('intu.qa_second_offer')::uuid and status='withdrawn') then raise exception 'Disabled proposal not withdrawn'; end if;
end $$;
rollback;
select 'Editable fares, permissions, scheduled premium, independent delivery and passenger-approved offers: PASS' as verification;
