package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Place
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.intu.taxi.data.CatalogPlace
import com.intu.taxi.data.categoryLabel
import com.intu.taxi.data.normalizedPlaceText
import com.intu.taxi.data.validPlaceCoordinates
import com.intu.taxi.repositories.PlaceCatalogRepository
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.intu.taxi.ui.map.createIntuMapView
import com.intu.taxi.ui.map.PinSelectionMapController
import com.intu.taxi.ui.map.pointUnderCenterPin
import com.intu.taxi.ui.map.rememberPinStartLocation
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

private val PlaceTeal = Color(0xFF08817E)
private val PlaceStatuses = listOf("draft" to "Borrador", "published" to "Publicado", "inactive" to "Inactivo")
private val PlaceLocalities = listOf("Satipo", "Río Negro")
private const val OtherLocality = "Otra localidad"
private val PlaceCategories = listOf("place", "square", "park", "mall", "marketplace", "restaurant", "cafe", "fast_food", "ice_cream",
    "hospital", "clinic", "pharmacy", "school", "kindergarten", "university", "stadium", "pitch", "fuel", "hotel", "bus_station", "place_of_worship", "bank", "police", "fire_station",
    "townhall", "courthouse", "parking", "convenience", "mortuary", "party")

@Composable
fun AdminPlacesTab(reloadKey: Int) {
    val context = LocalContext.current
    val repository = remember { PlaceCatalogRepository(context) }
    var places by remember { mutableStateOf<List<CatalogPlace>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf("draft") }
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<CatalogPlace?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    LaunchedEffect(reloadKey, refresh) {
        loading = true
        error = null
        try { places = repository.listAdmin() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No se pudieron cargar los lugares." }
        finally { loading = false }
    }
    AdminPlacesContent(places, loading, error, filter, query, onFilter = { filter = it }, onQuery = { query = it },
        onRetry = { refresh++ }, onNew = { editing = null; showEditor = true }, onEdit = { editing = it; showEditor = true })
    if (showEditor) PlaceEditorDialog(editing, onDismiss = { showEditor = false }, onSave = { place ->
        repository.save(place, isNew = editing == null)
        showEditor = false
        refresh++
    })
}

@Composable
internal fun AdminPlacesContent(places: List<CatalogPlace>, loading: Boolean, error: String?, filter: String, query: String,
    onFilter: (String) -> Unit, onQuery: (String) -> Unit, onRetry: () -> Unit, onNew: () -> Unit, onEdit: (CatalogPlace) -> Unit) {
    val needle = normalizedPlaceText(query)
    val visible = places.filter { (filter == "all" || it.status == filter) &&
        needle in normalizedPlaceText("${it.name} ${it.aliases.joinToString(" ")} ${it.locality} ${it.address}") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AdminSectionHeading("Lugares de Intu", "Revisa el nombre y la entrada de recojo. Solo los lugares publicados aparecen en el buscador.", if (loading) null else places.size) }
        item {
            Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp)); Text("Nuevo lugar")
            }
        }
        item { AdminSearchField(query, onQuery, "Buscar en el catálogo") }
        item { AdminFilters(PlaceStatuses.map { (code, label) -> code to "$label (${places.count { it.status == code }})" } +
            listOf("all" to "Todos (${places.size})"), filter, { onFilter(it ?: "all") }) }
        when {
            loading -> item { AdminLoading() }
            error != null -> item { AdminMessage(error, true, onRetry) }
            visible.isEmpty() -> item { AdminMessage(if (query.isBlank()) "No hay lugares en esta lista. Agrega uno o cambia el filtro."
                else "No hay coincidencias. Puedes buscar por nombre, localidad o referencia.") }
            else -> items(visible, key = { it.id }) { place -> AdminCatalogPlaceCard(place) { onEdit(place) } }
        }
    }
}

