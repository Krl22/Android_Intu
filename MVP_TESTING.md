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

1. Aplicar `supabase/migrations/20260927020000_mvp_payment_and_live_location.sql`.
2. Desplegar únicamente las funciones de autenticación nuevas, sin borrar las funciones existentes:

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
4. Activar el modo conductor y pulsar **Buscar clientes**.
5. En el teléfono del pasajero, elegir destino, ajustar el punto de recojo, elegir Yape o efectivo y confirmar.
6. El chofer acepta la solicitud.
7. Verificar en ambos teléfonos la ubicación y los datos del viaje.
8. El chofer pulsa **Llegué**, luego **Iniciar viaje**.
9. Al terminar, el chofer recibe el pago directo y pulsa **Confirmar pago y finalizar**.
10. El pasajero debe ver la confirmación y cerrar con **Listo**.

## Compilación

El workflow `Android MVP` genera `intu-mvp-debug`. Requiere los secretos de GitHub `MAPBOX_ACCESS_TOKEN` y `MAPBOX_DOWNLOADS_TOKEN`.
