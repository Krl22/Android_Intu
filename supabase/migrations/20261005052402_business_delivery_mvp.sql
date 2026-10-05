-- Demo business advertisements share the existing motorcycle delivery lifecycle.
-- Disabled by default; ordinary parcel deliveries are unaffected.
create table private.business_delivery_settings (
  singleton boolean primary key default true check (singleton),
  enabled boolean not null default false
);
insert into private.business_delivery_settings(singleton) values (true);
create table private.business_ads (
  id uuid primary key default gen_random_uuid(),
  name text not null check (length(trim(name)) between 2 and 100),
  category text not null check (category in ('food','shop','pharmacy')),
  title text not null check (length(trim(title)) between 3 and 100),
  description text not null check (length(trim(description)) between 3 and 500),
  image_url text check (image_url is null or (length(image_url) <= 1000 and image_url ~ '^https://[^[:space:]]+$')),
  address text not null check (length(trim(address)) between 3 and 200),
  lat double precision not null check (lat between -90 and 90),
  lng double precision not null check (lng between -180 and 180),
  published boolean not null default false,
  sort_order integer not null default 0 check (sort_order between 0 and 999),
  archived_at timestamptz,
  updated_at timestamptz not null default now()
);
create table private.business_test_couriers (
  driver_id text primary key references public.drivers(id) on delete cascade
);
alter table private.business_delivery_settings enable row level security;
alter table private.business_ads enable row level security;
alter table private.business_test_couriers enable row level security;
revoke all on private.business_delivery_settings, private.business_ads, private.business_test_couriers from public, anon, authenticated;

-- All sample businesses are fictitious. Publishing them still requires the master switch.
insert into private.business_ads(name,category,title,description,address,lat,lng,published,sort_order) values
 ('Cocina Demo','food','Algo rico, cerca de ti','Prueba el envío de un pedido pequeño desde un negocio ficticio.','Punto demo · Satipo (editar antes de probar)',-11.2521,-74.6382,true,0),
 ('Tienda Demo','shop','Lo que necesitas, a domicilio','Descubre cómo se verá la publicidad y prueba un delivery en moto.','Punto demo · Satipo (editar antes de probar)',-11.2505,-74.6365,true,1);

alter table public.rides add column business_ad_id uuid references private.business_ads(id);
create index rides_business_ad_id_idx on public.rides(business_ad_id) where business_ad_id is not null;
alter table public.delivery_details add column business_name text;
grant select(business_ad_id) on public.rides to authenticated;
grant select(business_name) on public.delivery_details to authenticated;

create function private.is_business_test_courier() returns boolean
language sql stable security definer set search_path = '' as $$
  select exists(select 1 from private.business_test_couriers t
    join public.drivers d on d.id=t.driver_id and d.status='approved'
    join public.vehicles v on v.driver_id=d.id and v.is_active and v.vehicle_type='motorcycle'
    where t.driver_id=(select private.requesting_uid()))
$$;
revoke all on function private.is_business_test_courier() from public, anon;
grant execute on function private.is_business_test_courier() to authenticated;
create policy business_demo_visibility on public.rides as restrictive for select to authenticated
  using (business_ad_id is null or rider_id=(select private.requesting_uid())
    or driver_id=(select private.requesting_uid()) or (select private.is_business_test_courier()));

-- accept_ride runs with elevated rights: enforce courier isolation here as well as in RLS.
create function private.guard_business_courier() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  if new.business_ad_id is not null and new.driver_id is not null and new.driver_id is distinct from old.driver_id
     and not exists(select 1 from private.business_test_couriers t
       join public.drivers d on d.id=t.driver_id and d.status='approved'
       join public.vehicles v on v.driver_id=d.id and v.is_active and v.vehicle_type='motorcycle'
       where t.driver_id=new.driver_id) then
    raise exception 'business_test_courier_required' using errcode='42501';
  end if;
  return new;
end $$;
revoke all on function private.guard_business_courier() from public, anon, authenticated;
create trigger rides_guard_business_courier before update of driver_id on public.rides
  for each row execute function private.guard_business_courier();

