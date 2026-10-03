# Catálogo de lugares de Intu

Implementado en Intu 1.4. La migración `20261001044050_places_catalog.sql` ya está aplicada en Supabase `vkguzpciwpfvaeyedepl`, con 55 candidatos de Satipo en borrador.

## Publicar un lugar

1. Entrar con una cuenta administradora y abrir Cuenta → Administración → Lugares.
2. Abrir un borrador o pulsar Nuevo lugar. Revisar nombre, categoría, localidad, dirección y otros nombres separados por comas.
3. Pulsar Elegir punto en el mapa, mover el mapa hasta colocar la entrada accesible bajo el marcador y confirmar el punto. También se pueden introducir coordenadas y confirmar explícitamente el recojo.
4. Elegir Publicado y Guardar lugar. Supabase rechaza publicaciones sin recojo confirmado.
5. En Inicio, escribir al menos dos caracteres. Los publicados del catálogo aparecen primero; desde Intu 1.5, las calles y direcciones de Mapbox aparecen después. Pulsar Actualizar lugares para obtener cambios del catálogo inmediatamente si ya había una copia descargada.

Borrador e Inactivo permanecen ocultos a pasajeros. Desactivar conserva el registro para revisión posterior. Al sincronizar, el teléfono reemplaza su catálogo completo y elimina los resultados retirados. No hace falta otro APK para agregar o editar contenido.

## Arquitectura y costos

- Backend: PostgreSQL y Data API del Supabase existente; no se agregó otro servidor, framework ni servicio de búsqueda.
- Acceso: Firebase Auth existente. Las funciones administrativas verifican `public.is_admin()` usando el UID de Firebase. La tabla tiene RLS, lectura de publicados y ninguna escritura directa para clientes.
- Datos: `public.places_catalog`, con nombres, alias, categorías, localidades, coordenadas de recojo, estados y procedencia. Los puntos originales importados y su fecha se conservan por separado.
- Sincronización: `places_catalog_sync` devuelve una revisión y, solo si cambió, una copia completa de publicados. Revisión y contenido se leen en una misma instantánea. Las altas, cambios y retiradas incrementan la revisión.
- Teléfono: caché pública en SharedPreferences; la búsqueda por nombre, alias, categoría y dirección se realiza localmente, sin peticiones por cada letra. Funciona sin GPS y tolera tildes. La cercanía ordena resultados de igual relevancia, sin excluir otros lugares por una caja geográfica pequeña.
- Actualización automática al entrar a Inicio, con una ventana de cinco minutos entre comprobaciones; actualización manual inmediata. Un error conserva el último catálogo válido y muestra una opción para reintentar.
- Casa, Trabajo y otros favoritos personales siguen separados del catálogo compartido.

El autocompletado del catálogo es local. Desde Intu 1.5, la búsqueda híbrida añade resultados temporales de Mapbox Geocoding v5 para calles, direcciones y localidades, sin copiarlos a Supabase ni a la caché del catálogo. Se consulta solo un buscador activo, después de una pausa de 600 ms y desde dos caracteres; cambiar el texto, cerrar el buscador o seleccionar un resultado cancela la consulta anterior. Los resultados de Intu aparecen inmediatamente y primero, conservando su punto verificado frente a duplicados cercanos. Mapbox mantiene el área de búsqueda anterior de 15 millas alrededor de la ubicación, con Satipo como referencia sin GPS; el catálogo propio no usa esa restricción. Si Mapbox falla, se conserva la búsqueda local y la elección en el mapa.

Las consultas externas pueden consumir la cuota de Mapbox Geocoding. Mapbox también proporciona mapas, rutas y la resolución de dirección del marcador. El almacenamiento y la transferencia siguen sujetos al plan de Supabase. No se ha verificado el plan de facturación ni se promete costo cero para toda la app. Referencia: [Geocoding v5 y solicitudes de autocompletado](https://docs.mapbox.com/api/search/geocoding-v5/).

## Procedencia de los 55 candidatos

`satipo_candidates.json` y `satipo_candidates.csv` contienen datos reales extraídos de OpenStreetMap para el centro de Satipo. El JSON conserva área, fecha, atribución y referencia de cada elemento.

Todos se importaron como `draft` y `pickup_verified=false`. Los puntos `bounding_box_center` son centros aproximados de áreas, no entradas verificadas. Puede haber duplicados o negocios desactualizados; revisar y mantener inactivos los registros innecesarios. La muestra no representa un inventario completo de Satipo ni incluye todos los sitios de Río Negro.

© OpenStreetMap contributors. La muestra derivada se distribuye bajo ODbL 1.0: [licencia y atribución](https://www.openstreetmap.org/copyright). Conservar la procedencia y cumplir las condiciones de atribución y distribución de datos derivados. El panel y los resultados de búsqueda muestran esta atribución para lugares importados.

No se copiaron resultados de Google Places ni Mapbox Search Box al catálogo. Las nuevas entradas manuales deben proceder de información propia o de fuentes que permitan ese uso.

## Validación

`supabase/tests/places_catalog_permissions.sql` comprueba permisos, privacidad de borradores, publicación verificada, revisiones, retiradas, coordenadas inválidas, protección de procedencia y conflictos de edición. Usa una transacción revertida: no deja lugares QA publicados ni modifica los 55 candidatos.

`CatalogSearchTest` verifica alias y tildes, búsqueda sin GPS, relevancia, cercanía, visibilidad, límites y coordenadas inválidas. `PlaceCatalogTest` verifica caché sin conexión, eliminación de lugares retirados, publicación, resultados y selección del punto en un MapView nativo. La prueba de caché usa preferencias aisladas de las del usuario.

Referencias: [Data API de Supabase](https://supabase.com/docs/guides/api), [RLS de Supabase](https://supabase.com/docs/guides/database/postgres/row-level-security), [licencia de OpenStreetMap](https://www.openstreetmap.org/copyright).
