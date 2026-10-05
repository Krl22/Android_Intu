# Intu: preparación para pruebas privadas en Google Play

Preparación inicial del 1 de octubre de 2026. Aplicación `com.intu.taxi`, Android 1.14 / código 15.

**Actualización del 5 de octubre:** Carlos confirmó que su cuenta de Play Console está verificada. Se regeneró y validó el AAB **1.31/código 32** con todos los cambios integrados en `main`; la entrega actual es `build/play/intu-1.31.aab`. Firma release, lint, bundletool, ELF ARM64 y APK universal con alineación de 16 KB: correctos. Ver `docs/RELEASE_1_31.md` para huellas y comprobaciones. Las referencias a 1.14/1.15 más abajo son el historial de preparación, no la entrega actual. No se ha publicado en Play Console: siguen pendientes su configuración, el certificado de Play en Firebase y la instalación de comprobación; también las decisiones legales documentadas aquí.

La vía inicial es **Prueba interna**, con lista de correos Google y enlace de inscripción, hasta 100 testers. No aparece en búsquedas públicas mientras solo tenga versiones internas/cerradas. No se ha creado una app ni subido una versión a Play Console desde esta sesión.

## Cambios de esta entrega

- Cuenta → Eliminar cuenta abre un diálogo específico. No exige motivo ni contraseña. Envía una solicitud autenticada a `bug_reports`, que el administrador ya puede leer en Reportes.
- Solo muestra «Solicitud recibida» después de un envío correcto. Una falla conserva el texto y permite reintentar; durante el envío deshabilita las acciones para evitar doble envío. Cancelar no envía nada.
- La confirmación explica que el proceso es manual y que la cuenta sigue activa. No cierra sesión ni cancela viajes al enviar la solicitud.
- Terms describe ese procedimiento. Sigue marcado como borrador porque Carlos confirmó que responsable, contacto y retención aún no están definidos.
- `web/legal/` contiene plantillas de privacidad y solicitud de eliminación por correo, independientes de la página de descarga. No se copian ni despliegan sobre `web/public` hasta revisar y completar la configuración.
- `scripts/prepare-play-bundle.ps1` prepara el AAB con una clave **de subida** exclusiva para Play, guardada fuera del repositorio. No modifica las propiedades de firma permanentes ni reemplaza el APK de QA en R2.

## Preparar las páginas finales

Copiar `web/legal/config.example.json` a un archivo local y completar todos los campos con decisiones reales del responsable:

```powershell
node scripts/prepare-play-legal.cjs RUTA_CONFIG_JSON
```

El generador valida campos obligatorios, escapa el contenido HTML y entrega `build/play-legal/privacidad.html`, `eliminar-cuenta.html` y `legal.css`. Rechaza la configuración vacía incluida como ejemplo. **No publica** las páginas. Antes de copiarlas a `web/public`, comprobar que el correo recibe mensajes y revisar que los plazos y datos conservados coincidan con el proceso real. Después podrán usarse `/privacidad` y `/eliminar-cuenta` en Play Console. La ruta web debe permitir pedir la eliminación sin reinstalar Intu.

El correo `contacto@viajaconintu.com` que aparece en el pie de la web existente no se reutilizó: el README del sitio todavía describe ese dominio como pendiente de compra y Carlos no ha confirmado un buzón operativo.

## Atención de solicitudes

1. En Reportes, localizar «Solicitud de eliminación de cuenta» y su identificador de cuenta. Confirmar que corresponde a la persona solicitante mediante el contacto verificado de la cuenta; el nombre visible por sí solo no es una comprobación de identidad.
2. Contactar al usuario para informar del plazo y de cualquier conservación necesaria. Resolver viajes abiertos y comprobar que no se elimine al último administrador.
3. No marcar «Resuelto» por haber recibido la solicitud. Solo hacerlo cuando se haya completado y comunicado el resultado.

**La eliminación administrativa actual requiere revisión antes de prometer un borrado completo en una política pública**: `admin_delete_user` quita perfiles, conductores, vehículos, ubicaciones y tokens; desvincula las identidades de los viajes, pero conserva sus direcciones/coordenadas/ruta, importes y otros campos. `bug_reports` conserva mensajes tras quitar el perfil; `private.deleted_accounts` conserva identificadores para bloquear perfiles antiguos. `adminDeleteUser` borra Firebase Auth y trata de borrar el avatar, pero no purga todos los datos heredados de Firestore; la falla de avatar se registra sin impedir el éxito.

Por ello no se debe describir el historial conservado como completamente anónimo ni garantizar que todos los datos ya se borran. Definir primero la retención legítima y aplicar/verificar una purga o transformación que la cumpla en Supabase, Firebase Storage y Firestore, incluyendo mensajes libres, copias históricas y respaldos. Este trabajo no ha cambiado tablas ni funciones de producción mientras las otras sesiones validan login y viajes.

