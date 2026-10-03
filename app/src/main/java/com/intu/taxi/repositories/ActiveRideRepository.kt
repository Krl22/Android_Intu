package com.intu.taxi.repositories

import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.data.PostgresChangeFilter
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.SupabaseRealtime
import com.intu.taxi.data.str
import com.intu.taxi.data.deliveryDetails
import com.intu.taxi.models.ActiveRide
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.time.Instant

class ActiveRideRepository {
    fun getActiveRideByRequestId(requestId: String): Flow<ActiveRide?> = liveRide(requestId)
    fun getActiveRide(rideId: String): Flow<ActiveRide?> = liveRide(rideId)

    suspend fun findOpenRideForRider(userId: String): ActiveRide? {
        val rows = SupabaseApi.rows(
            "rides?rider_id=eq.${SupabaseApi.encode(userId)}&status=in.(searching,accepted,arrived,in_progress)&select=*,delivery_details(*)&order=requested_at.desc&limit=1"
        )
        return rows.optJSONObject(0)?.toActiveRide()
    }

    suspend fun findOpenRideForDriver(userId: String): ActiveRide? {
        val rows = SupabaseApi.rows(
            "rides?driver_id=eq.${SupabaseApi.encode(userId)}&status=in.(accepted,arrived,in_progress)&select=*,delivery_details(*)&order=requested_at.desc&limit=1"
        )
        return rows.optJSONObject(0)?.toActiveRide()
    }

    /**
     * Viajes abiertos del conductor. Pueden ser dos: uno en curso y el siguiente que aceptó
     * mientras llevaba al pasajero (como Uber).
     */
    suspend fun findOpenRidesForDriver(userId: String): List<ActiveRide> {
        val rows = SupabaseApi.rows(
            "rides?driver_id=eq.${SupabaseApi.encode(userId)}&status=in.(accepted,arrived,in_progress)&select=*,delivery_details(*)&order=accepted_at.asc"
        )
        return (0 until rows.length()).map { rows.getJSONObject(it).toActiveRide() }
    }

    /** Fila del viaje, sin la ubicación del conductor. */
    suspend fun getRide(rideId: String): ActiveRide? =
        SupabaseApi.rows("rides?id=eq.${SupabaseApi.encode(rideId)}&select=*,delivery_details(*)&limit=1").optJSONObject(0)?.toActiveRide()

    /** Estado de un viaje cada 3 s, sin la ubicación del conductor (para el siguiente viaje en espera). */
    fun watchRide(rideId: String): Flow<ActiveRide?> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                emit(SupabaseApi.rows("rides?id=eq.${SupabaseApi.encode(rideId)}&select=*,delivery_details(*)&limit=1").optJSONObject(0)?.toActiveRide())
            } catch (_: Exception) { }
            delay(3_000)
        }
    }

    fun getUserActiveRide(userId: String): Flow<ActiveRide?> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val rows = SupabaseApi.rows(
                    "rides?driver_id=eq.${SupabaseApi.encode(userId)}&status=in.(accepted,arrived,in_progress)&select=*,delivery_details(*)&limit=1"
                )
                emit(rows.optJSONObject(0)?.toActiveRide())
            } catch (_: Exception) { }
            delay(1_500)
        }
    }

    /**
     * Viaje en vivo. Por Supabase Realtime llegan al instante los cambios del viaje y cada
     * movimiento del conductor; además se consulta cada 8 s como respaldo (cada 1.5 s si
     * Realtime no está conectado), así que funciona igual aunque se caiga la conexión.
     */
    private fun liveRide(rideId: String): Flow<ActiveRide?> = channelFlow {
        var last: ActiveRide? = null
        var realtimeReady = false
        val refresh = Channel<Unit>(Channel.CONFLATED)
        val activeDriverId = MutableStateFlow<String?>(null)

        // Cambios de la fila del viaje: estado, conductor asignado, cancelación…
        launch {
            SupabaseRealtime.changes("ride-$rideId", listOf(PostgresChangeFilter("rides", "id=eq.$rideId")))
                .collect { event ->
                    when (event.type) {
                        "joined" -> realtimeReady = true
                        "error" -> realtimeReady = false
                        "change" -> refresh.trySend(Unit)
                    }
                }
        }

        // Ubicación del conductor mientras dura el viaje (la RLS solo la deja ver a su pasajero)
        launch {
            activeDriverId.collectLatest { driverId ->
                if (driverId == null) return@collectLatest
                SupabaseRealtime.changes(
                    "ride-$rideId-location",
                    listOf(PostgresChangeFilter("driver_locations", "driver_id=eq.$driverId"))
                ).collect { event ->
                    val record = event.record ?: return@collect
                    val current = last ?: return@collect
                    if (event.type == "change" && record.has("latitude")) {
                        val moved = current.copy(driverLocation = GeoPoint(record.getDouble("latitude"), record.getDouble("longitude")))
                        last = moved
                        send(moved)
                    }
                }
            }
        }

        while (currentCoroutineContext().isActive) {
            runCatching { fetchRide(rideId) }.getOrNull()?.let { ride ->
                last = ride
                send(ride)
                activeDriverId.value = ride.driverId.takeIf {
                    it.isNotBlank() && ride.status in setOf("accepted", "arrived", "in_progress")
                }
            }
            withTimeoutOrNull(if (realtimeReady) 8_000L else 1_500L) { refresh.receive() }
        }
    }

    /** Fila del viaje y, si hay conductor en camino o en viaje, su última ubicación. */
    private suspend fun fetchRide(rideId: String): ActiveRide? {
        val rows = SupabaseApi.rows("rides?id=eq.${SupabaseApi.encode(rideId)}&select=*,delivery_details(*)&limit=1")
        val ride = rows.optJSONObject(0)?.toActiveRide() ?: return null
        if (ride.driverId.isBlank() || ride.status !in setOf("accepted", "arrived", "in_progress")) return ride
        val location = runCatching {
            SupabaseApi.rpc("ride_driver_location", JSONObject().put("p_ride_id", rideId))
        }.getOrNull()
        return ride.copy(driverLocation = location?.let {
            if (it.has("latitude")) GeoPoint(it.getDouble("latitude"), it.getDouble("longitude")) else null
        })
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

    suspend fun confirmDeliveryPayment(rideId: String): Result<Unit> = runCatching {
        SupabaseApi.rpc("confirm_delivery_payment", JSONObject().put("p_ride_id", rideId)); Unit
    }

    /** PIN de seguridad del viaje. Solo el pasajero lo recibe; para cualquier otro es null. */
    suspend fun startPin(rideId: String): String? = runCatching {
        SupabaseApi.rpc("ride_start_pin", JSONObject().put("p_ride_id", rideId)).str("pin").ifBlank { null }
    }.getOrNull()

    /** The server decides per service; connection errors must never bypass a required PIN. */
    suspend fun requiresStartPin(rideId: String): Boolean =
        SupabaseApi.rpc("ride_pin_requirement", JSONObject().put("p_ride_id", rideId)).getBoolean("required")

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
        driverPhotoUrl = str("driver_photo_url"), riderPhotoUrl = str("rider_photo_url"),
        driverOnOtherTrip = optBoolean("driver_on_other_trip", false),
        paymentConfirmed = !isNull("payment_confirmed_at"),
        vehicleType = str("vehicle_type", "mototaxi"), serviceKind = str("service_kind", "passenger"),
        delivery = deliveryDetails()
        )
    }

    private fun millis(value: String): Long = runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
}
