-- Run as postgres against the development project. All fixtures/preferences roll back.
begin;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-policy-20261005-%') then raise exception 'Fixture collision'; end if;
  if (select show_driver_rating or show_rider_rating or cancellation_penalties_enabled
      from private.trip_policy_settings where singleton) then raise exception 'Expected everything off by default'; end if;
  if has_function_privilege('anon','public.admin_set_trip_policy(jsonb)','execute')
     or has_function_privilege('anon','public.cancel_ride_with_reason(uuid,text,text)','execute')
     or has_function_privilege('anon','public.cancellation_preview(uuid,text)','execute')
     or has_function_privilege('anon','public.my_cancellation_status()','execute') then raise exception 'Anonymous access'; end if;
  if has_table_privilege('authenticated','private.ride_cancellations','select')
     or has_table_privilege('authenticated','private.cancellation_blocks','insert')
     or has_column_privilege('authenticated','public.rides','driver_rating','update')
     or has_column_privilege('authenticated','public.rides','rider_rating','insert') then raise exception 'Direct access'; end if;
end $$;
insert into public.profiles(id,first_name,last_name)
select 'intu-qa-policy-20261005-' || name, 'QA', name from unnest(array['admin','rider','driver','other']) name;
insert into private.admins(user_id) values ('intu-qa-policy-20261005-admin');
insert into public.drivers(id,document_type,document_number,license_number,status,rating,rating_count)
values ('intu-qa-policy-20261005-driver','dni','99051001','QA','approved',4.5,10);
insert into public.vehicles(driver_id,vehicle_type,brand,model,year,plate)
values ('intu-qa-policy-20261005-driver','mototaxi','QA','QA',2024,'QAPOL01');
insert into public.driver_locations(driver_id,latitude,longitude,is_available)
values ('intu-qa-policy-20261005-driver',-11.262,-74.637,true);
-- Rider history: two rated trips (5 and 4)
insert into public.rides(rider_id,driver_id,vehicle_type,status,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,
  destination_address,distance_meters,duration_seconds,rating_for_rider,completed_at)
select 'intu-qa-policy-20261005-rider','intu-qa-policy-20261005-driver','mototaxi','completed',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420,r,now()
from unnest(array[5,4]) r;

-- Hidden ratings while off; non-admins cannot read or change the policy
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
do $$ begin
  begin perform public.admin_get_trip_policy(); raise exception 'Non-admin read policy';
  exception when insufficient_privilege then null; end;
  begin perform public.admin_set_trip_policy('{"cancellation_penalties_enabled":true}'); raise exception 'Non-admin changed policy';
  exception when insufficient_privilege then null; end;
end $$;
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id, rider_rating, rider_rating_count
) select set_config('intu.qa_ride',id::text,true) from created where rider_rating is null and rider_rating_count is null;
do $$ begin
  if current_setting('intu.qa_ride', true) is null or current_setting('intu.qa_ride') = '' then raise exception 'Rating visible while off'; end if;
end $$;
select public.cancel_ride(current_setting('intu.qa_ride')::uuid, 'cancelled_by_rider');

select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-admin","role":"authenticated"}',true);
do $$ declare s jsonb; begin
  s := public.admin_set_trip_policy('{"show_driver_rating":true,"show_rider_rating":true,"cancellation_penalties_enabled":true,
    "strike_limit":2,"excused_limit":1,"free_cancel_seconds":120,"max_requests_per_10_min":60,"first_block_minutes":30}');
  if not (s->>'cancellation_penalties_enabled')::boolean or (s->>'strike_limit')::integer <> 2 then raise exception 'Admin save failed'; end if;
  begin perform public.admin_set_trip_policy('{"strike_limit":0}'); raise exception 'Out-of-range accepted';
  exception when check_violation then null; end;
  begin perform public.admin_set_trip_policy('{"strike_limit":null}'); raise exception 'Null accepted';
  exception when invalid_parameter_value then null; end;
end $$;

-- Ride 1: free cancellation inside the window; ratings are copied when visible
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id, rider_rating, rider_rating_count
) select set_config('intu.qa_ride',id::text,true) from created where rider_rating = 4.50 and rider_rating_count = 2;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-driver","role":"authenticated"}',true);
do $$ declare r public.rides; begin
  r := public.accept_ride(current_setting('intu.qa_ride')::uuid);
  if r.driver_rating <> 4.50 or r.driver_rating_count <> 10 then raise exception 'Driver rating not copied'; end if;
  if r.accept_pickup_distance_m not between 1000 and 1300 then raise exception 'Pickup distance missing: %', r.accept_pickup_distance_m; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
do $$ declare p jsonb; begin
  p := public.cancellation_preview(current_setting('intu.qa_ride')::uuid, 'changed_mind');
  if p->>'outcome' <> 'free' or (p->>'counts')::boolean then raise exception 'Free window not applied: %', p; end if;
  perform public.cancel_ride_with_reason(current_setting('intu.qa_ride')::uuid, 'changed_mind', null);
  if (public.my_cancellation_status()->'rider'->>'strikes')::integer <> 0 then raise exception 'Free cancellation counted'; end if;
