-- Run as postgres. Settings and accounts are rolled back.
begin;
update private.location_simulation_settings set users_enabled=false where singleton;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-simulation-access-%') then raise exception 'Fixture collision'; end if;
  if has_table_privilege('authenticated','private.location_simulation_settings','update')
    or has_table_privilege('authenticated','private.location_simulation_settings','select') then raise exception 'Direct settings access'; end if;
  if has_table_privilege('authenticated','private.admin_simulation_preferences','update')
    or has_table_privilege('authenticated','private.admin_simulation_preferences','select') then raise exception 'Direct personal preferences access'; end if;
  if has_function_privilege('anon','public.admin_set_simulation_bar(boolean)','execute')
    or has_function_privilege('anon','private.set_admin_simulation_bar(boolean)','execute') then raise exception 'Anonymous personal preferences access'; end if;
  if has_function_privilege('anon','public.location_simulation_access()','execute')
    or has_function_privilege('anon','public.admin_set_location_simulation(boolean)','execute')
    or has_function_privilege('anon','private.location_simulation_access()','execute')
    or has_function_privilege('anon','private.set_location_simulation(boolean)','execute') then raise exception 'Anonymous access'; end if;
end $$;
insert into public.profiles(id,first_name,last_name) values
('intu-qa-simulation-access-admin','QA','Admin'),('intu-qa-simulation-access-other-admin','QA','Admin'),
('intu-qa-simulation-access-user','QA','User');
insert into private.admins(user_id) values ('intu-qa-simulation-access-admin'),('intu-qa-simulation-access-other-admin');
set local role authenticated;
select set_config('request.jwt.claims','{"role":"authenticated"}',true);
do $$ begin
  begin perform public.location_simulation_access(); raise exception 'No session allowed'; exception when invalid_authorization_specification then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-simulation-access-user","role":"authenticated"}',true);
do $$ declare access jsonb; begin
  access := public.location_simulation_access();
  if access->>'allowed' is distinct from 'false' or access->>'users_enabled' is distinct from 'false' then raise exception 'Default user access'; end if;
  begin perform public.admin_set_location_simulation(true); raise exception 'User enabled simulation'; exception when insufficient_privilege then null; end;
  begin perform private.set_location_simulation(true); raise exception 'User bypassed wrapper'; exception when insufficient_privilege then null; end;
  begin perform public.admin_set_simulation_bar(true); raise exception 'User changed personal admin preference'; exception when insufficient_privilege then null; end;
  begin perform private.set_admin_simulation_bar(true); raise exception 'User bypassed personal wrapper'; exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-simulation-access-admin","role":"authenticated"}',true);
do $$ begin
  if public.location_simulation_access()->>'allowed' is distinct from 'true' then raise exception 'Admin denied'; end if;
  if public.location_simulation_access()->>'admin_bar_enabled' is distinct from 'false' then raise exception 'Admin bar visible by default'; end if;
  begin perform public.admin_set_simulation_bar(null); raise exception 'Null personal preference accepted'; exception when invalid_parameter_value then null; end;
  if public.admin_set_simulation_bar(true)->>'admin_bar_enabled' is distinct from 'true' then raise exception 'Personal enable failed'; end if;
  if public.location_simulation_access()->>'admin_bar_enabled' is distinct from 'true' then raise exception 'Preference not persisted'; end if;
  if public.location_simulation_access()->>'users_enabled' is distinct from 'false' then raise exception 'Personal enable changed global permission'; end if;
  begin perform public.admin_set_location_simulation(null); raise exception 'Null accepted'; exception when invalid_parameter_value then null; end;
  if public.admin_set_location_simulation(true)->>'users_enabled' is distinct from 'true' then raise exception 'Enable failed'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-simulation-access-other-admin","role":"authenticated"}',true);
do $$ begin
  if public.location_simulation_access()->>'admin_bar_enabled' is distinct from 'false' then raise exception 'Preference leaked to another admin'; end if;
  if public.location_simulation_access()->>'allowed' is distinct from 'true' then raise exception 'Other admin denied'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-simulation-access-user","role":"authenticated"}',true);
do $$ begin
  if public.location_simulation_access()->>'allowed' is distinct from 'true' then raise exception 'Enabled user denied'; end if;
  if public.is_admin() then raise exception 'Simulation grants admin'; end if;
  if public.location_simulation_access()->>'admin_bar_enabled' is distinct from 'false' then raise exception 'User inherited admin bar preference'; end if;
  begin perform public.admin_set_simulation_bar(true); raise exception 'Enabled user saved admin preference'; exception when insufficient_privilege then null; end;
  begin perform public.admin_get_ride_security_settings(); raise exception 'Simulation grants admin panel'; exception when insufficient_privilege then null; end;
  begin perform public.admin_set_location_simulation(false); raise exception 'Enabled user changed switch'; exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-simulation-access-admin","role":"authenticated"}',true);
do $$ begin
  if public.admin_set_location_simulation(false)->>'allowed' is distinct from 'true' then raise exception 'Switch-off denied admin'; end if;
  if public.location_simulation_access()->>'admin_bar_enabled' is distinct from 'true' then raise exception 'Global switch changed personal preference'; end if;
  if public.admin_set_simulation_bar(false)->>'admin_bar_enabled' is distinct from 'false' then raise exception 'Personal hide failed'; end if;
  if public.location_simulation_access()->>'allowed' is distinct from 'true' then raise exception 'Hiding bar removed admin authority'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-simulation-access-user","role":"authenticated"}',true);
do $$ begin
  if public.location_simulation_access()->>'allowed' is distinct from 'false' then raise exception 'User not revoked'; end if;
end $$;
reset role;
delete from private.admins where user_id='intu-qa-simulation-access-admin';
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-simulation-access-admin","role":"authenticated"}',true);
do $$ begin
  if public.location_simulation_access()->>'allowed' is distinct from 'false' then raise exception 'Former admin still allowed'; end if;
  begin perform public.admin_set_simulation_bar(true); raise exception 'Former admin changed preference'; exception when insufficient_privilege then null; end;
end $$;
reset role;
do $$ begin
  if (select updated_by from private.location_simulation_settings where singleton) <> 'intu-qa-simulation-access-admin' then raise exception 'Missing editor audit'; end if;
  if exists(select 1 from private.admin_simulation_preferences where user_id='intu-qa-simulation-access-admin') then raise exception 'Former admin preference not removed'; end if;
end $$;
rollback;
select 'Admin defaults, personal bar persistence/isolation, global user opt-in/revocation and permissions: PASS' as verification;
