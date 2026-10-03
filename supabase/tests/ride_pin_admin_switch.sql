-- Run as postgres against the development project. All fixtures/preferences roll back.
begin;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-pin-20261003-%') then raise exception 'Fixture collision'; end if;
  if (select pin_enabled from private.ride_security_settings where singleton) then raise exception 'Expected default off'; end if;
  if has_function_privilege('anon','public.admin_set_ride_security_settings(boolean)','execute')
     or has_function_privilege('anon','public.admin_get_ride_security_settings()','execute')
     or has_function_privilege('anon','public.ride_pin_requirement(uuid)','execute') then raise exception 'Anonymous access'; end if;
  if has_column_privilege('authenticated','public.rides','start_pin_required','insert')
     or has_column_privilege('authenticated','public.rides','start_pin_required','update')
     or has_table_privilege('authenticated','private.ride_security_settings','update') then raise exception 'Direct write access'; end if;
end $$;
insert into public.profiles(id,first_name,last_name)
select 'intu-qa-pin-20261003-' || name, 'QA', name from unnest(array['admin','rider1','rider2','rider3','sender','driver','courier']) name;
insert into private.admins(user_id) values ('intu-qa-pin-20261003-admin');
insert into public.drivers(id,document_type,document_number,license_number,status)
values ('intu-qa-pin-20261003-driver','dni','99161031','QA','approved'),
       ('intu-qa-pin-20261003-courier','dni','99161032','QA','approved');
insert into public.vehicles(driver_id,vehicle_type,brand,model,year,plate)
values ('intu-qa-pin-20261003-driver','mototaxi','QA','QA',2024,'QAPIN01'),
       ('intu-qa-pin-20261003-courier','motorcycle','QA','QA',2024,'QAPIN02');
update public.vehicle_types set is_active = true where code = 'motorcycle';
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-pin-20261003-rider1","role":"authenticated"}',true);
do $$ begin
  begin perform public.admin_get_ride_security_settings(); raise exception 'Non-admin read settings';
  exception when insufficient_privilege then null; end;
  begin perform public.admin_set_ride_security_settings(true); raise exception 'Non-admin changed settings';
  exception when insufficient_privilege then null; end;
end $$;
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id
) select set_config('intu.qa_pin_off',id::text,true) from created;
do $$ begin
  if (public.ride_pin_requirement(current_setting('intu.qa_pin_off')::uuid)->>'required')::boolean then raise exception 'Default PIN required'; end if;
  if exists(select 1 from public.ride_start_pin(current_setting('intu.qa_pin_off')::uuid)) then raise exception 'Disabled PIN visible'; end if;
end $$;
reset role;
update public.rides set driver_id = 'intu-qa-pin-20261003-driver', status = 'arrived' where id = current_setting('intu.qa_pin_off')::uuid;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-pin-20261003-driver","role":"authenticated"}',true);
do $$ begin
  if (public.ride_pin_requirement(current_setting('intu.qa_pin_off')::uuid)->>'required')::boolean then raise exception 'Driver PIN requirement mismatch'; end if;
end $$;
select public.advance_ride(current_setting('intu.qa_pin_off')::uuid,'in_progress');
select public.advance_ride(current_setting('intu.qa_pin_off')::uuid,'completed');
select set_config('request.jwt.claims','{"sub":"intu-qa-pin-20261003-admin","role":"authenticated"}',true);
do $$ begin
  if (public.admin_get_ride_security_settings()->>'pin_enabled')::boolean then raise exception 'Wrong initial setting'; end if;
  if not (public.admin_set_ride_security_settings(true)->>'pin_enabled')::boolean then raise exception 'Admin enable failed'; end if;
  begin perform public.admin_set_ride_security_settings(null); raise exception 'Null preference accepted';
  exception when invalid_parameter_value then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-pin-20261003-rider2","role":"authenticated"}',true);
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id
) select set_config('intu.qa_pin_on',id::text,true) from created;
select set_config('intu.qa_pin_value',(select pin from public.ride_start_pin(current_setting('intu.qa_pin_on')::uuid)),true);
do $$ begin
  if current_setting('intu.qa_pin_value') !~ '^[0-9]{4}$' then raise exception 'Missing PIN'; end if;
