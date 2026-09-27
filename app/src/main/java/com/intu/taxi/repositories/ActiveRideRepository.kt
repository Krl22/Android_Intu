package com.intu.taxi.repositories

import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.models.ActiveRide
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.time.Instant

class ActiveRideRepository {
    fun getActiveRideByRequestId(requestId: String): Flow<ActiveRide?> = pollRide(requestId)
    fun getActiveRide(rideId: String): Flow<ActiveRide?> = pollRide(rideId)

    suspend fun findOpenRideForRider(userId: String): ActiveRide? {
        val rows = SupabaseApi.rows(
            "rides?rider_id=eq.${SupabaseApi.encode(userId)}&status=in.(searching,accepted,arrived,in_progress)&select=*&order=requested_at.desc&limit=1"
        )
        return rows.optJSONObject(0)?.toActiveRide()
    }

    suspend fun findOpenRideForDriver(userId: String): ActiveRide? {
        val rows = SupabaseApi.rows(
            "rides?driver_id=eq.${SupabaseApi.encode(userId)}&status=in.(accepted,arrived,in_progress)&select=*&order=requested_at.desc&limit=1"
        )
        return rows.optJSONObject(0)?.toActiveRide()
    }

    fun getUserActiveRide(userId: String): Flow<ActiveRide?> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val rows = SupabaseApi.rows(
                    "rides?driver_id=eq.${SupabaseApi.encode(userId)}&status=in.(accepted,arrived,in_progress)&select=*&limit=1"
                )
                emit(rows.optJSONObject(0)?.toActiveRide())
            } catch (_: Exception) { }
            delay(1_500)
        }
    }

    private fun pollRide(rideId: String): Flow<ActiveRide?> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val rows = SupabaseApi.rows("rides?id=eq.${SupabaseApi.encode(rideId)}&select=*&limit=1")
                val ride = rows.optJSONObject(0)?.toActiveRide()
                if (ride != null && ride.driverId.isNotBlank() && ride.status in setOf("accepted", "arrived", "in_progress")) {
                    val location = runCatching {
                        SupabaseApi.rpc("ride_driver_location", JSONObject().put("p_ride_id", rideId))
                    }.getOrNull()
                    emit(ride.copy(driverLocation = location?.let {
                        if (it.has("latitude")) GeoPoint(it.getDouble("latitude"), it.getDouble("longitude")) else null
                    }))
                } else emit(ride)
            } catch (_: Exception) { }
            delay(1_500)
        }
    }

    suspend fun updateDriverLocation(rideId: String, location: GeoPoint): Result<Unit> = runCatching {
        SupabaseApi.rpc("set_driver_location", JSONObject().put("p_latitude", location.latitude)
            .put("p_longitude", location.longitude).put("p_heading", JSONObject.NULL).put("p_is_available", false)); Unit
    }

    suspend fun updateClientLocation(rideId: String, location: GeoPoint): Result<Unit> = Result.success(Unit)

    suspend fun advanceRide(rideId: String, status: String): Result<ActiveRide> = runCatching {
        SupabaseApi.rpc("advance_ride", JSONObject().put("p_ride_id", rideId).put("p_status", status)).toActiveRide()
    }

    suspend fun completeRide(rideId: String): Result<Unit> = advanceRide(rideId, "completed").map { Unit }

    suspend fun cancelRide(rideId: String): Result<Unit> = runCatching {
        SupabaseApi.rpc("cancel_ride", JSONObject().put("p_ride_id", rideId).put("p_reason", "cancelled_by_driver")); Unit
    }

    suspend fun updateRouteGeometry(rideId: String, geometry: String): Result<Unit> = Result.success(Unit)

    @Suppress("UNUSED_PARAMETER")
    suspend fun createActiveRide(
        requestId: String, driverId: String, clientId: String, driverLocation: GeoPoint,
        clientLocation: GeoPoint, destination: GeoPoint, originAddress: String, destinationAddress: String,
        originLatitude: Double = 0.0, originLongitude: Double = 0.0,
        destinationLatitude: Double = 0.0, destinationLongitude: Double = 0.0
    ): Result<String> = Result.success(requestId)

    private fun JSONObject.toActiveRide(): ActiveRide {
        val rideStatus = optString("status")
        val routeTarget = if (rideStatus == "in_progress") {
            GeoPoint(optDouble("destination_lat"), optDouble("destination_lng"))
        } else {
            GeoPoint(optDouble("origin_lat"), optDouble("origin_lng"))
        }
        return ActiveRide(
        rideId = optString("id"), requestId = optString("id"), driverId = optString("driver_id"),
        clientId = optString("rider_id"), clientLocation = routeTarget,
        destination = GeoPoint(optDouble("destination_lat"), optDouble("destination_lng")),
        originAddress = optString("origin_address"), destinationAddress = optString("destination_address"),
        status = rideStatus, createdAt = millis(optString("requested_at")), updatedAt = millis(optString("updated_at")),
        originLatitude = optDouble("origin_lat"), originLongitude = optDouble("origin_lng"),
        destinationLatitude = optDouble("destination_lat"), destinationLongitude = optDouble("destination_lng"),
        paymentMethod = optString("payment_method"), fare = optDouble("final_fare", optDouble("estimated_fare")),
        driverName = optString("driver_name"), driverPhone = optString("driver_phone"),
        riderName = optString("rider_name"), riderPhone = optString("rider_phone"),
        vehiclePlate = optString("vehicle_plate"), vehicleDescription = optString("vehicle_description"),
        paymentConfirmed = !isNull("payment_confirmed_at")
        )
    }

    private fun millis(value: String): Long = runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
}
