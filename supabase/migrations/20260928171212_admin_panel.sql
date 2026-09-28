-- Panel de administración dentro de la app: los administradores ven a los conductores registrados
-- y los aprueban, rechazan o suspenden. Antes había que hacerlo con SQL.
--
-- Para hacer admin a alguien (una vez, en el SQL Editor):
--   insert into private.admins (user_id) values ('<uid de Firebase>');

create table private.admins (
  user_id    text primary key references public.profiles (id) on delete cascade,
  created_at timestamptz not null default now()
);
alter table private.admins enable row level security;
revoke all on private.admins from public, anon, authenticated;

-- La app lo usa para mostrar u ocultar el panel
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (select 1 from private.admins where user_id = private.requesting_uid())
$$;
revoke execute on function public.is_admin() from public, anon;
grant execute on function public.is_admin() to authenticated;

-- Conductores con su perfil y vehículo activo (solo administradores)
create or replace function public.admin_list_drivers(p_status public.driver_status default null)
returns table (
  id text,
  first_name text,
  last_name text,
  phone text,
  email text,
  photo_url text,
  document_type text,
  document_number text,
  license_number text,
  status public.driver_status,
  rating numeric,
  rating_count integer,
  created_at timestamptz,
  approved_at timestamptz,
  vehicle_type text,
  brand text,
  model text,
  year smallint,
  color text,
  plate text
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;

  return query
    select d.id, p.first_name, p.last_name, p.phone, p.email, p.photo_url,
           d.document_type, d.document_number, d.license_number, d.status, d.rating, d.rating_count,
           d.created_at, d.approved_at,
           v.vehicle_type, v.brand, v.model, v.year, v.color, v.plate
    from public.drivers d
    join public.profiles p on p.id = d.id
    left join public.vehicles v on v.driver_id = d.id and v.is_active
    where p_status is null or d.status = p_status
    order by d.created_at desc;
end
$$;
revoke execute on function public.admin_list_drivers(public.driver_status) from public, anon;
grant execute on function public.admin_list_drivers(public.driver_status) to authenticated;

-- Aprobar, rechazar o suspender a un conductor (solo administradores)
create or replace function public.admin_set_driver_status(p_driver_id text, p_status public.driver_status)
returns public.drivers
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_driver public.drivers;
begin
  if not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;

  update public.drivers
     set status = p_status,
         approved_at = case when p_status = 'approved' then now() else approved_at end
   where id = p_driver_id
  returning * into v_driver;

  if not found then
    raise exception 'driver_not_found' using errcode = 'P0002';
  end if;

  -- Rechazado o suspendido: deja de aparecer como disponible
  if p_status <> 'approved' then
    update public.driver_locations set is_available = false where driver_id = p_driver_id;
  end if;
  return v_driver;
end
$$;
revoke execute on function public.admin_set_driver_status(text, public.driver_status) from public, anon;
grant execute on function public.admin_set_driver_status(text, public.driver_status) to authenticated;
