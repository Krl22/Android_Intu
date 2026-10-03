# Intu 1.22 — Elige tu viaje y ruta discreta

Versión 1.22 / código 23, sobre 1.21.

- Panel blanco compacto: opción horizontal Mototaxi con ilustración, tarifa, duración del recorrido y kilómetros. Se elimina el carrusel de una sola opción, el icono sobredimensionado y los bordes brillantes.
- Pago con valor y acción Cambiar; botón Confirmar viaje siempre accesible. El contenido puede desplazarse en pantallas cortas o con texto grande.
- Ruta del pasajero turquesa de 5 dp, borde claro de 8 dp; sin halo pulsante, núcleo blanco ni destellos. El conductor comparte el mismo turquesa y ancho nominal 5. Su ruta ya era turquesa en 1.21; los rojos del radar y los marcadores no son rutas calculadas.
- Conserva cálculo de tarifa, persistencia del pago, Confirmar viaje → elegir recojo → Solicitar viaje y regreso al GPS más reciente con Atrás.

## Verificación

- APK debug y APK de pruebas compilados. 40 pruebas JVM aprobadas; lint: 0 errores, 72 avisos y 10 sugerencias. Alineación de 16 KB comprobada.
- Emulador Android 37: dos pruebas de panel (selección/pago/confirmación y texto 2× en pantalla corta) y siete casos de Atrás aprobados. HomeRideFlow pasó en la repetición final, con recojo a pantalla completa, zoom/paneo y recálculo simulado. Su primer intento falló en desplazamiento durante el arranque del emulador; se conserva el registro, sin reducir el umbral ni modificar el gesto.
- Samsung: instalación `adb install -r` correcta, versión 1.22 / código 23 confirmada. Datos conservados. Carlos confirmó disponibilidad, pero Android reportó pantalla apagada y keyguard activo. Se detuvo la instrumentación propia; luego desapareció la conexión ADB. Revisión física pendiente de desbloqueo/reconexión, no contabilizada como resultado de la app.
- No se enviaron viajes reales ni se cerró la sesión. No hay cambios de backend propios de esta entrega.

## Snapshot

- Compilado desde `build/qa-ui-1.22`, copia aislada de fuentes para evitar interferencias con la integración courier de la sesión QA.
- Solo en esa copia se oculta la opción nueva Moto lineal del formulario; la integración de reparto sigue en otra entrega. Se incluyen dos correcciones compatibles de esa sesión: código canónico de vehículo y campo de vehículo del modelo admin.
- APK: `build/qa-ui-1.22/intu-1.22.apk`, 106165588 bytes.
- SHA-256: `7528D1CCB42FE29EE0F34799C806C3D8A7F0C619CD160D50C3C33F833E530B75`.
- Registros nativos: `native-results.txt`, `native-final-results.txt`, `native-phone-results.txt` en el snapshot. Las capturas iniciales pueden contener el frame anterior y no se usan como evidencia visual.
- La sesión de QA preserva este diseño en su siguiente entrega 1.23. No se publicó un binario nuevo en la web desde este chat.
