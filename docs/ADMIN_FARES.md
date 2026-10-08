# Tarifas y propuestas de precio

Panel admin → **Tarifas** permite guardar la base por viaje, el cobro por km y minuto,
el mínimo, el recargo porcentual Honda, el incremento de redondeo y el permiso de propuestas.
Valores iniciales: S/ 1.50, S/ 1.00/km, S/ 0.10/min, mínimo S/ 3.00, Honda 12 % y redondeo S/ 0.10.
Las propuestas empiezan desactivadas. Los envíos mantienen su cálculo independiente.

La app carga la configuración antes de mostrar opciones y la consulta nuevamente al solicitar.
Si cambian los valores durante la selección, pide revisar el nuevo precio y confirmar de nuevo.
El servidor calcula la tarifa de cada nueva solicitud. Los viajes ya solicitados conservan su importe.
Las reservas programadas muestran una estimación y usan los valores vigentes al despacharse.
El mínimo se eleva al siguiente incremento si no es un múltiplo del redondeo, para nunca cobrar menos del mínimo.

Con **Permitir propuestas de precio** activado, un conductor aprobado y disponible puede enviar
una propuesta por solicitud de mototaxi. Puede ofrecer un precio mayor o menor, con dos decimales,
entre S/ 0.01 y S/ 9999.99. No se recalcula ni se añade recargo Honda al importe propuesto.
El pasajero ve el conductor, vehículo, importe propuesto y precio original. Debe aceptar y confirmar
el importe antes de asignar el viaje. Rechazar o seguir buscando mantiene la solicitud y el precio original.
La aceptación comprueba nuevamente disponibilidad, estado, marca, permisos y bloqueos del conductor.
La tarifa acordada se conserva al completar el viaje. El conductor recibe la asignación al sincronizar su pantalla.
El precio de la app sigue disponible para la aceptación normal por cualquier conductor compatible.

Las ofertas quedan en `private.ride_price_offers`; las RPC solo entregan las del participante autorizado.
Solo un admin puede guardar tarifas y habilitar propuestas. Apagar el permiso retira las ofertas pendientes.
La asignación normal, cancelación y vencimiento cierran las propuestas pendientes. La aceptación usa
el mismo orden de bloqueos que la asignación normal: conductor, viaje y propuesta.

Verificación: pruebas JVM de tarifas, precisión, mínimo, marcas y validación; pruebas nativas de edición
admin, envío, rechazo y confirmación del pasajero; `supabase/tests/admin_fares_and_price_offers.sql`
valida permisos, cálculo, reservas, independencia del delivery y oferta hasta el pago final con rollback.
`FareBookingFlowTest` comprueba el flujo actual de planificación, tarifas del drawer y la revisión obligatoria
cuando el admin cambia un precio antes de enviar. Las pruebas históricas de marcas, reservas y contactos
fijan sus coeficientes dentro de la transacción. La antigua prueba `HomeRideFlowTest` comienza desde una
pantalla anterior de la app y no alcanza el planificador actual; la validación de este cambio usa el flujo vigente.
Las tablas privadas tienen RLS sin políticas y sin acceso directo para clientes por diseño.
