const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const moduleFor = file => import('data:text/javascript;base64,' + fs.readFileSync(path.join(__dirname, '..', file)).toString('base64'));
const apk = { size: 106000000, uploaded: new Date('2026-10-02T21:15:47.212Z'), httpEtag: '"apk"',
  writeHttpMetadata: () => {} };
const release = { packageName: 'com.intu.taxi', versionCode: 29, versionName: '1.28', minSdk: 24,
  size: apk.size, sha256: 'a'.repeat(64), apkUploaded: apk.uploaded.toISOString() };
const envFor = (data, fixed = apk) => ({ APK: {
  head: async key => key === 'intu.apk' ? apk : fixed,
  get: async () => data ? { size: 500, json: async () => data } : null,
} });

test('published version retains website size/date and supplies the pinned download', async () => {
  const { onRequestGet } = await moduleFor('web/functions/api/apk.js');
  const response = await onRequestGet({ env: envFor(release) });
  const body = await response.json();
  assert.equal(body.updateAvailable, true);
  assert.equal(body.versionCode, 29);
  assert.equal(body.uploaded, apk.uploaded.toISOString());
  assert.equal(body.downloadUrl, 'https://viajaconintu.pages.dev/descargar?versionCode=29');
  assert.equal(response.headers.get('cache-control'), 'no-store');
});

test('unfinished publication, invalid metadata or missing fixed APK never announces an update', async () => {
  const { onRequestGet } = await moduleFor('web/functions/api/apk.js');
  for (const data of [null, { ...release, apkUploaded: 'older' }, { ...release, size: 1 },
    { ...release, packageName: 'foreign.app' }, { ...release, versionCode: 0 }, { ...release, sha256: 'invalid' }]) {
    const body = await (await onRequestGet({ env: envFor(data) })).json();
    assert.equal(body.available, true);
    assert.equal(body.updateAvailable, false);
    assert.equal(body.versionCode, undefined);
  }
  assert.equal((await (await onRequestGet({ env: envFor(release, null) })).json()).updateAvailable, false);
});

test('missing APK is unavailable', async () => {
  const { onRequestGet } = await moduleFor('web/functions/api/apk.js');
  const response = await onRequestGet({ env: { APK: { head: async () => null } } });
  assert.equal(response.status, 404);
  assert.deepEqual(await response.json(), { available: false });
});

test('version downloads remain fixed while the ordinary website downloads the current alias', async () => {
  const { onRequestGet, onRequestHead } = await moduleFor('web/functions/descargar.js');
  const keys = [];
  const env = { APK: {
    get: async key => { keys.push(key); return { ...apk, body: 'apk' }; },
    head: async key => { keys.push(key); return apk; },
  } };
  assert.equal((await onRequestGet({ request: new Request('https://test/descargar?versionCode=29'), env })).status, 200);
  assert.equal((await onRequestHead({ request: new Request('https://test/descargar?versionCode=29'), env })).status, 200);
  await onRequestGet({ request: new Request('https://test/descargar?v=1.28-qa'), env });
  assert.deepEqual(keys, ['releases/29/intu.apk', 'releases/29/intu.apk', 'intu.apk']);
});

test('invalid download version never reaches R2; Range still resumes the fixed APK', async () => {
  const { onRequestGet } = await moduleFor('web/functions/descargar.js');
  for (const code of ['0', '-1', '../x', '2147483648', '29.0']) {
    const response = await onRequestGet({ request: new Request('https://test/descargar?versionCode=' + encodeURIComponent(code)),
      env: { APK: { get: () => { throw Error('Unexpected R2 access'); } } } });
    assert.equal(response.status, 400);
  }
  const response = await onRequestGet({ request: new Request('https://test/descargar?versionCode=29', { headers: { range: 'bytes=2-4' } }),
    env: { APK: { get: async (key, options) => {
      assert.equal(key, 'releases/29/intu.apk'); assert.equal(options.range.get('range'), 'bytes=2-4');
      return { ...apk, body: 'apk', range: { offset: 2, length: 3 } };
    } } } });
  assert.equal(response.status, 206);
  assert.equal(response.headers.get('content-range'), 'bytes 2-4/106000000');
});
