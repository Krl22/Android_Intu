package com.intu.taxi

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.models.DriverRideRequest
import com.intu.taxi.ui.screens.EnhancedActiveRideCard
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/** El botón "Navegar" aparece yendo al recojo y en viaje, pero no mientras espera al pasajero. */
class DriverNavigateButtonTest {
    @get:Rule val compose = createComposeRule()

    private val request = DriverRideRequest(requestId = "qa-nav", userName = "María Ríos",
        originLatitude = -8.3791, originLongitude = -74.5539, originAddress = "Jr. Tarapacá 340",
        destinationLatitude = -8.3830, destinationLongitude = -74.5320, destinationAddress = "Plaza de Armas de Pucallpa",
        estimatedPrice = 12.0, paymentMethod = "efectivo")

    private fun show(status: String) {
        compose.setContent {
            IntuTheme(darkTheme = false) {
                Box(Modifier.width(380.dp).background(Color(0xFFE5ECEA)).testTag("ride-card")) {
                    EnhancedActiveRideCard(request, status, distance = 1.4, duration = 5.0, isCalculatingRoute = false,
                        onArrived = {}, onCancel = {})
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun goingToPickupShowsNavigate() {
        show("accepted")
        compose.onNodeWithText("Navegar").assertIsDisplayed()
        compose.onNodeWithText("Llegué").assertIsDisplayed()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "qa-driver-navigate.png").outputStream().use {
            compose.onNodeWithTag("ride-card").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun inProgressShowsNavigate() {
        show("in_progress")
        compose.onNodeWithTag("driver-navigate").assertIsDisplayed()
    }

    @Test fun waitingAtPickupHidesNavigate() {
        show("arrived")
        compose.onNodeWithText("Iniciar viaje").assertIsDisplayed()
        compose.onNodeWithTag("driver-navigate").assertDoesNotExist()
    }
}
