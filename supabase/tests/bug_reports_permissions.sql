-- Verifies the same insert used by the Android repository, with an existing admin's auth claims.
-- All data is rolled back: no report is published to the administration panel.
begin;
do $$
declare test_uid text;
begin
  select a.user_id into test_uid from private.admins a join public.profiles p on p.id = a.user_id limit 1;
  if test_uid is null then raise exception 'An admin with a profile is required for this test'; end if;
  perform set_config('request.jwt.claims', jsonb_build_object('sub', test_uid, 'role', 'authenticated')::text, true);
end $$;
set local role authenticated;
do $$
declare inserted integer;
begin
  if current_user <> 'authenticated' or not public.is_admin() then raise exception 'Admin test claims were not applied'; end if;
  insert into public.bug_reports(title, description, screen, app_version, device_info)
    values ('QA report submit 20261001', 'Transactional verification of an admin submitting from Cuenta.', 'Cuenta', 'QA transaction', 'QA isolated transaction');
  get diagnostics inserted = row_count;
  if inserted <> 1 then raise exception 'Admin insert failed'; end if;
  if not exists(select 1 from public.admin_list_bug_reports() where title = 'QA report submit 20261001') then
    raise exception 'Submitted report did not reach the admin list';
  end if;
  begin
    insert into public.bug_reports(title, description, app_version, device_info)
      values ('GPS', 'Description that meets the minimum length.', 'QA', 'QA');
    raise exception 'Expected rejection of a short title';
  exception when check_violation then null;
  end;
  begin
    insert into public.bug_reports(title, description, app_version, device_info)
      values ('Valid title', 'Short', 'QA', 'QA');
    raise exception 'Expected rejection of a short description';
  exception when check_violation then null;
  end;
  begin
    insert into public.bug_reports(title, description, app_version, device_info)
      values (repeat('🚕', 3), repeat('🚕', 15), 'QA', 'QA');
    raise exception 'Expected rejection of a three-character emoji title';
  exception when check_violation then null;
  end;
end $$;
reset role;
set local role anon;
do $$
begin
  begin
    insert into public.bug_reports(title, description, app_version, device_info)
      values ('Valid anonymous title', 'Description that meets the minimum length.', 'QA', 'QA');
    raise exception 'Expected anonymous submission to be denied';
  exception when insufficient_privilege then null;
  end;
end $$;
reset role;
rollback;
