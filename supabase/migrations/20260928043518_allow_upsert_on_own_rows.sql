-- La app guarda perfiles y conductores con upsert (POST ?on_conflict=id).
-- PostgREST lo traduce a INSERT ... ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id, ...
-- y Postgres exige permiso UPDATE sobre todas las columnas del SET, incluida la llave.
-- Lo mismo pasa al editar el vehículo: el PATCH incluye driver_id.
--
-- Es seguro: las políticas RLS de update ya obligan (WITH CHECK) a que la llave sea
-- el uid del propio usuario, así que nadie puede reasignar filas a otra persona.

grant update (id) on public.profiles to authenticated;
grant update (id) on public.drivers to authenticated;
grant update (driver_id) on public.vehicles to authenticated;
