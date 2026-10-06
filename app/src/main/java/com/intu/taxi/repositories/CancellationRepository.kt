package com.intu.taxi.repositories

import com.intu.taxi.data.CancellationPreview
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import org.json.JSONObject

/** Pausa por cancelaciones de un rol (pasajero o conductor). */
data class CancellationStanding(val strikes: Int, val limit: Int, val blockedUntil: String?)

class CancellationRepository {
    suspend fun preview(rideId: String, reason: String): CancellationPreview = CancellationPreview.parse(
        SupabaseApi.rpc("cancellation_preview", JSONObject().put("p_ride_id", rideId).put("p_reason", reason)))

    /** Devuelve el estado del viaje después de cancelar. */
    suspend fun cancel(rideId: String, reason: String, note: String?): String =
        SupabaseApi.rpc("cancel_ride_with_reason", JSONObject().put("p_ride_id", rideId).put("p_reason", reason)
            .put("p_note", note?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL)).str("status")

    suspend fun driverStanding(): CancellationStanding = standing("driver")

    private suspend fun standing(role: String): CancellationStanding {
        val json = SupabaseApi.rpc("my_cancellation_status").optJSONObject(role) ?: JSONObject()
        return CancellationStanding(json.optInt("strikes", 0), json.optInt("limit", 3),
            json.str("blocked_until").ifBlank { null })
    }
}
