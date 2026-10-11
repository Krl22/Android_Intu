# viajaconintu — página de descarga de Intu

Página estática en **Cloudflare Pages** (`https://viajaconintu.pages.dev`). El APK vive en **Cloudflare R2**
(bucket `intu-apk`, archivo `intu.apk`) porque Pages no acepta archivos de más de 25 MB; la función
`functions/descargar.js` lo entrega en `/descargar` como `Intu.apk`, y `/api/apk` da su tamaño y fecha.

Imágenes generadas con Higgsfield (Nano Banana Pro) y optimizadas a WebP en `public/img/`.

En PowerShell usa `npx.cmd` (los scripts `.ps1` están bloqueados en esta PC). Todos los comandos, desde `web/`.

## Primera vez

```powershell
npx.cmd wrangler r2 bucket create intu-apk
npx.cmd wrangler pages project create viajaconintu --production-branch main
```

## Publicar o actualizar el APK

1. Compila el APK firmado (Android Studio o `gradlew assembleRelease`). Sube `versionCode` en
   `app/build.gradle.kts` en cada versión nueva para que Android la acepte como actualización.
2. Desde la raíz del repositorio, publica el APK con el helper. Inspecciona versión y firma, conserva una descarga fija por código y anuncia la versión al terminar de subir el archivo:

```powershell
.\scripts\publish-apk.ps1 -ApkPath .\app\build\outputs\apk\release\app-release.apk
```

Para la distribución de QA, usa `app/build/outputs/apk/debug/app-debug.apk` con ese mismo helper. No subas solo `intu.apk`: el aviso de actualización necesita `latest.json` y `releases/<versionCode>/intu.apk`. La API conserva `size`/`uploaded` para la web y añade versión, Android mínimo, SHA-256 y URL fija. Si falta metadata o no corresponde al APK actual, la descarga sigue funcionando y no se anuncia una actualización. Incrementa siempre `versionCode`; el helper rechaza republicar un código anunciado.

## Publicar la página

```powershell
npx.cmd wrangler pages deploy --branch main
```
Sin `--branch main`, wrangler usa la rama de git actual (`codex/intu-mvp`) y publica solo una vista previa;
`viajaconintu.pages.dev` mostraría «Nothing is here yet».

## Probar en la computadora

```powershell
npx.cmd wrangler r2 object put intu-apk/intu.apk --file ..\app\build\outputs\apk\release\app-release.apk --content-type application/vnd.android.package-archive --local
npx.cmd wrangler pages dev --port 8788
```

## Acceso de testers de Google Play

El botón adicional conserva todos los enlaces de descarga del APK. Guarda el correo con consentimiento,
avisa a los administradores que permiten «Reportes y testers» y muestra los pasos para que el propio
usuario se una al grupo gratuito. Registrar un correo en Intu no incorpora a esa persona a Google Groups
ni a Play. La app 1.35 agrega la bandeja Admin → Testers con revisión y archivo de registros.

Grupo creado: `viajaconintu-satipo-testers@googlegroups.com`, vinculado a la prueba cerrada Alpha (Perú).
No se necesita Workspace ni una API para gestionar miembros: los usuarios se unen libremente en Groups.
La lista de miembros y sus correos está restringida al propietario; los miembros no pueden publicar.

`wrangler.toml` contiene los enlaces y dos interruptores. `TESTER_ACCESS_READY` habilita el formulario;
`TESTER_PLAY_READY` debe permanecer en `false` mientras Alpha no esté disponible para los miembros.
Solo se activa tras comprobar la publicación de la prueba cerrada en Play. El enlace de prueba interna
existente sigue teniendo su propia lista de correos y no admite automáticamente a los miembros de este grupo.

Pages necesita el secreto `TESTER_GATEWAY_KEY`, 32 bytes aleatorios codificados como 64 caracteres hex.
Su SHA-256 se configura en `private.tester_gateway`. Nunca se guarda en Git ni se envía al navegador.
La clave de esta instalación está fuera del proyecto en `~/.gradle/intu-tester-gateway/gateway.key`.
Al rotarla, actualizar ambos destinos y volver a desplegar Pages. Un desajuste impide enviar solicitudes.

La migración `play_tester_requests` crea tablas privadas con RLS y funciones con permisos explícitos.
Solo Pages con el secreto puede enviar correos al RPC anónimo; las consultas y revisiones comprueban
el rol Admin con el UID de Firebase. No se guarda la IP: se usa un digest con secreto para limitar envíos
(5 por 10 minutos, 30 al día y 1000 globales al día). Los intentos anteriores a dos días se purgan al enviar.
Un correo duplicado recibe la misma respuesta y no genera un segundo aviso. Los avisos reutilizan el
tipo `bug_report` de la función push ya desplegada, sin incluir el correo en el texto de la notificación.

Pruebas del endpoint: `node --test web/test/testers.test.mjs`.

## Dominio propio

Cuando compres `viajaconintu.com`: Cloudflare → Workers & Pages → viajaconintu → Custom domains.
Luego cambia `viajaconintu.pages.dev` en `public/index.html` (etiquetas `og:` y el código QR).
