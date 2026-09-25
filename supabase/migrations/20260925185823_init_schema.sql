-- =====================================================================
-- Intu: esquema inicial
--
-- Login: Firebase Auth (Third-Party Auth en Supabase). El uid de Firebase
-- llega en el claim `sub` del JWT y es TEXTO (no uuid), por eso usamos
-- private.requesting_uid() en lugar de auth.uid().
--
-- Flujo de un viaje (tabla rides):
--   searching -> accepted -> arrived -> in_progress -> completed
--        \-----------\----------\--> cancelled
-- Los cambios de estado solo se hacen con las funciones RPC de abajo
-- (accept_ride, advance_ride, cancel_ride); la app no puede hacer UPDATE
-- directo sobre rides.
-- =====================================================================

create extension if not exists postgis with schema extensions;
create extension if not exists pg_cron;

create schema if not exists private;
revoke all on schema private from public;
grant usage on schema private to authenticated;
alter default privileges in schema private revoke execute on functions from public;

-- ---------------------------------------------------------------------
-- Helpers
-- ---------------------------------------------------------------------

-- uid de Firebase del usuario que hace la petición (null si no hay sesión)
create or replace function private.requesting_uid()
returns text
language sql
stable
set search_path = ''
as $$
  select nullif(auth.jwt() ->> 'sub', '')
$$;
grant execute on function private.requesting_uid() to authenticated;

create or replace function private.touch_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at := now();
  return new;
end
$$;

-- ---------------------------------------------------------------------
-- Tipos de vehículo y tarifas (datos de referencia)
-- ---------------------------------------------------------------------

create table public.vehicle_types (
  code           text primary key,
  name           text not null,
  base_fare      numeric(8,2) not null check (base_fare >= 0),
  per_km         numeric(8,2) not null check (per_km >= 0),
  per_minute     numeric(8,2) not null check (per_minute >= 0),
  min_fare       numeric(8,2) not null check (min_fare >= 0),
  max_passengers smallint not null check (max_passengers > 0),
  is_active      boolean not null default true
);

alter table public.vehicle_types enable row level security;
revoke all on public.vehicle_types from anon, authenticated;
grant select on public.vehicle_types to authenticated;

create policy "vehicle_types: lectura para usuarios con sesión"
  on public.vehicle_types for select to authenticated
  using (true);

-- Por ahora solo mototaxi. Para agregar otro tipo basta un INSERT (p. ej. 'car').
insert into public.vehicle_types (code, name, base_fare, per_km, per_minute, min_fare, max_passengers)
values ('mototaxi', 'Mototaxi', 2.50, 1.00, 0.10, 4.00, 3);

-- Tarifa en soles, redondeada a 10 céntimos
create or replace function private.compute_fare(p_vehicle_type text, p_distance_m integer, p_duration_s integer)
returns numeric
language sql
stable
security definer
set search_path = ''
as $$
  select round(
           greatest(
             vt.min_fare,
             vt.base_fare
               + vt.per_km * (p_distance_m / 1000.0)
               + vt.per_minute * (p_duration_s / 60.0)
           ) * 10
         ) / 10
  from public.vehicle_types vt
  where vt.code = p_vehicle_type and vt.is_active
$$;

-- La app muestra este precio antes de pedir el viaje; es el mismo que se guarda en rides
create or replace function public.estimate_fare(p_vehicle_type text, p_distance_m integer, p_duration_s integer)
returns numeric
language sql
stable
security definer
set search_path = ''
as $$
  select private.compute_fare(p_vehicle_type, p_distance_m, p_duration_s)
$$;
revoke execute on function public.estimate_fare(text, integer, integer) from public, anon;
grant execute on function public.estimate_fare(text, integer, integer) to authenticated;

-- ---------------------------------------------------------------------
-- Perfiles (1 por usuario de Firebase)
-- ---------------------------------------------------------------------

create table public.profiles (
  id                text primary key default private.requesting_uid(),
  first_name        text not null default '',
  last_name         text not null default '',
  birthdate         date,
  phone             text check (phone is null or phone ~ '^\+[1-9][0-9]{6,14}$'),
  email             text,
  photo_url         text,
  terms_accepted_at timestamptz,
  driver_mode       boolean not null default false,
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now()
);

alter table public.profiles enable row level security;
revoke all on public.profiles from anon, authenticated;
grant select on public.profiles to authenticated;
grant insert (id, first_name, last_name, birthdate, phone, email, photo_url, terms_accepted_at, driver_mode)
  on public.profiles to authenticated;
