package com.intu.taxi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.intu.taxi.data.PlaceSearchResult
import com.intu.taxi.data.PlaceSearchSource
import com.intu.taxi.data.SavedPlace

/** Saved addresses, suggestions and map selection live in the same destination list. */
@Composable
internal fun DestinationSearchPanel(
    query: String,
    savedPlaces: List<SavedPlace>,
    results: List<PlaceSearchResult>,
    hasCatalog: Boolean,
    loading: Boolean,
    error: String?,
    addressLoading: Boolean,
    addressError: String?,
    onSelect: (PlaceSearchResult) -> Unit,
    onSavedPlaceClick: (SavedPlace) -> Unit,
    onRefresh: () -> Unit,
    onPickMap: () -> Unit,
    onManagePlaces: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(modifier.fillMaxWidth().testTag("destination-search-panel").intuCardBackground(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text("Tus direcciones", style = MaterialTheme.typography.labelLarge, color = colors.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            val saved = listOf("casa" to "Casa", "trabajo" to "Trabajo") +
                savedPlaces.filterNot { it.id == "casa" || it.id == "trabajo" }.map { it.id to it.name }
            saved.forEach { (id, name) ->
                val place = savedPlaces.find { it.id == id }
                DestinationRow(name, place?.address?.takeIf { it.isNotBlank() } ?: "Agregar dirección",
                    when (id) {
                        "casa" -> Icons.Outlined.Home
                        "trabajo" -> Icons.Outlined.Work
                        else -> Icons.Outlined.StarOutline
                    }, "home-place-$id") {
                    if (place != null) onSavedPlaceClick(place) else onManagePlaces()
                }
            }
            if (query.trim().length >= 2) {
                Text("Direcciones sugeridas", style = MaterialTheme.typography.labelLarge, color = colors.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                results.forEach { place ->
                    DestinationRow(place.name, place.subtitle, Icons.Outlined.Place, "destination-result-${place.id}") {
                        onSelect(place)
                    }
                }
                if (addressLoading || loading) Text(
                    if (addressLoading) "Buscando calles y direcciones…" else "Actualizando lugares…",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                else if (results.isEmpty()) Text("No encontramos ese lugar. Puedes elegirlo en el mapa.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                addressError?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                error?.let { Text(if (hasCatalog) "No se pudo actualizar. Se muestra el catálogo guardado." else it,
                    color = colors.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                val uri = LocalUriHandler.current
                if (results.any { it.openStreetMap }) Text("© OpenStreetMap contributors · ODbL 1.0",
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp)
                        .clickable { uri.openUri("https://www.openstreetmap.org/copyright") })
                if (results.any { it.source == PlaceSearchSource.MAPBOX }) Text("© Mapbox",
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp)
                        .clickable { uri.openUri("https://www.mapbox.com/about/maps/") })
                if (error != null) TextButton(onClick = onRefresh, enabled = !loading,
                    modifier = Modifier.padding(horizontal = 8.dp)) { Text("Actualizar lugares") }
            }
            DestinationRow("Elegir en mapa", "", Icons.Outlined.Place, "home-pick-destination", onPickMap)
            DestinationRow("Lugares guardados", "", Icons.Outlined.StarOutline, "home-manage-places", onManagePlaces)
        }
    }
}

@Composable
private fun DestinationRow(title: String, subtitle: String, icon: ImageVector, tag: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle.isBlank()) null else ({ Text(subtitle) }),
        leadingContent = { Icon(icon, null, tint = colors.primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent,
            headlineColor = colors.onSurface, supportingColor = colors.onSurfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick)
    )
    HorizontalDivider(Modifier.padding(start = 56.dp, end = 16.dp), color = colors.outlineVariant)
}
