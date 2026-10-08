-- Real authorization and lifecycle checks, with every fixture rolled back.
begin;
update public.vehicle_types set base_fare=2.50, per_km=1.00, per_minute=0.10, min_fare=4.00 where code='mototaxi';
update private.fare_settings set honda_premium_percent=12, rounding_step=0.10 where singleton;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-contact-20261005-%') then raise exception 'Fixture collision'; end if;
  if has_table_privilege('anon','public.ride_passengers','select') or has_table_privilege('authenticated','public.ride_passengers','insert') then raise exception 'Guest contact grants'; end if;
  if has_column_privilege('authenticated','public.delivery_details','sender_phone','insert') then raise exception 'Sender contact writable'; end if;
  if has_function_privilege('anon','public.create_guest_ride_request(double precision,double precision,text,double precision,double precision,text,integer,integer,text,text,text,jsonb)','execute') then raise exception 'Anonymous guest RPC'; end if;
end $$;
insert into public.profiles(id,first_name,last_name) select 'intu-qa-contact-20261005-' || name,'QA',name
  from unnest(array['booker','stranger','taxi','courier']) name;
insert into public.drivers(id,document_type,document_number,license_number,status)
values ('intu-qa-contact-20261005-taxi','dni','99805101','QA','approved'),('intu-qa-contact-20261005-courier','dni','99805102','QA','approved');
insert into public.vehicles(driver_id,vehicle_type,brand,model,year,plate)
values ('intu-qa-contact-20261005-taxi','mototaxi','honda','QA',2024,'QAC051'),('intu-qa-contact-20261005-courier','motorcycle','QA','QA',2024,'QAC052');
update public.vehicle_types set is_active=true where code='motorcycle';
update private.ride_security_settings set pin_enabled=true where singleton;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-contact-20261005-booker","role":"authenticated"}',true);
do $$ begin
  begin
    perform public.create_guest_ride_request(-11.252,-74.637,'Recojo del contacto',-11.25,-74.63,'Destino del contacto',2100,420,null,'efectivo',null,'{"name":"Ana QA","phone":"987"}');
    raise exception 'Invalid guest accepted';
  exception when invalid_parameter_value then null; end;
  if exists(select 1 from public.rides) then raise exception 'Orphan guest ride'; end if;
end $$;
select set_config('intu.qa_guest',(public.create_guest_ride_request(-11.252,-74.637,'Recojo del contacto',-11.25,-74.63,'Destino del contacto',2100,420,null,'efectivo','honda',
  '{"name":" Ana QA ","phone":"+51987654321"}')).id::text,true);
select set_config('intu.qa_guest_pin',(select pin from public.ride_start_pin(current_setting('intu.qa_guest')::uuid)),true);
do $$ begin
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_guest')::uuid and rider_id='intu-qa-contact-20261005-booker' and estimated_fare=5.9 and service_kind='passenger') then raise exception 'Guest replaced owner or fare'; end if;
  if not exists(select 1 from public.ride_passengers where ride_id=current_setting('intu.qa_guest')::uuid and name='Ana QA') then raise exception 'Booker cannot read guest'; end if;
  begin update public.ride_passengers set phone='+51911111111'; raise exception 'Guest writable'; exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-contact-20261005-stranger","role":"authenticated"}',true);
do $$ begin if exists(select 1 from public.ride_passengers) then raise exception 'Stranger sees guest'; end if; end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-contact-20261005-taxi","role":"authenticated"}',true);
do $$ begin if exists(select 1 from public.ride_passengers) then raise exception 'Unassigned driver sees guest'; end if; end $$;
select public.accept_ride(current_setting('intu.qa_guest')::uuid);
do $$ begin if not exists(select 1 from public.ride_passengers where name='Ana QA' and phone='+51987654321') then raise exception 'Assigned driver cannot read guest'; end if; end $$;
select public.advance_ride(current_setting('intu.qa_guest')::uuid,'arrived');
select public.verify_ride_pin(current_setting('intu.qa_guest')::uuid,current_setting('intu.qa_guest_pin'));
select public.advance_ride(current_setting('intu.qa_guest')::uuid,'in_progress');
select public.advance_ride(current_setting('intu.qa_guest')::uuid,'completed');
select set_config('request.jwt.claims','{"sub":"intu-qa-contact-20261005-booker","role":"authenticated"}',true);
do $$ begin if not exists(select 1 from public.ride_passengers where name='Ana QA') then raise exception 'History lost contact'; end if; end $$;
select set_config('intu.qa_contact_delivery',(public.create_contact_delivery_request(-11.252,-74.637,'Recojo de Luis',-11.25,-74.63,'Destino de Ana',2100,420,null,'efectivo',
  '{"sender_name":"Luis QA","sender_phone":"+51988888888","recipient_name":"Ana QA","recipient_phone":"+51987654321","description":"Documentos QA","payer":"recipient","small_package_confirmed":true}')).id::text,true);
do $$ begin
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_contact_delivery')::uuid and rider_id='intu-qa-contact-20261005-booker' and service_kind='delivery' and estimated_fare=4.2) then raise exception 'Delegated courier owner or fare'; end if;
  if not exists(select 1 from public.delivery_details where ride_id=current_setting('intu.qa_contact_delivery')::uuid and sender_name='Luis QA' and recipient_name='Ana QA') then raise exception 'Sender/receiver mixed'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-contact-20261005-courier","role":"authenticated"}',true);
do $$ begin if exists(select 1 from public.delivery_details) then raise exception 'Unassigned courier sees contacts'; end if; end $$;
select public.accept_ride(current_setting('intu.qa_contact_delivery')::uuid);
do $$ begin if not exists(select 1 from public.delivery_details where sender_phone='+51988888888' and recipient_phone='+51987654321') then raise exception 'Assigned courier cannot read contacts'; end if; end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-contact-20261005-booker","role":"authenticated"}',true);
select public.cancel_ride(current_setting('intu.qa_contact_delivery')::uuid,'QA cancelled by booker');
select set_config('request.jwt.claims','{"role":"authenticated"}',true);
do $$ begin
  begin perform public.create_guest_ride_request(-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420,null,'efectivo',null,'{"name":"Ana QA","phone":"+51987654321"}'); raise exception 'Unauthenticated guest accepted'; exception when invalid_authorization_specification then null; end;
end $$;
rollback;
select 'third-party ownership, privacy, fares, PIN, lifecycle and contacts passed' as result;
