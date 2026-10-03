# Modo oscuro opcional

## Uso

En Cuenta → Apariencia, activar **Modo oscuro**. Disponible para cualquier usuario, incluidos pasajeros y conductores. El cambio aplica inmediatamente a la app y a sus mapas. Desactivarlo devuelve la apariencia clara.

La preferencia empieza desactivada y se guarda por UID de Firebase en SharedPreferences del dispositivo. Se conserva al cerrar/reabrir la app o instalar una actualización conservando los datos. Otra cuenta en el mismo dispositivo tiene su propia elección; cerrar sesión devuelve las pantallas de acceso al modo claro. No cambia las preferencias de otros testers ni depende del tema de Android.

El interruptor aparece en Cuenta sin depender de permisos de administrador ni del estado del perfil de conductor. Sustituye al control de prueba del panel de administración. No requiere migraciones ni cambios en Firebase, Supabase o FCM.

## Implementación

- `IntuAppearanceHost` observa la cuenta, carga su preferencia y proporciona `LocalIntuDarkMode` a la composición. Se mantienen las preferencias originales del modo claro.
- Paleta grafito con acentos turquesa, texto claro, superficies y campos adaptados. Incluye Inicio, Cuenta, Viajes, selección de motos, herramientas de cuenta, formularios de lugares, panel admin y tarjetas de conductor.
- `intuMapStyle()` selecciona Mapbox Streets o Dark para Inicio, conductor, catálogo, ubicación simulada/direcciones guardadas (usan el mismo picker) y detalle de viajes.
- Inicio y conductor recargan el estilo sobre el mismo MapView, sin reiniciar los estados de recojo/destino ni la suscripción GPS. Los managers de anotaciones conservan sus capas persistentes; el SDK 11.16.4 conserva las fuentes e imágenes asociadas al cambiar de estilo.
- Se conserva la configuración común de los mapas: escala oculta, logo/atribución visibles y zoom centrado en el pin cuando se elige un punto.

## QA

`DarkModeTest` usa cuentas de preferencia ficticias y GPS/rutas aislados. Comprueba preferencia opt-in e independencia entre cuentas, interruptor en Cuenta sin acceso admin tanto en modo pasajero como conductor, cambio de estilo sobre una ruta preparada sin perder cámara/destino/moto, drawer reducido, zoom del picker oscuro y detalle de ruta almacenada. También revisa el aviso de actualización en modo oscuro.

Las capturas nativas y los resultados de la versión 1.28 se guardan en `build/qa-dark-mode-1.28`. Consultar `RELEASE_1_28.md` para la evidencia final y el hash publicado.

### Control en Cuenta · 3 de octubre de 2026

- Compilación debug y APK de pruebas correctas.
- Dos pruebas instrumentadas aprobadas: persistencia e independencia por cuenta; control de Cuenta disponible sin acceso admin en las vistas de pasajero y conductor, con cambio inmediato de paleta en ambos sentidos.
- APK reinstalado en el Samsung con `-r`, conservando los datos.
