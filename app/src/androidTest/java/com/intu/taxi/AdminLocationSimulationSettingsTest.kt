package com.intu.taxi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import com.intu.taxi.repositories.LocationSimulationAccess
import com.intu.taxi.ui.screens.AdminLocationSimulationSettingsContent
import com.intu.taxi.ui.screens.AdminPanelTheme
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AdminLocationSimulationSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun personalBarStartsHiddenAndDoesNotChangeTheUserPermission() {
        val access = mutableStateOf(LocationSimulationAccess(false, true, isAdmin = true))
        val busy = mutableStateOf(false)
        var personalRequest: Boolean? = null
        var globalRequest: Boolean? = null
        compose.setContent { IntuTheme { AdminPanelTheme {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                AdminLocationSimulationSettingsContent(access.value, busy.value, null, {},
                    onChange = { globalRequest = it }, onAdminBarChange = { personalRequest = it })
            }
        } } }
        compose.onNodeWithTag("admin-location-simulation-own-bar").assertIsOff().performClick()
        compose.runOnIdle { assertEquals(true, personalRequest); assertNull(globalRequest); busy.value = true }
        compose.onNodeWithTag("admin-location-simulation-own-bar").assertIsOff().assertIsNotEnabled()
        compose.runOnIdle { access.value = access.value.copy(adminBarEnabled = true); busy.value = false }
        compose.onNodeWithTag("admin-location-simulation-own-bar").assertIsOn().performClick()
        compose.runOnIdle {
            assertEquals(false, personalRequest)
            assertNull(globalRequest)
            access.value = access.value.copy(adminBarEnabled = false)
        }
        compose.onNodeWithTag("admin-location-simulation-own-bar").assertIsOff()
        compose.onNodeWithTag("admin-location-simulation-users").performScrollTo().assertIsOff()
        captureNativeScreenshot(compose, "admin-location-simulation-own-bar.png")
    }

    @Test fun switchStartsOffAndOnlyDisplaysConfirmedServerValues() {
        val access = mutableStateOf(LocationSimulationAccess(false,true))
        val busy = mutableStateOf(false)
        var requested: Boolean? = null
        compose.setContent { IntuTheme { AdminPanelTheme { Column(Modifier.padding(16.dp)) {
            AdminLocationSimulationSettingsContent(access.value,busy.value,null,{}, { requested = it })
        } } } }
        compose.onNodeWithTag("admin-location-simulation-users").assertIsOff().performClick()
        compose.runOnIdle { assertEquals(true,requested); busy.value = true }
        compose.onNodeWithTag("admin-location-simulation-users").assertIsOff().assertIsNotEnabled()
        compose.runOnIdle { access.value = LocationSimulationAccess(true,true); busy.value = false }
        compose.onNodeWithTag("admin-location-simulation-users").assertIsOn().performClick()
        compose.runOnIdle { assertEquals(false,requested) }
        captureNativeScreenshot(compose,"admin-location-simulation-permission.png")
    }

    @Test fun failedSaveKeepsTheSwitchOffAndFailedLoadCanBeRetried() {
        val access = mutableStateOf<LocationSimulationAccess?>(LocationSimulationAccess(false,true))
        var retries = 0
        compose.setContent { IntuTheme { AdminPanelTheme { Column {
            AdminLocationSimulationSettingsContent(access.value,false,"No se pudo guardar el permiso.", { retries++ }, {})
        } } } }
        compose.onNodeWithTag("admin-location-simulation-users").assertIsOff()
        compose.onNodeWithText("No se pudo guardar el permiso.").assertIsDisplayed()
        compose.runOnIdle { access.value = null }
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(1,retries) }
    }
}
