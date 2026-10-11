-- Public intake passes through Pages; only admins can read contact addresses.
create table private.tester_gateway (
  singleton boolean primary key default true check (singleton),
  secret_hash text not null check (secret_hash ~ '^[a-f0-9]{64}$')
);
create table private.tester_requests (
  id uuid primary key default gen_random_uuid(),
  email text not null unique check (length(email) between 3 and 254),
  status text not null default 'new' check (status in ('new','reviewed','archived')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  reviewed_by text references public.profiles(id) on delete set null
);
create index tester_requests_status_created on private.tester_requests(status,created_at desc);
create table private.tester_request_attempts (
  id bigint generated always as identity primary key,
  ip_hash text not null check (ip_hash ~ '^[a-f0-9]{64}$'),
  created_at timestamptz not null default now()
);
create index tester_attempts_ip_created on private.tester_request_attempts(ip_hash,created_at);
create index tester_attempts_created on private.tester_request_attempts(created_at);
alter table private.tester_gateway enable row level security;
alter table private.tester_requests enable row level security;
alter table private.tester_request_attempts enable row level security;
revoke all on private.tester_gateway,private.tester_requests,private.tester_request_attempts from public,anon,authenticated;

create function private.submit_tester_request(p_email text,p_ip_hash text,p_secret text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_email text := lower(btrim(p_email)); v_id uuid; v_secret text; v_recipient record;
begin
  if p_secret is null or length(p_secret) <> 64 or not exists (
    select 1 from private.tester_gateway where secret_hash=encode(extensions.digest(p_secret,'sha256'),'hex')
  ) then raise exception 'forbidden' using errcode='42501'; end if;
  if v_email is null or length(v_email)>254 or v_email !~ '^[a-z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$'
    or p_ip_hash is null or p_ip_hash !~ '^[a-f0-9]{64}$' then
    raise exception 'invalid_tester_request' using errcode='22023';
  end if;
  -- Serialize the small public queue so concurrent requests cannot exceed limits.
  perform pg_advisory_xact_lock(7241059);
  delete from private.tester_request_attempts where created_at < now()-interval '2 days';
  if (select count(*) from private.tester_request_attempts where created_at>now()-interval '1 day')>=1000
    or (select count(*) from private.tester_request_attempts where ip_hash=p_ip_hash and created_at>now()-interval '1 day')>=30
    or (select count(*) from private.tester_request_attempts where ip_hash=p_ip_hash and created_at>now()-interval '10 minutes')>=5 then
    raise exception 'too_many_requests' using errcode='P0001';
  end if;
  insert into private.tester_request_attempts(ip_hash) values(p_ip_hash);
  insert into private.tester_requests(email) values(v_email) on conflict(email) do nothing returning id into v_id;
  -- Same reply for new and duplicate emails; never expose another request's status.
  if v_id is not null then
    begin
      select decrypted_secret into v_secret from vault.decrypted_secrets where name='push_webhook_secret';
      if v_secret is not null then
        for v_recipient in select * from private.admin_activity_recipients('bug_report') loop
          perform net.http_post(
            url := 'https://us-central1-intu-e8403.cloudfunctions.net/ridePush',
            headers := jsonb_build_object('Content-Type','application/json','X-Intu-Secret',v_secret),
            body := jsonb_build_object('kind','admin_activity','eventType','bug_report','eventId','tester:'||v_id::text,
              'recipientUid',v_recipient.user_id,'tokens',to_jsonb(v_recipient.tokens),
              'title','Nuevo interesado en probar Intu','body','Alguien dejó su correo para probar Intu en Play. Revisa Testers en Administración.'),
            timeout_milliseconds := 8000);
        end loop;
      end if;
    exception when others then raise warning 'tester request notification failed'; end;
  end if;
  return jsonb_build_object('saved',true);
end $$;
create function public.submit_tester_request(p_email text,p_ip_hash text,p_secret text)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.submit_tester_request(p_email,p_ip_hash,p_secret)
$$;
revoke all on function private.submit_tester_request(text,text,text), public.submit_tester_request(text,text,text) from public,anon,authenticated;
grant usage on schema private to anon;
grant execute on function private.submit_tester_request(text,text,text),public.submit_tester_request(text,text,text) to anon;

create function private.admin_tester_requests()
returns jsonb language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  return jsonb_build_object('requests',coalesce((select jsonb_agg(to_jsonb(r)) from (
    select id,email,status,created_at,updated_at from private.tester_requests
    order by case status when 'new' then 0 when 'reviewed' then 1 else 2 end,created_at desc limit 200
  ) r),'[]'::jsonb));
end $$;
create function public.admin_tester_requests()
returns jsonb language sql security invoker set search_path = '' as $$ select private.admin_tester_requests() $$;

create function private.admin_review_tester_request(p_id uuid,p_status text)
returns jsonb language plpgsql security definer set search_path = '' as $$
begin
  if private.requesting_uid() is null or not public.is_admin() then raise exception 'not_admin' using errcode='42501'; end if;
  if p_status is null or p_status not in ('new','reviewed','archived') then raise exception 'invalid_preferences' using errcode='22023'; end if;
  update private.tester_requests set status=p_status,updated_at=now(),reviewed_by=private.requesting_uid() where id=p_id;
  if not found then raise exception 'tester_request_not_found' using errcode='22023'; end if;
  return jsonb_build_object('saved',true);
end $$;
create function public.admin_review_tester_request(p_id uuid,p_status text)
returns jsonb language sql security invoker set search_path = '' as $$ select private.admin_review_tester_request(p_id,p_status) $$;
revoke all on function private.admin_tester_requests(),public.admin_tester_requests(),
  private.admin_review_tester_request(uuid,text),public.admin_review_tester_request(uuid,text) from public,anon,authenticated;
grant execute on function private.admin_tester_requests(),public.admin_tester_requests(),
  private.admin_review_tester_request(uuid,text),public.admin_review_tester_request(uuid,text) to authenticated;
