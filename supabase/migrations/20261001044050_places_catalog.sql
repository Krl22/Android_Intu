-- Intu catalog: Firebase UIDs are text; only verified, published pickups are public.
create table public.places_catalog (
  id uuid primary key default gen_random_uuid(),
  name text not null check (length(btrim(name)) between 2 and 120),
  aliases text[] not null default '{}' check (cardinality(aliases) <= 12 and length(aliases::text) <= 1500),
  category text not null default 'place' check (length(btrim(category)) between 1 and 40),
  locality text not null default 'Satipo' check (length(btrim(locality)) between 1 and 80),
  address text not null default '' check (length(address) <= 240),
  latitude double precision not null check (latitude between -90 and 90),
  longitude double precision not null check (longitude between -180 and 180),
  status text not null default 'draft' check (status in ('draft','published','inactive')),
  pickup_verified boolean not null default false,
  source text not null default 'manual' check (source in ('manual','OpenStreetMap')),
  source_id text,
  source_url text,
  source_retrieved_at timestamptz,
  source_latitude double precision,
  source_longitude double precision,
  coordinate_kind text not null default 'manual',
  attribution text not null default '',
  license text not null default '',
  updated_by text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default clock_timestamp(),
  constraint places_catalog_verified_publication check (status <> 'published' or pickup_verified),
  constraint places_catalog_source_id_key unique (source, source_id)
);
create index places_catalog_status_name_idx on public.places_catalog (status, name);
comment on table public.places_catalog is 'Intu curated places. Imported OpenStreetMap records retain ODbL 1.0 attribution and provenance. Source points may not be entrances; publication requires pickup verification.';
alter table public.places_catalog enable row level security;
revoke all on public.places_catalog from public, anon, authenticated;
grant select on public.places_catalog to authenticated;
create policy published_places_read on public.places_catalog for select to authenticated
  using (status = 'published' and pickup_verified and (select private.requesting_uid()) is not null);

create table private.places_catalog_revision (
  singleton boolean primary key default true check (singleton),
  revision bigint not null default 1
);
insert into private.places_catalog_revision (singleton) values (true);
revoke all on private.places_catalog_revision from public, anon, authenticated;
create function private.bump_places_catalog_revision() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  update private.places_catalog_revision set revision = revision + 1 where singleton;
  return null;
end;
$$;
revoke all on function private.bump_places_catalog_revision() from public, anon, authenticated;
create trigger places_catalog_revision after insert or update or delete on public.places_catalog
for each statement execute function private.bump_places_catalog_revision();

-- A single statement returns a consistent revision + full replacement, including removals.
create function public.places_catalog_sync(p_revision bigint default -1) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare result jsonb;
begin
  if private.requesting_uid() is null then raise exception 'not_authenticated' using errcode = '42501'; end if;
  select jsonb_build_object('revision', r.revision, 'changed', r.revision <> p_revision,
    'places', case when r.revision <> p_revision then
      coalesce((select jsonb_agg(jsonb_build_object(
        'id',p.id,'name',p.name,'aliases',p.aliases,'category',p.category,'locality',p.locality,
        'address',p.address,'latitude',p.latitude,'longitude',p.longitude,
        'status',p.status,'pickup_verified',p.pickup_verified,'source',p.source,
        'source_url',p.source_url,'attribution',p.attribution,'license',p.license,'updated_at',p.updated_at
      ) order by p.name,p.id) from public.places_catalog p
        where p.status = 'published' and p.pickup_verified), '[]'::jsonb)
      else '[]'::jsonb end) into result from private.places_catalog_revision r where r.singleton;
  return result;
end;
$$;
revoke all on function public.places_catalog_sync(bigint) from public, anon, authenticated;
grant execute on function public.places_catalog_sync(bigint) to authenticated;

