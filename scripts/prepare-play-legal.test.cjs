const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { validate, render } = require('./prepare-play-legal.cjs');

test('no genera una política final si faltan las decisiones del responsable', () => {
  const config = JSON.parse(fs.readFileSync(path.join(__dirname, '../web/legal/config.example.json'), 'utf8'));
  assert.throws(() => validate(config), /Falta definir/);
});
test('contacto revisable y correo de eliminación con contenido escapado', () => {
  const config = Object.fromEntries(['responsibleName', 'responsibleAddress', 'supportEmail',
    'profileRetention', 'rideRetention', 'reportRetention', 'deletionTimeframe',
    'retainedAfterDeletion', 'effectiveDate'].map(key => [key, 'Definido para pruebas']));
  config.responsibleName = '<script>prueba</script>';
  config.supportEmail = 'soporte@example.com';
  const output = render('{{responsibleName}} <a href="{{deletionMailto}}">correo</a>', config);
  assert.ok(!output.includes('<script>'));
  assert.match(output, /&lt;script&gt;/);
  assert.match(output, /mailto:soporte@example.com\?subject=/);
  assert.ok(!output.includes('{{'));
  config.supportEmail = '" onclick="alert(1)';
  assert.throws(() => validate(config), /correo/);
});
