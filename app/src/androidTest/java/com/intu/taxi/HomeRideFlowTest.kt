package com.intu.taxi

import android.os.SystemClock
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import com.intu.taxi.ui.map.TripMap
import com.intu.taxi.ui.map.TripRoute
import com.intu.taxi.ui.map.pointUnderCenterPin
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.screens.RideBooking
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred

class HomeRideFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pickupIsChosenAfterVehicleAndTripConfirmationAndRecalculatesTheRequest() {
        val requested = AtomicReference<RideBooking>()
        val destinationPreview = AtomicReference<TripRoute>()
        val releaseRequest = CompletableDeferred<Unit>()
        val gps = FakeLocationProvider(Point.fromLngLat(-74.6382, -11.2521))
        compose.setContent { IntuTheme {
            HomeScreen(PaddingValues(),
                locationProvider = gps,
                routeLoader = { origin, destination ->
                    TripRoute(listOf(origin, destination), if (destinationPreview.get() == null) 1200.0 else 2100.0,
                        if (destinationPreview.get() == null) 240.0 else 420.0).also { destinationPreview.compareAndSet(null, it) }
                },
                rideRequestSender = { booking ->
                    requested.set(booking)
                    releaseRequest.await()
                    Result.failure(IllegalStateException("Solicitud simulada para QA"))
                })
        } }
        waitForText("Marcador")
        compose.waitUntil(60_000) { gps.ready.get() }
        compose.onNodeWithText("Marcador").performClick()
        waitForText("Confirmar destino")
        compose.onNodeWithText("Elegir punto de recojo").assertDoesNotExist()
        compose.onNodeWithText("Confirmar punto").assertDoesNotExist()
        val previewMap = AtomicReference<MapView>()
        onView(isAssignableFrom(MapView::class.java)).check { view, noMatch ->
            if (noMatch != null) throw noMatch
            previewMap.set(view as MapView)
        }
        // A real destination away from pickup gives the camera a nondegenerate route to fit.
        NativeMapGestures(previewMap.get()).pan()
        SystemClock.sleep(800)
        compose.onNodeWithText("Confirmar destino").performClick()
        waitForText("Elige tu moto")
        compose.waitForIdle()
        // Semantics can precede the displayed frame while the panel animates in.
        SystemClock.sleep(1200)
        val optionsBounds = compose.onNodeWithTag("ride-options-sheet").fetchSemanticsNode().boundsInRoot
        val optionsRoot = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("The compact options panel must leave room for the route", optionsBounds.height < optionsRoot.height * .65f)
        val expandedPin = compose.onNodeWithTag("home-confirmed-destination-pin").fetchSemanticsNode().boundsInRoot
        assertTrue("Destination must stay above the expanded panel: pin=$expandedPin, panel=$optionsBounds",
            expandedPin.top >= 0f && expandedPin.bottom < optionsBounds.top)
        val optionsInstrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val optionsScreenshot = optionsInstrumentation.uiAutomation.takeScreenshot()
        java.io.File(optionsInstrumentation.targetContext.getExternalFilesDir(null), "qa-ride-options-1-22.png").outputStream().use {
            optionsScreenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Mototaxi Honda").performScrollTo().performClick()
        compose.onNodeWithTag("moto-drawer-handle").performClick()
        compose.onNodeWithContentDescription("Mostrar opciones de moto").assertIsDisplayed()
        compose.onNodeWithText("Mototaxi Honda").assertIsDisplayed()
        SystemClock.sleep(800)
        val compactBounds = compose.onNodeWithTag("ride-options-sheet").fetchSemanticsNode().boundsInRoot
        assertTrue("Collapsed drawer must reveal more of the route", compactBounds.height < optionsBounds.height * .8f)
        val compactPin = compose.onNodeWithTag("home-confirmed-destination-pin").fetchSemanticsNode().boundsInRoot
        assertTrue("Destination must stay above the collapsed panel", compactPin.top >= 0f && compactPin.bottom < compactBounds.top)
        val beforePreviewPan = compose.runOnIdle { previewMap.get().mapboxMap.cameraState.center }
        // Inject through the whole screen, so an invisible overlay would make this test fail.
        NativeMapGestures(previewMap.get()).panThroughWindow()
        SystemClock.sleep(800)
        val afterPreviewPan = compose.runOnIdle { previewMap.get().mapboxMap.cameraState.center }
        assertTrue("Exposed map must remain interactive: before=$beforePreviewPan, after=$afterPreviewPan",
            TripMap.metersBetween(beforePreviewPan, afterPreviewPan) > 1.0)
        val compactScreenshot = optionsInstrumentation.uiAutomation.takeScreenshot()
        java.io.File(optionsInstrumentation.targetContext.getExternalFilesDir(null), "qa-drawer-compact.png").outputStream().use {
            compactScreenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithTag("moto-drawer-handle").performClick()
        compose.onNodeWithTag("moto-option-honda").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Elegir punto de recojo").assertDoesNotExist()
        assertNull(requested.get())
        compose.onNodeWithText("Elegir recojo").assertIsEnabled().performClick()
        waitForText("Elegir punto de recojo")
        compose.onNodeWithTag("home-pickup-pin").assertIsDisplayed()
        compose.onNodeWithText("Guardar este lugar").assertDoesNotExist()
        assertNull("Trip confirmation must not send a request before pickup confirmation", requested.get())
        val map = AtomicReference<MapView>()
        onView(isAssignableFrom(MapView::class.java)).check { view, noMatch ->
            if (noMatch != null) throw noMatch
            map.set(view as MapView)
        }
        val bounds = compose.onNodeWithTag("home-map").fetchSemanticsNode().boundsInRoot
        val rootBounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Pickup must use the full-screen Home map", bounds.height >= rootBounds.height * .95f)
        compose.runOnIdle { assertEquals(0.0, map.get().mapboxMap.cameraState.padding.bottom, .01) }
        val initialPickup = compose.runOnIdle { map.get().pointUnderCenterPin()!! }
        val beforeZoomCamera = compose.runOnIdle { map.get().mapboxMap.cameraState }
        NativeMapGestures(map.get()).pinchOut()
        SystemClock.sleep(1500)
        val afterZoom = compose.runOnIdle { map.get().pointUnderCenterPin()!! }
        val afterZoomCamera = compose.runOnIdle { map.get().mapboxMap.cameraState }
        assertTrue("Zoom must keep the pickup under the center pin", TripMap.metersBetween(initialPickup, afterZoom) < 1.0)
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "qa-fullscreen-pickup.png").outputStream().use {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        NativeMapGestures(map.get()).pan()
        SystemClock.sleep(1500)
        val pickup = compose.runOnIdle { map.get().pointUnderCenterPin()!! }
        val afterPanCamera = compose.runOnIdle { map.get().mapboxMap.cameraState }
        assertTrue("Pan must move the pickup: before=$beforeZoomCamera, afterZoom=$afterZoomCamera, afterPan=$afterPanCamera",
            TripMap.metersBetween(initialPickup, pickup) > 5.0)
        compose.onNodeWithText("Solicitar viaje").performClick()
        compose.waitUntil(10_000) { requested.get() != null }
        val booking = requested.get()
        assertTrue(TripMap.metersBetween(pickup, booking.origin) < 1.0)
        assertEquals(destinationPreview.get().points.last(), booking.destination)
        assertEquals("mototaxi", booking.rideType)
        assertEquals("honda", booking.preferredVehicleBrand)
        assertEquals(pickup, booking.route.points.first())
        assertEquals(2100.0, booking.route.distanceMeters, 0.0)
        assertEquals(420.0, booking.route.durationSeconds, 0.0)
        assertEquals(5.9, booking.estimatedPrice, 0.0001) // Honda: 5.3 + 12% luggage premium
        compose.onNodeWithText("Solicitando viaje").assertIsDisplayed()
        compose.onNodeWithText("Solicitar viaje").assertDoesNotExist()
        compose.onNodeWithText("Solicitando viaje…").assertIsNotEnabled()
        releaseRequest.complete(Unit)
        waitForText("Solicitud simulada para QA")
        compose.onNodeWithText("Elige tu moto").assertIsDisplayed()
    }

    private class FakeLocationProvider(private val point: Point) : LocationProvider {
        val ready = java.util.concurrent.atomic.AtomicBoolean()
        override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
            locationConsumer.onLocationUpdated(point)
            ready.set(true)
        }
        override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) = Unit
    }

    private fun waitForText(text: String) {
        compose.waitUntil(30_000) {
            runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
    }
}