grant update (first_name, last_name, birthdate, phone, email, photo_url, terms_accepted_at, driver_mode)
  on public.profiles to authenticated;

create policy "profiles: ver el propio"
  on public.profiles for select to authenticated
  using (id = (select private.requesting_uid()));
create policy "profiles: crear el propio"
  on public.profiles for insert to authenticated
  with check (id = (select private.requesting_uid()));
create policy "profiles: editar el propio"
  on public.profiles for update to authenticated
  using (id = (select private.requesting_uid()))
  with check (id = (select private.requesting_uid()));

-- Si el login fue por teléfono, el número verificado por Firebase manda
create or replace function private.profiles_before_write()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.phone := coalesce(nullif(auth.jwt() ->> 'phone_number', ''), new.phone);
  return new;
end
$$;

create trigger profiles_before_write
  before insert or update on public.profiles
  for each row execute function private.profiles_before_write();
create trigger profiles_touch_updated_at
  before update on public.profiles
  for each row execute function private.touch_updated_at();

-- ---------------------------------------------------------------------
-- Conductores
-- ---------------------------------------------------------------------

create type public.driver_status as enum ('pending', 'approved', 'rejected', 'suspended');

create table public.drivers (
  id              text primary key default private.requesting_uid()
                    references public.profiles (id) on delete cascade,
  document_type   text not null check (document_type in ('dni', 'ce')),
  document_number text not null,
  license_number  text not null,
  status          public.driver_status not null default 'pending',
  rating          numeric(3,2) not null default 5.00 check (rating between 1 and 5),
  rating_count    integer not null default 0,
  approved_at     timestamptz,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  unique (document_type, document_number),
  check (document_type <> 'dni' or document_number ~ '^[0-9]{8}$')
);

alter table public.drivers enable row level security;
revoke all on public.drivers from anon, authenticated;
grant select on public.drivers to authenticated;
-- status, rating y approved_at no se pueden tocar desde la app
grant insert (id, document_type, document_number, license_number) on public.drivers to authenticated;
grant update (document_type, document_number, license_number) on public.drivers to authenticated;

create policy "drivers: ver el propio"
  on public.drivers for select to authenticated
  using (id = (select private.requesting_uid()));
create policy "drivers: registrarse"
  on public.drivers for insert to authenticated
  with check (id = (select private.requesting_uid()));
create policy "drivers: editar el propio"
  on public.drivers for update to authenticated
  using (id = (select private.requesting_uid()))
  with check (id = (select private.requesting_uid()));

-- Si un conductor aprobado cambia sus documentos, vuelve a revisión
create or replace function private.drivers_before_update()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if auth.role() = 'authenticated'
     and old.status = 'approved'
     and (new.document_type, new.document_number, new.license_number)
         is distinct from (old.document_type, old.document_number, old.license_number) then
    new.status := 'pending';
    new.approved_at := null;
  end if;
  return new;
end
$$;

create trigger drivers_before_update
  before update on public.drivers
  for each row execute function private.drivers_before_update();
create trigger drivers_touch_updated_at
  before update on public.drivers
  for each row execute function private.touch_updated_at();

-- ---------------------------------------------------------------------
-- Vehículos
-- ---------------------------------------------------------------------

create table public.vehicles (
  id             uuid primary key default gen_random_uuid(),
  driver_id      text not null default private.requesting_uid()
                   references public.drivers (id) on delete cascade,
  vehicle_type   text not null references public.vehicle_types (code),
  brand          text not null,
  model          text not null,
  year           smallint check (year between 1980 and 2100),
  color          text,
  plate          text not null unique,
  soat_expires_on date,
  is_active      boolean not null default true,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now()
);

create index vehicles_driver_id_idx on public.vehicles (driver_id);
create unique index vehicles_one_active_per_driver on public.vehicles (driver_id) where is_active;

alter table public.vehicles enable row level security;
revoke all on public.vehicles from anon, authenticated;
grant select, delete on public.vehicles to authenticated;
grant insert (driver_id, vehicle_type, brand, model, year, color, plate, soat_expires_on, is_active)
  on public.vehicles to authenticated;
grant update (vehicle_type, brand, model, year, color, plate, soat_expires_on, is_active)
  on public.vehicles to authenticated;

