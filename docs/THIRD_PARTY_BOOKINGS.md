# Servicios para otra persona

Al tocar el buscador se abre la planificación sobre el mapa. Recojo y destino se editan por separado; las sugerencias aparecen antes de las direcciones guardadas y de «Elegir en mapa». La pantalla conserva los modos claro y oscuro y el borde turquesa/índigo.

«Para mí» permite elegir una persona del selector de teléfonos de Android o escribir su nombre y celular peruano. Solo se utiliza el contacto elegido. Referencias: [selector de teléfonos clásico](https://developer.android.com/guide/components/intents-common#PickContactData), [compatibilidad del selector de Android 17](https://developer.android.com/about/versions/17/features/contact-picker).

| Servicio | Contacto elegido | Recojo |
| --- | --- | --- |
| Mototaxi | Persona que viaja | Se elige explícitamente para el contacto |
| Courier en moto | Persona que entrega el paquete | Se elige explícitamente; el destinatario se indica después |
| Pedido de negocio demo | Persona que recibe | Permanece fijado al local por el servidor |

La cuenta solicitante sigue siendo propietaria del servicio: conserva seguimiento, cancelación, historial y calificación del conductor. El GPS posterior no sustituye el recojo confirmado. Si el administrador activa el PIN, la persona que solicita puede compartirlo manualmente con quien viaja o entrega el paquete.

Los contactos del pasajero se guardan en `ride_passengers`, con lectura restringida por RLS al solicitante y al conductor asignado. No aparecen en las ofertas para conductores sin asignar. Los contactos de recojo del courier usan las mismas restricciones de `delivery_details`. Las RPC mantienen las tarifas, límites y ciclo de pago existentes. El interruptor de negocios y la selección de repartidores de prueba mantienen su estado.

Validación: pruebas JVM de teléfonos y contactos distintos; pruebas nativas de planificación, GPS, selector del teléfono, sugerencias y pedidos de negocios; pruebas SQL con rollback de propiedad, privacidad, tarifas, PIN y ciclo del servicio. No se crean pedidos reales en las pruebas.
