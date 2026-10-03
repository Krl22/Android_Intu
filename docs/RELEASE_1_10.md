# Intu 1.10 QA — mapa de los detalles del viaje

Versión 1.10, código 11. APK debug con la firma habitual de QA.

Se quitó la barra de escala de distancia del mapa que aparece al abrir un viaje. El logo de Mapbox se colocó en la esquina inferior derecha y el botón de atribución en la inferior izquierda, con márgenes de 8 dp. Ambos permanecen visibles y conservan su aspecto original, conforme a las [condiciones del Maps SDK de Mapbox](https://docs.mapbox.com/android/maps/guides/) y su [guía de atribución](https://docs.mapbox.com/help/dive-deeper/attribution/).

El ajuste corresponde al mapa de los detalles del historial. Se conserva el dibujo de la ruta guardada y los puntos de recojo y destino.

## Validación y artefacto

- Firma APK y alineación ZIP de 16 KB verificadas.
- Reinstalado en el Samsung por ADB inalámbrico, conservando los datos. Se verificó versión 1.10 / código 11 y se solicitó abrir `MainActivity`; el celular estaba bloqueado en ese momento.
- Archivo: `app/build/outputs/apk/debug/app-debug.apk`.
- Tamaño: 105,009,812 bytes.
- SHA-256: `87B6CE83D93601964AB0B3BD8FF643F110EEA2DCAB01402F0C52465DA39BE7E0`.
- La compilación del APK y del APK de pruebas pasó. Lint terminó sin errores (72 avisos y 10 sugerencias en el proyecto).
- La prueba existente `storedRouteOpensAMapInTripDetails` pasó con Intu 1.10 / código 11 en el emulador API 37 con páginas de memoria de 16 KB. Se revisó la captura: la escala no aparece, el logo queda abajo a la derecha, el botón de atribución abajo a la izquierda y la ruta y ambos puntos siguen visibles.
- Evidencia de la prueba y captura: `%LOCALAPPDATA%/Temp/intu-qa-1.10/`.
- Publicado en R2 el 2026-10-01 a las 14:27:01 UTC. La descarga completa desde [Cloudflare Pages](https://viajaconintu.pages.dev/descargar?v=1.10-qa-11) coincide en tamaño y SHA-256 y confirma versión 1.10 / código 11.
- La descarga verificada de Intu 1.9 permanece guardada localmente para recuperación.
