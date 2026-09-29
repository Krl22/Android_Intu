// Entrega el APK guardado en R2 (bucket intu-apk, archivo intu.apk) desde el mismo dominio de la página.
// Cloudflare Pages no acepta archivos de más de 25 MB, por eso el APK vive en R2.
// Soporta descargas por partes (Range), que Android usa para reanudar una descarga cortada.

const KEY = 'intu.apk';

function apkHeaders(object) {
  const headers = new Headers();
  object.writeHttpMetadata(headers);
  headers.set('content-type', 'application/vnd.android.package-archive');
  headers.set('content-disposition', 'attachment; filename="Intu.apk"');
  headers.set('etag', object.httpEtag);
  headers.set('accept-ranges', 'bytes');
  headers.set('cache-control', 'public, max-age=300');
  return headers;
}

function notReady() {
  return new Response('La app aún no está disponible para descargar. Intenta de nuevo más tarde.', {
    status: 404,
    headers: { 'content-type': 'text/plain; charset=utf-8' },
  });
}

export async function onRequestGet({ request, env }) {
  const object = await env.APK.get(KEY, { range: request.headers, onlyIf: request.headers });
  if (object === null) return notReady();

  const headers = apkHeaders(object);
  // Sin cuerpo: el navegador ya tiene esta misma versión (If-None-Match)
  if (!('body' in object)) return new Response(null, { status: 304, headers });

  const range = object.range;
  if (range && request.headers.has('range')) {
    const isSuffix = typeof range.suffix === 'number';
    const start = isSuffix ? object.size - range.suffix : (range.offset ?? 0);
    const length = isSuffix ? range.suffix : (range.length ?? object.size - start);
    headers.set('content-range', `bytes ${start}-${start + length - 1}/${object.size}`);
    headers.set('content-length', String(length));
    return new Response(object.body, { status: 206, headers });
  }
  headers.set('content-length', String(object.size));
  return new Response(object.body, { headers });
}

export async function onRequestHead({ env }) {
  const object = await env.APK.head(KEY);
  if (object === null) return notReady();
  const headers = apkHeaders(object);
  headers.set('content-length', String(object.size));
  return new Response(null, { headers });
}
