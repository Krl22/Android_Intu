# Acceso por teléfono y vinculación

Firebase Auth identifica la cuenta. Su UID es también el identificador del perfil y de los viajes en Supabase.

- El acceso por teléfono verifica un código SMS de seis dígitos. El formulario admite Perú (+51) y Estados Unidos (+1). Si es un usuario nuevo, continúa a completar su perfil; si ya existe y tiene perfil completo, abre Inicio.
- Google y teléfono se vinculan al usuario actual con `linkWithCredential`, conservando el UID. Cambiar un teléfono ya vinculado usa `updatePhoneNumber` después de verificar el nuevo número.
- Las cuentas nuevas con Google verifican su teléfono durante el registro. Las cuentas existentes pueden vincularlo desde Cuenta.
- Si el teléfono o Google ya pertenece a otro UID, se muestra un conflicto. No se fusionan cuentas ni se reemplaza la sesión original.
- El teléfono y correo de contacto guardados en el perfil no habilitan acceso por sí mismos. No hay login con correo y contraseña; el acceso mediante correo se realiza con Google vinculado.

## Panel admin

Administración → Usuarios muestra el UID, correo y teléfono del perfil junto con la información de acceso consultada directamente en Firebase Auth: Google vinculado y su correo, teléfono vinculado para SMS, correo de la cuenta y su verificación, y acceso habilitado o deshabilitado.

La callable `adminGetAccountAccess` autentica al solicitante y comprueba su permiso actual con `is_admin` de Supabase antes de consultar Firebase Admin SDK. Es de solo lectura, consulta lotes de hasta 100 UID y devuelve únicamente una lista explícita de campos. No devuelve hashes, tokens, claims o registros completos de Firebase.

Una cuenta de Firebase inexistente tiene un estado separado de una consulta fallida. Si falla la consulta de vinculación, el panel mantiene visibles los perfiles, muestra un aviso y permite reintentar con Actualizar. No muestra «Sin vincular» a partir de un error de conexión.

## Pruebas

Verificado el 1 de octubre de 2026 en el Samsung con la app **1.19 / código 20**, integrada con la actualización de reportes y términos de la otra sesión. SHA-256 del APK instalado: `DE2AF460B7796830550539FF16064C263A8B0ECD4AA95ED6FA45BF37F65014FD`.

La nueva función se desplegó en `intu-e8403`, región `us-central1`, sin modificar otras funciones ni el esquema SQL. Una consulta HTTP sin sesión devolvió 401 / UNAUTHENTICATED. Las cinco pruebas del servidor y tres pruebas unitarias de interpretación pasaron; la compilación compartida completó 38 pruebas unitarias y lint sin errores. En el celular pasaron las cuatro pruebas de pantalla y servidor real. El primer intento de una prueba no encontró la jerarquía Compose al arrancar; pasó al repetir el caso y después el lote completo, sin cambios de código.

La comprobación real consultó los perfiles existentes y confirmó que los accesos del usuario actual coincidían con Firebase. Se reabrió MainActivity al terminar, conservando la sesión.

- `firebase/functions/account-access.test.js`: permisos, fallo cerrado del control admin, validación de lotes, resultado desordenado, UID inexistente, campos permitidos y fallo de Firebase.
- `AdminAccountAccessTest`: interpretación de la respuesta y distinción entre datos de contacto y proveedores vinculados.
- `AdminAccountAccessInfoTest`: correo de contacto sin Google, ambos métodos vinculados y consulta no disponible.
- `AdminAccountAccessLiveTest`: consulta de solo lectura con la sesión admin existente; compara el resultado desplegado con la identidad Firebase actual. No vincula cuentas, envía SMS, modifica usuarios ni cierra la sesión.

Referencias: [vincular proveedores en Android](https://firebase.google.com/docs/auth/android/account-linking), [consulta de usuarios con Firebase Admin SDK](https://firebase.google.com/docs/auth/admin/manage-users), [funciones callable](https://firebase.google.com/docs/functions/callable), [Firebase Auth con Supabase](https://supabase.com/docs/guides/auth/third-party/firebase-auth).
