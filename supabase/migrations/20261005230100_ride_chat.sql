-- In-ride chat between the rider and the assigned driver.
-- Messages are written only through send_ride_message, read under RLS (also by Realtime),
-- kept 30 days for complaints and readable afterwards only by admins.

create table public.ride_messages (
  id uuid primary key default gen_random_uuid(),
  ride_id uuid not null references public.rides(id) on delete cascade,
  sender_id text not null,
  -- Driver assignment the conversation belongs to: if a driver releases the ride,
  -- neither they nor the next driver see the other conversation
  driver_id text not null,
  body text not null check (char_length(body) between 1 and 500),
  quick_reply text,
  created_at timestamptz not null default now()
);
create index ride_messages_ride_idx on public.ride_messages (ride_id, created_at);
create index ride_messages_created_idx on public.ride_messages (created_at);
alter table public.ride_messages enable row level security;
revoke all on public.ride_messages from public, anon, authenticated;
grant select on public.ride_messages to authenticated;

create policy ride_messages_participants_read on public.ride_messages
  for select to authenticated
  using (exists (
    select 1 from public.rides r
    where r.id = ride_messages.ride_id
      and r.driver_id = ride_messages.driver_id
      and (r.rider_id = (select private.requesting_uid()) or r.driver_id = (select private.requesting_uid()))
  ));

alter publication supabase_realtime add table public.ride_messages;

-- Fixed texts: drivers on the move answer with these instead of typing.
create function private.ride_quick_reply_text(p_role text, p_code text)
returns text language sql immutable set search_path = '' as $$
  select case p_role
    when 'driver' then case p_code
      when 'on_my_way' then 'Voy en camino.'
      when 'few_minutes' then 'Llego en unos minutos.'
      when 'traffic' then 'Hay tráfico, llegaré un poco tarde.'
      when 'arrived' then 'Ya llegué al punto de recojo.'
      when 'where_are_you' then 'No te encuentro, ¿dónde estás?'
      when 'ok' then 'Entendido.'
    end
    when 'rider' then case p_code
      when 'coming_out' then 'Ya salgo.'
      when 'waiting' then 'Te estoy esperando.'
      when 'where_are_you' then '¿Dónde estás?'
      when 'two_minutes' then 'Voy 2 minutos tarde, por favor espérame.'
      when 'ok' then 'Entendido.'
    end
  end
$$;

create function private.send_ride_message(p_ride_id uuid, p_body text, p_quick_reply text)
returns public.ride_messages language plpgsql security definer set search_path = '' as $$
declare
  v_uid text := private.requesting_uid();
  v_ride public.rides;
  v_role text;
  v_body text := trim(coalesce(p_body, ''));
  v_message public.ride_messages;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  select * into v_ride from public.rides where id = p_ride_id;
  if not found or v_ride.driver_id is null or (v_ride.rider_id <> v_uid and v_ride.driver_id <> v_uid) then
    raise exception 'ride_not_found' using errcode = 'P0002';
  end if;
  if v_ride.status not in ('accepted', 'arrived', 'in_progress') then
    raise exception 'chat_closed' using errcode = 'P0001';
  end if;
  v_role := case when v_ride.driver_id = v_uid then 'driver' else 'rider' end;
  if p_quick_reply is not null then
    v_body := private.ride_quick_reply_text(v_role, p_quick_reply);
    if v_body is null then raise exception 'invalid_message' using errcode = '22023'; end if;
  elsif v_role = 'driver' and v_ride.status <> 'arrived' then
    -- Typing while driving is not allowed; stopped at the pickup the driver can write
    raise exception 'driver_quick_replies_only' using errcode = 'P0001';
  end if;
  if v_body = '' or char_length(v_body) > 500 then
    raise exception 'invalid_message' using errcode = '22023';
  end if;
  if (select count(*) from public.ride_messages
      where ride_id = p_ride_id and sender_id = v_uid and created_at > now() - interval '1 minute') >= 12 then
    raise exception 'too_many_messages' using errcode = 'P0001';
  end if;
  insert into public.ride_messages(ride_id, sender_id, driver_id, body, quick_reply)
  values (p_ride_id, v_uid, v_ride.driver_id, v_body, p_quick_reply)
  returning * into v_message;
  return v_message;
end $$;
revoke all on function private.send_ride_message(uuid, text, text) from public, anon;
grant execute on function private.send_ride_message(uuid, text, text) to authenticated;
create function public.send_ride_message(p_ride_id uuid, p_body text default null, p_quick_reply text default null)
returns public.ride_messages language sql security invoker set search_path = '' as $$
  select * from private.send_ride_message(p_ride_id, p_body, p_quick_reply)
$$;
revoke all on function public.send_ride_message(uuid, text, text) from public, anon;
grant execute on function public.send_ride_message(uuid, text, text) to authenticated;

