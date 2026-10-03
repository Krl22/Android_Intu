-- Isolated Firebase-style UID fixtures. All accounts, approvals and rides roll back.
begin;
-- Exercise the disabled-service guard independently of the deployed activation flag.
update public.vehicle_types set is_active = false where code = 'motorcycle';
do $$ begin
  if exists (select 1 from public.profiles where id like 'intu-qa-vehicle-20261002-%') then
    raise exception 'QA fixture namespace already exists';
  end if;
  if has_function_privilege('anon', 'public.submit_driver_application(text,text,text,text,text,integer,text)', 'execute') then
    raise exception 'Anonymous application endpoint is executable';
  end if;
  if exists (select 1 from pg_proc p join pg_namespace n on n.oid = p.pronamespace
               where n.nspname = 'public' and p.proname = 'submit_driver_application' and p.prosecdef) then
    raise exception 'Submission must use ownership RLS';
  end if;
end $$;
insert into public.profiles (id, first_name, last_name)
  select 'intu-qa-vehicle-20261002-' || name, 'QA', name
  from unnest(array['courier','taxi','rider','duplicate','admin']) name;
insert into private.admins (user_id) values ('intu-qa-vehicle-20261002-admin');
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"intu-qa-vehicle-20261002-courier","role":"authenticated"}', true);
select public.submit_driver_application('99161001','QA-Courier','motorcycle','Honda','Wave 125',2024,' qa mc 102 ');
do $$ begin
  if not exists(select 1 from public.drivers where status = 'pending')
     or not exists(select 1 from public.vehicles where vehicle_type = 'motorcycle' and plate = 'QAMC102') then
    raise exception 'Courier application lost its type or pending state';
  end if;
  if private.current_driver_vehicle_type() is not null then raise exception 'Pending courier can drive'; end if;
  begin
    perform public.set_driver_location(-11.252,-74.637,null,true);
    raise exception 'Pending courier published availability';
  exception when insufficient_privilege then null;
  end;
end $$;
select set_config('request.jwt.claims', '{"sub":"intu-qa-vehicle-20261002-taxi","role":"authenticated"}', true);
select public.submit_driver_application('99161002','QA-Taxi','mototaxi','Bajaj','RE',2024,'QATX102');
select set_config('request.jwt.claims', '{"sub":"intu-qa-vehicle-20261002-duplicate","role":"authenticated"}', true);
do $$ begin
  begin
    perform public.submit_driver_application('99161003','QA-Duplicate','motorcycle','Honda','Wave 125',2024,'QAMC102');
    raise exception 'Duplicate plate was accepted';
  exception when unique_violation then null;
  end;
  if exists(select 1 from public.drivers) then raise exception 'Failed application left partial driver'; end if;
  begin
    perform public.submit_driver_application('99161001','QA-Duplicate','motorcycle','Honda','Wave 125',2024,'QADU102');
    raise exception 'Duplicate DNI was accepted';
  exception when unique_violation then null;
  end;
  if exists(select 1 from public.drivers) then raise exception 'Failed DNI left partial driver'; end if;
  begin
    perform public.submit_driver_application('99161003','QA-Duplicate','car','Honda','Wave 125',2024,'QADU102');
    raise exception 'Unsupported vehicle was accepted';
  exception when invalid_parameter_value then null;
  end;
  begin
    perform public.admin_set_driver_status('intu-qa-vehicle-20261002-courier','approved');
    raise exception 'Ordinary user approved a courier';
  exception when insufficient_privilege then null;
  end;
end $$;
select set_config('request.jwt.claims', '{"sub":"intu-qa-vehicle-20261002-admin","role":"authenticated"}', true);
do $$ begin
  if not exists (select 1 from public.admin_list_drivers('pending')
                 where id = 'intu-qa-vehicle-20261002-courier' and vehicle_type = 'motorcycle') then
    raise exception 'Admin cannot identify courier request';
  end if;
end $$;
select public.admin_set_driver_status('intu-qa-vehicle-20261002-courier','approved');
select public.admin_set_driver_status('intu-qa-vehicle-20261002-taxi','approved');
select set_config('request.jwt.claims', '{"sub":"intu-qa-vehicle-20261002-courier","role":"authenticated"}', true);
do $$ begin
  if private.current_driver_vehicle_type() is not null then raise exception 'Disabled courier service can drive'; end if;
  begin
    perform public.set_driver_location(-11.252,-74.637,null,true);
    raise exception 'Disabled courier published availability';
  exception when insufficient_privilege then null;
  end;
  begin
    perform public.accept_ride(gen_random_uuid());
    raise exception 'Disabled courier accepted a request';
  exception when insufficient_privilege then null;
  end;
