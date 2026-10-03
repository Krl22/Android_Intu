# Preparación de Intu para QA — 30 de septiembre de 2026

**Estado actualizado: proyecto preparado para compilar y depurar la versión 1.3.** Los cambios y la validación actual están en [RELEASE_1_3.md](RELEASE_1_3.md). La prueba completa pasajero–conductor en dos teléfonos físicos sigue pendiente. El diagnóstico inicial conservado abajo describe la situación anterior a la preparación del proyecto.

## Actualización tras preparar el proyecto

- `local.properties` contiene el SDK instalado y el token público de Mapbox proporcionado por el usuario; está ignorado por Git. Gradle ahora carga ese archivo y rechaza tokens privados o una configuración ausente.
- La consulta de un estilo con el token responde HTTP 200 y se confirmó que el APK generado contiene ese mismo token.
- Se eliminó la autenticación innecesaria del repositorio Maven de Mapbox y el requisito del token privado de descargas en el workflow.
- Java Temurin 17 está preparado localmente; `gradlew-local.cmd` aplica la alternativa TCP que evita el fallo AF_UNIX de este entorno. Las instrucciones están en `DEV_SETUP.md`.
- Comando verificado: `gradlew-local.cmd :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain --no-daemon`. Resultado **BUILD SUCCESSFUL**.
- **9 pruebas pasaron**, incluidas las dos conexiones a Supabase Realtime. Lint terminó sin errores, con **70 advertencias y 9 sugerencias**. Entre las advertencias hay compatibilidad de bibliotecas nativas con páginas de 16 KB; el Samsung conectado usa páginas de 4 KB.
- APK debug firmado generado: `app/build/outputs/apk/debug/app-debug.apk`, **1.1 / versionCode 2**, con depuración habilitada.
- El Samsung ya aparece conectado por ADB. Tiene instalada una versión 1.0 debug con una firma distinta a la clave debug local, por lo que no admite una actualización conservando datos con esta clave. Se necesita la clave original o autorizar una reinstalación; se conservó la instalación existente.
- El `google-services.json` local tampoco contiene la huella de la clave debug actual. Debe comprobarse su registro en Firebase antes de dar por validado el login; las huellas están en `DEV_SETUP.md`.
- Continúan pendientes las cuentas para el viaje completo, la aprobación del conductor y la verificación de Firebase/push en dispositivos.

Las secciones siguientes conservan la evidencia de la revisión inicial, anterior a esta preparación. La descarga pública sigue siendo la versión 1.0; el nuevo APK no se publicó.

## Accesos comprobados

| Servicio | Resultado | Evidencia |
|---|---|---|
| Supabase de Intu | Confirmado | Lectura del proyecto `vkguzpciwpfvaeyedepl`, tablas y consultas SQL. Estado `ACTIVE_HEALTHY`. El listado inicial de proyectos omitió Intu; la consulta por su ID sí funcionó. |
| Cloudflare | Confirmado | Wrangler autenticado; listado de despliegues de Pages de `viajaconintu` correcto. |
| Firebase | Administración sin confirmar | El repositorio configura `intu-e8403`, pero esta sesión no expone herramientas de Firebase ni se encontró una sesión local de su CLI en las ubicaciones habituales. Esto no demuestra que Firebase esté mal configurado. |

## Verificaciones realizadas

- La web, `/api/apk` y `/descargar` responden HTTP 200.
- APK descargado: `com.intu.taxi`, **versionName 1.0 / versionCode 1**, 53,633,442 bytes, Android mínimo 7.0 (API 24), target API 36, ARM de 32 y 64 bits.
- `apksigner verify` confirma firma válida v2; certificado `CN=Intu, O=Intu, L=Satipo, C=PE`.
- SHA-256 del archivo: `13a601dca47d7cb6cb4ee9e19dac6ccd871de03d44e9a9d746de3d8528f7891e`.
- El APK contiene un token público de Mapbox; la consulta del estilo `streets-v12` con ese token responde HTTP 200. No se verificaron aún renderizado, GPS, búsqueda ni rutas en la app.
- Supabase tiene las **10 migraciones** del repositorio aplicadas, desde `20260925185823` hasta `20260928174530`.
- Las siete tablas públicas tienen RLS habilitado. Solicitudes REST con el rol anónimo a `profiles`, `drivers`, `rides`, `driver_locations` y `device_tokens` devuelven HTTP 401 / `42501`.
- El servicio `mototaxi` está activo; el cron de expiración de solicitudes está activo.
- Existe el trigger `rides_notify_status` y está configurado el secreto `push_webhook_secret`. Esto no confirma entrega de push ni coincidencia del secreto con Firebase.
- `node --check firebase/functions/index.js` pasa; no comprueba despliegue ni ejecución de esas funciones.

