package com.intu.taxi.repositories

import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
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

    /** PIN de seguridad del viaje. Solo el pasajero lo recibe; para cualquier otro es null. */
    suspend fun startPin(rideId: String): String? = runCatching {
        SupabaseApi.rpc("ride_start_pin", JSONObject().put("p_ride_id", rideId)).str("pin").ifBlank { null }
    }.getOrNull()

    /** El conductor verifica el PIN que le dicta el pasajero. Devuelve si es correcto y los intentos que quedan. */
    suspend fun verifyStartPin(rideId: String, pin: String): Result<Pair<Boolean, Int>> = runCatching {
        val result = SupabaseApi.rpc("verify_ride_pin", JSONObject().put("p_ride_id", rideId).put("p_pin", pin))
        result.optBoolean("verified", false) to result.optInt("attempts_left", 0)
    }

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
        val rideStatus = str("status")
        val routeTarget = if (rideStatus == "in_progress") {
            GeoPoint(optDouble("destination_lat"), optDouble("destination_lng"))
        } else {
            GeoPoint(optDouble("origin_lat"), optDouble("origin_lng"))
        }
        return ActiveRide(
        rideId = str("id"), requestId = str("id"), driverId = str("driver_id"),
        clientId = str("rider_id"), clientLocation = routeTarget,
        destination = GeoPoint(optDouble("destination_lat"), optDouble("destination_lng")),
        originAddress = str("origin_address"), destinationAddress = str("destination_address"),
        status = rideStatus, createdAt = millis(str("requested_at")), updatedAt = millis(str("updated_at")),
        originLatitude = optDouble("origin_lat"), originLongitude = optDouble("origin_lng"),
        destinationLatitude = optDouble("destination_lat"), destinationLongitude = optDouble("destination_lng"),
        paymentMethod = str("payment_method"), fare = optDouble("final_fare", optDouble("estimated_fare")),
        driverName = str("driver_name"), driverPhone = str("driver_phone"),
        riderName = str("rider_name"), riderPhone = str("rider_phone"),
        vehiclePlate = str("vehicle_plate"), vehicleDescription = str("vehicle_description"),
        paymentConfirmed = !isNull("payment_confirmed_at")
        )
    }

    private fun millis(value: String): Long = runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
}
