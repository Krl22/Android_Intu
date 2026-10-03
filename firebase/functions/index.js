// Supabase acepta los tokens de Firebase solo si traen el claim `role: 'authenticated'`
// (así asigna el rol de Postgres `authenticated`, que es el que usan las políticas RLS).
// Estas funciones "blocking" lo agregan en cada registro e inicio de sesión.
// Requieren Firebase Authentication with Identity Platform (también necesario para MFA).
// Docs: https://supabase.com/docs/guides/auth/third-party/firebase-auth

const crypto = require('node:crypto');
const { beforeUserCreated, beforeUserSignedIn } = require('firebase-functions/v2/identity');
const { onCall, onRequest, HttpsError } = require('firebase-functions/v2/https');
const { defineSecret } = require('firebase-functions/params');
const logger = require('firebase-functions/logger');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getMessaging } = require('firebase-admin/messaging');
const { getStorage } = require('firebase-admin/storage');
const { createAccountAccessHandler } = require('./account-access');
const { adminActivityMessage } = require('./admin-activity');

initializeApp();

const supabaseClaims = { role: 'authenticated' };

exports.beforecreated = beforeUserCreated(() => ({ customClaims: supabaseClaims }));

exports.beforesignedin = beforeUserSignedIn(() => ({ customClaims: supabaseClaims }));

// Mismos valores públicos que usa la app (BuildConfig)
const SUPABASE_URL = 'https://vkguzpciwpfvaeyedepl.supabase.co';
const SUPABASE_PUBLISHABLE_KEY =
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZrZ3V6cGNpd3BmdmFleWVkZXBsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMwNzYxMjYsImV4cCI6MjA3ODY1MjEyNn0.gHosYEPeqBHMjkezz5b9wuMQ6-PRFONcYrUuO62TYBc';

/** Llama a una función RPC de Supabase. Con [bearer] actúa como ese usuario; sin él, como anónimo. */
async function supabaseRpc(name, body, bearer = SUPABASE_PUBLISHABLE_KEY) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/rpc/${name}`, {
    method: 'POST',
    headers: {
      apikey: SUPABASE_PUBLISHABLE_KEY,
      Authorization: `Bearer ${bearer}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) {
    let message = '';
    try {
      message = JSON.parse(text).message || '';
    } catch (_) {
      // respuesta sin JSON
    }
    const error = new Error(message || `supabase_${res.status}`);
    error.status = res.status;
    throw error;
  }
  return text;
}

// Datos de vinculación obtenidos de Firebase Auth, nunca inferidos del perfil de contacto.
exports.adminGetAccountAccess = onCall(createAccountAccessHandler({
  isAdmin: async (bearer) => JSON.parse(await supabaseRpc('is_admin', {}, bearer)) === true,
  getUsers: (identifiers) => getAuth().getUsers(identifiers),
  HttpsError,
}));

// ---------------------------------------------------------------------
// Avisos push de los viajes
// ---------------------------------------------------------------------

// Mismo valor que el secreto `push_webhook_secret` del Vault de Supabase
const pushWebhookSecret = defineSecret('PUSH_WEBHOOK_SECRET');

function sameSecret(received, expected) {
  const a = Buffer.from(String(received || ''));
  const b = Buffer.from(String(expected || ''));
  return a.length > 0 && a.length === b.length && crypto.timingSafeEqual(a, b);
}

// Tokens que FCM ya no acepta: la app se desinstaló o se borraron sus datos
const DEAD_TOKEN_CODES = new Set([
  'messaging/registration-token-not-registered',
  'messaging/invalid-registration-token',
  'messaging/invalid-argument',
]);

/**
 * La llama Supabase (trigger rides_notify_status con pg_net) cuando un viaje cambia de estado.
 * Cuerpo: { tokens, title, body, rideId, status }.
 */