create policy "vehicles: ver los propios"
  on public.vehicles for select to authenticated
  using (driver_id = (select private.requesting_uid()));
create policy "vehicles: crear los propios"
  on public.vehicles for insert to authenticated
  with check (driver_id = (select private.requesting_uid()));
create policy "vehicles: editar los propios"
  on public.vehicles for update to authenticated
  using (driver_id = (select private.requesting_uid()))
  with check (driver_id = (select private.requesting_uid()));
create policy "vehicles: borrar los propios"
  on public.vehicles for delete to authenticated
  using (driver_id = (select private.requesting_uid()));

create or replace function private.vehicles_before_write()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.plate := upper(regexp_replace(new.plate, '\s', '', 'g'));
  return new;
end
$$;

create trigger vehicles_before_write
  before insert or update on public.vehicles
  for each row execute function private.vehicles_before_write();
create trigger vehicles_touch_updated_at
  before update on public.vehicles
  for each row execute function private.touch_updated_at();

-- Tipo de vehículo del conductor aprobado que hace la petición (null si no es conductor aprobado)
create or replace function private.current_driver_vehicle_type()
returns text
language sql
stable
security definer
set search_path = ''
as $$
  select v.vehicle_type
  from public.drivers d
  join public.vehicles v on v.driver_id = d.id and v.is_active
  where d.id = private.requesting_uid()
    and d.status = 'approved'
$$;
grant execute on function private.current_driver_vehicle_type() to authenticated;

-- ---------------------------------------------------------------------
-- Ubicación de conductores (para "conductores cercanos")
-- La app escribe cada 10-30 s con set_driver_location(). La ubicación fina
-- durante un viaje va por Realtime Broadcast en el canal 'ride:<id>'.
-- ---------------------------------------------------------------------

create table public.driver_locations (
  driver_id    text primary key default private.requesting_uid()
                 references public.drivers (id) on delete cascade,
  latitude     double precision not null check (latitude between -90 and 90),
  longitude    double precision not null check (longitude between -180 and 180),
  location     extensions.geography(point, 4326)
                 generated always as (
                   extensions.st_setsrid(extensions.st_makepoint(longitude, latitude), 4326)::extensions.geography
                 ) stored,
  heading      real check (heading is null or (heading >= 0 and heading < 360)),
  is_available boolean not null default true,
  updated_at   timestamptz not null default now()
);

create index driver_locations_location_idx on public.driver_locations using gist (location);

alter table public.driver_locations enable row level security;
revoke all on public.driver_locations from anon, authenticated;
grant select, delete on public.driver_locations to authenticated;
grant insert (driver_id, latitude, longitude, heading, is_available) on public.driver_locations to authenticated;
grant update (latitude, longitude, heading, is_available) on public.driver_locations to authenticated;

-- Otros usuarios NO leen esta tabla: ven conductores cercanos solo con nearby_drivers()
create policy "driver_locations: ver la propia"
  on public.driver_locations for select to authenticated
  using (driver_id = (select private.requesting_uid()));
create policy "driver_locations: publicar la propia (solo aprobados)"
  on public.driver_locations for insert to authenticated
  with check (
    driver_id = (select private.requesting_uid())
    and (select private.current_driver_vehicle_type()) is not null
  );
create policy "driver_locations: actualizar la propia (solo aprobados)"
  on public.driver_locations for update to authenticated
  using (driver_id = (select private.requesting_uid()))
  with check (
    driver_id = (select private.requesting_uid())
    and (select private.current_driver_vehicle_type()) is not null
  );
create policy "driver_locations: borrar la propia"
  on public.driver_locations for delete to authenticated
  using (driver_id = (select private.requesting_uid()));

create trigger driver_locations_touch_updated_at
  before update on public.driver_locations
  for each row execute function private.touch_updated_at();

create or replace function public.set_driver_location(
  p_latitude double precision,
  p_longitude double precision,
  p_heading real default null,
  p_is_available boolean default true
)
returns void
language sql
security invoker
set search_path = ''
as $$
  insert into public.driver_locations (driver_id, latitude, longitude, heading, is_available)
  values (private.requesting_uid(), p_latitude, p_longitude, p_heading, p_is_available)
  on conflict (driver_id) do update
    set latitude = excluded.latitude,
        longitude = excluded.longitude,
        heading = excluded.heading,
        is_available = excluded.is_available
$$;
revoke execute on function public.set_driver_location(double precision, double precision, real, boolean) from public, anon;
grant execute on function public.set_driver_location(double precision, double precision, real, boolean) to authenticated;

