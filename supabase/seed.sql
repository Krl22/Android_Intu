-- =====================================================================
-- Datos de PRUEBA para desarrollo (NO usar en producción)
--
-- Crea mototaxistas falsos en distritos de Lima donde el mototaxi es común,
-- y un job de pg_cron que los mueve un poco cada minuto para que sigan
-- apareciendo en nearby_drivers() (que ignora ubicaciones de más de 2 min).
--
-- Todos los ids de prueba empiezan con 'test_'. Correr este archivo de nuevo
-- reinicia los datos de prueba sin tocar usuarios reales.
--
-- Para apagar la simulación:
--   select cron.unschedule('dev-simulate-test-drivers');
-- =====================================================================

-- Limpiar datos de prueba anteriores
delete from public.rides where rider_id like 'test\_%' or driver_id like 'test\_%';
delete from public.profiles where id like 'test\_%'; -- cascade: drivers, vehicles, driver_locations, device_tokens
drop table if exists private.test_driver_homes;

-- Punto "base" de cada conductor de prueba; la simulación los mueve alrededor de este punto
create table private.test_driver_homes (
  driver_id text primary key,
  latitude  double precision not null,
  longitude double precision not null
);

drop table if exists seed_drivers;
create temporary table seed_drivers (
  n int, first_name text, last_name text, district text,
  lat double precision, lng double precision, brand text, model text, color text
);

insert into seed_drivers values
  (1,  'Jhon',     'Quispe Mamani',     'San Juan de Lurigancho',  -11.9795, -77.0050, 'Bajaj', 'RE 205',   'Rojo'),
  (2,  'Wilmer',   'Huamán Ccori',      'San Juan de Lurigancho',  -11.9920, -77.0100, 'Honda', 'Wave 110', 'Azul'),
  (3,  'Edwin',    'Condori Apaza',     'Comas',                   -11.9380, -77.0580, 'Bajaj', 'RE 205',   'Amarillo'),
  (4,  'Percy',    'Flores Rojas',      'Comas',                   -11.9290, -77.0480, 'TVS',   'King',     'Verde'),
  (5,  'Richard',  'Mendoza Salas',     'Villa El Salvador',       -12.2130, -76.9360, 'Bajaj', 'RE 205',   'Rojo'),
  (6,  'Yuri',     'Choque Ticona',     'Villa El Salvador',       -12.2040, -76.9450, 'Honda', 'Wave 110', 'Negro'),
  (7,  'Fredy',    'Vargas Paucar',     'Ate',                     -12.0260, -76.9180, 'Bajaj', 'RE 205',   'Azul'),
  (8,  'Elmer',    'Torres Huanca',     'Puente Piedra',           -11.8640, -77.0740, 'TVS',   'King',     'Rojo'),
  (9,  'Rosa',     'Gutiérrez Layme',   'Carabayllo',              -11.8900, -77.0300, 'Bajaj', 'RE 205',   'Blanco'),
  (10, 'Juan',     'Ramos Cusi',        'Chorrillos',              -12.1740, -77.0150, 'Honda', 'Wave 110', 'Amarillo'),
  (11, 'Marco',    'Chávez Pari',       'Los Olivos',              -11.9700, -77.0720, 'Bajaj', 'RE 205',   'Verde'),
  (12, 'Luis',     'Sánchez Yupanqui',  'Villa María del Triunfo', -12.1600, -76.9420, 'TVS',   'King',     'Azul');

insert into public.profiles (id, first_name, last_name, phone, terms_accepted_at, driver_mode)
select 'test_driver_' || n, first_name, last_name, '+5199900' || lpad(n::text, 4, '0'), now(), true
from seed_drivers;

insert into public.drivers (id, document_type, document_number, license_number, status, rating, rating_count, approved_at)
select 'test_driver_' || n, 'dni', (70000000 + n)::text, 'Q' || (40000000 + n)::text,
       'approved', round((4.5 + random() * 0.5)::numeric, 2), 20 + n * 3, now()
from seed_drivers;

insert into public.vehicles (driver_id, vehicle_type, brand, model, year, color, plate, soat_expires_on)
select 'test_driver_' || n, 'mototaxi', brand, model, 2018 + (n % 6), color,
       (1000 + n * 37)::text || '-' || (n % 9 + 1) || 'A', current_date + 180
from seed_drivers;

insert into private.test_driver_homes (driver_id, latitude, longitude)
select 'test_driver_' || n, lat, lng from seed_drivers;

insert into public.driver_locations (driver_id, latitude, longitude, heading, is_available)
select 'test_driver_' || n, lat, lng, (random() * 359)::real, true
from seed_drivers;

drop table seed_drivers;

-- Pasajeros de prueba (para simular solicitudes al probar el modo conductor)
insert into public.profiles (id, first_name, last_name, phone, terms_accepted_at)
values ('test_rider_1', 'Ana', 'Pérez Lozano', '+51999100001', now()),
       ('test_rider_2', 'Carlos', 'Díaz Ríos', '+51999100002', now());

-- Mueve a cada conductor de prueba hasta ~300 m alrededor de su punto base
create or replace function private.dev_simulate_test_drivers()
returns void
language sql
security definer
set search_path = ''
as $$
  update public.driver_locations dl
     set latitude  = h.latitude  + (random() - 0.5) * 0.006,
         longitude = h.longitude + (random() - 0.5) * 0.006,
         heading   = (random() * 359)::real
    from private.test_driver_homes h
   where dl.driver_id = h.driver_id;
$$;

select cron.unschedule(jobid) from cron.job where jobname = 'dev-simulate-test-drivers';
select cron.schedule('dev-simulate-test-drivers', '* * * * *', 'select private.dev_simulate_test_drivers()');
