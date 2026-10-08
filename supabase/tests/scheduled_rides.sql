-- Run as postgres against the development project. All fixtures roll back.
begin;
update public.vehicle_types set base_fare=2.50, per_km=1.00, per_minute=0.10, min_fare=4.00 where code='mototaxi';
update private.fare_settings set honda_premium_percent=12, rounding_step=0.10 where singleton;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-sched-20261006-%') then raise exception 'Fixture collision'; end if;
  if has_function_privilege('anon','public.my_scheduled_rides()','execute')
     or has_function_privilege('anon','public.cancel_scheduled_ride(uuid)','execute') then raise exception 'Anonymous access'; end if;
  if has_table_privilege('authenticated','private.scheduled_rides','select')
     or has_table_privilege('authenticated','private.scheduled_rides','insert') then raise exception 'Direct access'; end if;
  if not exists(select 1 from cron.job where jobname = 'dispatch-scheduled-rides') then raise exception 'Dispatcher missing'; end if;
end $$;
insert into public.profiles(id,first_name,last_name)
select 'intu-qa-sched-20261006-' || name, 'QA', name from unnest(array['rider','other']) name;
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-sched-20261006-rider","role":"authenticated"}',true);
do $$ declare s jsonb; begin
  begin perform public.schedule_ride(now() + interval '5 minutes',-11.252,-74.637,'QA origen',-11.25,-74.63,'QA destino',2100,420);
    raise exception 'Too soon accepted';
  exception when invalid_parameter_value then if sqlerrm <> 'invalid_schedule_time' then raise; end if; end;
  begin perform public.schedule_ride(now() + interval '8 days',-11.252,-74.637,'QA origen',-11.25,-74.63,'QA destino',2100,420);
    raise exception 'Too far accepted';
  exception when invalid_parameter_value then if sqlerrm <> 'invalid_schedule_time' then raise; end if; end;
  begin perform public.schedule_ride(now() + interval '1 hour',-11.252,-74.637,'QA origen',-11.25,-74.63,'QA destino',2100,420,
      null,'efectivo',null,'{"name":"Ana","phone":"987"}');
    raise exception 'Bad contact accepted';
  exception when invalid_parameter_value then if sqlerrm <> 'invalid_passenger_contact' then raise; end if; end;
  s := public.schedule_ride(now() + interval '1 hour',-11.252,-74.637,'QA origen',-11.25,-74.63,'QA destino',2100,420);
  if s->>'status' <> 'scheduled' or (s->>'estimated_fare')::numeric <> 5.3 or s ? 'rider_id' then raise exception 'Scheduled row: %', s; end if;
  perform set_config('intu.qa_self', s->>'id', true);
  begin perform public.schedule_ride(now() + interval '80 minutes',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420);
    raise exception 'Overlap accepted';
  exception when raise_exception then if sqlerrm <> 'schedule_conflict' then raise; end if; end;
  s := public.schedule_ride(now() + interval '3 hours',-11.252,-74.637,'QA origen',-11.25,-74.63,'QA destino',2100,420,
      null,'yape_plin','honda','{"name":"Ana QA","phone":"+51987654321"}');
  if (s->>'estimated_fare')::numeric <> 5.9 or s->>'passenger_name' <> 'Ana QA' then raise exception 'Guest row: %', s; end if;
  perform set_config('intu.qa_guest', s->>'id', true);
  perform public.schedule_ride(now() + interval '5 hours',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420);
  begin perform public.schedule_ride(now() + interval '7 hours',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420);
    raise exception 'Fourth accepted';
  exception when raise_exception then if sqlerrm <> 'too_many_scheduled' then raise; end if; end;
  if jsonb_array_length(public.my_scheduled_rides()) <> 3 then raise exception 'List size'; end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-sched-20261006-other","role":"authenticated"}',true);
do $$ begin
  if jsonb_array_length(public.my_scheduled_rides()) <> 0 then raise exception 'Other rider sees schedule'; end if;
  begin perform public.cancel_scheduled_ride(current_setting('intu.qa_self')::uuid); raise exception 'Other rider cancelled';
  exception when no_data_found then null; end;
end $$;
reset role;
-- Due in 5 minutes: both dispatch as normal requests owned by the rider
update private.scheduled_rides set scheduled_for = now() + interval '5 minutes'
 where id in (current_setting('intu.qa_self')::uuid);
select private.dispatch_scheduled_rides();
do $$ declare s private.scheduled_rides; r public.rides; begin
  select * into s from private.scheduled_rides where id = current_setting('intu.qa_self')::uuid;
  if s.status <> 'dispatched' or s.ride_id is null then raise exception 'Not dispatched: % %', s.status, s.failure_reason; end if;
  select * into r from public.rides where id = s.ride_id;
  if r.rider_id <> 'intu-qa-sched-20261006-rider' or r.status <> 'searching' or r.estimated_fare <> 5.3 then raise exception 'Dispatched ride wrong'; end if;
  if coalesce(current_setting('request.jwt.claims', true), '') <> '' then raise exception 'Claims leaked after dispatch'; end if;
end $$;
-- The rider already has an open ride now: the guest ride fails with a clear reason
update private.scheduled_rides set scheduled_for = now() + interval '5 minutes' where id = current_setting('intu.qa_guest')::uuid;
select private.dispatch_scheduled_rides();
do $$ begin
  if (select failure_reason from private.scheduled_rides where id = current_setting('intu.qa_guest')::uuid) <> 'open_ride' then
    raise exception 'Open ride not reported'; end if;
end $$;
update public.rides set status = 'cancelled', cancelled_by = 'rider' where id = (select ride_id from private.scheduled_rides where id = current_setting('intu.qa_self')::uuid);
update private.scheduled_rides set status = 'scheduled', failure_reason = null, scheduled_for = now() + interval '5 minutes'
 where id = current_setting('intu.qa_guest')::uuid;
select private.dispatch_scheduled_rides();
do $$ declare s private.scheduled_rides; begin
  select * into s from private.scheduled_rides where id = current_setting('intu.qa_guest')::uuid;
  if s.status <> 'dispatched' or not exists(select 1 from public.ride_passengers where ride_id = s.ride_id and name = 'Ana QA') then
    raise exception 'Guest ride not dispatched with passenger'; end if;
end $$;
-- Missed by more than 15 minutes: fails instead of sending a late driver
update public.rides set status = 'cancelled', cancelled_by = 'rider' where rider_id = 'intu-qa-sched-20261006-rider' and status = 'searching';
update private.scheduled_rides set scheduled_for = now() - interval '20 minutes'
 where rider_id = 'intu-qa-sched-20261006-rider' and status = 'scheduled';
select private.dispatch_scheduled_rides();
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-sched-20261006-rider","role":"authenticated"}',true);
do $$ declare l jsonb; begin
  l := public.my_scheduled_rides();
  if jsonb_array_length(l) <> 1 or l->0->>'status' <> 'failed' or l->0->>'failure_reason' <> 'expired' then raise exception 'Expired row: %', l; end if;
  perform public.cancel_scheduled_ride((l->0->>'id')::uuid);
  if jsonb_array_length(public.my_scheduled_rides()) <> 0 then raise exception 'Dismiss failed'; end if;
end $$;
reset role;
rollback;
select 'Scheduled rides: time window, limits, privacy, dispatch as rider, open-ride failure and expiry: PASS' as verification;
