begin;
-- Keep these historical rounding fixtures independent of the admin's current rates.
update public.vehicle_types set base_fare=2.50, per_km=1.00, per_minute=0.10, min_fare=4.00 where code='mototaxi';
update private.fare_settings set honda_premium_percent=12, rounding_step=0.10 where singleton;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-brand-20261002-%') then raise exception 'Fixture collision'; end if;
  if has_column_privilege('authenticated','public.rides','preferred_vehicle_brand','UPDATE') then raise exception 'Client can change a sent brand preference'; end if;
end $$;
insert into public.profiles(id,first_name,last_name)
 select 'intu-qa-brand-20261002-' || name,'QA',name from unnest(array['riderhonda','riderbajaj','riderany','honda','bajaj','tvs']) name;
insert into public.drivers(id,document_type,document_number,license_number,status) values
 ('intu-qa-brand-20261002-honda','dni','99161201','QA','approved'),
 ('intu-qa-brand-20261002-bajaj','dni','99161202','QA','approved'),
 ('intu-qa-brand-20261002-tvs','dni','99161203','QA','approved');
insert into public.vehicles(driver_id,vehicle_type,brand,model,year,plate) values
 ('intu-qa-brand-20261002-honda','mototaxi',' Honda ','QA',2024,'QABH101'),
 ('intu-qa-brand-20261002-bajaj','mototaxi','BAJAJ','QA',2024,'QABB101'),
 ('intu-qa-brand-20261002-tvs','mototaxi','TVS','QA',2024,'QABT101');
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-brand-20261002-riderhonda","role":"authenticated"}',true);
insert into public.rides(vehicle_type,preferred_vehicle_brand,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
 values ('mototaxi','honda',-11.252,-74.637,'QA',-11.25,-74.63,'QA',1950,480);
select set_config('intu.qa_honda',(select id::text from public.rides where rider_id='intu-qa-brand-20261002-riderhonda'),true);
do $$ begin
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_honda')::uuid and estimated_fare=5.90) then raise exception 'Honda 12%% luggage premium changed'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-brand-20261002-riderbajaj","role":"authenticated"}',true);
insert into public.rides(vehicle_type,preferred_vehicle_brand,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
 values ('mototaxi','bajaj',-11.252,-74.637,'QA',-11.25,-74.63,'QA',1950,480);
do $$ begin
  if not exists(select 1 from public.rides where rider_id='intu-qa-brand-20261002-riderbajaj' and estimated_fare=5.30) then raise exception 'Passenger HALF_UP rounding changed'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-brand-20261002-riderany","role":"authenticated"}',true);
insert into public.rides(vehicle_type,preferred_vehicle_brand,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
 values ('mototaxi',null,-11.252,-74.637,'QA',-11.25,-74.63,'QA',1950,480);
do $$ begin
  if not exists(select 1 from public.rides where rider_id='intu-qa-brand-20261002-riderany' and estimated_fare=5.30) then raise exception 'Any-brand fare changed'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-brand-20261002-tvs","role":"authenticated"}',true);
do $$ begin
  if (select count(*) from public.rides where rider_id like 'intu-qa-brand-20261002-rider%') <> 1 then raise exception 'Other brand sees restricted Honda/Bajaj order'; end if;
end $$;
select public.accept_ride((select id from public.rides where rider_id='intu-qa-brand-20261002-riderany'));
select set_config('request.jwt.claims','{"sub":"intu-qa-brand-20261002-bajaj","role":"authenticated"}',true);
do $$ begin
  begin
    perform public.accept_ride(current_setting('intu.qa_honda')::uuid);
    raise exception 'Bajaj accepted searching Honda order';
  exception when no_data_found then null;
  end;
  if (select count(*) from public.rides where rider_id like 'intu-qa-brand-20261002-rider%') <> 1 then raise exception 'Bajaj filtering failed'; end if;
end $$;
select public.accept_ride((select id from public.rides where rider_id='intu-qa-brand-20261002-riderbajaj'));
select set_config('request.jwt.claims','{"sub":"intu-qa-brand-20261002-honda","role":"authenticated"}',true);
do $$ begin
  if (select count(*) from public.rides where rider_id like 'intu-qa-brand-20261002-rider%') <> 1 then raise exception 'Honda filtering failed'; end if;
end $$;
select public.accept_ride(current_setting('intu.qa_honda')::uuid);
set constraints all immediate;
reset role;
rollback;
select 'Honda/Bajaj RLS, case/space normalization, any-brand acceptance, server fare and Honda premium: PASS' verification;
