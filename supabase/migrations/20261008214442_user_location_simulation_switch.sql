-- Simulation is admin-only by default in every build; admins may temporarily enable users.
create table private.location_simulation_settings (
  singleton boolean primary key default true check (singleton),
  users_enabled boolean not null default false,
  updated_at timestamptz not null default now(),
  updated_by text
);
alter table private.location_simulation_settings enable row level security;
revoke all on private.location_simulation_settings from public, anon, authenticated;
insert into private.location_simulation_settings(singleton) values (true);

create function private.location_simulation_access()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  return (select jsonb_build_object('users_enabled', users_enabled, 'allowed', public.is_admin() or users_enabled)
    from private.location_simulation_settings where singleton);
end $$;
create function public.location_simulation_access()
returns jsonb language sql security invoker set search_path = '' as $$ select private.location_simulation_access() $$;
revoke all on function private.location_simulation_access(), public.location_simulation_access() from public, anon;
grant execute on function private.location_simulation_access(), public.location_simulation_access() to authenticated;

create function private.set_location_simulation(p_users_enabled boolean)
returns jsonb language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  if p_users_enabled is null then raise exception 'invalid_preferences' using errcode = '22023'; end if;
  update private.location_simulation_settings set users_enabled=p_users_enabled,
    updated_at=now(), updated_by=private.requesting_uid() where singleton;
  return private.location_simulation_access();
end $$;
create function public.admin_set_location_simulation(p_users_enabled boolean)
returns jsonb language sql security invoker set search_path = '' as $$ select private.set_location_simulation(p_users_enabled) $$;
revoke all on function private.set_location_simulation(boolean), public.admin_set_location_simulation(boolean) from public, anon;
grant execute on function private.set_location_simulation(boolean), public.admin_set_location_simulation(boolean) to authenticated;
