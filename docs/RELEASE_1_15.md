# Intu 1.15 QA — direcciones guardadas y punto de recojo

Versión 1.15, código 16, con el certificado debug habitual. Instalación y publicación del snapshot registradas abajo.

## Cambios

- Los atajos de Casa, Trabajo y favoritos en Inicio utilizan el punto guardado como destino y preparan la ruta para abrir Elige tu viaje, sin pasar por Confirmar destino.
- La selección manual de un destino mantiene su confirmación en el mapa.
- Confirmar viaje abre el selector de recojo después de elegir mototaxi. El botón final Solicitar viaje recalcula ruta y tarifa desde ese punto y envía la solicitud. Volver conserva las opciones y el destino.
- Incluye el ajuste de inicio del pin al guardar un lugar: ubicaciones nuevas toman GPS o el preset administrativo, las ediciones conservan su punto y un fallo de ubicación ofrece selección manual.
- Los atajos esperan la ubicación con una pantalla de preparación cancelable si todavía no llegó. Un fallo de ruta permite reintentar y no solicita un taxi. La selección manual conserva su confirmación.
- El selector administrativo y el editor de lugares conservan su botón habitual; Solicitar viaje es específico del recojo del pedido.

## Coordinación

Ambos cambios del flujo de Inicio se integran en una única edición de HomeScreen. Esta entrega incrementa la versión para distinguirla del APK 1.14 publicado y de los builds locales posteriores que conservaron ese mismo número.

## Verificación

- Compilación debug y APK de pruebas aprobadas. Las 35 pruebas JVM del build de integración pasaron, sin fallos.
- Lint final: 0 errores, 72 avisos y 10 sugerencias.
- La sesión de direcciones ejecutó HomeRideFlowTest: aprobado con callbacks de ruta y solicitud locales, sin solicitar viajes reales. Verificó orden destino / mototaxi / confirmar viaje / recojo, cancelación del recojo, conservación del destino, origen del pin y recálculo de distancia, duración y tarifa; un envío simulado pendiente bloquea el duplicado y un error vuelve a opciones.
- Ese test nativo precedió al cambio únicamente textual del botón final a Solicitar viaje y a quitar un estado sin usos. El APK final y su test APK compilaron después de esos ajustes.
- SavedShortcutFlowTest contiene tres casos específicos de Casa, Trabajo y favorito: abrir Elige tu viaje sin confirmar destino, conservar coordenadas y no enviar solicitudes. Se comprobó compilación mediante assembleDebugAndroidTest incremental. Al publicar 1.15 su ejecución nativa quedó pendiente de confirmación de disponibilidad del celular; no se contó como prueba aprobada en esa entrega.
- Verificación posterior: tras la disponibilidad confirmada por Carlos en la sesión de Google Play, los tres SavedShortcutFlowTest pasaron sobre la versión 1.18 en el Samsung. Utilizan GPS, ruta y envío simulados; restauran las direcciones originales y comprueban cero solicitudes. El comportamiento de atajos de 1.15 se conserva. Registro: `%TEMP%/intu-qa-shortcuts-1.18/native-shortcut-tests.txt`.
- Firma debug y alineación ZIP de 16 KB verificadas.
- La sesión de direcciones reinstaló con `-r`, conservando datos, y abrió MainActivity. QA leyó el APK instalado y verificó que su SHA-256 coincide exactamente con el snapshot local 1.15 / código 16.
- Snapshot y APK de pruebas: `%TEMP%/intu-qa-shortcuts-1.15/`. No hubo cambios de esquema ni despliegues del backend.

## Publicación

- APK final: 105,124,500 bytes.
- SHA-256: `5BDFC28ABCC84C6EB31E851D52859C579711387998E2C6C702CF4E4437A8DAFF`.
- Publicado en R2 el 2026-10-01 a las 23:42:00 UTC.
- La descarga completa de [Intu 1.15](https://viajaconintu.pages.dev/descargar?v=1.15-qa-16) coincide con el APK instalado y el snapshot local en tamaño y SHA-256.
