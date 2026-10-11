# Supabase – backend de Intu

Datos, tiempo real y reglas de seguridad viven en Supabase (Postgres + PostGIS).
El login y las notificaciones push siguen en **Firebase** (Auth + FCM).
Supabase acepta los tokens de Firebase mediante **Third-Party Auth**.

```
App Android ── login ──► Firebase Auth (teléfono, Google, MFA)
     │                          │ token (JWT con role=authenticated)
     └──── datos/tiempo real ───┴──► Supabase (Postgres + PostGIS + Realtime)
```

- Proyecto de desarrollo: `vkguzpciwpfvaeyedepl` (región us-east-2)
- URL: `https://vkguzpciwpfvaeyedepl.supabase.co`
- Proyecto de Firebase: `intu-e8403`

## Estructura

| Archivo | Qué es |
|---|---|
| `migrations/*_init_schema.sql` | Tablas, reglas RLS, funciones RPC, Realtime y cron |
| `migrations/*_rides_policy_and_indexes.sql` | Ajuste de rendimiento de políticas e índices |
| `seed.sql` | Datos **de prueba**: 12 mototaxistas en Lima que se mueven solos cada minuto |
| `../firebase/functions/` | Función de Firebase que agrega `role: authenticated` al token |

## Modelo de datos

| Tabla | Para qué |
|---|---|
| `profiles` | Un perfil por usuario de Firebase (`id` = uid de Firebase, **texto**) |
| `drivers` | Datos de conductor: DNI/CE, brevete, `status` (`pending` → `approved`), rating |
| `vehicles` | Vehículo del conductor (placa, marca, SOAT). Uno activo por conductor |
| `vehicle_types` | Tipos de vehículo y tarifas independientes de mototaxi y envío en moto |
| `driver_locations` | Última ubicación y disponibilidad de cada conductor (PostGIS) |
| `rides` | Solicitudes y viajes: `searching → accepted → arrived → in_progress → completed` / `cancelled` |
| `device_tokens` | Tokens de FCM para notificaciones push |
| `ride_messages` | Chat del servicio; solo lo leen el pasajero y el conductor asignado (también por Realtime) |

### Funciones que llama la app (RPC)

