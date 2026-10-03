# Intu 1.19 QA — formularios de Cuenta

Versión 1.19, código 20. Certificado debug habitual; actualización conservando los datos del teléfono.

## Diseño

- Reportar un error usa cabecera con los colores de Intu, guía breve, campos redondeados con ejemplos, validación junto al campo y botón principal de envío.
- Las acciones permanecen visibles con el teclado abierto; el contenido se desplaza y el borrador se conserva ante errores de envío. Durante el envío se bloquean acciones e inputs para evitar duplicados.
- Términos y privacidad tiene pestañas, secciones en tarjetas y cierre fijo. Cada pestaña conserva su posición de lectura. Se mantiene el contenido legal previo y la indicación de borrador para pruebas.
- La compilación final integra también los cambios Admin de la sesión coordinada: consulta de métodos de acceso verificados en Firebase. Su backend y QA se validan por esa sesión antes de publicar.

## Verificación

- Compilación debug, APK de pruebas, 38 pruebas JVM y lint debug aprobados.
- AccountToolsTest: tres casos pasaron en el Samsung sobre el APK final: validación y envío único, conservación del texto/reintento ante fallo y navegación/lectura/cierre de las pestañas legales.
- Se verificó que Enviar reporte permanezca visible con el teclado abierto. Capturas nativas revisadas del formulario y del contenido legal.
- Las pruebas de reporte usan callbacks ficticios: no generan reportes reales. La sesión Firebase del teléfono se conserva; MainActivity se reabre después de las pruebas.
- Registro y capturas locales: `%TEMP%/intu-qa-account-1.19-final/`. Las capturas contienen overlays del teléfono y no se publican en la web.
- Lint final: 0 errores, 72 avisos y 10 sugerencias. El primer análisis falló porque una fuente Admin apareció durante la compilación paralela; se congelaron las fuentes y la compilación completa final pasó.
- QA Admin coordinado: cuatro casos nativos aprobados (tres UI y uno de consulta real), tres JVM específicos y cinco casos Node del servidor. La callable `adminGetAccountAccess` se desplegó en Firebase y rechaza consultas anónimas (HTTP 401). Un caso UI falló al arrancar en el primer lote; pasó aislado y en el lote final completo sin cambios de código.
- Firma debug y alineación de 16 KB verificadas; SHA-256 del APK instalado coincide con el snapshot.

## APK

- Snapshot: `build/ui-account-1.19/intu-1.19.apk`.
- Tamaño: 105,301,851 bytes.
- SHA-256: `DE2AF460B7796830550539FF16064C263A8B0ECD4AA95ED6FA45BF37F65014FD`.
- Publicado en R2 el 1 de octubre de 2026 a las 20:26:55, hora de Nueva York (`2026-10-02T00:26:55.747Z`). [Descargar Intu 1.19](https://viajaconintu.pages.dev/descargar?v=1.19-qa-20).
- La descarga completa coincide con el APK instalado y probado en tamaño y SHA-256.
