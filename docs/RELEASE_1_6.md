# Intu 1.6 QA — botones del acceso con Google

Versión 1.6, código 7. APK debug con la misma firma de QA de las versiones anteriores.

La tarjeta de conexión colocaba Cancelar y Reintentar en una fila estrecha, haciendo que sus nombres se partieran dentro de una palabra. Ahora los botones ocupan todo el ancho y se apilan: Reintentar como acción principal, con icono de actualización, y Cancelar debajo con borde. Ambos labels usan una sola línea y una altura mínima de 52 dp que puede crecer con el tamaño del texto.

El padding interior de la tarjeta pasa de 32 a 24 dp para dar más espacio al contenido. La pantalla permite desplazamiento vertical cuando la altura disponible o el texto ampliado lo requieren.

## Validación

- `:app:assembleDebug` completado correctamente.
- Vista previa nativa de los botones con ancho de 220 dp y fuente al 200%: ambos nombres completos y sin cortes. Captura y jerarquía de accesibilidad revisadas en el emulador; las vistas previas también están disponibles en Android Studio.
- Firma y alineación ZIP de 16 KB verificadas.
- APK instalado por ADB inalámbrico en el Samsung conectado, conservando los datos de la app.

## Artefacto

- Archivo: `app/build/outputs/apk/debug/app-debug.apk`.
- Tamaño: 105,009,816 bytes.
- SHA-256: `13BFD35683CBAAE0D264D532C23D7EF664BF7693E15DD9390DDDF4FA70816B04`.
- Publicado en R2 el 2026-10-01 a las 06:14:07 UTC. La descarga completa desde [Cloudflare Pages](https://viajaconintu.pages.dev/descargar?v=1.6-qa-7) coincide en tamaño y SHA-256 y confirma versión 1.6 / código 7.
- Se conserva localmente la descarga verificada de Intu 1.5 para recuperación.
