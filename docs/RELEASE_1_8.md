# Intu 1.8 QA — envío de reportes

Versión 1.8, código 9. APK debug con la firma habitual de QA.

El formulario de Cuenta mantenía «Enviar reporte» desactivado mientras el título tenía menos de 5 caracteres o la descripción menos de 15. El mínimo del título no aparecía en pantalla. No había una restricción especial para las cuentas admin.

Ahora el botón permite intentar el envío y señala el campo que falta completar. Ambos campos explican sus mínimos y muestran un contador. Durante el envío se bloquean los campos y los botones para evitar duplicados; si falla la conexión, se conserva el texto y se permite reintentar. La validación cuenta caracteres Unicode igual que el servidor, incluidos los emoji.

## Validación

- Las 26 pruebas unitarias pasaron, incluida la comparación de límites con caracteres Unicode.
- La prueba transaccional `supabase/tests/bug_reports_permissions.sql` confirmó que un admin autenticado puede insertar un reporte y verlo en el listado de administración. También verificó los límites del formulario y el rechazo a solicitudes anónimas. Se revirtió la transacción: no quedó un reporte de prueba en el panel.
- No se modificaron las políticas ni el esquema de Supabase.
- La compilación final del APK y del APK de pruebas pasó. Lint terminó sin errores (70 avisos y 10 sugerencias en el proyecto).
- Las dos pruebas del formulario pasaron en el Samsung conectado mediante ADB inalámbrico: campos incompletos, envío único, botones bloqueados durante el envío y conservación del borrador con reintento tras un fallo.
- Firma APK y alineación ZIP de 16 KB verificadas.
- Se reinstaló Intu 1.8 / código 9 en el celular conservando los datos y se abrió `MainActivity`.

## Artefacto

- Archivo: `app/build/outputs/apk/debug/app-debug.apk`.
- Tamaño: 105,859,763 bytes.
- SHA-256: `8A466D63454473E2F8CE65196C01B95CA5640DFC598F77340FC8DA884D4773D2`.
- Publicado en R2 el 2026-10-01 a las 13:20:35 UTC. La descarga completa desde [Cloudflare Pages](https://viajaconintu.pages.dev/descargar?v=1.8-qa-9) coincide en tamaño y SHA-256 y confirma versión 1.8 / código 9.
- Evidencia de las pruebas y descarga verificada: `%LOCALAPPDATA%/Temp/intu-qa-1.8/`.
- La descarga verificada de Intu 1.7 permanece guardada localmente para recuperación.
