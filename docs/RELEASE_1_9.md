# Intu 1.9 QA — zoom centrado en el pin

Versión 1.9, código 10. APK debug con la firma habitual de QA.

Durante la elección de destino o punto de recojo, los gestos de zoom y rotación usan como referencia el centro del mapa, donde se coloca la punta del pin. El zoom deja de pivotar alrededor del punto donde se ponen los dedos. Se desactiva el desplazamiento simultáneo con dos dedos durante la selección; el arrastre con un dedo sigue permitiendo elegir otra ubicación.

El punto seleccionado se obtiene del píxel fijo del pin cuando cambia la cámara, incluidos el zoom, la inercia y los cambios de tamaño. La dirección se resuelve después de detenerse el movimiento; pequeños cambios de precisión durante el zoom no provocan nuevas consultas. Al salir de la selección se restauran los gestos anteriores y se retiran los listeners.

## Validación

- Las 26 pruebas unitarias pasaron.
- Firma APK y alineación ZIP de 16 KB verificadas.
- La compilación final del APK y del APK de pruebas pasó. Lint terminó sin errores (70 avisos y 10 sugerencias en el proyecto).
- La prueba Android pasó en el emulador API 37 con páginas de memoria de 16 KB: un pinch fuera del centro mantiene el punto bajo el pin con una tolerancia de un píxel; el arrastre con un dedo cambia la selección; el punto focal se adapta al tamaño del mapa y al salir se restauran los gestos y se cancela la suscripción a la cámara. Se usó un estilo local sin descargar mapas ni enviar solicitudes al backend.

## Artefacto

- Archivo: `app/build/outputs/apk/debug/app-debug.apk`.
- Tamaño: 105,009,816 bytes.
- SHA-256: `2C9A5A58687DA01688CFAE83019FD1A04CC396DB4A5DF0B70C014C193B710CEC`.
- Reinstalado en el Samsung conectado mediante ADB inalámbrico, conservando los datos. Se verificó versión 1.9 / código 10 y se solicitó abrir `MainActivity`; el celular estaba bloqueado en ese momento.
- Publicado en R2 el 2026-10-01 a las 14:05:41 UTC. La descarga completa desde [Cloudflare Pages](https://viajaconintu.pages.dev/descargar?v=1.9-qa-10) coincide en tamaño y SHA-256 y confirma versión 1.9 / código 10.
- Evidencia de la prueba Android y descarga verificada: `%LOCALAPPDATA%/Temp/intu-qa-1.9/`.
- La descarga verificada de Intu 1.8 permanece guardada localmente para recuperación.
