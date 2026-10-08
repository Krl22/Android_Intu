package com.intu.taxi

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.location.MapTestLocation
import com.intu.taxi.models.DriverRideRequest
import com.intu.taxi.ui.components.DriverHeaderContent
import com.intu.taxi.ui.components.IncomingRideRequestCard
import com.intu.taxi.ui.screens.TestLocationBanner
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Exercises actual request cards with the QA toolbar and the reserved stop-button area. */
class DriverRequestLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val request = DriverRideRequest(requestId = "qa", userName = "Carlos Villar",
        originLatitude = -12.13, originLongitude = -76.98,
        originAddress = "Lorenzo Lotto Mz.F - Lt.33, Santiago de Surco",
        destinationAddress = "Av. Los Próceres 1090, Santiago de Surco",
        distanceMeters = 600.0, durationSeconds = 120.0, estimatedPrice = 4.0,
        paymentMethod = "efectivo")
    private var accepted = 0
    private var declined = 0
    private var pixelsPerDp = 1f

    private fun show(height: Int, fontScale: Float = 1f, count: Int = 1) {
        compose.setContent {
            pixelsPerDp = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(pixelsPerDp, fontScale)) {
                IntuTheme(darkTheme = true) {
                    Column(Modifier.width(360.dp).height(height.dp).testTag("driver-layout")) {
                        TestLocationBanner(MapTestLocation(-12.13, -76.98), {}, onEdit = {})
                        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF242738))) {
                            DriverHeaderContent(Modifier.padding(bottom = 228.dp), scrollable = true) {
                                Text("Buscando clientes cerca…", color = Color.White, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(16.dp))
                                repeat(count) { index ->
                                    IncomingRideRequestCard(request.copy(requestId = "qa-$index"),
                                        -12.13, -76.98, onAccept = { accepted++ }, onDecline = { declined++ })
                                }
                            }
                            Button(onClick = {}, modifier = Modifier.align(Alignment.BottomCenter)
                                .padding(bottom = 56.dp).size(100.dp).testTag("stop")) { Text("Parar") }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun assertFullAction(text: String, index: Int = 0) {
        val action = compose.onAllNodes(hasText(text) and hasClickAction())[index]
        action.assertIsDisplayed().assertIsEnabled()
        val bounds = action.fetchSemanticsNode().boundsInRoot
        assertTrue("$text must retain a usable height", bounds.height >= 48f * pixelsPerDp - 1f)
        assertTrue("Requests must stay above Parar", bounds.bottom < compose.onNodeWithTag("stop").fetchSemanticsNode().boundsInRoot.top)
        compose.onAllNodesWithText(text, useUnmergedTree = true)[index].assertIsDisplayed()
    }

    @Test fun toolbarDoesNotCompressAcceptOrDeclineButtons() {
        show(680)
        assertFullAction("Aceptar")
        assertFullAction("Rechazar")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "qa-driver-request-buttons.png").outputStream().use {
            compose.onNodeWithTag("driver-layout").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Aceptar").performClick()
        compose.runOnIdle { assertEquals(1, accepted); assertEquals(0, declined) }
    }

    @Test fun shortScreenAndLargerTextCanScrollToEveryRequestAction() {
        show(560, fontScale = 1.3f, count = 2)
        compose.onAllNodes(hasText("Rechazar") and hasClickAction())[1].performScrollTo()
        assertFullAction("Aceptar", 1)
        assertFullAction("Rechazar", 1)
        compose.onAllNodesWithText("Rechazar")[1].performClick()
        compose.runOnIdle { assertEquals(0, accepted); assertEquals(1, declined) }
    }
}
