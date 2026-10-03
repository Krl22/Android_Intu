# Intu 1.27 — rediseño del panel de administración

Versión 1.27 / código 28. Publicada el 2 de octubre de 2026 a las 17:15 (America/New_York), 21:15:47.212 UTC.

## Cambios

- Diseño común para Conductores, Usuarios, Reportes, Lugares y Notificaciones: fondo suave, tarjetas blancas, acentos turquesa, iconos, filtros y estados de carga/error/vacío consistentes. El tema está limitado al panel y sus formularios.
- Cabecera compacta con actualizar, pestañas desplazables y acceso a simulación de ubicación. La barra de estado y navegación usa iconos oscuros durante el panel y recupera su aspecto previo al salir.
- Conductores: búsqueda local por nombre, contacto, vehículo o placa; estados más visibles; documentación desplegable; acciones legibles. Se mantienen las confirmaciones para rechazar/suspender.
- Usuarios: búsqueda local por nombre, contacto o UID, resumen de viajes, acceso Google/SMS desplegable y menú Gestionar cuenta. Reiniciar, cambiar permisos y eliminar siguen pasando por las confirmaciones existentes. Una acción deshabilitada también se distingue visualmente. Se conserva la consulta real de proveedores de Firebase y el estado desconocido ante una consulta fallida.
- Reportes: búsqueda y filtros por estado, descripción legible, datos técnicos desplegables y acciones de revisar/resolver/reabrir.
- Catálogo: estados con cantidades, búsqueda, tarjetas con categoría/localidad/referencia y verificación del recojo, y formulario con el mismo tema del panel. Se mantienen campos, validaciones, atribución y selección en el mapa.
- Notificaciones: tarjeta de pausa general, elecciones agrupadas y acciones ante el bloqueo de Android. Se conserva el guardado por cuenta y el bloqueo de edición mientras se guarda.
- Simulación: ubicación actual visible, elección en el mapa destacada, accesos a Satipo/Río Negro y restauración del GPS real.
- Incluye los cambios coordinados de otros chats: retiro de Guardar este lugar desde el marcador de Inicio y nuevo formulario de lugares guardados. Los atajos y la gestión de direcciones guardadas siguen disponibles.
- No se modificaron repositorios, RPC, permisos ni backend en este rediseño.

## Verificación

- Build final app/test, 52 pruebas JVM y lint PASS. JVM: 0 fallos y 0 errores. Lint: 0 errores, 75 advertencias y 10 sugerencias existentes.
- Primera revisión nativa: OK (10 tests), 64.462 s, para el editor de lugares; OK (12 tests), 79.195 s, para panel, notificaciones, acceso Google/SMS, catálogo y simulación sobre mapas reales. Fixtures aislados, sin modificar usuarios, permisos, reportes o viajes en el servidor.
- La revisión visual detectó falta de contraste en la barra de estado y en una opción deshabilitada. Se corrigieron y se reconstruyó el APK final.
- APK final: OK (7 tests), 43.759 s. Navegación por las cinco pestañas con tipografía al 130 %, acciones de tarjetas, búsqueda/filtros, menús, guardado de notificaciones y editor de lugares con teclado/reintento. Capturas de las cinco pestañas, menú de cuenta y simulación inspeccionadas.
- Adicionalmente: OK (2 tests), 10.343 s, con font_scale real de Android a 1.8 para los diálogos del editor. LocalDensity externo no basta para aumentar el texto de una ventana Dialog nativa; se usó la configuración real del emulador. Guardar permanece visible con el teclado. Se restauró el ajuste original.
- Firma debug habitual y alineación de 16 KB verificadas. Certificado SHA-1: 7a4ec69cae01b4b8a5eeeff832d833e176c0efd0. Versión del APK comprobada con aapt2. Hashes de app/src/main comprobados tras compilación y antes de publicar.
- Emulador Phone_1 API 37 cerrado después de QA. No se realizaron pruebas interactivas en el teléfono físico.

## Entrega

- APK publicado en R2 intu-apk/intu.apk; descarga completa verificada desde https://viajaconintu.pages.dev/descargar?v=1.27-28-admin-design.
- Tamaño: 106486557 bytes. SHA-256: 9DB5B801C06B0ACA04A3942A3F33874E171A4998D125FE1AC9D821A973370A36. El archivo descargado coincide con el APK validado.
- Reinstalación física pendiente: ADB y mDNS no encuentran el Samsung; la dirección inalámbrica anterior no responde. No se afirma instalación en ese dispositivo.
- Evidencia, APK y capturas en build/qa-admin-design-1.27. Ver también docs/SAVED_PLACE_EDITOR_UI.md para el formulario coordinado.
