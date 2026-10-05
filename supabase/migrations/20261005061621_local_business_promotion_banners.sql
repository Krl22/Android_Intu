-- Commercial demo banners and atomic, editable menus. Merchandise totals never enter transport fares.
create function private.normalize_business_menu(p_menu jsonb) returns jsonb
language plpgsql immutable set search_path='' as $$
declare v_item jsonb; v_result jsonb:='[]'; v_ids uuid[]:='{}'; v_id uuid; v_price numeric; v_photo text; v_url text;
begin
  if p_menu is null or jsonb_typeof(p_menu)<>'array' or jsonb_array_length(p_menu)>12 then
    raise exception 'invalid_business_menu' using errcode='22023';
  end if;
  for v_item in select value from jsonb_array_elements(p_menu) loop
    if jsonb_typeof(v_item)<>'object' or jsonb_typeof(v_item->'id') is distinct from 'string'
      or jsonb_typeof(v_item->'name') is distinct from 'string'
      or jsonb_typeof(v_item->'price') is distinct from 'number'
      or jsonb_typeof(v_item->'available') is distinct from 'boolean' then
      raise exception 'invalid_business_menu' using errcode='22023';
    end if;
    v_id:=(v_item->>'id')::uuid;
    v_price:=(v_item->>'price')::numeric;
    v_photo:=coalesce(v_item->>'demo_photo','');
    v_url:=trim(coalesce(v_item->>'image_url',''));
    if v_id=any(v_ids) or length(trim(v_item->>'name')) not between 2 and 80
      or length(coalesce(v_item->>'description',''))>160
      or v_price not between 0.10 and 999.99 or v_price<>round(v_price,2)
      or v_photo not in ('','chicken','juane','coffee','chaufa')
      or length(v_url)>1000 or (v_url<>'' and v_url !~ '^https://[^/@[:space:]]+([/?#][^[:space:]]*)?$') then
      raise exception 'invalid_business_menu' using errcode='22023';
    end if;
    v_ids:=array_append(v_ids,v_id);
    v_result:=v_result||jsonb_build_array(jsonb_build_object('id',v_id,'name',trim(v_item->>'name'),
      'description',trim(coalesce(v_item->>'description','')),'price',v_price,'demo_photo',v_photo,
      'image_url',v_url,'available',(v_item->>'available')::boolean));
  end loop;
  return v_result;
end $$;
revoke all on function private.normalize_business_menu(jsonb) from public,anon,authenticated;
alter table private.business_ads
  add column city text not null default '' check(length(city)<=50),
  add column offer_detail text not null default '' check(length(offer_detail)<=100),
  add column offer_price numeric(5,2) check(offer_price between 0.10 and 999.99),
  add column demo_photo text not null default '' check(demo_photo in ('','chicken','juane','coffee','chaufa')),
  add column menu_items jsonb not null default '[]' check(menu_items=private.normalize_business_menu(menu_items));
alter table public.delivery_details
  add column business_order_items jsonb not null default '[]' check(jsonb_typeof(business_order_items)='array'),
  add column business_products_total numeric(7,2) not null default 0 check(business_products_total between 0 and 19999.80);
grant select(business_order_items,business_products_total) on public.delivery_details to authenticated;

