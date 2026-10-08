package com.intu.taxi

import com.intu.taxi.location.AutomaticRideSimulation
import org.junit.Assert.*
import org.junit.Test

class AutomaticRideSimulationTest {
    @Test fun approachesPickupWaitsForTripStartThenContinuesToDestinationOnce() {
        val automatic = AutomaticRideSimulation()
        automatic.enable("ride", 40)
        assertFalse(automatic.takeLeg("ride", "accepted", ready = false))
        assertTrue(automatic.takeLeg("ride", "accepted", ready = true))
        repeat(10) { assertFalse(automatic.takeLeg("ride", "accepted", ready = true)) }
        automatic.updateTrip("ride", "arrived")
        assertFalse(automatic.takeLeg("ride", "arrived", ready = true))
        assertNotNull(automatic.state.value)
        assertTrue(automatic.takeLeg("ride", "in_progress", ready = true))
        assertEquals(40, automatic.state.value!!.speedKmh)
        assertFalse(automatic.takeLeg("ride", "in_progress", ready = true))
        automatic.updateTrip("ride", "completed")
        assertNull(automatic.state.value)
    }

    @Test fun cancellationPauseOrAnotherRideCannotContinueAnOldAutomaticTrip() {
        for (status in listOf("cancelled", "completed", "searching")) {
            val automatic = AutomaticRideSimulation()
            automatic.enable("ride", 30)
            automatic.updateTrip("ride", status)
            assertFalse(automatic.takeLeg("ride", "in_progress", ready = true))
        }
        val automatic = AutomaticRideSimulation()
        automatic.enable("ride", 50)
        assertFalse(automatic.takeLeg("another-ride", "accepted", ready = true))
        assertNull(automatic.state.value)
        automatic.enable("ride", 50)
        automatic.stop()
        assertFalse(automatic.takeLeg("ride", "accepted", ready = true))
    }

    @Test fun interruptedRoutePermissionCanRetryWithoutReplayingThePreviousLeg() {
        val automatic = AutomaticRideSimulation()
        automatic.enable("ride", 20)
        assertTrue(automatic.takeLeg("ride", "accepted", ready = true))
        automatic.retryLeg("ride", "accepted")
        assertTrue(automatic.takeLeg("ride", "accepted", ready = true))
        assertTrue(automatic.takeLeg("ride", "in_progress", ready = true))
        automatic.retryLeg("ride", "accepted")
        assertFalse(automatic.takeLeg("ride", "in_progress", ready = true))
    }
}