exports.ridePush = onRequest({ secrets: [pushWebhookSecret], cors: false }, async (req, res) => {
  if (req.method !== 'POST' || !sameSecret(req.get('x-intu-secret'), pushWebhookSecret.value())) {
    res.status(403).send('forbidden');
    return;
  }
  const { tokens, title, body, rideId, status } = req.body || {};
  const list = Array.isArray(tokens) ? tokens.filter((t) => typeof t === 'string' && t).slice(0, 500) : [];
  if (list.length === 0 || !title) {
    res.status(400).send('bad_request');
    return;
  }

  let payload;
  if (req.body.kind === 'admin_activity') {
    try { payload = adminActivityMessage(req.body, list); }
    catch (_) { res.status(400).send('bad_request'); return; }
  } else payload = {
    tokens: list,
    notification: { title: String(title), body: String(body || '') },
    data: { rideId: String(rideId || ''), status: String(status || '') },
    android: {
      priority: 'high',
      ttl: 10 * 60 * 1000,
      notification: {
        channelId: 'ride_updates',
        icon: 'ic_stat_intu',
        color: '#08817E',
        // Un aviso por viaje: el nuevo estado reemplaza al anterior
        tag: String(rideId || 'ride'),
      },
    },
  };
  const result = await getMessaging().sendEachForMulticast(payload);

  const dead = list.filter((_, i) => {
    const r = result.responses[i];
    return !r.success && r.error && DEAD_TOKEN_CODES.has(r.error.code);
  });
  if (dead.length > 0) {
    try {
      await supabaseRpc('prune_device_tokens', { p_secret: pushWebhookSecret.value(), p_tokens: dead });
    } catch (e) {
      logger.warn('No se pudieron borrar tokens vencidos', e.message);
    }
  }
  res.json({ sent: result.successCount, failed: result.failureCount, pruned: dead.length });
});

// ---------------------------------------------------------------------
// Eliminar cuentas (panel de administración)
// ---------------------------------------------------------------------

/**
 * Borra una cuenta por completo: sus datos en Supabase (admin_delete_user comprueba que quien
 * llama es admin; los viajes quedan anónimos), su cuenta de Firebase Auth y su foto de perfil.
 * Los mensajes de error se muestran tal cual en la app.
 */
exports.adminDeleteUser = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Inicia sesión para continuar.');
  }
  const userId = request.data && request.data.userId;
  if (typeof userId !== 'string' || userId.length === 0 || userId.length > 128) {
    throw new HttpsError('invalid-argument', 'No se encontró esa cuenta.');
  }
  const idToken = (request.rawRequest.headers.authorization || '').replace(/^Bearer\s+/i, '');

  try {
    await supabaseRpc('admin_delete_user', { p_user_id: userId }, idToken);
  } catch (e) {
    if (e.message === 'not_admin') {
      throw new HttpsError('permission-denied', 'Solo un administrador puede hacer esto.');
    }
    if (e.message === 'last_admin') {
      throw new HttpsError('failed-precondition', 'Debe quedar al menos un administrador. Haz admin a otra cuenta primero.');
    }
    logger.error('admin_delete_user falló', { userId, error: e.message, status: e.status });
    throw new HttpsError('internal', 'No se pudo eliminar la cuenta. Intenta de nuevo.');
  }

  try {
    await getAuth().deleteUser(userId);
  } catch (e) {
    if (e.code !== 'auth/user-not-found') {
      logger.error('No se pudo borrar la cuenta de Firebase Auth', { userId, error: e.message });
      throw new HttpsError('internal', 'Se borraron los datos, pero no la cuenta de inicio de sesión. Bórrala en la consola de Firebase.');
    }
  }

  try {
    await getStorage().bucket().file(`avatars/${userId}.jpg`).delete({ ignoreNotFound: true });
  } catch (e) {
    logger.warn('No se pudo borrar la foto de perfil', { userId, error: e.message });
  }

  logger.info('Cuenta eliminada', { userId, by: request.auth.uid });
  return { deleted: true };
});
