-- Help assistant: Claude answers user questions from an admin-edited knowledge text.
-- Off until an admin enables it. The Edge Function support-chat calls these functions with the
-- user's own token, so PostgREST verifies who is asking before any Claude request is made.

create table private.support_chat_settings (
  singleton boolean primary key default true check (singleton),
  enabled boolean not null default false,
  daily_limit_per_user integer not null default 20 check (daily_limit_per_user between 1 and 200),
  knowledge text not null default '' check (char_length(knowledge) <= 60000),
  updated_at timestamptz not null default now(),
  updated_by text
);
alter table private.support_chat_settings enable row level security;
revoke all on private.support_chat_settings from public, anon, authenticated;

create table private.support_chat_usage (
  id bigint generated always as identity primary key,
  user_id text not null,
  created_at timestamptz not null default now(),
  input_tokens integer,
  output_tokens integer,
  cache_read_tokens integer,
  cache_write_tokens integer,
  failed boolean not null default false,
  finished_at timestamptz
);
create index support_chat_usage_user_idx on private.support_chat_usage (user_id, created_at desc);
create index support_chat_usage_created_idx on private.support_chat_usage (created_at);
alter table private.support_chat_usage enable row level security;
revoke all on private.support_chat_usage from public, anon, authenticated;

-- Initial draft: docs/support/ayuda-intu-borrador.md. The live copy is edited in the admin panel.
insert into private.support_chat_settings(singleton, knowledge) values (true, $faq$
# Ayuda de Intu — preguntas frecuentes (borrador)

Este texto es lo que sabe el asistente de Ayuda. Se edita desde Admin → Seguridad → Asistente de ayuda.
Revisa cada respuesta antes de activarlo: el asistente solo repite lo que está aquí.

## Qué es Intu
Intu es una app para pedir mototaxis y envíos en moto lineal. Conecta pasajeros con conductores aprobados por Intu. La disponibilidad depende de los conductores conectados cerca de ti.

## Pedir un viaje
- En Inicio toca el buscador, elige tu destino (por dirección, en el mapa o desde tus direcciones guardadas) y confirma el punto de recojo.
- Antes de confirmar verás el precio estimado y podrás elegir el método de pago (efectivo o Yape/Plin).
- Puedes preferir una marca de mototaxi. Elegir Honda cuesta 12 % más.
- Si nadie acepta en 5 minutos, la solicitud se cancela sola y puedes intentarlo de nuevo.
- Cuando un conductor acepta verás su nombre, su foto, la placa y su ubicación en el mapa mientras va hacia ti.
- También puedes pedir un viaje para otra persona: eliges quién viaja y su recojo. Tú sigues el viaje desde tu teléfono.

## Precios
- La app calcula el precio con la distancia y el tiempo estimados de la ruta, y lo muestra antes de confirmar.
- Mototaxi: S/ 2.50 de base, S/ 1.00 por km y S/ 0.10 por minuto, con un mínimo de S/ 4.00.
- Envío en moto lineal: S/ 2.00 de base, S/ 0.80 por km y S/ 0.08 por minuto, con un mínimo de S/ 3.20.
- El precio que ves al confirmar es el que pagas; no cambia durante el viaje.

## Pagos
- Pagas directamente al conductor, en efectivo o por Yape/Plin. Intu no cobra con tarjeta ni guarda datos bancarios.
- Si eliges Yape o Plin, la app muestra el número del conductor; tócalo para copiarlo.
- Al terminar, el conductor confirma en la app que recibió el pago.

## PIN de seguridad
Cuando está activado, la app te muestra un PIN de 4 dígitos. Díselo al conductor solo al subir; sin ese PIN no puede iniciar el viaje. En los envíos, el PIN se le dice al repartidor al entregarle el paquete. No compartas el PIN antes.

## Chat con el conductor
- Cuando un conductor acepta, puedes escribirle desde la tarjeta del viaje con el botón Enviar mensaje.
- Hay respuestas rápidas como "Ya salgo" o "¿Dónde estás?".
- Por seguridad, el conductor solo puede escribir texto cuando está detenido en el punto de recojo; mientras maneja usa respuestas rápidas.
- El chat se cierra al terminar el servicio. Intu guarda los mensajes 30 días para revisar reclamos y luego se borran.
- El conductor también puede llamarte al número de tu cuenta si necesita ubicarte.

## Cancelar
- Al cancelar, la app te pide el motivo y te avisa si esa cancelación cuenta en tu contra antes de confirmar.
- No tiene penalidad cancelar mientras se busca conductor, durante los primeros minutos después de que acepta, si el conductor está terminando otro viaje, o si la app confirma que llega muy tarde o no se acerca.
- Cancelar varias veces después de eso, o no presentarte en el punto de recojo, puede pausar tu cuenta por un tiempo (por ejemplo 30 minutos, y 24 horas si se repite). La app te dice hasta qué hora.
- Si el conductor espera en el punto de recojo varios minutos y no apareces, puede cancelar el viaje.
- Si un conductor te pide que canceles tú, elige el motivo "El conductor me pidió cancelar": así no cuenta en tu contra y el equipo revisa el caso.
- Si un envío ya se pagó y el repartidor tiene el paquete, ya no se puede cancelar desde la app; contacta al cliente o reporta el problema.

## Envíos en moto lineal
- En Inicio elige Enviar. Indica quién entrega el paquete, quién lo recibe (celular peruano de 9 dígitos que empieza con 9) y una descripción.
- Solo paquetes pequeños que se puedan llevar en moto. Intu solo hace el transporte: no compra productos ni cobra su valor.
- Puedes elegir quién paga el transporte: quien envía (se paga en el recojo) o quien recibe (se paga en la entrega).
- Puedes agregar referencias de recojo y de entrega, por ejemplo "puerta azul junto a la farmacia".

## Calificaciones
- Al terminar puedes calificar de 1 a 5 estrellas al conductor (o el conductor al pasajero). También puedes hacerlo después en la pestaña Viajes.
- Las calificaciones deben reflejar tu experiencia real. Durante el servicio Intu puede mostrar el promedio de la otra persona; con menos de 3 calificaciones aparece "Nuevo".

## Tu cuenta
- Direcciones guardadas: Cuenta → Direcciones guardadas (Casa, Trabajo y favoritas).
- Modo oscuro: Cuenta → Modo oscuro.
- Actualizar la app: Cuenta → Actualizaciones.
- Reportar un problema o pedir algo sobre tus datos: Cuenta → Reportar un error. Para solicitudes sobre tus datos usa el título "Solicitud sobre mis datos".
- Eliminar tu cuenta: Cuenta → Eliminar cuenta → Solicitar eliminación. El equipo la atiende manualmente y te avisa.

## Ser conductor o repartidor
- Desde Cuenta puedes registrarte como conductor de mototaxi o como repartidor en moto lineal.
- Te pediremos tu DNI, tu licencia de conducir y los datos del vehículo (marca, modelo y placa).
- Intu revisa cada solicitud. No podrás recibir viajes hasta que esté aprobada; si cambias tus documentos, vuelve a revisión.
- Para recibir solicitudes, en el modo conductor toca el botón para ponerte en línea. Con un pasajero a bordo puedes aceptar tu siguiente viaje.
- Si cancelas muchos viajes después de aceptarlos, tu cuenta puede quedar en pausa un tiempo y no recibirás solicitudes.

## Seguridad y emergencias
- Intu no es un servicio de emergencias. Si estás en peligro llama al 105 (Policía Nacional), al 106 (SAMU) o al 116 (Bomberos).
- Nunca compartas contraseñas, códigos de verificación ni datos bancarios por el chat.
- Si algo salió mal en un viaje, repórtalo en Cuenta → Reportar un error con la fecha y lo que pasó.
$faq$);