| Función | Quién | Qué hace |
|---|---|---|
| `estimate_fare(vehicle_type, distance_m, duration_s)` | pasajero | Precio en soles (el mismo que se guarda al pedir) |
| `nearby_drivers(lat, lng, radius_m?, vehicle_type?, limit?)` | pasajero | Conductores disponibles cerca, ordenados por distancia |
| *insert en `rides`* | pasajero | Pedir viaje (el precio lo calcula el servidor) |
| `set_driver_location(lat, lng, heading?, is_available?)` | conductor | Publicar ubicación cada 10–30 s (solo aprobados) |
| `nearby_ride_requests(lat, lng, radius_m?)` | conductor | Solicitudes abiertas cerca |
| `accept_ride(ride_id)` | conductor | Aceptar (si dos aceptan a la vez, solo uno gana) |
| `advance_ride(ride_id, status)` | conductor | `arrived` → `in_progress` → `completed` |
| `ride_pin_requirement(ride_id)` | participantes | Consultar si su solicitud requiere PIN antes de iniciar |
| `admin_get_ride_security_settings()` / `admin_set_ride_security_settings(pin_enabled)` | admin | Activar o desactivar el PIN para nuevas solicitudes de viajes y envíos |
| `location_simulation_access()` / `admin_set_location_simulation(users_enabled)` | sesión / admin | Simulación solo para admins por defecto; un admin puede habilitarla para pasajeros y conductores sin darles permisos de admin |
| `admin_set_simulation_bar(enabled)` | admin | Mostrar u ocultar la barra de pruebas en su propia cuenta (oculta por defecto); se sincroniza entre dispositivos y no cambia el permiso global de usuarios |
| `get_fare_settings()` / `admin_set_fare_settings(settings)` | sesión / admin | Consultar tarifas vigentes; editar base, km, minuto, mínimo, recargo Honda, redondeo y permiso de propuestas |
| `get_driver_request_settings()` / `admin_set_driver_request_settings(request_order, timeout_seconds)` | sesión / admin | Pila de solicitudes del conductor: qué va al frente (`fare` = paga más, `distance` = más cercana) y segundos para decidir (10–120); al vencer se pasa sola solo para ese conductor |
| `propose_ride_price(ride_id, amount)` / `my_ride_price_offers(ride_id?)` | participantes | Conductor disponible propone otro precio si está habilitado; cada participante consulta solo sus ofertas |
| `respond_ride_price_offer(offer_id, accept)` | solicitante | Rechazar conserva la búsqueda; aceptar asigna atómicamente al conductor con el precio acordado |
| `cancel_ride(ride_id, reason?)` | ambos | Versión anterior; llama a `cancel_ride_with_reason` y guarda el motivo como `other` |
| `cancel_ride_with_reason(ride_id, reason, note?)` | ambos | Pasajero: cancela. Conductor: la solicitud vuelve a `searching`, salvo "no aparece" verificado (espera, distancia), que termina el viaje y da la falta al pasajero |
| `cancellation_preview(ride_id, reason)` | ambos | Antes de confirmar: si la cancelación cuenta, faltas, si causaría una pausa o cuánto falta para "no aparece" |
| `my_cancellation_status()` | ambos | Faltas y pausa activa como pasajero y como conductor |
| `admin_get_trip_policy()` / `admin_set_trip_policy(settings)` | admin | Estrellas visibles (conductor/pasajero) y reglas de cancelación; todo empieza desactivado |
| `admin_cancellation_overview(days?)` / `admin_lift_cancellation_block(user_id, role)` | admin | Quién cancela más, reportes contra la otra persona y quitar una pausa |
| `send_ride_message(ride_id, body?, quick_reply?)` | ambos | Chat del servicio (aceptado, llegó, en curso). El conductor solo escribe texto detenido en el recojo |
| `admin_recent_ride_chats(days?)` / `admin_ride_chat(ride_id)` | admin | Revisar chats de los últimos 30 días ante un reclamo |
| `schedule_ride(scheduled_for, …, contact?)` / `my_scheduled_rides()` / `cancel_scheduled_ride(id)` | pasajero | Viajes en mototaxi programados de 20 min a 7 días antes (máx. 3). El cron `dispatch-scheduled-rides` los convierte en solicitud 10 min antes actuando como el pasajero |
| `support_chat_status()` | ambos | Si el asistente de Ayuda está activo y cuántas preguntas quedan hoy |
| `support_chat_begin()` / `support_chat_finish(usage_id, usage, failed?)` | Edge Function `support-chat` (con el token del usuario) | Reserva una pregunta del cupo diario y entrega el texto de ayuda; luego guarda los tokens usados |
| `admin_get_support_chat()` / `admin_set_support_chat(settings)` | admin | Prender/apagar el asistente, límite diario, texto de ayuda y uso de 30 días |

### Asistente de Ayuda (Edge Function `support-chat`)

`supabase/functions/support-chat` responde con Claude Haiku 4.5 usando solo el texto de ayuda del admin
(borrador en `docs/support/ayuda-intu-borrador.md`). La conversación vive en el teléfono; Intu solo guarda cuántas
preguntas hizo cada cuenta y los tokens, para el límite diario y el costo estimado.

- Secreto: `supabase secrets set ANTHROPIC_API_KEY=...` (clave de la consola de Anthropic; se cobra del saldo de la API, no de una suscripción de Claude).
- Se despliega con `verify_jwt = false` porque los tokens son de Firebase: la función valida al usuario al llamar `support_chat_begin` con su propio token.
| `rate_ride(ride_id, rating)` | ambos | Calificar 1–5 al otro, una vez |
| `register_device_token(token, platform?)` | ambos | Guardar el token de FCM (se borra al cerrar sesión) |
| `admin_set_admin(user_id, is_admin)` | admin | Dar o quitar admin; siempre queda al menos uno |
| `admin_delete_user(user_id)` | admin | Borrar la cuenta en Supabase; la llama la Cloud Function `adminDeleteUser`, que además borra el login de Firebase y la foto. Los viajes quedan como "Cuenta eliminada" |

Tiempo real:
- **Postgres Changes** en `rides`: el pasajero escucha su viaje; los conductores aprobados escuchan solicitudes nuevas.
- **Broadcast privado** en el canal `ride:<id>`: ubicación en vivo del conductor durante el viaje (solo los 2 participantes).

Avisos push: el trigger `rides_notify_status` llama con pg_net a la Cloud Function `ridePush` cuando un viaje
cambia de estado (aceptado, llegó, iniciado, terminado, cancelado, o el conductor lo soltó). La función los envía
por FCM y borra los tokens vencidos con `prune_device_tokens`. Ambos lados comparten el secreto `push_webhook_secret`.