@Composable
internal fun AdminCatalogPlaceCard(place: CatalogPlace, onEdit: () -> Unit) {
    AdminCard(Modifier.clickable(onClickLabel = "Editar ${place.name}", onClick = onEdit)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Outlined.Place, null, Modifier.padding(12.dp), tint = AppearanceColors.highlight(AdminTeal))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${categoryLabel(place.category)} · ${place.locality}", style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(AdminMuted))
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = AppearanceColors.secondary(AdminMuted))
        }
        if (place.address.isNotBlank()) Text(place.address, style = MaterialTheme.typography.bodyMedium)
        AdminBadge(PlaceStatuses.firstOrNull { it.first == place.status }?.second ?: place.status,
            if (place.status == "draft") Color(0xFF99641C) else AdminTeal,
            if (place.status == "draft") Color(0xFFFFF2DB) else Color(0xFFE3F2EE))
        Text(if (place.pickupVerified) "Punto de recojo confirmado" else "Falta confirmar el punto de recojo",
            style = MaterialTheme.typography.bodySmall, color = AppearanceColors.highlight(if (place.pickupVerified) AdminTeal else Color(0xFF99641C)))
        if (place.source == "OpenStreetMap") Text("© OpenStreetMap contributors · ODbL 1.0", style = MaterialTheme.typography.labelSmall, color = AppearanceColors.secondary(AdminMuted))
    }
}

