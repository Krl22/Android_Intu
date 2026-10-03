# Intu 1.21 QA — Atrás y mapa de recojo

Versión 1.21, código 22. Incluye la barra flotante de 1.20 y las mejoras acumuladas de 1.19.

## Comportamiento

- Atrás y la flecha de selección vuelven a Inicio desde el marcador de destino, el marcador de recojo y Elige tu viaje. Se descarta la preparación, se limpia destino/ruta/recojo/búsquedas y se centra la última ubicación recibida con zoom 14 y sin padding de cámara.
- Atrás también limpia la búsqueda inicial y permite descartar el cálculo de una ruta pendiente. Un cálculo que termine después no puede reabrir el borrador descartado.
- Desde las pestañas principales Cuenta y Viajes, Atrás vuelve a Inicio conservando la sesión. Las pantallas internas y diálogos mantienen su navegación propia.
- El manejo de Atrás de Home se limita a la preparación; no cancela solicitudes enviadas ni viajes activos. Al enviar una solicitud, la acción de regreso queda deshabilitada hasta conocer el resultado.
- Tras Confirmar viaje, el recojo se elige en el mismo MapView a pantalla completa que el destino: pin centrado, búsqueda de catálogo/direcciones y botón Solicitar viaje. La ruta y la tarifa se recalculan desde el recojo definitivo sin cambiar el destino confirmado.
- El mapa empieza en la ubicación del pasajero o su último recojo elegido, sin el padding de la tarjeta de opciones. Conserva el logo de Mapbox y la configuración global de zoom centrado/sin escala.

## Verificación y distribución

- Compilación debug y del APK de pruebas, 38 pruebas unitarias JVM y lint debug aprobados (0 errores, 72 avisos, 10 sugerencias). Firma debug habitual y alineación de 16 KB comprobadas.
- Instalación `adb install -r` correcta en el Samsung, conservando los datos. Versión instalada 1.21 / código 22; SHA-256 del APK instalado coincide con el snapshot.
- Hay nueve pruebas nativas preparadas: siete casos de regreso/descartado de borrador, un flujo de recojo a pantalla completa con zoom/paneo y recálculo, y un caso de Cuenta/Viajes hacia Inicio. Todavía no se ejecutaron: la pregunta de disponibilidad actual del celular sigue pendiente. La conexión ADB no se considera confirmación para usar su interfaz.
- No se hicieron cambios ni despliegues de backend, ni se enviaron solicitudes de viaje.
- Snapshot: `build/qa-home-1.21/intu-1.21.apk` y APK de pruebas en el mismo directorio.
- Tamaño: 105,206,420 bytes.
- SHA-256: `A1D5F3BA54EE3A22C3C4F963EF44728722D7E210106B4760497A926D9F652CAD`.
- Publicado en R2 el 2 de octubre de 2026 a las 02:17:49, hora de Nueva York (`2026-10-02T06:17:49.103Z`). [Descargar Intu 1.21](https://viajaconintu.pages.dev/descargar?v=1.21-qa-22).
- La descarga completa coincide con el APK instalado en tamaño y SHA-256. El binario incluye las fuentes finales de BottomBar 1.20 y Home/MainActivity 1.21; no incluye cambios posteriores de la configuración PIN de la otra sesión.
