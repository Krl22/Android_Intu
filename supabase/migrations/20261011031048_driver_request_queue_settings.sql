-- How a driver sees several requests at once: stack order and seconds to decide on each one.
create table private.driver_request_settings (
  singleton boolean primary key default true check (singleton),
  request_order text not null default 'fare' check (request_order in ('fare','distance')),
  timeout_seconds integer not null default 30 check (timeout_seconds between 10 and 120),
  updated_at timestamptz not null default now(),
  updated_by text
);
alter table private.driver_request_settings enable row level security;
revoke all on private.driver_request_settings from public, anon, authenticated;
insert into private.driver_request_settings(singleton) values (true);

create function private.get_driver_request_settings()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  return (select jsonb_build_object('request_order', request_order, 'timeout_seconds', timeout_seconds)
    from private.driver_request_settings where singleton);
end $$;
create function public.get_driver_request_settings()
returns jsonb language sql security invoker set search_path = '' as $$ select private.get_driver_request_settings() $$;
revoke all on function private.get_driver_request_settings(), public.get_driver_request_settings() from public, anon;
grant execute on function private.get_driver_request_settings(), public.get_driver_request_settings() to authenticated;

create function private.set_driver_request_settings(p_request_order text, p_timeout_seconds integer)
returns jsonb language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  if p_request_order is null or p_request_order not in ('fare','distance')
    or p_timeout_seconds is null or p_timeout_seconds not between 10 and 120 then
    raise exception 'invalid_driver_request_settings' using errcode = '22023';
  end if;
  update private.driver_request_settings set request_order = p_request_order, timeout_seconds = p_timeout_seconds,
    updated_at = now(), updated_by = private.requesting_uid() where singleton;
  return private.get_driver_request_settings();
end $$;
create function public.admin_set_driver_request_settings(p_request_order text, p_timeout_seconds integer)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.set_driver_request_settings(p_request_order, p_timeout_seconds)
$$;
revoke all on function private.set_driver_request_settings(text,integer), public.admin_set_driver_request_settings(text,integer) from public, anon;
grant execute on function private.set_driver_request_settings(text,integer), public.admin_set_driver_request_settings(text,integer) to authenticated;
