package com.intu.taxi

import com.intu.taxi.location.AdminLocationController
import com.intu.taxi.location.TestLocationPreset
import com.intu.taxi.location.MapTestLocation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AdminLocationTest {
    @Test fun userCanSimulateOnlyWhileAdminHasEnabledUsersAndRevocationStopsMovement() = runBlocking {
        var usersEnabled = false
        val controller = AdminLocationController { usersEnabled }
        controller.updateAccount("regular-user")
        assertTrue(runCatching { controller.activate(TestLocationPreset.SATIPO) }.isFailure)
        usersEnabled = true
        controller.activate(TestLocationPreset.SATIPO)
        val permit = controller.beginMovement()
        assertTrue(controller.isMoving.value)
        usersEnabled = false
        controller.recheckPermission()
        assertNull(controller.preset.value)
        assertFalse(controller.isMoving.value)
        assertFalse(controller.move(permit, MapTestLocation(-11.25, -74.63)))
        assertTrue(runCatching { controller.activate(TestLocationPreset.SATIPO) }.isFailure)
    }

    @Test fun signedOutAndNonAdminCannotSimulate() = runBlocking {
        val controller = AdminLocationController { false }
        assertTrue(runCatching { controller.activate(TestLocationPreset.SATIPO) }.isFailure)
        controller.updateAccount("passenger")
        assertTrue(runCatching { controller.activate(TestLocationPreset.RIO_NEGRO) }.isFailure)
        assertTrue(runCatching { controller.activate(MapTestLocation(-11.247, -74.625)) }.isFailure)
        assertNull(controller.preset.value)
    }

    @Test fun customPointIsExactSessionBoundAndRejectsInvalidCoordinates() = runBlocking {
        val controller = AdminLocationController { true }
        controller.updateAccount("admin")
        val custom = MapTestLocation(-11.2459123, -74.6267251)
        controller.activate(custom)
        assertEquals(custom, controller.preset.value)
        assertEquals(com.google.firebase.firestore.GeoPoint(custom.latitude, custom.longitude), controller.effectiveLocation(null))
        controller.updateAccount("another-admin")
        assertNull(controller.preset.value)
        for ((lat, lon) in listOf(Double.NaN to -74.0, -11.0 to Double.POSITIVE_INFINITY, 91.0 to 0.0, 0.0 to 181.0)) {
            assertTrue(runCatching { MapTestLocation(lat, lon) }.isFailure)
        }
    }

    @Test fun switchesCitiesRestoresGpsAndClearsOnAccountChangeOrRoleRevocation() = runBlocking {
        var admin = true
        val controller = AdminLocationController { admin }
        controller.updateAccount("admin")
        controller.activate(TestLocationPreset.SATIPO)
        assertEquals(TestLocationPreset.SATIPO, controller.preset.value)
        controller.activate(TestLocationPreset.RIO_NEGRO)
        assertEquals(TestLocationPreset.RIO_NEGRO, controller.preset.value)
        controller.clear()
        assertNull(controller.preset.value)
        controller.activate(TestLocationPreset.SATIPO)
        controller.updateAccount("another-account")
        assertNull(controller.preset.value)
        controller.updateAccount("admin")
        controller.activate(TestLocationPreset.SATIPO)
        admin = false
        controller.recheckPermission()
        assertNull(controller.preset.value)
        admin = true
        controller.activate(TestLocationPreset.RIO_NEGRO)
        controller.updateAccount(null)
        assertNull(controller.preset.value)
    }

    @Test fun aDelayedAdminResponseCannotReactivateAfterLogoutOrGpsRestore() = runBlocking {
        for (logout in listOf(true, false)) {
            val started = CompletableDeferred<Unit>()
            val response = CompletableDeferred<Boolean>()
            val controller = AdminLocationController { started.complete(Unit); response.await() }
            controller.updateAccount("admin")
            val activation = async { runCatching { controller.activate(TestLocationPreset.SATIPO) } }
            started.await()
            if (logout) {
                controller.updateAccount(null)
                controller.updateAccount("admin") // Same UID, but a different login session.
            } else controller.clear()
            response.complete(true)
            assertTrue(activation.await().isFailure)
            assertNull(controller.preset.value)
        }
    }
}
