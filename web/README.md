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
2. Súbelo (reemplaza al anterior; la página muestra solo el nuevo tamaño y fecha):

```powershell
npx.cmd wrangler r2 object put intu-apk/intu.apk --file ..\app\build\outputs\apk\release\app-release.apk --content-type application/vnd.android.package-archive --remote
```

## Publicar la página

```powershell
npx.cmd wrangler pages deploy
```

## Probar en la computadora

```powershell
npx.cmd wrangler r2 object put intu-apk/intu.apk --file ..\app\build\outputs\apk\release\app-release.apk --content-type application/vnd.android.package-archive --local
npx.cmd wrangler pages dev --port 8788
```

## Dominio propio

Cuando compres `viajaconintu.com`: Cloudflare → Workers & Pages → viajaconintu → Custom domains.
Luego cambia `viajaconintu.pages.dev` en `public/index.html` (etiquetas `og:` y el código QR).
