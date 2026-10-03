// Genera páginas revisables. No publica ni modifica web/public.
const fs = require('node:fs');
const path = require('node:path');

const required = [
  'responsibleName', 'responsibleAddress', 'supportEmail', 'profileRetention',
  'rideRetention', 'reportRetention', 'deletionTimeframe', 'retainedAfterDeletion', 'effectiveDate',
];
function validate(config) {
  const missing = required.filter(key => typeof config[key] !== 'string' || !config[key].trim());
  if (missing.length) throw new Error(`Falta definir: ${missing.join(', ')}. No se generaron páginas finales.`);
  if (!/^[^\s@<>]+@[^\s@<>]+\.[^\s@<>]+$/.test(config.supportEmail)) throw new Error('El correo de soporte no es válido.');
  return Object.fromEntries(required.map(key => [key, config[key].trim()]));
}
function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
}
function render(template, config) {
  const data = { ...validate(config) };
  data.deletionMailto = `mailto:${data.supportEmail}?subject=${encodeURIComponent('Eliminar mi cuenta de Intu')}&body=${encodeURIComponent('Solicito eliminar mi cuenta de Intu y mis datos personales asociados.\nTeléfono o correo de acceso: \n')}`;
  return template.replace(/\{\{(\w+)\}\}/g, (_, key) => {
    if (!(key in data)) throw new Error(`Campo desconocido: ${key}`);
    return escapeHtml(data[key]);
  });
}
function main() {
  const configFile = process.argv[2];
  if (!configFile) throw new Error('Uso: node scripts/prepare-play-legal.cjs RUTA_CONFIG_JSON');
  const config = validate(JSON.parse(fs.readFileSync(configFile, 'utf8').replace(/^\uFEFF/, '')));
  const root = path.resolve(__dirname, '..');
  const output = path.join(root, 'build', 'play-legal');
  const rendered = ['privacidad', 'eliminar-cuenta'].map(name => [name,
    render(fs.readFileSync(path.join(root, 'web', 'legal', `${name}.template.html`), 'utf8'), config)]);
  fs.mkdirSync(output, { recursive: true });
  for (const [name, html] of rendered) fs.writeFileSync(path.join(output, `${name}.html`), html);
  fs.copyFileSync(path.join(root, 'web', 'legal', 'style.css'), path.join(output, 'legal.css'));
  console.log(`Páginas preparadas para revisión: ${output}. Confirma que el buzón de soporte recibe correos antes de publicarlas.`);
}
module.exports = { validate, render };
if (require.main === module) {
  try { main(); } catch (e) { console.error(e.message); process.exitCode = 1; }
}
