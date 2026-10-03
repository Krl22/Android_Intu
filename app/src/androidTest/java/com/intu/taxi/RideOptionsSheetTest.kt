package com.intu.taxi

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.intu.taxi.ui.formatSoles
import com.intu.taxi.ui.screens.RideOptionsSheet
import com.intu.taxi.ui.theme.IntuTheme
import com.intu.taxi.models.MotoOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RideOptionsSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactOptionPreservesSelectionPaymentAndConfirmation() {
        var confirmations = 0
        compose.setContent { IntuTheme {
            var selected by remember { mutableStateOf<MotoOption?>(null) }
            var payment by remember { mutableStateOf("efectivo") }
            RideOptionsSheet(8.1, 6.5, 3.2, 8.0, selected, payment, selected != null, null,
                onSelect = { selected = it },
                onChangePayment = { payment = "yape_plin" },
                onConfirm = { confirmations++ },
                modifier = Modifier.width(360.dp).heightIn(max = 500.dp))
        } }
        compose.onNodeWithText("Elegir recojo").assertIsNotEnabled()
        compose.onNodeWithTag("moto-option-honda").assertIsNotSelected().performClick().assertIsSelected()
        compose.onNodeWithTag("moto-option-bajaj").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("moto-option-any").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("moto-option-delivery").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText(formatSoles(6.5)).assertIsDisplayed()
        compose.onNodeWithText("Cambiar").performScrollTo().performClick()
        compose.onNodeWithText("Yape / Plin").assertIsDisplayed()
        compose.onNodeWithText("Elegir recojo").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, confirmations) }
        val height = compose.onNodeWithTag("ride-options-sheet").fetchSemanticsNode().boundsInRoot.height
        val density = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        assertTrue("Vehicle choices must stay within their panel", height <= 500 * density + 1)
    }

    @Test fun largeTextOnAShortScreenKeepsConfirmationReachable() {
        var confirmed = false
        compose.setContent { IntuTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                RideOptionsSheet(8.1, 6.5, 3.2, 8.0, MotoOption.ANY, "efectivo", true, null,
                    onSelect = {}, onChangePayment = {}, onConfirm = { confirmed = true },
                    modifier = Modifier.width(320.dp).heightIn(max = 320.dp))
            }
        } }
        compose.onNodeWithText("Elegir recojo").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(true, confirmed) }
        compose.onNodeWithText("Método de pago").performScrollTo().assertIsDisplayed()
    }
}
