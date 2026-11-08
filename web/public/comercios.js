(() => {
  const form = document.getElementById('business-form');
  const done = document.getElementById('form-done');
  const error = document.getElementById('form-error');
  const submit = document.getElementById('submit');
  const locate = document.getElementById('locate');
  const locationStatus = document.getElementById('location-status');
  let point = null;

  const setLocation = (message, kind) => {
    locationStatus.textContent = message;
    locationStatus.className = 'location-status' + (kind ? ' ' + kind : '');
  };

  locate.addEventListener('click', () => {
    if (!('geolocation' in navigator)) { setLocation('Tu navegador no permite compartir la ubicación. La coordinamos contigo.', 'error'); return; }
    locate.disabled = true;
    setLocation('Buscando tu ubicación…');
    navigator.geolocation.getCurrentPosition(position => {
      locate.disabled = false;
      const {latitude, longitude, accuracy} = position.coords;
      if (latitude < -18.5 || latitude > 0.1 || longitude < -81.5 || longitude > -68.5) {
        point = null; setLocation('Esa ubicación está fuera del Perú. La coordinamos contigo.', 'error'); return;
      }
      point = {latitude, longitude};
      setLocation(accuracy > 150
        ? 'Ubicación guardada, pero es poco precisa. Igual la confirmaremos contigo.'
        : 'Ubicación del negocio guardada ✓', 'ok');
      locate.textContent = 'Volver a tomar la ubicación';
    }, () => {
      locate.disabled = false;
      setLocation('No pudimos obtener la ubicación. Puedes enviar el formulario igual; la coordinamos contigo.', 'error');
    }, {enableHighAccuracy: true, timeout: 15000, maximumAge: 0});
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    error.textContent = '';
    const value = name => form.elements[name]?.value ?? '';
    const payload = {
      name: value('name'), category: value('category'), locality: value('locality'),
      description: value('description'), address: value('address'),
      contactName: value('contactName'), phone: value('phone'), showPhone: form.elements.showPhone.checked,
      email: value('email'), socialUrl: value('socialUrl'),
      interest: form.querySelector('input[name="interest"]:checked')?.value ?? '',
      consent: document.getElementById('consent').checked, website: value('website'),
      ...(point ?? {}),
    };
    // Mensajes rápidos en el navegador; el servidor vuelve a validar todo.
    if (payload.name.trim().length < 2) return fail('Escribe el nombre del negocio.', 'name');
    if (!payload.category) return fail('Elige el tipo de negocio.', 'category');
    if (payload.contactName.trim().length < 2) return fail('Escribe tu nombre.', 'contact-name');
    if (!/^\+?[0-9]{6,15}$/.test(payload.phone.replace(/[\s()-]/g, ''))) return fail('Escribe un número de celular o WhatsApp válido.', 'phone');
    if (!payload.consent) return fail('Marca la casilla para aceptar que te contactemos.', 'consent');
    submit.disabled = true; submit.textContent = 'Enviando…';
    try {
      const response = await fetch('/api/comercios', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)});
      const body = await response.json().catch(() => ({}));
      if (!response.ok || !body.saved) throw new Error(body.error || 'No pudimos enviar tus datos. Intenta nuevamente.');
      form.hidden = true; done.hidden = false; done.focus();
      window.scrollTo({top: done.offsetTop - 80, behavior: 'smooth'});
    } catch (e) {
      error.textContent = e.message === 'Failed to fetch' ? 'Sin conexión. Revisa tu internet e intenta de nuevo.' : e.message;
    } finally {
      submit.disabled = false; submit.textContent = 'Enviar mis datos';
    }
  });

  function fail(message, id) {
    error.textContent = message;
    document.getElementById(id)?.focus();
  }

  document.getElementById('another').addEventListener('click', () => {
    form.reset(); point = null; setLocation(''); locate.textContent = 'Usar mi ubicación actual';
    done.hidden = true; form.hidden = false; document.getElementById('name').focus();
  });
})();
