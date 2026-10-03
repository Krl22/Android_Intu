# Intu 1.26 — avisos de actividad para administradores

Versión 1.26 / código 27. Publicada el 2 de octubre de 2026 a las 15:09 (America/New_York), 19:09:42.584 UTC.

## Comportamiento

- Administración tiene una pestaña Notificaciones. Cada administrador puede pausar todos sus avisos o elegir usuarios nuevos, solicitudes de viaje/envío, postulaciones de conductores y reportes de errores. Las opciones se guardan en su cuenta y se aplican a sus dispositivos registrados. El valor inicial es activado para los cuatro tipos.
- La pausa general conserva las elecciones individuales. Las ediciones se bloquean durante el guardado y, si falla, se conserva el valor confirmado por el servidor y se muestra el error.
- El panel detecta si Android bloquea las notificaciones o el canal Actividad de Intu y ofrece solicitar el permiso o abrir los ajustes. Los avisos de los propios viajes mantienen su canal y comportamiento.
- Los eventos se generan en Supabase cuando se guarda actividad nueva: INSERT de profiles, rides en búsqueda, postulaciones pendientes y bug_reports. Una actualización ordinaria de un perfil/viaje/reporte no crea otro aviso. Una postulación reenviada desde otro estado vuelve a avisar.
- El servidor filtra destinatarios por membresía actual en private.admins y preferencias. No se usan topics a los que un cliente pueda suscribirse por su cuenta.
- Firebase envía datos de actividad con prioridad alta y TTL de 10 minutos. Intu los procesa tanto en primer plano como en segundo plano; comprueba la cuenta receptora y vuelve a consultar el permiso/preferencias antes de mostrar el aviso. Las solicitudes HTTP de esa comprobación tienen un timeout acotado. Si falla la autorización o cambió la cuenta, descarta el mensaje.
- El canal administrativo tiene visibilidad privada en la pantalla de bloqueo. Cada evento tiene un tag propio, conservando solicitudes independientes; una entrega repetida del mismo evento reemplaza su aviso. Pulsar el aviso abre Administración después de comprobar nuevamente la cuenta y su permiso.

## Backend y configuración

- Migración 20261002185506_admin_activity_notifications aplicada en vkguzpciwpfvaeyedepl. Preferencias en tabla privada con RLS y sin acceso directo para anon/authenticated; las dos RPC públicas comprueban que el usuario autenticado sea administrador. Solo puede modificar sus propias preferencias. Quitar el permiso de admin elimina esas preferencias por FK y detiene los envíos.
- ridePush desplegada de forma aislada en Firebase intu-e8403. Conserva el payload de estado de viaje y añade la variante administrativa; los demás exports no se desplegaron.
- Se encontró una configuración previa incompatible de push_webhook_secret en Vault: Firebase devolvía HTTP 403. Se alineó Vault con el secreto fuerte ya configurado en Firebase. Una prueba posterior fue aceptada (HTTP 200). El valor no se imprimió y el archivo temporal local se eliminó. Esto también repara la autenticación de los avisos existentes de viajes.

## Verificación

- Build final de app y APK de pruebas, 52 pruebas JVM y lint PASS. JVM: 0 fallos / 0 errores. Lint: 0 errores, 75 advertencias y 10 sugerencias existentes.
- Ocho pruebas Node PASS (avisos administrativos y account-access). Verifican payload administrativo sin notificación automática de Android, destinatario explícito, tipos permitidos y tamaños acotados.
- Dos pruebas nativas del panel PASS en Phone_1, API 37 (OK 2 tests, 11.971 s): opciones individuales, pausa/reanudación conservando elecciones, bloqueo durante guardado y acciones ante permiso denegado/carga fallida. El emulador se cerró al terminar.
- SQL admin_activity_notifications.sql PASS con rollback: acceso denegado a no administradores/sin sesión, preferencias, mute general/individual, seis eventos en pg_net (dos usuarios, postulación, viaje, envío y reporte), ausencia de duplicados por actualizaciones y revocación. Los datos y solicitudes de esa transacción no se conservaron; verificado que quedaron cero perfiles ficticios.
- Lectura/escritura/readback de las RPC con SET LOCAL ROLE authenticated PASS en otra transacción con rollback. Verificados grants: anon no puede llamar las RPC; authenticated no puede leer la tabla privada ni llamar al filtro interno de destinatarios.
- Advisors revisados: los avisos nuevos corresponden a la tabla privada deliberadamente sin políticas de acceso directo y RPC SECURITY DEFINER deliberadamente autorizadas tras comprobar el administrador. Referencias: https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy y https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable.
- Prueba del webhook desplegado, request 23: HTTP 200, sent=0 / failed=1 / pruned=1 con un token ficticio no registrado. Confirma aceptación del webhook y rechazo/limpieza de tokens inválidos; no verifica entrega en un teléfono ni envía mensajes a usuarios reales.
- Las pruebas de lógica comprueban que una cuenta distinta, sin sesión, silenciada o cambiada durante la consulta no puede mostrar avisos administrativos.
- Firma debug y alineación de 16 KB verificadas. Certificado SHA-1: 7a4ec69cae01b4b8a5eeeff832d833e176c0efd0; SHA-256: 7ee48b402d20042fcfa7b0db98b91fcae410d12efe7d9f9f3dd8f4be9968fa0d.

## Entrega

- Instalado mediante adb install -r en Samsung SM_F976U conservando datos. Confirmada versión 1.26 / código 27, lastUpdateTime 2026-10-02 14:09:17 (Perú). No se hicieron pruebas interactivas en el celular.
- APK publicado en R2 intu-apk/intu.apk y descargado completo desde https://viajaconintu.pages.dev/descargar?v=1.26-27-admin-notifications.
- El archivo descargado coincide con el validado: 106383155 bytes, SHA-256 37341B94AFD0B35F102101BCC698514850B28D2297FC537CE0826C7AD19CA877.
- Hashes de app/src/main comprobados contra la compilación final antes de entregar; sin cambios posteriores.

Evidencia y APK: build/qa-admin-notifications-1.26 (build-final.txt, unit-results.json, functions-tests.txt, native-results.txt, backend-verification.json, firebase-deploy.txt, phone-version.txt y web-verification.json).
