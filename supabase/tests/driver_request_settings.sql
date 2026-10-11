-- Run as postgres against the development project. All fixtures/preferences roll back.
begin;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-reqs-20261010-%') then raise exception 'Fixture collision'; end if;
  if has_function_privilege('anon','public.get_driver_request_settings()','execute')
     or has_function_privilege('anon','public.admin_set_driver_request_settings(text,integer)','execute') then raise exception 'Anonymous access'; end if;
  if has_table_privilege('authenticated','private.driver_request_settings','select')
     or has_table_privilege('authenticated','private.driver_request_settings','update') then raise exception 'Direct table access'; end if;
end $$;
-- Known starting point so the default checks do not depend on what an admin already chose.
update private.driver_request_settings set request_order = 'fare', timeout_seconds = 30 where singleton;
insert into public.profiles(id,first_name,last_name)
select 'intu-qa-reqs-20261010-' || name, 'QA', name from unnest(array['admin','driver']) name;
insert into private.admins(user_id) values ('intu-qa-reqs-20261010-admin');
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-reqs-20261010-driver","role":"authenticated"}',true);
do $$ declare s jsonb := public.get_driver_request_settings(); begin
  if s->>'request_order' <> 'fare' or (s->>'timeout_seconds')::int <> 30 then raise exception 'Unexpected defaults: %', s; end if;
  begin perform public.admin_set_driver_request_settings('distance', 45); raise exception 'Non-admin changed settings';
  exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-reqs-20261010-admin","role":"authenticated"}',true);
do $$ declare s jsonb; begin
  s := public.admin_set_driver_request_settings('distance', 45);
  if s->>'request_order' <> 'distance' or (s->>'timeout_seconds')::int <> 45 then raise exception 'Save mismatch: %', s; end if;
  begin perform public.admin_set_driver_request_settings('newest', 45); raise exception 'Accepted unknown order';
  exception when invalid_parameter_value then null; end;
  begin perform public.admin_set_driver_request_settings('fare', 5); raise exception 'Accepted short timeout';
  exception when invalid_parameter_value then null; end;
  begin perform public.admin_set_driver_request_settings('fare', 121); raise exception 'Accepted long timeout';
  exception when invalid_parameter_value then null; end;
  begin perform public.admin_set_driver_request_settings(null, 30); raise exception 'Accepted null order';
  exception when invalid_parameter_value then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-reqs-20261010-driver","role":"authenticated"}',true);
do $$ declare s jsonb := public.get_driver_request_settings(); begin
  if s->>'request_order' <> 'distance' or (s->>'timeout_seconds')::int <> 45 then raise exception 'Driver sees stale settings: %', s; end if;
end $$;
select set_config('request.jwt.claims','{}',true);
do $$ begin
  begin perform public.get_driver_request_settings(); raise exception 'Read without session';
  exception when sqlstate '28000' then null; end;
end $$;
reset role;
rollback;
