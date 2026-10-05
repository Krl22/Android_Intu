# Intu 1.31 — entrega integrada del inicio comercial

Versión 1.31, código 32, package `com.intu.taxi`. Preparada el 5 de octubre de 2026.

## Cambios incluidos

- Inicio comercial con los gradientes turquesa e índigo, anuncios compactos y modo oscuro. Viajes y Cuenta usan el mismo diseño.
- Anuncios de negocios demo de Satipo y Río Negro, menús con imágenes, pedidos en moto y controles del panel administrador. El interruptor de negocios conserva disponible el courier normal.
- Carrusel con altura uniforme, precio junto al título y rotación de 3 segundos por defecto, configurable en el panel.
- Planificación al tocar la búsqueda: mapa, recojo editable, destino, sugerencias primero y lugares guardados. Casa y Trabajo aparecen cuando tienen una dirección guardada.
- Mototaxi, courier y pedidos para otras personas con sus contactos y puntos de recojo/entrega.
- Pin elevado seleccionado en la revisión de diseño y direcciones guardadas legibles en modo oscuro.
- PIN de seguridad opcional y reactivable desde el panel administrador.
- GitHub Actions comprueba también los cambios enviados a `main`.

Esta entrega reúne la rama `codex/inicio-comercial` con los cambios de pin y direcciones de 1.29/1.30. El APK 1.30 se compiló en una copia aislada y no contenía las nuevas funciones comerciales; esta versión sí las incluye.

## Validación del APK

- `assembleDebug`, `assembleDebugAndroidTest` y `testDebugUnitTest`: correctos; 67 tests JVM sin fallas.
- 18 tests nativos de AdCarouselTest, CommercialHomeTest, SavedPlacesTest y RideOptionsDrawerTest: correctos en emulador Android con páginas de 16 KB.
- 3 tests nativos adicionales de pedido de negocio en moto, mototaxi para un contacto y courier con contactos separados: correctos. Total: 21 tests nativos.
- 7 tests Node del protocolo de actualización web y generador de páginas legales: correctos.
- Firma QA verificada; conserva SHA-1 `7a4ec69cae01b4b8a5eeeff832d833e176c0efd0` para actualizar las instalaciones existentes.
- `zipalign -c -P 16 4`: correcto. Objetivo Android API 36.
- APK: 112445967 bytes; SHA-256 `469ed680f76b1264477da5336df4b1f411b60539d59509a8d0d8278de9318dc2`.

Los tests usan pedidos y contactos ficticios con callbacks locales: no solicitan viajes reales ni envían SMS. Evidencia local en `build/qa-release-1.31-native.txt` y `build/qa-release-1.31-screenshots/`.

## Google Play

La cuenta de Play Console está verificada según Carlos. Se conserva la clave de subida existente fuera del repositorio. La distribución web sigue usando la firma QA; Play usa un AAB release firmado por separado.

Antes de distribuir desde Play: crear o seleccionar la aplicación `com.intu.taxi`, configurar Play App Signing, registrar su certificado de firma en Firebase/OAuth, definir la lista de testers y comprobar Google/SMS desde una instalación del canal interno. Las páginas legales siguen como plantillas: faltan responsable, contacto operativo y decisiones de conservación/eliminación. Ver `docs/PLAY_TESTING_READINESS.md` para los pendientes de privacidad y declaraciones de servicios.

## Publicación confirmada

- La rama `codex/inicio-comercial` se integró en `main` por fast-forward; ambas se subieron a GitHub. Commit del código de esta entrega: `9e7a8bd5956e3504e6f44291cdc556cc556c242e`.
- El helper `publish-apk.ps1` subió la copia fija, la descargó y comparó SHA-256, actualizó `intu.apk` y publicó `latest.json` al terminar. La API confirmó 1.31/código 32 y la misma huella. La descarga principal confirmó el tamaño de esta versión.
- Web: https://viajaconintu.pages.dev. Descarga fija: https://viajaconintu.pages.dev/descargar?versionCode=32.
- Samsung actualizado con `adb install -r`: correcto; package confirma 1.31/32. Sin desinstalar ni interactuar con la cuenta.
- Evidencia local: `build/published-apk-32-results.txt`, `build/published-apk-32-api.json` y `build/qa-release-1.31-bookings.txt`.

## AAB actualizado para prueba interna

- `prepare-play-bundle.ps1 -Build`: `bundleRelease` y `lintRelease` correctos. Firma JAR verificada con `CN=Intu Upload`, distinta del certificado debug.
- `bundletool` 1.18.3 valida el AAB; manifest confirma `com.intu.taxi`, 1.31/32, mínimo API 24 y objetivo API 36. Configuración `PAGE_ALIGNMENT_16K`; cuatro bibliotecas ELF ARM64 con segmentos alineados a 16 KB.
- APK universal generado desde este AAB: `apksigner verify` y `zipalign -c -P 16 4` correctos, ARM64/ARM32, sin `debuggable`. No se instaló sobre la app QA del Samsung.
- Entrega local: `build/play/intu-1.31.aab`, 40758375 bytes, SHA-256 `8c749608148cee10421072eb0d8edf338ac6eda69a8f07c08df015691318fead`.
- Esta entrega reemplaza el AAB antiguo 1.14 documentado en la preparación inicial. No se ha subido a Play Console ni se ha comprobado todavía el acceso desde una instalación firmada por Google Play.

La preparación técnica permite comenzar con el canal interno; la distribución real requiere completar su configuración en Console y validar Firebase con el certificado de firma de Play. Los pendientes legales y de servicios del documento de preparación siguen vigentes. Google Play determina la aceptación de la entrega.