-- ---------------------------------------------------------------------
-- Viajes
-- ---------------------------------------------------------------------

create type public.ride_status as enum (
  'searching', 'accepted', 'arrived', 'in_progress', 'completed', 'cancelled'
);

create table public.rides (
  id                  uuid primary key default gen_random_uuid(),
  rider_id            text not null default private.requesting_uid() references public.profiles (id),
  driver_id           text references public.drivers (id),
  vehicle_id          uuid references public.vehicles (id),
  vehicle_type        text not null references public.vehicle_types (code),
  status              public.ride_status not null default 'searching',

  origin_lat          double precision not null check (origin_lat between -90 and 90),
  origin_lng          double precision not null check (origin_lng between -180 and 180),
  origin_address      text not null,
  destination_lat     double precision not null check (destination_lat between -90 and 90),
  destination_lng     double precision not null check (destination_lng between -180 and 180),
  destination_address text not null,
  origin              extensions.geography(point, 4326)
                        generated always as (
                          extensions.st_setsrid(extensions.st_makepoint(origin_lng, origin_lat), 4326)::extensions.geography
                        ) stored,
  distance_meters     integer not null check (distance_meters between 0 and 200000),
  duration_seconds    integer not null check (duration_seconds between 0 and 36000),
  route_polyline      text,

  estimated_fare      numeric(10,2) not null,
  final_fare          numeric(10,2),
  currency            char(3) not null default 'PEN',
  payment_method      text not null default 'efectivo' check (payment_method in ('efectivo', 'yape_plin')),

  -- Copias de datos del otro participante: así nadie necesita leer el perfil ajeno
  rider_name          text,
  rider_photo_url     text,
  rider_phone         text, -- solo se llena cuando un conductor acepta
  driver_name         text,
  driver_photo_url    text,
  driver_phone        text,
  vehicle_plate       text,
  vehicle_description text,

  rating_for_driver   smallint check (rating_for_driver between 1 and 5),
  rating_for_rider    smallint check (rating_for_rider between 1 and 5),

  cancelled_by        text check (cancelled_by in ('rider', 'driver', 'system')),
  cancel_reason       text,

  requested_at        timestamptz not null default now(),
  accepted_at         timestamptz,
  arrived_at          timestamptz,
  started_at          timestamptz,
  completed_at        timestamptz,
  cancelled_at        timestamptz,
  updated_at          timestamptz not null default now()
);

create unique index rides_one_open_per_rider on public.rides (rider_id)
  where status in ('searching', 'accepted', 'arrived', 'in_progress');
create unique index rides_one_open_per_driver on public.rides (driver_id)
  where status in ('accepted', 'arrived', 'in_progress');
create index rides_searching_origin_idx on public.rides using gist (origin) where status = 'searching';
create index rides_rider_history_idx on public.rides (rider_id, requested_at desc);
create index rides_driver_history_idx on public.rides (driver_id, requested_at desc);
create index rides_vehicle_id_idx on public.rides (vehicle_id);
create index rides_vehicle_type_idx on public.rides (vehicle_type);

alter table public.rides enable row level security;
revoke all on public.rides from anon, authenticated;
grant select on public.rides to authenticated;
grant insert (vehicle_type, origin_lat, origin_lng, origin_address, destination_lat, destination_lng,
              destination_address, distance_meters, duration_seconds, route_polyline, payment_method)
  on public.rides to authenticated;

create policy "rides: participantes ven su viaje"
  on public.rides for select to authenticated
  using (
    rider_id = (select private.requesting_uid())
    or driver_id = (select private.requesting_uid())
  );
create policy "rides: conductores aprobados ven solicitudes de su tipo de vehículo"
  on public.rides for select to authenticated
  using (
    status = 'searching'
    and vehicle_type = (select private.current_driver_vehicle_type())
  );
create policy "rides: pasajero crea su solicitud"
  on public.rides for insert to authenticated
  with check (
    rider_id = (select private.requesting_uid())
    and status = 'searching'
    and driver_id is null
  );

