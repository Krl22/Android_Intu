-- MVP: direct payment confirmation and participant-safe driver tracking.

alter table public.rides
  add column if not exists payment_confirmed_at timestamptz;

create or replace function public.advance_ride(p_ride_id uuid, p_status public.ride_status)
returns public.rides
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
begin
  if v_uid is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  update public.rides
     set status = p_status,
         arrived_at   = case when p_status = 'arrived'     then now() else arrived_at end,
         started_at   = case when p_status = 'in_progress' then now() else started_at end,
         completed_at = case when p_status = 'completed'   then now() else completed_at end,
         final_fare   = case when p_status = 'completed'   then estimated_fare else final_fare end,
         payment_confirmed_at = case when p_status = 'completed' then now() else payment_confirmed_at end
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
    update public.driver_locations set is_available = true where driver_id = v_uid;
  end if;
  return v_ride;
end
$$;

revoke execute on function public.advance_ride(uuid, public.ride_status) from public, anon;
grant execute on function public.advance_ride(uuid, public.ride_status) to authenticated;

create or replace function public.ride_driver_location(p_ride_id uuid)
returns table (latitude double precision, longitude double precision, heading real, updated_at timestamptz)
language sql
stable
security definer
set search_path = ''
as $$
  select dl.latitude, dl.longitude, dl.heading, dl.updated_at
  from public.rides r
  join public.driver_locations dl on dl.driver_id = r.driver_id
  where r.id = p_ride_id
    and r.status in ('accepted', 'arrived', 'in_progress')
    and private.requesting_uid() in (r.rider_id, r.driver_id)
$$;

revoke execute on function public.ride_driver_location(uuid) from public, anon;
grant execute on function public.ride_driver_location(uuid) to authenticated;
