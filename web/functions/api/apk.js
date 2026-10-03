// Publication metadata is advertised only after the matching APK is available.
export async function onRequestGet({ env }) {
  const object = await env.APK.head('intu.apk');
  const headers = { 'cache-control': 'no-store' };
  if (object === null) return Response.json({ available: false }, { status: 404, headers });
  let release = null;
  try {
    const metadata = await env.APK.get('latest.json');
    if (metadata && metadata.size <= 32768) {
      const candidate = await metadata.json();
      if (candidate.packageName === 'com.intu.taxi' &&
          Number.isSafeInteger(candidate.versionCode) && candidate.versionCode > 0 && candidate.versionCode <= 2147483647 &&
          typeof candidate.versionName === 'string' && candidate.versionName.trim() && candidate.versionName.length <= 40 &&
          Number.isSafeInteger(candidate.minSdk) && candidate.minSdk > 0 &&
          candidate.size === object.size && candidate.apkUploaded === object.uploaded.toISOString() &&
          typeof candidate.sha256 === 'string' && /^[a-f0-9]{64}$/i.test(candidate.sha256)) {
        // Also check the immutable download exists before asking a user to update.
        const versioned = await env.APK.head(`releases/${candidate.versionCode}/intu.apk`);
        if (versioned && versioned.size === candidate.size) release = candidate;
      }
    }
  } catch (_) { /* The download page still works while publishing or repairing metadata. */ }
  return Response.json(
    { available: true, size: object.size, uploaded: object.uploaded.toISOString(),
      updateAvailable: release !== null,
      ...(release ? {
        packageName: release.packageName, versionCode: release.versionCode, versionName: release.versionName,
        minSdk: release.minSdk, sha256: release.sha256,
        downloadUrl: `https://viajaconintu.pages.dev/descargar?versionCode=${release.versionCode}`,
      } : {}),
    },
    { headers }
  );
}
