-- Guía «Descubre Satipo»: los comercios se registran desde la web (vía Pages con la clave
-- de la pasarela) y un administrador decide qué se publica. Solo lo publicado es público.
create table private.guide_businesses (
  id uuid primary key default gen_random_uuid(),
  name text not null check (length(btrim(name)) between 2 and 120),
  category text not null check (category in ('hospedaje','restaurante','turismo','tienda','servicios','otro')),
  description text not null default '' check (length(description) <= 500),
  address text not null default '' check (length(address) <= 240),
  locality text not null default 'Satipo' check (length(btrim(locality)) between 2 and 80),
  latitude double precision check (latitude between -90 and 90),
  longitude double precision check (longitude between -180 and 180),
  contact_name text not null check (length(btrim(contact_name)) between 2 and 120),
  phone text not null check (phone ~ '^\+?[0-9]{6,15}$'),
  show_phone boolean not null default false,
  email text check (email is null or length(email) between 3 and 254),
  social_url text check (social_url is null or (length(social_url) <= 300 and social_url ~ '^https://')),
  interest text not null default 'gratis' check (interest in ('destacado','gratis','info')),
  status text not null default 'new' check (status in ('new','contacted','published','archived')),
  featured boolean not null default false,
  admin_note text not null default '' check (length(admin_note) <= 500),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  reviewed_by text references public.profiles(id) on delete set null,
  constraint guide_businesses_point check ((latitude is null) = (longitude is null)),
  -- «Ir con Intu» necesita un punto: no se publica un comercio sin ubicación.
  constraint guide_businesses_published_point check (status <> 'published' or latitude is not null)
);
create index guide_businesses_status_created on private.guide_businesses(status, created_at desc);
create table private.guide_business_attempts (
  id bigint generated always as identity primary key,
  ip_hash text not null check (ip_hash ~ '^[a-f0-9]{64}$'),
  created_at timestamptz not null default now()
);
create index guide_attempts_ip_created on private.guide_business_attempts(ip_hash, created_at);
create index guide_attempts_created on private.guide_business_attempts(created_at);
alter table private.guide_businesses enable row level security;
alter table private.guide_business_attempts enable row level security;
revoke all on private.guide_businesses, private.guide_business_attempts from public, anon, authenticated;

-- Texto libre de una sola línea: sin caracteres de control y con espacios normalizados.
create function private.guide_clean(p_value text, p_max int)
returns text language sql immutable set search_path = '' as $$
  select nullif(left(btrim(regexp_replace(coalesce(p_value,''), '[[:cntrl:][:space:]]+', ' ', 'g')), p_max), '')
$$;
revoke all on function private.guide_clean(text,int) from public, anon, authenticated;

