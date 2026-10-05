-- Shared carousel interval; business availability remains independently controlled.
alter table private.business_delivery_settings
  add column ad_interval_seconds integer not null default 3
  check (ad_interval_seconds between 1 and 60);

create or replace function private.business_feed() returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare v_enabled boolean; v_interval integer;
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode='28000'; end if;
  select enabled,ad_interval_seconds into v_enabled,v_interval from private.business_delivery_settings where singleton;
  return jsonb_build_object('enabled',v_enabled,'ad_interval_seconds',v_interval,'ads',case when v_enabled then
    coalesce((select jsonb_agg(to_jsonb(a) order by a.sort_order,a.name,a.id)
      from private.business_ads a where a.published and a.archived_at is null),'[]'::jsonb)
    else '[]'::jsonb end);
end $$;

create or replace function private.admin_business_state() returns jsonb
language plpgsql stable security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  return jsonb_build_object('enabled',(select enabled from private.business_delivery_settings where singleton),
    'ad_interval_seconds',(select ad_interval_seconds from private.business_delivery_settings where singleton),
    'ads',coalesce((select jsonb_agg(to_jsonb(a) order by a.sort_order,a.name,a.id)
      from private.business_ads a where a.archived_at is null),'[]'::jsonb),
    'couriers',coalesce((select jsonb_agg(jsonb_build_object('id',d.id,'name',trim(p.first_name || ' ' || coalesce(p.last_name,'')),
      'plate',v.plate,'selected',t.driver_id is not null) order by p.first_name,d.id)
      from public.drivers d join public.profiles p on p.id=d.id
      join public.vehicles v on v.driver_id=d.id and v.is_active and v.vehicle_type='motorcycle'
      left join private.business_test_couriers t on t.driver_id=d.id where d.status='approved'),'[]'::jsonb));
end $$;

create function private.admin_set_ad_interval(p_seconds integer) returns jsonb
language plpgsql security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  if p_seconds is null or p_seconds not between 1 and 60 then
    raise exception 'El intervalo debe estar entre 1 y 60 segundos.' using errcode='22023';
  end if;
  update private.business_delivery_settings set ad_interval_seconds=p_seconds where singleton;
  return private.admin_business_state();
end $$;

create function public.admin_set_ad_interval(p_seconds integer) returns jsonb
language sql security invoker set search_path = '' as $$ select private.admin_set_ad_interval(p_seconds) $$;
revoke all on function private.admin_set_ad_interval(integer),public.admin_set_ad_interval(integer) from public,anon;
grant execute on function private.admin_set_ad_interval(integer),public.admin_set_ad_interval(integer) to authenticated;
