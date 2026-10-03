# Regresar desde «Elige tu viaje»

Fecha: 2026-10-01.

El botón Atrás del teléfono y la flecha «Regresar» vuelven al inicio, limpian el destino y la ruta preparados, restauran la barra inferior y centran el mapa en la última ubicación recibida. La cámara vuelve a zoom 14, sin inclinación, giro ni padding. El manejo de Atrás se limita a la preparación del viaje, sin cancelar solicitudes enviadas o viajes activos.

HomeScreen ahora conserva cada actualización de ubicación, en lugar de guardar solamente la primera lectura. Su proveedor de ubicación opcional permite verificar este comportamiento con GPS simulado; las llamadas de producción usan el proveedor habitual.

## Verificación

- Compilación debug y APK de pruebas, 35 pruebas unitarias y lint debug: correctos. Lint sin errores; conserva advertencias del proyecto.
- `HomeBackNavigationTest`: 2 pruebas correctas en Samsung SM_F976U con app **1.18 / código 19**. Se usó el APK de pruebas compilado como snapshot 1.17, compatible con el mismo HomeScreen de 1.18.
- Se verificaron Atrás del teléfono, flecha Regresar, centrado a menos de un metro del último GPS simulado, zoom 14, padding inferior cero, barra inferior visible y elección de un nuevo destino desde la ubicación actual.
- Ubicación, ruta y envío de viaje simulados. Cero solicitudes de viaje. Se reabrió MainActivity al terminar, sin cerrar la sesión.

La otra sesión compiló e instaló la app 1.18 incluyendo este cambio. No se reemplazó por el snapshot 1.17 ni se publicó un APK adicional desde esta sesión. El AAB de `build/play/intu-play.aab` sigue siendo el snapshot anterior 1.14 y debe regenerarse para incluir estos cambios antes de subirlo a Play.
