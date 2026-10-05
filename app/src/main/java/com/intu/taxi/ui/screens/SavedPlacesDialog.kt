package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import com.intu.taxi.ui.theme.LocalIntuDarkMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.data.SavedPlace
import com.intu.taxi.data.SavedPlaces
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import java.util.UUID

private val SavedPlaceTeal = Color(0xFF08817E)
private val SavedPlaceIndigo = Color(0xFF1E1F47)
private val SavedPlaceColors = lightColorScheme(
    primary = SavedPlaceTeal, onPrimary = Color.White,
    surface = Color.White, onSurface = Color(0xFF183236),
    surfaceVariant = Color(0xFFEDF4F3), onSurfaceVariant = Color(0xFF637579),
    background = Color(0xFFF4F8F7), onBackground = Color(0xFF183236),
    outline = Color(0xFFB3C8C5), outlineVariant = Color(0xFFDCE8E6)
)

@Composable
fun SavedPlacesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val store = remember(uid) { SavedPlaces(context, uid) }
    val places by store.changes.collectAsState(initial = store.read())
    var editPlace by remember { mutableStateOf<SavedPlace?>(null) }
    var editName by remember { mutableStateOf<String?>(null) }
    if (editName != null) {
        SavedPlaceEditor(editPlace, initialName = editName.orEmpty(), onDismiss = { editName = null })
        return
    }
    SavedPlacesDialogContent(places, onDismiss,
        onEdit = { place, name -> editPlace = place; editName = name },
        onRemove = store::remove)
}