Automático (pg_cron, cada minuto):
- `expire-stale-ride-requests`: solicitudes sin conductor por más de 5 min se cancelan (`cancelled_by = 'system'`).
- `purge-ride-messages` (diario): borra los mensajes del chat con más de 30 días.
- `dev-simulate-test-drivers`: mueve a los conductores de prueba (solo desarrollo).

## Seguridad (resumen)

- Ningún usuario lee perfiles, vehículos ni ubicaciones de otros. Los datos del otro participante van copiados en `rides`, y el teléfono del pasajero solo se copia cuando un conductor acepta.
- La app no puede cambiar `status`, precios ni `driver_id` de un viaje directamente: solo con las funciones RPC, que validan cada transición.
- Un conductor no puede aprobarse a sí mismo. Si cambia sus documentos, vuelve a `pending`.
- Si el login fue por teléfono, el número del perfil se toma del token verificado de Firebase.
- Sin sesión (rol `anon`) no se puede leer ni ejecutar nada, salvo `prune_device_tokens`, que exige el secreto de los avisos push.
- Las cuentas eliminadas quedan en `private.deleted_accounts`: aunque su app siga abierta, no pueden volver a crear el perfil.

## Configuración pendiente (una sola vez)

1. **Conectar Firebase con Supabase**
   Dashboard de Supabase → Authentication → **Third-Party Auth** → Add → Firebase → Project ID: `intu-e8403`.
   Enlace directo: https://supabase.com/dashboard/project/vkguzpciwpfvaeyedepl/auth/third-party

2. **Activar Identity Platform en Firebase** (necesario para las funciones de abajo y para MFA)
   Consola de Firebase → Authentication → Settings → *Upgrade to Firebase Authentication with Identity Platform*.
   Requiere el plan Blaze, que igual es necesario para el login por teléfono.

3. **Desplegar la función que agrega el rol al token**
   ```bash
   npm install -g firebase-tools
   firebase login
   cd firebase/functions && npm install && cd ..
   firebase deploy --only functions:beforecreated,functions:beforesignedin
   ```
   Despliega siempre por nombre: `--only functions` a secas propone borrar las funciones antiguas del proyecto.
   Después, en la consola de Firebase → Authentication → Settings → **Blocking functions**, verifica que
   `beforecreated` y `beforesignedin` estén asignadas.
   Los usuarios que ya existían obtienen el rol en su próximo inicio de sesión.

4. **Hacer privado el Broadcast**
   Dashboard de Supabase → Realtime → Settings → desactivar *Allow public access*.
   Enlace directo: https://supabase.com/dashboard/project/vkguzpciwpfvaeyedepl/realtime/settings

5. **Llave de la app**
   Dashboard de Supabase → Project Settings → API Keys → copia (o crea) la **publishable key**.
   Esa llave y la URL van en la app. Son públicas por diseño: la seguridad la ponen las reglas RLS.

6. **Avisos push y eliminar cuentas**
   Genera un valor largo al azar y guárdalo en los dos lados:
   - Firebase (desde `firebase/`): `firebase functions:secrets:set PUSH_WEBHOOK_SECRET` y pega el valor.
   - Supabase → SQL Editor: `select vault.create_secret('<valor>', 'push_webhook_secret');`

   Luego despliega las funciones: `firebase deploy --only functions:ridePush,functions:adminDeleteUser`.
   Para cambiar el secreto: `select vault.update_secret((select id from vault.secrets where name = 'push_webhook_secret'), '<nuevo>');`,
   vuelve a correr `secrets:set` y despliega de nuevo `ridePush`.

## Tareas comunes

**Aprobar a un conductor** (SQL Editor del dashboard):
```sql
update public.drivers set status = 'approved', approved_at = now() where id = '<uid de Firebase>';
```

**Reiniciar los datos de prueba:** correr `seed.sql` completo en el SQL Editor.

**Apagar la simulación de conductores:**
```sql
select cron.unschedule('dev-simulate-test-drivers');
```

**Agregar otro tipo de vehículo** (p. ej. auto):
```sql
insert into public.vehicle_types (code, name, base_fare, per_km, per_minute, min_fare, max_passengers)
values ('car', 'Auto', 4.00, 1.50, 0.20, 6.00, 4);
```

**Mover el backend a otra región (p. ej. São Paulo):** crear el proyecto nuevo, aplicar las migraciones
en orden (`supabase db push` con la CLI, o pegarlas en el SQL Editor) y correr `seed.sql`.