-- Al crear: precio calculado en el servidor y datos del pasajero copiados del perfil
create or replace function private.rides_before_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.role() = 'authenticated' then
    new.rider_id := private.requesting_uid();
    new.status := 'searching';
    new.requested_at := now();
  end if;

  new.estimated_fare := private.compute_fare(new.vehicle_type, new.distance_meters, new.duration_seconds);
  if new.estimated_fare is null then
    raise exception 'vehicle_type_not_available' using errcode = '22023';
  end if;

  select nullif(trim(p.first_name || ' ' || p.last_name), ''), p.photo_url
    into new.rider_name, new.rider_photo_url
  from public.profiles p
  where p.id = new.rider_id;

  return new;
end
$$;

create trigger rides_before_insert
  before insert on public.rides
  for each row execute function private.rides_before_insert();
create trigger rides_touch_updated_at
  before update on public.rides
  for each row execute function private.touch_updated_at();

-- Conductor acepta una solicitud. Atómico: si dos conductores aceptan a la vez, solo uno gana.
create or replace function public.accept_ride(p_ride_id uuid)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_vehicle public.vehicles;
  v_driver public.profiles;
  v_ride public.rides;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  select v.* into v_vehicle
  from public.drivers d
  join public.vehicles v on v.driver_id = d.id and v.is_active
  where d.id = v_uid and d.status = 'approved';
  if not found then
    raise exception 'driver_not_approved' using errcode = '42501';
  end if;

  if exists (select 1 from public.rides
             where driver_id = v_uid and status in ('accepted', 'arrived', 'in_progress')) then
    raise exception 'driver_busy' using errcode = 'P0001';
  end if;

  select * into v_driver from public.profiles where id = v_uid;

  update public.rides r
     set status = 'accepted',
         driver_id = v_uid,
         vehicle_id = v_vehicle.id,
         accepted_at = now(),
         driver_name = nullif(trim(v_driver.first_name || ' ' || v_driver.last_name), ''),
         driver_phone = v_driver.phone,
         driver_photo_url = v_driver.photo_url,
         vehicle_plate = v_vehicle.plate,
         vehicle_description = concat_ws(' ', v_vehicle.brand, v_vehicle.model, v_vehicle.color),
         rider_phone = (select p.phone from public.profiles p where p.id = r.rider_id)
   where r.id = p_ride_id
     and r.status = 'searching'
     and r.vehicle_type = v_vehicle.vehicle_type
     and r.rider_id <> v_uid
  returning * into v_ride;

  if not found then
    raise exception 'ride_not_available' using errcode = 'P0002';
  end if;

  update public.driver_locations set is_available = false where driver_id = v_uid;
  return v_ride;
end
$$;

-- Conductor avanza el viaje: accepted -> arrived -> in_progress -> completed
create or replace function public.advance_ride(p_ride_id uuid, p_status public.ride_status)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
begin
  update public.rides
     set status = p_status,
         arrived_at   = case when p_status = 'arrived'     then now() else arrived_at end,
         started_at   = case when p_status = 'in_progress' then now() else started_at end,
         completed_at = case when p_status = 'completed'   then now() else completed_at end,
         final_fare   = case when p_status = 'completed'   then estimated_fare else final_fare end
   where id = p_ride_id
     and driver_id = v_uid
     and (   (status = 'accepted'    and p_status = 'arrived')
          or (status = 'arrived'     and p_status = 'in_progress')
          or (status = 'in_progress' and p_status = 'completed'))
  returning * into v_ride;

  if not found then
    raise exception 'invalid_transition' using errcode = 'P0001';
  end if;

  if p_status = 'completed' then
    update public.driver_locations set is_available = true where driver_id = v_uid;
  end if;
  return v_ride;
end
$$;

-- Cancelar. Si cancela el pasajero, el viaje termina.
-- Si cancela el conductor (antes de iniciar), la solicitud vuelve a 'searching' para otro conductor.
create or replace function public.cancel_ride(p_ride_id uuid, p_reason text default null)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
begin
  select * into v_ride from public.rides where id = p_ride_id for update;

  if not found or v_uid is null or (v_ride.rider_id <> v_uid and v_ride.driver_id is distinct from v_uid) then
    raise exception 'ride_not_found' using errcode = 'P0002';
  end if;
  if v_ride.status not in ('searching', 'accepted', 'arrived') then
    raise exception 'invalid_transition' using errcode = 'P0001';
  end if;

  if v_ride.rider_id = v_uid then
    update public.rides
       set status = 'cancelled', cancelled_at = now(), cancelled_by = 'rider', cancel_reason = left(p_reason, 500)
     where id = p_ride_id
    returning * into v_ride;
  else
    update public.rides
       set status = 'searching', driver_id = null, vehicle_id = null, accepted_at = null, arrived_at = null,
           driver_name = null, driver_phone = null, driver_photo_url = null,
           vehicle_plate = null, vehicle_description = null, rider_phone = null
     where id = p_ride_id
    returning * into v_ride;
    update public.driver_locations set is_available = true where driver_id = v_uid;
  end if;

  if v_ride.driver_id is not null then
    update public.driver_locations set is_available = true where driver_id = v_ride.driver_id;
  end if;
  return v_ride;
