package com.intu.taxi

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.ui.map.TripMap
import com.intu.taxi.ui.map.TripRoute
import com.intu.taxi.ui.map.pointUnderCenterPin
import com.intu.taxi.ui.screens.*
import com.intu.taxi.ui.theme.*
import com.intu.taxi.repositories.RideHistoryItem
import com.intu.taxi.updates.AppUpdateState
import com.intu.taxi.updates.PublishedAppRelease
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Isolated appearance and GPS fixtures. No real accounts, requests or SMS are created. */
class DarkModeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun preferencesAreOptInAndIsolatedPerAccountAndSurviveReopening() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "qa_appearance_${System.nanoTime()}"
        try {
            val store = AppearancePreferences(context, name)
            assertFalse(store.isDark("alice"))
            assertFalse(store.isDark(null))
            store.setDark("alice", true)
            assertTrue(AppearancePreferences(context, name).isDark("alice"))
            assertFalse(store.isDark("bob"))
            assertFalse(store.isDark(null))
            store.setDark("alice", false)
            assertFalse(AppearancePreferences(context, name).isDark("alice"))
        } finally { context.getSharedPreferences(name, 0).edit().clear().commit() }
    }

    @Test fun accountToggleIsAvailableToPassengersAndDriversAndChangesPalette() {
        var dark by mutableStateOf(false)
        var driver by mutableStateOf(false)
        var paletteIsDark = false
        compose.setContent {
            CompositionLocalProvider(LocalAppearanceController provides AppearanceController(dark, true) { dark = it }) {
                IntuTheme(darkTheme = dark) {
                    val palette = MaterialTheme.colorScheme.background.luminance() < .2f
                    SideEffect { paletteIsDark = palette }
                    AccountScreenEnhanced(PaddingValues(top = 24.dp, bottom = 24.dp), driver, {})
                }
            }
        }
        // No admin callback is provided: this is the ordinary account screen.
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("account-dark-mode").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasTestTag("account-dark-mode") and hasAnyAncestor(hasTestTag("account-settings"))).assertExists()
        compose.onNodeWithTag("account-dark-mode").performScrollTo().assertIsOff().performClick()
        compose.onNodeWithTag("account-dark-mode").assertIsOn()
        compose.runOnIdle { assertTrue(paletteIsDark) }
        captureNativeScreenshot(compose, "dark-account.png")
        compose.runOnIdle { driver = true }
        compose.onNodeWithTag("account-dark-mode").performScrollTo().assertIsOn().performClick()
        compose.onNodeWithTag("account-dark-mode").assertIsOff()
        compose.runOnIdle { assertFalse(paletteIsDark) }
        captureNativeScreenshot(compose, "light-account.png")
    }

    @Test fun liveMapStyleChangeKeepsDestinationRouteVehicleAndCamera() {
        var dark by mutableStateOf(false)
        val plannedRoute = AtomicReference<TripRoute>()
        val ready = AtomicBoolean()
        val gps = object : LocationProvider {
            override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
                locationConsumer.onLocationUpdated(Point.fromLngLat(-74.6382, -11.2521))
                ready.set(true)
            }
            override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) = Unit
        }
        compose.setContent { IntuTheme(darkTheme = dark) {
            HomeScreen(PaddingValues(), locationProvider = gps,
                routeLoader = { origin, destination -> TripRoute(listOf(origin, destination), 1800.0, 360.0).also { plannedRoute.set(it) } },
                rideRequestSender = { error("QA must never request a trip") })
        } }
        waitForText("Elegir en mapa")
        compose.waitUntil(60_000) { ready.get() }
        compose.onNodeWithTag("home-pick-destination").performClick()
        waitForText("Confirmar destino")
        val map = mapView()
        // Use distinct pickup/destination coordinates so preserving a zero-length draft cannot pass.
        // The real map controller reports this camera position to the same selected-pin state as a drag.
        compose.runOnIdle { map.mapboxMap.setCamera(CameraOptions.Builder()
            .center(Point.fromLngLat(-74.6315, -11.256)).zoom(16.0).build()) }
        SystemClock.sleep(1800)
        compose.onNodeWithText("Confirmar destino").performClick()
        waitForText("Elige tu moto")
        compose.runOnIdle {
            val route = plannedRoute.get()
            assertTrue("A real nondegenerate route must be visible", TripMap.metersBetween(route.points.first(), route.points.last()) > 100.0)
        }
        compose.onNodeWithTag("moto-option-honda").performScrollTo().performClick()
        SystemClock.sleep(1500)
        val camera = compose.runOnIdle { map.mapboxMap.cameraState }
        val pin = compose.onNodeWithTag("home-confirmed-destination-pin").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { dark = true }
        waitForStyle(map, Style.DARK)
        SystemClock.sleep(1500)
        compose.runOnIdle {
            assertTrue(TripMap.metersBetween(camera.center, map.mapboxMap.cameraState.center) < 1.0)
            assertEquals(camera.zoom, map.mapboxMap.cameraState.zoom, .01)
            assertFalse(map.scalebar.enabled)
            assertTrue(map.logo.enabled)
        }
        compose.onNodeWithTag("moto-option-honda").performScrollTo().assertIsSelected()
        val darkPin = compose.onNodeWithTag("home-confirmed-destination-pin").fetchSemanticsNode().boundsInRoot
        assertEquals(pin.center.x, darkPin.center.x, 1f)
        assertEquals(pin.center.y, darkPin.center.y, 1f)
        captureNativeScreenshot(compose, "dark-route-motos.png")
        compose.onNodeWithTag("moto-drawer-handle").performClick()
        compose.onNodeWithContentDescription("Mostrar opciones de moto").assertIsDisplayed()
        captureNativeScreenshot(compose, "dark-route-compact.png")
        compose.runOnIdle { dark = false }
        waitForStyle(map, Style.MAPBOX_STREETS)
        compose.onNodeWithText("Mototaxi Honda").assertIsDisplayed()
        compose.onNodeWithText("Elegir recojo").assertIsEnabled()
    }

    @Test fun darkCatalogPickerKeepsPinCenteredWhenZoomingAndConfirmsPoint() {
        var picked: Point? = null
        compose.setContent { IntuTheme(darkTheme = true) {
            PlacePointPicker(Point.fromLngLat(-74.6382, -11.2521), {}, { picked = it })
        } }
        compose.waitUntil(60_000) { compose.onAllNodesWithTag("place-map-ready").fetchSemanticsNodes().isNotEmpty() }
        val map = mapView()
        waitForStyle(map, Style.DARK)
        SystemClock.sleep(1500)
        val point = compose.runOnIdle {
            assertFalse(map.scalebar.enabled)
            assertTrue(map.logo.enabled)
            map.pointUnderCenterPin()!!
        }
        val zoom = compose.runOnIdle { map.mapboxMap.cameraState.zoom }
        NativeMapGestures(map).pinchOut()
        SystemClock.sleep(1500)
        compose.runOnIdle {
            assertTrue(map.mapboxMap.cameraState.zoom < zoom - .5)
            assertTrue(TripMap.metersBetween(point, map.pointUnderCenterPin()!!) < 1.0)
        }
        captureNativeScreenshot(compose, "dark-catalog-map.png")
        compose.onNodeWithText("Confirmar punto").assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(TripMap.metersBetween(point, picked!!) < 1.0) }
    }

    @Test fun darkHistoryLoadsStoredRouteWithoutDirectionsAndCanClose() {
        val points = listOf(Point.fromLngLat(-74.6382, -11.2521), Point.fromLngLat(-74.635, -11.257))
        val ride = RideHistoryItem("qa-dark-history", "completed", null,
            "Plaza principal de Satipo", "Hospital de Satipo", 5.2, "efectivo",
            "Conductor de prueba", "", "Mototaxi Honda", "QA-123", "Pasajero de prueba", "", null, null,
            distanceMeters = 1800, durationSeconds = 360)
        var closed = false
        compose.setContent { IntuTheme(darkTheme = true) {
            RideDetailsDialog(ride, mutableMapOf(ride.id to points)) { closed = true }
        } }
        compose.onNodeWithText("Ruta planificada").performScrollTo()
        val map = mapView()
        waitForStyle(map, Style.DARK)
        compose.runOnIdle { assertFalse(map.scalebar.enabled); assertTrue(map.logo.enabled) }
        captureNativeScreenshot(compose, "dark-trip-details.png")
        compose.onNodeWithText("Cerrar").performClick()
        compose.runOnIdle { assertTrue(closed) }
    }

    @Test fun darkUpdateDialogKeepsActionsAccessible() {
        val release = PublishedAppRelease(30, "1.29", 106000000, "a".repeat(64))
        var download = false
        var closed = false
        compose.setContent { IntuTheme(darkTheme = true) {
            AppUpdateDialog(AppUpdateState(release = release, checked = true), 29, "1.28", false,
                {}, { download = true }, { closed = true })
        } }
        captureNativeScreenshot(compose, "dark-update-dialog.png")
        compose.onNodeWithTag("app-update-download").assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(download) }
        compose.onNodeWithText("Más tarde").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(closed) }
    }

    private fun mapView(): MapView {
        lateinit var map: MapView
        onView(isAssignableFrom(MapView::class.java)).check { view, noMatch ->
            if (noMatch != null) throw noMatch
            map = view as MapView
        }
        return map
    }
    private fun waitForStyle(map: MapView, uri: String) {
        compose.waitUntil(60_000) { compose.runOnIdle { map.mapboxMap.style?.styleURI == uri } }
    }
    private fun waitForText(text: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
}
