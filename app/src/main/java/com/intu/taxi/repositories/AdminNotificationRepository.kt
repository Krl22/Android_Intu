package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.push.AdminNotificationPreferences
import org.json.JSONObject

class AdminNotificationRepository {
    suspend fun get(timeoutMillis: Long? = null): AdminNotificationPreferences = parse(
        SupabaseApi.request("POST", "rpc/admin_get_notification_preferences", timeoutMillis = timeoutMillis))

    suspend fun save(value: AdminNotificationPreferences): AdminNotificationPreferences = parse(
        SupabaseApi.request("POST", "rpc/admin_set_notification_preferences", JSONObject()
            .put("p_enabled", value.enabled).put("p_new_users", value.newUsers)
            .put("p_ride_requests", value.rideRequests).put("p_driver_applications", value.driverApplications)
            .put("p_bug_reports", value.bugReports)))

    private fun parse(text: String): AdminNotificationPreferences = JSONObject(text).let {
        AdminNotificationPreferences(it.getBoolean("enabled"), it.getBoolean("new_users"),
            it.getBoolean("ride_requests"), it.getBoolean("driver_applications"), it.getBoolean("bug_reports"))
    }
}
