-- Avisos push de los viajes, administradores desde la app y eliminación de cuentas.
--
-- Push: cuando un viaje cambia de estado, un trigger envía el aviso a la Cloud Function
-- `ridePush` de Firebase (pg_net, después del commit), que lo manda por FCM a los teléfonos
-- registrados con register_device_token. La función comparte con Supabase el secreto
-- `push_webhook_secret` (Vault aquí, Secret Manager en Firebase). Sin el secreto no se envía nada.
--
-- Eliminar cuenta: la Cloud Function `adminDeleteUser` llama a admin_delete_user con el token
-- del admin y después borra la cuenta de Firebase Auth y su foto. Los viajes se conservan
-- anónimos para la otra persona.

create extension if not exists pg_net with schema extensions;

-- ---------------------------------------------------------------------
-- Avisos push
-- ---------------------------------------------------------------------

create or replace function private.notify_ride_update()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_to     text;
  v_title  text;
  v_body   text;
  v_tokens text[];
  v_secret text;
  v_plate  text := nullif(new.vehicle_plate, '');
  v_driver text := coalesce(nullif(new.driver_name, ''), 'Tu conductor');
begin
  case new.status
    when 'accepted' then
      v_to := new.rider_id;
      v_title := 'Tu conductor va en camino';
      v_body := concat_ws(' · ', v_driver, nullif(new.vehicle_description, ''), 'Placa ' || v_plate);
    when 'arrived' then
      v_to := new.rider_id;
      v_title := 'Tu conductor llegó';
      v_body := v_driver || ' te espera en el punto de recojo.' || coalesce(' Placa ' || v_plate || '.', '');
    when 'in_progress' then
      v_to := new.rider_id;
      v_title := 'Viaje iniciado';
      v_body := 'Vas camino a ' || new.destination_address || '.';
    when 'completed' then
      v_to := new.rider_id;
      v_title := 'Llegaste a tu destino';
      v_body := 'Total: S/ ' || to_char(coalesce(new.final_fare, new.estimated_fare), 'FM999990.00')
                || case when new.payment_method = 'yape_plin' then ' por Yape o Plin' else ' en efectivo' end
                || '. Califica tu viaje.';
    when 'searching' then
      -- El conductor soltó el viaje (cancel_ride lo devuelve a la búsqueda)
      if old.status not in ('accepted', 'arrived') then
        return null;
      end if;
      v_to := new.rider_id;
      v_title := 'Tu conductor canceló';
      v_body := 'Estamos buscando otro conductor para ti.';
    when 'cancelled' then
      if new.cancelled_by = 'rider' then
        v_to := old.driver_id;
        v_title := 'El pasajero canceló el viaje';
        v_body := 'Ya puedes recibir otras solicitudes.';
      elsif new.cancel_reason = 'no_driver_found' then
        v_to := new.rider_id;
        v_title := 'No encontramos conductor';
        v_body := 'No hay conductores disponibles cerca. Intenta de nuevo en unos minutos.';
      else
        v_to := new.rider_id;
        v_title := 'Tu viaje fue cancelado';
        v_body := 'Puedes pedir otro viaje cuando quieras.';
      end if;
    else
      return null;
  end case;

  if v_to is null then
    return null;
  end if;
  select array_agg(token) into v_tokens from public.device_tokens where user_id = v_to;
  select decrypted_secret into v_secret from vault.decrypted_secrets where name = 'push_webhook_secret';
  if v_tokens is null or v_secret is null then
    return null;
  end if;

  perform net.http_post(
    url := 'https://us-central1-intu-e8403.cloudfunctions.net/ridePush',
    body := jsonb_build_object(
      'tokens', to_jsonb(v_tokens),
      'title', v_title,
      'body', v_body,
      'rideId', new.id,
      'status', new.status
    ),
    headers := jsonb_build_object('Content-Type', 'application/json', 'X-Intu-Secret', v_secret),
    timeout_milliseconds := 8000
  );
  return null;
exception when others then
  -- Un aviso que falla nunca debe impedir el cambio de estado del viaje
  raise warning 'notify_ride_update: %', sqlerrm;
  return null;
end
$$;

create trigger rides_notify_status
  after update of status on public.rides
  for each row
  when (old.status is distinct from new.status)
  execute function private.notify_ride_update();

-- La Cloud Function borra los tokens que FCM ya no acepta (app desinstalada, datos borrados)
create or replace function public.prune_device_tokens(p_secret text, p_tokens text[])
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_deleted integer;
begin
  if p_secret is null or p_secret is distinct from
     (select decrypted_secret from vault.decrypted_secrets where name = 'push_webhook_secret') then
    raise exception 'forbidden' using errcode = '42501';
  end if;
  delete from public.device_tokens where token = any (p_tokens);
  get diagnostics v_deleted = row_count;
  return v_deleted;
