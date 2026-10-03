# Intu 1.18 QA — actualización conjunta

Versión 1.18, código 19, con el certificado debug habitual. Reúne los cambios locales 1.16, 1.17 y 1.18 después del APK 1.15 publicado.

## Cambios

- Se conserva Casa / Trabajo / favoritos → Elige tu viaje, sin confirmar nuevamente el destino, y la selección de recojo después de confirmar el viaje.
- Direcciones guardadas usa encabezado con degradado, tarjetas Casa y Trabajo con iconos y estados, sección de favoritos, acción Agregar y cierre fijo con contenido desplazable.
- El botón físico Atrás y la flecha de Elige tu viaje vuelven al inicio, limpian selección y ruta, y centran la cámara en la última ubicación recibida. Un nuevo destino parte de esa ubicación, sin conservar el recojo anterior.
- Detalle del viaje usa cabecera con fecha y estado, tarjeta de tarifa, direcciones separadas, estimados, mapa redondeado y cierre fijo. Se conserva la carga y el formato de datos.

## Verificación

- Las sesiones responsables confirmaron compilación y lint de sus cambios; lint debug final: 0 errores, 72 avisos y 10 sugerencias. La regresión JVM del flujo de Home tiene 35 pruebas aprobadas.
- Dos HomeBackNavigationTest pasaron sobre el APK 1.18 en el Samsung: botón físico y flecha vuelven al inicio y centran la última ubicación simulada; la siguiente selección utiliza ese origen.
- Tres SavedShortcutFlowTest pasaron sobre el APK 1.18: Casa, Trabajo y favorito abren Elige tu viaje sin Confirmar destino ni seleccionar recojo todavía; las coordenadas permanecen iguales a las guardadas y no se envían solicitudes.
- Los cinco casos anteriores usan GPS/rutas/envíos simulados. Las direcciones originales del teléfono se restauran y la sesión Firebase principal se conserva. Se abrió MainActivity al terminar y se liberó el dispositivo.
- Registros de atajos: `%TEMP%/intu-qa-shortcuts-1.18/native-shortcut-tests.txt`. Las dos pruebas de Atrás fueron informadas por la sesión de Google Play, con APK de tests compatible `build/qa-back-1.17/intu-1.17-tests.apk`.
- Los cambios visuales de lista y detalle tienen compilación/lint aprobados, sin una validación visual nativa adicional. No se presentan esas pantallas como verificadas visualmente.
- Firma debug y alineación ZIP de 16 KB verificadas. Se comprobó SHA-256 del APK instalado y del snapshot.
- No hubo cambios ni despliegues del backend. La entrega es debug para QA; el AAB de Play preparado anteriormente continúa en versión 1.14.

## Publicación

- Snapshot: `build/ui-trip-details-1.18/intu-1.18.apk`; copia de QA en `%TEMP%/intu-qa-shortcuts-1.18/intu-1.18-debug.apk`.
- Tamaño: 105,173,652 bytes.
- SHA-256: `D9B94B9BED17EA6ED8BC35F724652DE5E2DAD8DE8D2A01BDDEA30F1C818CF6B7`.
- R2 actualizado con ese snapshot el 1 de octubre de 2026 a las 20:05:43, hora de Nueva York. [Descargar Intu 1.18](https://viajaconintu.pages.dev/descargar?v=1.18-qa-19).
- La descarga completa coincide con el APK instalado y probado en tamaño y SHA-256.
