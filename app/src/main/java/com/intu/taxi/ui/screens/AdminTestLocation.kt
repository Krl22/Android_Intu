package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.intu.taxi.location.TestLocationPreset
import com.intu.taxi.location.TestLocation
import com.intu.taxi.location.MapTestLocation
import com.intu.taxi.location.AdminLocationSimulation
import com.intu.taxi.location.TestDriveRoutePlanner
import com.intu.taxi.location.TestLocationMapSelection
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.mapbox.geojson.Point
import java.util.Locale

@Composable
fun TestLocationDialog(
    active: TestLocation?,
    busy: Boolean,
    error: String?,
    onSelect: (TestLocation) -> Unit,
    onRealGps: () -> Unit,
    onDismiss: () -> Unit,
    onChooseOnMap: () -> Unit
) {
    AdminPanelTheme {
        AlertDialog(
            containerColor = AppearanceColors.surface,
            icon = { Icon(Icons.Outlined.MyLocation, null, tint = AdminTeal) },
            onDismissRequest = { if (!busy) onDismiss() },
            title = { Text("Ubicación de prueba") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Elige un punto para probar como pasajero o conductor. Puedes cambiarlo durante la búsqueda y el viaje. El cliente verá los movimientos del conductor de prueba.")
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Ubicación actual: ${active?.label ?: "GPS real"}", style = MaterialTheme.typography.titleSmall)
                            if (active is MapTestLocation) Text(locationCoordinates(active), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(onClick = onChooseOnMap, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Map, null); Spacer(Modifier.width(8.dp)); Text("Elegir en el mapa")
                    }
                    TestLocationPreset.entries.forEach { preset ->
                        OutlinedButton(onClick = { onSelect(preset) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Place, null); Spacer(Modifier.width(8.dp)); Text("Simular en ${preset.label}")
                        }
                    }
                    TextButton(onClick = onRealGps, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.MyLocation, null); Spacer(Modifier.width(8.dp)); Text("Usar GPS real")
                    }
                    if (busy) Text("Verificando permisos…")
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                    Text("En modo conductor, usa Avanzar al recojo o Avanzar al destino para recorrer la ruta. La simulación termina al cerrar sesión o volver al GPS real.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cerrar") } }
        )
    }
}

@Composable
fun TestLocationBanner(preset: TestLocation?, onRealGps: () -> Unit, modifier: Modifier = Modifier, onEdit: (() -> Unit)? = null) {
    Surface(modifier.fillMaxWidth(), color = Color(0xFFFFEAC2), contentColor = Color(0xFF5D3B00)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(preset?.let { "Prueba: ${it.label}" } ?: "Pruebas de ubicación", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            onEdit?.let { TextButton(onClick = it) { Text(if (preset == null) "Simular" else "Cambiar", color = Color(0xFF5D3B00)) } }
            if (preset != null) TextButton(onClick = onRealGps) { Text("GPS real", color = Color(0xFF5D3B00)) }
        }
    }
}

