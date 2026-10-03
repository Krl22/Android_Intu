-- Verify sender PIN before collecting payment at pickup.
create or replace function private.collect_delivery_payment(p_ride_id uuid)
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
  if v_detail.payer = 'sender' and not exists (select 1 from private.ride_start_pins
       where ride_id = p_ride_id and driver_id = v_uid and verified_at is not null) then
    raise exception 'pin_required' using errcode = 'P0001';
  end if;
  update public.delivery_details set payment_collected_at = now() where ride_id = p_ride_id;
  return jsonb_build_object('confirmed', true);
end
$$;
