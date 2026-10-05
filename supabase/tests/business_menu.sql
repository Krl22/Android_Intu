-- Canonical menu/prices, quantities, versions and history. Every fixture rolls back.
begin;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-menu-20261005-%') then raise exception 'Fixture collision'; end if;
  if has_column_privilege('authenticated','public.delivery_details','business_order_items','insert')
    or has_column_privilege('authenticated','public.delivery_details','business_order_items','update')
    or has_column_privilege('authenticated','public.delivery_details','business_products_total','insert')
    or has_column_privilege('authenticated','public.delivery_details','business_products_total','update') then raise exception 'Unsafe menu grants'; end if;
end $$;
insert into public.profiles(id,first_name,last_name)
select 'intu-qa-menu-20261005-'||name,'QA',name from unnest(array['admin','rider','courier']) name;
insert into private.admins(user_id) values('intu-qa-menu-20261005-admin');
insert into public.drivers(id,document_type,document_number,license_number,status)
values('intu-qa-menu-20261005-courier','dni','99161061','QA','approved');
insert into public.vehicles(driver_id,vehicle_type,brand,model,year,plate)
values('intu-qa-menu-20261005-courier','motorcycle','QA','QA',2024,'QAMENU1');
insert into private.business_test_couriers(driver_id) values('intu-qa-menu-20261005-courier');
update public.vehicle_types set is_active=true where code='motorcycle';
update private.ride_security_settings set pin_enabled=false;
update private.business_delivery_settings set enabled=true;
do $$ declare v_ad private.business_ads;
begin
  insert into private.business_ads(name,category,title,description,address,lat,lng,published,menu_items)
  values('QA Menú','food','QA promoción','Menú ficticio QA','Recojo QA',-11.2521,-74.6382,true,
    private.normalize_business_menu(jsonb_build_array(
      jsonb_build_object('id',gen_random_uuid(),'name','Pollo','price',18.90,'available',true,'demo_photo','chicken'),
      jsonb_build_object('id',gen_random_uuid(),'name','Juane','price',14.90,'available',true,'demo_photo','juane'),
      jsonb_build_object('id',gen_random_uuid(),'name','Café','price',9.90,'available',true,'demo_photo','coffee'),
      jsonb_build_object('id',gen_random_uuid(),'name','Chaufa','price',15.90,'available',false,'demo_photo','chaufa'))))
    returning * into v_ad;
  perform set_config('intu.menu_ad',v_ad.id::text,true);
  perform set_config('intu.menu_version',v_ad.updated_at::text,true);
  perform set_config('intu.menu_items',v_ad.menu_items::text,true);