## Pendientes para preparar la prueba completa

1. **Alinear el APK con el código que se quiere probar.** `app/build.gradle.kts` declara **1.1 / versionCode 2**, mientras la descarga entrega **1.0 / versionCode 1**. El APK público no representa la versión declarada actualmente por el repositorio.
2. **Preparar un conductor de prueba.** Las tablas `drivers`, `vehicles` y `driver_locations` están vacías. Registrar al conductor, completar vehículo/documentos y aprobarlo antes de probar aceptación y ejecución de viajes. Hay dos perfiles, pero no se comprobó que sean cuentas aptas para QA.
3. **Validar Firebase en su consola y con el APK firmado.** Comprobar proveedores de login, integración Firebase–Supabase, funciones de bloqueo/claim `role: authenticated`, despliegue de `ridePush`, reglas de Storage y huellas de firma. La SHA-1 del APK publicado (`5cf84a4ae0bdddfa08ff4cf5fd1efa14ff0d7fd9`) no coincide con el único cliente OAuth Android en el `google-services.json` local. Es una discrepancia a revisar, no una prueba de fallo del login ni de ausencia de esa huella en la consola.
4. **Habilitar verificación en dispositivos y compilación.** `adb devices -l` no encuentra teléfonos/emuladores. El intento de `assembleDebug`, `testDebugUnitTest` y `lintDebug` falla antes de ejecutar esas tareas: Java informa `Unable to establish loopback connection`. Los intentos con IPv4 y otro proveedor de selector tampoco resolvieron el problema. No se confirmó compilación, pruebas unitarias ni lint; el fallo observado corresponde al entorno de ejecución.

## Prueba mínima de aceptación

- Instalar la misma versión identificada en dos teléfonos Android compatibles; comprobar apertura y permisos de ubicación/notificaciones.
- Registrar/iniciar sesión con cuentas distintas; guardar perfiles y comprobar persistencia sin cierres de la app.
- Registrar y aprobar al conductor; activar disponibilidad y verificar que el pasajero lo encuentre.
- Solicitar y aceptar viaje; comprobar datos, tarifa y actualización de ubicación en ambos teléfonos.
- Marcar llegada; probar PIN incorrecto y correcto; iniciar viaje.
- Confirmar pago en efectivo y finalizar; repetir con Yape y verificar el número mostrado/copiar.
- Probar cancelaciones de ambos lados, vencimiento de solicitud y reconexión tras perder internet.
- Minimizar ambas apps y verificar actualizaciones/notificaciones; probar retorno a la app y cierre de sesión.
- Comprobar que dos cuentas distintas no puedan leer ni modificar datos privados ajenos.

## Observaciones de documentación y seguridad

- `ARCHITECTURE_DATA.md` describe una arquitectura anterior con viajes en Firebase Realtime Database. La implementación actual y `supabase/README.md` sitúan los datos en Supabase.
- La guía HTML existente identifica una versión 1.0 debug y menciona un cierre al guardar perfiles; el código actual de `MainActivity.kt` ya maneja el fallo con `runCatching`. Esa guía debe revisarse antes de utilizarla como descripción del comportamiento actual.
- Los asesores de Supabase señalan tablas privadas sin políticas, objetos visibles en GraphQL y funciones `SECURITY DEFINER` ejecutables. Esos avisos incluyen componentes intencionales del diseño; no demuestran por sí mismos una exposición de filas. La autorización por cuenta y por rol queda pendiente de prueba autenticada. Referencias: [tablas sin políticas](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy), [visibilidad GraphQL](https://supabase.com/docs/guides/database/database-linter?lint=0027_pg_graphql_authenticated_table_exposed), [funciones anónimas](https://supabase.com/docs/guides/database/database-linter?lint=0028_anon_security_definer_function_executable), [funciones autenticadas](https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable).

La revisión no publicó cambios ni modificó datos del backend. El diagnóstico de sockets de Java se contrastó con el [código de OpenJDK](https://raw.githubusercontent.com/openjdk/jdk21u/master/src/java.base/windows/classes/sun/nio/ch/PipeImpl.java); no se cambió la configuración permanente de Java.
