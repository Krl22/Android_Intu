-- Reversible integration assertions. Existing administrator UID is used only inside this transaction.
begin;
do $$
declare admin_uid text;
begin
  select user_id into admin_uid from private.admins order by created_at limit 1;
  if admin_uid is null then raise exception 'Test requires an existing administrator'; end if;
  perform set_config('intu.qa_admin_uid', admin_uid, true);
end;
$$;
set local role authenticated;
do $$
begin
  perform set_config('request.jwt.claims','{"sub":"intu-qa-ordinary-user","role":"authenticated"}',true);
  begin
    perform public.admin_places_catalog();
    raise exception 'Ordinary user accessed admin list';
  exception when insufficient_privilege then null; end;
  begin
    perform public.admin_save_place('{}'::jsonb);
    raise exception 'Ordinary user wrote via admin RPC';
  exception when insufficient_privilege then null; end;
  begin
    insert into public.places_catalog(name,latitude,longitude) values ('QA blocked',-11,-74);
    raise exception 'Ordinary user inserted directly';
  exception when insufficient_privilege then null; end;
end;
$$;
do $$
declare saved jsonb; original jsonb; snapshot jsonb; revision bigint; place_id uuid;
begin
  perform set_config('request.jwt.claims',jsonb_build_object('sub',current_setting('intu.qa_admin_uid'),'role','authenticated')::text,true);
  if jsonb_array_length(public.admin_places_catalog()->'places') < 55 then raise exception 'Initial catalog missing'; end if;
  saved := public.admin_save_place('{"name":"QA temporary pickup","aliases":["QA entrada"],"category":"square","locality":"Satipo","address":"QA temporary","latitude":-11.252,"longitude":-74.638,"status":"draft","pickup_verified":false}'::jsonb);
  place_id := (saved->>'id')::uuid;
  if exists (select 1 from public.places_catalog where id=place_id) then raise exception 'Draft leaked through RLS'; end if;
  original := saved;
  snapshot := public.places_catalog_sync(-1);
  revision := (snapshot->>'revision')::bigint;
  begin
    perform public.admin_save_place(saved || '{"status":"published"}'::jsonb);
    raise exception 'Unverified pickup published';
  exception when invalid_parameter_value then null; end;
  begin
    perform public.admin_save_place(saved || '{"latitude":"NaN"}'::jsonb);
    raise exception 'Invalid latitude accepted';
  exception when check_violation then null; end;
  saved := public.admin_save_place(saved || '{"status":"published","pickup_verified":true}'::jsonb);
  snapshot := public.places_catalog_sync(revision);
  if not (snapshot->>'changed')::boolean then raise exception 'Publishing failed to change revision'; end if;
  if not exists (select 1 from jsonb_array_elements(snapshot->'places') p where p->>'id'=place_id::text) then raise exception 'Published pickup missing'; end if;
  if (public.places_catalog_sync((snapshot->>'revision')::bigint)->>'changed')::boolean then raise exception 'Unchanged catalog downloaded again'; end if;
  begin
    perform public.admin_save_place(original || '{"name":"QA stale edit"}'::jsonb);
    raise exception 'Stale edit overwrote newer data';
  exception when serialization_failure then null; end;
  perform set_config('request.jwt.claims','{"sub":"intu-qa-ordinary-user","role":"authenticated"}',true);
  if not exists (select 1 from public.places_catalog where id=place_id) then raise exception 'Published pickup not readable'; end if;
  begin
    update public.places_catalog set name='QA blocked' where id=place_id;
    raise exception 'Ordinary user updated directly';
  exception when insufficient_privilege then null; end;
  perform set_config('request.jwt.claims',jsonb_build_object('sub',current_setting('intu.qa_admin_uid'),'role','authenticated')::text,true);
  revision := (snapshot->>'revision')::bigint;
  saved := public.admin_save_place(saved || '{"status":"inactive","source":"OpenStreetMap","source_url":"https://invalid.test"}'::jsonb);
  if saved->>'source' <> 'manual' or saved->>'source_url' is not null then raise exception 'Editable payload overwrote provenance'; end if;
  snapshot := public.places_catalog_sync(revision);
  if not (snapshot->>'changed')::boolean then raise exception 'Inactivation did not change revision'; end if;
  if exists (select 1 from jsonb_array_elements(snapshot->'places') p where p->>'id'=place_id::text) then raise exception 'Inactive pickup still cached'; end if;
  perform set_config('request.jwt.claims','{"sub":"intu-qa-ordinary-user","role":"authenticated"}',true);
  if exists (select 1 from public.places_catalog where id=place_id) then raise exception 'Inactive pickup leaked through RLS'; end if;
end;
$$;
set local role anon;
do $$
begin
  begin
    perform public.places_catalog_sync(-1);
    raise exception 'Anonymous caller accessed catalog';
  exception when insufficient_privilege then null; end;
  begin
    perform public.admin_places_catalog();
    raise exception 'Anonymous caller accessed admin list';
  exception when insufficient_privilege then null; end;
end;
$$;
reset role;
select 'PASS: permissions, draft privacy, verified publication, revision sync, removals, coordinate validation, edit conflicts, provenance protection' as result;
rollback;