end
$$;
revoke execute on function public.prune_device_tokens(text, text[]) from public;
grant execute on function public.prune_device_tokens(text, text[]) to anon, authenticated;

-- ---------------------------------------------------------------------
-- Administradores
-- ---------------------------------------------------------------------

-- Dar o quitar permisos de administrador (solo administradores). Siempre queda al menos uno.
create or replace function public.admin_set_admin(p_user_id text, p_is_admin boolean)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  if not exists (select 1 from public.profiles where id = p_user_id) then
    raise exception 'user_not_found' using errcode = 'P0002';
  end if;

  -- Evita que dos admins se quiten el permiso a la vez y no quede ninguno
  lock table private.admins in exclusive mode;

  if p_is_admin then
    insert into private.admins (user_id) values (p_user_id) on conflict do nothing;
  else
    if exists (select 1 from private.admins where user_id = p_user_id)
       and (select count(*) from private.admins) <= 1 then
      raise exception 'last_admin' using errcode = 'P0001';
    end if;
    delete from private.admins where user_id = p_user_id;
  end if;
end
$$;
revoke execute on function public.admin_set_admin(text, boolean) from public, anon;
grant execute on function public.admin_set_admin(text, boolean) to authenticated;

-- ---------------------------------------------------------------------
-- Eliminar cuentas
-- ---------------------------------------------------------------------

-- Los uid de Firebase no se reutilizan. Si la app de una cuenta borrada sigue abierta con un
-- token aún vigente, no puede volver a crear el perfil.
create table private.deleted_accounts (
  user_id    text primary key,
  deleted_by text,
  deleted_at timestamptz not null default now()
);
alter table private.deleted_accounts enable row level security;
revoke all on private.deleted_accounts from public, anon, authenticated;

create or replace function private.is_deleted_account(p_user_id text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (select 1 from private.deleted_accounts where user_id = p_user_id)
$$;
grant execute on function private.is_deleted_account(text) to authenticated;

create or replace function private.profiles_before_write()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' and private.is_deleted_account(new.id) then
    raise exception 'account_deleted' using errcode = '42501';
  end if;
  if new.id = private.requesting_uid() then
    new.phone := coalesce(nullif(auth.jwt() ->> 'phone_number', ''), new.phone);
  end if;
  return new;
end
$$;

-- Viajes de cuentas eliminadas: se conservan sin pasajero
alter table public.rides alter column rider_id drop not null;

-- Borra todos los datos de una cuenta en Supabase (solo administradores). Los viajes quedan
-- anónimos. La cuenta de Firebase Auth la borra después la Cloud Function adminDeleteUser.
-- Si el perfil ya no existe no falla, para poder reintentar.
create or replace function public.admin_delete_user(p_user_id text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  if coalesce(p_user_id, '') = '' then
    raise exception 'user_not_found' using errcode = 'P0002';
  end if;

  lock table private.admins in exclusive mode;
  if exists (select 1 from private.admins where user_id = p_user_id)
     and (select count(*) from private.admins) <= 1 then
    raise exception 'last_admin' using errcode = 'P0001';
  end if;

  -- Primero los tokens, para no avisar de la cancelación a la cuenta que se borra
  delete from public.device_tokens where user_id = p_user_id;

  -- Viajes abiertos: se cancelan (la otra persona recibe el aviso)
  update public.rides
     set status = 'cancelled', cancelled_at = now(), cancelled_by = 'system',
         cancel_reason = 'Cuenta eliminada por un administrador'
   where status in ('searching', 'accepted', 'arrived', 'in_progress')
     and (rider_id = p_user_id or driver_id = p_user_id);

  update public.rides
     set rider_id = null, rider_name = 'Cuenta eliminada', rider_photo_url = null, rider_phone = null
   where rider_id = p_user_id;
  update public.rides
     set driver_id = null, vehicle_id = null,
         driver_name = 'Cuenta eliminada', driver_photo_url = null, driver_phone = null
   where driver_id = p_user_id;

  delete from public.driver_locations where driver_id = p_user_id;
  delete from public.vehicles where driver_id = p_user_id;
  delete from public.drivers where id = p_user_id;
  delete from private.admins where user_id = p_user_id;
  delete from public.profiles where id = p_user_id;

  insert into private.deleted_accounts (user_id, deleted_by)
  values (p_user_id, private.requesting_uid())
  on conflict (user_id) do nothing;
end
$$;
revoke execute on function public.admin_delete_user(text) from public, anon;
grant execute on function public.admin_delete_user(text) to authenticated;
