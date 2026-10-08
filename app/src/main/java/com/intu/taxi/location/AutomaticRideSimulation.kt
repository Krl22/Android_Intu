package com.intu.taxi.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AutomaticTestRide(val rideId: String, val speedKmh: Int)

/** Plays each leg once and waits for the normal trip/PIN flow between pickup and destination. */
class AutomaticRideSimulation {
    private val enabled = MutableStateFlow<AutomaticTestRide?>(null)
    val state = enabled.asStateFlow()
    private var startedLeg: String? = null

    fun enable(rideId: String, speedKmh: Int) {
        require(rideId.isNotBlank() && speedKmh in 5..120)
        startedLeg = null
        enabled.value = AutomaticTestRide(rideId, speedKmh)
    }

    fun updateTrip(rideId: String?, status: String) {
        val current = enabled.value ?: return
        if (current.rideId != rideId || status !in setOf("accepted", "arrived", "in_progress")) stop()
    }

    fun takeLeg(rideId: String?, status: String, ready: Boolean): Boolean {
        updateTrip(rideId, status)
        if (enabled.value == null || !ready || status !in setOf("accepted", "in_progress")) return false
        val leg = "$rideId:$status"
        if (startedLeg == leg) return false
        startedLeg = leg
        return true
    }

    fun retryLeg(rideId: String?, status: String) {
        if (startedLeg == "$rideId:$status") startedLeg = null
    }

    fun stop() { enabled.value = null; startedLeg = null }
}
