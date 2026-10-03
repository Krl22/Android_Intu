# Intu 1.24 — panel deslizable y pin de ruta

Versión 1.24 / código 25. Integra los cambios terminados por Claude.

Publicada el 2 de octubre de 2026 a las 13:11 (America/New_York), 17:11:08 UTC.

## Comportamiento

- Elige tu moto se desliza entre dos alturas. El panel nunca se oculta completamente: contraído conserva la moto elegida, su tarifa y Elegir recojo. La barra superior también permite cambiar de altura con un toque.
- La lista y el método de pago siguen disponibles al expandir. Se conserva la selección al contraer, expandir y continuar; con texto grande el contenido se desplaza y el botón continúa accesible.
- La cámara encuadra nuevamente la ruta cuando el panel termina de cambiar de altura. Se reserva el espacio del panel mediante CameraOptions.padding: coordinatesPadding por sí solo no desplaza el centro visible del mapa. Se limita el acercamiento automático a zoom 16 para recorridos muy cortos. El área descubierta del mapa recibe gestos de desplazamiento.
- En el mapa principal, el pin de destino usa un contorno violeta, halo blanco y centro abierto sobre la ruta turquesa; el recojo usa ámbar. El zoom alrededor del pin y Atrás → inicio se conservan.
- Se conservaron las ilustraciones de Claude y el recargo de Honda de 12%. La tarifa contraída usa el mismo cálculo que la lista, la reserva y el servidor.
- Se corrigió un uso inválido de padding que impedía compilar el panel y se reinicia el desplazamiento de su contenido al contraerlo.

## Verificación

- 46 pruebas JVM: 0 errores / 0 fallos.
- Compilación de app/APK de pruebas y lint PASS. Lint: 0 errores, 75 advertencias y 10 sugerencias.
- 3 pruebas nativas del drawer: arrastre en ambos sentidos, límite inferior, conservación de Honda/pago, confirmación, texto al 200% y contraste/transparencia del pin.
- 19 pruebas nativas distintas verificadas sobre el APK final: drawer (3), opciones (2), recorrido/recojo (1), envíos (3), regreso a inicio (7) y registro de conductor (3). La prueba de recorrido confirma que el destino queda por encima del panel expandido y contraído, que eventos táctiles a través de la ventana mueven el mapa y que el recojo conserva el zoom centrado y elimina el padding del panel.
- El emulador API 37 mostró un diálogo de ANR de System UI que impedía tomar foco a ocho pruebas. Las otras once pasaron; después de cerrar ese diálogo, las ocho afectadas se repitieron y pasaron (OK 8 tests, 63.197 s). Se conservaron ambos registros. Las escalas de animaciones de ventanas/transiciones, desactivadas temporalmente en el emulador para esa repetición, se restauraron a 1 al terminar.
- Supabase: prueba transaccional mototaxi_brand_selection.sql PASS, con rollback de todos los datos ficticios. Confirma tarifa Honda, filtros Honda/Bajaj y aceptación de cualquier marca.
- Migración de Claude 20261002161822_honda_luggage_premium confirmada en el proyecto vkguzpciwpfvaeyedepl. Ejemplo: S/ 5.30 base → S/ 5.90 Honda, igual a la app.
- Firma debug verificada; certificado SHA-1 7a4ec69cae01b4b8a5eeeff832d833e176c0efd0 y SHA-256 7ee48b402d20042fcfa7b0db98b91fcae410d12efe7d9f9f3dd8f4be9968fa0d. Alineación de 16 KB verificada.
- Las pruebas de pantalla usan GPS, rutas y solicitudes simuladas en Phone_1 (emulador). No envían SMS ni pedidos reales.

## Entrega

- Reinstalado el APK final en Samsung SM_F976U mediante adb install -r, conservando los datos y la sesión. Confirmada versión 1.24/código 25 mediante el administrador de paquetes; las pruebas interactivas se hicieron en el emulador.
- APK actualizado en R2 intu-apk/intu.apk y disponible en https://viajaconintu.pages.dev/descargar?v=1.24-25-drawer-final.
- Se descargó el archivo completo desde la web y se comparó su SHA-256 con el APK validado: 6489B8EDE9A9AF9802140FDD9E43D55C2CAB363DE1BBB114028C0109A0E240DC. Tamaño 105289439 bytes; metadata de publicación 2026-10-02T17:11:08 UTC.
- Comprobados los hashes de los archivos de app/src/main después de compilar y antes de entregar: sin cambios posteriores al build.

Evidencia y APK: build/qa-drawer-1.24 (build-final.txt, native-final-results.txt, native-home-back-final.txt, unit-summary.json, lint-summary.json, capturas expandida/contraída y web-verification.json).
