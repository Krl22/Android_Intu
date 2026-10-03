package com.intu.taxi

import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.intu.taxi.ui.map.PinSelectionMapController
import com.intu.taxi.ui.map.TripMap
import com.intu.taxi.ui.map.rememberMapViewWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.attribution.attribution
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class PinSelectionMapTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var mapView: MapView

    @Test fun offCenterZoomKeepsThePinLocationAndPanningStillChangesIt() {
        val active = mutableStateOf(true)
        val height = mutableStateOf(500.dp)
        val loaded = AtomicBoolean()
        val selected = AtomicReference<Point>()
        compose.setContent {
            val nativeMap = rememberMapViewWithLifecycle(stringResource(R.string.mapbox_access_token))
            mapView = nativeMap
            AndroidView(factory = { nativeMap }, modifier = Modifier.fillMaxWidth().height(height.value))
            LaunchedEffect(nativeMap) {
                // Empty local style: this checks real gestures and projection without fetching map tiles.
                nativeMap.mapboxMap.loadStyle("""{"version":8,"sources":{},"layers":[]}""") {
                    nativeMap.mapboxMap.setCamera(CameraOptions.Builder()
                        .center(Point.fromLngLat(-74.6382, -11.2521)).zoom(16.0).build())
                    loaded.set(true)
                }
            }
            DisposableEffect(nativeMap, active.value) {
                val controller = if (active.value) PinSelectionMapController(nativeMap) { selected.set(it) } else null
                onDispose { controller?.close() }
            }
        }
        compose.waitUntil(15000) { loaded.get() && selected.get() != null }
        val pinLocation = compose.runOnIdle {
            assertTrue(mapView.gestures.scrollEnabled)
            assertMapControls()
            mapView.mapboxMap.coordinateForPixel(mapView.gestures.focalPoint!!)
        }
        NativeMapGestures(mapView).pinchOut()
        SystemClock.sleep(1500) // Let the SDK's gesture deceleration finish.
        compose.runOnIdle {
            assertTrue("The pinch must actually zoom out: zoom=${mapView.mapboxMap.cameraState.zoom}",
                mapView.mapboxMap.cameraState.zoom < 15.5)
            assertUnderPin(pinLocation)
            assertTrue(TripMap.metersBetween(pinLocation, selected.get()) < 1.0)
        }

        val beforePan = selected.get()
        NativeMapGestures(mapView).pan()
        SystemClock.sleep(1500)
        compose.runOnIdle {
            assertTrue("One-finger panning must change the selected location",
                TripMap.metersBetween(beforePan, selected.get()) > 5.0)
            assertUnderPin(selected.get())
            height.value = 300.dp
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(mapView.width / 2.0, mapView.gestures.focalPoint!!.x, 0.5)
            assertEquals(mapView.height / 2.0, mapView.gestures.focalPoint!!.y, 0.5)
            assertUnderPin(selected.get())
            active.value = false
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertMapControls()
            assertEquals(mapView.width / 2.0, mapView.gestures.focalPoint!!.x, 0.5)
            assertEquals(mapView.height / 2.0, mapView.gestures.focalPoint!!.y, 0.5)
        }
        val selectionAfterExit = selected.get()
        compose.runOnIdle {
            mapView.mapboxMap.setCamera(CameraOptions.Builder()
                .center(Point.fromLngLat(-74.6400, -11.2540)).build())
        }
        SystemClock.sleep(500)
        assertEquals("The selection listener must stop after leaving pin mode", selectionAfterExit, selected.get())
        val centerAfterExit = compose.runOnIdle {
            mapView.mapboxMap.coordinateForPixel(mapView.gestures.focalPoint!!)
        }
        val zoomAfterExit = compose.runOnIdle { mapView.mapboxMap.cameraState.zoom }
        NativeMapGestures(mapView).pinchOut()
        SystemClock.sleep(1500)
        compose.runOnIdle {
            assertTrue("Zoom must also work outside pin mode", mapView.mapboxMap.cameraState.zoom < zoomAfterExit - 0.5)
            assertUnderPin(centerAfterExit)
            height.value = 240.dp
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertMapControls()
            assertEquals(mapView.width / 2.0, mapView.gestures.focalPoint!!.x, 0.5)
            assertEquals(mapView.height / 2.0, mapView.gestures.focalPoint!!.y, 0.5)
        }
        assertEquals(selectionAfterExit, selected.get())
    }

    private fun assertMapControls() {
        assertFalse(mapView.scalebar.enabled)
        assertTrue(mapView.logo.enabled)
        assertTrue(mapView.attribution.enabled)
        assertFalse(mapView.gestures.pinchScrollEnabled)
    }

    private fun assertUnderPin(point: Point) {
        val pixel = mapView.mapboxMap.pixelForCoordinate(point)
        assertEquals(mapView.width / 2.0, pixel.x, 1.0)
        assertEquals(mapView.height / 2.0, pixel.y, 1.0)
    }

}
