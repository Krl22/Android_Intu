package com.intu.taxi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import com.intu.taxi.ui.map.TripMap
import com.intu.taxi.ui.map.TripRoute
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** GPS/ruta/envío locales: no solicita viajes ni cambia la sesión del teléfono. */
class HomeBackNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun phoneBackReturnsHomeAtTheLatestLocationAndAllowsANewDestination() = verifyReturn { pressBack() }

    @Test fun screenArrowAlsoReturnsHomeAtTheLatestLocation() = verifyReturn {
        compose.onNodeWithContentDescription("Regresar").performClick()
    }

    @Test fun phoneBackFromDestinationPinReturnsHome() = verifyReturn("destination") { pressBack() }

    @Test fun arrowFromDestinationPinReturnsHome() = verifyReturn("destination") {
        compose.onNodeWithContentDescription("Regresar").performClick()
    }

    @Test fun phoneBackFromPickupPinDiscardsTheDraftAndReturnsHome() = verifyReturn("pickup") { pressBack() }

    @Test fun arrowFromPickupPinDiscardsTheDraftAndReturnsHome() = verifyReturn("pickup") {
        compose.onNodeWithContentDescription("Regresar").performClick()
    }

    @Test fun lateRouteResultCannotReopenADraftDiscardedWithBack() {
        val releaseRoute = kotlinx.coroutines.CompletableDeferred<Unit>()
        val entered = AtomicBoolean()
        val gps = FakeLocationProvider(Point.fromLngLat(-74.6382, -11.2521))
        compose.setContent { IntuTheme {
            HomeScreen(PaddingValues(), locationProvider = gps, routeLoader = { origin, destination ->
                entered.set(true)
                // Model a slow provider that returns even after its caller was cancelled.
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { releaseRoute.await() }
                TripRoute(listOf(origin, destination), 1800.0, 300.0)
            }, rideRequestSender = { error("Esta prueba nunca debe enviar viajes") })
        } }
        compose.waitUntil(60_000) { gps.consumers.isNotEmpty() }
        compose.onNodeWithText("Marcador").performClick()
        compose.onNodeWithText("Confirmar destino").performClick()
        compose.waitUntil(5000) { entered.get() }
        pressBack()
        compose.onNodeWithText("Marcador").assertIsDisplayed()
        compose.runOnIdle { releaseRoute.complete(Unit) }
        compose.waitForIdle()
        compose.onNodeWithText("Elige tu moto").assertDoesNotExist()
        compose.onNodeWithText("Confirmar destino").assertDoesNotExist()
    }

    private fun verifyReturn(stage: String = "options", goBack: () -> Unit) {
        val initial = Point.fromLngLat(-74.6382, -11.2521)
        val latest = Point.fromLngLat(-74.63, -11.245)
        val destination = Point.fromLngLat(-74.62, -11.24)
        val gps = FakeLocationProvider(initial)
        val requests = AtomicInteger()
        val bottomBarVisible = AtomicBoolean(true)
        val routeOrigin = AtomicReference<Point>()
        compose.setContent { IntuTheme {
            HomeScreen(PaddingValues(), locationProvider = gps,
                routeLoader = { origin, target ->
                    routeOrigin.set(origin)
                    TripRoute(listOf(origin, target), 1800.0, 300.0)
                }, rideRequestSender = {
                    requests.incrementAndGet()
                    Result.failure(IllegalStateException("No deben solicitarse viajes en esta prueba"))
                }, onBottomBarVisibilityChanged = bottomBarVisible::set)
        } }
        compose.waitUntil(60_000) { gps.consumers.isNotEmpty() }
        val mapRef = AtomicReference<MapView>()
        onView(isAssignableFrom(MapView::class.java)).check { view, noMatch ->
            if (noMatch != null) throw noMatch
            mapRef.set(view as MapView)
        }
        val map = mapRef.get()
        compose.onNodeWithText("Marcador").performClick()
        compose.runOnIdle { map.mapboxMap.setCamera(CameraOptions.Builder().center(destination).zoom(15.0).build()) }
        if (stage != "destination") {
            compose.onNodeWithText("Confirmar destino").assertIsEnabled().performClick()
            waitForText("Elige tu moto")
            assertTrue(TripMap.metersBetween(initial, routeOrigin.get()) < 1.0)
            if (stage == "pickup") {
                compose.onNodeWithText("Elegir recojo").performClick()
                waitForText("Elegir punto de recojo")
                compose.onNodeWithTag("home-pickup-pin").assertIsDisplayed()
            }
        }
        compose.waitUntil(5000) { !bottomBarVisible.get() }

        // Actualización posterior a la primera lectura: Atrás debe usar esta ubicación.
        compose.runOnIdle { gps.emit(latest) }
        goBack()
        compose.waitUntil(5000) { bottomBarVisible.get() }
        compose.onNodeWithText("Elige tu moto").assertDoesNotExist()
        compose.onNodeWithText("Confirmar destino").assertDoesNotExist()
        compose.onNodeWithText("Elegir punto de recojo").assertDoesNotExist()
        compose.onNodeWithText("Solicitar viaje").assertDoesNotExist()
        compose.onNodeWithText("Marcador").assertIsDisplayed()
        val camera = compose.runOnIdle { map.mapboxMap.cameraState }
        assertTrue("El inicio debe centrarse en el GPS más reciente", TripMap.metersBetween(latest, camera.center) < 1.0)
        assertEquals(0.0, camera.padding.bottom, 0.01)
        assertEquals(14.0, camera.zoom, 0.01)
        assertEquals(0, requests.get())

        // El siguiente destino parte de la ubicación actual, sin el recojo/ruta anteriores.
        compose.onNodeWithText("Marcador").performClick()
        compose.onNodeWithText("Confirmar destino").performClick()
        waitForText("Elige tu moto")
        assertTrue(TripMap.metersBetween(latest, routeOrigin.get()) < 1.0)
        assertEquals(0, requests.get())
    }

    private fun waitForText(text: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private class FakeLocationProvider(var point: Point) : LocationProvider {
        val consumers = mutableSetOf<LocationConsumer>()
        fun emit(next: Point) { point = next; consumers.toList().forEach { it.onLocationUpdated(next) } }
        override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
            consumers += locationConsumer
            locationConsumer.onLocationUpdated(point)
        }
        override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) { consumers -= locationConsumer }
    }
}

