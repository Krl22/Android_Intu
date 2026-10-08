package com.intu.taxi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.intu.taxi.models.*
import com.intu.taxi.ui.map.TripRoute
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.screens.RideBooking
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class FareBookingFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun adminRatesReachTheDrawerAndAChangedPriceRequiresReviewBeforeSending() {
        val settings = AtomicReference(FareSettings.Default)
        val requested = AtomicReference<RideBooking>()
        val gpsConsumer = AtomicReference<LocationConsumer>()
        val gps = object : LocationProvider {
            override fun registerLocationConsumer(value: LocationConsumer) {
                gpsConsumer.set(value); value.onLocationUpdated(Point.fromLngLat(-74.6382, -11.2521))
            }
            override fun unRegisterLocationConsumer(value: LocationConsumer) = Unit
        }
        compose.setContent { IntuTheme {
            HomeScreen(PaddingValues(), locationProvider = gps, businessFeedLoader = { BusinessFeed() },
                fareSettingsLoader = { settings.get() },
                routeLoader = { origin, target -> TripRoute(listOf(origin,target),3000.0,600.0) },
                rideRequestSender = { requested.set(it); Result.failure(IllegalStateException("QA · solicitud simulada")) })
        } }
        compose.waitUntil(60_000) { gpsConsumer.get() != null }
        compose.onNodeWithTag("home-start-trip").performScrollTo().performClick()
        closeSoftKeyboard()
        compose.onNodeWithTag("home-pick-destination").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Confirmar destino").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Confirmar destino").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("moto-option-honda").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("moto-option-honda").performScrollTo().performClick()
        compose.onAllNodesWithText("S/ 6.20").onFirst().assertExists()
        settings.set(FareSettings.Default.copy(baseFare = 2.0))
        compose.onNodeWithText("Solicitar viaje").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Las tarifas se actualizaron. Revisa el nuevo precio y confirma de nuevo.").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { assertNull(requested.get()) }
        compose.onAllNodesWithText("S/ 6.70").onFirst().assertExists()
        compose.onNodeWithText("Solicitar viaje").performClick()
        compose.waitUntil(20_000) { requested.get() != null }
        assertEquals(settings.get(),requested.get().fareSettings)
        assertEquals(6.7,requested.get().estimatedPrice,0.0)
    }
}
