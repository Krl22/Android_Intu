(() => {
  const CATEGORIES = {
    hospedaje: 'Hospedaje', restaurante: 'Comida', turismo: 'Paseos y turismo',
    tienda: 'Tiendas', servicios: 'Servicios', otro: 'Otros',
  };
  const grid = document.getElementById('grid');
  const status = document.getElementById('status');
  const search = document.getElementById('search');
  const chips = document.getElementById('chips');
  const template = document.getElementById('card-template');
  const isAndroid = /Android/i.test(navigator.userAgent);
  let businesses = [];
  let installUrl = 'https://viajaconintu.pages.dev/#instalar';
  let category = 'all';

  /**
   * Android: el enlace intent:// abre Intu con el destino elegido; si no está instalada,
   * el navegador va a installUrl (Play o la sección de instalación de la web).
   */
  function goLink(b) {
    const query = 'lat=' + b.latitude.toFixed(6) + '&lng=' + b.longitude.toFixed(6) + '&nombre=' + encodeURIComponent(b.name);
    if (isAndroid) {
      return 'intent://destino?' + query + '#Intent;scheme=intu;package=com.intu.taxi;S.browser_fallback_url=' +
        encodeURIComponent(installUrl) + ';end';
    }
    return 'https://www.google.com/maps/search/?api=1&query=' + b.latitude + ',' + b.longitude;
  }

  const normalize = text => (text || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');

  function renderChips() {
    const present = new Set(businesses.map(b => b.category));
    const entries = [['all', 'Todo'], ...Object.entries(CATEGORIES).filter(([key]) => present.has(key))];
    chips.replaceChildren(...entries.map(([key, label]) => {
      const chip = document.createElement('button');
      chip.type = 'button'; chip.className = 'chip'; chip.textContent = label;
      chip.setAttribute('aria-pressed', String(key === category));
      chip.addEventListener('click', () => { category = key; renderChips(); render(); });
      return chip;
    }));
  }

  function render() {
    const words = normalize(search.value).split(/\s+/).filter(Boolean);
    const visible = businesses.filter(b => (category === 'all' || b.category === category) &&
      words.every(word => normalize([b.name, b.description, b.address, b.locality, CATEGORIES[b.category]].join(' ')).includes(word)));
    grid.replaceChildren(...visible.map(card));
    if (!businesses.length) {
      status.innerHTML = '';
      const empty = document.createElement('div'); empty.className = 'empty';
      empty.innerHTML = '<h2>Muy pronto</h2><p class="muted">Estamos sumando los primeros hospedajes, restaurantes y paseos de Satipo.</p>' +
        '<a class="btn" href="/comercios">¿Tienes un negocio? Aparece aquí</a>';
      status.append(empty);
    } else {
      status.textContent = visible.length ? '' : 'No encontramos lugares con esa búsqueda.';
    }
  }

  function card(b) {
    const node = template.content.firstElementChild.cloneNode(true);
    if (b.featured) { node.classList.add('featured'); node.querySelector('[data-featured]').hidden = false; }
    node.querySelector('[data-category]').textContent = CATEGORIES[b.category] || 'Lugar';
    node.querySelector('[data-name]').textContent = b.name;
    const description = node.querySelector('[data-description]');
    if (b.description) description.textContent = b.description; else description.remove();
    node.querySelector('[data-place]').textContent = [b.address, b.locality].filter(Boolean).join(' · ');
    const go = node.querySelector('[data-go]');
    go.href = goLink(b);
    if (!isAndroid) go.textContent = 'Ver en el mapa';
    go.setAttribute('aria-label', (isAndroid ? 'Ir con Intu a ' : 'Ver en el mapa ') + b.name);
    if (b.phone) {
      const wa = node.querySelector('[data-whatsapp]');
      const digits = b.phone.replace(/^\+/, '');
      wa.href = 'https://wa.me/' + (digits.length === 9 && digits.startsWith('9') ? '51' + digits : digits) +
        '?text=' + encodeURIComponent('Hola, los vi en Descubre Satipo de Intu.');
      wa.hidden = false;
    }
    if (b.socialUrl) {
      const social = node.querySelector('[data-social]');
      social.href = b.socialUrl;
      const host = (() => { try { return new URL(b.socialUrl).hostname; } catch { return ''; } })();
      social.textContent = /facebook|fb\./.test(host) ? 'Facebook' : /instagram/.test(host) ? 'Instagram' : 'Web';
      social.hidden = false;
    }
    return node;
  }

  search.addEventListener('input', render);

  fetch('/api/guia').then(r => r.json().then(body => ({ok: r.ok, body}))).then(({ok, body}) => {
    if (!ok) throw new Error(body.error);
    businesses = Array.isArray(body.businesses) ? body.businesses : [];
    if (typeof body.installUrl === 'string') installUrl = body.installUrl;
    renderChips(); render();
  }).catch(e => {
    status.textContent = (e && e.message) || 'No pudimos cargar la guía. Revisa tu conexión y recarga la página.';
  });
})();
