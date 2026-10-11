-- Disposable fixtures and notification queue entries are rolled back.
begin;
do $$ begin
  if exists(select 1 from public.profiles where id='intu-qa-retention-20261009') then
    raise exception 'Fixture collision';
  end if;
  if has_function_privilege('anon','private.purge_expired_personal_records()','execute')
    or has_function_privilege('authenticated','private.purge_expired_personal_records()','execute') then
    raise exception 'Client can purge records';
  end if;
end $$;
insert into public.profiles(id,first_name,last_name) values ('intu-qa-retention-20261009','QA','Retention');
insert into public.rides(id,rider_id,vehicle_type,status,origin_lat,origin_lng,origin_address,
  destination_lat,destination_lng,destination_address,distance_meters,duration_seconds,requested_at,completed_at)
values
 ('f36bfc20-a7ac-49a5-baaa-0a86c672fe01','intu-qa-retention-20261009','mototaxi','completed',-11.25,-74.63,'QA',-11.24,-74.62,'QA',1000,300,now()-interval '13 months',now()-interval '13 months'),
 ('f36bfc20-a7ac-49a5-baaa-0a86c672fe02','intu-qa-retention-20261009','mototaxi','completed',-11.25,-74.63,'QA',-11.24,-74.62,'QA',1000,300,now()-interval '13 months',now()-interval '1 month'),
 ('f36bfc20-a7ac-49a5-baaa-0a86c672fe03','intu-qa-retention-20261009','mototaxi','searching',-11.25,-74.63,'QA',-11.24,-74.62,'QA',1000,300,now()-interval '13 months',null);
insert into public.bug_reports(user_id,title,description,app_version,device_info,created_at)
values ('intu-qa-retention-20261009','QA expired report','Disposable retention verification.','QA','QA',now()-interval '13 months'),
       ('intu-qa-retention-20261009','QA current report','Disposable account deletion verification.','QA','QA',now());
select private.purge_expired_personal_records();
do $$ begin
  if exists(select 1 from public.rides where id='f36bfc20-a7ac-49a5-baaa-0a86c672fe01') then raise exception 'Old closed ride retained'; end if;
  if (select count(*) from public.rides where id in ('f36bfc20-a7ac-49a5-baaa-0a86c672fe02','f36bfc20-a7ac-49a5-baaa-0a86c672fe03'))<>2 then raise exception 'Current closure or live ride removed'; end if;
  if exists(select 1 from public.bug_reports where user_id='intu-qa-retention-20261009' and title='QA expired report') then raise exception 'Old report retained'; end if;
  if not exists(select 1 from public.bug_reports where user_id='intu-qa-retention-20261009' and title='QA current report') then raise exception 'Current report removed'; end if;
end $$;
-- Remove the fixture rides before testing the profile hook (ordinary FK protection).
delete from public.rides where rider_id='intu-qa-retention-20261009';
delete from public.profiles where id='intu-qa-retention-20261009';
do $$ begin
  if exists(select 1 from public.bug_reports where title='QA current report' and app_version='QA') then raise exception 'Deleted profile report text retained'; end if;
end $$;
rollback;
