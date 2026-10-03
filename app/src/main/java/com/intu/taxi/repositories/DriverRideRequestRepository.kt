package com.intu.taxi.repositories

import com.intu.taxi.data.PostgresChangeFilter
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.SupabaseRealtime
import com.intu.taxi.data.str
import com.intu.taxi.models.DriverRideRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.time.Instant

class DriverRideRequestRepository {
    /**
     * Solicitudes abiertas. Una nueva llega al instante por Supabase Realtime (la RLS solo deja ver
     * al conductor las de su tipo de vehículo); además se consulta cada 8 s como respaldo, o cada
     * 2 s si Realtime no está conectado.
     */
    fun getActiveRideRequests(): Flow<List<DriverRideRequest>> = channelFlow {
        var realtimeReady = false
        val refresh = Channel<Unit>(Channel.CONFLATED)
        launch {
            SupabaseRealtime.changes("driver-requests", listOf(PostgresChangeFilter("rides")))
                .collect { event ->
                    when (event.type) {
                        "joined" -> realtimeReady = true
                        "error" -> realtimeReady = false
                        "change" -> refresh.trySend(Unit)
                    }
                }
        }
        while (currentCoroutineContext().isActive) {
            runCatching {
                val rows = SupabaseApi.rows("rides?status=eq.searching&select=*&order=requested_at.asc&limit=30")
                (0 until rows.length()).map { rows.getJSONObject(it).toRequest() }
            }.onSuccess { send(it) }
            withTimeoutOrNull(if (realtimeReady) 8_000L else 2_000L) { refresh.receive() }
        }
    }

    suspend fun acceptRideRequest(requestId: String): Result<String> = runCatching {
        SupabaseApi.rpc("accept_ride", JSONObject().put("p_ride_id", requestId)); requestId
    }

    suspend fun declineRideRequest(requestId: String): Result<Unit> = Result.success(Unit)

    private fun JSONObject.toRequest() = DriverRideRequest(
        requestId = getString("id"), userId = str("rider_id"), userName = str("rider_name", "Pasajero"),
        userPhone = str("rider_phone"), userPhotoUrl = str("rider_photo_url").ifBlank { null },
        originLatitude = optDouble("origin_lat"), originLongitude = optDouble("origin_lng"), originAddress = str("origin_address"),
        destinationLatitude = optDouble("destination_lat"), destinationLongitude = optDouble("destination_lng"), destinationAddress = str("destination_address"),
        distanceMeters = optDouble("distance_meters"), durationSeconds = optDouble("duration_seconds"), estimatedPrice = optDouble("estimated_fare"),
        rideType = str("vehicle_type"), paymentMethod = str("payment_method", "efectivo"), status = str("status"),
        createdAt = runCatching { Instant.parse(str("requested_at")).toEpochMilli() }.getOrDefault(0L),
        updatedAt = runCatching { Instant.parse(str("updated_at")).toEpochMilli() }.getOrDefault(0L),
        serviceKind = str("service_kind", "passenger")
    )
}
