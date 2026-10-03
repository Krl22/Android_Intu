# Desarrollo local de Intu

## SDK y Mapbox

Crear `local.properties` en la raíz; está ignorado por Git:

```properties
sdk.dir=C\:/Users/TU_USUARIO/AppData/Local/Android/Sdk
MAPBOX_ACCESS_TOKEN=TU_TOKEN_PUBLICO_PK
```

Gradle lee el token desde propiedades de Gradle, el entorno o `local.properties`, en ese orden. El token público se incluye en el recurso Android `mapbox_access_token`; no usar un token privado `sk.` aquí. La ausencia del token produce un error de configuración explícito.

El repositorio Maven de Mapbox permite descargar los artefactos usados por este proyecto sin credenciales. Se comprobó HTTP 200 para los POM de la versión 11.16.4. La [guía oficial de instalación](https://docs.mapbox.com/android/maps/guides/install/) configura ese repositorio sin autenticación.

## Java y Android Studio

Android Studio usa `GRADLE_LOCAL_JAVA_HOME`, definido por `java.home` en `.gradle/config.properties`. En esta PC se preparó un JDK Temurin 17 dentro de `.gradle/jdks/`, con SHA-256 verificado contra la publicación de Adoptium. El runtime y la configuración son locales y están ignorados por Git.

En Android Studio, abrir la raíz del proyecto y sincronizar Gradle. La configuración del proyecto ya selecciona `#GRADLE_LOCAL_JAVA_HOME`. Para ejecutar la app, seleccionar el módulo `app` y el teléfono conectado.

## Compilar y comprobar desde PowerShell

```powershell
.\gradlew-local.cmd :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

`gradlew-local.cmd` usa `JAVA_HOME` si existe, luego el JDK de `.gradle/config.properties`, o el JDK incluido con Android Studio. Sus cambios de entorno solo duran durante el comando.

En esta PC, Java 17 y 21 fallan al abrir sockets AF_UNIX con `Invalid argument: connect`. El comando local dirige `jdk.net.unixdomain.tmpdir` a `NUL`, que no admite archivos, para que Java utilice su alternativa TCP. El [código de OpenJDK](https://raw.githubusercontent.com/openjdk/jdk21u/master/src/java.base/windows/classes/sun/nio/ch/PipeImpl.java) implementa esta alternativa cuando no puede crear el socket Unix. El wrapper estándar permanece disponible para otros entornos. Si Android Studio presenta el mismo fallo, usar el comando local para compilar y la opción de adjuntar el depurador al proceso instalado.

El APK debug se genera en `app/build/outputs/apk/debug/app-debug.apk`. Las pruebas de `SupabaseRealtimeTest` requieren internet y se conectan al backend real; no crean viajes ni usuarios.

## Instalar para QA

Carlos pide reinstalar la app al terminar cada cambio, conservando sus datos. Antes de usar el celular para pruebas de interfaz, avisarle y confirmar disponibilidad; no asumirla por una conexión ADB activa. Coordinar instalaciones y pruebas del mismo dispositivo entre las sesiones de trabajo para evitar interferencias.

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

Una instalación con `-r` conserva los datos si el certificado es el mismo. Si Android informa una firma incompatible, conservar los datos existentes y resolver la firma antes de desinstalar.

En la preparación del 30 de septiembre se comprobó que el Samsung conectado tiene una versión 1.0 debug firmada con otra clave. Su SHA-1 es `4ec9816a4041c3387fd9bdc203e85c0080ce649b`; la clave debug local actual tiene SHA-1 `7a4ec69cae01b4b8a5eeeff832d833e176c0efd0`. No se intentó sobrescribir ni desinstalar la app existente.

Para login con Google y verificación telefónica, comprobar en Firebase que estén registradas las huellas de la clave debug actual:

- SHA-1: `7A:4E:C6:9C:AE:01:B4:B8:A5:EE:EF:F8:32:D8:33:E1:76:C0:EF:D0`.
- SHA-256: `7E:E4:8B:40:2D:20:04:2F:CF:A7:B0:DB:98:B9:1F:CA:E4:10:D1:2E:FE:7D:9F:9F:3D:D8:F4:BE:99:68:FA:0D`.

El 1 de octubre de 2026 se comprobó en Firebase que las dos huellas anteriores están registradas, Google y teléfono están habilitados, y la política de SMS permite Perú y Estados Unidos. La facturación del proyecto está habilitada. El APK debug actual usa ese mismo certificado y permite actualizar la instalación de QA con `-r`.

## Login y vinculación de cuentas

Firebase Auth identifica la cuenta; su UID es también `profiles.id` en Supabase. Google y teléfono deben vincularse con `linkWithCredential` sobre el usuario actual, conservando ese UID. No crear otra sesión para vincular un método. Si el método pertenece a otro UID, mostrar el conflicto y conservar la sesión original; Carlos decidió no fusionar cuentas.

En Perú el formulario acepta celulares de 9 dígitos que empiezan con 9 y normaliza pegados con `+51` sin duplicar el prefijo. El número de contacto guardado en el perfil no habilita login SMS por sí mismo: requiere verificación y vinculación en Firebase. Las cuentas existentes pueden hacerlo desde Cuenta. Las nuevas cuentas con Google verifican el teléfono durante el registro. No se admite acceso con correo y contraseña; el correo de contacto opcional tampoco constituye un método de acceso.

Las pruebas `AccountLinkingTest` usan un FirebaseApp secundario con proyecto `demo-intu-qa` y Auth Emulator local en el puerto 9099. No modifican la sesión Firebase principal ni envían SMS reales. Requieren el emulador iniciado y `adb reverse tcp:9099 tcp:9099`; retirar ese reverse al finalizar. Los números ficticios de Firebase y el emulador comprueban el flujo, pero no demuestran entrega de SMS por una operadora peruana.

La compilación debug habilita depuración; login, GPS, push y el flujo de viaje deben verificarse en dispositivos con cuentas de prueba.
