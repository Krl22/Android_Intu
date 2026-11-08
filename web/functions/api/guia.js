// Listado público de la guía «Descubre Satipo»: solo comercios publicados por un administrador.
const SUPABASE_URL = 'https://vkguzpciwpfvaeyedepl.supabase.co';
const SUPABASE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZrZ3V6cGNpd3BmdmFleWVkZXBsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMwNzYxMjYsImV4cCI6MjA3ODY1MjEyNn0.gHosYEPeqBHMjkezz5b9wuMQ6-PRFONcYrUuO62TYBc';
const DEFAULT_INSTALL_URL = 'https://viajaconintu.pages.dev/#instalar';
const json = (body, status, cache) => new Response(JSON.stringify(body), {
  status, headers: {'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': cache, 'X-Content-Type-Options': 'nosniff'},
});

/** Adónde va quien toca «Ir con Intu» sin tener la app. Solo se aceptan la web de Intu y Google Play. */
export function installUrl(env) {
  try {
    const url = new URL(env.GUIDE_INSTALL_URL || DEFAULT_INSTALL_URL);
    if (url.protocol === 'https:' && (url.hostname === 'viajaconintu.pages.dev' || url.hostname === 'play.google.com') && !url.username) return url.href;
  } catch {}
  return DEFAULT_INSTALL_URL;
}

const text = (value, max) => typeof value === 'string' ? value.slice(0, max) : '';
/** Copia solo los campos públicos esperados, con tipos comprobados. */
export function sanitizeListing(row) {
  if (!row || typeof row !== 'object') return null;
  const lat = row.latitude, lng = row.longitude;
  if (typeof row.id !== 'string' || typeof row.name !== 'string' || !Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  const phone = typeof row.phone === 'string' && /^\+?[0-9]{6,15}$/.test(row.phone) ? row.phone : null;
  const social = typeof row.social_url === 'string' && row.social_url.startsWith('https://') ? row.social_url.slice(0, 300) : null;
  return {
    id: row.id, name: text(row.name, 120), category: text(row.category, 20), description: text(row.description, 500),
    address: text(row.address, 240), locality: text(row.locality, 80), latitude: lat, longitude: lng,
    phone, socialUrl: social, featured: row.featured === true,
  };
}

export async function onRequest({request, env}) {
  if (request.method !== 'GET') return json({error: 'Método no permitido.'}, 405, 'no-store');
  let result;
  try {
    result = await fetch(`${SUPABASE_URL}/rest/v1/rpc/guide_listings`, {
      method: 'POST', headers: {'Content-Type': 'application/json', apikey: SUPABASE_KEY, Authorization: `Bearer ${SUPABASE_KEY}`},
      body: '{}', signal: AbortSignal.timeout(8000),
    });
  } catch { return json({error: 'No pudimos cargar la guía. Revisa tu conexión.'}, 503, 'no-store'); }
  if (!result.ok) return json({error: 'No pudimos cargar la guía. Intenta más tarde.'}, 503, 'no-store');
  const body = await result.json().catch(() => null);
  const rows = Array.isArray(body?.businesses) ? body.businesses : [];
  const businesses = rows.map(sanitizeListing).filter(Boolean);
  return json({businesses, installUrl: installUrl(env)}, 200, 'public, max-age=60');
}
