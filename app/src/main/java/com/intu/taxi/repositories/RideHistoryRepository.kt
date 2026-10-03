package com.intu.taxi.repositories

import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Un viaje terminado o cancelado, para la pestaña Viajes. */
data class RideHistoryItem(
    val id: String,
    val status: String,
    val requestedAt: Instant?,
    val originAddress: String,
    val destinationAddress: String,
    val fare: Double,
    val paymentMethod: String,
    val driverName: String,
    val driverPhotoUrl: String,
    val vehicleDescription: String,
    val vehiclePlate: String,
    val riderName: String,
    val riderPhotoUrl: String,
    val ratingForDriver: Int?,
    val ratingForRider: Int?,
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val routeGeometry: String? = null,
    val distanceMeters: Int = 0,
    val durationSeconds: Int = 0,
    val vehicleType: String = "mototaxi",
    val serviceKind: String = "passenger"
)

/** Ganancias de un período (solo viajes completados). */
data class EarningsSummary(val rides: Int, val total: Double)

class RideHistoryRepository {
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: error("Inicia sesión para continuar.")

    /** Últimos viajes del pasajero, del más reciente al más antiguo. */
    suspend fun riderHistory(): List<RideHistoryItem> = history("rider_id")

    /** Últimos servicios del conductor, del más reciente al más antiguo. */
    suspend fun driverHistory(): List<RideHistoryItem> = history("driver_id")

    private suspend fun history(column: String): List<RideHistoryItem> {
        val rows = SupabaseApi.rows(
            "rides?$column=eq.${SupabaseApi.encode(uid)}&status=in.(completed,cancelled)" +
                "&select=*&order=requested_at.desc&limit=200"
        )
        return (0 until rows.length()).map { rows.getJSONObject(it).toHistoryItem() }
    }

    /** Califica al otro participante (1 a 5 estrellas). El servidor decide si es al conductor o al pasajero. */
    suspend fun rate(rideId: String, stars: Int) {
        SupabaseApi.rpc("rate_ride", JSONObject().put("p_ride_id", rideId).put("p_rating", stars))
    }

    /** Calificación promedio del conductor con sesión y cuántas veces lo calificaron. */
    suspend fun driverRating(): Pair<Double, Int>? {
        val row = SupabaseApi.rows("drivers?id=eq.${SupabaseApi.encode(uid)}&select=rating,rating_count&limit=1")
            .optJSONObject(0) ?: return null
        return row.optDouble("rating", 5.0) to row.optInt("rating_count", 0)
    }

    private fun JSONObject.toHistoryItem() = RideHistoryItem(
        id = str("id"),
        status = str("status"),
        // PostgREST devuelve "2026-09-28T04:48:01.8+00:00"; OffsetDateTime acepta el desfase
        requestedAt = runCatching { java.time.OffsetDateTime.parse(str("requested_at")).toInstant() }.getOrNull(),
        originAddress = str("origin_address"),
        destinationAddress = str("destination_address"),
        fare = if (isNull("final_fare")) optDouble("estimated_fare", 0.0) else optDouble("final_fare", 0.0),
        paymentMethod = str("payment_method", "efectivo"),
        driverName = str("driver_name"),
        driverPhotoUrl = str("driver_photo_url"),
        vehicleDescription = str("vehicle_description"),
        vehiclePlate = str("vehicle_plate"),
        riderName = str("rider_name"),
        riderPhotoUrl = str("rider_photo_url"),
        ratingForDriver = if (isNull("rating_for_driver")) null else optInt("rating_for_driver"),
        ratingForRider = if (isNull("rating_for_rider")) null else optInt("rating_for_rider"),
        originLatitude = if (isNull("origin_lat")) null else optDouble("origin_lat"),
        originLongitude = if (isNull("origin_lng")) null else optDouble("origin_lng"),
        destinationLatitude = if (isNull("destination_lat")) null else optDouble("destination_lat"),
        destinationLongitude = if (isNull("destination_lng")) null else optDouble("destination_lng"),
        routeGeometry = str("route_polyline").ifBlank { null },
        distanceMeters = optInt("distance_meters"),
        durationSeconds = optInt("duration_seconds"),
        vehicleType = str("vehicle_type", "mototaxi"), serviceKind = str("service_kind", "passenger")
    )

    companion object {
        /** Ganancias de hoy, de los últimos 7 días y del mes, según la hora del teléfono. */
        fun earnings(rides: List<RideHistoryItem>, zone: ZoneId = ZoneId.systemDefault()): Triple<EarningsSummary, EarningsSummary, EarningsSummary> {
            val today = LocalDate.now(zone)
            val completed = rides.filter { it.status == "completed" && it.requestedAt != null }
            fun sum(from: LocalDate) = completed
                .filter { !it.requestedAt!!.atZone(zone).toLocalDate().isBefore(from) }
                .let { EarningsSummary(it.size, it.sumOf { r -> r.fare }) }
            return Triple(sum(today), sum(today.minusDays(6)), sum(today.withDayOfMonth(1)))
        }
    }
}
