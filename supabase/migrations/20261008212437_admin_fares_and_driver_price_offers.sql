-- Editable mototaxi coefficients; delivery pricing remains independent.
create table private.fare_settings (
  singleton boolean primary key default true check (singleton),
  honda_premium_percent numeric(5,2) not null default 12 check (honda_premium_percent between 0 and 100),
  rounding_step numeric(4,2) not null default 0.10 check (rounding_step between 0.01 and 10),
  driver_price_offers_enabled boolean not null default false,
  updated_at timestamptz not null default now(),
  updated_by text
);
alter table private.fare_settings enable row level security;
revoke all on private.fare_settings from public, anon, authenticated;
insert into private.fare_settings(singleton) values (true);
update public.vehicle_types set base_fare = 1.50, min_fare = 3.00 where code = 'mototaxi';

create function private.get_fare_settings()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  return (select jsonb_build_object('base_fare', v.base_fare, 'per_km', v.per_km,
    'per_minute', v.per_minute, 'min_fare', v.min_fare, 'honda_premium_percent', s.honda_premium_percent,
    'rounding_step', s.rounding_step, 'driver_price_offers_enabled', s.driver_price_offers_enabled)
    from public.vehicle_types v cross join private.fare_settings s where v.code = 'mototaxi' and s.singleton);
end $$;
create function public.get_fare_settings()
returns jsonb language sql security invoker set search_path = '' as $$ select private.get_fare_settings() $$;
revoke all on function private.get_fare_settings(), public.get_fare_settings() from public, anon;
grant execute on function private.get_fare_settings(), public.get_fare_settings() to authenticated;

create table private.ride_price_offers (
  id uuid primary key default extensions.gen_random_uuid(),
  ride_id uuid not null references public.rides(id) on delete cascade,
  driver_id text not null references public.profiles(id) on delete cascade,
  amount numeric(10,2) not null check (amount between 0.01 and 9999.99),
  app_fare numeric(10,2) not null,
  status text not null default 'pending' check (status in ('pending','accepted','rejected','withdrawn')),
  created_at timestamptz not null default now(),
  unique (ride_id, driver_id)
);
create index ride_price_offers_driver_pending on private.ride_price_offers(driver_id) where status = 'pending';
alter table private.ride_price_offers enable row level security;
revoke all on private.ride_price_offers from public, anon, authenticated;

create function private.set_fare_settings(p_settings jsonb)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare k text; n numeric; v_base numeric; v_km numeric; v_minute numeric; v_minimum numeric;
  v_premium numeric; v_step numeric; v_enabled boolean;
begin
  if private.requesting_uid() is null or not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  if p_settings is null or jsonb_typeof(p_settings) <> 'object' then raise exception 'invalid_fare_settings' using errcode = '22023'; end if;
  foreach k in array array['base_fare','per_km','per_minute','min_fare','honda_premium_percent','rounding_step'] loop
    if jsonb_typeof(p_settings -> k) is distinct from 'number' then raise exception 'invalid_fare_settings' using errcode = '22023'; end if;
    n := (p_settings ->> k)::numeric;
    if n < 0 or n > 9999.99 or n <> round(n,2) then raise exception 'invalid_fare_settings' using errcode = '22023'; end if;
  end loop;
  if jsonb_typeof(p_settings -> 'driver_price_offers_enabled') is distinct from 'boolean' then
    raise exception 'invalid_fare_settings' using errcode = '22023'; end if;
  v_base := (p_settings ->> 'base_fare')::numeric;
  v_km := (p_settings ->> 'per_km')::numeric;
  v_minute := (p_settings ->> 'per_minute')::numeric;
  v_minimum := (p_settings ->> 'min_fare')::numeric;
  v_premium := (p_settings ->> 'honda_premium_percent')::numeric;
  v_step := (p_settings ->> 'rounding_step')::numeric;
  v_enabled := (p_settings ->> 'driver_price_offers_enabled')::boolean;
  if v_premium > 100 or v_step not between 0.01 and 10 then raise exception 'invalid_fare_settings' using errcode = '22023'; end if;
  update private.fare_settings set honda_premium_percent = v_premium, rounding_step = v_step,
    driver_price_offers_enabled = v_enabled, updated_at = now(), updated_by = private.requesting_uid() where singleton;
  update public.vehicle_types set base_fare = v_base, per_km = v_km, per_minute = v_minute, min_fare = v_minimum where code = 'mototaxi';
  if not v_enabled then update private.ride_price_offers set status = 'withdrawn' where status = 'pending'; end if;
  return private.get_fare_settings();
end $$;
create function public.admin_set_fare_settings(p_settings jsonb)
returns jsonb language sql security invoker set search_path = '' as $$ select private.set_fare_settings(p_settings) $$;
revoke all on function private.set_fare_settings(jsonb), public.admin_set_fare_settings(jsonb) from public, anon;
grant execute on function private.set_fare_settings(jsonb), public.admin_set_fare_settings(jsonb) to authenticated;

