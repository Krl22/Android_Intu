package com.intu.taxi.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.repositories.GuideBusiness
import com.intu.taxi.repositories.GuideBusinessRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import org.json.JSONObject

/** Abierto desde la notificación «Nuevo comercio para la guía». */
@Composable
fun AdminGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.96f).fillMaxHeight(.92f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                TextButton(onDismiss) { Text("Cerrar") }
                AdminGuideTab(0)
            }
        }
    }
}

@Composable
fun AdminGuideTab(reloadKey: Int) {
    val repository = remember { GuideBusinessRepository() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var businesses by remember(uid) { mutableStateOf<List<GuideBusiness>?>(null) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var busyId by remember(uid) { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(uid, reloadKey, revision, lifecycle) {
        if (uid == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                try {
                    val loaded = repository.list()
                    if (FirebaseAuth.getInstance().currentUser?.uid == uid) { businesses = loaded; error = null }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "No se pudo cargar la lista." }
                delay(20_000)
            }
        }
    }
    fun perform(id: String, done: String?, action: suspend () -> Unit) {
        if (busyId != null) return
        busyId = id
        scope.launch {
            try {
                action(); revision++
                done?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No se pudo guardar." }
            finally { busyId = null }
        }
    }
    AdminGuideContent(businesses, error, busyId,
        onRefresh = { revision++ },
        onPatch = { id, patch, done -> perform(id, done) { repository.update(id, patch) } },
        onDelete = { id -> perform(id, "Datos del comercio eliminados") { repository.delete(id) } })
}

@Composable
internal fun AdminGuideContent(
    businesses: List<GuideBusiness>?, error: String?, busyId: String?,
    onRefresh: () -> Unit, onPatch: (String, JSONObject, String?) -> Unit, onDelete: (String) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("new") }
    var editing by remember { mutableStateOf<GuideBusiness?>(null) }
    var deleting by remember { mutableStateOf<GuideBusiness?>(null) }
    val context = LocalContext.current
    editing?.let { business ->
        GuideBusinessEditor(business, onDismiss = { editing = null },
            onSave = { patch -> editing = null; onPatch(business.id, patch, "Cambios guardados") })
    }
    deleting?.let { business ->
        AlertDialog(onDismissRequest = { deleting = null },
            title = { Text("¿Eliminar ${business.name}?") },
            text = { Text("Se borran todos sus datos, también el contacto. No se puede deshacer. Úsalo cuando el comercio pida que eliminemos su información.") },
            confirmButton = { TextButton({ deleting = null; onDelete(business.id) }) { Text("Eliminar") } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancelar") } })
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminSectionHeading("Guía Descubre Satipo",
            "Comercios que se registraron en viajaconintu.pages.dev/comercios. Solo los publicados aparecen en la guía.",
            businesses?.count { it.status == "new" })
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("new" to "Nuevos", "contacted" to "Contactados", "published" to "Publicados", "archived" to "Archivados", "all" to "Todos")
                .forEach { (key, label) ->
                    val count = businesses?.count { key == "all" || it.status == key }
                    FilterChip(filter == key, onClick = { filter = key },
                        label = { Text(if (count != null) "$label ($count)" else label) })
                }
        }
        TextButton(onRefresh, enabled = busyId == null) { Text("Actualizar") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("guide-error")) }
        if (businesses == null && error == null) CircularProgressIndicator()
        val visible = businesses.orEmpty().filter { filter == "all" || it.status == filter }
        if (businesses != null && visible.isEmpty()) Text("No hay comercios en esta sección.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(visible, key = { it.id }) { business ->
                GuideBusinessCard(business, enabled = busyId == null,
                    onPatch = { patch, done -> onPatch(business.id, patch, done) },
                    onEdit = { editing = business }, onDelete = { deleting = business },
                    onOpen = { uri ->
                        val opened = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }.isSuccess
                        if (!opened) Toast.makeText(context, "No hay una app para abrir esto.", Toast.LENGTH_SHORT).show()
                    })
            }
        }
    }
}

