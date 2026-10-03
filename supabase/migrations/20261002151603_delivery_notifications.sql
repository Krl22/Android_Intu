create or replace function private.notify_ride_update()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_to     text;
  v_title  text;
  v_body   text;
  v_tokens text[];
  v_secret text;
  v_plate  text := nullif(new.vehicle_plate, '');
  v_driver text := coalesce(nullif(new.driver_name, ''), case when new.service_kind = 'delivery' then 'Tu repartidor' else 'Tu conductor' end);
begin
  case new.status
    when 'accepted' then
      v_to := new.rider_id;
      v_title := case when new.service_kind = 'delivery' then 'Tu repartidor va en camino' else 'Tu conductor va en camino' end;
      v_body := concat_ws(' · ', v_driver, nullif(new.vehicle_description, ''), 'Placa ' || v_plate);
    when 'arrived' then
      v_to := new.rider_id;
      v_title := case when new.service_kind = 'delivery' then 'Tu repartidor llegó al recojo' else 'Tu conductor llegó' end;
      v_body := v_driver || ' te espera en el punto de recojo.' || coalesce(' Placa ' || v_plate || '.', '');
    when 'in_progress' then
      v_to := new.rider_id;
      v_title := case when new.service_kind = 'delivery' then 'Tu paquete está en camino' else 'Viaje iniciado' end;
      v_body := case when new.service_kind = 'delivery' then 'Tu paquete va hacia ' else 'Vas camino a ' end || new.destination_address || '.';
    when 'completed' then
      v_to := new.rider_id;
      v_title := case when new.service_kind = 'delivery' then 'Envío entregado' else 'Llegaste a tu destino' end;
      v_body := 'Total: S/ ' || to_char(coalesce(new.final_fare, new.estimated_fare), 'FM999990.00')
                || case when new.payment_method = 'yape_plin' then ' por Yape o Plin' else ' en efectivo' end
                || case when new.service_kind = 'delivery' then '. Califica a tu repartidor.' else '. Califica tu viaje.' end;
    when 'searching' then
      -- El conductor soltó el viaje (cancel_ride lo devuelve a la búsqueda)
      if old.status not in ('accepted', 'arrived') then
        return null;
      end if;
      v_to := new.rider_id;
      v_title := case when new.service_kind = 'delivery' then 'Tu repartidor canceló' else 'Tu conductor canceló' end;
      v_body := case when new.service_kind = 'delivery' then 'Estamos buscando otro repartidor para tu envío.' else 'Estamos buscando otro conductor para ti.' end;
    when 'cancelled' then
      if new.cancelled_by = 'rider' then
        v_to := old.driver_id;
        v_title := case when new.service_kind = 'delivery' then 'Quien envía canceló el pedido' else 'El pasajero canceló el viaje' end;
        v_body := 'Ya puedes recibir otras solicitudes.';
      elsif new.cancel_reason = 'no_driver_found' then
        v_to := new.rider_id;
        v_title := case when new.service_kind = 'delivery' then 'No encontramos repartidor' else 'No encontramos conductor' end;
        v_body := case when new.service_kind = 'delivery' then 'No hay repartidores disponibles cerca. Intenta de nuevo en unos minutos.' else 'No hay conductores disponibles cerca. Intenta de nuevo en unos minutos.' end;
      else
        v_to := new.rider_id;
        v_title := case when new.service_kind = 'delivery' then 'Tu envío fue cancelado' else 'Tu viaje fue cancelado' end;
        v_body := case when new.service_kind = 'delivery' then 'Puedes pedir otro envío cuando quieras.' else 'Puedes pedir otro viaje cuando quieras.' end;
      end if;
    else
      return null;
  end case;

  if v_to is null then
    return null;
  end if;
  select array_agg(token) into v_tokens from public.device_tokens where user_id = v_to;
  select decrypted_secret into v_secret from vault.decrypted_secrets where name = 'push_webhook_secret';
  if v_tokens is null or v_secret is null then
    return null;
  end if;

  perform net.http_post(
    url := 'https://us-central1-intu-e8403.cloudfunctions.net/ridePush',
    body := jsonb_build_object(
      'tokens', to_jsonb(v_tokens),
      'title', v_title,
      'body', v_body,
      'rideId', new.id,
      'status', new.status
    ),
    headers := jsonb_build_object('Content-Type', 'application/json', 'X-Intu-Secret', v_secret),
    timeout_milliseconds := 8000
  );
  return null;
exception when others then
  -- Un aviso que falla nunca debe impedir el cambio de estado del viaje
  raise warning 'notify_ride_update: %', sqlerrm;
  return null;
end
$$;

revoke all on function private.notify_ride_update() from public, anon, authenticated;

