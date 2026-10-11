-- Help assistant on the public website. Visitors have no account, so the Cloudflare Pages function
-- authenticates with a shared secret (only its hash is stored) and identifies each visitor by a
-- salted IP digest. Web questions have their own switch, per-visitor quota and daily total cap.
alter table private.support_chat_settings
  add column web_enabled boolean not null default false,
  add column web_daily_limit_per_visitor integer not null default 10 check (web_daily_limit_per_visitor between 1 and 50),
  add column web_daily_total integer not null default 300 check (web_daily_total between 1 and 5000);

create table private.support_chat_web_gateway (
  singleton boolean primary key default true check (singleton),
  secret_hash text not null check (secret_hash ~ '^[0-9a-f]{64}$')
);
alter table private.support_chat_web_gateway enable row level security;
revoke all on private.support_chat_web_gateway from public, anon, authenticated;

create function private.support_chat_web_gateway_ok(p_secret text)
returns boolean language sql stable security definer set search_path = '' as $$
  select p_secret is not null and length(p_secret) = 64 and exists (
    select 1 from private.support_chat_web_gateway
    where singleton and secret_hash = encode(extensions.digest(p_secret, 'sha256'), 'hex'))
$$;
revoke all on function private.support_chat_web_gateway_ok(text) from public, anon, authenticated;

-- Whether the website should show the chat button. Reveals nothing else.
create function public.support_chat_web_status()
returns jsonb language sql stable security definer set search_path = '' as $$
  select jsonb_build_object('enabled', s.enabled and s.web_enabled and btrim(s.knowledge) <> '')
  from private.support_chat_settings s where s.singleton
$$;
revoke all on function public.support_chat_web_status() from public;
grant execute on function public.support_chat_web_status() to anon, authenticated;