create or replace function private.admin_save_business_ad(p_id uuid,p_ad jsonb,p_updated_at timestamptz) returns jsonb
language plpgsql security definer set search_path='' as $$
declare v_menu jsonb;
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  if p_ad is null or jsonb_typeof(p_ad)<>'object' then raise exception 'invalid_business_ad' using errcode='22023'; end if;
  if p_ad ? 'menu_items' then v_menu:=private.normalize_business_menu(p_ad->'menu_items'); end if;
  if p_ad ? 'offer_price' and p_ad->>'offer_price' is not null and
    ((p_ad->>'offer_price')::numeric not between .10 and 999.99 or
      (p_ad->>'offer_price')::numeric<>round((p_ad->>'offer_price')::numeric,2)) then
    raise exception 'invalid_business_ad' using errcode='22023';
  end if;
  if p_id is null then
    insert into private.business_ads(name,category,title,description,image_url,address,lat,lng,published,sort_order,
      city,offer_detail,offer_price,demo_photo,menu_items)
    values(trim(p_ad->>'name'),p_ad->>'category',trim(p_ad->>'title'),trim(p_ad->>'description'),
      nullif(trim(p_ad->>'image_url'),''),trim(p_ad->>'address'),(p_ad->>'lat')::float8,(p_ad->>'lng')::float8,
      (p_ad->>'published')::boolean,(p_ad->>'sort_order')::int,trim(coalesce(p_ad->>'city','')),
      trim(coalesce(p_ad->>'offer_detail','')),(p_ad->>'offer_price')::numeric,coalesce(p_ad->>'demo_photo',''),coalesce(v_menu,'[]'));
  else
    update private.business_ads set name=trim(p_ad->>'name'),category=p_ad->>'category',title=trim(p_ad->>'title'),
      description=trim(p_ad->>'description'),image_url=nullif(trim(p_ad->>'image_url'),''),address=trim(p_ad->>'address'),
      lat=(p_ad->>'lat')::float8,lng=(p_ad->>'lng')::float8,published=(p_ad->>'published')::boolean,
      sort_order=(p_ad->>'sort_order')::int,updated_at=clock_timestamp(),
      city=case when p_ad ? 'city' then trim(p_ad->>'city') else city end,
      offer_detail=case when p_ad ? 'offer_detail' then trim(p_ad->>'offer_detail') else offer_detail end,
      offer_price=case when p_ad ? 'offer_price' then (p_ad->>'offer_price')::numeric else offer_price end,
      demo_photo=case when p_ad ? 'demo_photo' then p_ad->>'demo_photo' else demo_photo end,
      menu_items=coalesce(v_menu,menu_items)
    where id=p_id and archived_at is null and updated_at=p_updated_at;
    if not found then raise exception 'business_ad_changed' using errcode='P0001'; end if;
  end if;
  return private.admin_business_state();
end $$;

create or replace function private.create_business_delivery_request(p_ad_id uuid,p_destination_lat float8,p_destination_lng float8,
  p_destination_address text,p_distance_meters int,p_duration_seconds int,p_route_polyline text,p_payment_method text,p_details jsonb)
returns public.rides language plpgsql security definer set search_path = '' as $$
declare v_ad private.business_ads; v_ride public.rides; v_enabled boolean; v_uid text:=private.requesting_uid();
  v_selected jsonb; v_item jsonb; v_product jsonb; v_items jsonb:='[]'; v_ids uuid[]:='{}';
  v_id uuid; v_quantity int; v_count int:=0; v_total numeric:=0;
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
  v_selected:=coalesce(p_details->'business_order_items','[]');
  if jsonb_typeof(v_selected)<>'array' or jsonb_array_length(v_selected)>12
    or (jsonb_array_length(v_ad.menu_items)>0 and jsonb_array_length(v_selected)=0) then
    raise exception 'invalid_business_order' using errcode='22023';
  end if;
  for v_item in select value from jsonb_array_elements(v_selected) loop
    if jsonb_typeof(v_item)<>'object' or jsonb_typeof(v_item->'item_id') is distinct from 'string'
      or jsonb_typeof(v_item->'quantity') is distinct from 'number' or (v_item->>'quantity')!~ '^[0-9]+$' then
      raise exception 'invalid_business_order' using errcode='22023';
    end if;
    v_id:=(v_item->>'item_id')::uuid; v_quantity:=(v_item->>'quantity')::int;
    select value into v_product from jsonb_array_elements(v_ad.menu_items) where value->>'id'=v_id::text;
    if not found or (v_product->>'available')::boolean is not true or v_id=any(v_ids) or v_quantity not between 1 and 10 then
      raise exception 'invalid_business_order' using errcode='22023';
    end if;
    v_count:=v_count+v_quantity;
    if v_count>20 then raise exception 'invalid_business_order' using errcode='22023'; end if;
    v_ids:=array_append(v_ids,v_id);
    v_total:=v_total+(v_product->>'price')::numeric*v_quantity;
    v_items:=v_items||jsonb_build_array(jsonb_build_object('item_id',v_id,'name',v_product->>'name',
      'unit_price',(v_product->>'price')::numeric,'quantity',v_quantity));
  end loop;
  insert into public.rides(business_ad_id,vehicle_type,origin_lat,origin_lng,origin_address,
    destination_lat,destination_lng,destination_address,distance_meters,duration_seconds,route_polyline,payment_method)
  values(v_ad.id,'motorcycle',v_ad.lat,v_ad.lng,'[DEMO] '||v_ad.name||' · '||v_ad.address,
    p_destination_lat,p_destination_lng,p_destination_address,p_distance_meters,p_duration_seconds,p_route_polyline,p_payment_method)
  returning * into v_ride;
  insert into public.delivery_details(ride_id,recipient_name,recipient_phone,description,pickup_reference,
    delivery_reference,payer,small_package_confirmed,business_name,business_order_items,business_products_total)
  values(v_ride.id,trim(p_details->>'recipient_name'),p_details->>'recipient_phone',trim(p_details->>'description'),
    trim(coalesce(p_details->>'pickup_reference','')),trim(coalesce(p_details->>'delivery_reference','')),'recipient',true,v_ad.name,v_items,v_total);
  return v_ride;
