import test from 'node:test';
import assert from 'node:assert/strict';
import {parseBusiness, onRequest} from '../functions/api/comercios.js';
import {sanitizeListing, installUrl} from '../functions/api/guia.js';

const valid = {
  name: '  Hostal  El Cafetal ', category: 'hospedaje', locality: 'Satipo', description: 'Cuartos\ncon baño',
  address: 'Jr. Colonos 123', contactName: 'Ana', phone: '987 654 321', showPhone: true, email: 'Ana@Correo.pe',
  socialUrl: 'facebook.com/elcafetal', interest: 'destacado', consent: true, latitude: -11.2541234567, longitude: -74.6382,
};

test('normaliza un registro válido', () => {
  const {data, error} = parseBusiness(valid);
  assert.equal(error, undefined);
  assert.equal(data.name, 'Hostal El Cafetal');
  assert.equal(data.description, 'Cuartos con baño');
  assert.equal(data.phone, '987654321');
  assert.equal(data.email, 'ana@correo.pe');
  assert.equal(data.social_url, 'https://facebook.com/elcafetal');
  assert.equal(data.latitude, -11.254123);
  assert.equal(data.show_phone, true);
});

test('rechaza datos obligatorios faltantes o inválidos', () => {
  assert.match(parseBusiness({...valid, name: 'a'}).error, /nombre del negocio/);
  assert.match(parseBusiness({...valid, category: 'casino'}).error, /tipo/);
  assert.match(parseBusiness({...valid, phone: '12'}).error, /celular/);
  assert.match(parseBusiness({...valid, email: 'no-es-correo'}).error, /correo/);
  assert.match(parseBusiness({...valid, consent: false}).error, /Acepta/);
  assert.match(parseBusiness({...valid, interest: 'vip'}).error, /aparecer/);
  assert.match(parseBusiness({...valid, socialUrl: 'javascript:alert(1)'}).error, /enlace/);
  assert.ok(parseBusiness([]).error);
});

test('descarta ubicaciones fuera del Perú sin rechazar el registro', () => {
  const {data} = parseBusiness({...valid, latitude: 40.7, longitude: -74});
  assert.equal(data.latitude, undefined);
  assert.equal(data.longitude, undefined);
});

const request = (body, headers = {}) => new Request('https://viajaconintu.pages.dev/api/comercios', {
  method: 'POST', body: JSON.stringify(body),
  headers: {'Content-Type': 'application/json', Origin: 'https://viajaconintu.pages.dev', 'CF-Connecting-IP': '1.2.3.4', ...headers},
});
const env = {TESTER_GATEWAY_KEY: 'a'.repeat(64)};

test('el endpoint exige origen propio y no llama a Supabase ante el honeypot', async () => {
  assert.equal((await onRequest({request: request(valid, {Origin: 'https://evil.example'}), env})).status, 403);
  let called = false;
  const original = globalThis.fetch;
  globalThis.fetch = async () => { called = true; return new Response('{}'); };
  try {
    const response = await onRequest({request: request({...valid, website: 'spam'}), env});
    assert.equal(response.status, 200);
    assert.equal(called, false);
  } finally { globalThis.fetch = original; }
});

test('envía a Supabase con la clave y un resumen de IP, nunca la IP', async () => {
  const original = globalThis.fetch;
  let sent;
  globalThis.fetch = async (url, init) => { sent = {url, body: JSON.parse(init.body)}; return new Response('{"saved":true}'); };
  try {
    const response = await onRequest({request: request(valid), env});
    assert.equal(response.status, 200);
    assert.match(sent.url, /rpc\/submit_guide_business$/);
    assert.match(sent.body.p_ip_hash, /^[a-f0-9]{64}$/);
    assert.ok(!JSON.stringify(sent.body).includes('1.2.3.4'));
    assert.equal(sent.body.p_data.consent, true);
  } finally { globalThis.fetch = original; }
});

test('sin clave de pasarela el formulario queda deshabilitado', async () => {
  assert.equal((await onRequest({request: request(valid), env: {}})).status, 503);
});

test('el listado público solo expone campos permitidos', () => {
  const row = sanitizeListing({id: 'x', name: 'Hostal', category: 'hospedaje', latitude: -11.2, longitude: -74.6,
    phone: '987654321', social_url: 'javascript:alert(1)', contact_name: 'Ana', email: 'a@b.pe', featured: true});
  assert.equal(row.socialUrl, null);
  assert.equal(row.contact_name, undefined);
  assert.equal(row.email, undefined);
  assert.equal(row.featured, true);
  assert.equal(sanitizeListing({id: 'x', name: 'Sin punto', latitude: null, longitude: null}), null);
});

test('installUrl solo acepta la web de Intu o Google Play', () => {
  assert.equal(installUrl({}), 'https://viajaconintu.pages.dev/#instalar');
  assert.equal(installUrl({GUIDE_INSTALL_URL: 'https://play.google.com/store/apps/details?id=com.intu.taxi'}),
    'https://play.google.com/store/apps/details?id=com.intu.taxi');
  assert.equal(installUrl({GUIDE_INSTALL_URL: 'https://evil.example/app.apk'}), 'https://viajaconintu.pages.dev/#instalar');
});
