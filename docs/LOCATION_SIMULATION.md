# Simulación de ubicación y recorrido

La barra «Pruebas de ubicación» permite simular sin salir de la búsqueda ni del viaje. **Simular** y **Cambiar** abren directamente un pin sobre el mapa de fondo de pasajero o conductor. La ruta, el punto de recojo/destino y las ubicaciones permanecen en ese mismo mapa. Mueve el mapa y pulsa **Simular aquí**, o **Cancelar** para conservar la ubicación anterior. En otras pantallas se abre el selector independiente.

Por defecto está disponible **solo para administradores**, tanto en **debug** como en **release**.
La barra empieza **oculta para cada administrador**. En **Admin → Seguridad → Simulación de ubicación**,
**Mostrar barra de simulación en mi cuenta** permite mostrarla cuando quieras hacer pruebas.
Esta preferencia se guarda por cuenta y se sincroniza entre sus dispositivos; no cambia la preferencia
de otros administradores ni el permiso de pasajeros y conductores. Al ocultarla, se cancela cualquier
simulación activa y se vuelve al GPS real. El guardado en este dispositivo actualiza la barra de inmediato;
los otros dispositivos la sincronizan al regresar al primer plano o cada 15 segundos.
En **Admin → Seguridad → Simulación de ubicación**, el interruptor **Permitir simulación a usuarios**
permite habilitarla para todos los pasajeros y conductores con sesión. Empieza apagado.
El RPC `location_simulation_access` comprueba el rol actual y el permiso global; solamente un admin
puede cambiarlo mediante `admin_set_location_simulation`. No concede acceso al panel administrativo.
El RPC `admin_set_simulation_bar` guarda únicamente la preferencia del administrador que llama;
la tabla privada no admite lecturas ni escrituras directas. Al retirar el rol de admin se borra su preferencia.
La app vuelve a consultar el permiso al regresar al primer plano y cada 15 segundos; la simulación activa
también lo comprueba en segundo plano. Al retirar el permiso se detiene el recorrido y se vuelve al GPS real.
Un fallo al verificar el permiso deja la simulación deshabilitada. Los controles de una versión anterior
no cambian hasta instalar el APK actualizado.

## Prueba con conductor y pasajero

Usa dos dispositivos o emuladores y dos cuentas distintas. El conductor debe estar aprobado, tener un vehículo compatible y conceder permiso de ubicación para que su servicio siga publicando en segundo plano. Las solicitudes y los cambios de estado utilizan el backend habitual.
Activa **Mostrar barra de simulación en mi cuenta** para cada cuenta admin que participe.
Si participa una cuenta de usuario común, activa también **Permitir simulación a usuarios**.

1. En el dispositivo del conductor, pulsa **Simular** en la barra y elige un punto en el mapa cercano al recojo. Conéctate para buscar clientes.
2. En el dispositivo del pasajero, elige el recojo y el destino y solicita el viaje. Si necesitas una posición ficticia del pasajero, usa su propia barra de simulación.
3. Acepta la solicitud como conductor. Pulsa **Avanzar al recojo**, elige la velocidad y pulsa **Simular viaje automático**. La posición avanza cada dos segundos por la ruta visible; el pasajero recibe las actualizaciones normales del conductor.
4. Al llegar al recojo, pulsa el botón habitual de llegada. Inicia el viaje y verifica el PIN si corresponde.
5. Al pasar a «viaje iniciado», el modo automático continúa hacia el destino con la misma velocidad. Al llegar, finaliza el viaje normalmente.

También puedes usar **Avanzar al recojo** o **Avanzar al destino** dentro del diálogo para reproducir solo una etapa. **Cambiar** permite mover manualmente tu propia ubicación en cualquier momento; esto desactiva el modo automático. Durante la selección, se pausa el seguimiento de cámara y los selectores de recojo/destino no cambian sus puntos. **Pausar simulación** detiene el movimiento conservando el último punto; activa nuevamente el recorrido para continuar. **GPS real** elimina la ubicación ficticia.

Cada etapa se detiene al llegar al último punto. El modo automático espera la confirmación de llegada y el inicio/PIN antes de reproducir la etapa de destino. Se desactiva al cambiar de viaje, cancelar/finalizar, salir de la pantalla del conductor, cerrar sesión, perder autorización o elegir otra ubicación. No inicia ni completa viajes automáticamente. No modifica el GPS de otras apps y no se guarda al reiniciar el proceso. Cada dispositivo controla su propia cuenta: el administrador no mueve remotamente al otro participante.

Los botones de recorrido se habilitan al tener una ubicación de prueba y un recojo/destino del viaje; no esperan la geometría dibujada en el mapa. Al iniciar cada etapa se solicita una ruta nueva desde la ubicación simulada actual. Sus extremos se conectan a los pines exactos, aunque Mapbox los ajuste a una calle cercana. Si Mapbox no entrega una ruta, la simulación utiliza una línea recta entre esos puntos. Durante la preparación se muestra un estado de carga; pausar, cambiar ubicación o cambiar de etapa invalida el inicio pendiente.
