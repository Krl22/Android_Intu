package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.models.DriverRideRequest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.time.Instant

class DriverRideRequestRepository {
    fun getActiveRideRequests(): Flow<List<DriverRideRequest>> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val rows = SupabaseApi.rows("rides?status=eq.searching&select=*&order=requested_at.asc&limit=30")
                emit((0 until rows.length()).map { rows.getJSONObject(it).toRequest() })
            } catch (_: Exception) { }
            delay(2_000)
        }
    }

    suspend fun acceptRideRequest(requestId: String): Result<String> = runCatching {
        SupabaseApi.rpc("accept_ride", JSONObject().put("p_ride_id", requestId)); requestId
    }

    suspend fun declineRideRequest(requestId: String): Result<Unit> = Result.success(Unit)

    private fun JSONObject.toRequest() = DriverRideRequest(
        requestId = getString("id"), userId = optString("rider_id"), userName = optString("rider_name", "Pasajero"),
        userPhone = optString("rider_phone"), userPhotoUrl = if (isNull("rider_photo_url")) null else optString("rider_photo_url"),
        originLatitude = optDouble("origin_lat"), originLongitude = optDouble("origin_lng"), originAddress = optString("origin_address"),
        destinationLatitude = optDouble("destination_lat"), destinationLongitude = optDouble("destination_lng"), destinationAddress = optString("destination_address"),
        distanceMeters = optDouble("distance_meters"), durationSeconds = optDouble("duration_seconds"), estimatedPrice = optDouble("estimated_fare"),
        rideType = optString("vehicle_type"), paymentMethod = optString("payment_method"), status = optString("status"),
        createdAt = runCatching { Instant.parse(optString("requested_at")).toEpochMilli() }.getOrDefault(0L),
        updatedAt = runCatching { Instant.parse(optString("updated_at")).toEpochMilli() }.getOrDefault(0L)
    )
}
