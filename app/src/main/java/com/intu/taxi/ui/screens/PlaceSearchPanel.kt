package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.intu.taxi.data.PlaceSearchResult
import com.intu.taxi.data.PlaceSearchSource
import com.intu.taxi.ui.map.TripMap
import com.mapbox.geojson.Point
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

data class AddressSearchState(
    val results: List<PlaceSearchResult> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

/** Only user-edited, visible searches trigger requests. A new query cancels the previous call. */
@Composable
internal fun rememberAddressSearch(
    query: String, enabled: Boolean,
    proximity: Pair<Double, Double>? = null,
    search: suspend (String) -> List<PlaceSearchResult>
): AddressSearchState {
    val text = query.trim()
    val active = enabled && text.length >= 2
    var state by remember(text, active) { mutableStateOf(AddressSearchState(loading = active)) }
    // GPS jitter must neither clear results nor repeatedly cancel the pending lookup.
    var searchProximity by remember { mutableStateOf(proximity) }
    LaunchedEffect(proximity) {
        val previous = searchProximity
        if (proximity != null && (previous == null || TripMap.metersBetween(
                Point.fromLngLat(previous.second, previous.first),
                Point.fromLngLat(proximity.second, proximity.first)) >= 250.0)) {
            searchProximity = proximity
        }
    }
    val currentSearch by rememberUpdatedState(search)
    LaunchedEffect(text, active, searchProximity) {
        if (!active) return@LaunchedEffect
        state = state.copy(loading = true, error = null)
        delay(600)
        try { state = AddressSearchState(results = currentSearch(text)) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { state = state.copy(loading = false,
            error = "No se pudieron buscar las direcciones. Revisa tu conexión o elige en el mapa.") }
    }
    return state
}

/** Suggestions and manual map fallback share the same behavior in both Home search fields. */
@Composable
fun PlaceSearchPanel(
    results: List<PlaceSearchResult>, hasCatalog: Boolean, loading: Boolean, error: String?,
    onSelect: (PlaceSearchResult) -> Unit, onRefresh: () -> Unit, onPickMap: () -> Unit,
    modifier: Modifier = Modifier, addressLoading: Boolean = false, addressError: String? = null
) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppearanceColors.surface)) {
        Column(Modifier.padding(8.dp)) {
            if (results.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                for (source in listOf(PlaceSearchSource.CATALOG, PlaceSearchSource.MAPBOX)) {
                    val group = results.filter { it.source == source }
                    if (group.isNotEmpty()) item(key = "heading:$source") {
                        Text(if (source == PlaceSearchSource.CATALOG) "Lugares de Intu" else "Calles y direcciones",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium,
                            color = AppearanceColors.highlight(Color(0xFF08817E)))
                    }
                    items(group, key = { it.id }) { place ->
                        Column(Modifier.fillMaxWidth().clickable { onSelect(place) }.padding(10.dp)) {
                            Text(place.name, style = MaterialTheme.typography.bodyMedium, color = AppearanceColors.foreground(Color(0xFF1C1C1E)))
                            if (place.subtitle.isNotBlank()) Text(place.subtitle, style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(Color(0xFF6E6E73)))
                        }
                    }
                }
            } else Text(
                if (addressLoading) "Buscando calles y direcciones…" else if (loading) "Actualizando lugares…"
                else "No encontramos ese lugar. Puedes elegirlo en el mapa.",
                modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall
            )
            if (addressLoading && results.isNotEmpty()) Text("Buscando calles y direcciones…",
                modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall)
            if (addressError != null) Text(addressError, modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            if (error != null) Text(if (hasCatalog) "No se pudo actualizar. Se muestra el catálogo guardado." else error,
                modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            if (results.any { it.openStreetMap }) {
                val uri = LocalUriHandler.current
                Text("© OpenStreetMap contributors · ODbL 1.0", style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp).clickable { uri.openUri("https://www.openstreetmap.org/copyright") })
            }
            if (results.any { it.source == PlaceSearchSource.MAPBOX }) {
                val uri = LocalUriHandler.current
                Text("© Mapbox", style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp).clickable { uri.openUri("https://www.mapbox.com/about/maps/") })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onPickMap) { Text("Elegir en mapa") }
                TextButton(onClick = onRefresh, enabled = !loading) { Text(if (loading) "Actualizando…" else "Actualizar lugares") }
            }
        }
    }
}
