-- Run as postgres against the development project. All fixtures/settings roll back.
begin;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-help-20261005-%') then raise exception 'Fixture collision'; end if;
  if (select enabled from private.support_chat_settings where singleton) then raise exception 'Expected assistant off by default'; end if;
  if btrim((select knowledge from private.support_chat_settings where singleton)) = '' then raise exception 'Draft knowledge missing'; end if;
  if has_function_privilege('anon','public.support_chat_begin()','execute')
     or has_function_privilege('anon','public.support_chat_status()','execute')
     or has_function_privilege('anon','public.admin_set_support_chat(jsonb)','execute') then raise exception 'Anonymous access'; end if;
  if has_table_privilege('authenticated','private.support_chat_settings','select')
     or has_table_privilege('authenticated','private.support_chat_usage','insert') then raise exception 'Direct access'; end if;
end $$;
insert into public.profiles(id,first_name,last_name)
select 'intu-qa-help-20261005-' || name, 'QA', name from unnest(array['admin','user','other']) name;
insert into private.admins(user_id) values ('intu-qa-help-20261005-admin');
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-help-20261005-user","role":"authenticated"}',true);
do $$ begin
  if (public.support_chat_status()->>'enabled')::boolean then raise exception 'Visible while off'; end if;
  begin perform public.support_chat_begin(); raise exception 'Question accepted while off';
  exception when raise_exception then if sqlerrm <> 'support_chat_disabled' then raise; end if; end;
  begin perform public.admin_get_support_chat(); raise exception 'Non-admin read settings';
  exception when insufficient_privilege then null; end;
  begin perform public.admin_set_support_chat('{"enabled":true}'); raise exception 'Non-admin enabled assistant';
  exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-help-20261005-admin","role":"authenticated"}',true);
do $$ declare s jsonb; begin
  s := public.admin_set_support_chat('{"enabled":true,"daily_limit_per_user":2}');
  if not (s->>'enabled')::boolean or (s->>'daily_limit_per_user')::integer <> 2 then raise exception 'Admin save failed'; end if;
  begin perform public.admin_set_support_chat('{"knowledge":"   "}'); raise exception 'Enabled without knowledge';
  exception when invalid_parameter_value then null; end;
  begin perform public.admin_set_support_chat('{"daily_limit_per_user":0}'); raise exception 'Out-of-range limit';
  exception when check_violation then null; end;
end $$;
-- Two questions, then the daily limit; a failed answer gives the question back
select set_config('request.jwt.claims','{"sub":"intu-qa-help-20261005-user","role":"authenticated"}',true);
do $$ declare first jsonb; second jsonb; begin
  if not (public.support_chat_status()->>'enabled')::boolean then raise exception 'Not visible when on'; end if;
  first := public.support_chat_begin();
  if (first->>'remaining_today')::integer <> 1 or btrim(first->>'knowledge') = '' then raise exception 'First reservation: %', first - 'knowledge'; end if;
  perform public.support_chat_finish((first->>'usage_id')::bigint, '{"input_tokens":2000,"output_tokens":150}', false);
  second := public.support_chat_begin();
  perform set_config('intu.qa_help_usage', second->>'usage_id', true);
  begin perform public.support_chat_begin(); raise exception 'Limit ignored';
  exception when raise_exception then if sqlerrm <> 'support_chat_limit' then raise; end if; end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-help-20261005-other","role":"authenticated"}',true);
select public.support_chat_finish(current_setting('intu.qa_help_usage')::bigint, '{"input_tokens":999999}', true);
select set_config('request.jwt.claims','{"sub":"intu-qa-help-20261005-user","role":"authenticated"}',true);
select public.support_chat_finish(current_setting('intu.qa_help_usage')::bigint, '{}', true);
do $$ begin
  if (public.support_chat_status()->>'remaining_today')::integer <> 1 then raise exception 'Failed answer still counted'; end if;
  perform public.support_chat_begin();
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-help-20261005-admin","role":"authenticated"}',true);
do $$ declare u jsonb; begin
  u := public.admin_get_support_chat()->'usage_30d';
  if (u->>'questions')::integer < 3 or (u->>'failed')::integer < 1 or (u->>'input_tokens')::integer < 2000
     or (u->>'input_tokens')::integer >= 999999 then raise exception 'Usage summary wrong: %', u; end if;
end $$;
reset role;
rollback;
select 'Support assistant off by default, admin switch, daily limit, failed refunds and usage summary: PASS' as verification;
