# PIN opcional de viajes y envíos

El administrador lo controla en **Cuenta → Administración → Seguridad → PIN de seguridad**.
La configuración inicial es desactivada. El cambio aplica a las nuevas solicitudes, tanto de
viajes como de envíos; los servicios existentes conservan el requisito que tenían al crearse.

- Desactivado: el pasajero/remitente no ve el PIN y el conductor inicia sin ingresarlo.
- Activado: el pasajero/remitente ve los cuatro dígitos, el conductor los verifica antes de
  iniciar o cobrar la recogida. Se conserva el límite de cinco intentos incorrectos.
- Los envíos mantienen sus reglas de pago, aunque el PIN esté desactivado.
- Una falla de conexión al consultar o guardar no se interpreta como PIN desactivado.

Se requiere el APK actualizado para participantes: versiones anteriores siempre muestran el
diálogo al conductor. La migración del servidor es `20261003220210_ride_pin_admin_switch.sql`
(archivo alineado con la versión registrada por Supabase al aplicarlo).
La preferencia vive en `private.ride_security_settings`, sin acceso directo de clientes;
las funciones administrativas verifican el UID de Firebase y el rol de administrador.
El trigger de inserción fija `rides.start_pin_required`; la app no puede editarlo directamente.

## Verificación (3 de octubre de 2026)

Migración aplicada al proyecto de desarrollo `vkguzpciwpfvaeyedepl`.

- `supabase/tests/ride_pin_admin_switch.sql`: default apagado, permisos de admin,
  reactivación, solicitudes existentes, PIN incorrecto/correcto y pagos sin PIN: PASS.
- `supabase/tests/delivery_lifecycle.sql`: flujo de envíos con PIN activado, privacidad,
  creación atómica y cobros: PASS. Ambos archivos revierten los datos y preferencias de prueba.
- `AdminRideSecurityTest`: dos pruebas de interfaz, guardado pendiente, error y reintento: PASS.
- APK y APK de pruebas compilados correctamente.
- Revisión de seguridad: mismas advertencias previas. La nueva tabla privada tiene RLS y
  permisos revocados, por lo que el aviso informativo de ausencia de políticas es intencional
  ([documentación del aviso](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy)).

Después de las pruebas se confirmó `pin_enabled = false` y cero perfiles de prueba residuales.