end $$;

-- Replace ONLY untouched original fixtures, preserving administrator edits and the master switch.
update private.business_ads set name='Brasa Satipo · Demo',title='1/4 de pollo + papas',
  description='Sabor a la brasa para tu almuerzo en Satipo. Menú ficticio para probar pedidos en moto.',
  city='Satipo',offer_detail='Papas, ensalada y ají',offer_price=18.90,demo_photo='chicken',
  address='Centro de Satipo · punto demo por verificar',lat=-11.253056,lng=-74.637222,updated_at=clock_timestamp(),
  menu_items=private.normalize_business_menu(jsonb_build_array(
    jsonb_build_object('id',gen_random_uuid(),'name','1/4 de pollo + papas','description','Papas, ensalada fresca y ají.','price',18.90,'demo_photo','chicken','available',true),
    jsonb_build_object('id',gen_random_uuid(),'name','Chaufa de pollo','description','Arroz salteado con pollo, huevo y cebolla china.','price',15.90,'demo_photo','chaufa','available',true)))
where name='Cocina Demo' and title='Algo rico, cerca de ti' and description='Prueba el envío de un pedido pequeño desde un negocio ficticio.'
  and address='Punto demo · Satipo (editar antes de probar)' and image_url is null and archived_at is null and menu_items='[]';
update private.business_ads set name='Sazón Río Negro · Demo',category='food',title='Juane tradicional',
  description='Un clásico de la selva para tu día en Río Negro. Negocio y menú ficticios para el MVP.',
  city='Río Negro',offer_detail='Pollo, arroz y sabor de la selva',offer_price=14.90,demo_photo='juane',
  address='Centro de Río Negro · punto demo por verificar',lat=-11.208611,lng=-74.659722,updated_at=clock_timestamp(),
  menu_items=private.normalize_business_menu(jsonb_build_array(
    jsonb_build_object('id',gen_random_uuid(),'name','Juane tradicional','description','Arroz sazonado, pollo, huevo y aceituna en hoja de bijao.','price',14.90,'demo_photo','juane','available',true),
    jsonb_build_object('id',gen_random_uuid(),'name','Juane + refresco','description','Juane tradicional con refresco de temporada de 350 ml.','price',17.90,'demo_photo','juane','available',true)))
where name='Tienda Demo' and title='Lo que necesitas, a domicilio' and description='Descubre cómo se verá la publicidad y prueba un delivery en moto.'
  and address='Punto demo · Satipo (editar antes de probar)' and image_url is null and archived_at is null and menu_items='[]';
insert into private.business_ads(name,category,title,description,address,lat,lng,published,sort_order,city,offer_detail,offer_price,demo_photo,menu_items)
select 'Café Satipo · Demo','food','Café + sánguche','Una pausa con café y un sánguche recién preparado. Menú ficticio de Satipo para pruebas.',
  'Centro de Satipo · punto demo por verificar',-11.253056,-74.637222,true,2,'Satipo','Tu pausa de media mañana',9.90,'coffee',
  private.normalize_business_menu(jsonb_build_array(
    jsonb_build_object('id',gen_random_uuid(),'name','Café + sánguche de pollo','description','Café americano de 250 ml y pan con pollo y lechuga.','price',9.90,'demo_photo','coffee','available',true),
    jsonb_build_object('id',gen_random_uuid(),'name','Combo para dos','description','Dos cafés americanos y dos sánguches de pollo.','price',18.90,'demo_photo','coffee','available',true)))
where not exists(select 1 from private.business_ads where name='Café Satipo · Demo' and archived_at is null);