create function public.admin_places_catalog() returns jsonb
language plpgsql stable security definer set search_path = '' as $$
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  return jsonb_build_object('places',coalesce((select jsonb_agg(to_jsonb(p) order by p.name,p.id)
    from public.places_catalog p), '[]'::jsonb));
end;
$$;
revoke all on function public.admin_places_catalog() from public, anon, authenticated;
grant execute on function public.admin_places_catalog() to authenticated;

-- Explicit editable fields protect provenance. An expected timestamp prevents lost edits.
create function public.admin_save_place(p_place jsonb) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare place_id uuid; saved public.places_catalog; old public.places_catalog;
  place_aliases text[];
begin
  if not public.is_admin() then raise exception 'not_admin' using errcode = '42501'; end if;
  place_id := nullif(p_place->>'id','')::uuid;
  if jsonb_typeof(coalesce(p_place->'aliases','[]'::jsonb)) <> 'array' then
    raise exception 'invalid_place' using errcode = '22023';
  end if;
  select coalesce(array_agg(btrim(v)), '{}') into place_aliases
    from jsonb_array_elements_text(coalesce(p_place->'aliases','[]'::jsonb)) v where length(btrim(v)) > 0;
  if p_place->>'name' is null or p_place->>'latitude' is null or p_place->>'longitude' is null
    or p_place->>'status' is null or p_place->>'pickup_verified' is null then
    raise exception 'invalid_place' using errcode = '22023';
  end if;
  if p_place->>'status' = 'published' and not (p_place->>'pickup_verified')::boolean then
    raise exception 'pickup_not_verified' using errcode = '22023';
  end if;
  if place_id is null then
    insert into public.places_catalog(name,aliases,category,locality,address,latitude,longitude,status,pickup_verified,updated_by)
    values (btrim(p_place->>'name'),place_aliases,btrim(coalesce(p_place->>'category','place')),
      btrim(coalesce(p_place->>'locality','Satipo')),btrim(coalesce(p_place->>'address','')),
      (p_place->>'latitude')::double precision,(p_place->>'longitude')::double precision,
      p_place->>'status',(p_place->>'pickup_verified')::boolean,private.requesting_uid()) returning * into saved;
  else
    select * into old from public.places_catalog where id = place_id for update;
    if not found then raise exception 'place_not_found' using errcode = '22023'; end if;
    if old.updated_at is distinct from (p_place->>'updated_at')::timestamptz then
      raise exception 'place_changed' using errcode = '40001';
    end if;
    -- Moving a published pickup requires the administrator to confirm the new point again.
    update public.places_catalog set name = btrim(p_place->>'name'), aliases = place_aliases,
      category = btrim(coalesce(p_place->>'category','place')), locality = btrim(coalesce(p_place->>'locality','Satipo')),
      address = btrim(coalesce(p_place->>'address','')), latitude = (p_place->>'latitude')::double precision,
      longitude = (p_place->>'longitude')::double precision, status = p_place->>'status',
      pickup_verified = (p_place->>'pickup_verified')::boolean, updated_by = private.requesting_uid(),
      updated_at = clock_timestamp() where id = place_id returning * into saved;
  end if;
  return to_jsonb(saved);
end;
$$;
revoke all on function public.admin_save_place(jsonb) from public, anon, authenticated;
grant execute on function public.admin_save_place(jsonb) to authenticated;

-- Small initial catalog, imported as drafts. Never overwrite subsequent admin edits.
insert into public.places_catalog(name,category,locality,address,latitude,longitude,status,pickup_verified,
 source,source_id,source_url,source_latitude,source_longitude,coordinate_kind,attribution,license,source_retrieved_at)
select name,category,'Satipo',address,latitude,longitude,'draft',false,
 source,source_id,source_url,latitude,longitude,coordinate_kind,'© OpenStreetMap contributors','ODbL 1.0', '2026-10-01T04:22:22.7243182Z'::timestamptz
