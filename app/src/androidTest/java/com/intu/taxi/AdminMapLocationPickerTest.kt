package com.intu.taxi

import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import com.intu.taxi.location.TestLocation
import com.intu.taxi.location.TestLocationPreset
import com.intu.taxi.location.MapTestLocation
import com.intu.taxi.ui.map.TripMap
import com.intu.taxi.ui.map.pointUnderCenterPin
import com.intu.taxi.ui.screens.TestLocationDialog
import com.intu.taxi.ui.screens.TestLocationMapPicker
import com.intu.taxi.ui.screens.TestLocationBanner
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.gestures.gestures
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AdminMapLocationPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mapSelectionConfirmsExactPointAndCancelPreservesPreviousSimulation() {
        val selected = mutableStateOf<TestLocation?>(TestLocationPreset.SATIPO)
        val dialog = mutableStateOf(true)
        val picker = mutableStateOf(false)
        var confirmations = 0
        compose.setContent { IntuTheme {
            if (picker.value) TestLocationMapPicker(selected.value,
                onDismiss = { picker.value = false },
                onPicked = { selected.value = it; confirmations++; picker.value = false; dialog.value = false })
            else if (dialog.value) TestLocationDialog(selected.value, false, null,
                onSelect = { selected.value = it; dialog.value = false },
                onRealGps = { selected.value = null; dialog.value = false },
                onDismiss = { dialog.value = false }, onChooseOnMap = { picker.value = true })
            else selected.value?.let { TestLocationBanner(it, { selected.value = null }) }
        } }
        compose.onNodeWithText("Elegir en el mapa").performClick()
        compose.waitUntil(60_000) { compose.onAllNodesWithTag("place-map-ready").fetchSemanticsNodes().isNotEmpty() }
        lateinit var map: MapView
        onView(isAssignableFrom(MapView::class.java)).check { view, noMatch ->
            if (noMatch != null) throw noMatch
            map = view as MapView
        }
        // Loading removes a status row and resizes this dialog's map. Wait for the SDK projection too.
        SystemClock.sleep(1500)
        val initial = compose.runOnIdle {
            assertFalse(map.scalebar.enabled)
            assertTrue(map.logo.enabled)
            assertEquals(map.width / 2.0, map.gestures.focalPoint!!.x, .5)
            assertEquals(map.height / 2.0, map.gestures.focalPoint!!.y, .5)
            map.pointUnderCenterPin()!!
        }
        val zoom = compose.runOnIdle { map.mapboxMap.cameraState.zoom }
        NativeMapGestures(map).pinchOut()
        SystemClock.sleep(1500)
        compose.runOnIdle {
            val after = map.pointUnderCenterPin()!!
            assertTrue("The pinch must zoom out", map.mapboxMap.cameraState.zoom < zoom - .5)
            assertTrue("Pin moved ${TripMap.metersBetween(initial, after)} m: $initial -> $after; map=${map.width}x${map.height}; focal=${map.gestures.focalPoint}",
                TripMap.metersBetween(initial, after) < 1.0)
        }
        NativeMapGestures(map).pan()
        SystemClock.sleep(1500)
        val chosen = compose.runOnIdle { map.pointUnderCenterPin()!! }
        assertTrue(TripMap.metersBetween(initial, chosen) > 5.0)
        compose.runOnIdle { assertEquals(TestLocationPreset.SATIPO, selected.value); assertEquals(0, confirmations) }
        compose.onNodeWithText("Simular aquí").assertIsEnabled().performClick()
        compose.runOnIdle {
            val result = selected.value as MapTestLocation
            assertEquals(chosen.latitude(), result.latitude, .0000001)
            assertEquals(chosen.longitude(), result.longitude, .0000001)
            assertEquals(1, confirmations)
            dialog.value = true
        }
        compose.onNodeWithText("Elegir en el mapa").performClick()
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithText("Ubicación actual: Punto en el mapa").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, confirmations) }
        compose.onNodeWithText("Usar GPS real").performClick()
        compose.runOnIdle { assertNull(selected.value) }
    }
}
