-- Run as postgres against the development project. All fixtures roll back.
begin;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-chat-20261005-%') then raise exception 'Fixture collision'; end if;
  if has_function_privilege('anon','public.send_ride_message(uuid,text,text)','execute')
     or has_function_privilege('anon','public.admin_ride_chat(uuid)','execute')
     or has_table_privilege('anon','public.ride_messages','select') then raise exception 'Anonymous access'; end if;
  if has_table_privilege('authenticated','public.ride_messages','insert')
     or has_table_privilege('authenticated','public.ride_messages','update')
     or has_table_privilege('authenticated','public.ride_messages','delete') then raise exception 'Direct write access'; end if;
  if not exists(select 1 from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'ride_messages') then raise exception 'Realtime missing'; end if;
  if not exists(select 1 from cron.job where jobname = 'purge-ride-messages') then raise exception 'Retention job missing'; end if;
end $$;
insert into public.profiles(id,first_name,last_name)
select 'intu-qa-chat-20261005-' || name, 'QA', name from unnest(array['admin','rider','driver','driver2','other']) name;
insert into private.admins(user_id) values ('intu-qa-chat-20261005-admin');
insert into public.drivers(id,document_type,document_number,license_number,status)
values ('intu-qa-chat-20261005-driver','dni','99052001','QA','approved'),
       ('intu-qa-chat-20261005-driver2','dni','99052002','QA','approved');
insert into public.vehicles(driver_id,vehicle_type,brand,model,year,plate)
values ('intu-qa-chat-20261005-driver','mototaxi','QA','QA',2024,'QACHT01'),
       ('intu-qa-chat-20261005-driver2','mototaxi','QA','QA',2024,'QACHT02');
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-chat-20261005-rider","role":"authenticated"}',true);
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id
) select set_config('intu.qa_chat',id::text,true) from created;
do $$ begin
  begin perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, 'Hola'); raise exception 'Chat before a driver';
  exception when no_data_found then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-chat-20261005-driver","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_chat')::uuid);
do $$ declare m public.ride_messages; begin
  begin perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, 'Escribiendo al manejar'); raise exception 'Driver typed while driving';
  exception when raise_exception then if sqlerrm <> 'driver_quick_replies_only' then raise; end if; end;
  begin perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, null, 'coming_out'); raise exception 'Rider reply used by driver';
  exception when invalid_parameter_value then null; end;
  m := public.send_ride_message(current_setting('intu.qa_chat')::uuid, 'ignorado', 'few_minutes');
  if m.body <> 'Llego en unos minutos.' or m.quick_reply <> 'few_minutes' then raise exception 'Quick reply text: %', m.body; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-chat-20261005-rider","role":"authenticated"}',true);
do $$ declare m public.ride_messages; begin
  m := public.send_ride_message(current_setting('intu.qa_chat')::uuid, '  Estoy en la puerta azul  ');
  if m.body <> 'Estoy en la puerta azul' or m.sender_id <> 'intu-qa-chat-20261005-rider' then raise exception 'Rider message stored wrong'; end if;
  begin perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, '   '); raise exception 'Blank message';
  exception when invalid_parameter_value then null; end;
  begin perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, repeat('a', 501)); raise exception 'Long message';
  exception when invalid_parameter_value then null; end;
  if (select count(*) from public.ride_messages where ride_id = current_setting('intu.qa_chat')::uuid) <> 2 then raise exception 'Rider cannot read chat'; end if;
  for i in 1..11 loop perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, 'Mensaje ' || i); end loop;
  begin perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, 'Uno más'); raise exception 'Flood allowed';
  exception when raise_exception then if sqlerrm <> 'too_many_messages' then raise; end if; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-chat-20261005-other","role":"authenticated"}',true);
do $$ begin
  if exists(select 1 from public.ride_messages where ride_id = current_setting('intu.qa_chat')::uuid) then raise exception 'Outsider read chat'; end if;
  begin perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, 'Hola'); raise exception 'Outsider wrote';
  exception when no_data_found then null; end;
  begin perform public.admin_ride_chat(current_setting('intu.qa_chat')::uuid); raise exception 'Non-admin read transcript';
  exception when insufficient_privilege then null; end;
end $$;
-- Stopped at the pickup the driver can type
select set_config('request.jwt.claims','{"sub":"intu-qa-chat-20261005-driver","role":"authenticated"}',true);
select public.advance_ride(current_setting('intu.qa_chat')::uuid, 'arrived');
do $$ begin
  perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, 'Estoy frente a la tienda');
  if (select count(*) from public.ride_messages where ride_id = current_setting('intu.qa_chat')::uuid) <> 14 then raise exception 'Driver cannot read chat'; end if;
end $$;
-- A released ride hides that conversation from both drivers
select public.cancel_ride(current_setting('intu.qa_chat')::uuid, 'cancelled_by_driver');
do $$ begin
  if exists(select 1 from public.ride_messages where ride_id = current_setting('intu.qa_chat')::uuid) then raise exception 'Released driver still reads chat'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-chat-20261005-driver2","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_chat')::uuid);
do $$ begin
  if exists(select 1 from public.ride_messages where ride_id = current_setting('intu.qa_chat')::uuid) then raise exception 'New driver reads previous chat'; end if;
  perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, null, 'on_my_way');
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-chat-20261005-rider","role":"authenticated"}',true);
do $$ begin
  if (select count(*) from public.ride_messages where ride_id = current_setting('intu.qa_chat')::uuid) <> 1 then raise exception 'Rider sees old conversation'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-chat-20261005-driver2","role":"authenticated"}',true);
select public.advance_ride(current_setting('intu.qa_chat')::uuid, 'arrived');
select public.advance_ride(current_setting('intu.qa_chat')::uuid, 'in_progress');
select public.advance_ride(current_setting('intu.qa_chat')::uuid, 'completed');
do $$ begin
  begin perform public.send_ride_message(current_setting('intu.qa_chat')::uuid, null, 'ok'); raise exception 'Chat open after completion';
  exception when raise_exception then if sqlerrm <> 'chat_closed' then raise; end if; end;
  if (select count(*) from public.ride_messages where ride_id = current_setting('intu.qa_chat')::uuid) <> 1 then raise exception 'History lost after completion'; end if;
end $$;
-- Admin sees every assignment of the ride
select set_config('request.jwt.claims','{"sub":"intu-qa-chat-20261005-admin","role":"authenticated"}',true);
do $$ declare t jsonb; begin
  t := public.admin_ride_chat(current_setting('intu.qa_chat')::uuid);
  if jsonb_array_length(t) <> 15 then raise exception 'Transcript size %', jsonb_array_length(t); end if;
  if not exists(select 1 from jsonb_array_elements(public.admin_recent_ride_chats(30)) e
                where e->>'ride_id' = current_setting('intu.qa_chat') and (e->>'messages')::integer = 15) then raise exception 'Recent chats missing ride'; end if;
end $$;
reset role;
rollback;
select 'Ride chat participants, quick replies, driver typing guard, reassignment privacy and admin review: PASS' as verification;
