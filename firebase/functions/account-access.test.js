const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createAccountAccessHandler } = require('./account-access');
class HttpsError extends Error { constructor(code, message) { super(message); this.code = code; } }
const request = (ids = ['u1']) => ({ auth: { uid: 'admin' }, rawRequest: { headers: { authorization: 'Bearer verified-token' } }, data: { userIds: ids } });

test('unauthenticated and non-admin callers cannot read Firebase identities', async () => {
  let reads = 0;
  const handler = createAccountAccessHandler({ HttpsError, isAdmin: async () => false, getUsers: async () => { reads++; } });
  await assert.rejects(handler({}), { code: 'unauthenticated' });
  await assert.rejects(handler({ ...request(), rawRequest: { headers: {} } }), { code: 'unauthenticated' });
  await assert.rejects(handler(request()), { code: 'permission-denied' });
  assert.equal(reads, 0);
});
test('authorization failure is closed, even with a forged admin flag in client data', async () => {
  const handler = createAccountAccessHandler({ HttpsError, isAdmin: async () => { throw new Error('offline'); }, getUsers: () => assert.fail('must not read') });
  await assert.rejects(handler({ ...request(), data: { userIds: ['u1'], isAdmin: true } }), { code: 'unavailable' });
});
test('validates batch bounds and identifiers before any Firebase read', async () => {
  const handler = createAccountAccessHandler({ HttpsError, isAdmin: async () => true, getUsers: () => assert.fail('must not read') });
  for (const ids of [[], Array(101).fill('u1'), [''], [' '], [null], ['a'.repeat(129)]]) {
    await assert.rejects(handler(request(ids)), { code: 'invalid-argument' });
  }
});
test('joins by UID despite unordered results, distinguishes missing and limits returned fields', async () => {
  const handler = createAccountAccessHandler({ HttpsError, isAdmin: async (token) => token === 'verified-token',
    getUsers: async (ids) => {
      assert.deepEqual(ids, [{ uid: 'u1' }, { uid: 'missing' }, { uid: 'u2' }]);
      return { users: [
        { uid: 'u2', email: 'google@example.com', emailVerified: true, phoneNumber: '+51987654321', disabled: false,
          passwordHash: 'secret', customClaims: { admin: true }, providerData: [{ providerId: 'google.com', email: 'google@example.com', uid: 'google-secret' }, { providerId: 'phone', phoneNumber: '+51987654321' }] },
        { uid: 'u1', emailVerified: false, disabled: true, providerData: [{ providerId: 'phone', phoneNumber: '+15555550123' }] },
      ], notFound: [{ uid: 'missing' }] };
    } });
  const { accounts } = await handler(request(['u1', 'missing', 'u2', 'u1']));
  assert.deepEqual(accounts.map((account) => [account.uid, account.status]), [['u1', 'found'], ['missing', 'missing'], ['u2', 'found']]);
  assert.equal(accounts[2].providers[0].email, 'google@example.com');
  assert.equal(accounts[2].emailVerified, true);
  assert.equal(accounts[0].disabled, true);
  assert.equal(accounts[0].email, null);
  assert.ok(!JSON.stringify(accounts).includes('secret'));
  assert.ok(!JSON.stringify(accounts).includes('customClaims'));
});
test('Firebase failure is not represented as an unlinked account', async () => {
  const handler = createAccountAccessHandler({ HttpsError, isAdmin: async () => true, getUsers: async () => { throw new Error('service failure'); } });
  await assert.rejects(handler(request()), { code: 'unavailable' });
});
