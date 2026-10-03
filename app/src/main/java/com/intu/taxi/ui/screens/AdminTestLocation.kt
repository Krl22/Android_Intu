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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.intu.taxi.location.TestLocationPreset
import com.intu.taxi.location.TestLocation
import com.intu.taxi.location.MapTestLocation
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
                    Text("Elige Satipo, Río Negro o cualquier punto en el mapa para probar un recojo. Como conductor, al conectarte enviarás esa ubicación y podrás recibir solicitudes.")
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
                    Text("La simulación usa un punto fijo y termina al cerrar sesión.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cerrar") } }
        )
    }
}

@Composable
fun TestLocationBanner(preset: TestLocation, onRealGps: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = Color(0xFFFFEAC2), contentColor = Color(0xFF5D3B00)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Ubicación de prueba: ${preset.label}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRealGps) { Text("GPS real", color = Color(0xFF5D3B00)) }
        }
    }
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
