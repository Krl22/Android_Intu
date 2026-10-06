package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import org.json.JSONArray
import org.json.JSONObject

/** Calificaciones visibles y reglas de cancelación (solo admins). */
data class TripPolicy(
    val showDriverRating: Boolean,
    val showRiderRating: Boolean,
    val penaltiesEnabled: Boolean,
    val freeCancelSeconds: Int,
    val strikeLimit: Int,
    val strikeWindowDays: Int,
    val excusedLimit: Int,
    val firstBlockMinutes: Int,
    val repeatBlockMinutes: Int,
    val noShowWaitSeconds: Int,
    val noShowRadiusM: Int,
    val maxRequestsPer10Min: Int
) {
    companion object {
        fun parse(json: JSONObject) = TripPolicy(
            showDriverRating = json.optBoolean("show_driver_rating"),
            showRiderRating = json.optBoolean("show_rider_rating"),
            penaltiesEnabled = json.optBoolean("cancellation_penalties_enabled"),
            freeCancelSeconds = json.optInt("free_cancel_seconds"),
            strikeLimit = json.optInt("strike_limit"),
            strikeWindowDays = json.optInt("strike_window_days"),
            excusedLimit = json.optInt("excused_limit"),
            firstBlockMinutes = json.optInt("first_block_minutes"),
            repeatBlockMinutes = json.optInt("repeat_block_minutes"),
            noShowWaitSeconds = json.optInt("no_show_wait_seconds"),
            noShowRadiusM = json.optInt("no_show_radius_m"),
            maxRequestsPer10Min = json.optInt("max_requests_per_10_min")
        )
    }
}

data class CancellationPerson(
    val userId: String, val role: String, val name: String,
    val counted: Int, val excused: Int, val total: Int, val reports: Int, val blockedUntil: String?
)

data class CancellationReport(
    val rideId: String, val createdAt: String, val role: String, val reason: String,
    val note: String, val userName: String, val reportedName: String
)

data class CancellationOverview(val people: List<CancellationPerson>, val reports: List<CancellationReport>)

data class AdminChatSummary(
    val rideId: String, val riderName: String, val driverName: String, val messages: Int, val lastMessageAt: String
)

data class AdminChatMessage(val role: String, val senderName: String, val body: String, val createdAt: String)

class TripPolicyRepository {
    suspend fun get(): TripPolicy = TripPolicy.parse(SupabaseApi.rpc("admin_get_trip_policy"))

    /** Cambia solo las claves enviadas, p. ej. {"show_driver_rating": true}. */
    suspend fun save(changes: JSONObject): TripPolicy =
        TripPolicy.parse(SupabaseApi.rpc("admin_set_trip_policy", JSONObject().put("p_settings", changes)))

    suspend fun overview(days: Int = 7): CancellationOverview {
        val json = SupabaseApi.rpc("admin_cancellation_overview", JSONObject().put("p_days", days))
        return CancellationOverview(
            people = json.optJSONArray("people").objects().map {
                CancellationPerson(it.str("user_id"), it.str("role"), it.str("name", "Sin nombre"),
                    it.optInt("counted"), it.optInt("excused"), it.optInt("total"), it.optInt("reports"),
                    it.str("blocked_until").ifBlank { null })
            },
            reports = json.optJSONArray("reports").objects().map {
                CancellationReport(it.str("ride_id"), it.str("created_at"), it.str("role"), it.str("reason_code"),
                    it.str("note"), it.str("user_name", "Sin nombre"), it.str("reported_name", "Sin nombre"))
            }
        )
    }

    suspend fun liftBlock(userId: String, role: String) {
        SupabaseApi.rpc("admin_lift_cancellation_block", JSONObject().put("p_user_id", userId).put("p_role", role))
    }

    suspend fun recentChats(): List<AdminChatSummary> =
        SupabaseApi.rpcRows("admin_recent_ride_chats", JSONObject().put("p_days", 30)).objects().map {
            AdminChatSummary(it.str("ride_id"), it.str("rider_name", "Pasajero"), it.str("driver_name", "Conductor"),
                it.optInt("messages"), it.str("last_message_at"))
        }

    suspend fun chat(rideId: String): List<AdminChatMessage> =
        SupabaseApi.rpcRows("admin_ride_chat", JSONObject().put("p_ride_id", rideId)).objects().map {
            AdminChatMessage(it.str("role"), it.str("sender_name"), it.str("body"), it.str("created_at"))
        }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
