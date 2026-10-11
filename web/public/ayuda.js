// Chat de ayuda de la página. La conversación vive solo en esta pestaña; el botón aparece únicamente
// si el administrador activó el asistente para la web.
(() => {
  const launcher = document.getElementById('help-launcher');
  const panel = document.getElementById('help-panel');
  const list = document.getElementById('help-messages');
  const form = document.getElementById('help-form');
  const input = document.getElementById('help-input');
  const send = document.getElementById('help-send');
  const suggestions = document.getElementById('help-suggestions');
  if (!launcher || !panel) return;
  const messages = [];
  let busy = false;

  function bubble(role, text) {
    const item = document.createElement('div');
    item.className = `help-msg help-${role}`;
    item.textContent = text;
    list.appendChild(item);
    list.scrollTop = list.scrollHeight;
    return item;
  }

  function open() {
    panel.hidden = false;
    launcher.setAttribute('aria-expanded', 'true');
    document.body.classList.add('help-open');
    input.focus();
  }
  function close() {
    panel.hidden = true;
    launcher.setAttribute('aria-expanded', 'false');
    document.body.classList.remove('help-open');
    launcher.focus();
  }

  async function ask(text) {
    const question = text.trim().slice(0, 1000);
    if (!question || busy) return;
    busy = true; send.disabled = true; suggestions.hidden = true;
    messages.push({role: 'user', content: question});
    bubble('user', question);
    input.value = '';
    const typing = bubble('assistant', 'Escribiendo…');
    typing.classList.add('help-typing');
    try {
      const response = await fetch('/api/ayuda', {
        method: 'POST', headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({messages: messages.slice(-12)}),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok || typeof data.reply !== 'string') throw new Error(data.error || 'No pudimos responder ahora. Intenta de nuevo en unos minutos.');
      typing.classList.remove('help-typing');
      typing.textContent = data.reply;
      messages.push({role: 'assistant', content: data.reply});
    } catch (e) {
      // La pregunta sin respuesta se quita para que la conversación siga alternando
      messages.pop();
      typing.classList.remove('help-typing');
      typing.classList.add('help-error');
      typing.textContent = e.message;
    } finally {
      busy = false; send.disabled = false; list.scrollTop = list.scrollHeight;
    }
  }

  launcher.addEventListener('click', () => panel.hidden ? open() : close());
  document.getElementById('help-close').addEventListener('click', close);
  panel.addEventListener('keydown', event => { if (event.key === 'Escape') close(); });
  form.addEventListener('submit', event => { event.preventDefault(); ask(input.value); });
  suggestions.querySelectorAll('button').forEach(button => button.addEventListener('click', () => ask(button.textContent)));

  fetch('/api/ayuda', {cache: 'no-store'}).then(r => r.json()).then(data => {
    if (data.enabled === true) launcher.hidden = false;
  }).catch(() => {});
})();
