# Intu 1.3 — Cuenta, solicitudes de conductor y Viajes

Versión Android: 1.3, código 4. Firma de QA: la misma clave debug autorizada en Firebase para 1.2.

## Comportamiento

- Enviar documentos registra una solicitud y vuelve a Cuenta en modo pasajero. No activa el modo conductor. Mientras está pendiente aparece «Solicitud en revisión», con una acción para consultar el estado actualizado. No se ofrece otra solicitud ni el selector de conductor.
- El selector solo aparece con aprobación del servidor y datos completos. Se comprueba de nuevo la aprobación al activar el modo. La carga de Cuenta corrige el modo antiguo si una cuenta sin aprobación quedó marcada como conductor.
- La solicitud deshabilita su botón mientras se envía. El modo se persiste una vez, antes de actualizar la interfaz.
- Viajes usa el degradado teal/índigo de Intu. Al tocar una tarjeta se abre el detalle con direcciones, precio, distancia estimada y mapa de la ruta planificada. No se presenta como un registro GPS real.
- Los nuevos viajes guardan la geometría de la ruta que Inicio ya calculó en `rides.route_polyline`. El historial reutiliza esa geometría sin otra consulta a Directions. Para viajes anteriores, solo consulta Directions al abrir el detalle y conserva el resultado durante esa visita a Viajes.
- Mapbox conserva la versión 11.16.4 y usa su variante `android-ndk27`, con librerías nativas compatibles con páginas de memoria de 16 KB.
- Se quitaron de Cuenta Idioma, Historial de viajes y Notificaciones. El historial permanece en la barra inferior y el permiso de notificaciones se solicita desde Inicio.
- Reportar un error envía título, descripción, pantalla opcional, versión de Intu, modelo y versión de Android. El formulario informa qué datos se incluyen y conserva el texto si falla el envío.
- El panel de administración tiene Reportes, con acciones para revisar, resolver y reabrir. Supabase permite insertar reportes con la cuenta autenticada; solo las funciones administrativas pueden leerlos o cambiar estados.
- Casa, Trabajo y favoritos de Inicio eligen un destino guardado y mantienen la confirmación habitual del viaje. Se puede agregar, editar y quitar desde Cuenta, o guardar el punto elegido con Marcador. Los lugares son locales al teléfono y están separados por uid; todavía no se sincronizan entre equipos.
- Términos y privacidad abre un borrador accesible desde Cuenta y Login. Se basa en las funciones reales de Intu y ofrece solicitudes sobre datos mediante el formulario que llega al administrador.

## Validación

- 14 pruebas de JVM pasaron: acceso de conductor, geometría del historial, teléfono, mapa y Supabase Realtime.
- La migración `bug_reports` se aplicó a Supabase. Una transacción temporal verificó envío autenticado, denegación de lectura/cambio a usuarios normales, lectura/resolución administrativa y denegación a anónimos. Se revirtió todo el contenido temporal.
- 7 pruebas distintas de interfaz pasaron en el emulador Android API 37 con páginas de 16 KB: cuatro del formulario inicial, persistencia de favoritos entre cuentas, validación del reporte y detalle del viaje con MapView nativo. Las dos últimas se repitieron tras ajustar la espera hasta `MapLoaded`, que confirma el dibujo de las teselas visibles. Las capturas finales muestran el mapa, la ruta y ambos marcadores, y el formulario completo.
- Compilación final `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` y `lintDebug` correcta; lint informa 0 errores, 67 advertencias y 9 sugerencias. Las librerías ARM64 y x86_64 tienen alineación ELF de 16 KB; `zipalign -c -P 16 4` también pasa.
- El primer arranque del emulador tuvo ANR en componentes de Android y timeout de inicio de la instrumentación. Se estabilizó el entorno, se repitieron las pruebas y se revisaron capturas sin diálogos del sistema. No se validó todavía un viaje completo entre dos teléfonos físicos.

## APK publicado

- Publicado en el objeto `intu-apk/intu.apk` de Cloudflare R2 el 1 de octubre de 2026 a las 03:46:38 UTC (30 de septiembre, 23:46 en Nueva York).
- [Descarga 1.3 QA](https://viajaconintu.pages.dev/descargar?v=1.3-qa-4), servido por la web existente de Cloudflare Pages.
- Se descargó el archivo completo desde esa URL y coincide con el APK local: 108,613,264 bytes; SHA-256 `CAB2ADBCEE46E26082870A37E140402DC9374ABEFDB95F5E72061499AB7C3E23`.
- La descarga verifica firma y versión 1.3 / código 4. Mantiene la firma debug de la versión 1.2 QA, registrada en Firebase. Puede actualizar esa instalación; no sustituye una instalación firmada con la antigua clave release.

## Textos de privacidad pendientes de definición del negocio

El texto está marcado «Versión de prueba · Borrador para revisión». Antes de presentarlo como política final, completar la identidad y domicilio del responsable de Intu, contacto definitivo, plazos de conservación y procedimiento para responder solicitudes sobre datos. No se inventaron esos datos ni promesas de certificación o cumplimiento.

Referencias consultadas: [Mapbox: facturación por producto](https://docs.mapbox.com/accounts/guides/pricing/) y [ANPD: derechos ARCO](https://www.gob.pe/9269-iniciar-procedimiento-para-el-ejercicio-de-derechos-de-acceso-rectificacion-cancelacion-y-oposicion).
