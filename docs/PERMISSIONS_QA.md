# Permisos de inicio — Intu 1.40 (41)

## Cambio

Inicio solicita ubicación y después notificaciones mediante una única cola en la actividad.
Las solicitudes no dependen del mapa ni del registro de tokens push. Pasajero, conductor y
el selector de puntos guardados comparten el permiso de ubicación. Una respuesta vacía es una
cancelación: queda pendiente hasta regresar al primer plano, sin registrarse como rechazo.

Las respuestas se conservan entre aperturas para evitar repetir permisos rechazados. Estas
preferencias se excluyen del backup y de la transferencia entre dispositivos. Al volver de
Ajustes se leen los permisos actuales; el interruptor de GPS también se observa en primer plano.
Inicio ofrece «Activar ubicación» o «Activar GPS» cuando corresponde. Una denegación permanente
solo abre los ajustes de la app cuando el usuario solicita explícitamente activar su ubicación.

## Validación local

```powershell
.\gradlew-local.cmd :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain
```

`PermissionRequestQueueTest` cubre el orden, denegaciones, cancelaciones, primer plano,
solicitudes manuales, permisos ya concedidos y simulación. `StartupPermissionsTest` verifica
los diálogos nativos de ubicación y notificaciones sin cargar ningún mapa.

En esta revisión, la suite completa ejecutó 123 pruebas: 121 pasaron y dos pruebas de
`SupabaseRealtimeTest` agotaron los 20 segundos de espera del servidor real. Se conservan
como pruebas de integración; ese resultado no equivale a un fallo del flujo de permisos.
Para comprobar la cola sin depender de esa conexión, usa `:app:testDebugUnitTest --tests
com.intu.taxi.PermissionRequestQueueTest`. Las pruebas de diálogos nativos deben ejecutarse
en el emulador; compilarlas por sí solo no valida su comportamiento.

## Prompt para un agente de QA

Revisa el cambio de permisos de Intu en `Krl22/Android_Intu`, rama `main`, versión 1.40
(versionCode 41). Usa el emulador de Android Studio; no uses teléfonos físicos ni borres sus datos.
Lee `docs/DEV_SETUP.md` y este documento. Verifica el commit probado y el estado de Git antes
de actualizar el repositorio; conserva cualquier cambio local ajeno.

Compila el APK debug y las pruebas. Usa un emulador dedicado con Android 13 o posterior.
Puedes reinstalar o limpiar únicamente la instalación del emulador dedicado para repetir un
primer inicio. No concedas los permisos automáticamente ni actives la ubicación simulada
de Intu: debes comprobar los diálogos reales de Android y el GPS del emulador.

Ejecuta `com.intu.taxi.StartupPermissionsTest` por separado, con ubicación y notificaciones
revocadas y sin flags `user-set`/`user-fixed` antes de iniciar instrumentation. Confirma que
se ejecutó una prueba y que no quedó omitida por sus precondiciones. La prueba acepta
ubicación y rechaza notificaciones; no monta Mapbox ni necesita iniciar sesión.

Para ejecutarla, sustituye el serial por el del emulador dedicado que observaste en
`adb devices`. Todos los comandos que modifican una instalación deben llevar ese serial.

```powershell
$qaAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$qaEmulatorSerial = 'SERIAL_DEL_EMULADOR_DEDICADO'
& $qaAdb -s $qaEmulatorSerial install -r app/build/outputs/apk/debug/app-debug.apk
& $qaAdb -s $qaEmulatorSerial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
foreach ($qaPermission in @('ACCESS_FINE_LOCATION','ACCESS_COARSE_LOCATION','POST_NOTIFICATIONS')) {
    & $qaAdb -s $qaEmulatorSerial shell pm revoke com.intu.taxi "android.permission.$qaPermission"
    & $qaAdb -s $qaEmulatorSerial shell pm clear-permission-flags com.intu.taxi "android.permission.$qaPermission" user-set user-fixed
}
& $qaAdb -s $qaEmulatorSerial shell am instrument -w -e class com.intu.taxi.StartupPermissionsTest com.intu.taxi.test/androidx.test.runner.AndroidJUnitRunner
```

Después prueba la app completa con una cuenta de QA y perfil completo. Si no tienes acceso
a una cuenta de prueba, reporta ese bloqueo y ejecuta las verificaciones independientes
del login; no uses cuentas personales ni crees viajes, envíos o avisos a usuarios reales.

Comprueba estos escenarios, reiniciando solo el emulador dedicado entre los casos que
requieren ambos permisos pendientes:

1. Primer ingreso a Inicio: ubicación aparece antes de notificaciones. Mantén cada diálogo
   abierto unos segundos. Debe aparecer el siguiente sin cambiar de pestaña.
2. Acepta ubicación precisa y notificaciones. Inyecta coordenadas desde Extended Controls
   del emulador y confirma que la app obtiene esa ubicación.
3. Acepta solo ubicación aproximada. La app debe seguir funcionando.
4. Rechaza ubicación: notificaciones debe aparecer igualmente. Navega por Inicio, Viajes
   y Cuenta y reinicia la app; no deben repetirse automáticamente los permisos rechazados.
5. Rechaza notificaciones y acepta ubicación: el GPS debe funcionar igualmente.
6. Pulsa «Activar ubicación» después de un rechazo. Prueba también una denegación
   permanente: la acción debe abrir Ajustes. Concede el permiso allí y vuelve; Inicio
   debe actualizarse sin cambiar de pantalla.
7. Con permiso concedido, apaga el GPS: debe ofrecer «Activar GPS». Enciéndelo y vuelve;
   comprueba recuperación de ubicación sin navegar a otra pestaña.
8. Rota el emulador o manda la app al fondo durante un diálogo: verifica que no se
   superpongan solicitudes ni quede bloqueada la secuencia.
9. Mantén una sesión de QA iniciada, restablece solo los permisos y sus preferencias
   locales en el emulador dedicado, y bloquea la carga de Mapbox o corta la red antes
   de abrir Inicio. La solicitud de ubicación debe aparecer aunque el mapa no cargue.
10. Con una cuenta de conductor de QA, comprueba el mismo flujo y el regreso de Ajustes.
    Valida «A chambear» únicamente en un entorno de pruebas sin usuarios reales.
11. Si hay una imagen Android 12 disponible, comprueba que solicita ubicación y no
    intenta mostrar un permiso de notificaciones que ese Android no requiere.

Captura pantallas y logs. Busca específicamente `Can request only one set of permissions at a time`,
`SecurityException`, crashes y solicitudes duplicadas. No incluyas tokens, códigos SMS ni datos
personales en el informe. Entrega una tabla con escenario, resultado, evidencia, versión Android
y commit probado; distingue las pruebas pasadas, fallidas y bloqueadas. No hagas cambios ni
publiques versiones durante esta revisión.