create or replace function private.compute_fare(p_vehicle_type text, p_distance_m integer, p_duration_s integer)
returns numeric language sql stable security definer set search_path = '' as $$
  select greatest(ceil(v.min_fare / s.step) * s.step,
    round((v.base_fare + v.per_km * (p_distance_m / 1000.0) + v.per_minute * (p_duration_s / 60.0)) / s.step) * s.step)
  from public.vehicle_types v cross join lateral (
    select case when p_vehicle_type = 'mototaxi' then f.rounding_step else 0.10 end as step
    from private.fare_settings f where singleton
  ) s where v.code = p_vehicle_type and v.is_active
$$;
create function private.honda_fare(p_fare numeric)
returns numeric language sql stable security definer set search_path = '' as $$
  select round((p_fare * (1 + honda_premium_percent / 100)) / rounding_step) * rounding_step
  from private.fare_settings where singleton
$$;
revoke all on function private.honda_fare(numeric) from public, anon, authenticated;

-- Preserve the existing PIN, cancellation and booking rules while sharing the configurable premium.
do $$ declare r record; body text; updated text; begin
  for r in select p.oid from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'private' and p.proname in ('rides_before_insert','schedule_ride') loop
    body := pg_get_functiondef(r.oid);
    updated := replace(replace(body, 'round(new.estimated_fare * 1.12, 1)', 'private.honda_fare(new.estimated_fare)'),
      'round(v_fare * 1.12, 1)', 'private.honda_fare(v_fare)');
    if body = updated then raise exception 'Expected Honda calculation missing'; end if;
    execute updated;
  end loop;
end $$;

-- Reuse exactly the normal acceptance checks, without changing JWT identity.
do $$ declare body text; updated text; begin
  body := pg_get_functiondef('public.accept_ride(uuid)'::regprocedure);
  updated := replace(replace(body, 'public.accept_ride(p_ride_id uuid)',
    'private.accept_ride_for_driver(p_ride_id uuid, p_driver_id text)'),
    'v_uid text := private.requesting_uid();', 'v_uid text := p_driver_id;');
  if updated = body or position('v_uid text := p_driver_id;' in updated) = 0 then raise exception 'Expected acceptance definition missing'; end if;
  execute replace(updated, 'RETURNS rides', 'RETURNS public.rides');
end $$;
revoke all on function private.accept_ride_for_driver(uuid,text) from public, anon, authenticated;
create function private.accept_ride(p_ride_id uuid)
returns public.rides language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  return private.accept_ride_for_driver(p_ride_id, private.requesting_uid());
end $$;
create or replace function public.accept_ride(p_ride_id uuid)
returns public.rides language sql security invoker set search_path = '' as $$ select private.accept_ride(p_ride_id) $$;
revoke all on function private.accept_ride(uuid), public.accept_ride(uuid) from public, anon;
grant execute on function private.accept_ride(uuid), public.accept_ride(uuid) to authenticated;

create function private.propose_ride_price(p_ride_id uuid, p_amount numeric)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_uid text := private.requesting_uid(); r public.rides; v_vehicle public.vehicles; offer private.ride_price_offers;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not (select driver_price_offers_enabled from private.fare_settings where singleton) then
    raise exception 'price_offers_disabled' using errcode = '42501'; end if;
  if p_amount is null or p_amount::text in ('NaN','Infinity','-Infinity') or p_amount not between 0.01 and 9999.99 or p_amount <> round(p_amount,2) then
    raise exception 'invalid_offer_amount' using errcode = '22023'; end if;
  select v.* into v_vehicle from public.drivers d join public.vehicles v on v.driver_id = d.id and v.is_active
    join public.vehicle_types vt on vt.code = v.vehicle_type and vt.is_active
    where d.id = v_uid and d.status = 'approved' for update of d;
  if not found then raise exception 'driver_not_approved' using errcode = '42501'; end if;
  perform private.assert_not_blocked(v_uid, 'driver');
  if not exists(select 1 from public.driver_locations where driver_id = v_uid and is_available) or
    exists(select 1 from public.rides where driver_id = v_uid and status in ('accepted','arrived')) then
    raise exception 'driver_unavailable' using errcode = 'P0001'; end if;
  select * into r from public.rides where id = p_ride_id for update;
  if not found or r.status <> 'searching' or r.service_kind <> 'passenger' or r.vehicle_type <> 'mototaxi'
    or r.rider_id = v_uid or r.vehicle_type <> v_vehicle.vehicle_type
    or (r.preferred_vehicle_brand is not null and r.preferred_vehicle_brand <> lower(trim(v_vehicle.brand))) then
    raise exception 'ride_not_available' using errcode = 'P0002'; end if;
  if p_amount = r.estimated_fare then raise exception 'offer_must_differ' using errcode = '22023'; end if;
  if exists(select 1 from private.ride_price_offers where ride_id = p_ride_id and driver_id = v_uid) then
    raise exception 'offer_already_sent' using errcode = 'P0001'; end if;
  insert into private.ride_price_offers(ride_id,driver_id,amount,app_fare) values (p_ride_id,v_uid,p_amount,r.estimated_fare) returning * into offer;
  return jsonb_build_object('id',offer.id,'ride_id',offer.ride_id,'amount',offer.amount,'status',offer.status);