create function private.support_chat_status()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare v_uid text := private.requesting_uid(); s private.support_chat_settings;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  select * into s from private.support_chat_settings where singleton;
  return jsonb_build_object(
    'enabled', s.enabled and btrim(s.knowledge) <> '',
    'remaining_today', greatest(0, s.daily_limit_per_user - (select count(*) from private.support_chat_usage u
      where u.user_id = v_uid and not u.failed and u.created_at > now() - interval '24 hours')));
end $$;
revoke all on function private.support_chat_status() from public, anon;
grant execute on function private.support_chat_status() to authenticated;
create function public.support_chat_status()
returns jsonb language sql stable security invoker set search_path = '' as $$
  select private.support_chat_status()
$$;
revoke all on function public.support_chat_status() from public, anon;
grant execute on function public.support_chat_status() to authenticated;

-- Reserves one question of the daily quota and hands the knowledge to the Edge Function.
create function private.support_chat_begin()
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_uid text := private.requesting_uid(); s private.support_chat_settings; v_used integer; v_id bigint;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  select * into s from private.support_chat_settings where singleton;
  if not s.enabled or btrim(s.knowledge) = '' then
    raise exception 'support_chat_disabled' using errcode = 'P0001';
  end if;
  -- One reservation at a time per user, so parallel calls cannot exceed the quota
  perform pg_advisory_xact_lock(hashtext('support_chat:' || v_uid));
  select count(*) into v_used from private.support_chat_usage
   where user_id = v_uid and not failed and created_at > now() - interval '24 hours';
  if v_used >= s.daily_limit_per_user then
    raise exception 'support_chat_limit' using errcode = 'P0001';
  end if;
  insert into private.support_chat_usage(user_id) values (v_uid) returning id into v_id;
  return jsonb_build_object('usage_id', v_id, 'knowledge', s.knowledge,
                            'remaining_today', s.daily_limit_per_user - v_used - 1);
