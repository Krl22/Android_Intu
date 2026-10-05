-- Postgres QA: all accounts, orders, announcements and settings roll back.
begin;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-business-20261005-%') then raise exception 'Fixture collision'; end if;
  if has_column_privilege('authenticated','public.rides','business_ad_id','insert')
    or has_column_privilege('authenticated','public.delivery_details','business_name','insert')
    or has_table_privilege('authenticated','private.business_ads','insert')
    or has_function_privilege('anon','public.business_feed()','execute') then raise exception 'Unsafe grants'; end if;
end $$;
insert into public.profiles(id,first_name,last_name)
select 'intu-qa-business-20261005-'||name,'QA',name from unnest(array['admin','rider','other','courier','normal','taxi']) name;
insert into private.admins(user_id) values('intu-qa-business-20261005-admin');
insert into public.drivers(id,document_type,document_number,license_number,status) values
 ('intu-qa-business-20261005-courier','dni','99161051','QA','approved'),
 ('intu-qa-business-20261005-normal','dni','99161052','QA','approved'),
 ('intu-qa-business-20261005-taxi','dni','99161053','QA','approved');
insert into public.vehicles(driver_id,vehicle_type,brand,model,year,plate) values
 ('intu-qa-business-20261005-courier','motorcycle','QA','QA',2024,'QABIZ01'),
 ('intu-qa-business-20261005-normal','motorcycle','QA','QA',2024,'QABIZ02'),
 ('intu-qa-business-20261005-taxi','mototaxi','QA','QA',2024,'QABIZ03');
update public.vehicle_types set is_active=true where code='motorcycle';
update private.ride_security_settings set pin_enabled=false;
update private.business_delivery_settings set enabled=false;
delete from private.business_test_couriers;
-- Isolate feed assertions from any other demo advertisements.
update private.business_ads set published=false;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-rider","role":"authenticated"}',true);
do $$ begin
  begin perform public.admin_business_state(); raise exception 'Non-admin reads admin state'; exception when insufficient_privilege then null; end;
  begin perform public.admin_set_business_enabled(true); raise exception 'Non-admin changes switch'; exception when insufficient_privilege then null; end;
  begin perform public.admin_save_business_ad(null,'{}',null); raise exception 'Non-admin creates ad'; exception when insufficient_privilege then null; end;
  begin perform public.admin_set_business_courier('intu-qa-business-20261005-normal',true); raise exception 'Non-admin selects courier'; exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-admin","role":"authenticated"}',true);
select public.admin_save_business_ad(null,'{"name":"QA Demo","category":"food","title":"QA Anuncio","description":"Negocio ficticio QA","image_url":"","address":"Recojo QA","lat":-11.2521,"lng":-74.6382,"published":true,"sort_order":0}',null);
select set_config('intu.qa_ad',(public.admin_business_state()->'ads'->0->>'id'),true);
select set_config('intu.qa_ad_version',(public.admin_business_state()->'ads'->0->>'updated_at'),true);
-- Only one published fixture, although older ads remain present as drafts.
reset role;
select set_config('intu.qa_ad',(select id::text from private.business_ads where name='QA Demo'),true);
select set_config('intu.qa_ad_version',(select updated_at::text from private.business_ads where id=current_setting('intu.qa_ad')::uuid),true);
set local role authenticated;
do $$ begin
  if (public.business_feed()->>'enabled')::boolean or public.business_feed()->'ads'<>'[]'::jsonb then raise exception 'Off feed exposes ads'; end if;
  begin perform public.admin_set_business_courier('intu-qa-business-20261005-taxi',true); raise exception 'Taxi designated'; exception when invalid_parameter_value then null; end;
end $$;
select public.admin_set_business_enabled(true);
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-rider","role":"authenticated"}',true);
select set_config('intu.qa_details',jsonb_build_object('recipient_name','Persona QA','recipient_phone','+51987654321',
  'description','Paquete demo','payer','recipient','small_package_confirmed',true,'business_ad_updated_at',current_setting('intu.qa_ad_version'))::text,true);
do $$ begin
  if jsonb_array_length(public.business_feed()->'ads')<>1 then raise exception 'Feed filtering failed'; end if;
  begin perform public.create_business_delivery_request(current_setting('intu.qa_ad')::uuid,-11.25,-74.63,'QA Destino',2100,420,null,'efectivo',current_setting('intu.qa_details')::jsonb);
    raise exception 'Order without test courier'; exception when raise_exception then if sqlerrm<>'business_no_test_courier' then raise; end if; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-admin","role":"authenticated"}',true);
select public.admin_set_business_courier('intu-qa-business-20261005-courier',true);
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-rider","role":"authenticated"}',true);
do $$ begin
  begin perform public.create_business_delivery_request(current_setting('intu.qa_ad')::uuid,-11.25,-74.63,'QA',2100,420,null,'efectivo',jsonb_set(current_setting('intu.qa_details')::jsonb,'{payer}','"sender"'));
    raise exception 'Demo charged merchant'; exception when invalid_parameter_value then null; end;
end $$;
select set_config('intu.qa_order',(public.create_business_delivery_request(current_setting('intu.qa_ad')::uuid,-11.25,-74.63,'QA Destino',2100,420,null,'efectivo',current_setting('intu.qa_details')::jsonb)).id::text,true);
do $$ begin
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_order')::uuid and business_ad_id=current_setting('intu.qa_ad')::uuid
    and origin_lat=-11.2521 and origin_lng=-74.6382 and origin_address='[DEMO] QA Demo · Recojo QA'
    and vehicle_type='motorcycle' and estimated_fare=4.20 and not start_pin_required) then raise exception 'Wrong business snapshot or fare'; end if;
  if not exists(select 1 from public.delivery_details where ride_id=current_setting('intu.qa_order')::uuid and business_name='QA Demo') then raise exception 'Missing business detail snapshot'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-normal","role":"authenticated"}',true);
