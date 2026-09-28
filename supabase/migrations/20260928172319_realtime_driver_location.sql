-- Ubicación del conductor en vivo para el pasajero, por Realtime (sin consultar cada 1.5 s).
-- El pasajero puede leer la fila de ubicación de SU conductor solo mientras dura su viaje;
-- es lo mismo que ya entregaba la función ride_driver_location.

create policy "driver_locations: el pasajero ve a su conductor durante el viaje"
  on public.driver_locations for select to authenticated
  using (
    exists (
      select 1 from public.rides r
      where r.driver_id = driver_locations.driver_id
        and r.rider_id = (select private.requesting_uid())
        and r.status in ('accepted', 'arrived', 'in_progress')
    )
  );

-- Postgres Changes: cada suscriptor recibe solo las filas que su RLS le deja ver
alter publication supabase_realtime add table public.driver_locations;
