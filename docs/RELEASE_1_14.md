# Intu 1.14 QA — acceso por SMS y Google

Versión 1.14, código 15, con el certificado debug habitual.

## Cambios

- Login por SMS para Perú con validación de 9 dígitos que comienzan con 9, normalización de números pegados con +51 y código de 6 dígitos. El formulario permite corregir códigos, cambiar el número y reenviar tras el intervalo indicado, sin envíos duplicados por pulsaciones repetidas.
- Cuenta muestra los métodos Google y teléfono. Permite vincular Google a una cuenta creada por SMS y verificar/vincular un teléfono a una cuenta creada con Google. Ambas operaciones conservan el UID y los datos de la misma cuenta.
- Si Google o el teléfono pertenecen a otra cuenta, la vinculación falla con una explicación. No se fusionan cuentas ni se cambia la sesión actual.
- Los perfiles nuevos con Google verifican el teléfono por SMS antes de completar el registro. Los perfiles existentes con teléfono de contacto sin verificar pueden vincularlo desde Cuenta.
- El correo que proporciona Google se utiliza directamente. Para registros por teléfono el correo de contacto es opcional; se retiró la acción que aparentaba verificar un correo sin tenerlo vinculado a Firebase.
- El perfil exige nombres, apellidos y fecha de nacimiento válida antes de guardar. La sincronización prioriza teléfono y correo de Firebase sobre valores de contacto antiguos.
- La verificación conserva el estado del envío y el intervalo de reenvío al recrear la actividad. Se descartan callbacks de solicitudes canceladas y se bloquea la salida durante la operación de credenciales.
- Incluye la mejora de direcciones guardadas de la sesión paralela: Casa y Trabajo usan el selector de mapa compartido del catálogo, guardan el punto seleccionado y ofrecen referencia opcional y cambiar ubicación.
- Casa y Trabajo ocultan las coordenadas al pasajero y conductor; el selector administrativo conserva esa información.
- Incluye la solicitud de eliminación de cuenta preparada en la sesión de Google Play: se envía al panel de administración, muestra confirmación solo tras éxito y explica que la eliminación se gestiona manualmente. No cambia el backend ni promete eliminación inmediata. El texto legal mantiene explícitos sus pendientes.

## Configuración y alcance

Se verificó en el proyecto Firebase `intu-e8403` que Google y teléfono están habilitados, ambas huellas del certificado debug están registradas, la política de SMS admite PE y US y la facturación está habilitada. No se modificó la configuración ni el esquema del backend.

La recepción de un SMS real de una operadora peruana requiere una prueba adicional con un número real. Las cuentas ficticias y Auth Emulator no envían SMS. Las pruebas del emulador usan un FirebaseApp secundario en `demo-intu-qa` y conservan la sesión de producción del celular.

## Verificación

- APK, APK de pruebas y pruebas JVM compilados correctamente: 35 pruebas JVM aprobadas, sin fallos.
- Pruebas REST contra Auth Emulator: ambas direcciones de vinculación conservan UID; los conflictos se rechazan sin modificar cuentas.
- 22 pruebas nativas aprobadas sobre el APK final en el Samsung: 11 de teléfono/vinculación/perfil, 3 de solicitud de eliminación y 8 de direcciones guardadas/catálogo/pin.
- Se comprobó la restauración del formulario SMS sin enviar otro código, el intervalo de reenvío aun reutilizando el mismo ID y el descarte de callbacks antiguos. Firebase SDK conserva UID al vincular en ambos sentidos, al cambiar un teléfono verificado y al rechazar colisiones o una sesión obsoleta.
- Los tests de eliminación utilizan callbacks locales: no envían solicitudes al panel ni borran datos. Los de Firebase utilizan el proyecto demo aislado, no SMS reales ni Supabase de producción.
- Lint: 0 errores, 72 avisos y 10 sugerencias, sin aumento respecto al historial de QA.
- Firma debug y alineación ZIP de 16 KB verificadas. Ambas huellas coinciden con las registradas en Firebase.
- Instalación con `-r` conserva los datos. Se confirmó versión 1.14 / código 15; la sesión de direcciones también verificó que el SHA-256 del APK instalado coincide con el snapshot final.
- Al terminar se retiró el reverse ADB del puerto 9099, se detuvo Auth Emulator y se abrió la actividad normal de Intu. No se cerró la sesión Firebase principal.
- Registro final de instrumentación y snapshot: `%TEMP%/intu-qa-auth/native-final-tests.txt` y `%TEMP%/intu-qa-auth/intu-1.14-debug.apk`.

## Publicación

- APK final: 105,197,137 bytes.
- SHA-256: `049800037D8EB7EF7EEA9662E026548214A84D861569F80FFB032CEDE068681F`.
- Publicado en R2 el 2026-10-01 a las 23:13:25 UTC.
- La descarga completa desde [Cloudflare Pages](https://viajaconintu.pages.dev/descargar?v=1.14-qa-15) coincide con el APK instalado y probado en tamaño y SHA-256.
- Se publica el snapshot validado, para evitar que los builds de otras sesiones cambien el archivo antes de subirlo. No hubo despliegues de Firebase functions, Supabase ni de las plantillas web legales pendientes.