end $$;

-- Ride 2: late cancellation counts
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id
) select set_config('intu.qa_ride',id::text,true) from created;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-driver","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_ride')::uuid);
reset role;
update public.rides set accepted_at = now() - interval '4 minutes' where id = current_setting('intu.qa_ride')::uuid;
update public.driver_locations set updated_at = now(), latitude = -11.255
 where driver_id = 'intu-qa-policy-20261005-driver';
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
do $$ declare p jsonb; begin
  -- The driver is approaching and on time: the claim is not verified
  p := public.cancellation_preview(current_setting('intu.qa_ride')::uuid, 'driver_not_moving');
  if p->>'outcome' <> 'counted' or not (p->>'counts')::boolean or (p->>'would_block')::boolean then raise exception 'Late cancellation preview: %', p; end if;
  perform public.cancel_ride_with_reason(current_setting('intu.qa_ride')::uuid, 'driver_not_moving', null);
  if (public.my_cancellation_status()->'rider'->>'strikes')::integer <> 1 then raise exception 'Strike missing'; end if;
end $$;

-- Ride 3: blaming the driver is excused once, then counts and blocks
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id
) select set_config('intu.qa_ride',id::text,true) from created;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-driver","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_ride')::uuid);
reset role;
update public.rides set accepted_at = now() - interval '4 minutes' where id = current_setting('intu.qa_ride')::uuid;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
select public.cancel_ride_with_reason(current_setting('intu.qa_ride')::uuid, 'driver_asked', null);
reset role;
do $$ begin
  if not exists(select 1 from private.ride_cancellations where ride_id = current_setting('intu.qa_ride')::uuid
                and outcome = 'excused' and reported_user_id = 'intu-qa-policy-20261005-driver') then raise exception 'Excuse not recorded'; end if;
end $$;
set local role authenticated;
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id
) select set_config('intu.qa_ride',id::text,true) from created;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-driver","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_ride')::uuid);
reset role;
update public.rides set accepted_at = now() - interval '4 minutes' where id = current_setting('intu.qa_ride')::uuid;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
do $$ declare p jsonb; begin
  p := public.cancellation_preview(current_setting('intu.qa_ride')::uuid, 'safety');
  if p->>'outcome' <> 'counted' or not (p->>'would_block')::boolean then raise exception 'Exhausted excuse preview: %', p; end if;
  perform public.cancel_ride_with_reason(current_setting('intu.qa_ride')::uuid, 'safety', 'QA');
  if public.my_cancellation_status()->'rider'->>'blocked_until' is null then raise exception 'Block missing'; end if;
  if (public.my_cancellation_status()->'rider'->>'strikes')::integer <> 0 then raise exception 'Block did not reset strikes'; end if;
  begin
    insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
    values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420);
    raise exception 'Blocked rider requested';
  exception when raise_exception then if sqlerrm <> 'cancellation_block' then raise; end if; end;
end $$;

-- Admin overview and lifting the block
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-admin","role":"authenticated"}',true);
do $$ declare o jsonb; begin
  o := public.admin_cancellation_overview(7);
  if not exists(select 1 from jsonb_array_elements(o->'people') e
                where e->>'user_id' = 'intu-qa-policy-20261005-rider' and e->>'blocked_until' is not null) then raise exception 'Overview missing rider'; end if;
  if not exists(select 1 from jsonb_array_elements(o->'people') e
                where e->>'user_id' = 'intu-qa-policy-20261005-driver' and (e->>'reports')::integer >= 2) then raise exception 'Overview missing reports'; end if;
  if jsonb_array_length(o->'reports') < 2 then raise exception 'Reports missing'; end if;
  if public.admin_lift_cancellation_block('intu-qa-policy-20261005-rider','rider')->>'blocked_until' is not null then raise exception 'Lift failed'; end if;
end $$;

-- Ride 4: driver no-show needs arrival, the wait and proximity; the rider gets the strike
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id
) select set_config('intu.qa_ride',id::text,true) from created;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-driver","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_ride')::uuid);
do $$ begin
  begin perform public.cancel_ride_with_reason(current_setting('intu.qa_ride')::uuid, 'rider_no_show', null); raise exception 'No-show before arrival';
  exception when raise_exception then if sqlerrm <> 'no_show_not_allowed' then raise; end if; end;
end $$;
select public.advance_ride(current_setting('intu.qa_ride')::uuid, 'arrived');
do $$ declare p jsonb; begin
  p := public.cancellation_preview(current_setting('intu.qa_ride')::uuid, 'rider_no_show');
  if p->>'outcome' <> 'no_show_wait' or (p->>'wait_seconds')::integer not between 290 and 300 then raise exception 'Wait preview: %', p; end if;