@Composable
private fun GuideBusinessCard(
    business: GuideBusiness, enabled: Boolean, onPatch: (JSONObject, String) -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit, onOpen: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(Modifier.fillMaxWidth().testTag("guide-${business.id}")) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(business.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (business.featured) AssistChip(onClick = {}, label = { Text("Destacado") })
            }
            Text(listOf(GuideBusiness.CATEGORY_LABELS[business.category] ?: business.category, business.locality,
                statusLabel(business.status)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Text(GuideBusiness.INTEREST_LABELS[business.interest] ?: business.interest,
                color = if (business.interest == "destacado") colors.primary else colors.onSurfaceVariant,
                fontWeight = if (business.interest == "destacado") FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium)
            if (business.description.isNotBlank()) Text(business.description, style = MaterialTheme.typography.bodyMedium)
            if (business.address.isNotBlank()) Text("Dirección: ${business.address}", style = MaterialTheme.typography.bodySmall)
            Text(if (business.hasPoint) "Ubicación guardada" else "Sin ubicación: agrégala antes de publicar",
                color = if (business.hasPoint) colors.onSurfaceVariant else colors.error, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("Contacto: ${business.contactName} · ${business.phone}" +
                if (business.showPhone) " (visible en la guía)" else " (privado)", style = MaterialTheme.typography.bodySmall)
            if (business.email.isNotBlank()) Text(business.email, style = MaterialTheme.typography.bodySmall)
            if (business.socialUrl.isNotBlank()) Text(business.socialUrl, style = MaterialTheme.typography.bodySmall, color = colors.primary)
            Text("Registrado: ${formatLima(business.createdAt)}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            if (business.adminNote.isNotBlank()) Text("Nota: ${business.adminNote}", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton({ onOpen("https://wa.me/${GuideBusiness.whatsappNumber(business.phone)}?text=" +
                    Uri.encode("Hola ${business.contactName}, te escribo de Intu por tu registro en Descubre Satipo.")) }) { Text("WhatsApp") }
                TextButton({ onOpen("tel:${business.phone}") }) { Text("Llamar") }
                if (business.hasPoint) TextButton({ onOpen("geo:${business.latitude},${business.longitude}?q=${business.latitude},${business.longitude}(${Uri.encode(business.name)})") }) { Text("Ver en mapa") }
                if (business.socialUrl.startsWith("https://")) TextButton({ onOpen(business.socialUrl) }) { Text("Redes") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onEdit, enabled = enabled) { Text("Editar") }
                when (business.status) {
                    "new" -> OutlinedButton({ onPatch(JSONObject().put("status", "contacted"), "Marcado como contactado") }, enabled = enabled) { Text("Contactado") }
                    "archived" -> OutlinedButton({ onPatch(JSONObject().put("status", "new"), "Devuelto a nuevos") }, enabled = enabled) { Text("Volver a nuevos") }
                    else -> {}
                }
                if (business.status == "published") {
                    OutlinedButton({ onPatch(JSONObject().put("status", "contacted"), "Quitado de la guía") }, enabled = enabled) { Text("Quitar de la guía") }
                } else if (business.status != "archived") {
                    Button({ onPatch(JSONObject().put("status", "published"), "Publicado en la guía") }, enabled = enabled && business.hasPoint) { Text("Publicar") }
                }
                OutlinedButton({ onPatch(JSONObject().put("featured", !business.featured),
                    if (business.featured) "Ya no es destacado" else "Marcado como destacado") }, enabled = enabled) {
                    Text(if (business.featured) "Quitar destacado" else "Destacar")
                }
                if (business.status != "archived") TextButton({ onPatch(JSONObject().put("status", "archived"), "Archivado") }, enabled = enabled) { Text("Archivar") }
                TextButton(onDelete, enabled = enabled) { Text("Eliminar datos", color = colors.error) }
            }
        }
    }
}

