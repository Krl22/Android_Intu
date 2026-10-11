const SUPABASE_URL = 'https://vkguzpciwpfvaeyedepl.supabase.co';
const SUPABASE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZrZ3V6cGNpd3BmdmFleWVkZXBsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMwNzYxMjYsImV4cCI6MjA3ODY1MjEyNn0.gHosYEPeqBHMjkezz5b9wuMQ6-PRFONcYrUuO62TYBc';
const json = (body, status = 200) => new Response(JSON.stringify(body), {
  status, headers: {'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options':'nosniff'},
});

export function testerLinks(env) {
  try {
    const group = new URL(env.TESTER_GROUP_URL);
    const play = new URL(env.TESTER_PLAY_URL);
    if (group.origin !== 'https://groups.google.com' || !/^\/g\/[a-z0-9-]+$/.test(group.pathname) ||
        play.origin !== 'https://play.google.com' || !/^\/apps\/testing\/com\.intu\.taxi$/.test(play.pathname) ||
        group.search || play.search || group.hash || play.hash || group.username || play.username) return null;
    return {groupUrl:group.href, playUrl:play.href};
  } catch { return null; }
}

export async function onRequest({request, env}) {
  const links = testerLinks(env);
  const enabled = env.TESTER_ACCESS_READY === 'true' && !!links && !!env.TESTER_GATEWAY_KEY;
  const playReady = env.TESTER_PLAY_READY === 'true';
  if (request.method === 'GET') return json({enabled, ...(enabled ? {...links,playReady} : {})});
  if (request.method !== 'POST') return json({error:'Método no permitido.'},405);
  if (!enabled) return json({error:'Estamos preparando las pruebas en Google Play. La descarga del APK sigue disponible.'},503);
  if (request.headers.get('Origin') !== new URL(request.url).origin) return json({error:'Abre el formulario desde la web de Intu.'},403);
  if (!request.headers.get('Content-Type')?.startsWith('application/json')) return json({error:'Formato no válido.'},415);
  // Read bounded bytes even when Content-Length is omitted/chunked.
  let size = 0; const chunks = []; const reader = request.body?.getReader();
  if (!reader) return json({error:'Escribe tu correo.'},400);
  try {
    while (true) {
      const {done,value} = await reader.read(); if (done) break;
      size += value.byteLength;
      if (size > 2048) { await reader.cancel(); return json({error:'El formulario es demasiado largo.'},413); }
      chunks.push(value);
    }
    const bytes = new Uint8Array(size); let offset = 0;
    for (const chunk of chunks) { bytes.set(chunk,offset); offset += chunk.byteLength; }
    const input = JSON.parse(new TextDecoder().decode(bytes));
    const email = typeof input.email === 'string' ? input.email.trim().toLowerCase() : '';
    if (input.consent !== true || email.length > 254 || !/^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$/.test(email))
      return json({error:'Escribe un correo válido y acepta el uso de tu correo.'},400);
    if (input.website) return json({saved:true,...links,playReady});
    const ip = request.headers.get('CF-Connecting-IP');
    if (!ip) return json({error:'No se pudo enviar. Intenta nuevamente.'},503);
    // Salted digest, never persist raw IP addresses or send them to the database.
    const digest = await crypto.subtle.digest('SHA-256',new TextEncoder().encode(env.TESTER_GATEWAY_KEY + ':' + ip));
    const ipHash = [...new Uint8Array(digest)].map(b=>b.toString(16).padStart(2,'0')).join('');
    let result;
    try { result = await fetch(`${SUPABASE_URL}/rest/v1/rpc/submit_tester_request`, {
      method:'POST',headers:{'Content-Type':'application/json',apikey:SUPABASE_KEY,Authorization:`Bearer ${SUPABASE_KEY}`},
      body:JSON.stringify({p_email:email,p_ip_hash:ipHash,p_secret:env.TESTER_GATEWAY_KEY}),
      signal:AbortSignal.timeout(10000),
    }); } catch { return json({error:'No pudimos guardar tu correo. Revisa tu conexión e intenta nuevamente.'},503); }
    if (!result.ok) {
      const error = await result.json().catch(()=>({}));
      if (error.message === 'too_many_requests') return json({error:'Has enviado varias solicitudes. Espera unos minutos e intenta de nuevo.'},429);
      return json({error:'No pudimos guardar tu correo. Intenta nuevamente.'},503);
    }
    return json({saved:true,...links,playReady});
  } catch {
    return json({error:'No se pudo enviar el formulario. Revisa tu conexión e intenta de nuevo.'},400);
  }
}
