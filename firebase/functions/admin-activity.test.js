const test = require('node:test');
const assert = require('node:assert/strict');
const { adminActivityMessage } = require('./admin-activity');

const payload = { eventType: 'ride_request', eventId: 'ride_request:qa1', recipientUid: 'admin-qa', title: 'Nueva solicitud', body: 'QA' };
test('admin push is data-only, account-bound and high priority', () => {
  const message = adminActivityMessage(payload, ['qa-token']);
  assert.equal(message.notification, undefined);
  assert.deepEqual(message.tokens, ['qa-token']);
  assert.equal(message.data.recipientUid, 'admin-qa');
  assert.equal(message.data.eventId, payload.eventId);
  assert.equal(message.android.priority, 'high');
  assert.equal(message.android.ttl, 600000);
});
test('all supported events survive independently and oversized copy is bounded', () => {
  for (const eventType of ['new_user','ride_request','driver_application','bug_report']) {
    const result = adminActivityMessage({ ...payload, eventType, title: 'a'.repeat(1000), body: 'b'.repeat(1000) }, []);
    assert.equal(result.data.eventType, eventType);
    assert.equal(result.data.title.length, 120);
    assert.equal(result.data.body.length, 240);
  }
});
test('unknown events, missing recipients and invalid identifiers are rejected', () => {
  for (const override of [{ eventType:'login' }, { recipientUid:'' }, { eventId:'' }, { eventId:'x'.repeat(257) }, { title:'' }]) {
    assert.throws(() => adminActivityMessage({ ...payload, ...override }, []), /invalid_admin_activity/);
  }
});
