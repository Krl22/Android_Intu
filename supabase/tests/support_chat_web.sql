-- Run as postgres against the development project. Settings, the test gateway and usage roll back.
begin;
do $$ begin
  if has_table_privilege('anon','private.support_chat_web_gateway','select')
     or has_table_privilege('authenticated','private.support_chat_settings','update') then raise exception 'Direct table access'; end if;
  if has_function_privilege('anon','private.support_chat_web_begin(text,text)','execute')
     or has_function_privilege('authenticated','public.support_chat_web_begin(text,text)','execute') then raise exception 'Unexpected grant'; end if;
end $$;
-- Known gateway secret (64 hex chars) only inside this transaction
insert into private.support_chat_web_gateway(singleton, secret_hash)
values (true, encode(extensions.digest(repeat('ab', 32), 'sha256'), 'hex'))
on conflict (singleton) do update set secret_hash = excluded.secret_hash;
update private.support_chat_settings set enabled = true, web_enabled = false,
  web_daily_limit_per_visitor = 2, web_daily_total = 3 where singleton;
delete from private.support_chat_usage where user_id like 'web:%';
set local role anon;
do $$ begin
  if (public.support_chat_web_status()->>'enabled')::boolean then raise exception 'Web shown while web switch is off'; end if;
  begin perform public.support_chat_web_begin(repeat('ab', 32), repeat('1', 64)); raise exception 'Began while web is off';
  exception when sqlstate 'P0001' then if sqlerrm <> 'support_chat_disabled' then raise; end if; end;
end $$;
reset role;
update private.support_chat_settings set web_enabled = true where singleton;
set local role anon;
do $$ declare r jsonb; begin
  if not (public.support_chat_web_status()->>'enabled')::boolean then raise exception 'Web hidden while enabled'; end if;
  begin perform public.support_chat_web_begin(repeat('cd', 32), repeat('1', 64)); raise exception 'Wrong secret accepted';
  exception when insufficient_privilege then null; end;
  begin perform public.support_chat_web_begin(repeat('ab', 32), 'not-a-hash'); raise exception 'Bad visitor accepted';
  exception when invalid_parameter_value then null; end;
  r := public.support_chat_web_begin(repeat('ab', 32), repeat('1', 64));
  if (r->>'remaining_today')::int <> 1 or coalesce(r->>'knowledge', '') = '' then raise exception 'Unexpected first reservation: %', r - 'knowledge'; end if;
  perform public.support_chat_web_finish(repeat('ab', 32), (r->>'usage_id')::bigint, '{"input_tokens": 10, "output_tokens": 5}', false);
  perform public.support_chat_web_begin(repeat('ab', 32), repeat('1', 64));
  begin perform public.support_chat_web_begin(repeat('ab', 32), repeat('1', 64)); raise exception 'Visitor limit not applied';
  exception when sqlstate 'P0001' then if sqlerrm <> 'support_chat_limit' then raise; end if; end;
  perform public.support_chat_web_begin(repeat('ab', 32), repeat('2', 64));
  begin perform public.support_chat_web_begin(repeat('ab', 32), repeat('3', 64)); raise exception 'Daily total not applied';
  exception when sqlstate 'P0001' then if sqlerrm <> 'support_chat_busy' then raise; end if; end;
end $$;
reset role;
do $$ begin
  if not exists (select 1 from private.support_chat_usage where user_id = 'web:' || repeat('1', 64)
      and input_tokens = 10 and output_tokens = 5 and finished_at is not null) then raise exception 'Usage not recorded'; end if;
  if (select count(*) from private.support_chat_usage where user_id like 'web:%') <> 3 then raise exception 'Unexpected web usage rows'; end if;
end $$;
rollback;
