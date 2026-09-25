-- Una sola política de lectura en rides (Postgres evalúa todas las permisivas en cada consulta)
drop policy "rides: participantes ven su viaje" on public.rides;
drop policy "rides: conductores aprobados ven solicitudes de su tipo de vehículo" on public.rides;

create policy "rides: participantes y conductores aprobados (solicitudes abiertas de su tipo)"
  on public.rides for select to authenticated
  using (
    rider_id = (select private.requesting_uid())
    or driver_id = (select private.requesting_uid())
    or (status = 'searching' and vehicle_type = (select private.current_driver_vehicle_type()))
  );

create index vehicles_vehicle_type_idx on public.vehicles (vehicle_type);
