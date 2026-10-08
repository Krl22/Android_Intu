package com.intu.taxi

import com.intu.taxi.repositories.LocationSimulationAccess
import org.junit.Assert.*
import org.junit.Test

class LocationSimulationAccessTest {
    @Test fun adminBarIsHiddenUntilPersonallyEnabledRegardlessOfGlobalUserPermission() {
        for (usersEnabled in listOf(false, true)) {
            val admin = LocationSimulationAccess(usersEnabled, allowed = true, isAdmin = true)
            assertFalse(admin.showControls)
            assertTrue(admin.copy(adminBarEnabled = true).showControls)
            assertFalse(admin.copy(allowed = false, adminBarEnabled = true).showControls)
        }
    }

    @Test fun userVisibilityFollowsServerPermissionWithoutAdminPreference() {
        val user = LocationSimulationAccess(usersEnabled = true, allowed = true)
        assertTrue(user.showControls)
        assertFalse(user.copy(usersEnabled = false, allowed = false).showControls)
    }
}
