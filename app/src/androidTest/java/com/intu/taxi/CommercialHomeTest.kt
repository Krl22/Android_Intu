package com.intu.taxi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import com.intu.taxi.ui.map.TripRoute
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

/** Local GPS/routes only; never sends a booking or changes the signed-in account. */
class CommercialHomeTest {
    @get:Rule val compose = createComposeRule()
    private val locationReady = AtomicBoolean()

    private fun launch(dark: Boolean) {
        val gps = object : LocationProvider {
            override fun registerLocationConsumer(consumer: LocationConsumer) {
                consumer.onLocationUpdated(Point.fromLngLat(-74.6382, -11.2521))
                locationReady.set(true)
            }
            override fun unRegisterLocationConsumer(consumer: LocationConsumer) = Unit
        }
        compose.setContent { IntuTheme(darkTheme = dark) {
            HomeScreen(PaddingValues(), locationProvider = gps,
                routeLoader = { origin, target -> TripRoute(listOf(origin, target), 1800.0, 300.0) },
                rideRequestSender = { error("QA never sends a booking") })
        } }
        compose.waitUntil(60_000) { locationReady.get() }
    }

    @Test fun darkLandingShowsBrandAndRestoresAfterMapBack() {
        launch(dark = true)
        compose.onNodeWithTag("commercial-home").assertIsDisplayed()
        compose.onNodeWithText("intu").assertIsDisplayed()
        compose.onNodeWithText("¿A dónde vamos?").assertIsDisplayed()
        compose.onNodeWithTag("home-map").assertDoesNotExist()
        captureNativeScreenshot(compose, "commercial-home-dark.png")
        compose.onNodeWithText("Marcador").performClick()
        compose.onNodeWithTag("home-map").assertIsDisplayed()
        compose.onNodeWithTag("commercial-home").assertDoesNotExist()
        pressBack()
        compose.onNodeWithTag("commercial-home").assertIsDisplayed()
        compose.onNodeWithTag("home-map").assertDoesNotExist()
    }

    @Test fun deliveryShortcutCarriesVehicleChoiceIntoBooking() {
        launch(dark = true)
        compose.onNodeWithTag("home-start-delivery").performScrollTo().performClick()
        compose.onNodeWithTag("home-map").assertIsDisplayed()
        compose.onNodeWithText("Confirmar destino").performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Elige tu moto").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("moto-option-delivery").performScrollTo().assertIsSelected()
        pressBack()
        compose.onNodeWithTag("commercial-home").assertIsDisplayed()
        compose.onNodeWithTag("home-start-trip").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar destino").performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Elige tu moto").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("moto-option-any").performScrollTo().assertIsSelected()
    }

    @Test fun lightLandingSearchKeepsMapHiddenAndBackRestoresPresentation() {
        launch(dark = false)
        compose.onNodeWithTag("commercial-home").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performClick().performTextInput("zzzz")
        compose.onNodeWithTag("home-map").assertDoesNotExist()
        compose.onNodeWithText("Tu día se mueve\ncon Intu.").assertDoesNotExist()
        // Cancel the draft/search directly even while the software keyboard is visible.
        compose.onNodeWithContentDescription("Limpiar").performClick()
        pressBack()
        compose.onNodeWithTag("commercial-home").assertIsDisplayed()
        compose.onNodeWithText("Tu día se mueve\ncon Intu.").performScrollTo().assertIsDisplayed()
        captureNativeScreenshot(compose, "commercial-home-light.png")
    }
}
