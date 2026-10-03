-- Run as database owner. Everything, including queued pg_net requests, rolls back.
begin;
do $$
declare
  v_admin text := 'qa-admin-push-' || gen_random_uuid()::text;
  v_user text := 'qa-user-push-' || gen_random_uuid()::text;
  v_extra text := 'qa-extra-push-' || gen_random_uuid()::text;
  v_ride uuid; v_report uuid; v_count integer; v_settings jsonb;
begin
  insert into public.profiles(id,first_name,last_name) values (v_admin,'QA','Admin');
  insert into private.admins(user_id) values(v_admin);
  insert into public.device_tokens(token,user_id) values('qa-token-' || v_admin,v_admin);
  insert into public.profiles(id,first_name,last_name) values(v_user,'QA','Tester');
  insert into public.device_tokens(token,user_id) values('qa-token-' || v_user,v_user);

  if exists(select 1 from private.admin_activity_recipients('new_user') where user_id=v_user) then
    raise exception 'non_admin_recipient';
  end if;
  if not exists(select 1 from private.admin_activity_recipients('new_user') where user_id=v_admin) then
    raise exception 'missing_default_recipient';
  end if;
  if exists(select 1 from private.admin_activity_recipients('unknown')) then raise exception 'unknown_event'; end if;

  perform set_config('request.jwt.claims',jsonb_build_object('sub',v_user,'role','authenticated')::text,true);
  begin
    perform public.admin_get_notification_preferences(); raise exception 'non_admin_get_allowed';
  exception when insufficient_privilege then null; end;
  begin
    perform public.admin_set_notification_preferences(true,true,true,true,true); raise exception 'non_admin_set_allowed';
  exception when insufficient_privilege then null; end;
  perform set_config('request.jwt.claims','{}',true);
  begin
    perform public.admin_get_notification_preferences(); raise exception 'anonymous_allowed';
  exception when insufficient_privilege then null; end;
  perform set_config('request.jwt.claims',jsonb_build_object('sub',v_admin,'role','authenticated')::text,true);
  v_settings := public.admin_get_notification_preferences();
  if v_settings <> '{"enabled":true,"new_users":true,"ride_requests":true,"driver_applications":true,"bug_reports":true}'::jsonb then
    raise exception 'wrong_defaults';
  end if;
  perform public.admin_set_notification_preferences(true,true,false,true,true);
  if exists(select 1 from private.admin_activity_recipients('ride_request') where user_id=v_admin) then raise exception 'individual_mute_ignored'; end if;
  if not exists(select 1 from private.admin_activity_recipients('bug_report') where user_id=v_admin) then raise exception 'other_event_muted'; end if;
  perform public.admin_set_notification_preferences(false,true,false,true,true);
  if exists(select 1 from private.admin_activity_recipients('new_user') where user_id=v_admin) then raise exception 'master_mute_ignored'; end if;
  perform public.admin_set_notification_preferences(true,true,false,true,true);
  if (public.admin_get_notification_preferences()->>'ride_requests')::boolean then raise exception 'choice_not_preserved'; end if;
  perform public.admin_set_notification_preferences(true,true,true,true,true);
  begin
    perform public.admin_set_notification_preferences(null,true,true,true,true); raise exception 'null_allowed';
  exception when invalid_parameter_value then null; end;

  insert into public.profiles(id,first_name,last_name) values(v_extra,'QA','New');
  update public.profiles set first_name='QA edited' where id=v_extra;
  insert into public.drivers(id,document_type,document_number,license_number)
    values(v_user,'ce','QA-' || v_user,'QA-LICENSE');
  update public.drivers set license_number='QA-EDITED' where id=v_user;
  insert into public.rides(rider_id,vehicle_type,origin_lat,origin_lng,origin_address,
    destination_lat,destination_lng,destination_address,distance_meters,duration_seconds,estimated_fare,rider_name)
    values(v_user,'mototaxi',-11.25,-74.63,'QA recojo',-11.26,-74.64,'QA destino',1000,300,4,'QA Tester') returning id into v_ride;
  -- Rider updates (e.g. corrected display name) must not produce another request alert.
  update public.rides set rider_name='QA renamed' where id=v_ride;
  perform set_config('request.jwt.claims',jsonb_build_object('sub',v_extra,'role','authenticated')::text,true);
  perform public.create_delivery_request(-11.252,-74.637,'QA Origen',-11.25,-74.63,'QA Destino',2100,420,null,'efectivo',
    '{"recipient_name":"Persona QA","recipient_phone":"+51987654321","description":"Sobre QA","payer":"sender","small_package_confirmed":true}');
  insert into public.bug_reports(user_id,title,description,app_version,device_info)
    values(v_user,'QA notification test','Reporte ficticio con rollback, no enviar.','1.26','QA') returning id into v_report;
  update public.bug_reports set status='resolved' where id=v_report;

  select count(*) into v_count from net.http_request_queue
    where convert_from(body,'UTF8')::jsonb @> jsonb_build_object('kind','admin_activity','recipientUid',v_admin);
  if v_count <> 6 then raise exception 'expected_6_events_got_%',v_count; end if;
  -- Two new profiles, driver application, passenger request, delivery request, bug report.
  if not exists(select 1 from net.http_request_queue
      where convert_from(body,'UTF8')::jsonb @> jsonb_build_object('recipientUid',v_admin,'title','Nueva solicitud de envío')) then
    raise exception 'missing_delivery_copy';
  end if;
  delete from private.admins where user_id=v_admin;
  if exists(select 1 from private.admin_activity_recipients('new_user') where user_id=v_admin) then raise exception 'revoked_admin_receives'; end if;
  if exists(select 1 from private.admin_notification_preferences where user_id=v_admin) then raise exception 'preferences_not_cascaded'; end if;
end $$;
select 'PASS: permissions, account preferences, per-event mute, 6 queued events, no update duplicates, delivery, revocation' as result;
rollback;
