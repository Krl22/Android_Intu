# Intu 1.28 (29)

## Cambios

- Cuenta → Panel de administración → **Modo oscuro**. Opcional, desactivado inicialmente y guardado por cuenta en ese dispositivo. Cambia la apariencia al instante y permite volver al modo claro.
- Mapas oscuros para pasajero, conductor, catálogo, direcciones guardadas, simulación de ubicación y detalles de viaje. Escala oculta, logo/atribución conservados, zoom centrado en el pin durante selección.
- Paleta grafito/turquesa y textos/superficies adaptados en Inicio, Cuenta, Viajes, elección de motos, panel admin, herramientas de cuenta y formularios. Se conservan los colores originales al desactivar el modo oscuro.
- Cambiar el estilo del mapa conserva recojo, destino, ruta, cámara y opción de moto. Se evita reiniciar la suscripción de ubicación durante la recarga del estilo.
- Incluye la funcionalidad coordinada de actualizaciones: comprobación al abrir la app y desde Cuenta, aviso con descarga opcional y aplazamiento, sin ofrecer una versión anterior ni interrumpir un viaje activo. Ver `APP_UPDATES.md`.
- Publicación mediante `scripts/publish-apk.ps1`: APK inmutable por versionCode, comprobación de SHA descargada, actualización de alias y metadata al final. Los usuarios de 1.28 podrán recibir los avisos de versiones posteriores.

## Verificación

- 58 pruebas JVM aprobadas; lint sin errores, 77 advertencias y 11 sugerencias. Dos advertencias añadidas recomiendan extensiones KTX para SharedPreferences/Uri.
- Primera revisión nativa: OK (23 tests), 150.449 s. Modo oscuro, cuatro casos del aviso de actualización, panel admin, preferencias de notificaciones, drawer, herramientas de cuenta y flujo completo de recojo/solicitud con fixtures aislados.
- Revisión posterior: OK (10 tests), 40.732 s. Ruta entre puntos separados más de 100 m, cámara y pin conservados al cambiar entre claro/oscuro; selección Honda y drawer reducido preservados. Picker oscuro con zoom real, detalle de ruta almacenada, preferencia por cuenta y actualización.
- Texto real de Android al 180 %: OK (4 tests), 16.948 s. Toggle admin, acciones de actualización y editor de lugares con teclado/reintento. Configuración original restaurada.
- La inspección visual detectó colores de acciones con poco contraste en Cuenta y botones destructivos del panel; se adaptaron a los colores semánticos oscuros y se reconstruyó para entrega.
- APK definitivo: OK (10 tests), 46.567 s, con font_scale real 1.8; OK (2 tests), 16.192 s, con font_scale 1.0, para captura de ruta y panel admin. Se comprobó la configuración efectiva de Android, se restauró 1.0 y se sincronizó antes de cerrar el emulador. Las capturas finales muestran texto, botones y pin legibles.
- Firma debug habitual y alineación de 16 KB verificadas. Certificado SHA-1: 7a4ec69cae01b4b8a5eeeff832d833e176c0efd0. Hashes de app/src/main y app/build.gradle.kts comparados tras build y antes de publicación.
- Las capturas son nativas de Phone_1 API 37. Datos, GPS y rutas de prueba aislados, sin crear cuentas, viajes, SMS ni reportes reales.

## Entrega

- Publicada el 2 de octubre de 2026, 22:25:27.712 UTC (18:25 en Nueva York), vía el helper de publicación. Su parser admite las etiquetas sdkVersion/minSdkVersion de aapt2 36 y 37; ambas se verificaron con el APK real.
- Descarga fija: https://viajaconintu.pages.dev/descargar?versionCode=29. Alias actual y metadata API también anuncian 1.28 (29).
- Tamaño: 106550563 bytes. SHA-256: DFFDF45D4DD6C5AD475D9D972B6230B238733DAF5BFC298391BB74FEAA082800. Tanto la descarga versionada como la descarga actual completa coinciden con el APK probado.
- Emulador cerrado. El teléfono físico no está conectado por ADB; reinstalación pendiente, sin afirmar instalación allí.

Evidencia y capturas: `build/qa-dark-mode-1.28`. Uso y arquitectura: `DARK_MODE.md`.
