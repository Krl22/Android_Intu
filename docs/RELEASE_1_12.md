# Intu 1.12 QA — configuración común de mapas

Versión 1.12, código 13, firmada con el certificado debug habitual.

## Cambios

- Inicio, conductor, selector del catálogo y detalle de viajes crean sus mapas mediante `createIntuMapView`.
- La escala se desactiva desde la creación de cada mapa. El logo y la atribución conservan su visibilidad y posiciones existentes.
- El punto focal del zoom se mantiene en el centro del viewport y se actualiza al cambiar el tamaño. Se desactiva el desplazamiento durante el pellizco; el desplazamiento con un dedo sigue permitido.
- Salir del modo de selección conserva la configuración común del zoom.
- El catálogo comparte con Inicio el seguimiento de la coordenada proyectada bajo el pin. Al confirmar consulta ese mismo punto, en vez de tomar directamente `cameraState.center`.
- No se modifica el esquema de datos ni los permisos del backend.

## Validación y límites

- APK y APK de pruebas compilados. 31 pruebas JVM aprobadas, sin fallos.
- Lint: 0 errores, 72 avisos y 10 sugerencias existentes.
- Firma debug y alineación ZIP de 16 KB verificadas.
- Una ejecución en el emulador API 37 / páginas de 16 KB aprobó cuatro pruebas: zoom centrado y desplazamiento, detalle de ruta, restauración del GPS después de simular la ubicación y actualización de la búsqueda al cambiar la zona.
- Las ejecuciones posteriores no completaron toda la validación táctil: hubo ANR de System UI, demora en el arranque del SDK y fallos del gesto sintético. La prueba del catálogo se ajustó para seleccionar la ventana del diálogo y usar un pellizco más amplio, manteniendo las comprobaciones de zoom real y coordenadas.
- En el Samsung las pruebas también encontraron una pantalla inactiva o ausencia de la jerarquía Compose de la actividad auxiliar. No se considera validada la prueba completa de zoom, desplazamiento y confirmación del catálogo. Se conservan los resultados fallidos para diagnóstico; no se presentan como aprobados.
- Evidencia de estas comprobaciones: `%LOCALAPPDATA%/Temp/intu-qa-1.12/`, incluida `instrumentation-retry.log` (cuatro pruebas aprobadas), los intentos posteriores y `startup-anr.txt`.
- El APK normal se reinstaló por ADB inalámbrico conservando los datos. Se comprobó versión 1.12 / código 13. El tiempo de apagado de pantalla que se amplió temporalmente para la prueba fue restaurado al finalizar.

## Publicación

- Archivo: `app/build/outputs/apk/debug/app-debug.apk`, 105,042,580 bytes.
- SHA-256: `F550425F755EE1FCAC3178D10B01A531D6244E36D0DE440D732C61881CA44F42`.
- Publicado en R2 el 2026-10-01 a las 17:33:34 UTC.
- La descarga completa desde [Cloudflare Pages](https://viajaconintu.pages.dev/descargar?v=1.12-qa-13) coincide con el APK local en tamaño, SHA-256 y versión.

Para completar el QA manual: abrir Administración → catálogo → elegir punto en el mapa, comprobar que no aparezca la escala, hacer zoom con los dedos fuera del centro, desplazar el mapa y confirmar que el lugar guardado corresponda al punto bajo el pin.
