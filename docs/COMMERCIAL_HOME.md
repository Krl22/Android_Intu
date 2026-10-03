# Prototipo de inicio comercial

Rama: `codex/inicio-comercial`. Base anterior guardada en `dc31ded` (Intu 1.28).

## Concepto implementado: Intu te conecta

El inicio presenta la marca con el título `intu`, el saludo de la cuenta y la
búsqueda `¿A dónde vamos?`. El saludo, la presentación y la búsqueda forman un
solo bloque sobre un fondo de marca que se desvanece hacia el resto de la página.
Casa, Trabajo y los favoritos se muestran como filas en **Tus lugares**, debajo
de los servicios, con un acceso a Gestionar. **Elegir en mapa** es un enlace junto
a la búsqueda. Se retiran los tres botones cuadrados de la cabecera anterior.
La búsqueda no muestra micrófono; permite limpiar el texto.

La portada incluye una presentación de Intu, accesos a Viajar y Enviar y una
tarjeta de próximos negocios locales. Enviar preselecciona moto para envíos;
Viajar preselecciona cualquier mototaxi. Ambos llevan a elegir el destino y
conservan los pasos de confirmación existentes.

La portada respeta la preferencia de apariencia existente y se adapta a modo
oscuro y claro. Su contenido se puede desplazar y deja espacio para la barra
inferior y el teclado. Al buscar, se oculta la presentación para dar espacio a
los resultados. Atrás descarta la preparación y vuelve a la portada.

El mapa es invisible y no recibe gestos en el inicio. Se muestra al elegir un
destino, preparar la ruta, solicitar un servicio o seguir un viaje existente.
El MapView sigue inicializado para conservar el proveedor de ubicación y el
comportamiento de los viajes; este cambio no elimina el consumo de Mapbox/GPS.

La sección de negocios es solo una presentación de futuras posibilidades.
No hay anuncios reales, cobros, métricas, campañas ni integración publicitaria.

## Alternativas visuales para evaluar

- **Intu te conecta:** presentación de marca dominante, servicios en dos tarjetas
  y espacio comercial debajo. Es la versión inicial de esta rama.
- **Servicios primero:** dos accesos en filas, mensaje de marca compacto y un
  anuncio horizontal. Da prioridad a pedir un servicio rápidamente.
- **Descubre tu ciudad:** portada editorial de ciudad, llamada principal a viajar
  y negocios destacados entre los servicios. Da más protagonismo al comercio.

Los ejemplos de publicidad en las alternativas son ficticios y están rotulados
como conceptos. Las alternativas no cambian la app hasta que se implementen.

## Comprobaciones de interfaz

`CommercialHomeTest` cubre la portada oscura, paso a mapa y regreso, selección de
envío/viaje y búsqueda en modo claro. Usa ubicación y rutas de prueba, sin enviar
solicitudes de viaje. `HomeBackNavigationTest` comprueba que Atrás conserva la
ubicación más reciente y no reabre rutas descartadas.

Validación de la versión inicial: compilación debug y APK de pruebas completados. Las tres pruebas de
`CommercialHomeTest` pasaron en el emulador `Phone_1` el 3 de octubre de 2026.
Se inspeccionaron capturas de la portada en ambos modos de apariencia.
También pasaron las siete pruebas de `HomeBackNavigationTest` (10 pruebas de
interfaz en total). El APK debug fue actualizado en el celular conectado con
`adb install -r`, conservando los datos; las pruebas se ejecutaron en el emulador.

## Revisión: portada integrada

Se unificó la cabecera y la presentación con un degradado de la paleta del tema,
sin el bloque índigo ni la tarjeta de presentación separada. La búsqueda usa los
mismos márgenes de contenido, esquinas de 20 dp y ninguna sombra. El mapa se
elige desde un enlace junto a la búsqueda. Los destinos guardados se muestran en
filas debajo de Viajar y Enviar, conservando la selección directa de destino y
la gestión de direcciones.

La revisión compiló y pasó seis pruebas en `Phone_1`: las cuatro de
`CommercialHomeTest`, el regreso desde preparación con GPS actualizado y el
flujo completo de envío con solicitud simulada. Se verificó la nueva portada
oscura en una captura nativa. Las pruebas anteriores que usaban el botón
Marcador ahora usan el acceso `home-pick-destination`.

## Revisión: Viajes y Cuenta con el estilo del Inicio

Las tres pestañas comparten el degradado suave de la paleta mediante
`intuPageBackground`. Viajes y Cuenta usan la misma cabecera de marca, márgenes
de 20 dp y tarjetas de 24 dp, con superficies sólidas y acentos del tema.
Se retiran el fondo índigo intenso de Viajes y la cabecera de cristal de Cuenta.
El perfil queda en una tarjeta compacta con foto editable, nombre, teléfono y
modo conductor cuando la cuenta está autorizada.

Modo oscuro se integra como una fila en la misma tarjeta de ajustes que pagos
y direcciones guardadas, manteniendo su preferencia por cuenta y el cambio
inmediato de apariencia. El historial conserva detalles, calificaciones,
estados de viajes/envíos, actualizaciones y ganancias del conductor.

Validación: compilación debug y APK de pruebas correctas; cinco pruebas de
interfaz aprobadas en `Phone_1`: historial para pasajero/conductor en ambos
temas, estados vacío/error y reintento, interruptor integrado de Cuenta y las
portadas oscura/clara con regreso desde mapa/búsqueda. Se revisaron capturas
nativas de Cuenta y Viajes en ambos temas y ganancias en modo oscuro. Los
datos del historial usados en estas capturas son fixtures locales; las pruebas
no califican ni crean viajes reales.

## Revisión: recuperar el turquesa e índigo originales

Se recuperan `#08817E` y `#1E1F47` como los dos colores del degradado compartido.
Los fondos y las tarjetas de Inicio, Viajes y Cuenta usan variaciones de esa
misma combinación: profundas en modo oscuro y claras en modo claro. Viajar,
Enviar, el perfil y las ganancias tienen un degradado más marcado. Las tarjetas
de lugares guardados conservan el degradado detrás de cada fila.

La paleta del tema reemplaza las superficies grises oscuras por índigo y
azul/turquesa, y los acentos verdes y las superficies lavanda predeterminadas
del modo claro por la identidad original. Se conserva la distribución nueva,
las funciones y el interruptor integrado de Cuenta.

Compilación debug y APK de pruebas correctas. Cuatro pruebas de interfaz
aprobadas: historial y ganancias en ambos temas, interruptor integrado de Cuenta,
Inicio oscuro con regreso desde el mapa e Inicio claro con búsqueda y regreso.
La última prueba cierra primero el teclado nativo antes de enviar Atrás a la
pantalla, y espera que terminen sus cambios de visibilidad. Se revisaron capturas
nativas de la nueva combinación y se reinstaló el APK en el Samsung con `-r`,
conservando los datos.
