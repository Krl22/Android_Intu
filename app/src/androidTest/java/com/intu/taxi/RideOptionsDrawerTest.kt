package com.intu.taxi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.intu.taxi.models.MotoOption
import com.intu.taxi.ui.formatSoles
import com.intu.taxi.ui.map.RoutePin
import com.intu.taxi.ui.map.RoutePinStyle
import com.intu.taxi.ui.map.TripRouteStyle
import com.intu.taxi.ui.screens.RideOptionsDrawer
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RideOptionsDrawerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draggingNeverDismissesAndPreservesHondaFarePaymentAndConfirmation() {
        var confirmed = 0
        var visibleHeight = 0
        compose.setContent { IntuTheme {
            var selected by remember { mutableStateOf<MotoOption?>(MotoOption.ANY) }
            var payment by remember { mutableStateOf("efectivo") }
            RideOptionsDrawer(8.1, 6.5, 3.2, 8.0, selected, payment, true, null,
                onSelect = { selected = it }, onChangePayment = { payment = "yape_plin" },
                onConfirm = { confirmed++ }, onVisibleHeightChanged = { visibleHeight = it },
                modifier = Modifier.width(360.dp).height(720.dp))
        } }
        compose.onNodeWithTag("moto-option-honda").performScrollTo().performClick()
        compose.onNodeWithText("Cambiar").performScrollTo().performClick()
        compose.onNodeWithText("Yape / Plin").assertIsDisplayed()
        val expandedHeight = compose.onNodeWithTag("ride-options-sheet").fetchSemanticsNode().boundsInRoot.height
        compose.onNodeWithTag("moto-drawer-handle").performTouchInput {
            swipe(center, center + Offset(0f, expandedHeight * .8f), durationMillis = 600)
        }
        compose.onNodeWithContentDescription("Mostrar opciones de moto").assertIsDisplayed()
        compose.onNodeWithText("Mototaxi Honda").assertIsDisplayed()
        compose.onNodeWithText(formatSoles(MotoOption.HONDA.fare(8.1, 6.5))).assertIsDisplayed()
        val compactHeight = compose.onNodeWithTag("ride-options-sheet").fetchSemanticsNode().boundsInRoot.height
        assertTrue("Drawer must expose more map", compactHeight < expandedHeight * .8f)
        compose.runOnIdle { assertTrue(visibleHeight > 0) }
        compose.onNodeWithTag("moto-drawer-handle").performTouchInput {
            swipe(center, center + Offset(0f, compactHeight * .8f), durationMillis = 600)
        }
        compose.onNodeWithText("Elegir recojo").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("moto-drawer-handle").performTouchInput {
            swipe(center, center - Offset(0f, expandedHeight * .8f), durationMillis = 600)
        }
        compose.onNodeWithContentDescription("Ver más mapa").assertIsDisplayed()
        compose.onNodeWithTag("moto-option-honda").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Yape / Plin").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Elegir recojo").performClick()
        compose.runOnIdle { assertEquals(1, confirmed) }
    }

    @Test fun shortScreenWithLargeTextKeepsButtonAndCanReopenChoices() {
        var confirmed = false
        compose.setContent { IntuTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RideOptionsDrawer(8.1, 6.5, 3.2, 8.0, MotoOption.DELIVERY, "efectivo", true, null,
                    {}, {}, { confirmed = true }, {}, Modifier.width(320.dp).height(480.dp))
            }
        } }
        compose.onNodeWithTag("moto-drawer-handle").performClick()
        compose.onNodeWithText("Elegir recojo").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(confirmed) }
        compose.onNodeWithTag("moto-drawer-handle").performClick()
        compose.onNodeWithTag("moto-option-delivery").performScrollTo().assertIsDisplayed().assertIsSelected()
    }

    @Test fun elevatedPinHasContrastAndLeavesRouteVisibleBesideStem() {
        compose.setContent { IntuTheme {
            Row {
                for (dark in listOf(false, true)) {
                    Box(Modifier.size(140.dp).testTag("pin-route-preview-$dark")) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(if (dark) Color(0xFF25272B) else Color(0xFFEEF0EC))
                            drawLine(TripRouteStyle.lineColor, Offset(size.width * .5f, 0f),
                                Offset(size.width * .5f, size.height), strokeWidth = 8.dp.toPx())
                        }
                        RoutePin("Destino", Modifier.offset(49.dp, 42.dp).size(42.dp))
                    }
                }
            }
        } }
        for (dark in listOf(false, true)) {
            val pixels = compose.onNodeWithTag("pin-route-preview-$dark").captureToImage().toPixelMap()
            fun at(x: Float, y: Float) = pixels[(pixels.width * x / 140).toInt(), (pixels.height * y / 140).toInt()]
            fun matches(expected: Color, actual: Color) {
                assertEquals(expected.red, actual.red, .05f)
                assertEquals(expected.green, actual.green, .05f)
                assertEquals(expected.blue, actual.blue, .05f)
            }
            // The thin mast exposes the route beside it; the white foot marks the anchor at (70, 81.5).
            matches(TripRouteStyle.lineColor, at(73.4f, 73f))
            matches(TripRouteStyle.lineColor, at(70f, 86f))
            matches(Color.White, at(70f, 82f))
            matches(RoutePinStyle.headColor, at(76f, 55.6f))
            matches(Color.White, at(81f, 55.6f))
        }
        assertNotEquals(TripRouteStyle.lineColor, RoutePinStyle.destinationColor)
        captureNativeScreenshot(compose, "pin-b-light-dark.png")
    }
}
