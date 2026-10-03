# Intu MVP: prueba pasajero–chofer

## Alcance

- Un solo tipo de servicio: mototaxi.
- El pasajero elige punto de recojo, destino y paga con Yape o efectivo.
- Supabase calcula y guarda la tarifa en soles.
- El chofer recibe el pago completo; no hay comisión ni transacción dentro de la app.
- El chofer confirma el pago al finalizar el viaje.
- La aprobación de choferes es manual.

## Servicios

- Firebase Authentication mantiene el inicio de sesión.
- Supabase almacena perfiles, choferes, vehículos, ubicaciones y viajes.
- La app actualiza el flujo mediante consultas cortas cada 1.5–2 segundos para esta prueba cerrada.

## Preparación del backend

1. Aplicar `supabase/migrations/20260928043517_mvp_payment_and_live_location.sql` y
   `supabase/migrations/20260928043518_allow_upsert_on_own_rows.sql` (ya aplicadas en `vkguzpciwpfvaeyedepl`).
2. Activar Identity Platform en Firebase (Authentication → Settings → *Upgrade to Firebase Authentication
   with Identity Platform*). Sin esto, Firebase rechaza las funciones de bloqueo del paso siguiente.
3. Desplegar únicamente las funciones de autenticación nuevas, sin borrar las funciones existentes:

   ```bash
   firebase deploy --config firebase/firebase.json \
     --only functions:beforecreated,functions:beforesignedin
   ```

3. Desplegar las reglas:

   ```bash
   firebase deploy --config firebase/firebase.json --only firestore:rules,database
   ```

4. Después del despliegue, cada tester existente debe cerrar sesión e iniciar sesión otra vez para recibir el claim `role: authenticated`.

## Aprobación manual del chofer

En Supabase, buscar el registro del tester en `public.drivers` y cambiar:

```sql
update public.drivers
set status = 'approved', approved_at = now()
where id = '<firebase_uid>';
```

La app no puede aprobar choferes por sí sola.

## Guion de prueba en dos teléfonos

1. Registrar al pasajero y al chofer con cuentas diferentes.
2. En el teléfono del chofer, completar DNI, licencia, placa y datos del mototaxi.
3. Aprobar manualmente al chofer y volver a entrar a la cuenta.
4. Activar el modo conductor y pulsar **Empezar ahora**.
5. En el teléfono del pasajero, elegir destino, ajustar el punto de recojo, elegir Yape o efectivo y confirmar.
6. El chofer acepta la solicitud.
7. Verificar en ambos teléfonos la ubicación y los datos del viaje.
8. El chofer pulsa **Llegué**, luego **Iniciar viaje**.
9. Al terminar, el chofer recibe el pago directo y pulsa **Confirmar pago y finalizar**.
10. El pasajero debe ver la confirmación y cerrar con **Listo**.

## Catálogo de lugares (1.4)

La migración `20261001044050_places_catalog.sql` ya está aplicada. Los 55 candidatos de Satipo están en borrador y requieren confirmar la entrada de recojo antes de publicar. Cuenta → Administración → Lugares permite crear, editar, publicar y desactivar.

Probar búsqueda por nombre y alias sin tildes, selección del destino, actualización manual, retiro de un lugar y conservación del catálogo sin conexión. El catálogo vacío ofrece elegir el destino en el mapa. Detalles y guía: `docs/poi/README.md`.

## Compilación

El workflow `Android MVP` genera `intu-mvp-debug`. Requiere el secreto de GitHub `MAPBOX_ACCESS_TOKEN`. Los artefactos de Mapbox usados por el proyecto se descargan sin un token privado.

Para desarrollo local en Windows, configurar `sdk.dir` y `MAPBOX_ACCESS_TOKEN` en `local.properties` (ignorado por Git). La preparación y los comandos están en `docs/DEV_SETUP.md`.

## Búsqueda híbrida de Intu 1.5

- En Inicio, buscar un lugar publicado y comprobar que aparece en «Lugares de Intu», antes de las calles de Mapbox.
- Buscar «jirón manuel prado» o «jirón julio»: deben aparecer calles de Satipo en «Calles y direcciones», aunque no haya coincidencias del catálogo.
- Elegir una calle y comprobar el destino en el mapa. Repetir en «Dirección del marcador» al ajustar recojo y destino.
- Escribir rápido y cambiar la consulta: los resultados de la búsqueda anterior deben desaparecer y no reaparecer cuando termine su petición.
- Sin conexión, comprobar que los lugares del catálogo guardado y «Elegir en mapa» siguen disponibles.
