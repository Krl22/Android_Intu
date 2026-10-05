# Publicidad en Inicio

Las promociones de negocios usan una altura común de 248 dp. El tamaño se adapta
por igual a la escala de texto del dispositivo. Títulos y detalles largos se
truncan en la tarjeta; el menú conserva la información completa.

El carrusel muestra cada anuncio durante 3 segundos por defecto, avanza al
siguiente y vuelve al primero al terminar. Permite deslizar manualmente y reinicia
el tiempo al cambiar de página. La rotación se pausa al abrir el menú del negocio
y cuando la app pasa a segundo plano.

En **Admin → Negocios → Rotación de publicidad**, el administrador
puede guardar un intervalo entero de 1 a 60 segundos. Es un ajuste compartido;
los clientes lo reciben con la actualización del feed de Inicio (cada 15 segundos
mientras la pantalla está activa). No modifica el interruptor de delivery de
negocios. Con un solo anuncio no hay avance automático.

El ajuste está en `private.business_delivery_settings.ad_interval_seconds`.
`business_feed` y `admin_business_state` lo incluyen en sus respuestas.
`admin_set_ad_interval(p_seconds)` exige una identidad de administrador y valida
el rango en el servidor. No concede acceso directo a la tabla.

Verificación: `AdCarouselTest`, `BusinessDeliveryTest` y
`supabase/tests/ad_carousel_settings.sql` cubren alturas, rotación, deslizamiento,
pausa, cambios del feed, edición confirmada y permisos. Las pruebas SQL revierten
todas las identidades y ajustes temporales.

Validado el 5 de octubre de 2026: 67 pruebas unitarias y 17 escenarios nativos
de carrusel y negocios completados (incluidas repeticiones de los casos pendientes),
tres suites SQL con rollback y lint sin errores. Se revisaron capturas en claro,
oscuro y texto grande. La APK se instaló en Samsung con `adb install -r`.
