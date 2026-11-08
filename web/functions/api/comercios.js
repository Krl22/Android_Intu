// Registro de comercios para la guía «Descubre Satipo». Mismo patrón y clave de pasarela que /api/testers.
const SUPABASE_URL = 'https://vkguzpciwpfvaeyedepl.supabase.co';
const SUPABASE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZrZ3V6cGNpd3BmdmFleWVkZXBsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMwNzYxMjYsImV4cCI6MjA3ODY1MjEyNn0.gHosYEPeqBHMjkezz5b9wuMQ6-PRFONcYrUuO62TYBc';
const json = (body, status = 200) => new Response(JSON.stringify(body), {
  status, headers: {'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff'},
});

export const CATEGORIES = ['hospedaje', 'restaurante', 'turismo', 'tienda', 'servicios', 'otro'];
export const INTERESTS = ['destacado', 'gratis', 'info'];
const EMAIL = /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$/;
const clean = (value, max) => typeof value === 'string' ? value.replace(/[\u0000-\u001f\u007f\s]+/g, ' ').trim().slice(0, max) : '';

/** Normaliza y valida el formulario. Devuelve {data} o {error}. */
export function parseBusiness(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) return {error: 'Formulario no válido.'};
  const name = clean(input.name, 120);
  const contactName = clean(input.contactName, 120);
  const phone = typeof input.phone === 'string' ? input.phone.replace(/[\s()-]/g, '') : '';
  const email = clean(input.email, 254).toLowerCase();
  let social = clean(input.socialUrl, 300);
  if (social && !/^https?:\/\//i.test(social)) social = 'https://' + social;
  if (social.startsWith('http://')) social = 'https://' + social.slice(7);
  if (name.length < 2) return {error: 'Escribe el nombre del negocio.'};
  if (!CATEGORIES.includes(input.category)) return {error: 'Elige el tipo de negocio.'};
  if (contactName.length < 2) return {error: 'Escribe el nombre de la persona de contacto.'};
  if (!/^\+?[0-9]{6,15}$/.test(phone)) return {error: 'Escribe un número de celular o WhatsApp válido.'};
  if (email && (email.length > 254 || !EMAIL.test(email))) return {error: 'El correo no es válido. Puedes dejarlo vacío.'};
  if (social) {
    try { const url = new URL(social); if (url.protocol !== 'https:' || !url.hostname.includes('.') || url.username || url.password) throw 0; social = url.href; }
    catch { return {error: 'El enlace de Facebook, Instagram o web no es válido. Puedes dejarlo vacío.'}; }
    if (social.length > 300) return {error: 'El enlace es demasiado largo.'};
  }
  if (!INTERESTS.includes(input.interest)) return {error: 'Elige cómo te gustaría aparecer.'};
  if (input.consent !== true) return {error: 'Acepta que Intu te contacte y publique los datos del negocio.'};
  const data = {
    name, category: input.category, description: clean(input.description, 500), address: clean(input.address, 240),
    locality: clean(input.locality, 80) || 'Satipo', contact_name: contactName, phone, show_phone: input.showPhone === true,
    email: email || null, social_url: social || null, interest: input.interest, consent: true,
  };
  const lat = input.latitude, lng = input.longitude;
  if (typeof lat === 'number' && typeof lng === 'number' && Number.isFinite(lat) && Number.isFinite(lng) &&
      lat >= -18.5 && lat <= 0.1 && lng >= -81.5 && lng <= -68.5) {
    data.latitude = Math.round(lat * 1e6) / 1e6; data.longitude = Math.round(lng * 1e6) / 1e6;
  }
  return {data};
}

export async function onRequest({request, env}) {
  const enabled = !!env.TESTER_GATEWAY_KEY && env.GUIDE_FORM_READY !== 'false';
  if (request.method === 'GET') return json({enabled});
  if (request.method !== 'POST') return json({error: 'Método no permitido.'}, 405);
  if (!enabled) return json({error: 'El registro de comercios no está disponible por ahora. Escríbenos por WhatsApp o correo.'}, 503);
  if (request.headers.get('Origin') !== new URL(request.url).origin) return json({error: 'Abre el formulario desde la web de Intu.'}, 403);
  if (!request.headers.get('Content-Type')?.startsWith('application/json')) return json({error: 'Formato no válido.'}, 415);
  let size = 0; const chunks = []; const reader = request.body?.getReader();
  if (!reader) return json({error: 'Completa el formulario.'}, 400);
  let input;
  try {
    while (true) {
      const {done, value} = await reader.read(); if (done) break;
      size += value.byteLength;
      if (size > 6144) { await reader.cancel(); return json({error: 'El formulario es demasiado largo.'}, 413); }
      chunks.push(value);
    }
    const bytes = new Uint8Array(size); let offset = 0;
    for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
    input = JSON.parse(new TextDecoder().decode(bytes));
  } catch { return json({error: 'No se pudo enviar el formulario. Revisa tu conexión e intenta de nuevo.'}, 400); }
  const parsed = parseBusiness(input);
  if (parsed.error) return json({error: parsed.error}, 400);
  // Honeypot: los bots reciben la misma respuesta y no se guarda nada.
  if (input.website) return json({saved: true});
  const ip = request.headers.get('CF-Connecting-IP');
  if (!ip) return json({error: 'No se pudo enviar. Intenta nuevamente.'}, 503);
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(env.TESTER_GATEWAY_KEY + ':guide:' + ip));
  const ipHash = [...new Uint8Array(digest)].map(b => b.toString(16).padStart(2, '0')).join('');
  let result;
  try {
    result = await fetch(`${SUPABASE_URL}/rest/v1/rpc/submit_guide_business`, {
      method: 'POST', headers: {'Content-Type': 'application/json', apikey: SUPABASE_KEY, Authorization: `Bearer ${SUPABASE_KEY}`},
      body: JSON.stringify({p_data: parsed.data, p_ip_hash: ipHash, p_secret: env.TESTER_GATEWAY_KEY}),
      signal: AbortSignal.timeout(10000),
    });
  } catch { return json({error: 'No pudimos guardar los datos. Revisa tu conexión e intenta nuevamente.'}, 503); }
  if (!result.ok) {
    const error = await result.json().catch(() => ({}));
    if (error.message === 'too_many_requests') return json({error: 'Has enviado varios registros. Espera unos minutos e intenta de nuevo.'}, 429);
    if (error.message === 'invalid_guide_business') return json({error: 'Revisa los datos del formulario e intenta de nuevo.'}, 400);
    return json({error: 'No pudimos guardar los datos. Intenta nuevamente.'}, 503);
  }
  return json({saved: true});
}
