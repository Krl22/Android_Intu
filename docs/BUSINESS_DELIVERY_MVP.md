# Delivery de negocios: MVP de pruebas

En **Cuenta → Panel admin → Negocios** se controla exclusivamente la publicidad y los pedidos de negocios. **Enviar** sigue funcionando para paquetes normales. La función de negocios comienza **desactivada**.

## Cómo probar

1. Aprueba una cuenta de conductor con **moto lineal** y selecciónala en **Repartidores de prueba**, dentro de Negocios. Usa otra cuenta como cliente. El repartidor debe estar conectado y cerca del recojo.
2. Edita Cocina Demo o Tienda Demo, o agrega otro anuncio. Configura nombre, categoría, título, descripción, imagen HTTPS opcional, dirección, punto de recojo en mapa y orden. Publica el anuncio y activa **Delivery de negocios**.
3. En Inicio, abre un anuncio de **Más de tu ciudad** y pulsa **Probar delivery en moto**. Selecciona el destino, revisa tarifa y método de pago, y completa paquete y destinatario.
4. El repartidor seleccionado recibe una solicitud de moto con el origen marcado **[DEMO]**. Acepta, llega al negocio, recoge el paquete, inicia el envío, confirma pago y entrega. El PIN sigue la configuración de Seguridad del panel.
5. Desactiva negocios para ocultar anuncios y bloquear nuevas solicitudes. Los pedidos ya creados conservan su flujo y pueden terminar. Retirar un anuncio o quitar un repartidor de pruebas tampoco bloquea un pedido ya asignado.

Los cambios aparecen al volver a Inicio o durante la actualización cada 15 segundos mientras la pantalla está visible. Un fallo temporal conserva los anuncios cargados y ofrece reintentar. El servidor verifica el interruptor y el anuncio en cada nueva solicitud.

## Alcance

Todos los anuncios son ficticios y se identifican como **DEMO**. Este MVP comprueba presentación, administración, recogida, tarifa y seguimiento; no tiene catálogo de productos, carrito, compras ni cobro de mercadería. El pago de transporte corresponde al destinatario, al entregar, utilizando el mismo flujo de moto lineal existente. Las dos muestras usan puntos ficticios en Satipo; ajustarlos antes de una prueba física.

Los anuncios pueden quedar en borrador, publicarse, editarse o retirarse. El editor impide guardar campos incompletos e imágenes sin HTTPS. Las imágenes son enlaces; no se agregó carga de archivos.

## Servidor

Migraciones `business_delivery_mvp` y `business_delivery_quote_guard`, aplicadas al proyecto de desarrollo `vkguzpciwpfvaeyedepl`.

- Anuncios, interruptor y selección de repartidores en tablas privadas, sin acceso directo de clientes; RLS con denegación explícita y RPC públicas invoker sobre funciones privadas que validan Firebase UID/permisos de admin.
- Los pedidos usan `rides` y `delivery_details`, con las tarifas, PIN, pago, cancelación y estados existentes. El servidor fija el recojo desde el anuncio y conserva nombre/dirección como instantánea del pedido.
- `business_ad_id` y `business_name` no admiten INSERT/UPDATE desde clientes. RLS oculta solicitudes demo a repartidores no seleccionados; un trigger también bloquea que `accept_ride`, con permisos elevados, eluda esa restricción.
- La versión del anuncio se comprueba al crear el pedido: una edición durante la preparación invalida la cotización, evitando enviar una ruta calculada para un recojo anterior.
- Editar o retirar anuncios valida su versión, evitando sobrescribir cambios de otro admin. Los retiros son archivos lógicos para mantener el historial.

## Verificación

`BusinessDeliveryTest`: presentación clara/oscura, detalle del anuncio, pedido con origen fijo y pago al destinatario, limpieza al volver, controles del panel, validación del editor y elección de recojo en mapa. Usa ubicaciones, rutas y solicitudes locales, sin pedidos reales.

`BusinessAdTest`: validación de texto, coordenadas, orden e imagen.

`supabase/tests/business_delivery_mvp.sql`: permisos, filtro del feed, repartidores de prueba, aislamiento y privacidad, tarifa, versiones, cambios de anuncio, apagado, envíos normales y ciclo completo de entrega/pago. Toda la prueba hace rollback. También se ejecutan `delivery_lifecycle.sql` y `ride_pin_admin_switch.sql` para comprobar compatibilidad.

Los asesores de Supabase conservan avisos previos sobre RPC existentes y descubrimiento del esquema GraphQL; las nuevas RPC públicas no son security definer. El nuevo índice de la relación anuncio/pedido puede figurar inicialmente como [índice sin uso](https://supabase.com/docs/guides/database/database-linter?lint=0005_unused_index) mientras no existan pedidos demo persistidos.
