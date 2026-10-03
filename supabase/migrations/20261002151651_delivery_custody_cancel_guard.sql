-- A paid handover cannot be cancelled or rematched, even by the sender.
create or replace function public.cancel_ride(p_ride_id uuid, p_reason text default null)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
  v_driver text;
begin
  select * into v_ride from public.rides where id = p_ride_id for update;

  if not found or v_uid is null or (v_ride.rider_id <> v_uid and v_ride.driver_id is distinct from v_uid) then
    raise exception 'ride_not_found' using errcode = 'P0002';
  end if;
  if v_ride.status not in ('searching', 'accepted', 'arrived') then
    raise exception 'invalid_transition' using errcode = 'P0001';
  end if;

  if v_ride.service_kind = 'delivery'
     and exists (select 1 from public.delivery_details where ride_id = p_ride_id and payment_collected_at is not null) then
    raise exception 'delivery_in_custody' using errcode = 'P0001';
  end if;
  v_driver := v_ride.driver_id;

  if v_ride.rider_id = v_uid then
    update public.rides
       set status = 'cancelled', cancelled_at = now(), cancelled_by = 'rider', cancel_reason = left(p_reason, 500)
     where id = p_ride_id
    returning * into v_ride;
  else
    -- El conductor suelta el viaje: vuelve a buscar conductor
    update public.rides
       set status = 'searching', driver_id = null, vehicle_id = null, accepted_at = null, arrived_at = null,
           driver_name = null, driver_phone = null, driver_photo_url = null,
           vehicle_plate = null, vehicle_description = null, rider_phone = null,
           driver_on_other_trip = false
     where id = p_ride_id
    returning * into v_ride;
  end if;

  if v_driver is not null then
    perform private.refresh_driver_availability(v_driver);
  end if;
  return v_ride;
end
$$;

