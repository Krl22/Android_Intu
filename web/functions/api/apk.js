// Tamaño y fecha del APK publicado, para mostrarlos junto al botón de descarga.
export async function onRequestGet({ env }) {
  const object = await env.APK.head('intu.apk');
  if (object === null) return Response.json({ available: false }, { status: 404 });
  return Response.json(
    { available: true, size: object.size, uploaded: object.uploaded.toISOString() },
    { headers: { 'cache-control': 'public, max-age=300' } }
  );
}