-- Push to the other participant. Its own tag keeps it from replacing the ride status notice.
create function private.notify_ride_message()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
  v_ride public.rides;
  v_to text;
  v_from text;
  v_tokens text[];
  v_secret text;
begin
  select * into v_ride from public.rides where id = new.ride_id;
  if new.sender_id = v_ride.driver_id then
    v_to := v_ride.rider_id;
    v_from := coalesce(nullif(v_ride.driver_name, ''), case when v_ride.service_kind = 'delivery' then 'Tu repartidor' else 'Tu conductor' end);
  else
    v_to := v_ride.driver_id;
    v_from := coalesce(nullif(v_ride.rider_name, ''), case when v_ride.service_kind = 'delivery' then 'Quien envía' else 'Tu pasajero' end);
  end if;
  select array_agg(token) into v_tokens from public.device_tokens where user_id = v_to;
  select decrypted_secret into v_secret from vault.decrypted_secrets where name = 'push_webhook_secret';
  if v_to is null or v_tokens is null or v_secret is null then
    return null;
  end if;
  perform net.http_post(
    url := 'https://us-central1-intu-e8403.cloudfunctions.net/ridePush',
    body := jsonb_build_object(
      'tokens', to_jsonb(v_tokens),
      'title', v_from,
      'body', left(new.body, 160),
      -- ridePush uses rideId as the notification tag
      'rideId', 'chat-' || new.ride_id,
      'status', 'chat'
    ),
    headers := jsonb_build_object('Content-Type', 'application/json', 'X-Intu-Secret', v_secret),
    timeout_milliseconds := 8000
  );
  return null;
exception when others then
  raise warning 'notify_ride_message: %', sqlerrm;
  return null;
end $$;
revoke all on function private.notify_ride_message() from public, anon, authenticated;
create trigger ride_messages_notify after insert on public.ride_messages
for each row execute function private.notify_ride_message();

-- Admin review for complaints.
create function private.recent_ride_chats(p_days integer)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  return coalesce((select jsonb_agg(row_to_json(t)::jsonb order by t.last_message_at desc) from (
    select m.ride_id, r.rider_name, r.driver_name, r.status, r.requested_at,
           count(*) as messages, max(m.created_at) as last_message_at
    from public.ride_messages m join public.rides r on r.id = m.ride_id
    where m.created_at > now() - make_interval(days => least(greatest(coalesce(p_days, 30), 1), 30))
    group by m.ride_id, r.rider_name, r.driver_name, r.status, r.requested_at
    order by max(m.created_at) desc limit 50) t), '[]'::jsonb);
end $$;
revoke all on function private.recent_ride_chats(integer) from public, anon;
grant execute on function private.recent_ride_chats(integer) to authenticated;
create function public.admin_recent_ride_chats(p_days integer default 30)
returns jsonb language sql stable security invoker set search_path = '' as $$
  select private.recent_ride_chats(p_days)
$$;
revoke all on function public.admin_recent_ride_chats(integer) from public, anon;
grant execute on function public.admin_recent_ride_chats(integer) to authenticated;

create function private.ride_chat_transcript(p_ride_id uuid)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  return coalesce((select jsonb_agg(jsonb_build_object(
      'id', m.id, 'body', m.body, 'created_at', m.created_at,
      'role', case when m.sender_id = m.driver_id then 'driver' else 'rider' end,
      'sender_name', case when m.sender_id = m.driver_id then r.driver_name else r.rider_name end)
    order by m.created_at)
    from public.ride_messages m join public.rides r on r.id = m.ride_id
    where m.ride_id = p_ride_id), '[]'::jsonb);
end $$;
revoke all on function private.ride_chat_transcript(uuid) from public, anon;
grant execute on function private.ride_chat_transcript(uuid) to authenticated;
create function public.admin_ride_chat(p_ride_id uuid)
returns jsonb language sql stable security invoker set search_path = '' as $$
  select private.ride_chat_transcript(p_ride_id)
$$;
revoke all on function public.admin_ride_chat(uuid) from public, anon;
grant execute on function public.admin_ride_chat(uuid) to authenticated;

-- Messages and cancellation records leave with the account.
create function private.profiles_before_delete_cleanup()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  delete from public.ride_messages where sender_id = old.id;
  delete from private.ride_cancellations where user_id = old.id;
  update private.ride_cancellations set reported_user_id = null where reported_user_id = old.id;
  delete from private.cancellation_blocks where user_id = old.id;
  return old;
end $$;
revoke all on function private.profiles_before_delete_cleanup() from public, anon, authenticated;
create trigger profiles_before_delete_cleanup before delete on public.profiles
for each row execute function private.profiles_before_delete_cleanup();

select cron.schedule('purge-ride-messages', '17 9 * * *',
  $$delete from public.ride_messages where created_at < now() - interval '30 days'$$);
