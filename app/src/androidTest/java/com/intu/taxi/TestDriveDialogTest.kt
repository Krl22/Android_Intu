package com.intu.taxi

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.ui.screens.TestDriveDialog
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Isolated trip controls: no accounts, GPS or server writes are needed to reproduce the button bug. */
class TestDriveDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun manualAndAutomaticStartsAreEnabledBeforeTheRoadRouteIsLoaded() {
        var busy by mutableStateOf(false)
        var starts = 0
        var automatic = 0
        compose.setContent { IntuTheme {
            TestDriveDialog("Avanzar al recojo", 40, busy, false, false, canStart = true, error = null,
                onSpeed = {}, onAutomatic = { automatic++ }, onPause = {},
                onStart = { starts++; busy = true }, onDismiss = {})
        } }
        compose.onNodeWithText("Simular viaje automático").assertIsEnabled()
        compose.onNodeWithText("Avanzar al recojo").assertIsEnabled().performClick()
        compose.onNodeWithText("Preparando recorrido desde tu ubicación…").assertIsDisplayed()
        compose.onNodeWithText("Avanzar al recojo").assertIsNotEnabled()
        compose.onNodeWithText("Simular viaje automático").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, starts); busy = false }
        compose.onNodeWithText("Simular viaje automático").performClick()
        compose.runOnIdle { assertEquals(1, automatic) }
        compose.onNodeWithText("Espera a que la ruta se actualice desde tu ubicación de prueba.").assertDoesNotExist()
    }

    @Test fun automaticRoutePreparationCanBePausedImmediately() {
        var pauses = 0
        compose.setContent { IntuTheme {
            TestDriveDialog("Avanzar al destino", 40, busy = true, moving = false, automaticMode = true,
                canStart = true, error = null, onSpeed = {}, onAutomatic = {},
                onPause = { pauses++ }, onStart = {}, onDismiss = {})
        } }
        compose.onNodeWithText("Pausar simulación").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, pauses) }
    }
}
