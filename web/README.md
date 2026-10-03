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

## Dominio propio

Cuando compres `viajaconintu.com`: Cloudflare → Workers & Pages → viajaconintu → Custom domains.
Luego cambia `viajaconintu.pages.dev` en `public/index.html` (etiquetas `og:` y el código QR).
