package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import com.intu.taxi.models.BookingContact
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ScheduledRide(
    val id: String,
    val scheduledFor: Instant,
    val status: String,
    val originAddress: String,
    val destinationAddress: String,
    val fare: Double,
    val passengerName: String?,
    val failureReason: String?
)

/** Ventana en la que se puede programar: el servidor aplica la misma. */
object ScheduleWindow {
    val MIN_AHEAD: Duration = Duration.ofMinutes(20)
    val MAX_AHEAD: Duration = Duration.ofDays(7)
    const val DISPATCH_MINUTES_BEFORE = 10L

    /** null si la hora sirve; si no, el motivo para mostrar. */
    fun problem(at: Instant, now: Instant = Instant.now()): String? = when {
        at.isBefore(now.plus(MIN_AHEAD)) -> "Programa con al menos 20 minutos de anticipación."
        at.isAfter(now.plus(MAX_AHEAD)) -> "Puedes programar hasta 7 días antes."
        else -> null
    }

    /** "Hoy 18:30", "Mañana 07:15" o "mié 8 oct, 09:00". */
    fun label(at: Instant, zone: ZoneId = ZoneId.systemDefault(), now: ZonedDateTime = ZonedDateTime.now(zone)): String {
        val local = at.atZone(zone)
        val time = local.format(DateTimeFormatter.ofPattern("HH:mm"))
        val days = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), local.toLocalDate())
        return when (days) {
            0L -> "Hoy $time"
            1L -> "Mañana $time"
            else -> local.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-PE"))).replace(".", "") + ", $time"
        }
    }
}

class ScheduledRideRepository {
    suspend fun schedule(
        at: Instant,
        originLatitude: Double, originLongitude: Double, originAddress: String,
        destinationLatitude: Double, destinationLongitude: Double, destinationAddress: String,
        distanceMeters: Double, durationSeconds: Double, routeGeometry: String?,
        paymentMethod: String, preferredVehicleBrand: String?, passenger: BookingContact?
    ): ScheduledRide {
        ScheduleWindow.problem(at)?.let { throw IllegalArgumentException(it) }
        val person = passenger?.normalized()
        val body = JSONObject()
            .put("p_scheduled_for", at.toString())
            .put("p_origin_lat", originLatitude).put("p_origin_lng", originLongitude).put("p_origin_address", originAddress)
            .put("p_destination_lat", destinationLatitude).put("p_destination_lng", destinationLongitude)
            .put("p_destination_address", destinationAddress)
            .put("p_distance_meters", distanceMeters.toInt()).put("p_duration_seconds", durationSeconds.toInt())
            .put("p_route_polyline", routeGeometry ?: JSONObject.NULL)
            .put("p_payment_method", if (paymentMethod == "yape_plin") "yape_plin" else "efectivo")
            .put("p_preferred_brand", preferredVehicleBrand ?: JSONObject.NULL)
            .put("p_contact", person?.let { JSONObject().put("name", it.name).put("phone", it.phone) } ?: JSONObject.NULL)
        return SupabaseApi.rpc("schedule_ride", body).toScheduledRide()
    }

    /** Próximos viajes programados y los que no se pudieron iniciar en el último día. */
    suspend fun list(): List<ScheduledRide> {
        val rows = SupabaseApi.rpcRows("my_scheduled_rides")
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.toScheduledRide() }
    }

    suspend fun cancel(id: String) {
        SupabaseApi.rpc("cancel_scheduled_ride", JSONObject().put("p_id", id))
    }

    private fun JSONObject.toScheduledRide() = ScheduledRide(
        id = str("id"),
        scheduledFor = str("scheduled_for").let { value ->
            runCatching { Instant.parse(value) }.getOrNull() ?: OffsetDateTime.parse(value).toInstant()
        },
        status = str("status"),
        originAddress = str("origin_address"),
        destinationAddress = str("destination_address"),
        fare = optDouble("estimated_fare", 0.0),
        passengerName = str("passenger_name").ifBlank { null },
        failureReason = str("failure_reason").ifBlank { null }
    )
}

