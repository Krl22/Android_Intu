-- Permissions, validation and persistence; every setting and identity rolls back.
begin;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-carousel-%') then raise exception 'Fixture collision'; end if;
  if has_function_privilege('anon','public.admin_set_ad_interval(integer)','execute')
    or has_function_privilege('anon','private.admin_set_ad_interval(integer)','execute')
    or has_table_privilege('authenticated','private.business_delivery_settings','update') then
    raise exception 'Unsafe settings grants';
  end if;
end $$;
insert into public.profiles(id,first_name,last_name) values
  ('intu-qa-carousel-admin','QA','Admin'),('intu-qa-carousel-rider','QA','Rider');
insert into private.admins(user_id) values('intu-qa-carousel-admin');
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-carousel-admin","role":"authenticated"}',true);
do $$ declare v_result jsonb; v_invalid integer; v_enabled boolean;
begin
  v_enabled := (public.admin_business_state()->>'enabled')::boolean;
  v_result := public.admin_set_ad_interval(7);
  if (v_result->>'ad_interval_seconds')::integer<>7 or (v_result->>'enabled')::boolean<>v_enabled then
    raise exception 'Interval not saved independently';
  end if;
  foreach v_invalid in array array[null,0,-1,61,2147483647] loop
    begin perform public.admin_set_ad_interval(v_invalid);
      raise exception 'Invalid interval accepted'; exception when invalid_parameter_value then null; end;
  end loop;
  if (public.admin_business_state()->>'ad_interval_seconds')::integer<>7 then raise exception 'Invalid edit changed interval'; end if;
  if (public.admin_set_business_enabled(false)->>'ad_interval_seconds')::integer<>7 then raise exception 'Switch lost interval'; end if;
  if public.business_feed()->'ads'<>'[]'::jsonb or (public.business_feed()->>'ad_interval_seconds')::integer<>7 then
    raise exception 'Disabled feed lost settings';
  end if;
  perform public.admin_set_business_enabled(true);
  if (public.business_feed()->>'ad_interval_seconds')::integer<>7 then raise exception 'Feed interval differs'; end if;
  perform public.admin_set_ad_interval(1);
  if (public.business_feed()->>'ad_interval_seconds')::integer<>1 then raise exception 'Minimum not accepted'; end if;
  perform public.admin_set_ad_interval(60);
  if (public.business_feed()->>'ad_interval_seconds')::integer<>60 then raise exception 'Maximum not accepted'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-carousel-rider","role":"authenticated"}',true);
do $$ begin
  begin perform public.admin_set_ad_interval(3);
    raise exception 'Non-admin updated interval'; exception when insufficient_privilege then null; end;
  begin perform private.admin_set_ad_interval(3);
    raise exception 'Private helper bypassed admin check'; exception when insufficient_privilege then null; end;
  if (public.business_feed()->>'ad_interval_seconds')::integer<>60 then raise exception 'Rider feed missing interval'; end if;
end $$;
select set_config('request.jwt.claims','{"role":"authenticated"}',true);
do $$ begin
  begin perform public.admin_set_ad_interval(3);
    raise exception 'Missing identity updated interval'; exception when insufficient_privilege then null; end;
end $$;
rollback;
select 'ad_carousel_settings PASS (fixtures rolled back)' as result;