end $$;
reset role;
update public.rides set arrived_at = now() - interval '6 minutes' where id = current_setting('intu.qa_ride')::uuid;
set local role authenticated;
do $$ begin
  -- Still about 330 m away
  begin perform public.cancel_ride_with_reason(current_setting('intu.qa_ride')::uuid, 'rider_no_show', null); raise exception 'Far no-show accepted';
  exception when raise_exception then if sqlerrm <> 'no_show_not_allowed' then raise; end if; end;
end $$;
reset role;
update public.driver_locations set updated_at = now(), latitude = -11.2525
 where driver_id = 'intu-qa-policy-20261005-driver';
set local role authenticated;
do $$ declare r public.rides; begin
  r := public.cancel_ride_with_reason(current_setting('intu.qa_ride')::uuid, 'rider_no_show', null);
  if r.status <> 'cancelled' or r.cancelled_by <> 'driver' or r.cancel_reason <> 'rider_no_show' then raise exception 'No-show did not end the ride'; end if;
  if (public.my_cancellation_status()->'driver'->>'strikes')::integer <> 0 then raise exception 'Driver penalized for no-show'; end if;
end $$;
reset role;
do $$ begin
  if not exists(select 1 from private.ride_cancellations where ride_id = current_setting('intu.qa_ride')::uuid
                and user_id = 'intu-qa-policy-20261005-rider' and reason_code = 'no_show' and penalized) then raise exception 'Rider no-show strike missing'; end if;
end $$;
set local role authenticated;

-- Ride 5: a driver release goes back to searching, clears the copied rating and counts for the driver
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id
) select set_config('intu.qa_ride',id::text,true) from created;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-driver","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_ride')::uuid);
do $$ declare r public.rides; begin
  r := public.cancel_ride(current_setting('intu.qa_ride')::uuid, 'cancelled_by_driver');
  if r.status <> 'searching' or r.driver_rating is not null or r.accept_pickup_distance_m is not null then raise exception 'Release did not reset the ride'; end if;
  if (public.my_cancellation_status()->'driver'->>'strikes')::integer <> 1 then raise exception 'Driver release not counted'; end if;
end $$;
-- Rider: verified late driver is free; then the request rate limit applies
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-driver","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_ride')::uuid);
reset role;
update public.rides set accepted_at = now() - interval '30 minutes' where id = current_setting('intu.qa_ride')::uuid;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
do $$ declare s integer; begin
  s := (public.my_cancellation_status()->'rider'->>'strikes')::integer;
  if public.cancellation_preview(current_setting('intu.qa_ride')::uuid, 'driver_late')->>'outcome' <> 'free' then raise exception 'Late driver not detected'; end if;
  perform public.cancel_ride_with_reason(current_setting('intu.qa_ride')::uuid, 'driver_late', null);
  if (public.my_cancellation_status()->'rider'->>'strikes')::integer <> s then raise exception 'Late driver cancellation counted'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-admin","role":"authenticated"}',true);
select public.admin_set_trip_policy('{"max_requests_per_10_min":1}');
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
do $$ begin
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420);
  raise exception 'Rate limit ignored';
exception when raise_exception then if sqlerrm <> 'too_many_requests' then raise; end if; end $$;

-- Penalties off: reasons are still recorded, nothing is penalized or limited
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-admin","role":"authenticated"}',true);
select public.admin_set_trip_policy('{"cancellation_penalties_enabled":false}');
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
with created as (
  insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
  values ('mototaxi',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420) returning id
) select set_config('intu.qa_ride',id::text,true) from created;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-driver","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_ride')::uuid);
reset role;
update public.rides set accepted_at = now() - interval '4 minutes' where id = current_setting('intu.qa_ride')::uuid;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-policy-20261005-rider","role":"authenticated"}',true);
do $$ declare p jsonb; begin
  p := public.cancellation_preview(current_setting('intu.qa_ride')::uuid, 'other');
  if p->>'outcome' <> 'counted' or (p->>'counts')::boolean then raise exception 'Penalty shown while disabled: %', p; end if;
  perform public.cancel_ride_with_reason(current_setting('intu.qa_ride')::uuid, 'other', 'Cambio de planes');
end $$;
reset role;
do $$ begin
  if not exists(select 1 from private.ride_cancellations where ride_id = current_setting('intu.qa_ride')::uuid
                and outcome = 'counted' and not penalized and note = 'Cambio de planes') then raise exception 'Disabled penalty recorded wrong'; end if;
  if (select updated_by from private.trip_policy_settings where singleton) <> 'intu-qa-policy-20261005-admin' then raise exception 'Audit missing'; end if;
end $$;
set constraints all immediate;
rollback;
select 'Ratings visibility, cancellation outcomes, no-show, blocks, rate limit and admin controls: PASS' as verification;
