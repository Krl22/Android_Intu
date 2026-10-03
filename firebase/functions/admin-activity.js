const EVENT_TYPES = new Set(['new_user', 'ride_request', 'driver_application', 'bug_report']);

// Data-only push: the app verifies the active account and preferences before displaying it.
// Unlike ride updates, independent events must not replace each other in the notification tray.
function adminActivityMessage(payload, tokens) {
  if (!EVENT_TYPES.has(payload.eventType) || typeof payload.eventId !== 'string' ||
      !payload.eventId || payload.eventId.length > 256 || typeof payload.recipientUid !== 'string' ||
      !payload.recipientUid || payload.recipientUid.length > 128 || !payload.title) {
    throw new Error('invalid_admin_activity');
  }
  return {
    tokens,
    data: {
      kind: 'admin_activity', eventType: payload.eventType, eventId: payload.eventId,
      recipientUid: payload.recipientUid, title: String(payload.title).slice(0, 120),
      body: String(payload.body || '').slice(0, 240),
    },
    android: { priority: 'high', ttl: 10 * 60 * 1000 },
  };
}
module.exports = { adminActivityMessage };
