# Intu 1.11 QA — ubicación de prueba para administradores

Versión 1.11, código 12, con la firma debug habitual de QA.

## Uso

1. Entrar con una cuenta admin y abrir Cuenta → Administración.
2. Pulsar **Simular mi ubicación** y elegir **Satipo** o **Río Negro**. La app vuelve a Inicio.
3. Se muestra un aviso **Ubicación de prueba** en todas las pantallas. El mapa, el marcador de ubicación, las búsquedas y el recojo usan el punto elegido.
4. En modo conductor, al conectarse se envía el mismo punto al backend, incluso con la app minimizada. No se cambia la aprobación del conductor ni se conecta automáticamente.
5. Pulsar **GPS real** en el aviso o **Usar GPS real** en el panel para restaurar la ubicación del celular.

La simulación representa un punto fijo; no simula desplazamiento por la ruta. Es temporal y no se guarda en preferencias: se pierde al cerrar sesión, cambiar de cuenta o reiniciar el proceso. La autorización admin se verifica con el RPC existente `is_admin` antes de cada activación y cada 60 segundos mientras está activa; una negativa o un fallo de verificación cancela el override.

Satipo usa el centro ya definido en la app (-11.2521, -74.6382). Río Negro usa un punto aproximado junto a la municipalidad (-11.2088663, -74.6594027), consultado en la [API de OpenStreetMap, way 956665921](https://api.openstreetmap.org/api/0.6/way/956665921/full.json). La [ficha del Mincetur](https://consultasenlinea.mincetur.gob.pe/fichaInventario/index.aspx?cod_Ficha=13411) confirma la plaza central frente a la municipalidad. No se presenta como un punto de recojo verificado del catálogo.

## Implementación

- Override dentro de Intu, sin modificar el GPS del sistema u otras apps.
- El mapa sustituye el proveedor de ubicación; al restaurar GPS vuelve a suscribir el proveedor real y centra la cámara una sola vez.
- El servicio del conductor resuelve la ubicación al enviar, serializa las solicitudes y conserva los intervalos de 2 segundos en viaje y 10 segundos en línea sin viaje. El heartbeat continúa aunque el punto simulado no se mueva.
- El mapa del conductor carga su estilo una sola vez y desuscribe el proveedor al salir de la pantalla.
- Cambiar la zona actualiza las búsquedas existentes, cancela respuestas de la zona anterior y limpia los borradores de rutas sin cambiar viajes ya solicitados.
- No se modifica el esquema ni los permisos de Supabase.

## Validación

- 31 pruebas JVM aprobadas, incluidas autorización, cambio de cuenta, cierre de sesión, revocación de permisos, heartbeat fijo y solicitudes en cola que resuelven la nueva ubicación.
- 3 pruebas instrumentadas aprobadas con el APK corregido en API 37 / páginas de memoria de 16 KB: Satipo → Río Negro → GPS real (modelo, cámara y puck); cambio de zona con la misma búsqueda y cancelación de respuestas anteriores; zoom centrado en el pin, desplazamiento y redimensionamiento.
- La prueba detectó y permitió corregir un caso en que el SDK emitía la posición anterior del puck al restaurar GPS. La app ahora recibe coordenadas del proveedor, independientemente de esa animación.
- Lint del APK final: 0 errores, 72 avisos y 10 sugerencias del proyecto. Compilación del APK y del APK de pruebas aprobada. Firma debug y alineación ZIP de 16 KB verificadas.
- El control de permisos existente se comprobó también en Supabase: sin identidad autenticada devuelve `false`; con una cuenta admin existente y rol `authenticated`, devuelve `true`. Consulta de verificación sin cambios persistentes.
- La prueba del mapa usa un estilo local vacío y un GPS de Lima controlado; las pruebas del emisor usan un destino de envío en memoria. No se despacharon viajes reales durante estas comprobaciones.
- Evidencia: `%LOCALAPPDATA%/Temp/intu-qa-1.11/`, incluyendo `instrumentation-fixed.log`, `qa-location-satipo.png` y `qa-location-rio-negro.png`.
- APK corregido reinstalado en el Samsung por ADB inalámbrico, conservando los datos y la sesión. Versión 1.11 / código 12 comprobada y `MainActivity` abierta; el celular estaba bloqueado durante la instalación.
- Archivo: `app/build/outputs/apk/debug/app-debug.apk`, 105,906,379 bytes.
- SHA-256: `976B869FF71D9483389CD80983E7ED409F76E2598AF9DFE7457A8DA49D44F101`.
- Publicado en R2 el 2026-10-01 a las 15:55:09 UTC. La descarga completa desde [Cloudflare Pages](https://viajaconintu.pages.dev/descargar?v=1.11-qa-12) coincide en tamaño y SHA-256 y confirma versión 1.11 / código 12.
- La descarga verificada de Intu 1.10 permanece guardada localmente para recuperación.
