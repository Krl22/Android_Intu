package com.intu.taxi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.data.SavedPlace
import com.intu.taxi.data.SavedPlaces
import com.intu.taxi.ui.map.TripRoute
import com.intu.taxi.ui.map.FixedLocationProvider
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Local route preview and request callbacks; restores saved places and never requests a taxi. */
class SavedShortcutFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun casaSkipsDestinationConfirmation() = verifyShortcut("casa", "Casa")
    @Test fun trabajoSkipsDestinationConfirmation() = verifyShortcut("trabajo", "Trabajo")
    @Test fun favoriteSkipsDestinationConfirmation() = verifyShortcut("qa-shortcut", "Cafetería QA")

    private fun verifyShortcut(id: String, label: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uid = checkNotNull(FirebaseAuth.getInstance().currentUser?.uid) { "Se requiere una sesión para probar atajos guardados." }
        val prefs = context.getSharedPreferences("intu_saved_places", 0)
        val key = "places_$uid"
        val previous = prefs.getString(key, null)
        val destination = Point.fromLngLat(-74.638, -11.252)
        val routedDestination = AtomicReference<Point>()
        val requests = AtomicInteger()
        try {
            SavedPlaces(context, uid).save(SavedPlace(id, label, "Referencia QA", destination.latitude(), destination.longitude()))
            compose.setContent { IntuTheme {
                HomeScreen(PaddingValues(), locationProvider = FixedLocationProvider(Point.fromLngLat(-74.64, -11.25)), routeLoader = { origin, target ->
                    routedDestination.set(target)
                    TripRoute(listOf(origin, target), 1500.0, 240.0)
                }, rideRequestSender = {
                    requests.incrementAndGet()
                    Result.failure(IllegalStateException("Las pruebas de atajos no deben solicitar viajes."))
                })
            } }
            compose.onNodeWithText(label).performScrollTo().performClick()
            compose.waitUntil(15_000) { routedDestination.get() != null }
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Elige tu viaje").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Elige tu viaje").assertIsDisplayed()
            compose.onNodeWithText("Confirmar destino").assertDoesNotExist()
            compose.onNodeWithText("Elegir punto de recojo").assertDoesNotExist()
            assertEquals(destination.longitude(), routedDestination.get().longitude(), 0.000001)
            assertEquals(destination.latitude(), routedDestination.get().latitude(), 0.000001)
            assertEquals("Selecting a shortcut only prepares the trip", 0, requests.get())
        } finally {
            if (previous == null) prefs.edit().remove(key).commit()
            else prefs.edit().putString(key, previous).commit()
        }
    }
}
