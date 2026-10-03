# Intu 1.4 — Catálogo de lugares

Android 1.4, código 5. Publicado en Cloudflare R2 el 1 de octubre de 2026 a las 05:25:32 UTC. [Descargar APK QA](https://viajaconintu.pages.dev/descargar?v=1.4-qa-5).

## Cambios

- Administración → Lugares permite crear y editar nombres, alias, categorías, localidad y dirección, elegir el punto de recojo en el mapa y guardar como borrador, publicado o inactivo.
- Publicar requiere confirmar una entrada adecuada. Modificar las coordenadas desmarca la confirmación. Supabase también exige esta condición.
- Inicio y su buscador de marcador usan el catálogo descargado. La búsqueda acepta tildes y alias y funciona sin GPS. No consulta APIs externas por cada letra; ofrece elegir en el mapa cuando no encuentra un lugar.
- El catálogo se sincroniza por revisión, con comprobación al entrar a Inicio y caché de cinco minutos entre comprobaciones. «Actualizar lugares» obtiene los cambios inmediatamente. Una respuesta actualizada reemplaza toda la caché y elimina los lugares retirados; un fallo de conexión conserva los datos anteriores.
- Se aplicó `20261001044050_places_catalog.sql` en Supabase con 55 candidatos reales de OpenStreetMap del centro de Satipo. Todos permanecen en borrador y sin recojo verificado. El panel conserva la procedencia, coordenadas originales y atribución. Revisar entradas, duplicados y negocios desactualizados antes de publicar.
- Se mantiene Firebase Auth y el backend existente. Las funciones administrativas comprueban permisos en el servidor; los clientes solo leen publicados y no escriben directamente en la tabla.
- Se evitó un posible cierre de Inicio al quedarse temporalmente sin sesión: no se abre una colección de favoritos personales con un UID vacío.

## Validación

- Compilación `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` y `lintDebug` correcta. 20 pruebas JVM pasaron. Lint: 0 errores, 70 advertencias y 10 sugerencias.
- 13 pruebas distintas de Android pasaron en el emulador API 37. Incluyen caché aislada con pérdida de conexión y retiradas, publicación verificada, selección de un resultado, atribución, búsqueda desde Inicio y desplazamiento/confirmación del punto en MapView nativo, además de las pruebas previas de perfil, favoritos, reportes e historial.
- La prueba de Inicio usa un catálogo QA temporal y restaura la preferencia original. No publica lugares de prueba en Supabase. Las pruebas del servidor crean y modifican contenido dentro de una transacción revertida.
- `supabase/tests/places_catalog_permissions.sql` verificó denegación de administración/escritura a pasajeros y anónimos, privacidad de borradores, publicación verificada, revisión atómica, retiradas, coordenadas inválidas, conflictos de edición y protección de procedencia.
- Los avisos del asesor sobre RPC `SECURITY DEFINER` ejecutables por autenticados son intencionales: las funciones administrativas verifican `is_admin()` y la sincronización devuelve únicamente publicados. Las pruebas de permisos verifican estos límites.
- El teléfono conectado estaba bloqueado e impedía dibujar las actividades de prueba; se instaló el APK, y la comprobación visual se completó en el emulador. Su primer arranque tuvo un ANR de System UI; se cerró el diálogo, se ajustó la densidad de la pantalla y se repitieron las interacciones afectadas. El mapa se desplazó con un gesto nativo y se confirmó el cambio real de coordenadas.
- Se revisaron capturas de Inicio, editor y mapa. Las coordenadas QA y el nombre «Plaza QA» solo son fixtures de pruebas, no lugares publicados del catálogo.
- La firma y la alineación ZIP de 16 KB pasan. Se descargó el APK completo desde la web y coincide con el local en tamaño y SHA-256; la descarga confirma versión 1.4 / código 5.

## Artefacto

- Tamaño: 105,074,528 bytes.
- SHA-256: `237777DAFDDF80A31785A534EFEDD6F0FD782DAC467A53CC5A92FBB10875164B`.
- Firma: la misma clave debug de QA registrada en Firebase para 1.2 y 1.3. No necesita desinstalar esas versiones QA.
- Se conserva una copia verificada del APK 1.3 anterior para recuperación.

El ahorro corresponde al autocompletado del catálogo. Mapbox mantiene mapas, rutas y resolución de direcciones del marcador, y Supabase mantiene sus límites de almacenamiento y transferencia. Los favoritos personales y el catálogo compartido siguen separados. La prueba de un viaje completo entre dos teléfonos físicos sigue perteneciendo al guion general de QA.

Guía de uso, arquitectura y licencia de los datos: [catálogo de lugares](poi/README.md).
