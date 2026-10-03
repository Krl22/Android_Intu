alter table public.delivery_details add column payment_collected_at timestamptz;

-- Only the assigned courier records a cash/Yape receipt, at the agreed pickup/delivery stage.
create function private.collect_delivery_payment(p_ride_id uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
  v_detail public.delivery_details;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  select * into v_ride from public.rides where id = p_ride_id and driver_id = v_uid for update;
  if not found or v_ride.service_kind <> 'delivery' then
    raise exception 'ride_not_found' using errcode = 'P0002';
  end if;
  select * into v_detail from public.delivery_details where ride_id = p_ride_id;
  if v_detail.payment_collected_at is not null then
    return jsonb_build_object('confirmed', true);
  end if;
  if (v_detail.payer = 'sender' and v_ride.status <> 'arrived')
     or (v_detail.payer = 'recipient' and v_ride.status <> 'in_progress') then
    raise exception 'invalid_payment_stage' using errcode = 'P0001';
  end if;
  update public.delivery_details set payment_collected_at = now() where ride_id = p_ride_id;
  return jsonb_build_object('confirmed', true);
end
$$;
revoke all on function private.collect_delivery_payment(uuid) from public, anon;
grant execute on function private.collect_delivery_payment(uuid) to authenticated;
create function public.confirm_delivery_payment(p_ride_id uuid)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.collect_delivery_payment(p_ride_id)
$$;
revoke all on function public.confirm_delivery_payment(uuid) from public, anon;
grant execute on function public.confirm_delivery_payment(uuid) to authenticated;

create or replace function public.advance_ride(p_ride_id uuid, p_status public.ride_status)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
  v_payment_collected_at timestamptz;
  v_payer text;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  select * into v_ride from public.rides where id = p_ride_id and driver_id = v_uid for update;
  if not found then raise exception 'invalid_transition' using errcode = 'P0001'; end if;
  if v_ride.service_kind = 'delivery' then
    select payer, payment_collected_at into v_payer, v_payment_collected_at
      from public.delivery_details where ride_id = p_ride_id;
    if v_payment_collected_at is null and
       (p_status = 'completed' or (p_status = 'in_progress' and v_payer = 'sender')) then
      raise exception 'delivery_payment_required' using errcode = 'P0001';
    end if;
  end if;
  -- No se va por el siguiente pasajero ni se inicia su viaje hasta terminar el actual
  if p_status in ('arrived', 'in_progress') and exists (
       select 1 from public.rides
       where driver_id = v_uid and status = 'in_progress' and id <> p_ride_id) then
    raise exception 'finish_current_trip' using errcode = 'P0001';
  end if;

  if p_status = 'in_progress' and exists (
       select 1 from private.ride_start_pins
       where ride_id = p_ride_id
         and (verified_at is null or driver_id is distinct from v_uid)) then
    raise exception 'pin_required' using errcode = 'P0001';
  end if;

  update public.rides
     set status = p_status,
         arrived_at   = case when p_status = 'arrived'     then now() else arrived_at end,
         started_at   = case when p_status = 'in_progress' then now() else started_at end,
         completed_at = case when p_status = 'completed'   then now() else completed_at end,
         final_fare   = case when p_status = 'completed'   then estimated_fare else final_fare end,
         payment_confirmed_at = case when p_status = 'completed' then coalesce(v_payment_collected_at, now()) else payment_confirmed_at end
   where id = p_ride_id
     and driver_id = v_uid
     and (   (status = 'accepted'    and p_status = 'arrived')
          or (status = 'arrived'     and p_status = 'in_progress')
          or (status = 'in_progress' and p_status = 'completed'))
  returning * into v_ride;

  if not found then
    raise exception 'invalid_transition' using errcode = 'P0001';
  end if;

  if p_status = 'completed' then
    -- El siguiente pasajero ya no espera a que termine otro viaje
    update public.rides set driver_on_other_trip = false
     where driver_id = v_uid and status in ('accepted', 'arrived') and driver_on_other_trip;
    perform private.refresh_driver_availability(v_uid);
  end if;
  return v_ride;
end
$$;
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

  if v_uid = v_ride.driver_id and v_ride.service_kind = 'delivery'
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
