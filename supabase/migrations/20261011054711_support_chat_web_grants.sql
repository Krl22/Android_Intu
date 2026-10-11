-- Supabase grants new public functions to signed-in users by default; only the website gateway (anon)
-- needs the web chat functions.
revoke execute on function public.support_chat_web_begin(text, text) from authenticated, service_role;
revoke execute on function public.support_chat_web_finish(text, bigint, jsonb, boolean) from authenticated, service_role;
