package com.intu.taxi

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.models.DriverRequestQueue
import com.intu.taxi.models.DriverRequestSettings
import com.intu.taxi.models.DriverRideRequest
import com.intu.taxi.repositories.EarningsSummary
import com.intu.taxi.ui.components.DriverRequestStack
import com.intu.taxi.ui.screens.ChambearButton
import com.intu.taxi.ui.screens.DriverOnlineBar
import com.intu.taxi.ui.screens.DriverSearchingCard
import com.intu.taxi.ui.screens.DriverTodayCard
import com.intu.taxi.ui.theme.IntuTheme
import com.intu.taxi.ui.theme.LocalIntuDarkMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Pila de solicitudes con lomos y pantalla de inicio del conductor, con datos de ejemplo de Pucallpa. */
class DriverRequestStackTest {
    @get:Rule val compose = createComposeRule()

    private val driverLat = -8.3791
    private val driverLng = -74.5539
    private fun request(id: String, name: String, fare: Double, northMeters: Double, origin: String, delivery: Boolean = false) =
        DriverRideRequest(requestId = id, userName = name, estimatedPrice = fare,
            originLatitude = driverLat + northMeters / 111_320.0, originLongitude = driverLng,
            originAddress = origin, destinationAddress = "Plaza de Armas de Pucallpa",
            distanceMeters = 4_200.0, durationSeconds = 720.0, paymentMethod = "efectivo",
            serviceKind = if (delivery) "delivery" else "passenger")

    private val requests = listOf(
        request("maria", "María Ríos", 12.0, 800.0, "Jr. Tarapacá 340"),
        request("jose", "José Panduro", 18.5, 1_400.0, "Mercado N.° 2"),
        request("envio", "Bodega Lucy", 9.0, 2_100.0, "Jr. Ucayali 512", delivery = true)
    )

    private fun capture(tag: String, file: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), file).outputStream().use {
            compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun tabsCompareRequestsAndBringTheChosenOneToTheFront() {
        val seen = mapOf("maria" to 0L, "jose" to 12_000L, "envio" to 5_000L)
        val queue = DriverRequestQueue.arrange(requests, seen, DriverRequestSettings(), 16_000L, driverLat, driverLng).visible
        var pinned by mutableStateOf<String?>(null)
        val accepted = mutableListOf<String>()
        val passed = mutableListOf<String>()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            IntuTheme(darkTheme = false) {
                Box(Modifier.width(380.dp).height(760.dp).background(Color(0xFFE5ECEA)).testTag("stack-screen"),
                    contentAlignment = Alignment.BottomCenter) {
                    val stack = queue.firstOrNull { it.request.requestId == pinned }?.let { listOf(it) + (queue - it) } ?: queue
                    DriverRequestStack(stack, 30, onSelect = { pinned = it }, onAccept = { accepted += it.requestId },
                        onPass = { passed += it.requestId }, offerAction = { null }, offerStatus = { null },
                        modifier = Modifier.padding(bottom = 16.dp))
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        // Paga más primero: José (S/ 18.50) al frente; María y el envío asoman detrás
        compose.onNodeWithTag("request-front-jose").assertIsDisplayed()
        compose.onNodeWithTag("request-tab-maria").assertIsDisplayed()
        compose.onNodeWithTag("request-tab-envio").assertIsDisplayed()
        // Recién aparecida, Aceptar espera un instante para evitar toques por error
        compose.onNodeWithTag("request-accept").assertIsNotEnabled()
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithTag("request-accept").assertIsEnabled()
        capture("stack-screen", "qa-driver-request-stack.png")

        compose.onNodeWithTag("request-tab-maria").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag("request-front-maria").assertIsDisplayed()
        compose.onNodeWithTag("request-tab-jose").assertIsDisplayed()
        compose.onNodeWithTag("request-accept").performClick()
        compose.onNodeWithTag("request-pass").performClick()
        compose.runOnIdle {
            assertEquals(listOf("maria"), accepted)
            assertEquals(listOf("maria"), passed)
        }
    }

    @Test fun idleAndOnlineScreensShowTheirMainActions() {
        var online = 0
        var rest = 0
        compose.setContent {
            IntuTheme(darkTheme = false) {
                Column(Modifier.width(380.dp).background(Color(0xFFE5ECEA)).padding(vertical = 16.dp).testTag("idle-screen"),
                    verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { DriverTodayCard(EarningsSummary(6, 84.5)) }
                    ChambearButton(onClick = { online++ })
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { DriverOnlineBar(onRest = { rest++ }) }
                    DriverSearchingCard()
                }
            }
        }
        compose.onNodeWithText("S/ 84.50").assertIsDisplayed()
        compose.onNodeWithText("6 viajes").assertIsDisplayed()
        capture("idle-screen", "qa-driver-idle.png")
        compose.onNodeWithTag("driver-go-online").performClick()
        compose.onNodeWithText("Descansar").performClick()
        compose.runOnIdle { assertEquals(1, online); assertEquals(1, rest) }
    }

    @Test fun darkModeKeepsTheStackReadable() {
        val queue = DriverRequestQueue.arrange(requests, emptyMap(), DriverRequestSettings(), 0L, driverLat, driverLng).visible
        compose.setContent {
            IntuTheme(darkTheme = true) {
                CompositionLocalProvider(LocalIntuDarkMode provides true) {
                    Box(Modifier.width(380.dp).height(760.dp).background(Color(0xFF242738)).testTag("stack-dark"),
                        contentAlignment = Alignment.BottomCenter) {
                        DriverRequestStack(queue, 30, {}, {}, {}, offerAction = { {} }, offerStatus = { null },
                            modifier = Modifier.padding(bottom = 16.dp))
                    }
                }
            }
        }
        compose.onNodeWithText("Otro precio").assertIsDisplayed()
        capture("stack-dark", "qa-driver-request-stack-dark.png")
    }
}
