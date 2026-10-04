package com.intu.taxi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.intu.taxi.ui.map.TripRoute
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.screens.CommercialHome
import com.intu.taxi.ui.screens.DestinationSearchPanel
import com.intu.taxi.data.SavedPlace
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import com.intu.taxi.data.PlaceSearchResult
import com.intu.taxi.data.PlaceSearchSource
import androidx.compose.material3.Text
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

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
        compose.onNodeWithTag("home-pick-destination").assertDoesNotExist()
        compose.onNodeWithTag("home-place-casa").assertDoesNotExist()
        compose.onNodeWithTag("home-place-trabajo").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performClick()
        compose.onNodeWithTag("destination-search-panel").assertIsDisplayed()
        closeSoftKeyboard()
        captureNativeScreenshot(compose, "destination-panel-empty-dark.png")
        compose.onNodeWithTag("home-pick-destination").performScrollTo().performClick()
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

    @Test fun lightLandingSearchShowsMapImmediatelyAndBackRestoresPresentation() {
        launch(dark = false)
        compose.onNodeWithTag("commercial-home").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performClick()
        compose.onNodeWithTag("home-map").assertIsDisplayed()
        compose.onNodeWithTag("home-search-overlay").assertIsDisplayed()
        compose.onNodeWithTag("commercial-home").assertDoesNotExist()
        compose.onNodeWithTag("home-place-casa").assertIsDisplayed()
        compose.onNodeWithTag("home-place-trabajo").assertIsDisplayed()
        compose.onAllNodesWithText("Elegir en mapa").assertCountEquals(1)
        compose.onNodeWithText("No encontramos ese lugar. Puedes elegirlo en el mapa.").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).assertIsFocused().performTextInput("zzzz")
        compose.onNodeWithTag("home-map").assertIsDisplayed()
        compose.onNodeWithText("Tu día se mueve\ncon Intu.").assertDoesNotExist()
        // Android handles IME dismissal before the screen's BackHandler.
        compose.onNodeWithContentDescription("Limpiar").performClick()
        closeSoftKeyboard()
        pressBack()
        compose.onNodeWithTag("commercial-home").assertIsDisplayed()
        // The native keyboard inset animation can finish after Compose becomes idle.
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Tu día se mueve\ncon Intu.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Tu día se mueve\ncon Intu.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("home-map").assertDoesNotExist()
        compose.onNodeWithTag("home-place-casa").assertDoesNotExist()
        compose.onNodeWithTag("home-pick-destination").assertDoesNotExist()
        captureNativeScreenshot(compose, "commercial-home-light.png")
    }

    @Test fun darkSearchPreservesInputOverMapAndCanContinueToDestinationPicker() {
        launch(dark = true)
        compose.onNode(hasSetTextAction()).performClick()
        compose.onNodeWithTag("home-map").assertIsDisplayed()
        compose.onNodeWithTag("home-search-overlay").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertIsFocused().performTextInput("zzzz")
        closeSoftKeyboard()
        compose.onNode(hasSetTextAction()).assertTextContains("zzzz")
        compose.onNodeWithTag("home-map").assertIsDisplayed()
        captureNativeScreenshot(compose, "home-search-map-dark.png")
        compose.onNodeWithTag("home-pick-destination").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar destino").assertIsDisplayed()
        compose.onNodeWithTag("home-search-overlay").assertDoesNotExist()
        pressBack()
        compose.onNodeWithTag("commercial-home").assertIsDisplayed()
        compose.onNodeWithTag("home-map").assertDoesNotExist()
    }

    @Test fun relocatedPlacesKeepSavedDestinationsAndManageAction() {
        val home = SavedPlace("casa", "Casa", "Mi casa", -11.25, -74.63)
        val work = SavedPlace("trabajo", "Trabajo", "Mi trabajo", -11.24, -74.62)
        val selected = AtomicReference<SavedPlace>()
        val managing = AtomicBoolean()
        val searching = mutableStateOf(false)
        val result = PlaceSearchResult("qa:park", "Parque Central", "Av. Principal", -11.26, -74.64, PlaceSearchSource.MAPBOX)
        val selectedResult = AtomicReference<PlaceSearchResult>()
        val pickingMap = AtomicBoolean()
        compose.setContent { IntuTheme(darkTheme = true) {
            CommercialHome(PaddingValues(), "Carlos", searchActive = searching.value,
                searchContent = {
                    TextButton(onClick = { searching.value = true }) { Text("¿A dónde vamos?") }
                    if (searching.value) DestinationSearchPanel("Parque", listOf(home, work), listOf(result),
                        true, false, null, false, null, selectedResult::set, selected::set, {},
                        { pickingMap.set(true) }, { managing.set(true) })
                }, onTravel = {}, onDelivery = {})
        } }
        compose.onNodeWithTag("home-place-casa").assertDoesNotExist()
        compose.onNodeWithTag("home-place-trabajo").assertDoesNotExist()
        compose.onNodeWithTag("home-pick-destination").assertDoesNotExist()
        compose.onNodeWithText("¿A dónde vamos?").performClick()
        compose.onNode(hasTestTag("home-place-casa") and hasAnyAncestor(hasTestTag("destination-search-panel")))
            .performScrollTo().performClick()
        assertEquals(home, selected.get())
        compose.onNodeWithTag("home-place-trabajo").performScrollTo().performClick()
        assertEquals(work, selected.get())
        compose.onNodeWithText("Parque Central").performScrollTo().performClick()
        assertEquals(result, selectedResult.get())
        compose.onNodeWithTag("home-manage-places").performScrollTo().performClick()
        assertTrue(managing.get())
        compose.onNodeWithTag("home-pick-destination").performScrollTo().performClick()
        assertTrue(pickingMap.get())
        compose.onAllNodesWithText("Elegir en mapa").assertCountEquals(1)
        captureNativeScreenshot(compose, "destination-list-dark.png")
    }
}
