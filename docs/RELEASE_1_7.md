# Intu 1.7 QA — número local en Cuenta

Versión 1.7, código 8. APK debug con la firma habitual de QA.

El encabezado de Cuenta muestra el número local sin el código de país: `+51 987654321` pasa a `987654321`. Se aplica a pasajeros y conductores. El formateador reconoce los prefijos del selector de países de Intu y los números peruanos antiguos guardados sin el signo `+`. El número original permanece en formato internacional para autenticación y contactos.

Cuando el número del perfil está vacío se usa el de Firebase; si no existe ninguno, se muestra «Sin número».

## Validación

- `:app:assembleDebug` y las tres pruebas existentes de `PhoneFormatTest` pasaron.
- Comprobación directa del formateador compilado con números de ejemplo de Perú, Estados Unidos y Ecuador, número local, formato peruano antiguo y valor vacío.
- Firma APK y alineación ZIP de 16 KB verificadas.
- Instalado en el Samsung conectado mediante ADB inalámbrico, conservando los datos de la app.

## Artefacto

- Archivo: `app/build/outputs/apk/debug/app-debug.apk`.
- Tamaño: 105,009,816 bytes.
- SHA-256: `A585E4B72FC8D33521B9A175775EDA987B0CADC8CA26A5699AF34013D4783EE4`.
- Publicado en R2 el 2026-10-01 a las 06:23:04 UTC. La descarga completa desde [Cloudflare Pages](https://viajaconintu.pages.dev/descargar?v=1.7-qa-8) coincide en tamaño y SHA-256 y confirma versión 1.7 / código 8.
- La descarga verificada de Intu 1.6 permanece guardada localmente para recuperación.
