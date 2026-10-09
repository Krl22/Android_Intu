# Intu 1.34

Versión 1.34, código 35.

- Reportes privados sobre el conductor o cliente, ligados al servicio, con atención en Admin → Reportes.
- Objetos perdidos: aviso para revisar el vehículo, respuesta del conductor, coordinación y confirmación de devolución por el pasajero.
- Incluye tarifas configurables (base S/ 1.50 y mínimo S/ 3.00), precios propuestos por el conductor con aceptación del pasajero y controles admin para mostrar la simulación de ubicación.

APK de web/QA: `build/qa-service-reports-1.34/intu-1.34.apk`, conserva firma de QA. AAB de Play: `build/play/intu-play.aab`, usa la clave de subida fuera del repositorio y la firma final de Google Play.

Pruebas JVM: 100 aprobadas. Pruebas nativas de reportes: tres aprobadas. Verificación SQL de permisos, flujo y límites de uso: aprobada. Compilación debug/release y lint release correctos. AAB con firma JAR verificada y cuatro bibliotecas ARM64 con segmentos ELF alineados a 16 KB. La huella SHA-256 de la clave de subida coincide con Play Console: `15:97:36:AD:D5:27:8C:5B:63:E1:11:D2:10:17:75:E9:BB:4D:83:B3:59:F5:19:EF:87:6A:76:B6:F1:E9:ED:B9`.

AAB SHA-256: `88B91783AA875450BEDA310EF401167FBFA16A373B0D43A94F300016C8A25F65`.

APK QA/web: 59,847,375 bytes, SHA-256 `8D7C2F7F2709B71E7334D9B955CABC74EBF57A6E2944446C87930512CFCA2207`. Lint release: cero errores, 80 advertencias, 12 sugerencias.

Google Play aceptó el AAB 35/1.34. La revisión muestra cuatro avisos sin errores: nueve dispositivos dejan de ser compatibles (esta entrega ARM64/ARM32 no incluye x86), aumento de tamaño, ausencia de archivo de desofuscación (R8 no está activado) y símbolos nativos sin subir. No impiden la prueba interna.

La cuenta de Play conectada no tiene permiso para listar apps en Firebase `intu-e8403`. Falta revisar la firma final de Play en Firebase y comprobar Google/SMS en una instalación desde Play; la firma de subida sí está comprobada. La publicación web continúa independiente.

Publicado el 8 de octubre de 2026 en Prueba interna. Console confirma **Available to internal testers**, versión 35/1.34. Enlace de inscripción: https://play.google.com/apps/internaltest/4701402864699537874. Se conservaron las listas seleccionadas “Intu - usuarios existentes” y “MyTesters”. Google advierte que la propagación puede tardar una hora o más. Captura de confirmación: `build/qa-service-reports-1.34/play-internal-1.34.jpg`.