## Servicios del viaje: declaración preparada

Se conservan los tipos actuales para no romper el seguimiento que QA está validando. La aprobación del uso `specialUse` corresponde a Google; una declaración no la garantiza.

| Permiso / servicio | Descripción basada en el código | Cómo demostrarlo |
| --- | --- | --- |
| `FOREGROUND_SERVICE_LOCATION` / `DriverOnlineService` | El conductor aprobado se pone en línea con la app visible y concede ubicación. El servicio publica su ubicación para solicitudes y viajes; mantiene una notificación visible y termina al quedar fuera de línea sin viaje o sin sesión. | Mostrar activación voluntaria, permiso, notificación con la app minimizada, actualización del conductor en el pasajero, desconexión y retirada de la notificación. |
| `FOREGROUND_SERVICE_SPECIAL_USE` / `RideTrackingService` | Al pedir/retomar un viaje, el pasajero mantiene el estado del viaje y la ubicación del conductor actualizados al minimizar. La notificación identifica el viaje; el servicio se detiene al terminar/cancelar. No es una carga de archivos ni un servicio que obtiene el GPS del pasajero. | Mostrar solicitud, aceptación, llegada, viaje en curso, cambios en segundo plano, vuelta al mapa y terminación/cancelación con retirada de la notificación. |

El video y las cuentas de revisión deben permitir repetir esas funciones. No inventar una URL de demostración. La declaración del conductor debe explicar el acceso mientras la app está minimizada; Play puede someterlo a requisitos de ubicación en segundo plano según el comportamiento, aun sin `ACCESS_BACKGROUND_LOCATION`. Antes de ampliar a prueba cerrada/producción, revisar la divulgación destacada de ubicación y consentimiento junto al permiso.

Si Play no admite `specialUse`, habrá que sustituirlo por notificaciones push y reanudación del seguimiento al volver a la app, y probar ese cambio. No cambiarlo artificialmente a `location`: este servicio no consume la ubicación del dispositivo del pasajero.

## AAB y acceso con Google/SMS

```powershell
.\scripts\prepare-play-bundle.ps1 -CreateUploadKey
.\scripts\prepare-play-bundle.ps1 -Build
```

La clave y su configuración están en `%USERPROFILE%/.gradle/intu-play/`; mantener una copia privada de ambos archivos. `-CreateUploadKey` no reemplaza una clave existente. `-Build` firma solo esa ejecución, corre `bundleRelease` y `lintRelease`, verifica la firma del AAB y copia la entrega a `build/play/intu-play.aab`. Las huellas públicas están en `build/play/upload-certificate.txt`.

La clave de subida **no es necesariamente la clave de firma de los APK que entregará Google Play**. Al configurar Play App Signing, registrar en Firebase y en los clientes OAuth correspondientes SHA-1/SHA-256 del certificado de **firma de la app** que muestra Play Console. Mantener las huellas de QA. Descargar/revisar `google-services.json` si la configuración de clientes cambia; coordinar ese archivo con la sesión de login. Verificar Google y SMS con una instalación real desde el track interno.

No asumir actualización directa desde el APK debug de QA: si Google usa otro certificado de firma para el mismo package, Android no puede actualizar la instalación existente. Conservar los datos y resolver la transición antes de desinstalar una app de un tester.

## Formularios y lanzamiento

- Mantener `targetSdk = 36`, ARM64 y compatibilidad de librerías nativas/paquetes generados con páginas de 16 KB.
- Crear ficha con nombre Intu, descripción real, icono, capturas y categoría; completar acceso de revisión, clasificación, público objetivo, anuncios y declaraciones que solicite Console.
- Antes de prueba cerrada/producción, completar Seguridad de los datos según los SDK y comportamiento reales: identidad/contacto, ubicación, fotos, documentos de conductor, viajes, reportes y token push. Evaluar la telemetría de Mapbox y los procesamientos por terceros antes de afirmar que no se comparte ningún dato. El track exclusivamente interno está exento de mostrar la sección Seguridad de los datos, pero no debe usarse para prometer cumplimiento general.
- Para una cuenta personal creada después del 13 de noviembre de 2023, acceso a producción requiere prueba **cerrada** con al menos 12 testers inscritos continuamente durante 14 días; las pruebas internas no sustituyen ese requisito.

## Fuentes oficiales consultadas

