package com.intu.taxi

import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.driver.DriverLocationPublisher
import com.intu.taxi.location.AdminLocationController
import com.intu.taxi.location.MapTestLocation
import com.intu.taxi.location.TestLocationPath
import com.intu.taxi.location.TestLocationPreset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TestLocationMovementTest {
    @Test fun followsCornersSkipsDuplicatesAndStopsExactlyAtDestination() {
        val a = MapTestLocation(-11.25, -74.63)
        val b = MapTestLocation(-11.249, -74.63)
        val c = MapTestLocation(-11.249, -74.629)
        val path = TestLocationPath(listOf(a, a, b, c, c))
        val firstLeg = TestLocationPath.distance(a, b)
        assertEquals(a, path.pointAt(0.0))
        assertEquals(-11.2495, path.pointAt(firstLeg / 2).latitude, 0.00000001)
        assertEquals(-74.63, path.pointAt(firstLeg / 2).longitude, 0.00000001)
        assertEquals(b, path.pointAt(firstLeg))
        assertEquals(c, path.pointAt(path.lengthMeters))
        assertEquals(c, path.pointAt(path.lengthMeters + 100))
        assertEquals(a, TestLocationPath(listOf(a, a)).pointAt(0.0))
    }

    @Test fun pickupAndDestinationMovementPublishToTheActiveRideWithoutUsingRealGps() = runBlocking {
        val controller = AdminLocationController { true }
        controller.updateAccount("admin-driver")
        val pickup = MapTestLocation(-11.249, -74.63)
        val destination = MapTestLocation(-11.249, -74.629)
        val start = MapTestLocation(-11.25, -74.63)
        controller.activate(start)
        val sent = mutableListOf<Pair<GeoPoint, String?>>()
        val publisher = DriverLocationPublisher({ controller.effectiveLocation(GeoPoint(-12.0, -77.0)) },
            { "test-trip" }, { false }, { point, ride -> sent += point to ride })
        for ((origin, target) in listOf(start to pickup, pickup to destination)) {
            val path = TestLocationPath(listOf(origin, target))
            val permit = controller.beginMovement()
            for (meters in listOf(path.lengthMeters / 2, path.lengthMeters)) {
                assertTrue(controller.move(permit, path.pointAt(meters)))
                publisher.publish(force = true)
                assertEquals(GeoPoint(path.pointAt(meters).latitude, path.pointAt(meters).longitude) to "test-trip", sent.last())
            }
            controller.finishMovement(permit)
            assertFalse(controller.isMoving.value)
            assertEquals(target, controller.preset.value)
        }
    }

    @Test fun oldMovementCannotOverridePauseManualPointLogoutOrRevokedPermission() = runBlocking {
        for (action in listOf("pause", "manual", "logout", "revoke", "gps")) {
            var allowed = true
            val controller = AdminLocationController { allowed }
            controller.updateAccount("admin")
            controller.activate(TestLocationPreset.SATIPO)
            val permit = controller.beginMovement()
            when (action) {
                "pause" -> controller.pauseMovement()
                "manual" -> controller.activate(TestLocationPreset.RIO_NEGRO)
                "logout" -> { controller.updateAccount(null); controller.updateAccount("admin") }
                "revoke" -> { allowed = false; controller.recheckPermission() }
                "gps" -> controller.clear()
            }
            val before = controller.preset.value
            assertFalse(controller.move(permit, MapTestLocation(-11.0, -74.0)))
            assertFalse(controller.isMoving.value)
            assertEquals(before, controller.preset.value)
        }
    }

    @Test fun delayedMovementPermissionCannotRestartAfterTripLegChanges() = runBlocking {
        val response = CompletableDeferred<Boolean>()
        var hold = false
        val started = CompletableDeferred<Unit>()
        val controller = AdminLocationController {
            if (hold) { started.complete(Unit); response.await() } else true
        }
        controller.updateAccount("admin")
        controller.activate(TestLocationPreset.SATIPO)
        hold = true
        val movement = async { runCatching { controller.beginMovement() } }
        started.await()
        controller.pauseMovement()
        response.complete(true)
        assertTrue(movement.await().isFailure)
        assertFalse(controller.isMoving.value)
    }
}
