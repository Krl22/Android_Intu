package com.intu.taxi.data

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class CancelRole { RIDER, DRIVER }

/** Motivo que se envía al servidor; el servidor decide si la cancelación cuenta. */
data class CancelReason(val code: String, val label: String)

object CancelReasons {
    const val OTHER = "other"
    const val RIDER_NO_SHOW = "rider_no_show"

    fun forRider(status: String, delivery: Boolean): List<CancelReason> {
        val noLongerNeeded = CancelReason("changed_mind", if (delivery) "Ya no necesito el envío" else "Ya no necesito el viaje")
        val other = CancelReason(OTHER, "Otro motivo")
        if (status == "searching") return listOf(
            CancelReason("search_too_long", if (delivery) "Tarda mucho en encontrar repartidor" else "Tarda mucho en encontrar conductor"),
            noLongerNeeded,
            CancelReason("wrong_address", "Me equivoqué de dirección"),
            other
        )
        val driver = if (delivery) "repartidor" else "conductor"
        return listOf(
            CancelReason("driver_late", "El $driver tarda demasiado"),
            CancelReason("driver_not_moving", "El $driver no se acerca"),
            CancelReason("driver_asked", "El $driver me pidió cancelar"),
            noLongerNeeded,
            CancelReason("wrong_address", "Me equivoqué de dirección"),
            CancelReason("other_transport", "Conseguí otro transporte"),
            CancelReason("safety", "Por mi seguridad"),
            other
        )
    }

    fun forDriver(delivery: Boolean): List<CancelReason> = listOf(
        CancelReason(RIDER_NO_SHOW, if (delivery) "Nadie entrega el paquete" else "El pasajero no aparece"),
        CancelReason("rider_asked", if (delivery) "Quien envía me pidió cancelar" else "El pasajero me pidió cancelar"),
        CancelReason("vehicle_problem", "Problema con mi vehículo"),
        CancelReason("unsafe_pickup", "Zona peligrosa o sin acceso"),
        CancelReason("accepted_by_mistake", "Acepté por error"),
        CancelReason(OTHER, "Otro motivo")
    )
}

/** Lo que significaría cancelar con un motivo, calculado por el servidor antes de confirmar. */
data class CancellationPreview(
    val outcome: String,
    val counts: Boolean,
    val penaltiesEnabled: Boolean,
    val strikes: Int,
    val limit: Int,
    val windowDays: Int,
    val wouldBlock: Boolean,
    val blockMinutes: Int,
    val waitSeconds: Int
) {
    /** El servidor rechazaría la cancelación con este motivo (no-show aún no verificado). */
    val blocked: Boolean get() = outcome in setOf("no_show_unavailable", "no_show_wait", "no_show_far")

    fun message(role: CancelRole): String = when (outcome) {
        "no_show_unavailable" -> "Primero marca que llegaste al punto de recojo."
        "no_show_wait" -> "Podrás usar este motivo en ${formatWait(waitSeconds)}. Espera en el punto de recojo."
        "no_show_far" -> "Debes estar en el punto de recojo para usar este motivo."
        "no_show" -> "No cuenta en tu contra. El servicio termina y la otra persona recibe una falta."
        else -> when {
            !penaltiesEnabled -> ""
            outcome == "free" -> "Esta cancelación no cuenta en tu contra."
            outcome == "excused" -> "Esta vez no cuenta en tu contra. Si se repite, revisaremos el caso."
            wouldBlock -> "Esta cancelación cuenta y llegas al límite: no podrás " +
                (if (role == CancelRole.DRIVER) "recibir solicitudes" else "pedir servicios") +
                " por ${formatMinutes(blockMinutes)}."
            counts -> "Esta cancelación cuenta (${strikes + 1} de $limit en $windowDays días)."
            else -> ""
        }
    }

    companion object {
        fun parse(json: JSONObject) = CancellationPreview(
            outcome = json.str("outcome", "counted"),
            counts = json.optBoolean("counts", false),
            penaltiesEnabled = json.optBoolean("penalties_enabled", false),
            strikes = json.optInt("strikes", 0),
            limit = json.optInt("limit", 3),
            windowDays = json.optInt("window_days", 7),
            wouldBlock = json.optBoolean("would_block", false),
            blockMinutes = json.optInt("block_minutes", 30),
            waitSeconds = json.optInt("wait_seconds", 0)
        )
    }
}

fun formatWait(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

fun formatMinutes(minutes: Int): String = when {
    minutes % 1440 == 0 -> (minutes / 1440).let { if (it == 1) "24 horas" else "$it días" }
    minutes % 60 == 0 -> (minutes / 60).let { if (it == 1) "1 hora" else "$it horas" }
    else -> "$minutes minutos"
}

/** "hasta las 15:30" hoy, o "hasta el 07/10 a las 15:30" otro día, en la hora del teléfono. */
fun formatBlockedUntil(isoUtc: String, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String? {
    val instant = runCatching { Instant.parse(isoUtc) }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(isoUtc).toInstant() }.getOrNull() ?: return null
    val local = instant.atZone(zone)
    val time = local.format(DateTimeFormatter.ofPattern("HH:mm"))
    return if (local.toLocalDate() == today) "hasta las $time"
    else "hasta el ${local.format(DateTimeFormatter.ofPattern("dd/MM"))} a las $time"
}
