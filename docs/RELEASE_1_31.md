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
- 7 tests Node del protocolo de actualización web y generador de páginas legales: correctos.
- Firma QA verificada; conserva SHA-1 `7a4ec69cae01b4b8a5eeeff832d833e176c0efd0` para actualizar las instalaciones existentes.
- `zipalign -c -P 16 4`: correcto. Objetivo Android API 36.
- APK: 112445967 bytes; SHA-256 `469ed680f76b1264477da5336df4b1f411b60539d59509a8d0d8278de9318dc2`.

Los tests usan pedidos y contactos ficticios con callbacks locales: no solicitan viajes reales ni envían SMS. Evidencia local en `build/qa-release-1.31-native.txt` y `build/qa-release-1.31-screenshots/`.

## Google Play

La cuenta de Play Console está verificada según Carlos. Se conserva la clave de subida existente fuera del repositorio. La distribución web sigue usando la firma QA; Play usa un AAB release firmado por separado.

Antes de distribuir desde Play: crear o seleccionar la aplicación `com.intu.taxi`, configurar Play App Signing, registrar su certificado de firma en Firebase/OAuth, definir la lista de testers y comprobar Google/SMS desde una instalación del canal interno. Las páginas legales siguen como plantillas: faltan responsable, contacto operativo y decisiones de conservación/eliminación. Ver `docs/PLAY_TESTING_READINESS.md` para los pendientes de privacidad y declaraciones de servicios.

No se ha subido esta versión a Play Console. El resultado de publicación web y la validación final del AAB se registran después de completarlos.
