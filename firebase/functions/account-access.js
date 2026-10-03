'use strict';

/** Read-only Firebase identity lookup; authorization is checked against the live admin registry. */
function createAccountAccessHandler({ isAdmin, getUsers, HttpsError }) {
  return async (request) => {
    if (!request.auth) throw new HttpsError('unauthenticated', 'Inicia sesión para continuar.');
    const bearer = (request.rawRequest?.headers?.authorization || '').replace(/^Bearer\s+/i, '');
    if (!bearer) throw new HttpsError('unauthenticated', 'Inicia sesión para continuar.');
    let allowed;
    try { allowed = await isAdmin(bearer); }
    catch (_) { throw new HttpsError('unavailable', 'No se pudo comprobar el permiso de administrador.'); }
    if (allowed !== true) throw new HttpsError('permission-denied', 'Solo un administrador puede ver los accesos de otras cuentas.');

    const ids = request.data?.userIds;
    if (!Array.isArray(ids) || ids.length < 1 || ids.length > 100 ||
        ids.some((id) => typeof id !== 'string' || !id.trim() || id.length > 128)) {
      throw new HttpsError('invalid-argument', 'Selecciona entre 1 y 100 cuentas válidas.');
    }
    const uniqueIds = [...new Set(ids)];
    let result;
    try { result = await getUsers(uniqueIds.map((uid) => ({ uid }))); }
    catch (_) { throw new HttpsError('unavailable', 'No se pudieron consultar los accesos. Vuelve a actualizar el panel.'); }
    const users = new Map(result.users.map((user) => [user.uid, user]));
    const missing = new Set(result.notFound.map((entry) => entry.uid));
    return { accounts: uniqueIds.map((uid) => {
      const user = users.get(uid);
      if (!user) return { uid, status: missing.has(uid) ? 'missing' : 'unavailable' };
      // Explicit allowlist: never return password hashes, tokens, claims or a full UserRecord.
      return {
        uid, status: 'found', email: user.email || null, emailVerified: user.emailVerified === true,
        phone: user.phoneNumber || null, disabled: user.disabled === true,
        providers: user.providerData.map((provider) => ({
          id: provider.providerId, email: provider.email || null, phone: provider.phoneNumber || null,
        })),
      };
    }) };
  };
}

module.exports = { createAccountAccessHandler };
