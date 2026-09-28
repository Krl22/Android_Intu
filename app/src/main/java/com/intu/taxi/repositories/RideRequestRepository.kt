package com.intu.taxi.repositories

import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.auth.AuthRepository
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import com.intu.taxi.models.RideRequest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.time.Instant

class RideRequestRepository {
    suspend fun createRideRequest(
        originLatitude: Double, originLongitude: Double, originAddress: String,
        destinationLatitude: Double, destinationLongitude: Double, destinationAddress: String,
        distanceMeters: Double, durationSeconds: Double, estimatedPrice: Double,
        rideType: String, paymentMethod: String
    ): Result<String> = runCatching {
        // El nombre del pasajero se copia del perfil de Supabase al crear el viaje
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Inicia sesión para continuar.")
        AuthRepository().syncProfileToSupabase(uid)
        val body = JSONObject()
            .put("vehicle_type", "mototaxi")
            .put("origin_lat", originLatitude).put("origin_lng", originLongitude)
            .put("origin_address", originAddress)
            .put("destination_lat", destinationLatitude).put("destination_lng", destinationLongitude)
            .put("destination_address", destinationAddress)
            .put("distance_meters", distanceMeters.toInt())
            .put("duration_seconds", durationSeconds.toInt())
            .put("payment_method", if (paymentMethod == "yape_plin") "yape_plin" else "efectivo")
        val response = SupabaseApi.request("POST", "rides", body, "return=representation")
        org.json.JSONArray(response).getJSONObject(0).getString("id")
    }

    fun listenToRideRequest(requestId: String): Flow<RideRequest?> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val rows = SupabaseApi.rows("rides?id=eq.${SupabaseApi.encode(requestId)}&select=*&limit=1")
                emit(rows.optJSONObject(0)?.toRideRequest())
            } catch (_: Exception) { }
            delay(1_500)
        }
    }

    suspend fun cancelRideRequest(requestId: String): Result<Unit> = runCatching {
        SupabaseApi.rpc("cancel_ride", JSONObject().put("p_ride_id", requestId).put("p_reason", "cancelled_by_rider"))
        Unit
    }

    private fun JSONObject.toRideRequest() = RideRequest(
        requestId = getString("id"), userId = str("rider_id"), userName = str("rider_name"),
        userPhone = str("rider_phone"), userPhotoUrl = nullable("rider_photo_url"),
        originLatitude = optDouble("origin_lat"), originLongitude = optDouble("origin_lng"),
        originAddress = str("origin_address"), destinationLatitude = optDouble("destination_lat"),
        destinationLongitude = optDouble("destination_lng"), destinationAddress = str("destination_address"),
        distanceMeters = optDouble("distance_meters"), durationSeconds = optDouble("duration_seconds"),
        estimatedPrice = optDouble("estimated_fare"), rideType = str("vehicle_type"),
        paymentMethod = str("payment_method", "efectivo"), status = str("status", "searching"),
        createdAt = millis(nullable("requested_at")), updatedAt = millis(nullable("updated_at")),
        driverId = nullable("driver_id"), driverName = nullable("driver_name"),
        driverPhone = nullable("driver_phone"), driverPhotoUrl = nullable("driver_photo_url")
    )

    private fun JSONObject.nullable(key: String): String? = str(key).ifBlank { null }
    private fun millis(value: String?): Long =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
}
