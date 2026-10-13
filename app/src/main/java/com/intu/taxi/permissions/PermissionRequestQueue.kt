package com.intu.taxi.permissions

enum class AppPermission { LOCATION, NOTIFICATIONS }

/** Only an answered system dialog counts as asked; a cancelled request remains pending. */
data class PermissionRequestQueue(
    val locationAsked: Boolean = false,
    val notificationsAsked: Boolean = false,
    val inFlight: AppPermission? = null,
    val queued: AppPermission? = null,
    val waitForResume: Boolean = false,
    val manualRequest: Boolean = false,
) {
    fun nextRequest(
        locationGranted: Boolean,
        notificationsGranted: Boolean,
        autoRequest: Boolean,
        needsLocation: Boolean,
        resumed: Boolean,
    ): AppPermission? {
        if (!resumed || inFlight != null || waitForResume) return null
        queued?.let { return if (manualRequest || autoRequest) it else null }
        if (!autoRequest) return null
        if (needsLocation && !locationGranted && !locationAsked) return AppPermission.LOCATION
        if (!notificationsGranted && !notificationsAsked) return AppPermission.NOTIFICATIONS
        return null
    }

    fun request(permission: AppPermission) = if (inFlight == permission) copy(manualRequest = true)
        else copy(queued = permission, waitForResume = false, manualRequest = true)

    fun launch(permission: AppPermission): PermissionRequestQueue {
        check(inFlight == null) { "A permission request is already in progress" }
        return copy(inFlight = permission, queued = null)
    }

    fun complete(cancelled: Boolean): PermissionRequestQueue {
        val permission = inFlight ?: return this
        return if (cancelled) copy(inFlight = null, queued = queued ?: permission, waitForResume = true)
        else copy(
            inFlight = null,
            manualRequest = queued != null,
            locationAsked = locationAsked || permission == AppPermission.LOCATION,
            notificationsAsked = notificationsAsked || permission == AppPermission.NOTIFICATIONS,
        )
    }

    fun resumed() = copy(waitForResume = false)
}
