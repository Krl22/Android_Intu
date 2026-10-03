# Intu 1.23 — elección de moto y envíos

Versión 1.23 / código 24. Publicada el 2 de octubre de 2026 a las 11:52 (America/New_York).

## Comportamiento

- Inicio conserva su buscador y accesos guardados; se retiraron los botones Viajar / Enviar paquete.
- El destino conduce a **Elige tu moto**: Mototaxi Honda, Mototaxi Bajaj, Cualquier mototaxi y Moto para envíos. Cualquier mototaxi acepta cualquier marca registrada, según la elección de Carlos. La marca específica se guarda en la solicitud y se aplica tanto a su visibilidad como a la aceptación en el servidor.
- Después de elegir la moto se elige el recojo en el mapa principal. Se conserva Atrás → inicio y el zoom alrededor del pin.
- Moto para envíos usa moto lineal aprobada, exclusivamente para paquetes pequeños. Solicita descripción, nombre y celular peruano del destinatario, referencias opcionales y confirmación explícita de transporte sin compras ni cobro de productos. No requiere que el destinatario tenga cuenta.
- El solicitante elige entre pagar al recoger o que pague el destinatario al entregar. Efectivo y Yape / Plin conservan su operación manual; la app registra la confirmación del repartidor y no procesa el movimiento de dinero.
- Tarifa de prueba de reparto: base S/ 2.00, S/ 0.80 por km, S/ 0.08 por minuto, mínimo S/ 3.20. Son coeficientes 20% menores que los de mototaxi. La tarifa final redondea a S/ 0.10, por lo que cada total mostrado puede diferir ligeramente de un descuento exacto de 20% al total ya redondeado.
- Registro de Moto lineal conserva el código motorcycle y queda pendiente hasta aprobación administrativa. La solicitud y el vehículo se guardan de forma atómica. Cambiar el vehículo aprobado requiere revisión nueva y se impide durante un servicio activo.
- El recojo exige el PIN del solicitante. Si paga quien envía, se verifica PIN y pago antes de iniciar; si paga quien recibe, se confirma entrega y pago antes de finalizar. Un recojo pagado no se puede cancelar ni reasignar desde la app.
- Datos del destinatario accesibles únicamente a quien envía y al repartidor asignado. El repartidor puede abrir el marcador telefónico para contactar a ambas personas. Avisos de estado e historial distinguen viajes y envíos.
- La aprobación de conductor sigue siendo obligatoria. Mototaxi no puede aceptar repartos y moto lineal no puede aceptar solicitudes de pasajeros.

## Backend desplegado

Proyecto Supabase vkguzpciwpfvaeyedepl. Migraciones aplicadas:

1. 20261002141921_register_motorcycle_couriers
2. 20261002143850_delivery_requests
3. 20261002145049_delivery_payment_collection
4. 20261002150042_delivery_pickup_pin_guard
5. 20261002150743_small_delivery_packages
6. 20261002151603_delivery_notifications
7. 20261002151651_delivery_custody_cancel_guard
8. 20261002153844_requested_mototaxi_brand
9. 20261002155003_enable_motorcycle_deliveries

motorcycle está activo, service_kind=delivery y capacidad de pasajeros=0. Las funciones públicas nuevas de solicitud, registro y confirmación de pago son SECURITY INVOKER y no ejecutables por anon. Las operaciones internas privilegiadas comprueban la identidad Firebase y la asignación del servicio.

## Verificación

- Compilación APK debug y APK de pruebas: correcta. 45 pruebas JVM aprobadas. Lint: 0 errores, 74 avisos y 10 sugerencias.
- Samsung SM_F976U, disponible y desbloqueado con confirmación de Carlos: **16 pruebas aprobadas en el APK final**. Registro (3), courier (3), selección/pago/texto grande (2), regreso a inicio (7), ruta y recojo de mototaxi Honda (1). Repetición final: OK (16 tests), 35.315 segundos.
- Los pedidos y rutas de las pruebas de pantalla fueron simulados. No se enviaron SMS ni pedidos reales, ni se cerró la sesión. Instalación final con adb install -r y versión 1.23 / 24 comprobada; se abrió MainActivity al terminar.
- Pruebas SQL de registro/aprobación, entrega/pago/PIN/privacidad y filtros Honda/Bajaj/cualquier marca: aprobadas con fixtures aislados dentro de transacciones revertidas. Cero perfiles QA restantes y cero entregas reales creadas. Se verificó nuevamente la activación del servicio.
- La captura qa-moto-options.png muestra las cuatro opciones en el Samsung. El mapa de esa prueba usa ruta simulada; la captura valida la presentación del panel, no la cobertura real de rutas.
- Un intento inicial de regresión en el emulador no ejecutó casos y reportó Process crashed. No se contabiliza como prueba aprobada; la regresión completa se verificó posteriormente en el Samsung. Se cerró el emulador propio para liberar memoria.
- Firma debug existente verificada y alineación de 16 KB correcta. Coincide con el certificado registrado para login Firebase.
- Fuentes Android congelados y hashes comprobados antes/después de compilar. Incluye el diseño y la ruta turquesa de 1.22, las correcciones previas de login, mapas, reportes y direcciones guardadas.

### Revisión de Supabase

Advisors conserva los avisos previos de funciones públicas con controles internos, políticas privadas sin acceso directo y rendimiento. La nueva tabla delivery_details añade una entrada al [aviso de esquema GraphQL visible para usuarios autenticados](https://supabase.com/docs/guides/database/database-linter?lint=0027_pg_graphql_authenticated_table_exposed): su esquema es visible porque la app necesita SELECT; las filas están protegidas por RLS y la prueba comprueba que repartidores no asignados no pueden leer contactos. Las RPC nuevas no añadieron funciones públicas SECURITY DEFINER.

## Artefacto y publicación

- Snapshot: build/qa-courier-1.23/intu-1.23.apk.
- Tamaño: 105306566 bytes.
- SHA-256: 125192CB800A047B84171F9220EF11A8E6B58AD9CC2ABBE01C19EC56797D3859.
- Certificado SHA-256: 7ee48b402d20042fcfa7b0db98b91fcae410d12efe7d9f9f3dd8f4be9968fa0d.
- Publicado en R2 intu-apk/intu.apk; web https://viajaconintu.pages.dev. Se descargó el archivo completo desde /descargar con parámetro de versión y se comprobaron tamaño y SHA-256 idénticos. La metadata informa 2026-10-02T15:52:45.832Z.
- Evidencia local en el snapshot: native-phone-final-results.txt, backend-verification.json, source-hashes.json, web-metadata.json y qa-moto-options.png. El APK inicial de registro es solo un snapshot interno; el enlace público entrega el APK completo descrito aquí.

La PC permanece encendida; Carlos revocó el apagado automático en la sesión de publicación.
