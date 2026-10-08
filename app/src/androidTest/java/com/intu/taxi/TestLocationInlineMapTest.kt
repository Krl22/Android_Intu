package com.intu.taxi

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import com.intu.taxi.location.MapTestLocation
import com.intu.taxi.location.TestLocation
import com.intu.taxi.location.TestLocationMapSelection
import com.intu.taxi.location.TestLocationPreset
import com.intu.taxi.ui.map.MapLocationBinding
import com.intu.taxi.ui.map.pointUnderCenterPin
import com.intu.taxi.ui.map.rememberMapViewWithLifecycle
import com.intu.taxi.ui.map.routePinBitmap
import com.intu.taxi.ui.screens.TestLocationBanner
import com.intu.taxi.ui.screens.TestLocationMapOverlay
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/** Real MapView and gestures with isolated callbacks; no authentication, rides or server writes. */
class TestLocationInlineMapTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var map: MapView
    private lateinit var lines: PolylineAnnotationManager
    private lateinit var markers: PointAnnotationManager
    private val picked = AtomicReference<MapTestLocation>()
    private val active = mutableStateOf<TestLocation?>(TestLocationPreset.SATIPO)
    private val ready = mutableStateOf(false)
    private val route = listOf(Point.fromLngLat(-74.6382, -11.2521), Point.fromLngLat(-74.637, -11.25),
        Point.fromLngLat(-74.634, -11.249))

    @After fun clearPicker() { TestLocationMapSelection.cancel() }

    private fun content() {
        compose.setContent { IntuTheme {
            map = rememberMapViewWithLifecycle(stringResource(R.string.mapbox_access_token))
            Box(Modifier.fillMaxSize()) {
                AndroidView(factory = { map }, modifier = Modifier.fillMaxSize())
                TestLocationBanner(active.value, { active.value = null },
                    modifier = Modifier.align(Alignment.TopCenter), onEdit = { TestLocationMapSelection.request() })
                MapLocationBinding(map, ready.value, false, active.value) { _, _ -> }
                TestLocationMapOverlay(map, ready.value, onPicked = {
                    picked.set(it)
                    active.value = it
                })
                LaunchedEffect(map) {
                    map.mapboxMap.loadStyle("""{"version":8,"sources":{},"layers":[]}""") {
                        lines = map.annotations.createPolylineAnnotationManager()
                        lines.create(PolylineAnnotationOptions().withPoints(route).withLineColor("#08817E").withLineWidth(5.0))
                        markers = map.annotations.createPointAnnotationManager()
                        markers.create(PointAnnotationOptions().withPoint(route.last())
                            .withIconImage(routePinBitmap(map.context, false)))
                        map.mapboxMap.setCamera(CameraOptions.Builder().center(route[1]).zoom(15.0).build())
                        ready.value = true
                    }
                }
            }
        } }
        compose.waitUntil(20_000) { ready.value && map.width > 0 && map.height > 0 }
    }

    @Test fun changeSelectsDirectlyOnTheSameTripMapAndPreservesRouteAndParticipants() {
        content()
        val original = map
        compose.onNodeWithText("Cambiar").performClick()
        compose.onNodeWithTag("test-location-pin").assertIsDisplayed()
        compose.onNodeWithText("Elegir en el mapa").assertDoesNotExist()
        compose.onNodeWithText("Simular en Satipo").assertDoesNotExist()
        SystemClock.sleep(800)
        val before = compose.runOnIdle { map.pointUnderCenterPin()!! }
        NativeMapGestures(map).panThroughWindow()
        SystemClock.sleep(1000)
        val selected = compose.runOnIdle { map.pointUnderCenterPin()!! }
        assertNotEquals(before, selected)
        captureNativeScreenshot(compose, "qa-inline-test-location.png")
        compose.onNodeWithText("Simular aquí").performClick()
        compose.waitUntil(10_000) { picked.get() != null && !TestLocationMapSelection.picking.value }
        assertEquals(selected.latitude(), picked.get().latitude, .0000001)
        assertEquals(selected.longitude(), picked.get().longitude, .0000001)
        compose.runOnIdle {
            assertSame(original, map)
            assertEquals(route, lines.annotations.single().points)
            assertEquals(route.last(), markers.annotations.single().point)
        }
        compose.onNodeWithTag("test-location-pin").assertDoesNotExist()
    }

    @Test fun cancellingSelectionKeepsCurrentLocationAndTheTripRoute() {
        content()
        val cameraBefore = compose.runOnIdle { map.mapboxMap.cameraState.center }
        compose.onNodeWithText("Cambiar").performClick()
        compose.onNodeWithTag("test-location-pin").assertIsDisplayed()
        NativeMapGestures(map).panThroughWindow()
        compose.onNodeWithText("Cancelar").performClick()
        // Compose idleness precedes the native map's camera update on this emulator.
        compose.waitUntil(5_000) { compose.runOnIdle { map.mapboxMap.cameraState.center == cameraBefore } }
        SystemClock.sleep(300)
        compose.runOnIdle {
            assertFalse(TestLocationMapSelection.picking.value)
            assertNull(picked.get())
            assertEquals(TestLocationPreset.SATIPO, active.value)
            assertEquals(cameraBefore, map.mapboxMap.cameraState.center)
            assertEquals(route, lines.annotations.single().points)
        }
    }
}
