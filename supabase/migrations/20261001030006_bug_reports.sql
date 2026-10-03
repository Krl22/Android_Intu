-- Reportes enviados desde Cuenta y atendidos dentro del panel de administración.
create table public.bug_reports (
  id uuid primary key default gen_random_uuid(),
  user_id text default private.requesting_uid() references public.profiles(id) on delete set null,
  title text not null check (char_length(btrim(title)) between 5 and 120),
  description text not null check (char_length(btrim(description)) between 15 and 4000),
  screen text not null default '' check (char_length(screen) <= 100),
  app_version text not null check (char_length(app_version) <= 40),
  device_info text not null check (char_length(device_info) <= 200),
  status text not null default 'open' check (status in ('open', 'in_progress', 'resolved')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index bug_reports_created_at_idx on public.bug_reports(created_at desc);
alter table public.bug_reports enable row level security;
revoke all on public.bug_reports from public, anon, authenticated;
grant insert(title, description, screen, app_version, device_info) on public.bug_reports to authenticated;
create policy "reportar con la cuenta propia" on public.bug_reports for insert to authenticated
  with check (user_id = private.requesting_uid() and status = 'open');
create trigger bug_reports_touch_updated_at before update on public.bug_reports
  for each row execute function private.touch_updated_at();

create function public.admin_list_bug_reports()
returns table(id uuid, title text, description text, screen text, app_version text,
  device_info text, status text, created_at timestamptz, reporter_name text)
language plpgsql stable security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  return query select r.id, r.title, r.description, r.screen, r.app_version, r.device_info,
    r.status, r.created_at, coalesce(nullif(btrim(p.first_name || ' ' || p.last_name), ''), 'Usuario')
    from public.bug_reports r left join public.profiles p on p.id = r.user_id
    order by r.created_at desc limit 200;
end $$;
revoke execute on function public.admin_list_bug_reports() from public, anon;
grant execute on function public.admin_list_bug_reports() to authenticated;

create function public.admin_set_bug_report_status(p_report_id uuid, p_status text)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  if p_status not in ('open', 'in_progress', 'resolved') then
    raise exception 'invalid_report_status' using errcode = '22023';
  end if;
  update public.bug_reports set status = p_status where id = p_report_id;
  if not found then raise exception 'report_not_found' using errcode = 'P0002'; end if;
end $$;
revoke execute on function public.admin_set_bug_report_status(uuid, text) from public, anon;
grant execute on function public.admin_set_bug_report_status(uuid, text) to authenticated;
