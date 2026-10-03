package com.intu.taxi

import com.intu.taxi.auth.DriverAccess
import org.junit.Assert.*
import org.junit.Test

class DriverAccessTest {
    @Test fun aCompletePendingApplicationDoesNotEnableDrivingOrAnotherApplication() {
        val pending = DriverAccess("pending", true)
        assertFalse(pending.canDrive)
        assertFalse(pending.canApply)
        assertTrue(pending.message.orEmpty().contains("pasajero"))
    }
    @Test fun onlyApprovedCompleteProfilesCanDrive() {
        listOf(null, "pending", "rejected", "suspended", "unexpected").forEach {
            assertFalse(DriverAccess(it, true).canDrive)
        }
        assertFalse(DriverAccess("approved", false).canDrive)
        assertTrue(DriverAccess("approved", true).canDrive)
    }
    @Test fun onlyAccountsWithoutAnApplicationSeeTheApplyAction() {
        assertTrue(DriverAccess(null, false).canApply)
        listOf("pending", "approved", "rejected", "suspended").forEach {
            assertFalse(DriverAccess(it, false).canApply)
        }
    }

    @Test fun approvedCourierCannotConnectUntilTheDeliveryServiceIsEnabled() {
        val access = DriverAccess("approved", true, false, com.intu.taxi.auth.DriverVehicleType.MOTORCYCLE)
        assertFalse(access.canDrive)
        assertFalse(access.canApply)
        assertTrue(access.message.orEmpty().contains("reparto todavía no está habilitado"))
        assertTrue(access.copy(serviceAvailable = true).canDrive)
    }
}
