-- Retention approved by Intu: closed services/reports 12 months, chat 30 days.
-- Never expire a live ride or a scheduled booking that has not been dispatched.
create function private.purge_expired_personal_records()
returns void language plpgsql security definer set search_path = '' as $$
begin
  delete from public.ride_messages where created_at < now() - interval '30 days';
  delete from private.service_reports where created_at < now() - interval '12 months';
  delete from public.bug_reports where created_at < now() - interval '12 months';
  delete from private.scheduled_rides
    where status in ('cancelled','failed','dispatched')
      and greatest(scheduled_for,updated_at) < now() - interval '12 months';
  delete from public.rides
    where status in ('completed','cancelled')
      and coalesce(completed_at,cancelled_at,requested_at) < now() - interval '12 months';
  delete from private.ride_cancellations where created_at < now() - interval '12 months';
  delete from private.cancellation_blocks where blocked_until < now() - interval '12 months';
  delete from private.support_chat_usage where created_at < now() - interval '30 days';
  delete from private.deleted_accounts where deleted_at < now() - interval '12 months';
  delete from private.tester_request_attempts where created_at < now() - interval '2 days';
end $$;
revoke all on function private.purge_expired_personal_records() from public,anon,authenticated;

-- Free-text reports and help usage are also removed when a profile is deleted.
-- Service reports/route history can be needed by the other participant for a claim;
-- their participant IDs are detached by the existing deletion flow and FKs.
create or replace function private.profiles_before_delete_cleanup()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  delete from public.ride_messages where sender_id = old.id;
  delete from public.bug_reports where user_id = old.id;
  delete from private.support_chat_usage where user_id = old.id;
  delete from private.ride_cancellations where user_id = old.id;
  update private.ride_cancellations set reported_user_id = null where reported_user_id = old.id;
  delete from private.cancellation_blocks where user_id = old.id;
  return old;
end $$;
revoke all on function private.profiles_before_delete_cleanup() from public,anon,authenticated;

select cron.schedule('purge-intu-personal-records','27 9 * * *',
  $$select private.purge_expired_personal_records()$$);