end $$;
revoke all on function private.support_chat_begin() from public, anon;
grant execute on function private.support_chat_begin() to authenticated;
create function public.support_chat_begin()
returns jsonb language sql security invoker set search_path = '' as $$
  select private.support_chat_begin()
$$;
revoke all on function public.support_chat_begin() from public, anon;
grant execute on function public.support_chat_begin() to authenticated;

-- Token usage for the admin's cost estimate. Only the caller's own row, once, shortly after it began.
create function private.support_chat_finish(p_usage_id bigint, p_usage jsonb, p_failed boolean)
returns void language plpgsql security definer set search_path = '' as $$
begin
  update private.support_chat_usage set
    input_tokens = greatest(0, (p_usage->>'input_tokens')::integer),
    output_tokens = greatest(0, (p_usage->>'output_tokens')::integer),
    cache_read_tokens = greatest(0, (p_usage->>'cache_read_tokens')::integer),
    cache_write_tokens = greatest(0, (p_usage->>'cache_write_tokens')::integer),
    failed = coalesce(p_failed, false),
    finished_at = now()
  where id = p_usage_id and user_id = private.requesting_uid() and finished_at is null
    and created_at > now() - interval '5 minutes';
end $$;
revoke all on function private.support_chat_finish(bigint, jsonb, boolean) from public, anon;
grant execute on function private.support_chat_finish(bigint, jsonb, boolean) to authenticated;
create function public.support_chat_finish(p_usage_id bigint, p_usage jsonb, p_failed boolean default false)
returns void language sql security invoker set search_path = '' as $$
  select private.support_chat_finish(p_usage_id, p_usage, p_failed)
$$;
revoke all on function public.support_chat_finish(bigint, jsonb, boolean) from public, anon;
grant execute on function public.support_chat_finish(bigint, jsonb, boolean) to authenticated;

create function private.get_support_chat()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  return (select jsonb_build_object(
    'enabled', s.enabled, 'daily_limit_per_user', s.daily_limit_per_user, 'knowledge', s.knowledge,
    'updated_at', s.updated_at,
    'usage_30d', (select jsonb_build_object(
        'questions', count(*), 'people', count(distinct u.user_id), 'failed', count(*) filter (where u.failed),
        'input_tokens', coalesce(sum(u.input_tokens), 0), 'output_tokens', coalesce(sum(u.output_tokens), 0),
        'cache_read_tokens', coalesce(sum(u.cache_read_tokens), 0), 'cache_write_tokens', coalesce(sum(u.cache_write_tokens), 0))
      from private.support_chat_usage u where u.created_at > now() - interval '30 days'))
    from private.support_chat_settings s where s.singleton);
end $$;
revoke all on function private.get_support_chat() from public, anon;
grant execute on function private.get_support_chat() to authenticated;
create function public.admin_get_support_chat()
returns jsonb language sql stable security invoker set search_path = '' as $$
  select private.get_support_chat()
$$;
revoke all on function public.admin_get_support_chat() from public, anon;
grant execute on function public.admin_get_support_chat() to authenticated;

create function private.set_support_chat(p_settings jsonb)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare s jsonb := coalesce(p_settings, '{}'::jsonb);
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  if jsonb_typeof(s) <> 'object' or exists (select 1 from jsonb_each(s) e where jsonb_typeof(e.value) = 'null') then
    raise exception 'invalid_preferences' using errcode = '22023';
  end if;
  update private.support_chat_settings set
    enabled = coalesce((s->>'enabled')::boolean, enabled),
    daily_limit_per_user = coalesce((s->>'daily_limit_per_user')::integer, daily_limit_per_user),
    knowledge = coalesce(s->>'knowledge', knowledge),
    updated_at = now(), updated_by = private.requesting_uid()
  where singleton;
  if exists (select 1 from private.support_chat_settings where singleton and enabled and btrim(knowledge) = '') then
    raise exception 'support_chat_knowledge_required' using errcode = '22023';
  end if;
  return private.get_support_chat();
end $$;
revoke all on function private.set_support_chat(jsonb) from public, anon;
grant execute on function private.set_support_chat(jsonb) to authenticated;
create function public.admin_set_support_chat(p_settings jsonb)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.set_support_chat(p_settings)
$$;
revoke all on function public.admin_set_support_chat(jsonb) from public, anon;
grant execute on function public.admin_set_support_chat(jsonb) to authenticated;

-- Usage rows leave with the account, like chat messages and cancellation records.
create or replace function private.profiles_before_delete_cleanup()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  delete from public.ride_messages where sender_id = old.id;
  delete from private.ride_cancellations where user_id = old.id;
  update private.ride_cancellations set reported_user_id = null where reported_user_id = old.id;
  delete from private.cancellation_blocks where user_id = old.id;
  delete from private.support_chat_usage where user_id = old.id;
  return old;
end $$;
