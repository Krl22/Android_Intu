create table private.service_reports (
  id uuid primary key,
  ride_id uuid not null references public.rides(id) on delete cascade,
  reporter_id text references public.profiles(id) on delete set null,
  reported_id text references public.profiles(id) on delete set null,
  reporter_role text not null check (reporter_role in ('rider','driver')),
  category text not null check (category in ('misconduct','harassment','fraud','theft','dangerous_driving',
    'payment_dispute','wrong_vehicle','damaged_package','missing_package','lost_item','other')),
  description text not null check (length(description) between 10 and 2000),
  status text not null default 'open' check (status in ('open','in_review','resolved','dismissed')),
  lost_state text check (lost_state in ('awaiting_check','found','not_found','returned')),
  admin_response text not null default '',
  admin_note text not null default '',
  reviewed_by text references public.profiles(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check ((category='lost_item') = (lost_state is not null))
);
create unique index service_reports_one_active on private.service_reports(ride_id,reporter_id,category)
  where status in ('open','in_review');
create index service_reports_reporter on private.service_reports(reporter_id,created_at desc);
create index service_reports_reported_lost on private.service_reports(reported_id,created_at desc) where category='lost_item';
create index service_reports_admin on private.service_reports(status,created_at desc);
alter table private.service_reports enable row level security;
revoke all on private.service_reports from public,anon,authenticated;

create table private.service_report_messages (
  id uuid primary key default gen_random_uuid(),
  report_id uuid not null references private.service_reports(id) on delete cascade,
  sender_id text references public.profiles(id) on delete set null,
  sender_role text not null check (sender_role in ('rider','driver')),
  body text not null check (length(body) between 1 and 500),
  created_at timestamptz not null default now()
);
create index service_report_messages_report on private.service_report_messages(report_id,created_at);
create index service_report_messages_sender on private.service_report_messages(sender_id,created_at desc);
alter table private.service_report_messages enable row level security;
revoke all on private.service_report_messages from public,anon,authenticated;

create function private.service_report_json(p_report private.service_reports)
returns jsonb language plpgsql stable security definer set search_path='' as $$
begin
  return (select jsonb_build_object('id',p_report.id,'ride_id',p_report.ride_id,'category',p_report.category,
    'description',p_report.description,'status',p_report.status,'lost_state',p_report.lost_state,
    'admin_response',p_report.admin_response,'admin_note',case when public.is_admin() then p_report.admin_note else null end,
    'reporter_role',p_report.reporter_role,'created_at',p_report.created_at,'updated_at',p_report.updated_at,
    'is_reporter',p_report.reporter_id=private.requesting_uid(),
    'can_check_item',p_report.category='lost_item' and p_report.reported_id=private.requesting_uid(),
    'reporter_name',coalesce(nullif(trim(concat_ws(' ',a.first_name,a.last_name)),''),'Cuenta eliminada'),
    'reported_name',coalesce(nullif(trim(concat_ws(' ',b.first_name,b.last_name)),''),'Cuenta eliminada'),
    'origin_address',r.origin_address,'destination_address',r.destination_address,
    'vehicle_plate',r.vehicle_plate,'service_kind',r.service_kind,
    'messages',coalesce((select jsonb_agg(jsonb_build_object('id',m.id,'body',m.body,
      'role',m.sender_role,'mine',m.sender_id=private.requesting_uid(),'created_at',m.created_at) order by m.created_at,m.id)
      from (select * from private.service_report_messages where report_id=p_report.id order by created_at desc,id desc limit 50) m),'[]'::jsonb))
  from public.rides r left join public.profiles a on a.id=p_report.reporter_id
    left join public.profiles b on b.id=p_report.reported_id where r.id=p_report.ride_id);
end
$$;
revoke all on function private.service_report_json(private.service_reports) from public,anon,authenticated;

-- Generic notifications carry no accusation, item description or personal contact details.
-- Uses the existing deployed ridePush endpoint; durable reports remain available if FCM fails.
create function private.service_report_push(p_uid text,p_report_id uuid,p_title text,p_body text,p_admin boolean default false)
returns void language plpgsql security definer set search_path='' as $$
declare v_secret text; v_tokens text[];
begin
  select array_agg(token) into v_tokens from public.device_tokens where user_id=p_uid and platform='android';
  select decrypted_secret into v_secret from vault.decrypted_secrets where name='push_webhook_secret';
  if v_tokens is null or v_secret is null then return; end if;
  perform net.http_post(url:='https://us-central1-intu-e8403.cloudfunctions.net/ridePush',
    headers:=jsonb_build_object('Content-Type','application/json','X-Intu-Secret',v_secret),
    body:=jsonb_build_object('tokens',to_jsonb(v_tokens),'title',p_title,'body',p_body,
      'rideId','report-'||p_report_id::text,'status',case when p_admin then 'admin_service_report' else 'service_report' end),
    timeout_milliseconds:=8000);
exception when others then raise warning 'service_report_push failed';
end $$;
revoke all on function private.service_report_push(text,uuid,text,text,boolean) from public,anon,authenticated;

create function private.create_service_report(p_request_id uuid,p_ride_id uuid,p_category text,p_description text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare v_uid text:=private.requesting_uid(); v_ride public.rides; v_report private.service_reports; v_admin record;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode='28000'; end if;
  if p_request_id is null or p_ride_id is null or p_category is null or p_category not in
    ('misconduct','harassment','fraud','theft','dangerous_driving','payment_dispute','wrong_vehicle',
     'damaged_package','missing_package','lost_item','other') or p_description is null
    or length(trim(p_description)) not between 10 and 2000 then
    raise exception 'invalid_service_report' using errcode='22023'; end if;
  perform 1 from public.profiles where id=v_uid for update;
  select * into v_report from private.service_reports where id=p_request_id;
  if found then
    if v_report.reporter_id=v_uid and v_report.ride_id=p_ride_id and v_report.category=p_category
      and v_report.description=trim(p_description) then return private.service_report_json(v_report); end if;
    raise exception 'report_request_conflict' using errcode='22023';
  end if;
  select * into v_ride from public.rides where id=p_ride_id for share;
  if not found or v_uid not in (v_ride.rider_id,v_ride.driver_id) or v_ride.driver_id is null
    or v_ride.rider_id is null or v_ride.rider_id=v_ride.driver_id then
    raise exception 'not_participant' using errcode='42501'; end if;
  if p_category in ('dangerous_driving','wrong_vehicle','lost_item') and v_uid<>v_ride.rider_id then
    raise exception 'invalid_service_report' using errcode='22023'; end if;
  if p_category in ('damaged_package','missing_package') and v_ride.service_kind<>'delivery' then
    raise exception 'invalid_service_report' using errcode='22023'; end if;
  if p_category='lost_item' and (v_ride.service_kind<>'passenger' or v_ride.status not in ('completed','cancelled')
    or (v_ride.status='cancelled' and v_ride.started_at is null)) then
    raise exception 'lost_item_after_trip' using errcode='22023'; end if;
  if exists(select 1 from private.service_reports where ride_id=p_ride_id and reporter_id=v_uid
    and category=p_category and status in ('open','in_review')) then
    raise exception 'service_report_exists' using errcode='23505'; end if;
  if (select count(*) from private.service_reports where reporter_id=v_uid and created_at>now()-interval '1 day')>=10 then
    raise exception 'too_many_reports' using errcode='54000'; end if;
  insert into private.service_reports(id,ride_id,reporter_id,reported_id,reporter_role,category,description,lost_state)
    values(p_request_id,p_ride_id,v_uid,case when v_uid=v_ride.rider_id then v_ride.driver_id else v_ride.rider_id end,
      case when v_uid=v_ride.rider_id then 'rider' else 'driver' end,p_category,trim(p_description),
      case when p_category='lost_item' then 'awaiting_check' end) returning * into v_report;
  if p_category='lost_item' then
    perform private.service_report_push(v_report.reported_id,v_report.id,'Revisa tu vehículo',
      'Un pasajero reportó un objeto perdido. Revisa el vehículo y responde desde Viajes → Reportes y objetos perdidos.');
  end if;
  for v_admin in select * from private.admin_activity_recipients('bug_report') loop
    perform private.service_report_push(v_admin.user_id,v_report.id,'Nuevo reporte de servicio',
      'Hay un nuevo caso para revisar en Admin → Reportes.',true);
  end loop;
  return private.service_report_json(v_report);
end $$;
create function public.create_service_report(p_request_id uuid,p_ride_id uuid,p_category text,p_description text)
returns jsonb language sql security invoker set search_path='' as $$
  select private.create_service_report(p_request_id,p_ride_id,p_category,p_description)
$$;

create function private.list_service_reports(p_ride_id uuid default null,p_admin boolean default false)
returns jsonb language plpgsql stable security definer set search_path='' as $$
declare v_uid text:=private.requesting_uid();
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode='28000'; end if;
  if p_admin and not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  return coalesce((select jsonb_agg(private.service_report_json(r::private.service_reports) order by
    (r.status in ('open','in_review')) desc,r.updated_at desc,r.id)
    from (select * from private.service_reports where (p_ride_id is null or ride_id=p_ride_id) and
      (p_admin or reporter_id=v_uid or (category='lost_item' and reported_id=v_uid))
      order by (status in ('open','in_review')) desc,updated_at desc,id limit 200) r),'[]'::jsonb);
end $$;
create function public.my_service_reports(p_ride_id uuid default null)
returns jsonb language sql security invoker set search_path='' as $$ select private.list_service_reports(p_ride_id,false) $$;
create function public.admin_service_reports()
returns jsonb language sql security invoker set search_path='' as $$ select private.list_service_reports(null,true) $$;

create function private.respond_lost_item(p_report_id uuid,p_state text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare v_uid text:=private.requesting_uid(); v_report private.service_reports;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode='28000'; end if;
  select * into v_report from private.service_reports where id=p_report_id for update;
  if not found or v_report.category<>'lost_item' or
    (v_uid is distinct from v_report.reporter_id and v_uid is distinct from v_report.reported_id) then
    raise exception 'not_participant' using errcode='42501'; end if;
  if v_report.status not in ('open','in_review') then raise exception 'service_report_closed' using errcode='22023'; end if;
  if p_state is null or not ((v_uid=v_report.reported_id and p_state in ('found','not_found')
    and v_report.lost_state in ('awaiting_check','not_found')) or
    (v_uid=v_report.reporter_id and p_state='returned' and v_report.lost_state='found')) then
    raise exception 'invalid_lost_item_state' using errcode='22023'; end if;
  if v_report.lost_state=p_state then return private.service_report_json(v_report); end if;
  update private.service_reports set lost_state=p_state,updated_at=now(),
    status=case when p_state='returned' then 'resolved' else status end where id=p_report_id returning * into v_report;
  perform private.service_report_push(case when v_uid=v_report.reporter_id then v_report.reported_id else v_report.reporter_id end,
    v_report.id,'Actualización de objeto perdido','Revisa la respuesta del caso en Viajes → Reportes y objetos perdidos.');
  return private.service_report_json(v_report);
end $$;
create function public.respond_lost_item(p_report_id uuid,p_state text)
returns jsonb language sql security invoker set search_path='' as $$ select private.respond_lost_item(p_report_id,p_state) $$;

create function private.send_service_report_message(p_report_id uuid,p_body text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare v_uid text:=private.requesting_uid(); v_report private.service_reports;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode='28000'; end if;
  if p_body is null or length(trim(p_body)) not between 1 and 500 then raise exception 'invalid_message' using errcode='22023'; end if;
  perform 1 from public.profiles where id=v_uid for update;
  select * into v_report from private.service_reports where id=p_report_id for update;
  if not found or v_report.category<>'lost_item' or
    (v_uid is distinct from v_report.reporter_id and v_uid is distinct from v_report.reported_id) then
    raise exception 'not_participant' using errcode='42501'; end if;
  if v_report.status not in ('open','in_review') then raise exception 'service_report_closed' using errcode='22023'; end if;
  if (select count(*) from private.service_report_messages where sender_id=v_uid and created_at>now()-interval '1 minute')>=5 then
    raise exception 'too_many_messages' using errcode='54000'; end if;
  insert into private.service_report_messages(report_id,sender_id,sender_role,body)
    values(p_report_id,v_uid,case when v_uid=v_report.reporter_id then 'rider' else 'driver' end,trim(p_body));
  update private.service_reports set updated_at=now() where id=p_report_id returning * into v_report;
  perform private.service_report_push(case when v_uid=v_report.reporter_id then v_report.reported_id else v_report.reporter_id end,
    v_report.id,'Mensaje sobre tu objeto perdido','Tienes un mensaje para coordinar la devolución. Revísalo en Viajes → Reportes y objetos perdidos.');
  return private.service_report_json(v_report);
end $$;
create function public.send_service_report_message(p_report_id uuid,p_body text)
returns jsonb language sql security invoker set search_path='' as $$ select private.send_service_report_message(p_report_id,p_body) $$;

create function private.review_service_report(p_report_id uuid,p_status text,p_response text,p_note text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare v_report private.service_reports;
begin
  if private.requesting_uid() is null or not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  if p_status is null or p_status not in ('open','in_review','resolved','dismissed') or p_response is null or p_note is null
    or length(p_response)>2000 or length(p_note)>2000 then raise exception 'invalid_service_report' using errcode='22023'; end if;
  update private.service_reports set status=p_status,admin_response=trim(p_response),admin_note=trim(p_note),
    reviewed_by=private.requesting_uid(),updated_at=now() where id=p_report_id returning * into v_report;
  if not found then raise exception 'report_not_found' using errcode='P0002'; end if;
  perform private.service_report_push(v_report.reporter_id,v_report.id,'Intu actualizó tu reporte',
    'El equipo revisó tu caso. Consulta el estado y la respuesta en Viajes → Reportes y objetos perdidos.');
  return private.service_report_json(v_report);
end $$;
create function public.admin_review_service_report(p_report_id uuid,p_status text,p_response text,p_note text)
returns jsonb language sql security invoker set search_path='' as $$ select private.review_service_report(p_report_id,p_status,p_response,p_note) $$;

revoke all on function private.create_service_report(uuid,uuid,text,text),public.create_service_report(uuid,uuid,text,text),
  private.list_service_reports(uuid,boolean),public.my_service_reports(uuid),public.admin_service_reports(),
  private.respond_lost_item(uuid,text),public.respond_lost_item(uuid,text),
  private.send_service_report_message(uuid,text),public.send_service_report_message(uuid,text),
  private.review_service_report(uuid,text,text,text),public.admin_review_service_report(uuid,text,text,text) from public,anon;
grant execute on function private.create_service_report(uuid,uuid,text,text),public.create_service_report(uuid,uuid,text,text),
  private.list_service_reports(uuid,boolean),public.my_service_reports(uuid),public.admin_service_reports(),
  private.respond_lost_item(uuid,text),public.respond_lost_item(uuid,text),
  private.send_service_report_message(uuid,text),public.send_service_report_message(uuid,text),
  private.review_service_report(uuid,text,text,text),public.admin_review_service_report(uuid,text,text,text) to authenticated;
