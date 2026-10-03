# Intu 1.25 — ubicación de prueba elegida en el mapa

Versión 1.25 / código 26. Publicada el 2 de octubre de 2026 a las 13:44 (America/New_York), 17:44:26.051 UTC.

## Comportamiento

- Administración → Simular mi ubicación → Elegir en el mapa abre el selector con un marcador fijo. Empieza en la simulación actual; si no hay una, intenta obtener el GPS real y usa Satipo como alternativa.
- Arrastrar el mapa elige un punto. Simular aquí confirma las coordenadas exactas bajo el marcador, verifica nuevamente el permiso de administrador y vuelve al inicio. Volver o cerrar el mapa conserva la ubicación anterior.
- La ubicación se aplica al mapa, búsqueda, recojo y coordenadas publicadas por el conductor conectado. Mantiene los latidos de disponibilidad aun sin movimiento del GPS real. El conductor conserva los requisitos normales de aprobación, vehículo y conexión para recibir solicitudes.
- Se conservan los botones de Satipo y Río Negro. GPS real elimina la simulación. La ubicación elegida permanece fija hasta cambiarla o desactivarla; se elimina al cerrar sesión, cambiar de cuenta, perder el permiso de administrador o terminar el proceso. No modifica el GPS de otras aplicaciones.
- El selector comparte la configuración de mapas de Intu: escala oculta, logo/atribución visibles y zoom alrededor del pin central.
- Se conservaron los cambios anteriores del drawer, tarifas y flujo de recojo. No requiere cambios de esquema o migraciones del backend.

## Verificación

- Compilación de app, APK de pruebas, 48 pruebas JVM y lint completados. JVM: 0 fallos / 0 errores. Lint: 0 errores, 75 advertencias y 10 sugerencias existentes.
- Pruebas de lógica incluyen coordenadas personalizadas con precisión completa, rechazo de coordenadas inválidas/no administradores, vinculación a la sesión y publicación/restauración de GPS del conductor.
- Cuatro pruebas nativas distintas completadas: selección/confirmación/cancelación/GPS real en el nuevo selector; puck/cámara personalizados que ignoran GPS de Lima; actualización y cancelación de búsquedas al cambiar ubicación; zoom centrado y desplazamiento con gestos reales de Mapbox.
- En la primera ejecución, un ANR de Pixel Launcher bloqueó el foco de la nueva prueba. Las otras tres pasaron. Tras cerrar el diálogo del emulador se ajustó la sincronización de la prueba para esperar la proyección nativa después del cambio de tamaño del mapa al terminar de cargar; la repetición final pasó (OK 1 test, 12.793 s). Los registros de las ejecuciones se conservaron. No se modificaron las escalas de animación del emulador.
- Pruebas de pantalla en Phone_1, API 37, con coordenadas y callbacks aislados. No se generaron pedidos ni SMS reales. El emulador se cerró al finalizar.
- Firma debug y alineación de 16 KB verificadas. Certificado SHA-1: 7a4ec69cae01b4b8a5eeeff832d833e176c0efd0. SHA-256 del certificado: 7ee48b402d20042fcfa7b0db98b91fcae410d12efe7d9f9f3dd8f4be9968fa0d.
- Hashes de app/src/main comprobados después del build y antes de entregar: sin cambios posteriores en producción.

## Entrega

- Instalado mediante adb install -r en Samsung SM_F976U, conservando datos. Confirmada versión 1.25 / código 26; lastUpdateTime del dispositivo 2026-10-02 12:44:04 (Perú). No se hicieron pruebas interactivas en el teléfono.
- APK publicado en R2 intu-apk/intu.apk. Descarga: https://viajaconintu.pages.dev/descargar?v=1.25-26-admin-map.
- Descargado el archivo completo desde la web: coincide con el APK validado, 105289443 bytes. SHA-256: 1343392016895C03DF8A7DFD837CAC94243B53C58E75A920784B9394E4F2552D.

Evidencia y APK: build/qa-admin-map-1.25, incluyendo build-results.txt, unit-results.json, lint-summary.json, native-results.txt, native-picker-retry.txt, native-picker-final.txt y web-verification.json.

Para probar con otro tester: el administrador elige el punto y pulsa Simular aquí. Si actuará como conductor, debe estar aprobado y conectarse; el otro tester pide un viaje con un recojo cercano y un vehículo compatible. Para cambiar de posición, se elige nuevamente otro punto desde Administración. GPS real termina la simulación.