@Composable
internal fun SavedPlacesDialogContent(
    places: List<SavedPlace>,
    onDismiss: () -> Unit,
    onEdit: (SavedPlace?, String) -> Unit,
    onRemove: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MaterialTheme(colorScheme = if (LocalIntuDarkMode.current) MaterialTheme.colorScheme else SavedPlaceColors) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight * 0.92f),
                    shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.background,
                    shadowElevation = 16.dp) {
                    Column {
                        Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(SavedPlaceTeal, SavedPlaceIndigo)))
                            .padding(start = 22.dp, top = 20.dp, end = 10.dp, bottom = 22.dp),
                            verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("TUS LUGARES", style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.76f))
                                Text("Direcciones guardadas", style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Tu destino, a un toque.", style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f))
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Outlined.Close, contentDescription = "Cerrar direcciones guardadas", tint = Color.White)
                            }
                        }
                        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Accesos rápidos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Surface(shape = RoundedCornerShape(50), color = if (LocalIntuDarkMode.current)
                                    MaterialTheme.colorScheme.primaryContainer else SavedPlaceTeal.copy(alpha = 0.08f)) {
                                    Text("${places.size} de 8", Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        color = if (LocalIntuDarkMode.current) MaterialTheme.colorScheme.onPrimaryContainer else SavedPlaceTeal,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            listOf("casa" to "Casa", "trabajo" to "Trabajo").forEach { (id, name) ->
                                val place = places.find { it.id == id }
                                SavedPlaceCard(name, place, if (id == "casa") Icons.Outlined.Home else Icons.Outlined.Work,
                                    if (id == "casa") SavedPlaceTeal else SavedPlaceIndigo,
                                    onEdit = { onEdit(place, name) }, onRemove = { onRemove(id) })
                            }
                            val favorites = places.filterNot { it.id == "casa" || it.id == "trabajo" }
                            if (favorites.isNotEmpty()) {
                                Text("Otros lugares", Modifier.padding(top = 8.dp),
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                favorites.forEach { place ->
                                    SavedPlaceCard(place.name, place, Icons.Outlined.StarOutline, SavedPlaceTeal,
                                        onEdit = { onEdit(place, place.name) }, onRemove = { onRemove(place.id) })
                                }
                            }
                            if (places.size < 8) OutlinedButton(onClick = { onEdit(null, "Favorito") },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = 50.dp),
                                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Agregar otro lugar", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(Modifier.fillMaxWidth().background(AppearanceColors.surface).padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Se guardan en este teléfono para tu cuenta.", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                shape = RoundedCornerShape(16.dp)) { Text("Listo", fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPlaceCard(name: String, place: SavedPlace?, icon: ImageVector, accent: Color, onEdit: () -> Unit, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = LocalIntuDarkMode.current
    val work = accent == SavedPlaceIndigo
    val iconColor = if (dark) { if (work) colors.onSecondaryContainer else colors.onPrimaryContainer } else accent
    val iconBackground = if (dark) { if (work) colors.secondaryContainer else colors.primaryContainer } else accent.copy(alpha = 0.09f)
    val statusColor = if (dark) { if (work) colors.secondary else colors.primary } else accent
    Surface(Modifier.fillMaxWidth().testTag("saved-place-card-${place?.id ?: name.lowercase()}").clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp), color = if (dark) colors.surfaceContainerLow else colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, if (dark) colors.outlineVariant.copy(alpha = 0.6f) else colors.outlineVariant)) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 12.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(15.dp), color = iconBackground) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Text(place?.address ?: "Elige su ubicación en el mapa", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (place != null) Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                    Text("Guardada", color = statusColor, style = MaterialTheme.typography.labelSmall)
                } else Text("Sin guardar", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onEdit) {
                    Icon(if (place == null) Icons.Outlined.Add else Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (place == null) "Agregar" else "Editar", fontWeight = FontWeight.SemiBold)
                }
                if (place != null) IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Quitar $name", modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(name = "Direcciones guardadas", widthDp = 380, heightDp = 760)
@Composable
private fun SavedPlacesDialogPreview() {
    SavedPlacesDialogContent(listOf(SavedPlace("casa", "Casa", "Av. Perú, junto al parque", -11.25, -74.63)), {}, { _, _ -> }, {})
}

@Composable
fun SavedPlaceEditor(place: SavedPlace? = null, point: Point? = null, initialName: String = "Favorito", onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val store = remember(uid) { SavedPlaces(context, uid) }
    SavedPlaceEditorDialog(place, point, initialName, onDismiss) { saved ->
        require(store.read().any { it.id == saved.id } || store.read().size < 8) { "Puedes guardar hasta ocho lugares." }
        store.save(saved)
    }
}

@Composable
fun SavedPlaceEditorDialog(
    place: SavedPlace? = null,
    point: Point? = null,
    initialName: String = "Favorito",
    onDismiss: () -> Unit,
    locationProvider: LocationProvider? = null,
    onSave: (SavedPlace) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(place?.name ?: initialName) }
    var address by rememberSaveable { mutableStateOf(place?.address.orEmpty()) }
    var latitude by rememberSaveable { mutableStateOf(point?.latitude() ?: place?.latitude) }
    var longitude by rememberSaveable { mutableStateOf(point?.longitude() ?: place?.longitude) }
    var picking by rememberSaveable { mutableStateOf(latitude == null || longitude == null) }
    var error by remember { mutableStateOf<String?>(null) }
    if (picking) {
        PlacePointPicker(
            initial = Point.fromLngLat(longitude ?: -74.6382, latitude ?: -11.2521),
            onDismiss = { if (latitude == null || longitude == null) onDismiss() else picking = false },
            onPicked = { selected ->
                latitude = selected.latitude()
                longitude = selected.longitude()
                picking = false
                error = null
            },
            title = "Ubicación de ${name.ifBlank { initialName }}",
            description = "Mueve el mapa hasta colocar tu dirección bajo el marcador.",
            pinDescription = "Ubicación del lugar",
            loadingMessage = "Cargando mapa… Espera para confirmar la ubicación.",
            showCoordinates = false,
            startAtCurrentLocation = latitude == null || longitude == null,
            locationProvider = locationProvider
        )
        return
    }
    SavedPlaceFormDialog(
        name = name, address = address, error = error,
        saveEnabled = name.isNotBlank() && latitude != null && longitude != null,
        onNameChange = { name = it.take(30) },
        onAddressChange = { address = it.take(250) },
        onChangeLocation = { picking = true },
        onDismiss = onDismiss,
        onSave = {
            error = null
            try {
                val slot = initialName.trim().lowercase()
                val id = place?.id ?: when {
                    slot in listOf("casa", "trabajo") -> slot
                    name.trim().lowercase() in listOf("casa", "trabajo") -> name.trim().lowercase()
                    else -> UUID.randomUUID().toString()
                }
                onSave(SavedPlace(id, name.trim(), address.trim().ifBlank { name.trim() }, latitude!!, longitude!!))
                onDismiss()
            } catch (e: Exception) { error = e.message ?: "No se pudo guardar el lugar." }
        }
    )
}
