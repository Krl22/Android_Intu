package com.intu.taxi.push

enum class AdminActivityType(val key: String) {
    NEW_USER("new_user"), RIDE_REQUEST("ride_request"),
    DRIVER_APPLICATION("driver_application"), BUG_REPORT("bug_report");
    companion object { fun fromKey(key: String?) = entries.firstOrNull { it.key == key } }
}

data class AdminNotificationPreferences(
    val enabled: Boolean = true,
    val newUsers: Boolean = true,
    val rideRequests: Boolean = true,
    val driverApplications: Boolean = true,
    val bugReports: Boolean = true
) {
    fun allows(type: AdminActivityType): Boolean = enabled && when (type) {
        AdminActivityType.NEW_USER -> newUsers
        AdminActivityType.RIDE_REQUEST -> rideRequests
        AdminActivityType.DRIVER_APPLICATION -> driverApplications
        AdminActivityType.BUG_REPORT -> bugReports
    }
}

data class AdminActivityMessage(
    val type: AdminActivityType, val eventId: String, val recipientUid: String,
    val title: String, val body: String
) {
    companion object {
        fun parse(data: Map<String, String>): AdminActivityMessage? {
            if (data["kind"] != "admin_activity") return null
            val type = AdminActivityType.fromKey(data["eventType"]) ?: return null
            val id = data["eventId"]?.takeIf { it.isNotBlank() && it.length <= 256 } ?: return null
            val uid = data["recipientUid"]?.takeIf { it.isNotBlank() && it.length <= 128 } ?: return null
            val title = data["title"]?.takeIf { it.isNotBlank() }?.take(120) ?: return null
            return AdminActivityMessage(type, id, uid, title, data["body"].orEmpty().take(240))
        }
    }
}

/** The server getter checks admin membership. An account change during that request drops the push. */
suspend fun canDisplayAdminActivity(message: AdminActivityMessage, currentUid: () -> String?,
    preferences: suspend () -> AdminNotificationPreferences): Boolean {
    if (currentUid() != message.recipientUid) return false
    val settings = preferences()
    return currentUid() == message.recipientUid && settings.allows(message.type)
}
