# Intu 1.13 QA — formulario del catálogo

Versión 1.13, código 14, con el certificado debug habitual.

## Cambios

- El formulario se organiza en tres tarjetas: Datos del lugar, Ubicación y recojo, y Publicación. La cabecera y las acciones permanecen visibles al desplazarse; el formulario se adapta al teclado.
- Categoría usa un campo desplegable con borde, etiqueta y flecha, igual que Ciudad / localidad.
- Ciudad / localidad permite seleccionar Satipo o Río Negro. Otra localidad muestra un campo de texto, conserva localidades existentes fuera de esas dos opciones y exige un nombre antes de guardar.
- Las variantes existentes de mayúsculas y tildes de Satipo / Río Negro se reconocen al abrir el editor. Las opciones principales se guardan con su nombre uniforme.
- Otros nombres y Dirección o referencia incluyen ejemplos y explicaciones. Los alias conservan el máximo de 12 y la referencia sigue siendo opcional.
- El mapa es la acción principal para elegir el recojo. La edición manual de coordenadas se despliega bajo Editar coordenadas y conserva la obligación de reconfirmar el punto al modificarlo.
- Borrador, Publicado e Inactivo muestran descripciones y botones de selección exclusivos. Sigue siendo obligatorio confirmar el punto para publicar.
- Colores explícitos aseguran contraste en las tarjetas claras incluso si el teléfono usa tema oscuro.
- Se conserva el formato de datos y los permisos del catálogo; no requiere migración del backend.

## Verificación

- APK y APK de pruebas compilados correctamente; 31 pruebas JVM aprobadas, sin fallos.
- Tres pruebas de interfaz aprobadas en el Samsung conectado por ADB inalámbrico: guardado de categoría, localidad, alias y referencia; conservación y cambio de localidades adicionales; y publicación condicionada a la confirmación del recojo.
- Las pruebas de interfaz usan callbacks locales y no crean ni modifican lugares en el backend.
- Capturas y registro de instrumentación: `%TEMP%/intu-qa-catalog-form/`.
- Lint: 0 errores, 72 avisos y 10 sugerencias; no aumentan respecto a la versión 1.12.
- Firma debug y alineación ZIP de 16 KB verificadas.
- Reinstalada conservando datos en el Samsung; se comprobó versión 1.13 / código 14. Las tres pruebas de interfaz se repitieron y aprobaron sobre este APK final. Se abrió de nuevo la actividad normal de Intu al terminar.

La verificación de esta entrega se centra en el formulario. No sustituye las comprobaciones pendientes de gestos nativos del mapa documentadas en la versión 1.12.

## Publicación

- APK: `app/build/outputs/apk/debug/app-debug.apk`, 105,075,352 bytes.
- SHA-256: `E107988789DB65FE1481B9DB7782FE4E9C9C53AA2FE7D8BB8F69172E911C6469`.
- Publicado en R2 el 2026-10-01 a las 18:29:44 UTC.
- La descarga completa desde [Cloudflare Pages](https://viajaconintu.pages.dev/descargar?v=1.13-qa-14) coincide con el APK local en tamaño y SHA-256.
