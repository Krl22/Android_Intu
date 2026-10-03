package com.intu.taxi

import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.repositories.RideSecuritySettings
import com.intu.taxi.ui.screens.AdminRideSecurityContent
import com.intu.taxi.ui.screens.AdminPanelLayout
import com.intu.taxi.ui.screens.AdminPanelTheme
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdminRideSecurityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun savedPreferenceControlsSwitchAndSavingBlocksAnotherChange() {
        val settings = mutableStateOf(RideSecuritySettings(false))
        val busy = mutableStateOf(false)
        var requested: Boolean? = null
        compose.setContent { IntuTheme(darkTheme = true) {
            AdminPanelTheme { AdminPanelLayout(PaddingValues(), 5, true, null, {}, {}, {}, {}) {
            AdminRideSecurityContent(settings.value, busy.value, null, {}, { requested = it })
            } }
        } }
        compose.onNodeWithTag("admin-ride-pin").assertIsOff().performClick()
        compose.runOnIdle { assertEquals(true, requested); busy.value = true }
        // Keep the confirmed server value while the save is pending.
        compose.onNodeWithTag("admin-ride-pin").assertIsOff().assertIsNotEnabled()
        compose.runOnIdle { settings.value = RideSecuritySettings(true); busy.value = false }
        compose.onNodeWithTag("admin-ride-pin").assertIsOn().assertIsEnabled()
        compose.onNodeWithText("Seguridad", substring = false).performScrollTo().assertIsDisplayed()
        captureNativeScreenshot(compose, "admin-ride-security-dark.png")
        compose.onNodeWithTag("admin-ride-pin").performClick()
        compose.runOnIdle { assertEquals(false, requested) }
    }

    @Test fun failedSaveKeepsConfirmedValueAndFailedLoadOffersRetry() {
        val settings = mutableStateOf<RideSecuritySettings?>(RideSecuritySettings(true))
        var retries = 0
        compose.setContent { IntuTheme(darkTheme = false) {
            AdminPanelTheme { AdminPanelLayout(PaddingValues(), 5, true, null, {}, {}, {}, {}) {
            AdminRideSecurityContent(settings.value, false, "Sin conexión", { retries++ }, {})
            } }
        } }
        compose.onNodeWithTag("admin-ride-pin").assertIsOn()
        compose.onNodeWithText("Sin conexión").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { settings.value = null }
        compose.onNodeWithTag("admin-ride-pin").assertDoesNotExist()
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
