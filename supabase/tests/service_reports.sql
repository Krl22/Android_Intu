begin;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-reports-%') then raise exception 'Fixture collision'; end if;
  if has_table_privilege('authenticated','private.service_reports','select')
    or has_table_privilege('authenticated','private.service_reports','insert')
    or has_table_privilege('authenticated','private.service_report_messages','select')
    or has_table_privilege('authenticated','private.service_report_messages','insert') then raise exception 'Direct access'; end if;
  if has_function_privilege('anon','public.create_service_report(uuid,uuid,text,text)','execute')
    or has_function_privilege('anon','public.my_service_reports(uuid)','execute')
    or has_function_privilege('anon','public.admin_service_reports()','execute')
    or has_function_privilege('anon','public.respond_lost_item(uuid,text)','execute')
    or has_function_privilege('anon','public.send_service_report_message(uuid,text)','execute')
    or has_function_privilege('anon','public.admin_review_service_report(uuid,text,text,text)','execute')
    or has_function_privilege('authenticated','private.service_report_json(private.service_reports)','execute')
    or has_function_privilege('authenticated','private.service_report_push(text,uuid,text,text,boolean)','execute') then raise exception 'Unsafe grants'; end if;
end $$;
insert into public.profiles(id,first_name,last_name)
select 'intu-qa-reports-'||n,'QA',n from unnest(array['rider','driver','other','admin']) n;
insert into private.admins(user_id) values('intu-qa-reports-admin');
insert into public.drivers(id,document_type,document_number,license_number,status)
values('intu-qa-reports-driver','dni','99081009','QA','approved');
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-reports-rider","role":"authenticated"}',true);
with r as (insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values('mototaxi',-11.25,-74.63,'QA recojo',-11.24,-74.62,'QA destino',2000,400) returning id)
select set_config('intu.qa_report_ride',id::text,true) from r;
select set_config('intu.qa_lost_report',gen_random_uuid()::text,true);
select set_config('intu.qa_theft_report',gen_random_uuid()::text,true);
do $$ begin
  begin perform public.create_service_report(gen_random_uuid(),current_setting('intu.qa_report_ride')::uuid,'misconduct','Mala conducta registrada.');
    raise exception 'Unassigned ride report'; exception when insufficient_privilege then null; end;
end $$;
reset role;
update public.rides set driver_id='intu-qa-reports-driver',status='accepted' where id=current_setting('intu.qa_report_ride')::uuid;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-reports-rider","role":"authenticated"}',true);
do $$ begin
  begin perform public.create_service_report(gen_random_uuid(),current_setting('intu.qa_report_ride')::uuid,'lost_item','Un bolso rojo en el asiento.');
    raise exception 'Lost item before trip ended'; exception when invalid_parameter_value then null; end;
  begin perform public.create_service_report(gen_random_uuid(),current_setting('intu.qa_report_ride')::uuid,'damaged_package','Paquete llegó dañado.');
    raise exception 'Delivery reason on passenger ride'; exception when invalid_parameter_value then null; end;
  begin perform public.create_service_report(gen_random_uuid(),current_setting('intu.qa_report_ride')::uuid,'theft','corto');
    raise exception 'Short description'; exception when invalid_parameter_value then null; end;
  perform public.create_service_report(current_setting('intu.qa_theft_report')::uuid,current_setting('intu.qa_report_ride')::uuid,'theft','Descripción privada del incidente.');
end $$;
reset role;
update public.rides set status='completed',started_at=now(),completed_at=now() where id=current_setting('intu.qa_report_ride')::uuid;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-reports-rider","role":"authenticated"}',true);
do $$ declare r jsonb; begin
  r:=public.create_service_report(current_setting('intu.qa_lost_report')::uuid,current_setting('intu.qa_report_ride')::uuid,'lost_item','Un bolso rojo en el asiento.');
  if r->>'lost_state'<>'awaiting_check' or r->>'reporter_role'<>'rider' then raise exception 'Lost item initial state'; end if;
  perform public.create_service_report(current_setting('intu.qa_lost_report')::uuid,current_setting('intu.qa_report_ride')::uuid,'lost_item','Un bolso rojo en el asiento.');
  if jsonb_array_length(public.my_service_reports())<>2 then raise exception 'Retry created duplicate'; end if;
  begin perform public.create_service_report(gen_random_uuid(),current_setting('intu.qa_report_ride')::uuid,'lost_item','Otro texto para el mismo caso.');
    raise exception 'Duplicate active report'; exception when unique_violation then null; end;
  begin perform public.respond_lost_item(current_setting('intu.qa_lost_report')::uuid,'found');
    raise exception 'Rider marked found'; exception when invalid_parameter_value then null; end;
  begin perform public.respond_lost_item(current_setting('intu.qa_lost_report')::uuid,'returned');
    raise exception 'Return before found'; exception when invalid_parameter_value then null; end;
  perform public.send_service_report_message(current_setting('intu.qa_lost_report')::uuid,'El bolso tiene una cinta azul.');
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-reports-driver","role":"authenticated"}',true);
do $$ declare r jsonb; begin
  if jsonb_array_length(public.my_service_reports())<>1 then raise exception 'Driver sees private allegation'; end if;
  begin perform public.send_service_report_message(current_setting('intu.qa_theft_report')::uuid,'Acceso a denuncia');
    raise exception 'Driver accesses allegation'; exception when insufficient_privilege then null; end;
  begin perform public.create_service_report(gen_random_uuid(),current_setting('intu.qa_report_ride')::uuid,'lost_item','Objeto perdido del conductor.');
    raise exception 'Driver lost item'; exception when invalid_parameter_value then null; end;
  perform public.create_service_report(gen_random_uuid(),current_setting('intu.qa_report_ride')::uuid,'payment_dispute','El cliente no pagó lo acordado.');
  r:=public.respond_lost_item(current_setting('intu.qa_lost_report')::uuid,'not_found');
  if r->>'lost_state'<>'not_found' then raise exception 'Search result not saved'; end if;
  r:=public.respond_lost_item(current_setting('intu.qa_lost_report')::uuid,'found');
  if r->>'lost_state'<>'found' then raise exception 'Later find denied'; end if;
  begin perform public.respond_lost_item(current_setting('intu.qa_lost_report')::uuid,'returned');
    raise exception 'Driver confirms return'; exception when invalid_parameter_value then null; end;
  perform public.send_service_report_message(current_setting('intu.qa_lost_report')::uuid,'Lo encontré, coordinemos por aquí.');
  for i in 1..4 loop
    perform public.send_service_report_message(current_setting('intu.qa_lost_report')::uuid,'Mensaje QA '||i);
  end loop;
  begin perform public.send_service_report_message(current_setting('intu.qa_lost_report')::uuid,'Sexto mensaje');
    raise exception 'Message flood allowed'; exception when program_limit_exceeded then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-reports-other","role":"authenticated"}',true);