end
$$;

-- Calificar al otro participante de un viaje completado (1 vez)
create or replace function public.rate_ride(p_ride_id uuid, p_rating smallint)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
begin
  if p_rating not between 1 and 5 then
    raise exception 'invalid_rating' using errcode = '22023';
  end if;

  update public.rides
     set rating_for_driver = p_rating
   where id = p_ride_id and rider_id = v_uid and status = 'completed' and rating_for_driver is null
  returning * into v_ride;

  if found then
    update public.drivers
       set rating = round((rating * rating_count + p_rating) / (rating_count + 1), 2),
           rating_count = rating_count + 1
     where id = v_ride.driver_id;
    return v_ride;
  end if;

  update public.rides
     set rating_for_rider = p_rating
   where id = p_ride_id and driver_id = v_uid and status = 'completed' and rating_for_rider is null
  returning * into v_ride;

  if not found then
    raise exception 'cannot_rate' using errcode = 'P0001';
  end if;
  return v_ride;
end
$$;

-- Conductores disponibles cerca de un punto (para el mapa del pasajero)
create or replace function public.nearby_drivers(
  p_latitude double precision,
  p_longitude double precision,
  p_radius_m integer default 3000,
  p_vehicle_type text default null,
  p_limit integer default 20
)
returns table (
  driver_id text,
  latitude double precision,
  longitude double precision,
  heading real,
  vehicle_type text,
  distance_m double precision
)
language sql
stable
security definer
set search_path = ''
as $$
  with p as (
    select extensions.st_setsrid(extensions.st_makepoint(p_longitude, p_latitude), 4326)::extensions.geography as pt
  )
  select dl.driver_id, dl.latitude, dl.longitude, dl.heading, v.vehicle_type,
         extensions.st_distance(dl.location, p.pt) as distance_m
  from public.driver_locations dl
  cross join p
  join public.drivers d on d.id = dl.driver_id and d.status = 'approved'
  join public.vehicles v on v.driver_id = dl.driver_id and v.is_active
  where private.requesting_uid() is not null
    and dl.is_available
    and dl.updated_at > now() - interval '2 minutes'
    and (p_vehicle_type is null or v.vehicle_type = p_vehicle_type)
    and extensions.st_dwithin(dl.location, p.pt, least(greatest(p_radius_m, 0), 10000))
    and not exists (
      select 1 from public.rides r
      where r.driver_id = dl.driver_id and r.status in ('accepted', 'arrived', 'in_progress')
    )
  order by distance_m
  limit least(greatest(p_limit, 1), 50)
$$;

-- Solicitudes abiertas cerca del conductor (respeta RLS: solo su tipo de vehículo)
create or replace function public.nearby_ride_requests(
  p_latitude double precision,
  p_longitude double precision,
  p_radius_m integer default 5000
)
returns table (
  id uuid,
  rider_name text,
  rider_photo_url text,
  origin_lat double precision,
  origin_lng double precision,
  origin_address text,
  destination_lat double precision,
  destination_lng double precision,
  destination_address text,
  distance_meters integer,
  duration_seconds integer,
  estimated_fare numeric,
  payment_method text,
  requested_at timestamptz,
  pickup_distance_m double precision
)
language sql
stable
security invoker
set search_path = ''
as $$
  with p as (
    select extensions.st_setsrid(extensions.st_makepoint(p_longitude, p_latitude), 4326)::extensions.geography as pt
  )
  select r.id, r.rider_name, r.rider_photo_url,
         r.origin_lat, r.origin_lng, r.origin_address,
         r.destination_lat, r.destination_lng, r.destination_address,
         r.distance_meters, r.duration_seconds, r.estimated_fare, r.payment_method, r.requested_at,
         extensions.st_distance(r.origin, p.pt) as pickup_distance_m
  from public.rides r
  cross join p
  where r.status = 'searching'
    and extensions.st_dwithin(r.origin, p.pt, least(greatest(p_radius_m, 0), 20000))
  order by pickup_distance_m
  limit 30