end $$;
-- Turning off preserves the enabled ride, but a subsequent request must be off.
select set_config('request.jwt.claims','{"sub":"intu-qa-pin-20261003-admin","role":"authenticated"}',true);
do $$ begin
  if (public.admin_set_ride_security_settings(false)->>'pin_enabled')::boolean then raise exception 'Admin disable failed'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-pin-20261003-rider3","role":"authenticated"}',true);
do $$ begin
  begin perform public.ride_pin_requirement(current_setting('intu.qa_pin_on')::uuid); raise exception 'Other rider read requirement';
  exception when no_data_found then null; end;
  if exists(select 1 from public.ride_start_pin(current_setting('intu.qa_pin_on')::uuid)) then raise exception 'Other rider read PIN'; end if;
end $$;
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id
) select set_config('intu.qa_pin_next',id::text,true) from created;
do $$ begin
  if (public.ride_pin_requirement(current_setting('intu.qa_pin_next')::uuid)->>'required')::boolean then raise exception 'Disable ignored'; end if;
end $$;
reset role;
do $$ begin
  if not (select start_pin_required from public.rides where id = current_setting('intu.qa_pin_on')::uuid) then raise exception 'Existing requirement changed'; end if;
  if (select start_pin_required from public.rides where id = current_setting('intu.qa_pin_off')::uuid) then raise exception 'Existing disabled requirement changed'; end if;
  if (select updated_by from private.ride_security_settings where singleton) <> 'intu-qa-pin-20261003-admin' then raise exception 'Audit missing'; end if;
end $$;
update public.rides set driver_id = 'intu-qa-pin-20261003-driver', status = 'arrived' where id = current_setting('intu.qa_pin_on')::uuid;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-pin-20261003-driver","role":"authenticated"}',true);
do $$ declare result record; begin
  if not (public.ride_pin_requirement(current_setting('intu.qa_pin_on')::uuid)->>'required')::boolean then raise exception 'Re-enabled ride bypass'; end if;
  begin perform public.advance_ride(current_setting('intu.qa_pin_on')::uuid,'in_progress'); raise exception 'Started without PIN';
  exception when raise_exception then if sqlerrm <> 'pin_required' then raise; end if; end;
  select * into result from public.verify_ride_pin(current_setting('intu.qa_pin_on')::uuid,
    case current_setting('intu.qa_pin_value') when '0000' then '0001' else '0000' end);
  if result.verified or result.attempts_left <> 4 then raise exception 'Wrong PIN accepted or not counted'; end if;
  select * into result from public.verify_ride_pin(current_setting('intu.qa_pin_on')::uuid,current_setting('intu.qa_pin_value'));
  if not result.verified then raise exception 'Correct PIN rejected'; end if;
end $$;
select public.advance_ride(current_setting('intu.qa_pin_on')::uuid,'in_progress');
select public.advance_ride(current_setting('intu.qa_pin_on')::uuid,'completed');
-- Disabled PIN must not bypass sender payment or completion rules.
select set_config('request.jwt.claims','{"sub":"intu-qa-pin-20261003-sender","role":"authenticated"}',true);
select set_config('intu.qa_pin_delivery',(public.create_delivery_request(-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420,null,'efectivo',
  '{"recipient_name":"Persona QA","recipient_phone":"+51987654321","description":"Sobre QA","payer":"sender","small_package_confirmed":true}')).id::text,true);
select set_config('request.jwt.claims','{"sub":"intu-qa-pin-20261003-courier","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_pin_delivery')::uuid);
select public.advance_ride(current_setting('intu.qa_pin_delivery')::uuid,'arrived');
do $$ begin
  if (public.ride_pin_requirement(current_setting('intu.qa_pin_delivery')::uuid)->>'required')::boolean then raise exception 'Delivery off ignored'; end if;
  begin perform public.advance_ride(current_setting('intu.qa_pin_delivery')::uuid,'in_progress'); raise exception 'Unpaid pickup accepted';
  exception when raise_exception then if sqlerrm <> 'delivery_payment_required' then raise; end if; end;
end $$;
select public.confirm_delivery_payment(current_setting('intu.qa_pin_delivery')::uuid);
select public.advance_ride(current_setting('intu.qa_pin_delivery')::uuid,'in_progress');
select public.advance_ride(current_setting('intu.qa_pin_delivery')::uuid,'completed');
set constraints all immediate;
reset role;
rollback;
select 'PIN default off/admin-only/reactivation/immutable requests/payment rules: PASS' as verification;