end $$;
create function public.propose_ride_price(p_ride_id uuid, p_amount numeric)
returns jsonb language sql security invoker set search_path = '' as $$ select private.propose_ride_price(p_ride_id,p_amount) $$;
revoke all on function private.propose_ride_price(uuid,numeric), public.propose_ride_price(uuid,numeric) from public, anon;
grant execute on function private.propose_ride_price(uuid,numeric), public.propose_ride_price(uuid,numeric) to authenticated;

create function private.my_ride_price_offers(p_ride_id uuid default null)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare v_uid text := private.requesting_uid();
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  return coalesce((select jsonb_agg(jsonb_build_object('id',o.id,'ride_id',o.ride_id,'amount',o.amount,
    'app_fare',o.app_fare,'status',o.status,'driver_name',nullif(trim(p.first_name || ' ' || p.last_name),''),
    'driver_photo',p.photo_url,'vehicle',concat_ws(' ',v.brand,v.model,v.color)) order by o.created_at)
    from private.ride_price_offers o join public.rides r on r.id=o.ride_id join public.profiles p on p.id=o.driver_id
    left join public.vehicles v on v.driver_id=o.driver_id and v.is_active
    where (p_ride_id is null or o.ride_id=p_ride_id) and
      ((o.driver_id=v_uid and r.status in ('searching','accepted','arrived','in_progress')) or
       (r.rider_id=v_uid and r.status='searching' and o.status='pending' and
        (select driver_price_offers_enabled from private.fare_settings where singleton) and
        exists(select 1 from public.driver_locations dl join public.drivers d on d.id=dl.driver_id
          where dl.driver_id=o.driver_id and dl.is_available and d.status='approved') and
        not exists(select 1 from public.rides busy where busy.driver_id=o.driver_id and busy.status in ('accepted','arrived'))))), '[]'::jsonb);
end $$;
create function public.my_ride_price_offers(p_ride_id uuid default null)
returns jsonb language sql security invoker set search_path = '' as $$ select private.my_ride_price_offers(p_ride_id) $$;
revoke all on function private.my_ride_price_offers(uuid), public.my_ride_price_offers(uuid) from public, anon;
grant execute on function private.my_ride_price_offers(uuid), public.my_ride_price_offers(uuid) to authenticated;

create function private.respond_ride_price_offer(p_offer_id uuid, p_accept boolean)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_uid text := private.requesting_uid(); offer private.ride_price_offers; r public.rides;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if p_accept is null then raise exception 'invalid_response' using errcode = '22023'; end if;
  select o.* into offer from private.ride_price_offers o join public.rides ride on ride.id=o.ride_id
    where o.id=p_offer_id and ride.rider_id=v_uid;
  if not found then raise exception 'offer_not_available' using errcode = 'P0002'; end if;
  -- Same lock order as normal acceptance: driver, then ride, then offer.
  perform 1 from public.drivers where id=offer.driver_id for update;
  select * into r from public.rides where id=offer.ride_id for update;
  select * into offer from private.ride_price_offers where id=p_offer_id for update;
  if r.status <> 'searching' or offer.status <> 'pending' then raise exception 'offer_not_available' using errcode = 'P0002'; end if;
  if not p_accept then
    update private.ride_price_offers set status='rejected' where id=p_offer_id;
    return jsonb_build_object('ride_id',r.id,'status','rejected');
  end if;
  if not (select driver_price_offers_enabled from private.fare_settings where singleton) then
    raise exception 'price_offers_disabled' using errcode = '42501'; end if;
  if not exists(select 1 from public.driver_locations where driver_id=offer.driver_id and is_available) then
    raise exception 'driver_unavailable' using errcode = 'P0001'; end if;
  perform private.assert_not_blocked(v_uid, 'rider');
  update public.rides set estimated_fare=offer.amount where id=r.id;
  r := private.accept_ride_for_driver(r.id,offer.driver_id);
  update private.ride_price_offers set status=case when id=p_offer_id then 'accepted' else 'rejected' end
    where ride_id=r.id and status='pending';
  return jsonb_build_object('ride_id',r.id,'status','accepted','fare',r.estimated_fare);
end $$;
create function public.respond_ride_price_offer(p_offer_id uuid,p_accept boolean)
returns jsonb language sql security invoker set search_path = '' as $$ select private.respond_ride_price_offer(p_offer_id,p_accept) $$;
revoke all on function private.respond_ride_price_offer(uuid,boolean), public.respond_ride_price_offer(uuid,boolean) from public, anon;
grant execute on function private.respond_ride_price_offer(uuid,boolean), public.respond_ride_price_offer(uuid,boolean) to authenticated;

-- Existing help copy must no longer advertise the previous fixed mototaxi prices.
update private.support_chat_settings set knowledge = replace(knowledge,
  '- Mototaxi: S/ 2.50 de base, S/ 1.00 por km y S/ 0.10 por minuto, con un mínimo de S/ 4.00.',
  '- Mototaxi: la base, el precio por km/minuto, el mínimo, el recargo Honda y el redondeo los configura el administrador. Consulta el importe vigente en la app antes de confirmar.')
where singleton;