create function private.business_feed() returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare v_enabled boolean;
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode='28000'; end if;
  select enabled into v_enabled from private.business_delivery_settings where singleton;
  return jsonb_build_object('enabled',v_enabled,'ads',case when v_enabled then
    coalesce((select jsonb_agg(to_jsonb(a) order by a.sort_order,a.name,a.id)
      from private.business_ads a where a.published and a.archived_at is null),'[]'::jsonb)
    else '[]'::jsonb end);
end $$;
create function private.admin_business_state() returns jsonb
language plpgsql stable security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  return jsonb_build_object('enabled',(select enabled from private.business_delivery_settings where singleton),
    'ads',coalesce((select jsonb_agg(to_jsonb(a) order by a.sort_order,a.name,a.id)
      from private.business_ads a where a.archived_at is null),'[]'::jsonb),
    'couriers',coalesce((select jsonb_agg(jsonb_build_object('id',d.id,'name',trim(p.first_name || ' ' || coalesce(p.last_name,'')),
      'plate',v.plate,'selected',t.driver_id is not null) order by p.first_name,d.id)
      from public.drivers d join public.profiles p on p.id=d.id
      join public.vehicles v on v.driver_id=d.id and v.is_active and v.vehicle_type='motorcycle'
      left join private.business_test_couriers t on t.driver_id=d.id where d.status='approved'),'[]'::jsonb));
end $$;
create function private.admin_set_business_enabled(p_enabled boolean) returns jsonb
language plpgsql security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  if p_enabled is null then raise exception 'invalid_business_ad' using errcode='22023'; end if;
  update private.business_delivery_settings set enabled=p_enabled where singleton;
  return private.admin_business_state();
end $$;
create function private.admin_set_business_courier(p_driver_id text,p_selected boolean) returns jsonb
language plpgsql security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  if p_selected is null then raise exception 'invalid_business_ad' using errcode='22023'; end if;
  if p_selected then
    if not exists(select 1 from public.drivers d join public.vehicles v on v.driver_id=d.id and v.is_active
      where d.id=p_driver_id and d.status='approved' and v.vehicle_type='motorcycle') then
      raise exception 'business_test_courier_required' using errcode='22023';
    end if;
    insert into private.business_test_couriers(driver_id) values(p_driver_id) on conflict do nothing;
  else delete from private.business_test_couriers where driver_id=p_driver_id;
  end if;
  return private.admin_business_state();
end $$;
create function private.admin_save_business_ad(p_id uuid,p_ad jsonb,p_updated_at timestamptz) returns jsonb
language plpgsql security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  if p_ad is null or jsonb_typeof(p_ad)<>'object' then raise exception 'invalid_business_ad' using errcode='22023'; end if;
  if p_id is null then
    insert into private.business_ads(name,category,title,description,image_url,address,lat,lng,published,sort_order)
    values(trim(p_ad->>'name'),p_ad->>'category',trim(p_ad->>'title'),trim(p_ad->>'description'),
      nullif(trim(p_ad->>'image_url'),''),trim(p_ad->>'address'),(p_ad->>'lat')::float8,(p_ad->>'lng')::float8,
      (p_ad->>'published')::boolean,(p_ad->>'sort_order')::int);
  else
    update private.business_ads set name=trim(p_ad->>'name'),category=p_ad->>'category',title=trim(p_ad->>'title'),
      description=trim(p_ad->>'description'),image_url=nullif(trim(p_ad->>'image_url'),''),address=trim(p_ad->>'address'),
      lat=(p_ad->>'lat')::float8,lng=(p_ad->>'lng')::float8,published=(p_ad->>'published')::boolean,
      sort_order=(p_ad->>'sort_order')::int,updated_at=clock_timestamp()
    where id=p_id and archived_at is null and updated_at=p_updated_at;
    if not found then raise exception 'business_ad_changed' using errcode='P0001'; end if;
  end if;
  return private.admin_business_state();
end $$;
create function private.admin_archive_business_ad(p_id uuid,p_updated_at timestamptz) returns jsonb
language plpgsql security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  update private.business_ads set archived_at=clock_timestamp(),published=false,updated_at=clock_timestamp()
    where id=p_id and archived_at is null and updated_at=p_updated_at;
  if not found then raise exception 'business_ad_changed' using errcode='P0001'; end if;
  return private.admin_business_state();