do $$ begin
  if jsonb_array_length(public.my_service_reports())<>0 then raise exception 'Outsider reads report'; end if;
  begin perform public.respond_lost_item(current_setting('intu.qa_lost_report')::uuid,'found');
    raise exception 'Outsider changes lost item'; exception when insufficient_privilege then null; end;
  begin perform public.send_service_report_message(current_setting('intu.qa_lost_report')::uuid,'Mensaje ajeno');
    raise exception 'Outsider writes'; exception when insufficient_privilege then null; end;
  begin perform public.create_service_report(gen_random_uuid(),current_setting('intu.qa_report_ride')::uuid,'theft','Usuario que no participó.');
    raise exception 'Outsider reports'; exception when insufficient_privilege then null; end;
  begin perform public.admin_service_reports(); raise exception 'User admin list'; exception when insufficient_privilege then null; end;
  begin perform private.list_service_reports(null,true); raise exception 'Private wrapper bypass'; exception when insufficient_privilege then null; end;
  begin perform public.admin_review_service_report(current_setting('intu.qa_lost_report')::uuid,'resolved','','');
    raise exception 'User reviews'; exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-reports-admin","role":"authenticated"}',true);
do $$ begin
  if not exists(select 1 from jsonb_array_elements(public.admin_service_reports()) r where r->>'id'=current_setting('intu.qa_theft_report')) then raise exception 'Admin cannot review'; end if;
  perform public.admin_review_service_report(current_setting('intu.qa_theft_report')::uuid,'in_review','Estamos revisando el caso.','Nota interna confidencial.');
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-reports-rider","role":"authenticated"}',true);
do $$ declare r jsonb; begin
  select x into r from jsonb_array_elements(public.my_service_reports()) x where x->>'id'=current_setting('intu.qa_theft_report');
  if r->>'admin_response'<>'Estamos revisando el caso.' or r->>'admin_note' is not null then raise exception 'Private admin note leaked'; end if;
  perform public.respond_lost_item(current_setting('intu.qa_lost_report')::uuid,'returned');
  begin perform public.send_service_report_message(current_setting('intu.qa_lost_report')::uuid,'Mensaje tras devolución');
    raise exception 'Closed case writable'; exception when invalid_parameter_value then null; end;
end $$;
-- NULL participant identifiers cannot turn a permission check into SQL UNKNOWN and grant access.
reset role;
update private.service_reports set reported_id=null,status='open',lost_state='awaiting_check' where id=current_setting('intu.qa_lost_report')::uuid;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-reports-other","role":"authenticated"}',true);
do $$ begin
  begin perform public.respond_lost_item(current_setting('intu.qa_lost_report')::uuid,'found'); raise exception 'NULL bypass'; exception when insufficient_privilege then null; end;
  begin perform public.send_service_report_message(current_setting('intu.qa_lost_report')::uuid,'NULL bypass test'); raise exception 'NULL message bypass'; exception when insufficient_privilege then null; end;
end $$;
reset role;
do $$ begin
  if (select count(*) from private.service_reports where reporter_id like 'intu-qa-reports-%')<>3 then raise exception 'Incorrect persisted count'; end if;
  if (select reviewed_by from private.service_reports where id=current_setting('intu.qa_theft_report')::uuid)<>'intu-qa-reports-admin' then raise exception 'Review audit missing'; end if;
end $$;
-- Daily report limit must reject a new category even when there is no active duplicate.
insert into private.service_reports(id,ride_id,reporter_id,reported_id,reporter_role,category,description,status)
select gen_random_uuid(),current_setting('intu.qa_report_ride')::uuid,'intu-qa-reports-rider',
  'intu-qa-reports-driver','rider','misconduct','Caso QA cerrado '||i,'resolved' from generate_series(1,8) i;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-reports-rider","role":"authenticated"}',true);
do $$ begin
  begin perform public.create_service_report(gen_random_uuid(),current_setting('intu.qa_report_ride')::uuid,'other','Otro problema sin duplicado.');
    raise exception 'Daily report flood allowed'; exception when program_limit_exceeded then null; end;
end $$;
reset role;
rollback;
select 'Report privacy, participant authorization, idempotent creation, lost-item coordination, admin audit and NULL isolation: PASS' as verification;
