# Intu 1.29 (30)

- Pin B elegido por el usuario: círculo elevado, borde blanco, interior grafito y centro turquesa. El recojo utiliza un centro ámbar para distinguirlo del destino.
- Se utiliza al elegir recojo/destino, en el destino confirmado sobre la ruta, y en los mapas de catálogo, lugares guardados y simulación administrativa.
- El centro del pequeño punto blanco inferior coincide con la coordenada elegida. La base fina deja ver la ruta a sus lados; el cambio de estilo del mapa conserva posición y selección.

## Verificación

- APK debug y APK de instrumentación compilados correctamente.
- Cinco casos nativos aprobados en la primera ejecución: contraste del pin sobre mapas claro/oscuro, drawer (dos casos), zoom del catálogo y selección/cancelación de simulación administrativa.
- El caso del mapa principal usaba una entrada obsoleta: esperaba “Elegir en mapa” antes de abrir el buscador del nuevo inicio. Se actualizó solo la navegación de la prueba; aprobó después en 20.262 s. Verifica una ruta no degenerada, la opción Honda, la posición del pin, la cámara y el cambio de tema.
- Seis casos aprobados en total. Capturas nativas revisadas en `build/qa-pin-b-1.29/screenshots`; ruta simulada, sin pedidos ni SMS reales. Pruebas realizadas en el emulador, no en la cuenta del teléfono físico.
- Certificado debug habitual comprobado (SHA-1 `7a4ec69cae01b4b8a5eeeff832d833e176c0efd0`) y alineación de 16 KB verificada.
- APK: 106587094 bytes; SHA-256 `70E85927BC40AE0BB6A7631F108A7E581B258CB90ECD0846783DCC999E12316C`.

## Entrega

- Publicada el 5 de octubre de 2026 mediante `scripts/publish-apk.ps1`; la descarga versionada fue comprobada por SHA-256 antes de anunciar la versión y la API confirmó 1.29 (30).
- Descarga: https://viajaconintu.pages.dev/descargar?versionCode=30.
- Reinstalada en el Samsung SM_F976U mediante `adb install -r` (Success), conservando los datos; `dumpsys package` confirmó 1.29 (30). No se interactuó con su cuenta.
- Evidencia: `build/qa-pin-b-1.29/publish-results.txt`, `published-api.json` y `phone-install.txt`.
