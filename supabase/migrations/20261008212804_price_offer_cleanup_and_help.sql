-- Close remaining proposals when a normal acceptance, cancellation or expiry ends the search.
create index ride_price_offers_driver_idx on private.ride_price_offers(driver_id,created_at);
create function private.close_ride_price_offers()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if old.status = 'searching' and new.status <> 'searching' then
    update private.ride_price_offers set status='rejected' where ride_id=new.id and status='pending';
  end if;
  return new;
end $$;
revoke all on function private.close_ride_price_offers() from public, anon, authenticated;
create trigger rides_close_price_offers after update of status on public.rides
for each row execute function private.close_ride_price_offers();

-- The acceptance trigger closes all pending offers; retain the winning offer as accepted.
do $$ declare body text; updated text; begin
  body := pg_get_functiondef('private.respond_ride_price_offer(uuid,boolean)'::regprocedure);
  updated := replace(body, 'where ride_id=r.id and status=''pending'';',
    'where ride_id=r.id and (status=''pending'' or id=p_offer_id);');
  if updated=body then raise exception 'Expected offer response missing'; end if;
  execute updated;
end $$;

update private.support_chat_settings set knowledge = replace(replace(knowledge,
  '- Puedes preferir una marca de mototaxi. Elegir Honda cuesta 12 % más.',
  '- Puedes preferir una marca de mototaxi. Honda puede tener un recargo configurable; consulta el precio en la app.'),
  '- El precio que ves al confirmar es el que pagas; no cambia durante el viaje.',
  '- El precio se conserva durante el viaje. Si el administrador habilita propuestas de precio, un conductor puede ofrecer otro importe durante la búsqueda. Solo cambia si tú aceptas y confirmas esa propuesta antes de asignar el viaje.')
where singleton;
