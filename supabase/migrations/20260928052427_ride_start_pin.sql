-- PIN de seguridad para iniciar el viaje (como Uber).
-- Al pedir un viaje se genera un PIN de 4 dígitos que solo ve el pasajero. Al subir al mototaxi,
-- el pasajero se lo dice al conductor y este lo ingresa; sin PIN correcto no se puede iniciar.
--
-- El PIN vive en una tabla del esquema private (no expuesto por la API): el conductor ve la fila
-- del viaje, pero nunca el PIN. Máximo 5 intentos fallidos por conductor.

create table private.ride_start_pins (
  ride_id         uuid primary key references public.rides (id) on delete cascade,
  pin             text not null check (pin ~ '^[0-9]{4}$'),
  driver_id       text,          -- conductor al que corresponden los intentos
  failed_attempts smallint not null default 0,
  verified_at     timestamptz
);
alter table private.ride_start_pins enable row level security;
revoke all on private.ride_start_pins from public, anon, authenticated;

-- PIN aleatorio criptográfico al crear el viaje
create or replace function private.rides_create_start_pin()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_bytes bytea := extensions.gen_random_bytes(2);
begin
  insert into private.ride_start_pins (ride_id, pin)
  values (new.id, lpad((((get_byte(v_bytes, 0) * 256) + get_byte(v_bytes, 1)) % 10000)::text, 4, '0'));
  return new;
end
$$;

create trigger rides_create_start_pin
  after insert on public.rides
  for each row execute function private.rides_create_start_pin();

-- Solo el pasajero del viaje puede ver su PIN, y solo antes de que empiece
create or replace function public.ride_start_pin(p_ride_id uuid)
returns table (pin text)
language sql
stable
security definer
set search_path = ''
as $$
  select p.pin
  from private.ride_start_pins p
  join public.rides r on r.id = p.ride_id
  where p.ride_id = p_ride_id
    and r.rider_id = private.requesting_uid()
    and r.status in ('searching', 'accepted', 'arrived')
$$;
revoke execute on function public.ride_start_pin(uuid) from public, anon;
grant execute on function public.ride_start_pin(uuid) to authenticated;

-- El conductor verifica el PIN. No lanza error si es incorrecto, para que el intento fallido
-- quede guardado (un error desharía el conteo).
create or replace function public.verify_ride_pin(p_ride_id uuid, p_pin text)
returns table (verified boolean, attempts_left integer)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid text := private.requesting_uid();
  v_row private.ride_start_pins;
  v_max constant integer := 5;
begin
  if not exists (select 1 from public.rides
                 where id = p_ride_id and driver_id = v_uid and status = 'arrived') then
    raise exception 'invalid_transition' using errcode = 'P0001';
  end if;

  select * into v_row from private.ride_start_pins where ride_id = p_ride_id for update;
  if not found then
    raise exception 'ride_not_found' using errcode = 'P0002';
  end if;

  -- Si cambió el conductor (el anterior canceló), sus intentos no cuentan para el nuevo
  if v_row.driver_id is distinct from v_uid then
    update private.ride_start_pins
       set driver_id = v_uid, failed_attempts = 0, verified_at = null
     where ride_id = p_ride_id
    returning * into v_row;
  end if;

  if v_row.failed_attempts >= v_max then
    raise exception 'pin_locked' using errcode = 'P0001';
  end if;

  if v_row.pin = p_pin then
    update private.ride_start_pins set verified_at = now() where ride_id = p_ride_id;
    return query select true, v_max - v_row.failed_attempts;
  else
    update private.ride_start_pins set failed_attempts = failed_attempts + 1 where ride_id = p_ride_id;
    return query select false, v_max - v_row.failed_attempts - 1;
  end if;
end
$$;
revoke execute on function public.verify_ride_pin(uuid, text) from public, anon;
grant execute on function public.verify_ride_pin(uuid, text) to authenticated;

-- advance_ride: para pasar a in_progress, el PIN tiene que estar verificado por este conductor.
-- Los viajes creados antes de esta migración no tienen PIN y siguen funcionando igual.
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
