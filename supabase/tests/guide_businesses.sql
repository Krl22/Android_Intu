-- Guía «Descubre Satipo»: permisos, validación, publicación y listado público. Todo se revierte.
begin;
do $$ begin
  if has_table_privilege('anon','private.guide_businesses','select') or has_table_privilege('authenticated','private.guide_businesses','select') then
    raise exception 'Clients can read guide submissions directly';
  end if;
  if not has_function_privilege('anon','public.guide_listings()','execute') then raise exception 'Public listing not callable'; end if;
  if has_function_privilege('anon','public.admin_guide_businesses()','execute') then raise exception 'Anon can list submissions'; end if;
  if has_function_privilege('anon','public.admin_update_guide_business(uuid,jsonb)','execute') then raise exception 'Anon can edit submissions'; end if;
end $$;

-- Disposable gateway secret for this transaction only (rolled back below).
update private.tester_gateway set secret_hash = encode(extensions.digest(repeat('b', 64), 'sha256'), 'hex');

do $$
declare v_id uuid; v_listing jsonb;
begin
  begin
    perform private.submit_guide_business('{"name":"QA"}'::jsonb, repeat('c',64), repeat('x',64));
    raise exception 'Wrong secret accepted';
  exception when insufficient_privilege then null; end;
  begin
    perform private.submit_guide_business(jsonb_build_object('name','QA Hostal','category','hospedaje','contact_name','Ana',
      'phone','987654321','show_phone',true,'interest','gratis','consent',false), repeat('c',64), repeat('b',64));
    raise exception 'Missing consent accepted';
  exception when invalid_parameter_value then null; end;

  perform private.submit_guide_business(jsonb_build_object('name','  QA   Hostal  ','category','hospedaje','contact_name','Ana',
    'phone','987 654 321','show_phone',false,'interest','destacado','consent',true,'description',E'Cuartos\ncon baño',
    'email','QA@Example.pe','latitude',40.7,'longitude',-74.0), repeat('c',64), repeat('b',64));
  select id into v_id from private.guide_businesses where name = 'QA Hostal' and contact_name = 'Ana';
  if v_id is null then raise exception 'Submission not stored'; end if;
  if (select latitude from private.guide_businesses where id = v_id) is not null then raise exception 'Point outside Peru kept'; end if;
  if (select phone || '|' || email || '|' || description from private.guide_businesses where id = v_id) <> '987654321|qa@example.pe|Cuartos con baño' then
    raise exception 'Submission not normalized';
  end if;

  -- Not published yet: hidden from the public listing.
  if exists (select 1 from jsonb_array_elements(private.guide_listings()->'businesses') e where e->>'id' = v_id::text) then
    raise exception 'Unpublished business is public';
  end if;
  -- Publishing without a point is rejected by the table itself.
  begin
    update private.guide_businesses set status = 'published' where id = v_id;
    raise exception 'Published without point';
  exception when check_violation then null; end;
  update private.guide_businesses set status = 'published', latitude = -11.2541, longitude = -74.6382 where id = v_id;
  select e into v_listing from jsonb_array_elements(private.guide_listings()->'businesses') e where e->>'id' = v_id::text;
  if v_listing is null then raise exception 'Published business missing'; end if;
  if v_listing ? 'contact_name' or v_listing ? 'email' or v_listing ? 'admin_note' then raise exception 'Private fields exposed'; end if;
  if v_listing->'phone' <> 'null'::jsonb then raise exception 'Private phone exposed'; end if;

  -- Rate limit: 5 per IP digest in 10 minutes (one attempt already used above).
  for i in 1..4 loop
    perform private.submit_guide_business(jsonb_build_object('name','QA Bulk','category','otro','contact_name','Ana',
      'phone','987654321','show_phone',false,'interest','info','consent',true), repeat('c',64), repeat('b',64));
  end loop;
  begin
    perform private.submit_guide_business(jsonb_build_object('name','QA Bulk','category','otro','contact_name','Ana',
      'phone','987654321','show_phone',false,'interest','info','consent',true), repeat('c',64), repeat('b',64));
    raise exception 'Rate limit not enforced';
  exception when raise_exception then
    if sqlerrm <> 'too_many_requests' then raise; end if;
  end;
end $$;

-- Admin functions refuse callers without an admin identity.
do $$ begin
  perform set_config('request.jwt.claims', '{}', true);
  begin
    perform private.admin_guide_businesses();
    raise exception 'Non-admin listed submissions';
  exception when insufficient_privilege then null; end;
end $$;
rollback;