from jsonb_to_recordset($intu_osm_20261001$[{"source":"OpenStreetMap","source_id":"way/656525953","source_url":"https://www.openstreetmap.org/way/656525953","name":"Cevicheria Agallas de Oro","category":"restaurant","latitude":-11.2561528,"longitude":-74.6390537,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/658485550","source_url":"https://www.openstreetmap.org/way/658485550","name":"Cevicheria Puntarhena","category":"restaurant","latitude":-11.25689715,"longitude":-74.639283,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/3250375767","source_url":"https://www.openstreetmap.org/node/3250375767","name":"Compañía de Bomberos Satipo Nº 147","category":"fire_station","latitude":-11.252396,"longitude":-74.6347159,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/7080516538","source_url":"https://www.openstreetmap.org/node/7080516538","name":"De Apoyo Manuel Higa Arakaki","category":"hospital","latitude":-11.2493056,"longitude":-74.6385852,"coordinate_kind":"mapped_point","address":"Calle Daniel Alcides Carrion 398","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/628590390","source_url":"https://www.openstreetmap.org/way/628590390","name":"Estadio de Satipo","category":"stadium","latitude":-11.24274905,"longitude":-74.6341784,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/630271929","source_url":"https://www.openstreetmap.org/way/630271929","name":"Gras Sintetico La Bombonera","category":"pitch","latitude":-11.249901000000001,"longitude":-74.6342946,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/6352334485","source_url":"https://www.openstreetmap.org/node/6352334485","name":"Grifo Vásquez","category":"fuel","latitude":-11.2479067,"longitude":-74.6371947,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/630691693","source_url":"https://www.openstreetmap.org/way/630691693","name":"Heladeria Karina","category":"ice_cream","latitude":-11.2567761,"longitude":-74.63910235,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/1030551649","source_url":"https://www.openstreetmap.org/way/1030551649","name":"Hospital de Apoyo Manuel Higa Arakaki","category":"hospital","latitude":-11.2487286,"longitude":-74.63559465,"coordinate_kind":"bounding_box_center","address":"Daniel Alcides Carrión 398","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/630271926","source_url":"https://www.openstreetmap.org/way/630271926","name":"Iglesia Evangelica Peruana Templo Amaus","category":"place_of_worship","latitude":-11.2550157,"longitude":-74.64023449999999,"coordinate_kind":"bounding_box_center","address":"Jirón San Martín 777","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/630271925","source_url":"https://www.openstreetmap.org/way/630271925","name":"Iglesia Filadelfia","category":"place_of_worship","latitude":-11.25397815,"longitude":-74.6396163,"coordinate_kind":"bounding_box_center","address":"Jirón San Martín","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/664248965","source_url":"https://www.openstreetmap.org/way/664248965","name":"Iglesia Metodista Satipo","category":"place_of_worship","latitude":-11.25800465,"longitude":-74.63896955,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560414947","source_url":"https://www.openstreetmap.org/node/5560414947","name":"Institución Educativa 30001-54","category":"school","latitude":-11.255308,"longitude":-74.645804,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388215","source_url":"https://www.openstreetmap.org/node/5560388215","name":"Institución Educativa 30632 Divino Niño Jesus","category":"school","latitude":-11.2491,"longitude":-74.6347,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388212","source_url":"https://www.openstreetmap.org/node/5560388212","name":"Institución Educativa 31515 Rafael Gastelua","category":"school","latitude":-11.2503159,"longitude":-74.6405617,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5381639716","source_url":"https://www.openstreetmap.org/node/5381639716","name":"Institución Educativa Emanuel","category":"school","latitude":-11.2569109,"longitude":-74.6255057,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560414948","source_url":"https://www.openstreetmap.org/node/5560414948","name":"Institución Educativa Francisco Irazola","category":"school","latitude":-11.2525,"longitude":-74.6421,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560414949","source_url":"https://www.openstreetmap.org/node/5560414949","name":"Institución educativa inicial 30001-54","category":"kindergarten","latitude":-11.2555,"longitude":-74.6458,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5381639715","source_url":"https://www.openstreetmap.org/node/5381639715","name":"Institución educativa inicial Emanuel","category":"kindergarten","latitude":-11.2569276,"longitude":-74.6254987,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560381537","source_url":"https://www.openstreetmap.org/node/5560381537","name":"Institución educativa inicial Jose Olaya","category":"kindergarten","latitude":-11.2568632,"longitude":-74.6376,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560414942","source_url":"https://www.openstreetmap.org/node/5560414942","name":"Institución educativa inicial Kinder Crayolitas","category":"kindergarten","latitude":-11.2553908,"longitude":-74.6457893,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388209","source_url":"https://www.openstreetmap.org/node/5560388209","name":"Institución educativa inicial No. 1028","category":"kindergarten","latitude":-11.2497,"longitude":-74.6333,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560414944","source_url":"https://www.openstreetmap.org/node/5560414944","name":"Institución educativa inicial No. 1029","category":"kindergarten","latitude":-11.2642,"longitude":-74.6436,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388216","source_url":"https://www.openstreetmap.org/node/5560388216","name":"Institución educativa inicial No. 140","category":"kindergarten","latitude":-11.2521,"longitude":-74.6396,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560414946","source_url":"https://www.openstreetmap.org/node/5560414946","name":"Institución educativa inicial No. 1785","category":"kindergarten","latitude":-11.2623,"longitude":-74.6424,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560414950","source_url":"https://www.openstreetmap.org/node/5560414950","name":"Institución educativa inicial No. 1786","category":"kindergarten","latitude":-11.2577,"longitude":-74.6482,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560414943","source_url":"https://www.openstreetmap.org/node/5560414943","name":"Institución educativa inicial No. 1799","category":"kindergarten","latitude":-11.2598,"longitude":-74.6473,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388218","source_url":"https://www.openstreetmap.org/node/5560388218","name":"Institución educativa inicial No. 652","category":"kindergarten","latitude":-11.2459,"longitude":-74.6342,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5381639718","source_url":"https://www.openstreetmap.org/node/5381639718","name":"Institución educativa inicial No. 667","category":"kindergarten","latitude":-11.2564,"longitude":-74.6249,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388217","source_url":"https://www.openstreetmap.org/node/5560388217","name":"Institución educativa inicial No. 669","category":"kindergarten","latitude":-11.245,"longitude":-74.6395,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388211","source_url":"https://www.openstreetmap.org/node/5560388211","name":"Institución educativa inicial Pamer","category":"kindergarten","latitude":-11.249979,"longitude":-74.6396,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388210","source_url":"https://www.openstreetmap.org/node/5560388210","name":"Institución educativa inicial Rafael Gastelua","category":"kindergarten","latitude":-11.2505764,"longitude":-74.6405134,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560381533","source_url":"https://www.openstreetmap.org/node/5560381533","name":"Institución Educativa Jose Olaya","category":"school","latitude":-11.2588251,"longitude":-74.6383337,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560414951","source_url":"https://www.openstreetmap.org/node/5560414951","name":"Institución Educativa Kinder Crayolitas","category":"school","latitude":-11.255258,"longitude":-74.645792,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560414945","source_url":"https://www.openstreetmap.org/node/5560414945","name":"Institución Educativa Niño Jesucito","category":"school","latitude":-11.2526158,"longitude":-74.6425302,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5381639717","source_url":"https://www.openstreetmap.org/node/5381639717","name":"Institución Educativa No. 30645","category":"school","latitude":-11.25595,"longitude":-74.625845,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388219","source_url":"https://www.openstreetmap.org/node/5560388219","name":"Institución Educativa No. 31834","category":"school","latitude":-11.2452,"longitude":-74.6392,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388214","source_url":"https://www.openstreetmap.org/node/5560388214","name":"Institución Educativa Pamer","category":"school","latitude":-11.2498685,"longitude":-74.6395946,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/5560388213","source_url":"https://www.openstreetmap.org/node/5560388213","name":"Institución Educativa Rafael Gastelua","category":"school","latitude":-11.2504632,"longitude":-74.6405402,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/6352238691","source_url":"https://www.openstreetmap.org/node/6352238691","name":"La Tuja","category":"restaurant","latitude":-11.253075,"longitude":-74.6384578,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/6352238693","source_url":"https://www.openstreetmap.org/node/6352238693","name":"Morgue Satipo","category":"mortuary","latitude":-11.2476432,"longitude":-74.6362935,"coordinate_kind":"mapped_point","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/515092971","source_url":"https://www.openstreetmap.org/way/515092971","name":"Municipalidad Provincial de Satipo","category":"townhall","latitude":-11.253693349999999,"longitude":-74.6362054,"coordinate_kind":"bounding_box_center","address":"Jirón Colonos Fundadores 312","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/628590378","source_url":"https://www.openstreetmap.org/way/628590378","name":"Parque de la Paz","category":"park","latitude":-11.24895875,"longitude":-74.63649079999999,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/628590382","source_url":"https://www.openstreetmap.org/way/628590382","name":"Parque del Amor","category":"park","latitude":-11.24864915,"longitude":-74.6362451,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/656525952","source_url":"https://www.openstreetmap.org/way/656525952","name":"Piñateria Yiyi","category":"party","latitude":-11.25607355,"longitude":-74.63999505,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/628590376","source_url":"https://www.openstreetmap.org/way/628590376","name":"Plaza de Armas José Olaya","category":"park","latitude":-11.25892885,"longitude":-74.6376332,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/628590389","source_url":"https://www.openstreetmap.org/way/628590389","name":"Plaza de Armas Santa Leonor","category":"park","latitude":-11.2463676,"longitude":-74.6355403,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/585523424","source_url":"https://www.openstreetmap.org/way/585523424","name":"Plaza Mayor de Satipo","category":"park","latitude":-11.25395355,"longitude":-74.63696185,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/515092982","source_url":"https://www.openstreetmap.org/way/515092982","name":"Poder Judicial","category":"courthouse","latitude":-11.2449304,"longitude":-74.63661975,"coordinate_kind":"bounding_box_center","address":"Avenida Antonio Raymondi Norte","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/656525951","source_url":"https://www.openstreetmap.org/way/656525951","name":"Pollería Koquetos","category":"restaurant","latitude":-11.2562373,"longitude":-74.63917624999999,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"node/6384433479","source_url":"https://www.openstreetmap.org/node/6384433479","name":"Recreo Turístico Mana","category":"restaurant","latitude":-11.2611832,"longitude":-74.6384171,"coordinate_kind":"mapped_point","address":"Avenida Micaela Bastidas","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/630691692","source_url":"https://www.openstreetmap.org/way/630691692","name":"Restaurant Turistico Katari","category":"restaurant","latitude":-11.2564685,"longitude":-74.6397172,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/630271927","source_url":"https://www.openstreetmap.org/way/630271927","name":"Terminal de Automovil Santa Rosa","category":"parking","latitude":-11.2541248,"longitude":-74.63552390000001,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/630271928","source_url":"https://www.openstreetmap.org/way/630271928","name":"Terminal Terrestre Satipo","category":"bus_station","latitude":-11.25993055,"longitude":-74.64460405,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false},{"source":"OpenStreetMap","source_id":"way/658485549","source_url":"https://www.openstreetmap.org/way/658485549","name":"Zoco Market","category":"convenience","latitude":-11.253938250000001,"longitude":-74.63794254999999,"coordinate_kind":"bounding_box_center","address":"","status":"draft","pickup_verified":false}]$intu_osm_20261001$::jsonb) as p(
 name text, category text, address text, latitude double precision, longitude double precision,
 source text,source_id text,source_url text,coordinate_kind text)
on conflict (source,source_id) do nothing;
