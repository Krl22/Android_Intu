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
