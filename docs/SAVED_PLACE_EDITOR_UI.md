# Guardar lugar — actualización visual de 1.27

El editor utiliza un diálogo blanco de ancho limitado, título e icono de ubicación, etiquetas cortas y textos de ayuda fuera de los campos. La ubicación elegida aparece en una sección propia con un botón para volver al mapa. Guardar es la acción primaria; Cancelar y el cierre superior siguen descartando el formulario.

El contenido se desplaza cuando aparece el teclado o se amplía el texto. Las acciones quedan fuera de ese desplazamiento y se apilan en ventanas estrechas. El encabezado se compacta cuando hay poco alto disponible o texto grande.

Solo se sustituyó la presentación de `SavedPlaceEditorDialog`. Se conservan los límites de 30/250 caracteres, el punto seleccionado, los identificadores Casa/Trabajo, los nombres y referencias al regresar del mapa, la validación y el tratamiento de errores. No se cambia el almacenamiento de direcciones.

La compilación y distribución se coordinan con QA en 1.27/código 28. `SavedPlaceEditorTest` contiene los ocho casos existentes y dos nuevos: nombre vacío/reintento de guardado y texto ampliado con teclado. Las referencias usan etiquetas semánticas estables para permitir las nuevas etiquetas visibles.

Verificación del 2 de octubre de 2026: el APK y el APK de pruebas compilaron; QA confirmó 52 pruebas JVM sin fallos y lint sin errores. Los 10 casos de `SavedPlaceEditorTest` pasaron en el emulador (`OK (10 tests)`), incluidos teclado, texto ampliado, reintento y cambios de ubicación. La evidencia está en `build/qa-admin-design-1.27/native-saved-editor.txt`. La instalación en el Samsung quedó pendiente al perderse la conexión inalámbrica; no se afirma una revisión física completada. QA conserva la distribución de la entrega conjunta.

QA detectó que el `LocalDensity` suministrado fuera de un `Dialog` nativo no altera por sí solo su tipografía. Por eso repitió los dos casos nuevos con `font_scale` de Android en 1.8: ambos pasaron (`OK (2 tests)`) y se restauró el ajuste del emulador. La evidencia adicional está en `build/qa-admin-design-1.27/native-system-font-1.8.txt`; las capturas `screenshots/saved-place-form-normal.png` y `screenshots/saved-place-system-font-1.8-keyboard.png` de esa carpeta muestran el formulario con tamaño normal y con texto grande/teclado. Guardar permanece visible en ambos casos.