/** Stays above navigation, including screens that hide the bottom bar during a search or trip. */
@Composable
fun TestLocationTools(uid: String?, modifier: Modifier = Modifier) {
    val active by AdminLocationSimulation.preset.collectAsState()
    val scope = rememberCoroutineScope()
    var showMap by remember(uid) { mutableStateOf(false) }
    var busy by remember(uid) { mutableStateOf(false) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    fun select(point: TestLocation) {
        if (busy) return
        busy = true
        error = null
        showMap = false
        scope.launch {
            try {
                AdminLocationSimulation.activate(point)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No se pudo cambiar la ubicación." }
            finally { busy = false }
        }
    }
    TestLocationBanner(active, onRealGps = { AdminLocationSimulation.clear() }, modifier = modifier,
        onEdit = {
            if (!busy) {
                error = null
                AdminLocationSimulation.pauseDrive()
                if (!TestLocationMapSelection.request()) showMap = true
            }
        })
    if (showMap) TestLocationMapPicker(active,
        onDismiss = { showMap = false }, onPicked = ::select)
    error?.let { message -> AlertDialog(onDismissRequest = { error = null },
        title = { Text("Ubicación de prueba") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { error = null }) { Text("Cerrar") } }) }
}

@Composable
fun TestDriveControls(rideId: String?, status: String, target: GeoPoint?,
    routeLoader: suspend (MapTestLocation, MapTestLocation) -> List<MapTestLocation>?) {
    val active by AdminLocationSimulation.preset.collectAsState()
    val moving by AdminLocationSimulation.isMoving.collectAsState()
    val automatic by AdminLocationSimulation.automatic.state.collectAsState()
    val picking by TestLocationMapSelection.picking.collectAsState()
    val scope = rememberCoroutineScope()
    var showDialog by remember(rideId, status) { mutableStateOf(false) }
    var speed by remember { mutableIntStateOf(40) }
    var busy by remember(rideId, status) { mutableStateOf(false) }
    var error by remember(rideId, status) { mutableStateOf<String?>(null) }
    val loader = rememberUpdatedState(routeLoader)
    suspend fun startLeg(speedKmh: Int, keepAutomatic: Boolean) {
        val origin = AdminLocationSimulation.currentPreset() ?: error("Primero elige una ubicación de prueba.")
        val finish = target ?: error("No se encontró el punto de recojo o destino del viaje.")
        val revision = AdminLocationSimulation.movementRevision()
        val path = TestDriveRoutePlanner { a, b -> loader.value(a, b) }.plan(
            MapTestLocation(origin.latitude, origin.longitude), MapTestLocation(finish.latitude, finish.longitude))
        AdminLocationSimulation.startDrive(path, speedKmh, keepAutomatic, expectedRevision = revision)
    }
    DisposableEffect(rideId) { onDispose { AdminLocationSimulation.pauseDrive() } }
    LaunchedEffect(rideId, status) {
        AdminLocationSimulation.automatic.updateTrip(rideId, status)
        AdminLocationSimulation.pauseDrive(keepAutomatic = true)
    }
    val automaticMode = automatic?.rideId == rideId && automatic != null
    val label = if (status == "in_progress") "Avanzar al destino" else "Avanzar al recojo"
    // Route loading belongs to starting a leg, never to enabling its button.
    val ready = active != null && target != null && !picking && status in setOf("accepted", "in_progress")
    LaunchedEffect(rideId, status, ready, moving, automatic) {
        val mode = automatic ?: return@LaunchedEffect
        if (moving || !AdminLocationSimulation.automatic.takeLeg(rideId, status, ready)) return@LaunchedEffect
        busy = true
        try {
            startLeg(mode.speedKmh, keepAutomatic = true)
        } catch (e: CancellationException) {
            AdminLocationSimulation.automatic.retryLeg(rideId, status)
            throw e
        } catch (e: Exception) {
            AdminLocationSimulation.pauseDrive()
            error = e.message ?: "No se pudo continuar el recorrido automático."
            showDialog = true
        } finally { busy = false }
    }
    if (active == null || rideId == null || picking ||
        (status !in setOf("accepted", "in_progress") && !automaticMode)) return
    TextButton(onClick = { error = null; showDialog = true }) {
        Text(when {
            automaticMode -> if (status == "arrived") "Automático · esperando inicio" else "Viaje automático · pausar"
            moving -> "Pausar recorrido de prueba"
            else -> label
        }, color = Color(0xFFFFD78A))
    }
    if (showDialog) TestDriveDialog(
        label = label, speed = speed, busy = busy, moving = moving, automaticMode = automaticMode,
        canStart = ready, error = error, onSpeed = { speed = it },
        onAutomatic = {
            AdminLocationSimulation.pauseDrive()
            AdminLocationSimulation.automatic.enable(rideId, speed)
            showDialog = false
        },
        onPause = { AdminLocationSimulation.pauseDrive(); showDialog = false },
        onStart = {
            busy = true
            scope.launch {
                try { startLeg(speed, keepAutomatic = false); showDialog = false }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "No se pudo iniciar el recorrido." }
                finally { busy = false }
            }
        }, onDismiss = { showDialog = false }
    )
}

@Composable
internal fun TestDriveDialog(label: String, speed: Int, busy: Boolean, moving: Boolean, automaticMode: Boolean,
    canStart: Boolean, error: String?, onSpeed: (Int) -> Unit, onAutomatic: () -> Unit, onPause: () -> Unit,
    onStart: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Recorrido de prueba") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("El modo automático va hasta el recojo. Confirma la llegada e inicia el viaje con el PIN si corresponde; luego continuará al destino sin activar otro recorrido. Al llegar al destino, finaliza el viaje normalmente.")
                Text("Velocidad: $speed km/h")
                androidx.compose.material3.Slider(value = speed.toFloat(), onValueChange = { onSpeed(it.toInt()) },
                    valueRange = 10f..100f, steps = 8, enabled = !busy && !moving && !automaticMode)
                if (busy) Text("Preparando recorrido desde tu ubicación…")
                else if (!canStart && !moving && !automaticMode) Text("No se encontró el punto de recojo o destino del viaje.")
                if (!automaticMode && !moving) OutlinedButton(enabled = canStart && !busy,
                    onClick = onAutomatic, modifier = Modifier.fillMaxWidth()) { Text("Simular viaje automático") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = moving || automaticMode || (canStart && !busy),
                onClick = { if (moving || automaticMode) onPause() else onStart() }
            ) { Text(if (moving || automaticMode) "Pausar simulación" else label) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cerrar") } }
    )
}

@Composable
fun TestLocationMapPicker(active: TestLocation?, onDismiss: () -> Unit, onPicked: (TestLocation) -> Unit) {
    val start = active ?: TestLocationPreset.SATIPO
    PlacePointPicker(
        initial = Point.fromLngLat(start.longitude, start.latitude),
        onDismiss = onDismiss,
        onPicked = { onPicked(MapTestLocation(it.latitude(), it.longitude())) },
        title = "Elige tu ubicación de prueba",
        description = "Mueve el mapa y coloca el marcador donde quieres simular que estás. Se aplicará al pasajero y al conductor.",
        pinDescription = "Ubicación simulada",
        loadingMessage = "Cargando el mapa… Puedes volver y reintentarlo.",
        startAtCurrentLocation = active == null,
        confirmLabel = "Simular aquí"
    )
}

private fun locationCoordinates(location: TestLocation): String =
    String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude)
