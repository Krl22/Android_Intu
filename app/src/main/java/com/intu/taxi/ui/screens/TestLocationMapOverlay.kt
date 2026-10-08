package com.intu.taxi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.intu.taxi.location.AdminLocationSimulation
import com.intu.taxi.location.MapTestLocation
import com.intu.taxi.location.TestLocationMapSelection
import com.intu.taxi.ui.map.PinSelectionMapController
import com.intu.taxi.ui.map.RoutePin
import com.intu.taxi.ui.map.RoutePinStyle
import com.intu.taxi.ui.map.pointUnderCenterPin
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.animation.camera
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

/** Adds a selection pin to the existing MapView; trip layers and participant markers stay intact. */
@Composable
fun TestLocationMapOverlay(
    mapView: MapView,
    ready: Boolean,
    onPicked: suspend (MapTestLocation) -> Unit = { AdminLocationSimulation.activate(it) }
) {
    val picking by TestLocationMapSelection.picking.collectAsState()
    val scope = rememberCoroutineScope()
    var selected by remember(mapView) { mutableStateOf<Point?>(null) }
    var busy by remember(mapView) { mutableStateOf(false) }
    var error by remember(mapView) { mutableStateOf<String?>(null) }
    var previousCamera by remember(mapView) { mutableStateOf<CameraOptions?>(null) }
    val pick = rememberUpdatedState(onPicked)
    DisposableEffect(mapView) {
        TestLocationMapSelection.register(mapView) {
            // Capture synchronously at the toolbar tap, before selection can recompose or be dragged.
            mapView.camera.cancelAllAnimators()
            val camera = mapView.mapboxMap.cameraState
            previousCamera = CameraOptions.Builder().center(camera.center).zoom(camera.zoom)
                .bearing(camera.bearing).pitch(camera.pitch).padding(camera.padding).build()
        }
        onDispose { TestLocationMapSelection.unregister(mapView) }
    }
    DisposableEffect(mapView, picking, ready) {
        if (!picking || !ready) return@DisposableEffect onDispose {}
        error = null
        val controller = PinSelectionMapController(mapView) { selected = it }
        onDispose { controller.close() }
    }
    fun cancel() {
        AdminLocationSimulation.pauseDrive()
        mapView.camera.cancelAllAnimators()
        previousCamera?.let { mapView.mapboxMap.setCamera(it) }
        TestLocationMapSelection.cancel()
    }
    BackHandler(enabled = picking) { cancel() }
    if (!picking) return
    Box(Modifier.fillMaxSize().testTag("test-location-map-overlay")) {
        val pinSize = 42.dp
        RoutePin("Nueva ubicación de prueba", pickup = true,
            modifier = Modifier.align(Alignment.Center).size(pinSize)
                .offset(y = -(pinSize / 2) + RoutePinStyle.anchorInset)
                .testTag("test-location-pin"))
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp, shadowElevation = 6.dp
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cambiar mi ubicación", style = MaterialTheme.typography.titleMedium)
                Text("Mueve este mapa para colocar el pin. La ruta y las ubicaciones siguen visibles.",
                    style = MaterialTheme.typography.bodySmall)
                selected?.let { Text(String.format(Locale.US, "%.5f, %.5f", it.latitude(), it.longitude()),
                    style = MaterialTheme.typography.bodySmall) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = ::cancel) { Text("Cancelar") }
                    Button(enabled = ready && selected != null && !busy, onClick = {
                        val point = mapView.pointUnderCenterPin() ?: return@Button
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                pick.value(MapTestLocation(point.latitude(), point.longitude()))
                                TestLocationMapSelection.cancel()
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { error = e.message ?: "No se pudo cambiar la ubicación." }
                            finally { busy = false }
                        }
                    }) { Text(if (busy) "Aplicando…" else "Simular aquí") }
                }
            }
        }
    }
}
