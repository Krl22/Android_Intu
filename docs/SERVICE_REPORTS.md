# Reportes de servicios y objetos perdidos

Disponibles desde el servicio asignado y desde cada servicio con participantes en el historial. La bandeja está en Viajes → Reportes y objetos perdidos. Admin → Reportes → Servicios y objetos permite revisar los casos y responder.

Motivos: mal trato, acoso/amenazas/discriminación, estafa, robo, conducción peligrosa, problemas de pago, conductor o vehículo diferente, paquete dañado/no entregado, objeto perdido y otros. Los motivos se adaptan al rol y al tipo de servicio. Un reporte siempre deriva la otra persona del viaje; no permite acusar a un usuario arbitrario.

Las denuncias son visibles para quien reporta y para admins. Los objetos perdidos se comparten con el conductor del viaje finalizado (también cancelado después de iniciarse): aviso genérico para revisar el vehículo, respuesta encontrado/no encontrado, mensajes para coordinar y confirmación del pasajero de que recibió su objeto. Esa confirmación resuelve el caso. El conductor puede corregir “no encontrado” si lo encuentra después.

Admin puede marcar recibido, en revisión, resuelto o cerrado sin acción, enviar una respuesta visible y mantener una nota interna. No hay sanciones automáticas. En objetos perdidos la respuesta visible también se comparte con el conductor. Las notas internas nunca aparecen para usuarios sin permiso admin.

Los avisos usan el webhook FCM existente; no incluyen la denuncia, descripción del objeto ni contactos. Las notificaciones requieren el permiso de Android y la entrega de FCM. La bandeja conserva el caso aunque no llegue el aviso, y se actualiza cada diez segundos mientras está abierta.

Las tablas privadas niegan acceso directo. Las funciones validan sesión, participantes y permisos admin. La creación usa un UUID de solicitud para reintentos sin duplicar y limita a un caso activo por persona/viaje/motivo, diez reportes diarios y cinco mensajes por minuto. Cada revisión guarda quién la hizo y la fecha. Bandeja: hasta 200 casos, primero los abiertos; coordinación: últimos 50 mensajes.

Limitación del historial actual: un conductor que libera un servicio antes de iniciarlo deja de ser su conductor asignado. No se reconstruyen participantes anteriores ni se habilitan reportes contra personas ajenas al servicio actual.

## Validación

- `supabase/tests/service_reports.sql`: aislamiento entre participantes y terceros, denuncia privada, notas internas, roles, objeto perdido tras el viaje, duplicados/reintentos, devolución, cierre y permisos con participantes eliminados. Ejecutado con rollback sin persistir casos de QA ni enviar avisos reales.
- Tres pruebas JVM de categorías, descripción y apertura de notificaciones; suite completa: 100 pruebas sin fallos.
- Tres pruebas nativas en emulador: formulario, revisión del conductor y confirmación del pasajero. Capturas en `build/qa-service-reports-1.34`.
