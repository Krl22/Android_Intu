# Avisos de actualización por APK

Desde 1.28/código 29, Intu consulta `/api/apk` al entrar y al volver al primer plano. Las consultas automáticas exitosas se limitan a una cada seis horas dentro del proceso; un fallo permite volver a consultar después de cinco minutos. Cuenta → Actualizaciones comprueba inmediatamente y muestra cargando, versión actual, actualización disponible o error con reintento.

El aviso de nueva versión puede posponerse hasta el siguiente inicio del proceso; no bloquea el acceso. Se muestra en login, Cuenta o Inicio de pasajero con los controles normales visibles, fuera de viajes/envíos activos. No abre automáticamente la descarga. Actualizar abre el APK de esa versión en el navegador; al terminar el usuario abre el archivo y confirma la actualización en Android. La app conserva el flujo de sesión y las direcciones. No solicita nuevos permisos ni anuncia por FCM a usuarios con la app cerrada.

La comparación usa `versionCode`, no el nombre de versión. Se rechazan metadata incompleta, paquetes diferentes, versiones incompatibles con Android y hashes inválidos. Una consulta fallida no afirma que la app esté al día. El botón no ofrece reinstalar versiones iguales o inferiores, ni permite iniciarlo durante un viaje/envío activo. La descarga usa un dominio fijo de Intu y un archivo versionado para evitar cambiar de APK si se publica otra versión mientras se descarga.

## Publicación

Publicar Pages una vez con los cambios de `web/functions/api/apk.js` y `web/functions/descargar.js`. Para cada APK validado, desde la raíz usar:

```powershell
.\scripts\publish-apk.ps1 -ApkPath .\app\build\outputs\apk\debug\app-debug.apk
```

El helper extrae versión y Android mínimo con aapt2, verifica firma, calcula tamaño/hash, sube la copia fija y la descarga actual, y escribe `latest.json` al final. La API solo anuncia metadata que coincide con tamaño/fecha del APK actual y cuya copia fija está disponible. Un código ya anunciado necesita incrementarse para publicar otro binario. Conservar la misma firma para actualizar instalaciones existentes; nunca desinstalar para resolver una firma incompatible.

La primera versión que incluye este sistema debe instalarse manualmente. Las versiones anteriores no tienen el código que detecta actualizaciones. Para una futura distribución por Play se debe conectar el botón al mecanismo de Google Play y su canal, en vez de servir APK de QA.

## Verificación

`AppUpdatesTest`: comparación, metadata inválida/incompatible, descarga fija, intervalos, reintento, concurrencia y cancelación. `AppUpdateDialogTest`: actualizar/posponer, evitar downgrades, error/reintento y bloqueo durante viaje. `scripts/app-updates.test.cjs`: publicación incompleta, compatibilidad con la página, descargas versionadas y Range. Los resultados de Android y distribución se registran en la entrega conjunta 1.28.

Entrega del 2 de octubre de 2026: 5 pruebas del servidor y 6 pruebas JVM del detector aprobadas; los 4 casos nativos del aviso pasaron, incluidos en la revisión final del APK con texto de Android al 180 %. Captura en `build/qa-dark-mode-1.28/screenshots/dark-update-dialog.png`. La entrega conjunta pasó 58 pruebas JVM y lint sin errores; ver `docs/RELEASE_1_28.md`.

API y descarga fija verificadas tras publicar 1.28/código 29: 106550563 bytes y SHA-256 `dffdf45d4dd6c5ad475d9d972b6230b238733daf5bfc298391bb74feaa082800`. El helper verificó el archivo completo antes de anunciarlo; QA comprobó también la descarga actual. La API devuelve el código, tamaño y hash correctos, y la descarga fija responde HTTP 200. El parser de publicación admite `sdkVersion` de aapt2 36 y `minSdkVersion` de aapt2 37; ambas variantes se comprobaron con el APK real. La instalación física continúa pendiente porque el Samsung está desconectado.
