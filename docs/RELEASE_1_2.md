# Intu 1.2 — primer ingreso

Versión: `1.2`, código Android: `3`.

## Corrección

El formulario de perfil dejaba siempre el teléfono en solo lectura. Un usuario de Google sin número verificado veía un campo vacío que no podía completar. Además, la pantalla pedía un correo nuevo incluso cuando Firebase ya proporcionaba el correo de Google.

- El teléfono puede editarse si Firebase no proporciona uno verificado. El número verificado mediante SMS se conserva en solo lectura.
- El teclado es de teléfono. Al guardar, se aceptan nueve dígitos peruanos o un número internacional y se normalizan a E.164. Un número incompleto muestra un error en el campo y no se envía.
- Se utiliza el correo que ya trae Firebase y se oculta el campo de correo cuando está disponible; tampoco aparece la acción de verificar ese correo.
- El formulario permite desplazarse y se adapta al teclado abierto.

## Verificación

- `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease`, `testDebugUnitTest` y `lintDebug`: compilación y verificaciones completadas.
- Nueve pruebas de JVM: pasaron, incluidas dos conexiones a Supabase Realtime.
- Cuatro pruebas de interfaz en `emulator-5556` (Android API 37, imagen con páginas de 16 KB): pasaron. Cubren edición y guardado de teléfono con correo de Google, conservación del número verificado y solicitud del correo faltante, error por número incompleto y preservación del prefijo internacional.
- Lint: sin errores. Permanecen advertencias existentes, incluidas las relacionadas con bibliotecas nativas y páginas de 16 KB; las pruebas de interfaz del formulario no validan el mapa en ese entorno.
- El emulador se ejecutó con `-read-only` y sin guardar snapshots del AVD existente. No se modificaron cuentas ni viajes reales durante las pruebas de interfaz.

## Artefactos y publicación de QA

- APK debug firmado: `app/build/outputs/apk/debug/app-debug.apk`.
- APK release ARM compilado, **sin firma**: `app/build/outputs/apk/release/app-release-unsigned.apk`.
- Por solicitud del usuario, se publicó el APK **debug firmado con la clave local de pruebas** en `intu-apk/intu.apk`. La descarga de la web ya entrega Intu 1.2, código 3, con depuración habilitada.
- Publicado el 2026-10-01 a las 00:16:44 UTC (2026-09-30 en Nueva York). Tamaño: 108531344 bytes.
- Se descargó de nuevo desde `https://viajaconintu.pages.dev/descargar?v=1.2-qa-3`; su firma es válida y el hash SHA-256 coincide con el archivo local: `003a9b5fa8d375f2eb38ad4ccff21e08de42f40ae6ba981f2d43d2a27d9f380f`.
- El APK público anterior se conserva localmente en `%LOCALAPPDATA%/Temp/intu-qa-20260930/Intu.apk` para recuperación.

### Instalación y Google login

Android no instala un APK sin firma. La firma debug permite instalar esta versión de QA, pero no coincide con las versiones anteriores: para cambiar a este APK es necesario desinstalar la app anterior, perdiendo sus datos locales.

Google login debe autorizar la firma de este APK en Firebase, proyecto `intu-e8403`, app Android `com.intu.taxi`:

- SHA-1: `7A:4E:C6:9C:AE:01:B4:B8:A5:EE:EF:F8:32:D8:33:E1:76:C0:EF:D0`.
- SHA-256: `7E:E4:8B:40:2D:20:04:2F:CF:A7:B0:DB:98:B9:1F:CA:E4:10:D1:2E:FE:7D:9F:9F:3D:D8:F4:BE:99:68:FA:0D`.

Tras el reporte `DEVELOPER_ERROR (10)`, el usuario autorizó Firebase CLI mediante el flujo remoto de Google. Se confirmó en Firebase que ambas huellas de QA faltaban y se añadieron a la app Android existente. Una nueva consulta confirmó las dos huellas registradas; las firmas anteriores se conservaron.

Se descargó la configuración actualizada de Firebase a `app/google-services.json`. Incluye el nuevo cliente OAuth Android correspondiente a la SHA-1 de QA: `882060234231-j28ql7cngbo0roh144fnkle4p60dtj06.apps.googleusercontent.com`. El cliente web sigue siendo `882060234231-kunqgqgc8gs6k95nr1h751190idbotpe.apps.googleusercontent.com`, que coincide con el valor incluido en el APK publicado.

La tarea `:app:processDebugGoogleServices` pasó y el hash de sus recursos generados se mantuvo idéntico. La corrección se realiza en Firebase y el APK 1.2 publicado sirve para volver a probar; no se requiere recompilar ni descargar otro APK por este cambio de huellas.

Las pruebas de interfaz verificaron el formulario con datos de Google simulados. El login real en el teléfono queda pendiente de reintento por el usuario; ADB no tenía dispositivos conectados al aplicar esta corrección.

### Futuras actualizaciones con la firma original

Para publicar una actualización compatible con la versión release anterior se requiere el archivo de firma original y su configuración. La clave original no se encontró en el proyecto ni en las ubicaciones locales revisadas.

Configurar estas propiedades en `C:/Users/Carlos_Villar/.gradle/gradle.properties`, fuera del repositorio:

```properties
INTU_KEYSTORE_FILE=RUTA_AL_ARCHIVO_ORIGINAL
INTU_KEYSTORE_PASSWORD=CONTRASENA_DEL_ALMACEN
INTU_KEY_ALIAS=ALIAS_ORIGINAL
INTU_KEY_PASSWORD=CONTRASENA_DE_LA_CLAVE
```

Después:

1. Ejecutar `gradlew-local.cmd :app:assembleRelease --console=plain`.
2. Verificar el APK firmado con `apksigner` y comprobar que su certificado coincida con el release original conservado, previo a la publicación de QA. SHA-256 esperada del certificado: `90dc0fd42f6443713181bb2e97c1753cbb7e87fd27c3db634575fbbb7e7c6e35`.
3. Subir el APK firmado a `intu-apk/intu.apk` mediante Wrangler con `--remote`, según `web/README.md`.
4. Descargar de nuevo desde `/descargar` y comprobar firma, versión 1.2, tamaño y hash frente al archivo local. La web sirve el objeto de R2; actualizar solo el APK no requiere volver a desplegar los archivos estáticos de Pages.

La publicación de QA no cambia la configuración de firma release del proyecto; el artefacto release sin firmar no se distribuyó.