@Composable
fun PlaceEditorDialog(initial: CatalogPlace?, onDismiss: () -> Unit, onSave: suspend (CatalogPlace) -> Unit) {
    val scope = rememberCoroutineScope()
    val base = remember(initial?.id) { initial ?: CatalogPlace("new", "Nuevo lugar", latitude = -11.2521, longitude = -74.6382) }
    var name by rememberSaveable(base.id) { mutableStateOf(initial?.name.orEmpty()) }
    var aliases by rememberSaveable(base.id) { mutableStateOf(base.aliases.joinToString(", ")) }
    var category by rememberSaveable(base.id) { mutableStateOf(base.category) }
    val initialLocality = PlaceLocalities.firstOrNull { normalizedPlaceText(it) == normalizedPlaceText(base.locality) }
    var locality by rememberSaveable(base.id) { mutableStateOf(initialLocality ?: base.locality) }
    var customLocality by rememberSaveable(base.id) { mutableStateOf(initialLocality == null) }
    var address by rememberSaveable(base.id) { mutableStateOf(base.address) }
    var latitude by rememberSaveable(base.id) { mutableStateOf(coordinateText(base.latitude)) }
    var longitude by rememberSaveable(base.id) { mutableStateOf(coordinateText(base.longitude)) }
    var status by rememberSaveable(base.id) { mutableStateOf(base.status) }
    var verified by rememberSaveable(base.id) { mutableStateOf(base.pickupVerified) }
    var picking by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingCoordinates by rememberSaveable(base.id) { mutableStateOf(false) }
    val lat = latitude.toDoubleOrNull()
    val lng = longitude.toDoubleOrNull()
    val validPoint = lat != null && lng != null && validPlaceCoordinates(lat, lng)
    val names = aliases.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct()
    val valid = name.trim().length in 2..120 && locality.trim().length in 1..80 && category.trim().length in 1..40 &&
        address.length <= 240 && names.size <= 12 && aliases.length <= 1200 && validPoint && (status != "published" || verified)
    if (picking) {
        PlacePointPicker(Point.fromLngLat(lng ?: base.longitude, lat ?: base.latitude), onDismiss = { picking = false }, onPicked = { point ->
            latitude = coordinateText(point.latitude()); longitude = coordinateText(point.longitude())
            verified = true; picking = false
        })
    } else Dialog(onDismissRequest = { if (!saving) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AdminPanelTheme {
            Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().fillMaxHeight(0.94f).padding(12.dp).imePadding(),
                shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.background) {
                Column {
                    Row(Modifier.fillMaxWidth().background(AppearanceColors.surface).padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("CATÁLOGO DE INTU", style = MaterialTheme.typography.labelSmall, color = AppearanceColors.highlight(PlaceTeal))
                            Text(if (initial == null) "Nuevo lugar" else "Editar lugar", style = MaterialTheme.typography.headlineSmall)
                            Text("Un nombre fácil de encontrar y una entrada precisa.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDismiss, enabled = !saving) { Icon(Icons.Default.Close, contentDescription = "Cerrar formulario") }
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).testTag("place-editor-form"),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PlaceEditorSection("1", "Datos del lugar", "Así lo encontrarán los pasajeros.") {
                            OutlinedTextField(name, { name = it.take(120) }, label = { Text("Nombre del lugar") },
                                placeholder = { Text("Ej. Mercado Central") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth(), enabled = !saving, shape = RoundedCornerShape(12.dp))
                            PlaceDropdownField("Categoría", categoryLabel(category),
                                PlaceCategories.map { it to categoryLabel(it) }, !saving, "place-category") { category = it }
                            OutlinedTextField(aliases, { aliases = it.take(1200) }, label = { Text("Otros nombres (opcional)") },
                                placeholder = { Text("Ej. mercado municipal, mercado viejo") },
                                supportingText = { Text(if (names.size > 12) "Puedes agregar hasta 12 nombres." else "Separa los nombres por comas. Ayudan a encontrar este mismo lugar.") },
                                isError = names.size > 12, modifier = Modifier.fillMaxWidth(), enabled = !saving, shape = RoundedCornerShape(12.dp))
                        }
                        PlaceEditorSection("2", "Ubicación y recojo", "Marca la entrada accesible, no el centro del edificio.") {
                            PlaceDropdownField("Ciudad / localidad", if (customLocality) OtherLocality else locality,
                                PlaceLocalities.map { it to it } + (OtherLocality to OtherLocality), !saving, "place-locality") { selected ->
                                if (selected == OtherLocality) {
                                    if (!customLocality) locality = ""
                                    customLocality = true
                                } else { locality = selected; customLocality = false }
                            }
                            if (customLocality) OutlinedTextField(locality, { locality = it.take(80) }, label = { Text("Nombre de la localidad") },
                                placeholder = { Text("Ej. Mazamari") }, singleLine = true,
                                supportingText = { Text("Usa el nombre oficial, sin abreviaturas.") },
                                modifier = Modifier.fillMaxWidth(), enabled = !saving, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(address, { address = it.take(240) }, label = { Text("Dirección o referencia (opcional)") },
                                placeholder = { Text("Ej. Entrada por Jr. Los Pinos, frente al parque") }, minLines = 2,
                                supportingText = { Text("Describe el acceso que marcarás en el mapa.") },
                                modifier = Modifier.fillMaxWidth(), enabled = !saving, shape = RoundedCornerShape(12.dp))
                            if (base.source == "OpenStreetMap") {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(if (base.coordinateKind == "bounding_box_center") "El punto importado es aproximado. Muévelo a la entrada de recojo."
                                            else "Revisa que el punto importado sea una entrada accesible.", style = MaterialTheme.typography.bodySmall)
                                        val uri = LocalUriHandler.current
                                        Text("© OpenStreetMap contributors · ODbL 1.0", color = AppearanceColors.highlight(PlaceTeal),
                                            modifier = Modifier.clickable { uri.openUri(base.sourceUrl.ifBlank { "https://www.openstreetmap.org/copyright" }) },
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            OutlinedButton(onClick = { picking = true }, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Elegir punto en el mapa")
                            }
                            if (validPoint) Text("${latitude}, ${longitude}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { editingCoordinates = !editingCoordinates }, enabled = !saving, contentPadding = PaddingValues(0.dp)) {
                                Text(if (editingCoordinates) "Ocultar coordenadas" else "Editar coordenadas")
                                Icon(if (editingCoordinates) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                            }
                            if (editingCoordinates) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(latitude, { latitude = it; verified = false }, label = { Text("Latitud") }, singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                        modifier = Modifier.fillMaxWidth(), enabled = !saving, shape = RoundedCornerShape(12.dp))
                                    OutlinedTextField(longitude, { longitude = it; verified = false }, label = { Text("Longitud") }, singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                        modifier = Modifier.fillMaxWidth(), enabled = !saving, shape = RoundedCornerShape(12.dp))
                                }
                            }
                            if (!validPoint) Text("Ingresa coordenadas válidas o elige el punto en el mapa.", color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = verified, onCheckedChange = { verified = it }, enabled = validPoint && !saving,
                                    modifier = Modifier.testTag("pickup-verified"))
                                Text("Confirmo que este punto es una entrada adecuada para recojo", style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                        PlaceEditorSection("3", "Publicación", "Decide si los pasajeros pueden encontrarlo.") {
                            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PlaceStatuses.forEach { (code, label) ->
                                    val selected = status == code
                                    Surface(shape = RoundedCornerShape(12.dp),
                                        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else AppearanceColors.surface,
                                        border = BorderStroke(1.dp, if (selected) PlaceTeal else MaterialTheme.colorScheme.outlineVariant)) {
                                        Row(Modifier.fillMaxWidth().selectable(selected, enabled = !saving, role = Role.RadioButton, onClick = { status = code })
                                            .padding(end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = selected, onClick = null, enabled = !saving)
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(label, style = MaterialTheme.typography.titleSmall)
                                                Text(when (code) { "published" -> "Visible en el buscador de Intu"
                                                    "inactive" -> "Oculto, conservando sus datos"
                                                    else -> "Guárdalo para terminar de revisarlo" },
                                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                            if (status == "published") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (verified) Icon(Icons.Default.CheckCircle, null, tint = AppearanceColors.highlight(PlaceTeal), modifier = Modifier.size(18.dp))
                                Text(if (verified) "Punto de recojo confirmado. Listo para publicar." else "Confirma la entrada de recojo para publicar.",
                                    color = if (verified) AppearanceColors.accent else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            if (status == "inactive") Text("Se ocultará cuando los usuarios actualicen su catálogo.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                    Column(Modifier.fillMaxWidth().background(AppearanceColors.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp)) { Text("Cancelar", maxLines = 1) }
                            Button(enabled = valid && !saving, modifier = Modifier.weight(1.4f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp), onClick = {
                                saving = true; error = null
                                scope.launch {
                                    try { onSave(base.copy(name = name.trim(), aliases = names, category = category.trim(), locality = locality.trim(), address = address.trim(),
                                        latitude = lat!!, longitude = lng!!, status = status, pickupVerified = verified)) }
                                    catch (e: CancellationException) { throw e }
                                    catch (e: Exception) { error = e.message ?: "No se pudo guardar el lugar." }
                                    finally { saving = false }
                                }
                            }) { Text(if (saving) "Guardando…" else "Guardar lugar", maxLines = 1) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceEditorSection(number: String, title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AppearanceColors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        Text(number, color = AppearanceColors.highlight(PlaceTeal), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceDropdownField(label: String, value: String, options: List<Pair<String, String>>, enabled: Boolean, tag: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, enabled = enabled, singleLine = true,
            label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            shape = RoundedCornerShape(12.dp), modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth().testTag(tag))
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { (code, title) -> DropdownMenuItem(text = { Text(title) }, onClick = { onSelected(code); expanded = false }) }
        }
    }
}

@Composable
fun PlacePointPicker(
    initial: Point,
    onDismiss: () -> Unit,
    onPicked: (Point) -> Unit,
    title: String = "Punto de recojo",
    description: String = "Mueve el mapa hasta colocar la entrada bajo el marcador.",
    pinDescription: String = "Entrada de recojo",
    loadingMessage: String = "Cargando mapa… Puedes volver para ingresar coordenadas manualmente.",
    showCoordinates: Boolean = true,
    startAtCurrentLocation: Boolean = false,
    locationProvider: LocationProvider? = null,
    confirmLabel: String = "Confirmar punto"
) {
    val start = rememberPinStartLocation(initial, startAtCurrentLocation, locationProvider)
    var map by remember { mutableStateOf<MapView?>(null) }
    var point by remember { mutableStateOf(initial) }
    var ready by remember { mutableStateOf(false) }
    val pinSize = 40.dp
    DisposableEffect(map) {
        val controller = map?.let { view -> PinSelectionMapController(view) { point = it } }
        onDispose { controller?.close() }
    }
    val mapStyle = com.intu.taxi.ui.theme.intuMapStyle()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(16.dp), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(description, style = MaterialTheme.typography.bodyMedium)
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    if (start.point != null) AndroidView(modifier = Modifier.fillMaxSize().testTag(if (ready) "place-map-ready" else "place-map-loading"), factory = { context ->
                        createIntuMapView(context).apply {
                            map = this
                            point = start.point
                            mapboxMap.setCamera(CameraOptions.Builder().center(start.point).zoom(17.0).build())
                            mapboxMap.subscribeMapLoaded { ready = true }
                            mapboxMap.loadStyleUri(mapStyle)
                        }
                    }, onRelease = { it.onDestroy(); map = null })
                    if (start.point == null) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Buscando tu ubicación…")
                    }
                    if (start.point != null) com.intu.taxi.ui.map.RoutePin(description = pinDescription,
                        modifier = Modifier.align(Alignment.Center).size(pinSize).offset(
                            y = -(pinSize / 2) + com.intu.taxi.ui.map.RoutePinStyle.anchorInset))
                }
                if (showCoordinates && start.point != null) Text("${coordinateText(point.latitude())}, ${coordinateText(point.longitude())}", style = MaterialTheme.typography.bodySmall)
                start.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (start.point != null && !ready) Text(loadingMessage, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Volver") }
                    Button(enabled = ready, onClick = { map?.pointUnderCenterPin()?.let(onPicked) }) { Text(confirmLabel) }
                }
            }
        }
    }
}

private fun coordinateText(value: Double): String = String.format(Locale.US, "%.7f", value)
