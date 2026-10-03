# Intu 1.5 QA — búsqueda de catálogo y direcciones

Versión 1.5, código 6. APK debug con la misma firma de QA registrada en Firebase.

## Cambio

La versión 1.4 reemplazó las sugerencias de Mapbox por el catálogo y dejó de mostrar las calles anteriores. Inicio y Dirección del marcador ahora combinan ambos: los lugares publicados y verificados de Intu aparecen primero; las calles, direcciones y localidades de Mapbox Geocoding v5 aparecen debajo. Cada resultado conserva sus coordenadas y se puede elegir en el mapa.

El catálogo se busca localmente y aparece de inmediato. Mapbox se consulta desde dos caracteres, tras 600 ms sin cambios y únicamente para el campo activo. Cambiar el texto, dejar el campo o elegir un resultado cancela la petición anterior. No se persisten sus sugerencias en el catálogo. Los duplicados cercanos conservan el recojo verificado de Intu y un error externo permite seguir usando el catálogo o elegir en el mapa.

Mapbox conserva el área de 15 millas del buscador anterior; sin ubicación se usa Satipo en lugar de la antigua referencia a Boston. Esa restricción no se aplica al catálogo. Se eliminó el tipo `poi` de la geocodificación, donde Mapbox ya no lo admite. Las consultas externas pueden consumir la cuota del servicio; la pausa reduce peticiones mientras se escribe.

## Validación

- Compilación de app y APK de pruebas, 25 pruebas JVM y `lintDebug` completados. Lint: 0 errores, 70 advertencias y 10 recomendaciones.
- Seis pruebas Android distintas pasaron: selección y atribución del catálogo, orden híbrido y coordenadas de la calle, cancelación/debounce y campos desactivados, fallo externo conservando el catálogo, elección manual/actualización y búsqueda desde Inicio.
- Cuatro pasaron en el Samsung conectado; su bloqueo de pantalla interrumpió dos. Las seis quedaron verificadas en el emulador tras repetir la primera, afectada por el arranque y el reinicio de System UI. No se cambió la configuración de bloqueo del teléfono.
- Las consultas reales de Mapbox devolvieron Jirón Manuel Prado y Jirón Julio C. Tello en Satipo. Los casos automatizados de mezcla usan respuestas controladas y no dependen de la cobertura externa.
- La prueba de Inicio usa un catálogo QA temporal y restaura su preferencia. No se publicaron lugares de prueba ni se modificó el backend.
- Firma APK y alineación ZIP de 16 KB verificadas. Instalado en el Samsung mediante ADB inalámbrico sin borrar datos; eliminado el paquete de pruebas del teléfono.

## Artefacto

- Archivo: `app/build/outputs/apk/debug/app-debug.apk`.
- Tamaño: 105,098,179 bytes.
- SHA-256: `ED1578F19E50AAFC417BE8172AFE319838F9E873173C8FD02891F266E471A7D6`.
- Publicado en R2 el 2026-10-01 a las 06:01:50 UTC. La descarga completa desde [Cloudflare Pages](https://viajaconintu.pages.dev/descargar?v=1.5-qa-6) coincide en tamaño y SHA-256; su firma y versión 1.5 / código 6 quedaron verificadas.
- La copia publicada de Intu 1.4 permanece guardada localmente para recuperación.

Referencias: [catálogo propio y búsqueda híbrida](poi/README.md), [Geocoding v5](https://docs.mapbox.com/api/search/geocoding-v5/).
