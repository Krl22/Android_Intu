# Intu 1.33 — avisos de actualización con la app cerrada

Versión 1.33, código 34. Conserva la firma de QA.

- Notificación local de Android cuando hay una versión compatible superior a la instalada. Comprobación periódica cada hora mediante WorkManager, con red disponible y respetando el permiso/canal de notificaciones; Android puede retrasar la ejecución por ahorro de batería.
- Guarda la última versión anunciada para no repetir avisos. Una nueva versión reemplaza la notificación anterior; instalar la versión anunciada retira el aviso.
- Tocar la notificación abre Intu, comprueba la versión actual y espera a una pantalla segura, fuera de viajes/envíos, para mostrar el diálogo.
- Aviso automático también para conductores libres. Comprobaciones mientras la app está visible, con caché de quince minutos y reintento tras cinco minutos si falla.
- Cuenta muestra la versión disponible aunque se haya pospuesto el diálogo. Actualizar continúa abriendo la descarga versionada en el navegador, con confirmación de instalación a cargo de Android.

## Validación

- Siete pruebas JVM de AppUpdatesTest aprobadas.
- Ocho casos nativos de AppUpdateDialogTest y AppUpdateNoticeTest aprobados en el emulador API 37.
- Tres casos de AppUpdateNotificationsTest aprobados sobre el APK final: notificación real/deduplicación/limpieza, programación única y apertura de MainActivity al tocar.
- Caso independiente de permiso denegado aprobado; comprueba que concederlo después permite anunciar la misma versión.
- assembleDebug y assembleDebugAndroidTest correctos. lintDebug: cero errores; conserva advertencias y recomendaciones del proyecto.
- Instalación con adb install -r en Samsung SM_F976U correcta, sin desinstalar. Package confirma 1.33/código 34. No se realizaron pruebas de interfaz en el teléfono físico.

APK y registros de validación: build/qa-app-updates-1.33. El usuario debe abrir Intu una vez después de instalar para registrar las comprobaciones. El APK queda preparado localmente; esta sesión no publica una nueva versión en la web.

APK final: 113327886 bytes. SHA-256: `760523ce3de3201b4c870c7e73ea264edb1a0af0b02da98dde6d2c3f037cf832`.