end $$;

create function private.create_business_delivery_request(p_ad_id uuid,p_destination_lat float8,p_destination_lng float8,
  p_destination_address text,p_distance_meters int,p_duration_seconds int,p_route_polyline text,p_payment_method text,p_details jsonb)
returns public.rides language plpgsql security definer set search_path = '' as $$
declare v_ad private.business_ads; v_ride public.rides; v_enabled boolean; v_uid text:=private.requesting_uid();
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode='28000'; end if;
  select enabled into v_enabled from private.business_delivery_settings where singleton for share;
  if not v_enabled then raise exception 'business_delivery_disabled' using errcode='P0001'; end if;
  select * into v_ad from private.business_ads where id=p_ad_id and published and archived_at is null for share;
  if not found then raise exception 'business_ad_unavailable' using errcode='P0001'; end if;
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

-- Public RPCs remain invoker wrappers. Private helpers enforce Firebase/admin identity.
create function public.business_feed() returns jsonb language sql stable security invoker set search_path='' as $$ select private.business_feed() $$;
create function public.admin_business_state() returns jsonb language sql stable security invoker set search_path='' as $$ select private.admin_business_state() $$;
create function public.admin_set_business_enabled(p_enabled boolean) returns jsonb language sql security invoker set search_path='' as $$ select private.admin_set_business_enabled(p_enabled) $$;
create function public.admin_set_business_courier(p_driver_id text,p_selected boolean) returns jsonb language sql security invoker set search_path='' as $$ select private.admin_set_business_courier(p_driver_id,p_selected) $$;
create function public.admin_save_business_ad(p_id uuid,p_ad jsonb,p_updated_at timestamptz) returns jsonb language sql security invoker set search_path='' as $$ select private.admin_save_business_ad(p_id,p_ad,p_updated_at) $$;
create function public.admin_archive_business_ad(p_id uuid,p_updated_at timestamptz) returns jsonb language sql security invoker set search_path='' as $$ select private.admin_archive_business_ad(p_id,p_updated_at) $$;
create function public.create_business_delivery_request(p_ad_id uuid,p_destination_lat float8,p_destination_lng float8,
  p_destination_address text,p_distance_meters int,p_duration_seconds int,p_route_polyline text,p_payment_method text,p_details jsonb)
returns public.rides language sql security invoker set search_path='' as $$ select private.create_business_delivery_request(
  p_ad_id,p_destination_lat,p_destination_lng,p_destination_address,p_distance_meters,p_duration_seconds,p_route_polyline,p_payment_method,p_details) $$;
revoke all on function private.business_feed(),private.admin_business_state(),private.admin_set_business_enabled(boolean),
  private.admin_set_business_courier(text,boolean),private.admin_save_business_ad(uuid,jsonb,timestamptz),
  private.admin_archive_business_ad(uuid,timestamptz),private.create_business_delivery_request(uuid,float8,float8,text,int,int,text,text,jsonb),
  public.business_feed(),public.admin_business_state(),public.admin_set_business_enabled(boolean),
  public.admin_set_business_courier(text,boolean),public.admin_save_business_ad(uuid,jsonb,timestamptz),
  public.admin_archive_business_ad(uuid,timestamptz),public.create_business_delivery_request(uuid,float8,float8,text,int,int,text,text,jsonb)
  from public,anon,authenticated;
grant execute on function private.business_feed(),private.admin_business_state(),private.admin_set_business_enabled(boolean),
  private.admin_set_business_courier(text,boolean),private.admin_save_business_ad(uuid,jsonb,timestamptz),
  private.admin_archive_business_ad(uuid,timestamptz),private.create_business_delivery_request(uuid,float8,float8,text,int,int,text,text,jsonb),
  public.business_feed(),public.admin_business_state(),public.admin_set_business_enabled(boolean),
  public.admin_set_business_courier(text,boolean),public.admin_save_business_ad(uuid,jsonb,timestamptz),
  public.admin_archive_business_ad(uuid,timestamptz),public.create_business_delivery_request(uuid,float8,float8,text,int,int,text,text,jsonb)
  to authenticated;