do $$ begin
  if exists(select 1 from public.rides where id=current_setting('intu.qa_order')::uuid) then raise exception 'Normal courier sees demo'; end if;
  if exists(select 1 from public.nearby_ride_requests(-11.2521,-74.6382,20000) where id=current_setting('intu.qa_order')::uuid) then raise exception 'Nearby leaks demo'; end if;
  begin perform public.accept_ride(current_setting('intu.qa_order')::uuid); raise exception 'Normal courier accepted demo'; exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-other","role":"authenticated"}',true);
do $$ begin
  if exists(select 1 from public.rides where id=current_setting('intu.qa_order')::uuid)
    or exists(select 1 from public.delivery_details where ride_id=current_setting('intu.qa_order')::uuid) then raise exception 'Other rider sees order'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-courier","role":"authenticated"}',true);
do $$ begin
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_order')::uuid) then raise exception 'Selected courier cannot see demo'; end if;
  if exists(select 1 from public.delivery_details where ride_id=current_setting('intu.qa_order')::uuid) then raise exception 'Unassigned courier reads contacts'; end if;
end $$;
-- An ad edit invalidates the old quote, without altering the already-created pickup snapshot.
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-admin","role":"authenticated"}',true);
select public.admin_save_business_ad(current_setting('intu.qa_ad')::uuid,
  '{"name":"QA Demo editado","category":"shop","title":"QA anuncio nuevo","description":"Anuncio editado QA","image_url":"","address":"Otro recojo QA","lat":-11.26,"lng":-74.64,"published":true,"sort_order":0}',current_setting('intu.qa_ad_version')::timestamptz);
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-other","role":"authenticated"}',true);
do $$ begin
  begin perform public.create_business_delivery_request(current_setting('intu.qa_ad')::uuid,-11.25,-74.63,'QA',2100,420,null,'efectivo',current_setting('intu.qa_details')::jsonb);
    raise exception 'Stale business quote accepted'; exception when raise_exception then if sqlerrm<>'business_ad_unavailable' then raise; end if; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-admin","role":"authenticated"}',true);
select set_config('intu.qa_ad_version',(select a->>'updated_at' from jsonb_array_elements(public.admin_business_state()->'ads') a
  where a->>'id'=current_setting('intu.qa_ad')),true);
-- Turning off / archiving must prevent new orders while keeping an already requested one operable.
select public.admin_archive_business_ad(current_setting('intu.qa_ad')::uuid,current_setting('intu.qa_ad_version')::timestamptz);
do $$ begin
  begin perform public.admin_archive_business_ad(current_setting('intu.qa_ad')::uuid,current_setting('intu.qa_ad_version')::timestamptz);
    raise exception 'Stale version accepted'; exception when raise_exception then if sqlerrm<>'business_ad_changed' then raise; end if; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-other","role":"authenticated"}',true);
do $$ begin
  begin perform public.create_business_delivery_request(current_setting('intu.qa_ad')::uuid,-11.25,-74.63,'QA',2100,420,null,'efectivo',current_setting('intu.qa_details')::jsonb);
    raise exception 'Archived ad accepts new order'; exception when raise_exception then if sqlerrm<>'business_ad_unavailable' then raise; end if; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-admin","role":"authenticated"}',true);
select public.admin_set_business_enabled(false);
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-other","role":"authenticated"}',true);
do $$ begin
  begin perform public.create_business_delivery_request(current_setting('intu.qa_ad')::uuid,-11.25,-74.63,'QA',2100,420,null,'efectivo',current_setting('intu.qa_details')::jsonb);
    raise exception 'Disabled business order accepted'; exception when raise_exception then if sqlerrm<>'business_delivery_disabled' then raise; end if; end;
end $$;
-- Generic parcels still work while business is disabled.
select set_config('intu.qa_generic',(public.create_delivery_request(-11.2521,-74.6382,'QA',-11.25,-74.63,'QA',0,0,null,'efectivo',current_setting('intu.qa_details')::jsonb)).id::text,true);
select public.cancel_ride(current_setting('intu.qa_generic')::uuid,'QA');
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-courier","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_order')::uuid);
select public.advance_ride(current_setting('intu.qa_order')::uuid,'arrived');
select public.advance_ride(current_setting('intu.qa_order')::uuid,'in_progress');
-- Removal of tester eligibility does not strand a parcel already in custody.
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-admin","role":"authenticated"}',true);
select public.admin_set_business_courier('intu-qa-business-20261005-courier',false);
select set_config('request.jwt.claims','{"sub":"intu-qa-business-20261005-courier","role":"authenticated"}',true);
do $$ begin
  if not exists(select 1 from public.delivery_details where ride_id=current_setting('intu.qa_order')::uuid and business_name='QA Demo') then raise exception 'Assigned courier lost access'; end if;
  begin perform public.advance_ride(current_setting('intu.qa_order')::uuid,'completed'); raise exception 'Unpaid demo completed';
    exception when raise_exception then if sqlerrm<>'delivery_payment_required' then raise; end if; end;
end $$;
select public.confirm_delivery_payment(current_setting('intu.qa_order')::uuid);
select public.advance_ride(current_setting('intu.qa_order')::uuid,'completed');
do $$ begin
  if not exists(select 1 from public.rides where id=current_setting('intu.qa_order')::uuid and status='completed' and final_fare=4.20
    and origin_address='[DEMO] QA Demo · Recojo QA') then raise exception 'Demo lifecycle failed'; end if;
end $$;
rollback;
select 'business_delivery_mvp PASS (fixtures rolled back)' as result;
