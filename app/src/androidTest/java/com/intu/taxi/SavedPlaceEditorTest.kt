package com.intu.taxi

import android.os.SystemClock
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.data.SavedPlace
import com.intu.taxi.data.SavedPlaces
import com.intu.taxi.ui.map.TripMap
import com.intu.taxi.ui.map.pointUnderCenterPin
import com.intu.taxi.ui.map.firstPinLocation
import com.intu.taxi.ui.screens.SavedPlaceEditorDialog
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class SavedPlaceEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun blankNameDisablesSaveAndFailedSaveCanBeRetriedWithoutLosingTheForm() {
        val original = SavedPlace("qa-favorite", "Favorito", "", -11.253, -74.639)
        val saved = AtomicReference<SavedPlace>()
        var attempts = 0
        var dismissed = false
        compose.setContent { IntuTheme {
            SavedPlaceEditorDialog(place = original, onDismiss = { dismissed = true }, onSave = {
                attempts++
                if (attempts == 1) error("No se pudo guardar. Intenta de nuevo.")
                saved.set(it)
            })
        } }
        captureNativeScreenshot(compose, "saved-place-form-normal.png")
        compose.onNodeWithTag("saved-place-name").performTextReplacement("  ")
        compose.onNodeWithText("Guardar").assertIsNotEnabled()
        compose.onNodeWithTag("saved-place-name").performTextReplacement("Parque")
        compose.onNodeWithTag("saved-place-reference").performScrollTo().performTextReplacement("Entrada norte")
        compose.onNodeWithText("Guardar").assertIsDisplayed().performClick()
        compose.onNodeWithTag("saved-place-error").performScrollTo().assertIsDisplayed()
        assertFalse(dismissed)
        compose.onNodeWithText("Guardar").performClick()
        assertEquals(original.copy(name = "Parque", address = "Entrada norte"), saved.get())
        assertTrue(dismissed)
    }

    @Test fun enlargedTextAndKeyboardKeepTheSaveActionReachable() {
        val saved = AtomicReference<SavedPlace>()
        compose.setContent { IntuTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.8f)) {
                SavedPlaceEditorDialog(point = Point.fromLngLat(-74.639, -11.253),
                    onDismiss = {}, onSave = saved::set)
            }
        } }
        captureNativeScreenshot(compose, "saved-place-form-large.png")
        compose.onNodeWithTag("saved-place-reference").performScrollTo().performTextReplacement("Junto al parque")
        captureNativeScreenshot(compose, "saved-place-form-large-keyboard.png")
        compose.onNodeWithText("Guardar").assertIsDisplayed().assertIsEnabled().performClick()
        assertEquals("Junto al parque", saved.get().address)
        assertEquals("Favorito", saved.get().name)
    }

    @Test fun addingCasaWaitsForGpsAndNeverRecentersAfterTheUserMovesThePin() {
        val gps = FakeLocationProvider()
        val current = Point.fromLngLat(-77.0428, -12.0464)
        val saved = AtomicReference<SavedPlace>()
        compose.setContent { IntuTheme {
            SavedPlaceEditorDialog(initialName = "Casa", onDismiss = {}, locationProvider = gps, onSave = saved::set)
        } }
        compose.onNodeWithText("Buscando tu ubicación…").assertIsDisplayed()
        compose.onNodeWithText("Confirmar punto").assertIsNotEnabled()
        compose.runOnIdle { gps.emit(Point.fromLngLat(0.0, 0.0)) }
        compose.onNodeWithText("Confirmar punto").assertIsNotEnabled()
        compose.runOnIdle { gps.emit(current) }
        val map = readyMap()
        val initial = compose.runOnIdle { map.pointUnderCenterPin()!! }
        assertTrue("The pin must start at the current GPS position", TripMap.metersBetween(current, initial) < 1.0)
        assertEquals(0, gps.consumers.size)
        NativeMapGestures(map).pan()
        SystemClock.sleep(1500)
        val selected = compose.runOnIdle { map.pointUnderCenterPin()!! }
        compose.runOnIdle { gps.emit(Point.fromLngLat(-77.05, -12.05)) }
        compose.waitForIdle()
        assertTrue(TripMap.metersBetween(selected, compose.runOnIdle { map.pointUnderCenterPin()!! }) < 1.0)
        compose.onNodeWithText("Confirmar punto").performClick()
        compose.onNodeWithText("Guardar").performClick()
        assertPoint(selected, saved.get())
    }

    @Test fun locationLookupStopsOnTimeoutAndCancellation() = runBlocking {
        val gps = FakeLocationProvider()
        assertNull(firstPinLocation(gps, timeoutMillis = 30))
        assertTrue(gps.consumers.isEmpty())
        val lookup = launch { firstPinLocation(gps) }
        yield()
        assertEquals(1, gps.consumers.size)
        lookup.cancelAndJoin()
        assertTrue(gps.consumers.isEmpty())
    }

    @Test fun unavailableGpsStillOffersManualSelection() {
        val gps = FakeLocationProvider()
        compose.setContent { IntuTheme {
            SavedPlaceEditorDialog(initialName = "Trabajo", onDismiss = {}, locationProvider = gps, onSave = {})
        } }
        compose.mainClock.advanceTimeBy(10_100)
        readyMap()
        compose.onNodeWithText("No pudimos obtener tu ubicación. Elige el punto en el mapa.").assertIsDisplayed()
        compose.onNodeWithText("Confirmar punto").assertIsEnabled()
        assertTrue(gps.consumers.isEmpty())
    }

    @Test fun addingCasaOpensCatalogMapAndPersistsTheMovedPin() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uid = "qa-saved-map-${UUID.randomUUID()}"
        val store = SavedPlaces(context, uid)
        try {
            compose.setContent { IntuTheme {
                SavedPlaceEditorDialog(initialName = "Casa", onDismiss = {}, onSave = store::save)
            } }
            compose.onNodeWithText("Ubicación de Casa").assertIsDisplayed()
            compose.onNodeWithText("Guardar").assertDoesNotExist()
            val map = readyMap()
            val before = compose.runOnIdle { map.pointUnderCenterPin()!! }
            NativeMapGestures(map).pan()
            SystemClock.sleep(1500)
            val selected = compose.runOnIdle { map.pointUnderCenterPin()!! }
            assertTrue(TripMap.metersBetween(before, selected) > 5.0)
            compose.onNodeWithText("Confirmar punto").performClick()
            compose.onNodeWithTag("saved-place-reference").performTextInput("Entrada junto al parque")
            compose.onNodeWithText("Guardar").performClick()
            val saved = SavedPlaces(context, uid).read().single()
            assertEquals("casa", saved.id)
            assertEquals("Casa", saved.name)
            assertEquals("Entrada junto al parque", saved.address)
            assertPoint(selected, saved)
        } finally {
            context.getSharedPreferences("intu_saved_places", 0).edit().remove("places_$uid").commit()
        }
    }

    @Test fun addingTrabajoCanSaveThePinWithoutATypedAddress() {
        val saved = AtomicReference<SavedPlace>()
        compose.setContent { IntuTheme {
            SavedPlaceEditorDialog(initialName = "Trabajo", onDismiss = {}, onSave = saved::set)
        } }
        compose.onNodeWithText("Ubicación de Trabajo").assertIsDisplayed()
        val map = readyMap()
        val selected = compose.runOnIdle { map.pointUnderCenterPin()!! }
        compose.onNodeWithText("Confirmar punto").performClick()
        compose.onNodeWithText("Guardar").assertIsEnabled().performClick()
        assertEquals("trabajo", saved.get().id)
        assertEquals("Trabajo", saved.get().address)
        assertPoint(selected, saved.get())
    }

    @Test fun editingReopensAtSavedLocationAndCancelRetainsCoordinatesAndReference() {
        val original = SavedPlace("casa", "Casa", "Referencia anterior", -11.253, -74.639)
        val saved = AtomicReference<SavedPlace>()
        compose.setContent { IntuTheme {
            SavedPlaceEditorDialog(place = original, onDismiss = {}, onSave = saved::set)
        } }
        compose.onNodeWithTag("saved-place-reference").performTextReplacement("Nueva referencia")
        ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4")
        ).use { it.readBytes() }
        compose.onNodeWithText("Cambiar ubicación en el mapa").performScrollTo().performClick()
        val map = readyMap()
        assertTrue(TripMap.metersBetween(Point.fromLngLat(original.longitude, original.latitude),
            compose.runOnIdle { map.pointUnderCenterPin()!! }) < 1.0)
        NativeMapGestures(map).pan()
        SystemClock.sleep(1500)
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithTag("saved-place-reference").assertTextContains("Nueva referencia")
        compose.onNodeWithText("Guardar").performClick()
        assertEquals(original.copy(address = "Nueva referencia"), saved.get())
    }

    @Test fun savingFromHomeMarkerUsesTheSuppliedPointAndCanChangeIt() {
        val initial = Point.fromLngLat(-74.64, -11.254)
        val saved = AtomicReference<SavedPlace>()
        compose.setContent { IntuTheme {
            SavedPlaceEditorDialog(point = initial, onDismiss = {}, onSave = saved::set)
        } }
        compose.onNodeWithText("Guardar").assertIsEnabled()
        compose.onNodeWithText("Confirmar punto").assertDoesNotExist()
        compose.onNodeWithText("Cambiar ubicación en el mapa").performClick()
        val map = readyMap()
        NativeMapGestures(map).pan()
        SystemClock.sleep(1500)
        val selected = compose.runOnIdle { map.pointUnderCenterPin()!! }
        compose.onNodeWithText("Confirmar punto").performClick()
        compose.onNodeWithText("Guardar").performClick()
        assertTrue(TripMap.metersBetween(initial, selected) > 5.0)
        assertPoint(selected, saved.get())
    }

    @Test fun cancelingInitialMapDismissesWithoutSaving() {
        var dismissed = false
        var saved = false
        compose.setContent { IntuTheme {
            SavedPlaceEditorDialog(initialName = "Casa", onDismiss = { dismissed = true }, onSave = { saved = true })
        } }
        compose.onNodeWithText("Volver").performClick()
        assertTrue(dismissed)
        assertFalse(saved)
    }

    private fun readyMap(): MapView {
        compose.waitUntil(60000) { compose.onAllNodesWithTag("place-map-ready").fetchSemanticsNodes().isNotEmpty() }
        val nativeMap = AtomicReference<MapView>()
        onView(isAssignableFrom(MapView::class.java)).inRoot(isDialog()).check { view, noMatch ->
            if (noMatch != null) throw noMatch
            nativeMap.set(view as MapView)
        }
        return nativeMap.get()
    }

    private fun assertPoint(expected: Point, saved: SavedPlace) {
        assertTrue("Saved coordinates must match the point under the pin",
            TripMap.metersBetween(expected, Point.fromLngLat(saved.longitude, saved.latitude)) < 1.0)
    }

    private class FakeLocationProvider : LocationProvider {
        val consumers = mutableSetOf<LocationConsumer>()
        fun emit(point: Point) { consumers.toList().forEach { it.onLocationUpdated(point) } }
        override fun registerLocationConsumer(locationConsumer: LocationConsumer) { consumers += locationConsumer }
        override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) { consumers -= locationConsumer }
    }
}
