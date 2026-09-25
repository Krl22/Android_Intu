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
| `vehicle_types` | Tipos de vehículo y tarifas. Hoy solo `mototaxi` |
| `driver_locations` | Última ubicación y disponibilidad de cada conductor (PostGIS) |
| `rides` | Solicitudes y viajes: `searching → accepted → arrived → in_progress → completed` / `cancelled` |
| `device_tokens` | Tokens de FCM para notificaciones push |

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
| `cancel_ride(ride_id, reason?)` | ambos | Pasajero: cancela. Conductor: la solicitud vuelve a `searching` |
| `rate_ride(ride_id, rating)` | ambos | Calificar 1–5 al otro, una vez |
| `register_device_token(token, platform?)` | ambos | Guardar el token de FCM |

Tiempo real:
- **Postgres Changes** en `rides`: el pasajero escucha su viaje; los conductores aprobados escuchan solicitudes nuevas.
- **Broadcast privado** en el canal `ride:<id>`: ubicación en vivo del conductor durante el viaje (solo los 2 participantes).

Automático (pg_cron, cada minuto):
- `expire-stale-ride-requests`: solicitudes sin conductor por más de 5 min se cancelan (`cancelled_by = 'system'`).
- `dev-simulate-test-drivers`: mueve a los conductores de prueba (solo desarrollo).

## Seguridad (resumen)

- Ningún usuario lee perfiles, vehículos ni ubicaciones de otros. Los datos del otro participante van copiados en `rides`, y el teléfono del pasajero solo se copia cuando un conductor acepta.
- La app no puede cambiar `status`, precios ni `driver_id` de un viaje directamente: solo con las funciones RPC, que validan cada transición.
- Un conductor no puede aprobarse a sí mismo. Si cambia sus documentos, vuelve a `pending`.
- Si el login fue por teléfono, el número del perfil se toma del token verificado de Firebase.
- Sin sesión (rol `anon`) no se puede leer ni ejecutar nada.

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
   firebase deploy --only functions
   ```
   Después, en la consola de Firebase → Authentication → Settings → **Blocking functions**, verifica que
   `beforecreated` y `beforesignedin` estén asignadas.
   Los usuarios que ya existían obtienen el rol en su próximo inicio de sesión.

4. **Hacer privado el Broadcast**
   Dashboard de Supabase → Realtime → Settings → desactivar *Allow public access*.
   Enlace directo: https://supabase.com/dashboard/project/vkguzpciwpfvaeyedepl/realtime/settings

5. **Llave de la app**
   Dashboard de Supabase → Project Settings → API Keys → copia (o crea) la **publishable key**.
   Esa llave y la URL van en la app. Son públicas por diseño: la seguridad la ponen las reglas RLS.

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
