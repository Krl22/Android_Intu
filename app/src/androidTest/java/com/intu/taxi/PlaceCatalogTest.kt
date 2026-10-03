package com.intu.taxi

import android.graphics.Bitmap
import android.os.SystemClock
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.RootMatchers.isDialog
import com.intu.taxi.data.CatalogPlace
import com.intu.taxi.data.PlaceSearchResult
import com.intu.taxi.data.PlaceSearchSource
import com.intu.taxi.data.asSearchResult
import com.intu.taxi.data.mergePlaceSearchResults
import com.intu.taxi.repositories.PlaceCatalogRepository
import com.intu.taxi.ui.screens.PlaceEditorDialog
import com.intu.taxi.ui.screens.PlacePointPicker
import com.intu.taxi.ui.screens.PlaceSearchPanel
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.screens.rememberAddressSearch
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import com.intu.taxi.ui.theme.IntuTheme
import com.intu.taxi.ui.map.TripMap
import com.intu.taxi.ui.map.pointUnderCenterPin
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.attribution.attribution
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class PlaceCatalogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cacheRetainsOfflineDataAndRemovesUnpublishedPlaces() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("intu_place_catalog_test", 0).edit().clear().commit()
        val repository = PlaceCatalogRepository(context, "intu_place_catalog_test")
        var calls = 0
        val row = JSONObject().put("id", "qa-place").put("name", "Hospital QA").put("latitude", -11.252).put("longitude", -74.638)
            .put("status", "published").put("pickup_verified", true)
        val first = repository.sync(fetch = { calls++; response(1, true, JSONArray().put(row)) })
        assertEquals(1, first.places.size)
        repository.sync(fetch = { calls++; error("Fresh cache should not fetch") })
        assertEquals(1, calls)
        val unchanged = repository.sync(force = true, fetch = { response(1, false) })
        assertEquals(first.places, unchanged.places)
        assertTrue(runCatching { repository.sync(force = true, fetch = { error("Offline") }) }.isFailure)
        assertEquals(first.places, repository.cached().places)
        val removed = repository.sync(force = true, fetch = { response(2, true) })
        assertTrue(removed.places.isEmpty())
        assertTrue(PlaceCatalogRepository(context, "intu_place_catalog_test").cached().places.isEmpty())
        assertEquals(2L, repository.cached().revision)
    }

    @Test fun publicationRequiresVerifiedPickupAndMovingItResetsConfirmation() {
        compose.setContent { IntuTheme { PlaceEditorDialog(null, {}, {}) } }
        compose.onNodeWithText("Nombre del lugar").performTextInput("Plaza QA")
        shell("input keyevent 4") // Hide the IME before tapping a status at the bottom of the form.
        compose.onNodeWithText("Publicado").performScrollTo().performClick()
        screenshot("qa-place-publication-state.png")
        compose.onNodeWithText("Guardar lugar").assertIsNotEnabled()
        compose.onNodeWithTag("pickup-verified").performScrollTo().performClick()
        compose.onNodeWithText("Guardar lugar").assertIsEnabled()
        compose.onNodeWithText("Editar coordenadas").performScrollTo().performClick()
        compose.onNodeWithText("Latitud").performScrollTo().performTextReplacement("-11.251")
        shell("input keyevent 4")
        compose.onNodeWithText("Guardar lugar").assertIsNotEnabled()
        compose.onNodeWithTag("pickup-verified").performScrollTo().performClick()
        compose.onNodeWithText("Guardar lugar").assertIsEnabled()
        screenshot("qa-place-editor.png")
    }

    @Test fun categoryAndLocalitySelectorsSaveTheirValuesWithAliasesAndReference() {
        val saved = AtomicReference<CatalogPlace>()
        compose.setContent { IntuTheme(darkTheme = true) { PlaceEditorDialog(null, {}, { saved.set(it) }) } }
        screenshot("qa-place-editor-overview.png")
        compose.onNodeWithText("Nombre del lugar").performTextInput("Plaza QA")
        shell("input keyevent 4")
        compose.onNodeWithTag("place-category").performScrollTo().performClick()
        compose.onNodeWithText("Plaza", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Otros nombres (opcional)").performScrollTo().performTextInput("plaza principal, parque central")
        shell("input keyevent 4")
        screenshot("qa-place-editor-details.png")
        compose.onNodeWithTag("place-locality").performScrollTo().performClick()
        compose.onNodeWithText("Río Negro").performClick()
        compose.onNodeWithText("Nombre de la localidad").assertDoesNotExist()
        compose.onNodeWithText("Dirección o referencia (opcional)").performScrollTo().performTextInput("Entrada frente al parque")
        shell("input keyevent 4")
        screenshot("qa-place-editor-location.png")
        compose.onNodeWithText("Guardar lugar").performClick()
        compose.waitUntil(5000) { saved.get() != null }
        assertEquals("square", saved.get().category)
        assertEquals("Río Negro", saved.get().locality)
        assertEquals(listOf("plaza principal", "parque central"), saved.get().aliases)
        assertEquals("Entrada frente al parque", saved.get().address)
        assertEquals("draft", saved.get().status)
    }

    @Test fun existingOtherLocalityIsPreservedAndCanBeChangedToPreset() {
        val original = CatalogPlace("qa-other", "Lugar QA", locality = "Mazamari", latitude = -11.252, longitude = -74.638)
        val saved = AtomicReference<CatalogPlace>()
        compose.setContent { IntuTheme { PlaceEditorDialog(original, {}, { saved.set(it) }) } }
        compose.onNodeWithText("Nombre de la localidad").performScrollTo().assertTextContains("Mazamari")
        compose.onNodeWithText("Guardar lugar").performClick()
        compose.waitUntil(5000) { saved.get() != null }
        assertEquals("Mazamari", saved.get().locality)
        compose.onNodeWithTag("place-locality").performScrollTo().performClick()
        compose.onNodeWithText("Satipo").performClick()
        saved.set(null)
        compose.onNodeWithText("Guardar lugar").performClick()
        compose.waitUntil(5000) { saved.get() != null }
        assertEquals("Satipo", saved.get().locality)
        compose.onNodeWithTag("place-locality").performScrollTo().performClick()
        compose.onAllNodesWithText("Otra localidad").onLast().performClick()
        compose.onNodeWithText("Guardar lugar").assertIsNotEnabled()
        compose.onNodeWithText("Nombre de la localidad").performScrollTo().performTextInput("Pangoa")
        shell("input keyevent 4")
        saved.set(null)
        compose.onNodeWithText("Guardar lugar").performClick()
        compose.waitUntil(5000) { saved.get() != null }
        assertEquals("Pangoa", saved.get().locality)
    }

    @Test fun emptyCatalogOffersManualMapAndRefresh() {
        var picked = false
        var refreshed = false
        compose.setContent { IntuTheme { PlaceSearchPanel(emptyList(), false, false, null, {}, { refreshed = true }, { picked = true }) } }
        compose.onNodeWithText("Elegir en mapa").performClick()
        compose.onNodeWithText("Actualizar lugares").performClick()
        assertTrue(picked && refreshed)
    }

    @Test fun selectedSuggestionRetainsPickupCoordinatesAndAttribution() {
        val place = CatalogPlace("qa", "Hospital QA", latitude = -11.252, longitude = -74.638, status = "published", pickupVerified = true, source = "OpenStreetMap")
        val chosen = AtomicReference<PlaceSearchResult>()
        compose.setContent { IntuTheme { PlaceSearchPanel(listOf(place.asSearchResult()), true, false, null, { chosen.set(it) }, {}, {}) } }
        compose.onNodeWithText("© OpenStreetMap contributors · ODbL 1.0").assertIsDisplayed()
        compose.onNodeWithText("Hospital QA").performClick()
        assertEquals(place.asSearchResult(), chosen.get())
    }

    @Test fun hybridSearchShowsCatalogFirstAndSelectsStreetCoordinates() {
        val place = CatalogPlace("qa", "Plaza QA", latitude = -11.252, longitude = -74.638, status = "published", pickupVerified = true)
        val street = PlaceSearchResult("mapbox:qa", "Jirón Lima", "Satipo", -11.251, -74.636, PlaceSearchSource.MAPBOX)
        val chosen = AtomicReference<PlaceSearchResult>()
        compose.setContent { IntuTheme {
            PlaceSearchPanel(mergePlaceSearchResults(listOf(place), listOf(street)), true, false, null, { chosen.set(it) }, {}, {})
        } }
        assertTrue(compose.onNodeWithText("Plaza QA").fetchSemanticsNode().boundsInRoot.top <
            compose.onNodeWithText("Jirón Lima").fetchSemanticsNode().boundsInRoot.top)
        compose.onNodeWithText("© Mapbox").assertIsDisplayed()
        compose.onNodeWithText("Jirón Lima").performClick()
        assertEquals(street, chosen.get())
    }

    @Test fun addressDebounceCancelsOldQueryAndNeverSearchesWhenDisabled() {
        val query = mutableStateOf("li")
        val enabled = mutableStateOf(false)
        val calls = java.util.Collections.synchronizedList(mutableListOf<String>())
        compose.setContent { IntuTheme {
            val state = rememberAddressSearch(query.value, enabled.value) { text ->
                calls.add(text)
                kotlinx.coroutines.delay(if (text == "lima") 1200 else 10)
                listOf(PlaceSearchResult("mapbox:$text", "Resultado $text", "Satipo", -11.252, -74.638, PlaceSearchSource.MAPBOX))
            }
            Column { state.results.forEach { Text(it.name) } }
        } }
        compose.mainClock.advanceTimeBy(700)
        assertTrue(calls.isEmpty())
        compose.runOnIdle { enabled.value = true; query.value = "lima" }
        compose.mainClock.advanceTimeBy(700)
        compose.waitUntil(5000) { calls.contains("lima") }
        compose.runOnIdle { query.value = "prado" }
        compose.mainClock.advanceTimeBy(700)
        compose.waitUntil(5000) { compose.onAllNodesWithText("Resultado prado").fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(2000)
        compose.onNodeWithText("Resultado lima").assertDoesNotExist()
        compose.runOnIdle { query.value = "p" }
        compose.mainClock.advanceTimeBy(700)
        compose.onNodeWithText("Resultado prado").assertDoesNotExist()
        assertEquals(listOf("lima", "prado"), calls.toList())
    }

    @Test fun addressFailurePreservesCatalogAndMapFallback() {
        val place = CatalogPlace("qa", "Plaza QA", latitude = -11.252, longitude = -74.638, status = "published", pickupVerified = true)
        compose.setContent { IntuTheme {
            val state = rememberAddressSearch("plaza", true) { error("Offline") }
            PlaceSearchPanel(mergePlaceSearchResults(listOf(place), state.results), true, false, null, {}, {}, {},
                addressLoading = state.loading, addressError = state.error)
        } }
        compose.mainClock.advanceTimeBy(700)
        compose.waitUntil(5000) { compose.onAllNodesWithText("No se pudieron buscar las direcciones. Revisa tu conexión o elige en el mapa.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Plaza QA").assertIsDisplayed()
        compose.onNodeWithText("Elegir en mapa").assertIsDisplayed()
    }

    @Test fun pickupMapRendersAndConfirmsMovedPoint() {
        val initial = Point.fromLngLat(-74.6382, -11.2521)
        val chosen = AtomicReference<Point>()
        compose.setContent { IntuTheme { PlacePointPicker(initial, {}, { chosen.set(it) }) } }
        compose.waitUntil(60000) { compose.onAllNodesWithTag("place-map-ready").fetchSemanticsNodes().isNotEmpty() }
        val nativeMap = AtomicReference<MapView>()
        onView(isAssignableFrom(MapView::class.java)).inRoot(isDialog()).check { view, noMatch ->
            if (noMatch != null) throw noMatch
            nativeMap.set(view as MapView)
        }
        val map = nativeMap.get()
        val beforeZoom = compose.runOnIdle {
            assertFalse(map.scalebar.enabled)
            assertTrue(map.logo.enabled)
            assertTrue(map.attribution.enabled)
            map.pointUnderCenterPin()!!
        }
        NativeMapGestures(map).pinchOut()
        SystemClock.sleep(1500)
        compose.runOnIdle {
            assertTrue("The catalog pinch must actually zoom out: zoom=${map.mapboxMap.cameraState.zoom}",
                map.mapboxMap.cameraState.zoom < 16.5)
            assertTrue("The catalog pin must retain its location during zoom",
                TripMap.metersBetween(beforeZoom, map.pointUnderCenterPin()!!) < 1.0)
        }
        NativeMapGestures(map).pan()
        SystemClock.sleep(1500)
        compose.waitUntil(10000) { compose.onAllNodesWithText("-11.2521000, -74.6382000").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
        val pointUnderPin = compose.runOnIdle { map.pointUnderCenterPin()!! }
        screenshot("qa-place-map.png")
        compose.onNodeWithText("Confirmar punto").performClick()
        assertNotNull(chosen.get())
        assertTrue("Panning must move the chosen pickup", TripMap.metersBetween(initial, chosen.get()) > 5.0)
        assertTrue("Confirm must use the location under the pin", TripMap.metersBetween(pointUnderPin, chosen.get()) < 1.0)
    }

    @Test fun homeSearchSelectsCachedPlaceAndExposesRefresh() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("intu_place_catalog", 0)
        val key = "v1_${BuildConfig.SUPABASE_URL}"
        val previous = prefs.getString(key, null)
        val row = JSONObject().put("id", "qa-home").put("name", "Plaza QA").put("latitude", -11.252).put("longitude", -74.638)
            .put("status", "published").put("pickup_verified", true).put("source", "OpenStreetMap")
        val snapshot = JSONObject().put("revision", 999).put("checked_at", System.currentTimeMillis()).put("places", JSONArray().put(row))
        val bottomVisible = AtomicReference<Boolean>()
        try {
            prefs.edit().putString(key, snapshot.toString()).commit()
            compose.setContent { IntuTheme { HomeScreen(PaddingValues()) { bottomVisible.set(it) } } }
            compose.onNodeWithText("¿A dónde quieres ir?").performTextInput("plaza qa")
            compose.waitUntil(10000) { compose.onAllNodesWithText("Plaza QA").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Plaza QA").assertIsDisplayed()
            compose.onNodeWithText("Actualizar lugares").assertIsDisplayed()
            screenshot("qa-home-place-search.png")
            compose.onNodeWithText("Plaza QA").performClick()
            compose.waitUntil(10000) { bottomVisible.get() == false }
        } finally {
            if (previous == null) prefs.edit().remove(key).commit() else prefs.edit().putString(key, previous).commit()
        }
    }

    private fun response(revision: Long, changed: Boolean, rows: JSONArray = JSONArray()) =
        JSONObject().put("revision", revision).put("changed", changed).put("places", rows)

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        SystemClock.sleep(500) // Let native keyboard/window transitions finish before capturing.
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }
}