create function private.submit_guide_business(p_data jsonb, p_ip_hash text, p_secret text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_id uuid; v_secret text; v_recipient record;
  v_name text := private.guide_clean(p_data->>'name', 120);
  v_category text := p_data->>'category';
  v_description text := coalesce(private.guide_clean(p_data->>'description', 500), '');
  v_address text := coalesce(private.guide_clean(p_data->>'address', 240), '');
  v_locality text := coalesce(private.guide_clean(p_data->>'locality', 80), 'Satipo');
  v_contact text := private.guide_clean(p_data->>'contact_name', 120);
  v_phone text := regexp_replace(coalesce(p_data->>'phone',''), '[\s()-]', '', 'g');
  v_email text := lower(private.guide_clean(p_data->>'email', 254));
  v_social text := private.guide_clean(p_data->>'social_url', 300);
  v_interest text := coalesce(p_data->>'interest', 'gratis');
  v_lat double precision; v_lng double precision;
begin
  -- Same gateway secret as the tester form: only Pages can reach this RPC.
  if p_secret is null or length(p_secret) <> 64 or not exists (
    select 1 from private.tester_gateway where secret_hash = encode(extensions.digest(p_secret,'sha256'),'hex')
  ) then raise exception 'forbidden' using errcode = '42501'; end if;
  if jsonb_typeof(p_data) is distinct from 'object' or p_ip_hash is null or p_ip_hash !~ '^[a-f0-9]{64}$' then
    raise exception 'invalid_guide_business' using errcode = '22023';
  end if;
  if jsonb_typeof(p_data->'latitude') = 'number' and jsonb_typeof(p_data->'longitude') = 'number' then
    v_lat := (p_data->>'latitude')::double precision; v_lng := (p_data->>'longitude')::double precision;
    -- Only points inside Peru are kept; anything else is dropped, not rejected.
    if v_lat not between -18.5 and 0.1 or v_lng not between -81.5 and -68.5 then v_lat := null; v_lng := null; end if;
  end if;
  if v_name is null or length(v_name) < 2 or v_contact is null or length(v_contact) < 2
    or v_category is null or v_category not in ('hospedaje','restaurante','turismo','tienda','servicios','otro')
    or v_interest not in ('destacado','gratis','info') or length(v_locality) < 2
    or v_phone !~ '^\+?[0-9]{6,15}$'
    or (v_email is not null and v_email !~ '^[a-z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$')
    or (v_social is not null and v_social !~ '^https://[^[:space:]]+$')
    or jsonb_typeof(p_data->'show_phone') is distinct from 'boolean'
    or (p_data->'consent') is distinct from 'true'::jsonb then
    raise exception 'invalid_guide_business' using errcode = '22023';
  end if;
  perform pg_advisory_xact_lock(7241060);
  delete from private.guide_business_attempts where created_at < now() - interval '2 days';
  if (select count(*) from private.guide_business_attempts where created_at > now() - interval '1 day') >= 300
    or (select count(*) from private.guide_business_attempts where ip_hash = p_ip_hash and created_at > now() - interval '1 day') >= 20
    or (select count(*) from private.guide_business_attempts where ip_hash = p_ip_hash and created_at > now() - interval '10 minutes') >= 5 then
    raise exception 'too_many_requests' using errcode = 'P0001';
  end if;
  insert into private.guide_business_attempts(ip_hash) values (p_ip_hash);
  insert into private.guide_businesses(name, category, description, address, locality, latitude, longitude,
    contact_name, phone, show_phone, email, social_url, interest)
  values (v_name, v_category, v_description, v_address, v_locality, v_lat, v_lng,
    v_contact, v_phone, (p_data->>'show_phone')::boolean, v_email, v_social, v_interest)
  returning id into v_id;
  begin
    select decrypted_secret into v_secret from vault.decrypted_secrets where name = 'push_webhook_secret';
    if v_secret is not null then
      for v_recipient in select * from private.admin_activity_recipients('bug_report') loop
        perform net.http_post(
          url := 'https://us-central1-intu-e8403.cloudfunctions.net/ridePush',
          headers := jsonb_build_object('Content-Type','application/json','X-Intu-Secret',v_secret),
          body := jsonb_build_object('kind','admin_activity','eventType','bug_report','eventId','guide:'||v_id::text,
            'recipientUid',v_recipient.user_id,'tokens',to_jsonb(v_recipient.tokens),
            'title','Nuevo comercio para la guía',
            'body','Un comercio pidió aparecer en Descubre Satipo. Revisa Guía en Administración.'),
          timeout_milliseconds := 8000);
      end loop;
    end if;
  exception when others then raise warning 'guide business notification failed'; end;
  return jsonb_build_object('saved', true);
end $$;
create function public.submit_guide_business(p_data jsonb, p_ip_hash text, p_secret text)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.submit_guide_business(p_data, p_ip_hash, p_secret)
$$;
revoke all on function private.submit_guide_business(jsonb,text,text), public.submit_guide_business(jsonb,text,text)
  from public, anon, authenticated;
grant usage on schema private to anon;
grant execute on function private.submit_guide_business(jsonb,text,text), public.submit_guide_business(jsonb,text,text) to anon;

-- Public listing: only published entries, never the contact name, email or internal note.
create function private.guide_listings()
returns jsonb language sql stable security definer set search_path = '' as $$
  select jsonb_build_object('businesses', coalesce((select jsonb_agg(jsonb_build_object(
    'id', b.id, 'name', b.name, 'category', b.category, 'description', b.description,
    'address', b.address, 'locality', b.locality, 'latitude', b.latitude, 'longitude', b.longitude,
    'phone', case when b.show_phone then b.phone end, 'social_url', b.social_url, 'featured', b.featured
  ) order by b.featured desc, b.name, b.id) from private.guide_businesses b where b.status = 'published'), '[]'::jsonb))
$$;
create function public.guide_listings()
returns jsonb language sql stable security invoker set search_path = '' as $$ select private.guide_listings() $$;
revoke all on function private.guide_listings(), public.guide_listings() from public, anon, authenticated;
grant execute on function private.guide_listings(), public.guide_listings() to anon, authenticated;

create function private.admin_guide_businesses()
returns jsonb language plpgsql stable security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  return jsonb_build_object('businesses', coalesce((select jsonb_agg(to_jsonb(r)) from (
    select id, name, category, description, address, locality, latitude, longitude, contact_name, phone,
      show_phone, email, social_url, interest, status, featured, admin_note, created_at, updated_at
    from private.guide_businesses
    order by case status when 'new' then 0 when 'contacted' then 1 when 'published' then 2 else 3 end, created_at desc
    limit 500) r), '[]'::jsonb));
end $$;
create function public.admin_guide_businesses()
returns jsonb language sql stable security invoker set search_path = '' as $$ select private.admin_guide_businesses() $$;

-- Partial update: only the keys present in p_patch change. Coordinates travel together.
create function private.admin_update_guide_business(p_id uuid, p_patch jsonb)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_row private.guide_businesses;
begin
  if private.requesting_uid() is null or not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  if jsonb_typeof(p_patch) is distinct from 'object' then raise exception 'invalid_guide_business' using errcode = '22023'; end if;
  select * into v_row from private.guide_businesses where id = p_id for update;
  if not found then raise exception 'guide_business_not_found' using errcode = '22023'; end if;
  if p_patch ? 'name' then v_row.name := private.guide_clean(p_patch->>'name', 120); end if;
  if p_patch ? 'category' then v_row.category := p_patch->>'category'; end if;
  if p_patch ? 'description' then v_row.description := coalesce(private.guide_clean(p_patch->>'description', 500), ''); end if;
  if p_patch ? 'address' then v_row.address := coalesce(private.guide_clean(p_patch->>'address', 240), ''); end if;
  if p_patch ? 'status' then v_row.status := p_patch->>'status'; end if;
  if p_patch ? 'featured' then
    if jsonb_typeof(p_patch->'featured') <> 'boolean' then raise exception 'invalid_guide_business' using errcode = '22023'; end if;
    v_row.featured := (p_patch->>'featured')::boolean;
  end if;
  if p_patch ? 'show_phone' then
    if jsonb_typeof(p_patch->'show_phone') <> 'boolean' then raise exception 'invalid_guide_business' using errcode = '22023'; end if;
    v_row.show_phone := (p_patch->>'show_phone')::boolean;
  end if;
  if p_patch ? 'admin_note' then v_row.admin_note := coalesce(private.guide_clean(p_patch->>'admin_note', 500), ''); end if;
  if p_patch ? 'latitude' or p_patch ? 'longitude' then
    if jsonb_typeof(p_patch->'latitude') = 'null' and jsonb_typeof(p_patch->'longitude') = 'null' then
      v_row.latitude := null; v_row.longitude := null;
    elsif jsonb_typeof(p_patch->'latitude') = 'number' and jsonb_typeof(p_patch->'longitude') = 'number' then
      v_row.latitude := (p_patch->>'latitude')::double precision; v_row.longitude := (p_patch->>'longitude')::double precision;
      if v_row.latitude not between -18.5 and 0.1 or v_row.longitude not between -81.5 and -68.5 then
        raise exception 'guide_point_outside_peru' using errcode = '22023';
      end if;
    else raise exception 'invalid_guide_business' using errcode = '22023'; end if;
  end if;
  if v_row.name is null or length(v_row.name) < 2 then raise exception 'invalid_guide_business' using errcode = '22023'; end if;
  if v_row.status = 'published' and v_row.latitude is null then
    raise exception 'guide_point_required' using errcode = '22023';
  end if;
  -- Check constraints validate category and status.
  update private.guide_businesses set name = v_row.name, category = v_row.category, description = v_row.description,
    address = v_row.address, status = v_row.status, featured = v_row.featured, show_phone = v_row.show_phone,
    admin_note = v_row.admin_note, latitude = v_row.latitude, longitude = v_row.longitude,
    updated_at = now(), reviewed_by = private.requesting_uid()
  where id = p_id;
  return jsonb_build_object('saved', true);
end $$;
create function public.admin_update_guide_business(p_id uuid, p_patch jsonb)
returns jsonb language sql security invoker set search_path = '' as $$ select private.admin_update_guide_business(p_id, p_patch) $$;

-- Hard delete for «elimina mis datos» requests.
create function private.admin_delete_guide_business(p_id uuid)
returns jsonb language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  delete from private.guide_businesses where id = p_id;
  if not found then raise exception 'guide_business_not_found' using errcode = '22023'; end if;
  return jsonb_build_object('deleted', true);
end $$;
create function public.admin_delete_guide_business(p_id uuid)
returns jsonb language sql security invoker set search_path = '' as $$ select private.admin_delete_guide_business(p_id) $$;

revoke all on function private.admin_guide_businesses(), public.admin_guide_businesses(),
  private.admin_update_guide_business(uuid,jsonb), public.admin_update_guide_business(uuid,jsonb),
  private.admin_delete_guide_business(uuid), public.admin_delete_guide_business(uuid) from public, anon, authenticated;
grant execute on function private.admin_guide_businesses(), public.admin_guide_businesses(),
  private.admin_update_guide_business(uuid,jsonb), public.admin_update_guide_business(uuid,jsonb),
  private.admin_delete_guide_business(uuid), public.admin_delete_guide_business(uuid) to authenticated;
