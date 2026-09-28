-- Herramientas de prueba para administradores: ver todas las cuentas y reiniciarlas para repetir
-- los procesos desde el inicio (registro de conductor, aprobación, completar perfil).
-- El historial de viajes se conserva; solo se cancelan los viajes abiertos.

-- El teléfono verificado del login solo reemplaza al del propio perfil. Antes también se aplicaba
-- cuando alguien editaba otro perfil (por ejemplo, un admin reiniciando una cuenta).
create or replace function private.profiles_before_write()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.id = private.requesting_uid() then
    new.phone := coalesce(nullif(auth.jwt() ->> 'phone_number', ''), new.phone);
  end if;
  return new;
end
$$;

-- Todas las cuentas con su rol (solo administradores)
create or replace function public.admin_list_users()
returns table (
  id text,
  first_name text,
  last_name text,
  phone text,
  email text,
  photo_url text,
  driver_mode boolean,
  driver_status public.driver_status,
  is_admin boolean,
  rides_as_rider bigint,
  rides_as_driver bigint,
  created_at timestamptz
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
    select p.id, p.first_name, p.last_name, p.phone, p.email, p.photo_url, p.driver_mode,
           d.status,
           exists (select 1 from private.admins a where a.user_id = p.id),
           (select count(*) from public.rides r where r.rider_id = p.id),
           (select count(*) from public.rides r where r.driver_id = p.id),
           p.created_at
    from public.profiles p
    left join public.drivers d on d.id = p.id
    order by p.created_at desc;
end
$$;
revoke execute on function public.admin_list_users() from public, anon;
grant execute on function public.admin_list_users() to authenticated;

-- Reinicia una cuenta para pruebas (solo administradores).
--   'driver':  borra datos de conductor, vehículo y ubicación; queda como pasajero.
--   'account': lo anterior y además vacía el perfil, para que la app vuelva a pedirlo.
create or replace function public.admin_reset_user(p_user_id text, p_scope text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  if p_scope not in ('driver', 'account') then
    raise exception 'invalid_scope' using errcode = '22023';
  end if;
  if not exists (select 1 from public.profiles where id = p_user_id) then
    raise exception 'user_not_found' using errcode = 'P0002';
  end if;

  -- Viajes abiertos donde participa: se cancelan para no dejar a nadie esperando
  update public.rides
     set status = 'cancelled', cancelled_at = now(), cancelled_by = 'system',
         cancel_reason = 'Cuenta reiniciada por un administrador'
   where status in ('searching', 'accepted', 'arrived', 'in_progress')
     and (driver_id = p_user_id or (p_scope = 'account' and rider_id = p_user_id));

  -- Datos de conductor. El historial se conserva, sin enlazar al registro que se borra.
  update public.rides set driver_id = null, vehicle_id = null where driver_id = p_user_id;
  delete from public.driver_locations where driver_id = p_user_id;
  delete from public.vehicles where driver_id = p_user_id;
  delete from public.drivers where id = p_user_id;
  update public.profiles set driver_mode = false where id = p_user_id;

  if p_scope = 'account' then
    update public.profiles
       set first_name = '', last_name = '', birthdate = null, phone = null, terms_accepted_at = null
     where id = p_user_id;
  end if;
end
$$;
revoke execute on function public.admin_reset_user(text, text) from public, anon;
grant execute on function public.admin_reset_user(text, text) to authenticated;
