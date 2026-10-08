-- Each admin can keep the QA bar hidden without changing other accounts' access.
create table private.admin_simulation_preferences (
  user_id text primary key references private.admins(user_id) on delete cascade,
  bar_enabled boolean not null default false,
  updated_at timestamptz not null default now()
);
alter table private.admin_simulation_preferences enable row level security;
revoke all on private.admin_simulation_preferences from public, anon, authenticated;

create or replace function private.location_simulation_access()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  v_uid text := private.requesting_uid();
  v_admin boolean;
  v_bar_enabled boolean;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  v_admin := public.is_admin();
  select coalesce((select bar_enabled from private.admin_simulation_preferences where user_id=v_uid), false)
    into v_bar_enabled;
  return (select jsonb_build_object('users_enabled', users_enabled, 'allowed', v_admin or users_enabled,
    'is_admin', v_admin, 'admin_bar_enabled', v_admin and v_bar_enabled)
    from private.location_simulation_settings where singleton);
end $$;

create function private.set_admin_simulation_bar(p_enabled boolean)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_uid text := private.requesting_uid();
begin
  if v_uid is null or not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  if p_enabled is null then raise exception 'invalid_preferences' using errcode = '22023'; end if;
  insert into private.admin_simulation_preferences(user_id, bar_enabled)
    values (v_uid, p_enabled)
    on conflict (user_id) do update set bar_enabled=excluded.bar_enabled, updated_at=now();
  return private.location_simulation_access();
end $$;
create function public.admin_set_simulation_bar(p_enabled boolean)
returns jsonb language sql security invoker set search_path = '' as $$ select private.set_admin_simulation_bar(p_enabled) $$;
revoke all on function private.set_admin_simulation_bar(boolean), public.admin_set_simulation_bar(boolean) from public, anon;
grant execute on function private.set_admin_simulation_bar(boolean), public.admin_set_simulation_bar(boolean) to authenticated;