@Composable
private fun GuideBusinessEditor(business: GuideBusiness, onDismiss: () -> Unit, onSave: (JSONObject) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(business.name) }
    var category by remember { mutableStateOf(business.category) }
    var description by remember { mutableStateOf(business.description) }
    var address by remember { mutableStateOf(business.address) }
    var point by remember { mutableStateOf(if (business.hasPoint) "${business.latitude}, ${business.longitude}" else "") }
    var showPhone by remember { mutableStateOf(business.showPhone) }
    var note by remember { mutableStateOf(business.adminNote) }
    var locating by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Editar comercio") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(120) }, label = { Text("Nombre") }, singleLine = true)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GuideBusiness.CATEGORY_LABELS.forEach { (key, label) ->
                        FilterChip(category == key, onClick = { category = key }, label = { Text(label) })
                    }
                }
                OutlinedTextField(description, { description = it.take(500) }, label = { Text("Descripción") }, minLines = 2)
                OutlinedTextField(address, { address = it.take(240) }, label = { Text("Dirección o referencia") })
                OutlinedTextField(point, { point = it; problem = null }, label = { Text("Ubicación (latitud, longitud)") },
                    supportingText = { Text("Pega lo que copia Google Maps al mantener presionado el lugar, o usa tu ubicación estando en el negocio.") },
                    singleLine = true)
                OutlinedButton(enabled = !locating, onClick = {
                    locating = true; problem = null
                    scope.launch {
                        val location = currentDeviceLocation(context)
                        locating = false
                        if (location == null) problem = "No se pudo obtener tu ubicación. Revisa el permiso y el GPS."
                        else point = "%.6f, %.6f".format(java.util.Locale.US, location.latitude, location.longitude)
                    }
                }) { Text(if (locating) "Buscando ubicación…" else "Usar mi ubicación actual") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(showPhone, { showPhone = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Mostrar el teléfono en la guía", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(note, { note = it.take(500) }, label = { Text("Nota interna (precio acordado, etc.)") }, minLines = 2)
                problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = if (point.isBlank()) null else GuideBusiness.parsePoint(point)
                when {
                    name.trim().length < 2 -> problem = "Escribe el nombre del comercio."
                    point.isNotBlank() && parsed == null -> problem = "Escribe la ubicación como «-11.2541, -74.6382», dentro del Perú."
                    point.isBlank() && business.status == "published" -> problem = "Un comercio publicado necesita ubicación. Quítalo de la guía primero."
                    else -> onSave(JSONObject().put("name", name.trim()).put("category", category).put("description", description.trim())
                        .put("address", address.trim()).put("show_phone", showPhone).put("admin_note", note.trim())
                        .put("latitude", parsed?.first ?: JSONObject.NULL).put("longitude", parsed?.second ?: JSONObject.NULL))
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } })
}

private fun statusLabel(status: String) = when (status) {
    "new" -> "Nuevo"; "contacted" -> "Contactado"; "published" -> "Publicado"; "archived" -> "Archivado"; else -> status
}

private fun formatLima(timestamp: String): String = runCatching {
    java.time.OffsetDateTime.parse(timestamp).atZoneSameInstant(java.time.ZoneId.of("America/Lima"))
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}.getOrDefault(timestamp)

/** Ubicación actual del teléfono del administrador (GPS o red), o la última conocida si es reciente. */
private suspend fun currentDeviceLocation(context: Context): Location? {
    val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    if (!granted) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (provider in providers) {
                val fresh = withTimeoutOrNull(15_000) {
                    suspendCancellableCoroutine<Location?> { continuation ->
                        val signal = android.os.CancellationSignal()
                        continuation.invokeOnCancellation { signal.cancel() }
                        manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { if (continuation.isActive) continuation.resume(it) }
                    }
                }
                if (fresh != null) return fresh
            }
        }
        return providers.mapNotNull { manager.getLastKnownLocation(it) }
            .filter { System.currentTimeMillis() - it.time < 2 * 60_000 }
            .maxByOrNull { it.time }
    } catch (e: SecurityException) { return null }
}