create function private.support_chat_web_begin(p_secret text, p_visitor text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare s private.support_chat_settings; v_visitor text; v_used integer; v_total integer; v_id bigint;
begin
  if not private.support_chat_web_gateway_ok(p_secret) then raise exception 'forbidden' using errcode = '42501'; end if;
  if p_visitor is null or p_visitor !~ '^[0-9a-f]{64}$' then raise exception 'invalid_visitor' using errcode = '22023'; end if;
  select * into s from private.support_chat_settings where singleton;
  if not (s.enabled and s.web_enabled) or btrim(s.knowledge) = '' then
    raise exception 'support_chat_disabled' using errcode = 'P0001';
  end if;
  v_visitor := 'web:' || p_visitor;
  -- One reservation at a time for all web visitors, so parallel calls cannot exceed either cap
  perform pg_advisory_xact_lock(hashtext('support_chat:web'));
  select count(*) filter (where user_id = v_visitor), count(*) into v_used, v_total
    from private.support_chat_usage
   where user_id like 'web:%' and not failed and created_at > now() - interval '24 hours';
  if v_used >= s.web_daily_limit_per_visitor then raise exception 'support_chat_limit' using errcode = 'P0001'; end if;
  if v_total >= s.web_daily_total then raise exception 'support_chat_busy' using errcode = 'P0001'; end if;
  insert into private.support_chat_usage(user_id) values (v_visitor) returning id into v_id;
  return jsonb_build_object('usage_id', v_id, 'knowledge', s.knowledge,
                            'remaining_today', s.web_daily_limit_per_visitor - v_used - 1);
end $$;
revoke all on function private.support_chat_web_begin(text, text) from public, anon, authenticated;
create function public.support_chat_web_begin(p_secret text, p_visitor text)
returns jsonb language sql security definer set search_path = '' as $$
  select private.support_chat_web_begin(p_secret, p_visitor)
$$;
revoke all on function public.support_chat_web_begin(text, text) from public;
grant execute on function public.support_chat_web_begin(text, text) to anon;

create function private.support_chat_web_finish(p_secret text, p_usage_id bigint, p_usage jsonb, p_failed boolean)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if not private.support_chat_web_gateway_ok(p_secret) then raise exception 'forbidden' using errcode = '42501'; end if;
  update private.support_chat_usage set
    input_tokens = greatest(0, (p_usage->>'input_tokens')::integer),
    output_tokens = greatest(0, (p_usage->>'output_tokens')::integer),
    cache_read_tokens = greatest(0, (p_usage->>'cache_read_tokens')::integer),
    cache_write_tokens = greatest(0, (p_usage->>'cache_write_tokens')::integer),
    failed = coalesce(p_failed, false),
    finished_at = now()
  where id = p_usage_id and user_id like 'web:%' and finished_at is null
    and created_at > now() - interval '5 minutes';
end $$;
revoke all on function private.support_chat_web_finish(text, bigint, jsonb, boolean) from public, anon, authenticated;
create function public.support_chat_web_finish(p_secret text, p_usage_id bigint, p_usage jsonb, p_failed boolean default false)
returns void language sql security definer set search_path = '' as $$
  select private.support_chat_web_finish(p_secret, p_usage_id, p_usage, p_failed)
$$;
revoke all on function public.support_chat_web_finish(text, bigint, jsonb, boolean) from public;
grant execute on function public.support_chat_web_finish(text, bigint, jsonb, boolean) to anon;

-- Admin settings now include the website switch, its limits and how many questions came from the web.
create or replace function private.get_support_chat()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  return (select jsonb_build_object(
    'enabled', s.enabled, 'daily_limit_per_user', s.daily_limit_per_user, 'knowledge', s.knowledge,
    'web_enabled', s.web_enabled, 'web_daily_limit_per_visitor', s.web_daily_limit_per_visitor,
    'web_daily_total', s.web_daily_total,
    'updated_at', s.updated_at,
    'usage_30d', (select jsonb_build_object(
        'questions', count(*), 'people', count(distinct u.user_id), 'failed', count(*) filter (where u.failed),
        'web_questions', count(*) filter (where u.user_id like 'web:%'),
        'input_tokens', coalesce(sum(u.input_tokens), 0), 'output_tokens', coalesce(sum(u.output_tokens), 0),
        'cache_read_tokens', coalesce(sum(u.cache_read_tokens), 0), 'cache_write_tokens', coalesce(sum(u.cache_write_tokens), 0))
      from private.support_chat_usage u where u.created_at > now() - interval '30 days'))
    from private.support_chat_settings s where s.singleton);
end $$;

create or replace function private.set_support_chat(p_settings jsonb)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare s jsonb := coalesce(p_settings, '{}'::jsonb);
begin
  if private.requesting_uid() is null or not public.is_admin() then
    raise exception 'not_admin' using errcode = '42501';
  end if;
  if jsonb_typeof(s) <> 'object' or exists (select 1 from jsonb_each(s) e where jsonb_typeof(e.value) = 'null') then
    raise exception 'invalid_preferences' using errcode = '22023';
  end if;
  update private.support_chat_settings set
    enabled = coalesce((s->>'enabled')::boolean, enabled),
    daily_limit_per_user = coalesce((s->>'daily_limit_per_user')::integer, daily_limit_per_user),
    knowledge = coalesce(s->>'knowledge', knowledge),
    web_enabled = coalesce((s->>'web_enabled')::boolean, web_enabled),
    web_daily_limit_per_visitor = coalesce((s->>'web_daily_limit_per_visitor')::integer, web_daily_limit_per_visitor),
    web_daily_total = coalesce((s->>'web_daily_total')::integer, web_daily_total),
    updated_at = now(), updated_by = private.requesting_uid()
  where singleton;
  if exists (select 1 from private.support_chat_settings where singleton and enabled and btrim(knowledge) = '') then
    raise exception 'support_chat_knowledge_required' using errcode = '22023';
  end if;
  return private.get_support_chat();
end $$;
