package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.models.*
import com.intu.taxi.repositories.BusinessRepository
import com.mapbox.geojson.Point
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AdminBusinesses(reloadKey: Int) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val repository = remember { BusinessRepository() }
    val scope = rememberCoroutineScope()
    var state by remember(uid) { mutableStateOf<AdminBusinessState?>(null) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<BusinessAd?>(null) }
    var removing by remember { mutableStateOf<BusinessAd?>(null) }
    LaunchedEffect(uid, reloadKey, retry) {
        error = null
        try { state = repository.adminState() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No se pudieron cargar los negocios." }
    }
    fun mutate(operation: suspend () -> AdminBusinessState, onSuccess: () -> Unit = {}) {
        if (busy) return
        scope.launch {
            busy = true; error = null
            try { state = operation(); onSuccess() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No se pudo guardar. Intenta de nuevo." }
            finally { busy = false }
        }
    }
    AdminBusinessesContent(state, busy, error, { retry++ },
        { enabled -> mutate({ repository.setEnabled(enabled) }) },
        { courier -> mutate({ repository.setCourier(courier.id, !courier.selected) }) },
        { editor = BusinessAd(); error = null }, { editor = it; error = null }, { removing = it; error = null })
    editor?.let { ad -> BusinessAdEditor(ad, busy, error, { if (!busy) { editor = null; error = null } },
        { draft -> mutate({ repository.save(draft) }) { editor = null } }) }
    removing?.let { ad -> AlertDialog(onDismissRequest = { if (!busy) removing = null },
        title = { Text("Retirar anuncio") },
        text = { Column { Text("${ad.name} dejará de aparecer en Inicio. Los pedidos existentes continuarán.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(enabled = !busy, onClick = { mutate({ repository.archive(ad) }) { removing = null } }) { Text("Retirar") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { removing = null }) { Text("Volver") } }) }
}

@Composable
internal fun AdminBusinessesContent(state: AdminBusinessState?, busy: Boolean, error: String?, onRetry: () -> Unit,
    onEnabled: (Boolean) -> Unit, onCourier: (BusinessTestCourier) -> Unit,
    onAdd: () -> Unit, onEdit: (BusinessAd) -> Unit, onArchive: (BusinessAd) -> Unit) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AdminSectionHeading("Negocios y publicidad", "Prueba los anuncios y pedidos de negocios ficticios.") }
        if (state == null) item {
            if (error == null) CircularProgressIndicator() else { Text(error, color = colors.error); Button(onRetry) { Text("Reintentar") } }
        } else {
            item {
                Card(Modifier.fillMaxWidth().intuCardBackground(emphasized = true),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                    Row(Modifier.fillMaxWidth().testTag("admin-business-enabled")
                        .toggleable(state.enabled, enabled = !busy, role = Role.Switch, onValueChange = onEnabled)
                        .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Delivery de negocios", fontWeight = FontWeight.Bold)
                            Text(if (state.enabled) "Anuncios y pedidos demo activados" else "Anuncios y pedidos demo desactivados",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(state.enabled, null, enabled = !busy)
                    }
                }
            }
            item { Text("Solo controla los negocios. Los envíos normales siguen disponibles y los pedidos ya solicitados pueden finalizar.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { item { Text(it, color = colors.error); TextButton(onClick = onRetry, enabled = !busy) { Text("Actualizar lista") } } }
            item { Text("Repartidores de prueba", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (state.couriers.isEmpty()) item { Text("Aprueba una cuenta con moto lineal en Conductores para seleccionarla aquí.", color = colors.onSurfaceVariant) }
            items(state.couriers, key = { "courier-${it.id}" }) { courier ->
                Row(Modifier.fillMaxWidth().testTag("business-courier-${courier.id}")
                    .toggleable(courier.selected, enabled = !busy, role = Role.Checkbox, onValueChange = { onCourier(courier) }),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(courier.selected, null, enabled = !busy)
                    Column { Text(courier.name.ifBlank { "Repartidor" }); Text(courier.plate, style = MaterialTheme.typography.bodySmall) }
                }
            }
            item { Text("Los pedidos demo solo aparecen para los repartidores seleccionados. El pago es solo por transporte, al entregar.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Anuncios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onAdd, enabled = !busy, modifier = Modifier.testTag("admin-add-business")) { Text("Agregar negocio") }
            } }
            items(state.ads, key = { "ad-${it.id}" }) { ad ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BusinessAdCard(ad, { if (!busy) onEdit(ad) }, Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (ad.published) "Publicado · orden ${ad.sortOrder}" else "Borrador · orden ${ad.sortOrder}",
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton({ onEdit(ad) }, enabled = !busy) { Text("Editar") }
                        TextButton({ onArchive(ad) }, enabled = !busy) { Text("Retirar") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BusinessAdEditor(ad: BusinessAd, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (BusinessAd) -> Unit) {
    var name by rememberSaveable(ad.id) { mutableStateOf(ad.name) }
    var title by rememberSaveable(ad.id) { mutableStateOf(ad.title) }
    var description by rememberSaveable(ad.id) { mutableStateOf(ad.description) }
    var image by rememberSaveable(ad.id) { mutableStateOf(ad.imageUrl) }
    var city by rememberSaveable(ad.id) { mutableStateOf(ad.city) }
    var offerDetail by rememberSaveable(ad.id) { mutableStateOf(ad.offerDetail) }
    var offerPrice by rememberSaveable(ad.id) { mutableStateOf(ad.offerPrice?.let { "%.2f".format(java.util.Locale.US, it) }.orEmpty()) }
    var photoCode by rememberSaveable(ad.id) { mutableStateOf(ad.demoPhoto.code) }
    var menuJson by rememberSaveable(ad.id) { mutableStateOf(com.intu.taxi.repositories.businessMenuJson(ad.menu).toString()) }
    var editingMenu by remember { mutableStateOf(false) }
    var address by rememberSaveable(ad.id) { mutableStateOf(ad.address) }
    var lat by rememberSaveable(ad.id) { mutableStateOf(ad.latitude) }
    var lng by rememberSaveable(ad.id) { mutableStateOf(ad.longitude) }
    var categoryCode by rememberSaveable(ad.id) { mutableStateOf(ad.category.code) }
    var published by rememberSaveable(ad.id) { mutableStateOf(ad.published) }
    var order by rememberSaveable(ad.id) { mutableStateOf(ad.sortOrder.toString()) }
    var picking by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf<String?>(null) }
    val draft = ad.copy(name = name, title = title, description = description, imageUrl = image, address = address,
        latitude = lat, longitude = lng, category = BusinessCategory.entries.first { it.code == categoryCode },
        published = published, sortOrder = order.toIntOrNull() ?: -1, city = city, offerDetail = offerDetail,
        offerPrice = if (offerPrice.isBlank()) null else offerPrice.replace(',', '.').toDoubleOrNull() ?: Double.NaN,
        demoPhoto = com.intu.taxi.repositories.businessPhoto(photoCode),
        menu = com.intu.taxi.repositories.parseBusinessMenu(org.json.JSONArray(menuJson)))
    if (editingMenu) {
        BusinessMenuEditor(draft.menu, { editingMenu = false }, { menu ->
            menuJson = com.intu.taxi.repositories.businessMenuJson(menu).toString(); editingMenu = false
        })
        return
    }
    if (picking) {
        PlacePointPicker(Point.fromLngLat(lng, lat), { picking = false }, { point ->
            lat = point.latitude(); lng = point.longitude(); picking = false
        }, title = "Recojo del negocio demo", description = "Coloca el marcador en el lugar donde tu repartidor recogerá el paquete.")
        return
    }
    AccountDialogLayout(eyebrow = "INTU · PUBLICIDAD DEMO", title = if (ad.id == null) "Agregar negocio" else "Editar negocio",
        subtitle = "Todos los negocios de este MVP son ficticios.", onDismiss = onDismiss, dismissEnabled = !busy,
        footer = { Button(onClick = {
            runCatching { draft.normalized() }.onSuccess { validation = null; onSave(it) }.onFailure { validation = it.message }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (busy) "Guardando…" else "Guardar anuncio") } },
        content = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(100) }, label = { Text("Nombre del negocio") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { BusinessCategory.entries.forEach { c ->
                    FilterChip(categoryCode == c.code, { categoryCode = c.code }, { Text(c.label) }, enabled = !busy)
                } }
                OutlinedTextField(title, { title = it.take(100) }, label = { Text("Título del anuncio") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                OutlinedTextField(city, { city = it.take(50) }, label = { Text("Ciudad") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                OutlinedTextField(offerDetail, { offerDetail = it.take(100) }, label = { Text("Detalle de la promoción") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                OutlinedTextField(offerPrice, { offerPrice = it.take(10) }, label = { Text("Precio destacado (opcional)") },
                    prefix = { Text("S/ ") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                BusinessPhotoPicker(photoCode, !busy) { photoCode = it }
                OutlinedTextField(description, { description = it.take(500) }, label = { Text("Descripción") }, minLines = 3, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                OutlinedTextField(image, { image = it.take(1000) }, label = { Text("URL HTTPS de imagen (opcional)") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                OutlinedButton({ editingMenu = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("admin-edit-business-menu")) {
                    Text("Editar menú (${draft.menu.size} productos)")
                }
                Text("La URL reemplaza la foto demo. El menú se guarda junto con el anuncio.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(address, { address = it.take(200) }, label = { Text("Dirección de recojo") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                OutlinedButton({ picking = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Elegir recojo en mapa") }
                Text("Punto: %.5f, %.5f".format(java.util.Locale.US, lat, lng), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(order, { order = it.filter(Char::isDigit).take(3) }, label = { Text("Orden (0 aparece primero)") },
                    enabled = !busy, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().toggleable(published, enabled = !busy, role = Role.Switch, onValueChange = { published = it }), verticalAlignment = Alignment.CenterVertically) {
                    Text("Publicar en Inicio", modifier = Modifier.weight(1f)); Switch(published, null, enabled = !busy)
                }
                Text("Solo se verá cuando Delivery de negocios esté activado.", style = MaterialTheme.typography.bodySmall)
                (validation ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        })
}