$$;

revoke execute on function public.accept_ride(uuid) from public, anon;
revoke execute on function public.advance_ride(uuid, public.ride_status) from public, anon;
revoke execute on function public.cancel_ride(uuid, text) from public, anon;
revoke execute on function public.rate_ride(uuid, smallint) from public, anon;
revoke execute on function public.nearby_drivers(double precision, double precision, integer, text, integer) from public, anon;
revoke execute on function public.nearby_ride_requests(double precision, double precision, integer) from public, anon;
grant execute on function public.accept_ride(uuid) to authenticated;
grant execute on function public.advance_ride(uuid, public.ride_status) to authenticated;
grant execute on function public.cancel_ride(uuid, text) to authenticated;
grant execute on function public.rate_ride(uuid, smallint) to authenticated;
grant execute on function public.nearby_drivers(double precision, double precision, integer, text, integer) to authenticated;
grant execute on function public.nearby_ride_requests(double precision, double precision, integer) to authenticated;

-- Solicitudes sin conductor por más de 5 minutos se cancelan solas
create or replace function private.expire_stale_ride_requests()
returns void
language sql
security definer
set search_path = ''
as $$
  update public.rides
     set status = 'cancelled', cancelled_at = now(), cancelled_by = 'system', cancel_reason = 'no_driver_found'
   where status = 'searching'
     and requested_at < now() - interval '5 minutes'
$$;

select cron.schedule('expire-stale-ride-requests', '* * * * *', 'select private.expire_stale_ride_requests()');

-- ---------------------------------------------------------------------
-- Tokens de FCM (notificaciones push por Firebase)
-- ---------------------------------------------------------------------

create table public.device_tokens (
  token      text primary key,
  user_id    text not null references public.profiles (id) on delete cascade,
  platform   text not null default 'android' check (platform in ('android', 'ios', 'web')),
  updated_at timestamptz not null default now()
);

create index device_tokens_user_id_idx on public.device_tokens (user_id);

alter table public.device_tokens enable row level security;
revoke all on public.device_tokens from anon, authenticated;
grant select, delete on public.device_tokens to authenticated;

create policy "device_tokens: ver los propios"
  on public.device_tokens for select to authenticated
  using (user_id = (select private.requesting_uid()));
create policy "device_tokens: borrar los propios"
  on public.device_tokens for delete to authenticated
  using (user_id = (select private.requesting_uid()));

-- Registrar/actualizar el token del dispositivo. Si otro usuario inicia sesión
-- en el mismo teléfono, el token pasa a ese usuario.
create or replace function public.register_device_token(p_token text, p_platform text default 'android')
returns void
language sql
security definer
set search_path = ''
as $$
  insert into public.device_tokens (token, user_id, platform)
  select p_token, private.requesting_uid(), p_platform
  where private.requesting_uid() is not null
  on conflict (token) do update
    set user_id = excluded.user_id, platform = excluded.platform, updated_at = now()
$$;
revoke execute on function public.register_device_token(text, text) from public, anon;
grant execute on function public.register_device_token(text, text) to authenticated;

-- ---------------------------------------------------------------------
-- Realtime
-- ---------------------------------------------------------------------

-- Postgres Changes sobre rides (cada usuario recibe solo las filas que su RLS le deja ver)
alter publication supabase_realtime add table public.rides;

-- Broadcast privado 'ride:<id>': solo pasajero y conductor de un viaje en curso
create policy "ride participants can receive broadcast"
  on realtime.messages for select to authenticated
  using (
    realtime.messages.extension in ('broadcast', 'presence')
    and exists (
      select 1 from public.rides r
      where (select realtime.topic()) = 'ride:' || r.id::text
        and r.status in ('accepted', 'arrived', 'in_progress')
        and (r.rider_id = (select private.requesting_uid()) or r.driver_id = (select private.requesting_uid()))
    )
  );

create policy "ride participants can send broadcast"
  on realtime.messages for insert to authenticated
  with check (
    realtime.messages.extension in ('broadcast', 'presence')
    and exists (
      select 1 from public.rides r
      where (select realtime.topic()) = 'ride:' || r.id::text
        and r.status in ('accepted', 'arrived', 'in_progress')
        and (r.rider_id = (select private.requesting_uid()) or r.driver_id = (select private.requesting_uid()))
    )
  );
