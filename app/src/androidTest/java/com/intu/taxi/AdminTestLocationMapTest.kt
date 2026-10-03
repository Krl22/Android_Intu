package com.intu.taxi

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.location.TestLocationPreset
import com.intu.taxi.location.TestLocation
import com.intu.taxi.location.MapTestLocation
import com.intu.taxi.ui.map.MapLocationBinding
import com.intu.taxi.ui.map.rememberMapViewWithLifecycle
import com.intu.taxi.ui.screens.TestLocationBanner
import com.intu.taxi.ui.screens.TestLocationDialog
import com.intu.taxi.ui.screens.rememberAddressSearch
import com.intu.taxi.ui.screens.AddressSearchState
import com.intu.taxi.data.PlaceSearchResult
import com.intu.taxi.data.PlaceSearchSource
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred

class AdminTestLocationMapTest {
    @get:Rule val compose = createComposeRule()

    @Test fun changingCityRefreshesTheSameQueryAndCancelsOldAddressResults() {
        val proximity = mutableStateOf(-12.0464 to -77.0428)
        val firstStarted = AtomicBoolean()
        val firstCancelled = AtomicBoolean()
        val lateResponse = CompletableDeferred<Unit>()
        val result = AtomicReference<AddressSearchState>()
        compose.setContent {
            val city = proximity.value
            result.set(rememberAddressSearch("avenida", true, proximity = city) {
                if (city.first == -12.0464) {
                    firstStarted.set(true)
                    try { lateResponse.await() } finally { firstCancelled.set(true) }
                }
                listOf(PlaceSearchResult("address", if (city.first == -12.0464) "Lima" else "Satipo", "",
                    city.first, city.second, PlaceSearchSource.MAPBOX))
            })
        }
        compose.waitUntil(10_000) { firstStarted.get() }
        compose.runOnIdle { proximity.value = TestLocationPreset.SATIPO.latitude to TestLocationPreset.SATIPO.longitude }
        compose.waitUntil(10_000) { result.get()?.results?.singleOrNull()?.name == "Satipo" }
        assertTrue(firstCancelled.get())
        lateResponse.complete(Unit)
        compose.waitForIdle()
        assertEquals("Satipo", result.get().results.single().name)
    }

    @Test fun buttonsSwitchTheRealMapPuckAndLocationAndRestoreGpsDespiteLimaUpdates() {
        val lima = Point.fromLngLat(-77.0428, -12.0464)
        val gps = FakeGps(lima)
        val preset = mutableStateOf<TestLocation?>(null)
        val dialog = mutableStateOf(false)
        val ready = mutableStateOf(false)
        val effective = AtomicReference<Point>()
        val puck = AtomicReference<Point>()
        lateinit var map: MapView
        compose.setContent {
            IntuTheme(darkTheme = false) {
                map = rememberMapViewWithLifecycle(stringResource(R.string.mapbox_access_token))
                Column {
                    preset.value?.let { TestLocationBanner(it, { preset.value = null }) }
                    Button(onClick = { dialog.value = true }) { Text("Simular mi ubicación") }
                    AndroidView(factory = { map }, modifier = Modifier.fillMaxWidth().height(400.dp))
                }
                LaunchedEffect(map) {
                    map.mapboxMap.loadStyle("""{"version":8,"sources":{},"layers":[]}""") { ready.value = true }
                }
                DisposableEffect(map) {
                    val listener = OnIndicatorPositionChangedListener { puck.set(it) }
                    map.location.addOnIndicatorPositionChangedListener(listener)
                    onDispose { map.location.removeOnIndicatorPositionChangedListener(listener) }
                }
                MapLocationBinding(map, ready.value, true, preset.value, gps) { point, first ->
                    effective.set(point)
                    if (first) map.mapboxMap.setCamera(CameraOptions.Builder().center(point).zoom(14.0).build())
                }
                if (dialog.value) TestLocationDialog(preset.value, false, null,
                    onSelect = { preset.value = it; dialog.value = false },
                    onRealGps = { preset.value = null; dialog.value = false }, onDismiss = { dialog.value = false },
                    onChooseOnMap = {})
            }
        }
        waitForPoint(effective, lima)
        compose.onNodeWithText("Simular mi ubicación").performClick()
        compose.onNodeWithText("Simular en Satipo").performClick()
        val satipo = TestLocationPreset.SATIPO.point()
        waitForPoint(effective, satipo)
        waitForPoint(puck, satipo)
        compose.runOnIdle { gps.update(Point.fromLngLat(-77.0500, -12.0500)) }
        compose.waitForIdle()
        assertEquals(satipo, effective.get())
        assertPoint(satipo, puck.get())
        screenshot("qa-location-satipo.png")
        compose.onNodeWithText("Simular mi ubicación").performClick()
        compose.onNodeWithText("Simular en Río Negro").performClick()
        val rioNegro = TestLocationPreset.RIO_NEGRO.point()
        waitForPoint(effective, rioNegro)
        waitForPoint(puck, rioNegro)
        compose.runOnIdle { assertPoint(rioNegro, map.mapboxMap.cameraState.center) }
        screenshot("qa-location-rio-negro.png")
        val custom = MapTestLocation(-11.24591, -74.62672)
        compose.runOnIdle { preset.value = custom }
        val customPoint = Point.fromLngLat(custom.longitude, custom.latitude)
        waitForPoint(effective, customPoint)
        waitForPoint(puck, customPoint)
        compose.runOnIdle { gps.update(lima) }
        compose.waitForIdle()
        assertPoint(customPoint, effective.get())
        compose.runOnIdle { assertPoint(customPoint, map.mapboxMap.cameraState.center) }
        compose.runOnIdle { gps.update(Point.fromLngLat(-77.0500, -12.0500)) }
        compose.onNodeWithText("GPS real").performClick()
        val latestGps = Point.fromLngLat(-77.0500, -12.0500)
        waitForPoint(effective, latestGps)
        waitForPoint(puck, latestGps)
        compose.runOnIdle { assertPoint(latestGps, map.mapboxMap.cameraState.center) }
    }

    private fun TestLocationPreset.point() = Point.fromLngLat(longitude, latitude)
    private fun assertPoint(expected: Point, actual: Point) {
        assertEquals(expected.latitude(), actual.latitude(), 0.0000001)
        assertEquals(expected.longitude(), actual.longitude(), 0.0000001)
    }
    private fun waitForPoint(value: AtomicReference<Point>, expected: Point) {
        compose.waitUntil(15_000) { value.get()?.let {
            kotlin.math.abs(it.latitude() - expected.latitude()) < 0.0000001 &&
                kotlin.math.abs(it.longitude() - expected.longitude()) < 0.0000001
        } == true }
    }
    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), name)
        file.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private class FakeGps(private var latest: Point) : LocationProvider {
        private val consumers = mutableSetOf<LocationConsumer>()
        fun update(point: Point) {
            latest = point
            consumers.toList().forEach { it.onLocationUpdated(point, options = { duration = 0 }) }
        }
        override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
            consumers += locationConsumer
            locationConsumer.onLocationUpdated(latest, options = { duration = 0 })
        }
        override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) { consumers -= locationConsumer }
    }
}