- [Pruebas internas/cerradas/abiertas](https://support.google.com/googleplay/android-developer/answer/9845334?hl=es).
- [Pruebas para nuevas cuentas personales](https://support.google.com/googleplay/android-developer/answer/14151465?hl=es).
- [API objetivo](https://developer.android.com/google/play/requirements/target-sdk).
- [Eliminación de cuentas: admite solicitudes gestionadas manualmente y vía correo](https://support.google.com/googleplay/android-developer/answer/13327111?hl=es).
- [Servicios en primer plano](https://support.google.com/googleplay/android-developer/answer/13392821?hl=es).
- [Ubicación en segundo plano](https://support.google.com/googleplay/android-developer/answer/9799150?hl=es).
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=es).
- [Certificado de Google Play para Firebase Auth](https://firebase.google.com/docs/android/troubleshooting-faq).
- [Páginas de memoria de 16 KB](https://developer.android.com/guide/practices/page-sizes).
- [Seguridad de la Data API de Supabase](https://supabase.com/docs/guides/api/securing-your-api).

## Validación

- QA confirmó **3/3 AccountDeletionTest** en el Samsung, incluidos en **22/22 tests nativos** sobre el APK final compartido; también **35/35 JVM**. Los tests de eliminación usan callbacks locales: no enviaron solicitudes a producción ni eliminaron cuentas. El APK 1.14 que QA publicó en R2 incluye las tres sesiones.
- `bundleRelease` y `lintRelease` correctos; **0 errores, 71 avisos, 10 sugerencias** de lint release. No se recompiló/instaló otro APK debug ni se sobrescribió el objeto R2 desde esta sesión.
- AAB actualizado con inicio del selector de direcciones en GPS: `build/play/intu-play.aab`, **34,300,553 bytes**, SHA-256 **47455F622B310DDEC29876FAF5674AB5B079F9CCC0A43F18C1056267B4486A73**. Firma JAR válida con certificado `CN=Intu Upload`, distinto del debug. Certificado público guardado en `build/play/upload-certificate.txt`. Conserva versión 1.14/código 15, aún sin entregas en Play.
- `bundletool` oficial **1.18.3** valida el AAB. La configuración indica `PAGE_ALIGNMENT_16K`; las cuatro librerías ARM64 del AAB tienen segmentos ELF alineados a 16 KB.
- Se generó un APK universal desde el AAB para verificar empaquetado: `zipalign -c -P 16 4` y `apksigner verify` pasan. `aapt dump badging` confirma package, versión **1.14/15**, SDK mínimo **24**, objetivo **36**, ARM64/ARM32 y ausencia de `debuggable`. No se instaló ese release sobre el teléfono de QA.
- Consulta de solo lectura en Supabase confirmó RLS activa, inserción autenticada ligada a `private.requesting_uid()`, sin permiso del cliente para insertar `user_id`, sin lectura directa de reportes por usuarios normales, sin inserción anónima y sin ejecución anónima de la RPC de reportes. La función de listado comprueba `is_admin()`. No se cambiaron permisos ni datos.
- Las páginas finales no se generaron: el generador rechazó correctamente los nueve campos sin definir. **2/2 tests** del generador pasan.

No se subió ninguna versión a Google Play ni se publicaron páginas legales incompletas. Falta la validación con una instalación real desde Google Play una vez configurada su firma en Firebase, además de las decisiones de privacidad/retención y la aceptación de las declaraciones de servicios por Play Console. La recepción de SMS reales en Perú y el viaje completo entre dos teléfonos siguen fuera de esta verificación.

El AAB ya se regeneró con `CurrentPinLocation.kt` y los cambios de GPS de Casa/Trabajo/Favorito. La sesión de direcciones confirmó pruebas nativas del GPS inicial, conservación del pin arrastrado, timeout/cancelación y reapertura de puntos editados. Se repitieron `bundleRelease`, `lintRelease`, verificación de firma, `bundletool validate`, alineación ELF ARM64 y generación/verificación del APK universal sobre este nuevo AAB; todos pasan.

Durante esa compilación Carlos pidió a la otra sesión cambiar el flujo de Home para elegir el punto de recojo después de elegir mototaxi y confirmar viaje. Ese cambio posterior **no está incluido** en este AAB. Antes de una subida definitiva, terminar y validar ese flujo y regenerar con el helper; coordinar incremento de versión/código si se publica otra actualización QA/web. El APK web y el AAB son entregas distintas: no se reemplazó el objeto R2 ni se instaló un release en el teléfono desde esta sesión.

QA reservó después **1.15/código 16** para los nuevos cambios de Home y atajos de direcciones guardadas. La compilación de esa versión corresponde a QA; no hay un AAB 1.15 generado aquí todavía. El archivo `build/play/intu-play.aab` sigue siendo la entrega 1.14/GPS indicada por su hash arriba.
