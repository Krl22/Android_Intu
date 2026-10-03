-- Account-wide admin preferences. No device can subscribe itself to admin events.
create table private.admin_notification_preferences (
  user_id text primary key references private.admins(user_id) on delete cascade,
  enabled boolean not null default true,
  new_users boolean not null default true,
  ride_requests boolean not null default true,
  driver_applications boolean not null default true,
  bug_reports boolean not null default true
);
alter table private.admin_notification_preferences enable row level security;
revoke all on private.admin_notification_preferences from public, anon, authenticated;

create function public.admin_get_notification_preferences()
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_result jsonb;
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  select to_jsonb(p) - 'user_id' into v_result
    from private.admin_notification_preferences p where p.user_id = private.requesting_uid();
  return coalesce(v_result, jsonb_build_object('enabled',true,'new_users',true,
    'ride_requests',true,'driver_applications',true,'bug_reports',true));
end $$;
revoke all on function public.admin_get_notification_preferences() from public, anon;
grant execute on function public.admin_get_notification_preferences() to authenticated;

create function public.admin_set_notification_preferences(p_enabled boolean, p_new_users boolean,
  p_ride_requests boolean, p_driver_applications boolean, p_bug_reports boolean)
returns jsonb language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  if p_enabled is null or p_new_users is null or p_ride_requests is null
    or p_driver_applications is null or p_bug_reports is null then
    raise exception 'invalid_preferences' using errcode = '22023';
  end if;
  insert into private.admin_notification_preferences as p
    (user_id,enabled,new_users,ride_requests,driver_applications,bug_reports)
  values (private.requesting_uid(),p_enabled,p_new_users,p_ride_requests,p_driver_applications,p_bug_reports)
  on conflict (user_id) do update set enabled=excluded.enabled,new_users=excluded.new_users,
    ride_requests=excluded.ride_requests,driver_applications=excluded.driver_applications,
    bug_reports=excluded.bug_reports;
  return public.admin_get_notification_preferences();
end $$;
revoke all on function public.admin_set_notification_preferences(boolean,boolean,boolean,boolean,boolean) from public, anon;
grant execute on function public.admin_set_notification_preferences(boolean,boolean,boolean,boolean,boolean) to authenticated;

-- Shared, testable recipient filter. Rechecks admin membership for every event.
create function private.admin_activity_recipients(p_type text)
returns table(user_id text, tokens text[]) language sql stable security definer set search_path = '' as $$
  select a.user_id, array_agg(d.token order by d.token)
  from private.admins a
  left join private.admin_notification_preferences p on p.user_id=a.user_id
  join public.device_tokens d on d.user_id=a.user_id and d.platform='android'
  where coalesce(p.enabled,true) and case p_type
    when 'new_user' then coalesce(p.new_users,true)
    when 'ride_request' then coalesce(p.ride_requests,true)
    when 'driver_application' then coalesce(p.driver_applications,true)
    when 'bug_report' then coalesce(p.bug_reports,true)
    else false end
  group by a.user_id
$$;
revoke all on function private.admin_activity_recipients(text) from public,anon,authenticated;

create function private.notify_admin_activity()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
  v_type text; v_title text; v_body text; v_id text; v_name text;
  v_secret text; v_recipient record;
begin
  if tg_table_name='profiles' then
    v_type := 'new_user'; v_id := new.id;
    v_title := 'Nuevo usuario en Intu';
    v_body := coalesce(nullif(trim(concat_ws(' ',new.first_name,new.last_name)),''),'Un usuario') || ' se registró en Intu.';
  elsif tg_table_name='rides' then
    if new.status <> 'searching' then return null; end if;
    v_type := 'ride_request'; v_id := new.id::text;
    v_title := case when new.service_kind='delivery' then 'Nueva solicitud de envío' else 'Nueva solicitud de viaje' end;
    v_body := coalesce(nullif(new.rider_name,''),'Un usuario') ||
      case when new.service_kind='delivery' then ' pidió un envío.' else ' pidió una moto.' end;
  elsif tg_table_name='drivers' then
    if new.status <> 'pending' then return null; end if;
    if tg_op='UPDATE' then
      if old.status is not distinct from new.status then return null; end if;
    end if;
    v_type := 'driver_application'; v_id := new.id || ':' || new.updated_at::text;
    v_title := 'Nueva postulación de conductor';
    select nullif(trim(concat_ws(' ',p.first_name,p.last_name)),'') into v_name from public.profiles p where p.id=new.id;
    v_body := coalesce(v_name,'Un usuario') || ' envió su postulación. Revisa sus datos en Administración.';
  elsif tg_table_name='bug_reports' then
    v_type := 'bug_report'; v_id := new.id::text;
    v_title := 'Nuevo reporte de error'; v_body := left(new.title,160);
  else return null;
  end if;
  select decrypted_secret into v_secret from vault.decrypted_secrets where name='push_webhook_secret';
  if v_secret is null then return null; end if;
  for v_recipient in select * from private.admin_activity_recipients(v_type) loop
    perform net.http_post(
      url := 'https://us-central1-intu-e8403.cloudfunctions.net/ridePush',
      headers := jsonb_build_object('Content-Type','application/json','X-Intu-Secret',v_secret),
      body := jsonb_build_object('kind','admin_activity','eventType',v_type,'eventId',v_type || ':' || v_id,
        'recipientUid',v_recipient.user_id,'tokens',to_jsonb(v_recipient.tokens),'title',v_title,'body',left(v_body,240)),
      timeout_milliseconds := 8000);
  end loop;
  return null;
exception when others then
  -- Notifications never prevent a registration/request/report from being saved.
  raise warning 'notify_admin_activity: %', sqlerrm;
  return null;
end $$;
revoke all on function private.notify_admin_activity() from public,anon,authenticated;

create trigger profiles_notify_admin after insert on public.profiles for each row execute function private.notify_admin_activity();
create trigger rides_notify_admin after insert on public.rides for each row execute function private.notify_admin_activity();
create trigger drivers_notify_admin after insert or update of status on public.drivers for each row execute function private.notify_admin_activity();
create trigger bug_reports_notify_admin after insert on public.bug_reports for each row execute function private.notify_admin_activity();
