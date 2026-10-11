// Chat de ayuda de la página web. Reenvía la conversación (solo vive en el navegador) al asistente de
// Supabase con la clave HELP_CHAT_KEY; el visitante se identifica con un hash salado de su IP, nunca
// con la IP. La base de datos aplica el interruptor del admin, el límite por visitante y el total diario.
const SUPABASE_URL = 'https://vkguzpciwpfvaeyedepl.supabase.co';
const SUPABASE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZrZ3V6cGNpd3BmdmFleWVkZXBsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMwNzYxMjYsImV4cCI6MjA3ODY1MjEyNn0.gHosYEPeqBHMjkezz5b9wuMQ6-PRFONcYrUuO62TYBc';
const MAX_BODY = 16384;
const MAX_TURNS = 12;
const MAX_CHARS = 1000;

const json = (body, status = 200) => new Response(JSON.stringify(body), {
  status, headers: {'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff'},
});

const ERRORS = {
  support_chat_disabled: [503, 'El asistente no está disponible ahora. Escríbenos a carthe5kboy@gmail.com.'],
  support_chat_limit: [429, 'Llegaste al límite de preguntas de hoy. Vuelve mañana o escríbenos a carthe5kboy@gmail.com.'],
  support_chat_busy: [429, 'El asistente recibió muchas preguntas hoy. Vuelve mañana o escríbenos a carthe5kboy@gmail.com.'],
};
const UNAVAILABLE = 'No pudimos responder ahora. Intenta de nuevo en unos minutos.';

/** Últimos turnos válidos, alternados y terminando en una pregunta. Igual que valida el asistente. */
export function cleanMessages(raw) {
  if (!Array.isArray(raw) || raw.length === 0 || raw.length > 40) return null;
  const turns = [];
  for (const item of raw.slice(-MAX_TURNS)) {
    if (!item || (item.role !== 'user' && item.role !== 'assistant') || typeof item.content !== 'string') return null;
    const content = item.content.trim().slice(0, MAX_CHARS);
    if (!content || (turns.length && turns[turns.length - 1].role === item.role)) return null;
    turns.push({role: item.role, content});
  }
  while (turns.length && turns[0].role !== 'user') turns.shift();
  return turns.length && turns[turns.length - 1].role === 'user' ? turns : null;
}

export async function visitorHash(secret, ip) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(`${secret}:help:${ip}`));
  return [...new Uint8Array(digest)].map(b => b.toString(16).padStart(2, '0')).join('');
}

const enabledFor = env => typeof env.HELP_CHAT_KEY === 'string' && /^[0-9a-f]{64}$/.test(env.HELP_CHAT_KEY);

export async function onRequest({request, env}) {
  if (request.method === 'GET') {
    if (!enabledFor(env)) return json({enabled: false});
    try {
      const response = await fetch(`${SUPABASE_URL}/rest/v1/rpc/support_chat_web_status`, {
        method: 'POST', headers: {'Content-Type': 'application/json', apikey: SUPABASE_KEY, Authorization: `Bearer ${SUPABASE_KEY}`},
        body: '{}', signal: AbortSignal.timeout(8000),
      });
      const status = response.ok ? await response.json() : null;
      return json({enabled: status?.enabled === true});
    } catch { return json({enabled: false}); }
  }
  if (request.method !== 'POST') return json({error: 'Método no permitido.'}, 405);
  if (!enabledFor(env)) return json({error: ERRORS.support_chat_disabled[1]}, 503);
  if (request.headers.get('Origin') !== new URL(request.url).origin) return json({error: 'Abre el chat desde la web de Intu.'}, 403);
  if (!request.headers.get('Content-Type')?.startsWith('application/json')) return json({error: 'Formato no válido.'}, 415);

  let messages;
  try {
    // Lee con límite aunque falte Content-Length
    const reader = request.body?.getReader();
    if (!reader) return json({error: 'Escribe tu pregunta.'}, 400);
    const chunks = []; let size = 0;
    while (true) {
      const {done, value} = await reader.read(); if (done) break;
      size += value.byteLength;
      if (size > MAX_BODY) { await reader.cancel(); return json({error: 'La conversación es demasiado larga. Recarga la página.'}, 413); }
      chunks.push(value);
    }
    const bytes = new Uint8Array(size); let offset = 0;
    for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
    messages = cleanMessages(JSON.parse(new TextDecoder().decode(bytes)).messages);
  } catch { messages = null; }
  if (!messages) return json({error: 'Escribe tu pregunta.'}, 400);

  const ip = request.headers.get('CF-Connecting-IP');
  if (!ip) return json({error: UNAVAILABLE}, 503);
  let response;
  try {
    response = await fetch(`${SUPABASE_URL}/functions/v1/support-chat`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json', apikey: SUPABASE_KEY, Authorization: `Bearer ${SUPABASE_KEY}`,
        'x-intu-web-key': env.HELP_CHAT_KEY},
      body: JSON.stringify({messages, visitor: await visitorHash(env.HELP_CHAT_KEY, ip)}),
      signal: AbortSignal.timeout(30000),
    });
  } catch { return json({error: UNAVAILABLE}, 503); }
  const data = await response.json().catch(() => ({}));
  if (response.ok && typeof data.reply === 'string') {
    return json({reply: data.reply, remaining: Number.isInteger(data.remaining_today) ? data.remaining_today : null});
  }
  const [status, message] = ERRORS[data.error] ?? [503, UNAVAILABLE];
  return json({error: message}, status);
}
