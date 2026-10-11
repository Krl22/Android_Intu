(() => {
  const dialog = document.getElementById('tester-dialog');
  const form = document.getElementById('tester-form');
  const result = document.getElementById('tester-result');
  const notice = document.getElementById('tester-notice');
  const submit = document.getElementById('tester-submit');
  let ready = false;
  function showLinks(data) {
    const group = new URL(data.groupUrl); const play = new URL(data.playUrl);
    if (group.origin !== 'https://groups.google.com' || play.origin !== 'https://play.google.com') throw new Error('Los enlaces no están disponibles.');
    document.getElementById('tester-group').href = group.href;
    const playButton = document.getElementById('tester-play');
    playButton.href = play.href;
    playButton.hidden = data.playReady !== true;
    document.getElementById('tester-play-pending').hidden = data.playReady === true;
  }
  function configure(data) {
    ready = data.enabled === true;
    if (!ready) result.hidden = true;
    else if (!result.hidden) showLinks(data);
    notice.hidden = !result.hidden;
    notice.textContent = ready ? (data.playReady ? 'Deja tu correo y sigue los dos pasos para instalar desde Play. Puedes unirte libremente.' :
      'Ya puedes unirte al grupo. Estamos preparando la publicación de la prueba en Google Play; mientras tanto, puedes descargar el APK.') :
      'Estamos preparando las pruebas en Google Play. Mientras tanto, puedes descargar el APK desde esta página.';
    submit.disabled = !ready;
    form.hidden = !ready || !result.hidden;
  }
  async function refresh() {
    try { const response = await fetch('/api/testers',{cache:'no-store'}); configure(await response.json()); }
    catch { configure({enabled:false}); }
  }
  document.querySelectorAll('[data-testers]').forEach(button=>button.addEventListener('click',()=>{
    dialog.showModal(); refresh();
  }));
  document.getElementById('tester-close').addEventListener('click',()=>dialog.close());
  form.addEventListener('submit',async event=>{
    event.preventDefault(); if (!ready || submit.disabled) return;
    submit.disabled = true; submit.textContent = 'Enviando…';
    const error = document.getElementById('tester-error'); error.textContent = '';
    try {
      const response = await fetch('/api/testers',{
        method:'POST',headers:{'Content-Type':'application/json'},
        body:JSON.stringify({email:form.elements.email.value,consent:form.elements.consent.checked,website:form.elements.website.value}),
      });
      const data = await response.json();
      if (!response.ok || data.saved !== true) throw new Error(data.error || 'No se pudo enviar. Intenta nuevamente.');
      showLinks(data);
      form.hidden = true; notice.hidden = true; result.hidden = false;
      result.focus();
    } catch (e) { error.textContent = e.message; }
    finally { submit.disabled = !ready; submit.textContent = 'Continuar a Google Play'; }
  });
})();
