# Intu 1.30 (31)

- El buscador muestra Casa y Trabajo únicamente cuando tienen una ubicación guardada. Conserva su orden antes de los demás favoritos y mantiene disponibles Elegir en mapa y Lugares guardados. La lista se actualiza al agregar o quitar una dirección.
- Direcciones guardadas utiliza superficies oscuras coherentes con el tema, textos legibles, iconos con fondos y colores adecuados, contador con contraste y bordes discretos. Se conservan las tarjetas vacías para configurar Casa y Trabajo desde el administrador de direcciones.
- Conserva el pin B de 1.29.

## Compilación y verificación

- Compilación aislada basada en el commit registrado en `build/qa-saved-places-1.30/base-commit.txt`, con los cambios del pin y las correcciones de direcciones. Los cambios paralelos de negocios/reparto que todavía estaban en desarrollo permanecen en el checkout original; no se incluyen en este APK.
- Fuentes y hashes de la compilación en `build/qa-saved-places-1.30/source` y `source-hashes.csv`.
- Tres casos nativos del buscador aprobados en el APK final: lista sin direcciones, agregar/quitar Casa o Trabajo, selección y gestión de guardadas, búsqueda sobre mapas claro/oscuro y regreso al inicio.
- Tres casos de direcciones guardadas aprobados (17.961 s): persistencia y separación por cuenta; contraste medido sobre las tarjetas realmente renderizadas en ambos temas y acciones Agregar/Editar/Quitar/Listo; acciones accesibles en el diseño con texto grande.
- Texto real de Android al 160 %: OK (1 test), 5.522 s. Se verificaron Editar, Agregar otro lugar y Listo; configuración original 1.0 restaurada. El fixture que cambiaba LocalDensity durante la misma prueba dejó de mostrar el diálogo; se separó esa comprobación y se verificó también la configuración real de Android.
- Capturas nativas revisadas en `build/qa-saved-places-1.30/screenshots`. Pruebas realizadas en el emulador con datos aislados, sin enviar pedidos, SMS ni reportes.
- Firma debug habitual (SHA-1 `7a4ec69cae01b4b8a5eeeff832d833e176c0efd0`) y alineación de 16 KB verificadas.
- APK final: 105436895 bytes. SHA-256 `A24988A432E6083E965C943F9BB05360CD43E362F93F3B6751CB92C55806E4A3`.

## Entrega

- Publicada el 5 de octubre de 2026 mediante `scripts/publish-apk.ps1`. La descarga versionada coincide por SHA-256 y la API confirma 1.30 (31).
- Descarga: https://viajaconintu.pages.dev/descargar?versionCode=31.
- Reinstalada en el Samsung SM_F976U mediante `adb install -r` (Success), conservando los datos. `dumpsys package` confirmó 1.30 (31). No se interactuó con la cuenta del teléfono.
- APK y evidencia: `build/qa-saved-places-1.30/intu-1.30.apk`, `publish-results.txt`, `published-api.json` y `phone-install.txt`.
