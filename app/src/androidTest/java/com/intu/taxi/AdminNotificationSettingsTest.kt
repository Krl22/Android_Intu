package com.intu.taxi

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.push.AdminNotificationPreferences
import com.intu.taxi.ui.screens.AdminNotificationSettingsContent
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AdminNotificationSettingsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun individualAndMasterTogglesPreserveChoicesAndSavingDisablesEdits() {
        val value = mutableStateOf(AdminNotificationPreferences())
        val saving = mutableStateOf(false)
        compose.setContent { IntuTheme { AdminNotificationSettingsContent(value.value,saving.value,null,true,
            { value.value=it }, {}, {}, {}) } }
        compose.onNodeWithContentDescription("Solicitudes de viaje y envío").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(value.value.rideRequests); assertTrue(value.value.newUsers) }
        compose.onNodeWithContentDescription("Recibir actividad").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Usuarios nuevos").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Recibir actividad").performClick()
        compose.runOnIdle { assertFalse(value.value.rideRequests); saving.value=true }
        compose.onNodeWithContentDescription("Recibir actividad").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Solicitudes de viaje y envío").assertIsNotEnabled()
    }
    @Test fun blockedPermissionHasAnActionAndFailedLoadCanRetry() {
        var permissionClicks=0; var retries=0
        compose.setContent { IntuTheme { AdminNotificationSettingsContent(null,false,"Sin conexión",false,
            {}, { retries++ }, { permissionClicks++ }, {}) } }
        compose.onNodeWithText("Habilitar en Android").performClick()
        compose.onNodeWithText("Reintentar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,permissionClicks); assertEquals(1,retries) }
    }
}
