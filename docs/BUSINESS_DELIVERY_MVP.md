# Delivery de negocios: MVP de pruebas

En **Cuenta → Panel admin → Negocios** se controla exclusivamente la publicidad y los pedidos de negocios. **Enviar** sigue funcionando para paquetes normales. La función de negocios comienza **desactivada**.

## Cómo probar

1. Aprueba una cuenta de conductor con **moto lineal** y selecciónala en **Repartidores de prueba**, dentro de Negocios. Usa otra cuenta como cliente. El repartidor debe estar conectado y cerca del recojo.
2. Edita Brasa Satipo, Sazón Río Negro o Café Satipo, o agrega otro anuncio. Configura título, ciudad, detalle/precio de promoción, foto demo o imagen HTTPS, dirección, punto en mapa y orden. En **Editar menú**, agrega/edita productos con fotos, precio y disponibilidad; vuelve con **Usar este menú** y guarda el anuncio. Publica y activa **Delivery de negocios**.
3. En Inicio, abre una promoción de **Más de tu ciudad**, elige productos y cantidades y pulsa **Continuar con delivery demo**. Selecciona el destino, revisa tarifa y método de pago y completa destinatario y paquete. Los anuncios sin menú conservan **Probar delivery en moto**.
4. El repartidor seleccionado recibe una solicitud de moto con el origen marcado **[DEMO]**. Al aceptar ve el menú elegido y el subtotal simulado. Llega al negocio, recoge el paquete, inicia el envío, confirma pago y entrega. El PIN sigue la configuración de Seguridad del panel.
5. Desactiva negocios para ocultar anuncios y bloquear nuevas solicitudes. Los pedidos ya creados conservan su flujo y pueden terminar. Retirar un anuncio o quitar un repartidor de pruebas tampoco bloquea un pedido ya asignado.

Los cambios aparecen al volver a Inicio o durante la actualización cada 15 segundos mientras la pantalla está visible. Un fallo temporal conserva los anuncios cargados y ofrece reintentar. El servidor verifica el interruptor y el anuncio en cada nueva solicitud.

## Alcance

Todos los anuncios son ficticios y se identifican como **DEMO**. Hay tres negocios de ejemplo de Satipo y Río Negro, con dos productos cada uno y fotos generadas con Higgsfield. Se simulan menú, cantidades y subtotal de productos, sin compras ni cobro de mercadería. El pago de transporte corresponde al destinatario, al entregar, utilizando el mismo flujo de moto lineal existente. Ajusta los puntos de prueba antes de una prueba física. Las fotos y prompts están documentados en [LOCAL_BUSINESS_PROMOTIONS.md](LOCAL_BUSINESS_PROMOTIONS.md).

Los anuncios pueden quedar en borrador, publicarse, editarse o retirarse. El editor impide guardar campos incompletos e imágenes sin HTTPS. Puedes usar las cuatro fotos incluidas sin red o reemplazarlas con enlaces HTTPS. No hay carga de archivos. Cada menú admite hasta 12 productos, con precios entre S/ 0.10 y S/ 999.99 y dos decimales. El pedido admite hasta 10 unidades por producto y 20 en total; la confirmación sigue requiriendo un paquete pequeño apto para moto.

## Servidor

Migraciones `business_delivery_mvp`, `business_delivery_quote_guard` y `local_business_promotion_banners`, aplicadas al proyecto de desarrollo `vkguzpciwpfvaeyedepl`. La nueva migración reemplaza solo las muestras originales sin editar y conserva el estado del interruptor y los repartidores seleccionados.

- Anuncios, interruptor y selección de repartidores en tablas privadas, sin acceso directo de clientes; RLS con denegación explícita y RPC públicas invoker sobre funciones privadas que validan Firebase UID/permisos de admin.
- Los pedidos usan `rides` y `delivery_details`, con las tarifas, PIN, pago, cancelación y estados existentes. El servidor fija el recojo desde el anuncio y conserva nombre/dirección como instantánea del pedido.
- `business_ad_id` y `business_name` no admiten INSERT/UPDATE desde clientes. RLS oculta solicitudes demo a repartidores no seleccionados; un trigger también bloquea que `accept_ride`, con permisos elevados, eluda esa restricción.
- La versión del anuncio se comprueba al crear el pedido: una edición durante la preparación invalida la cotización, evitando enviar una ruta calculada para un recojo anterior.
- Los menús se guardan atómicamente con el anuncio. El cliente envía únicamente IDs y cantidades; el servidor valida disponibilidad/límites y conserva nombres/precios y subtotal en `business_order_items` / `business_products_total`. Son columnas sin INSERT/UPDATE para clientes. El subtotal no cambia la tarifa de transporte ni el cobro. El historial y el repartidor asignado reciben la instantánea, que no cambia al editar el menú.
- Editar o retirar anuncios valida su versión, evitando sobrescribir cambios de otro admin. Los retiros son archivos lógicos para mantener el historial.

## Verificación

`BusinessDeliveryTest`: presentación clara/oscura, carrusel que conserva posición durante refrescos, menú y cantidades, subtotal, texto grande, edición de productos, pedido con origen fijo y pago al destinatario, limpieza al volver, controles del panel, validación del editor y elección de recojo en mapa. Usa ubicaciones, rutas y solicitudes locales, sin pedidos reales.

`BusinessAdTest`: validación de texto, coordenadas, orden e imagen.

`BusinessMenuTest`: validación de productos, cálculos decimales, cantidades/disponibilidad, JSON e historial compatible con envíos anteriores.

La revisión nativa incluye diez casos de `BusinessDeliveryTest`. Para verificar texto grande en un Dialog, ejecuta `largeTextMenuKeepsQuantityControlsAndContinueAccessible` con `adb -s emulator-5554 shell settings put system font_scale 1.4` y restaura el valor original al terminar; cambiar solo `LocalDensity` fuera del Dialog no cambia la densidad de su ventana.

`supabase/tests/business_delivery_mvp.sql`: permisos, filtro del feed, repartidores de prueba, aislamiento y privacidad, tarifa, versiones, cambios de anuncio, apagado, envíos normales y ciclo completo de entrega/pago. Toda la prueba hace rollback. También se ejecutan `delivery_lifecycle.sql` y `ride_pin_admin_switch.sql` para comprobar compatibilidad.

`supabase/tests/business_menu.sql`: menús inválidos, compatibilidad con editores anteriores, cantidades/IDs inválidos, precios manipulados, instantáneas, menú actualizado, lectura del repartidor y ciclo de entrega con tarifa independiente. Toda la prueba hace rollback.

Los asesores de Supabase conservan avisos previos sobre RPC existentes y descubrimiento del esquema GraphQL; las nuevas RPC públicas no son security definer. El nuevo índice de la relación anuncio/pedido puede figurar inicialmente como [índice sin uso](https://supabase.com/docs/guides/database/database-linter?lint=0005_unused_index) mientras no existan pedidos demo persistidos.