end $$;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-menu-20261005-admin","role":"authenticated"}',true);
do $$ declare v_ad jsonb; v_invalid jsonb; v_menu jsonb:=current_setting('intu.menu_items')::jsonb;
begin
  select value into v_ad from jsonb_array_elements(public.admin_business_state()->'ads')
    where value->>'id'=current_setting('intu.menu_ad');
  foreach v_invalid in array array[
    jsonb_set(v_menu,'{0,price}','1.001'),jsonb_set(v_menu,'{0,price}','0'),
    jsonb_set(v_menu,'{0,image_url}','"http://example.com/image.png"'),
    jsonb_set(v_menu,'{0,demo_photo}','"unknown"'),
    jsonb_set(v_menu,'{1,id}',v_menu->0->'id')]
  loop
    begin perform public.admin_save_business_ad(current_setting('intu.menu_ad')::uuid,jsonb_set(v_ad,'{menu_items}',v_invalid),current_setting('intu.menu_version')::timestamptz);
      raise exception 'Invalid menu saved'; exception when invalid_parameter_value then null; end;
  end loop;
  -- An older administrator client must preserve fields it cannot yet edit.
  perform public.admin_save_business_ad(current_setting('intu.menu_ad')::uuid,
    v_ad-'menu_items'-'city'-'offer_detail'-'offer_price'-'demo_photo',current_setting('intu.menu_version')::timestamptz);
  select value into v_ad from jsonb_array_elements(public.admin_business_state()->'ads')
    where value->>'id'=current_setting('intu.menu_ad');
  if v_ad->'menu_items'<>v_menu then raise exception 'Legacy edit lost menu'; end if;
  perform set_config('intu.menu_version',v_ad->>'updated_at',true);
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-menu-20261005-rider","role":"authenticated"}',true);
do $$ declare v_details jsonb; v_bad jsonb; v_menu jsonb:=current_setting('intu.menu_items')::jsonb; v_ride public.rides;
begin
  v_details:=jsonb_build_object('recipient_name','Persona QA','recipient_phone','+51987654321','description','Pedido demo',
    'payer','recipient','small_package_confirmed',true,'business_ad_updated_at',current_setting('intu.menu_version'));
  foreach v_bad in array array[
    '[]'::jsonb,
    jsonb_build_array(jsonb_build_object('item_id',gen_random_uuid(),'quantity',1)),
    jsonb_build_array(jsonb_build_object('item_id',v_menu->0->>'id','quantity',0)),
    jsonb_build_array(jsonb_build_object('item_id',v_menu->0->>'id','quantity',11)),
    jsonb_build_array(jsonb_build_object('item_id',v_menu->0->>'id','quantity',1.5)),
    jsonb_build_array(jsonb_build_object('item_id',v_menu->3->>'id','quantity',1)),
    jsonb_build_array(jsonb_build_object('item_id',v_menu->0->>'id','quantity',1),jsonb_build_object('item_id',v_menu->0->>'id','quantity',1)),
    jsonb_build_array(jsonb_build_object('item_id',v_menu->0->>'id','quantity',8),jsonb_build_object('item_id',v_menu->1->>'id','quantity',8),jsonb_build_object('item_id',v_menu->2->>'id','quantity',8))]
  loop
    begin perform public.create_business_delivery_request(current_setting('intu.menu_ad')::uuid,-11.25,-74.63,'QA',2100,420,null,'efectivo',
      v_details||jsonb_build_object('business_order_items',v_bad));
      raise exception 'Invalid order accepted'; exception when invalid_parameter_value then null; end;
  end loop;
  -- Names and prices supplied by the client cannot affect the stored order or fare.
  v_details:=v_details||jsonb_build_object('business_products_total',.01,'business_order_items',jsonb_build_array(
    jsonb_build_object('item_id',v_menu->0->>'id','quantity',2,'unit_price',.01,'name','Tampered'),
    jsonb_build_object('item_id',v_menu->1->>'id','quantity',1)));
  v_ride:=public.create_business_delivery_request(current_setting('intu.menu_ad')::uuid,-11.25,-74.63,'QA',2100,420,null,'efectivo',v_details);
  perform set_config('intu.menu_order',v_ride.id::text,true);
  if v_ride.estimated_fare<>4.20 then raise exception 'Products affected transport fare'; end if;
  if not exists(select 1 from public.delivery_details where ride_id=v_ride.id and business_products_total=52.70
    and business_order_items->0->>'name'='Pollo' and (business_order_items->0->>'unit_price')::numeric=18.90
    and jsonb_array_length(business_order_items)=2) then raise exception 'Wrong menu snapshot'; end if;
  perform set_config('intu.menu_details',v_details::text,true);
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-menu-20261005-admin","role":"authenticated"}',true);
do $$ declare v_ad jsonb;
begin
  select value into v_ad from jsonb_array_elements(public.admin_business_state()->'ads') where value->>'id'=current_setting('intu.menu_ad');
  perform public.admin_save_business_ad(current_setting('intu.menu_ad')::uuid,
    jsonb_set(jsonb_set(v_ad,'{menu_items,0,price}','20.90'),'{menu_items,0,name}','"Pollo editado"'),current_setting('intu.menu_version')::timestamptz);
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-menu-20261005-rider","role":"authenticated"}',true);
do $$ begin
  begin perform public.create_business_delivery_request(current_setting('intu.menu_ad')::uuid,-11.25,-74.63,'QA',2100,420,null,'efectivo',current_setting('intu.menu_details')::jsonb);
    raise exception 'Stale menu accepted'; exception when raise_exception then if sqlerrm<>'business_ad_unavailable' then raise; end if; end;
  if not exists(select 1 from public.delivery_details where ride_id=current_setting('intu.menu_order')::uuid
    and business_products_total=52.70 and business_order_items->0->>'name'='Pollo') then raise exception 'History changed after menu edit'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-menu-20261005-courier","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.menu_order')::uuid);
do $$ begin
  if not exists(select 1 from public.delivery_details where ride_id=current_setting('intu.menu_order')::uuid
    and business_products_total=52.70 and jsonb_array_length(business_order_items)=2) then raise exception 'Courier cannot read menu'; end if;
end $$;
select public.advance_ride(current_setting('intu.menu_order')::uuid,'arrived');
select public.advance_ride(current_setting('intu.menu_order')::uuid,'in_progress');
select public.confirm_delivery_payment(current_setting('intu.menu_order')::uuid);
select public.advance_ride(current_setting('intu.menu_order')::uuid,'completed');
do $$ begin
  if not exists(select 1 from public.rides where id=current_setting('intu.menu_order')::uuid and final_fare=4.20) then raise exception 'Final fare included products'; end if;
end $$;
rollback;
select 'business_menu PASS (fixtures rolled back)' as result;