end $$;
-- A stale availability row must not surface an inactive courier on the passenger map.
reset role;
insert into public.driver_locations (driver_id, latitude, longitude, is_available)
  values ('intu-qa-vehicle-20261002-courier', -11.252, -74.637, true);
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"intu-qa-vehicle-20261002-taxi","role":"authenticated"}', true);
do $$ begin
  if exists(select 1 from public.nearby_drivers(-11.252,-74.637,3000,null,50)
             where driver_id = 'intu-qa-vehicle-20261002-courier') then
    raise exception 'Inactive courier appears on nearby passenger map';
  end if;
end $$;
select set_config('request.jwt.claims', '{"sub":"intu-qa-vehicle-20261002-rider","role":"authenticated"}', true);
insert into public.rides (vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,
  destination_address,distance_meters,duration_seconds)
values ('mototaxi',-11.252,-74.637,'QA Origen',-11.25,-74.63,'QA Destino',1000,300);
select set_config('intu.qa_ride_id',(select id::text from public.rides limit 1),true);
do $$ begin
  begin
    insert into public.rides (vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,
      destination_address,distance_meters,duration_seconds)
    values ('motorcycle',-11.252,-74.637,'QA Origen',-11.25,-74.63,'QA Destino',1000,300);
    raise exception 'Disabled courier can be ordered';
  exception when invalid_parameter_value then null;
  end;
end $$;
-- Simulate the future service switch inside this transaction to test type isolation independently.
reset role;
update public.vehicle_types set is_active = true where code = 'motorcycle';
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"intu-qa-vehicle-20261002-courier","role":"authenticated"}', true);
do $$ begin
  if private.current_driver_vehicle_type() <> 'motorcycle' then raise exception 'Courier resolved as mototaxi'; end if;
  if exists(select 1 from public.rides where id = current_setting('intu.qa_ride_id')::uuid) then
    raise exception 'Courier sees passenger request';
  end if;
  begin
    perform public.accept_ride(current_setting('intu.qa_ride_id')::uuid);
    raise exception 'Courier accepted passenger request';
  exception when no_data_found then null;
  end;
end $$;
select public.set_driver_location(-11.252,-74.637,null,true);
update public.profiles set driver_mode = true;
select public.submit_driver_application('99161001','QA-Courier','motorcycle','Honda','Wave 125',2024,'QAMC102');
do $$ begin
  if not exists(select 1 from public.drivers where status = 'approved') then
    raise exception 'Identical retry removed approval';
  end if;
end $$;
select public.submit_driver_application('99161001','QA-Courier','mototaxi','Bajaj','RE',2024,'QAMC102');
do $$ begin
  if not exists(select 1 from public.drivers where status = 'pending' and approved_at is null)
     or exists(select 1 from public.profiles where driver_mode)
     or exists(select 1 from public.driver_locations where is_available)
     or private.current_driver_vehicle_type() is not null then
    raise exception 'Changing vehicle bypassed approval';
  end if;
  begin
    update public.drivers set status = 'approved';
    raise exception 'User can self approve';
  exception when insufficient_privilege then null;
  end;
end $$;
select set_config('request.jwt.claims', '{"sub":"intu-qa-vehicle-20261002-taxi","role":"authenticated"}', true);
select public.set_driver_location(-11.252,-74.637,null,true);
select public.accept_ride(current_setting('intu.qa_ride_id')::uuid);
do $$ begin
  if not exists(select 1 from public.rides where id = current_setting('intu.qa_ride_id')::uuid and status = 'accepted') then
    raise exception 'Mototaxi registration/acceptance regressed';
  end if;
  begin
    perform public.submit_driver_application('99161002','QA-Taxi','motorcycle','Honda','Wave 125',2024,'QATX102');
    raise exception 'Vehicle type changed while picking up passenger';
  exception when raise_exception then
    if sqlerrm <> 'vehicle_change_during_ride' then raise; end if;
  end;
end $$;
reset role;
rollback;
select 'courier registration, atomic retries, admin approval, RLS separation, reapproval and mototaxi acceptance: PASS' as verification;
