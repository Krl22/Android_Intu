package com.intu.taxi.models

/** Qué solicitud va al frente de la pila del conductor; lo elige un admin. */
enum class DriverRequestOrder(val code: String) {
    FARE("fare"), DISTANCE("distance");

    companion object {
        fun fromCode(code: String): DriverRequestOrder = entries.firstOrNull { it.code == code } ?: FARE
    }
}

/** Orden de la pila y segundos que tiene el conductor para decidir cada solicitud. */
data class DriverRequestSettings(
    val order: DriverRequestOrder = DriverRequestOrder.FARE,
    val timeoutSeconds: Int = 30
) {
    companion object {
        const val MIN_TIMEOUT_SECONDS = 10
        const val MAX_TIMEOUT_SECONDS = 120
    }
}

/** Solicitud lista para mostrarse: distancia al recojo y segundos que quedan (null = sin límite). */
data class QueuedDriverRequest(
    val request: DriverRideRequest,
    val pickupMeters: Double?,
    val secondsLeft: Int?
)

data class ArrangedDriverRequests(val visible: List<QueuedDriverRequest>, val expiredIds: List<String>)

object DriverRequestQueue {
    /**
     * Ordena las solicitudes de la pila: la que paga más (o la más cercana) va primero y, a igualdad,
     * la que llegó antes. El tiempo de cada una corre desde que el conductor la vio ([firstSeenMs]);
     * las vencidas salen aparte para no volver a mostrarlas. Las de [noTimeoutIds] (p. ej. con una
     * propuesta de precio pendiente) no vencen.
     */
    fun arrange(
        requests: List<DriverRideRequest>,
        firstSeenMs: Map<String, Long>,
        settings: DriverRequestSettings,
        nowMs: Long,
        driverLatitude: Double?,
        driverLongitude: Double?,
        noTimeoutIds: Set<String> = emptySet()
    ): ArrangedDriverRequests {
        val timeoutMs = settings.timeoutSeconds * 1000L
        val expired = mutableListOf<String>()
        val visible = mutableListOf<Pair<QueuedDriverRequest, Long>>()
        for (request in requests) {
            val seen = firstSeenMs[request.requestId] ?: nowMs
            val elapsed = (nowMs - seen).coerceAtLeast(0L)
            val exempt = request.requestId in noTimeoutIds
            if (!exempt && elapsed >= timeoutMs) {
                expired += request.requestId
                continue
            }
            val pickup = if (driverLatitude != null && driverLongitude != null)
                request.calculateDistanceFrom(driverLatitude, driverLongitude) else null
            val secondsLeft = if (exempt) null else ((timeoutMs - elapsed + 999) / 1000).toInt()
            visible += QueuedDriverRequest(request, pickup, secondsLeft) to seen
        }
        val arrival = compareBy<Pair<QueuedDriverRequest, Long>>({ it.second }, { it.first.request.createdAt })
        val sorted = when (settings.order) {
            DriverRequestOrder.FARE -> visible.sortedWith(
                compareByDescending<Pair<QueuedDriverRequest, Long>> { it.first.request.estimatedPrice }.then(arrival))
            DriverRequestOrder.DISTANCE -> visible.sortedWith(
                compareBy<Pair<QueuedDriverRequest, Long>, Double?>(nullsLast<Double>()) { it.first.pickupMeters }.then(arrival))
        }
        return ArrangedDriverRequests(sorted.map { it.first }, expired)
    }
}
