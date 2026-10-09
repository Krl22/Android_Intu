package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.models.ServiceReport
import com.intu.taxi.models.ServiceReportCategory
import com.intu.taxi.repositories.ServiceReportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ServiceReportButton(rideId: String, isDriver: Boolean, delivery: Boolean, finished: Boolean,
    modifier: Modifier = Modifier) {
    var show by rememberSaveable(rideId) { mutableStateOf(false) }
    TextButton(onClick = { show = true }, modifier = modifier.testTag("service-report-button")) {
        Text(if (isDriver) "Reportar un problema con el cliente" else "Reportar un problema con el conductor")
    }
    if (show) ServiceReportFormDialog(rideId, isDriver, delivery, finished, onDismiss = { show = false })
}

@Composable
private fun ServiceReportFormDialog(rideId: String, isDriver: Boolean, delivery: Boolean, finished: Boolean,
    onDismiss: () -> Unit) {
    val repository = remember { ServiceReportRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val requestId = rememberSaveable(rideId) { UUID.randomUUID().toString() }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    ReportWindow {
        ServiceReportFormContent(isDriver, delivery, finished, busy, error, onDismiss = onDismiss,
            onSubmit = { category, description ->
                if (!busy) {
                    busy = true; error = null
                    scope.launch {
                        try {
                            repository.create(requestId, rideId, category, description)
                            android.widget.Toast.makeText(context, "Reporte guardado. Sigue el caso en Viajes → Reportes y objetos perdidos.",
                                android.widget.Toast.LENGTH_LONG).show()
                            onDismiss()
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { error = e.message ?: "No se pudo enviar el reporte." }
                        finally { busy = false }
                    }
                }
            })
    }
}

@Composable
internal fun ServiceReportFormContent(isDriver: Boolean, delivery: Boolean, finished: Boolean,
    busy: Boolean, error: String?, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    val options = remember(isDriver, delivery, finished) { ServiceReportCategory.options(isDriver, delivery, finished) }
    var selected by rememberSaveable(isDriver, delivery, finished) { mutableStateOf(options.first().code) }
    var description by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Reportar un problema", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton(onDismiss, enabled = !busy) { Text("Cerrar") }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("El reporte queda vinculado a este servicio para que el equipo de Intu pueda revisarlo.") }
            items(options, key = { it.code }) { option ->
                Row(Modifier.fillMaxWidth().testTag("report-reason-${option.code}")
                    .selectable(selected == option.code, enabled = !busy, role = Role.RadioButton,
                        onClick = { selected = option.code }).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == option.code, onClick = null, enabled = !busy)
                    Text(option.label, Modifier.weight(1f))
                }
            }
            item {
                if (selected == "lost_item") Text("Describe el objeto, su color y dónde pudo quedar. Compartiremos esta descripción " +
                    "con el conductor y le avisaremos que revise su vehículo. Podrán coordinar la devolución dentro del caso.")
                else Text("Esta denuncia la ven tu cuenta y el equipo de Intu. No se comparte con la persona reportada.")
            }
            item { OutlinedTextField(description, onValueChange = { description = it.take(2000) }, enabled = !busy,
                label = { Text(if (selected == "lost_item") "Describe tu objeto" else "¿Qué ocurrió?") },
                supportingText = { Text("${description.trim().length}/2000 · mínimo 10 caracteres") }, minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("service-report-description")) }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
        Button(onClick = { onSubmit(selected, description.trim()) },
            enabled = !busy && ServiceReportCategory.validDescription(description),
            modifier = Modifier.fillMaxWidth().testTag("service-report-submit")) {
            Text(if (busy) "Enviando…" else if (selected == "lost_item") "Enviar aviso de objeto perdido" else "Enviar reporte")
        }
    }
}

@Composable
fun ServiceReportsInboxButton(modifier: Modifier = Modifier) {
    var show by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = modifier.testTag("service-reports-inbox")) {
        Text("Reportes y objetos perdidos")
    }
    if (show) ServiceReportsDialog(onDismiss = { show = false })
}

@Composable
fun ServiceReportsDialog(admin: Boolean = false, onDismiss: () -> Unit) {
    ReportWindow { ServiceReportsPanel(admin, onDismiss = onDismiss) }
}

@Composable
private fun ReportWindow(content: @Composable () -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(0.95f), shape = MaterialTheme.shapes.large, content = content)
    }
}

@Composable
internal fun AdminReportsTab(reloadKey: Int) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(section == 0, onClick = { section = 0 }, label = { Text("Servicios y objetos") })
            FilterChip(section == 1, onClick = { section = 1 }, label = { Text("Errores de la app") })
        }
        Box(Modifier.weight(1f)) {
            if (section == 0) ServiceReportsPanel(admin = true, reloadKey = reloadKey)
            else BugReportsTab(reloadKey)
        }
    }
}

@Composable
private fun ServiceReportsPanel(admin: Boolean, reloadKey: Int = 0, onDismiss: (() -> Unit)? = null) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val repository = remember { ServiceReportRepository() }
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    var reports by remember(uid) { mutableStateOf<List<ServiceReport>?>(null) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var selected by rememberSaveable(uid) { mutableStateOf<String?>(null) }
    var busy by remember(uid) { mutableStateOf(false) }
    LaunchedEffect(uid, admin, reloadKey, retry, owner) {
        owner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                try { reports = repository.list(admin); error = null }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "No se pudieron cargar los reportes." }
                delay(10_000)
            }
        }
    }
    fun update(action: suspend () -> ServiceReport) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try { val updated = action(); reports = reports?.map { if (it.id == updated.id) updated else it }; retry++ }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No se pudo guardar." }
            finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (admin) "Reportes de servicios" else "Reportes y objetos perdidos", Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge)
            if (selected != null) TextButton(onClick = { selected = null }, enabled = !busy) { Text("Volver") }
            else onDismiss?.let { TextButton(it, enabled = !busy) { Text("Cerrar") } }
        }
        error?.let { Row(verticalAlignment = Alignment.CenterVertically) {
            Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { retry++ }, enabled = !busy) { Text("Reintentar") }
        } }
        val detail = reports?.find { it.id == selected }
        if (detail != null) ServiceReportDetailContent(detail, admin, busy,
            onState = { state -> update { repository.respond(detail.id, state) } },
            onMessage = { message -> update { repository.message(detail.id, message) } },
            onReview = { status, response, note -> update { repository.review(detail.id, status, response, note) } })
        else ServiceReportsListContent(reports, admin, onSelect = { selected = it.id }, onRefresh = { retry++ })
    }
}

@Composable
internal fun ServiceReportsListContent(reports: List<ServiceReport>?, admin: Boolean,
    onSelect: (ServiceReport) -> Unit, onRefresh: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("open") }
    val visible = reports.orEmpty().filter { (filter == "all" || (filter == "open" && it.open) ||
        (filter == "lost" && it.lostItem)) && (query.isBlank() ||
        "${it.description} ${it.reporterName} ${it.reportedName} ${ServiceReportCategory.label(it.category)}".contains(query, true)) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("open" to "Abiertos", "lost" to "Objetos", "all" to "Todos").forEach { (value, label) ->
                FilterChip(filter == value, onClick = { filter = value }, label = { Text(label) })
            }
        } }
        if (admin) item { OutlinedTextField(query, onValueChange = { query = it }, label = { Text("Buscar un caso") },
            modifier = Modifier.fillMaxWidth()) }
        item { Text(if (admin) "Revisa cada caso antes de tomar una medida. Hasta 200 casos, primero los abiertos."
            else "Aquí puedes seguir tus reportes y responder a los avisos de objetos perdidos.", style = MaterialTheme.typography.bodySmall) }
        if (reports == null) item { CircularProgressIndicator() }
        else if (visible.isEmpty()) item { Text("No hay casos en esta lista.") }
        items(visible, key = { it.id }) { report ->
            Card(onClick = { onSelect(report) }, modifier = Modifier.fillMaxWidth().testTag("service-report-${report.id}")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(ServiceReportCategory.label(report.category), style = MaterialTheme.typography.titleMedium)
                    Text(if (report.lostItem) report.lostStateLabel else report.statusLabel)
                    if (admin) Text("${report.reporterName} → ${report.reportedName}")
                    Text(report.description, maxLines = 3)
                    Text("${report.createdAt.take(10)} · ${report.originAddress} → ${report.destinationAddress}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { TextButton(onRefresh) { Text("Actualizar") } }
    }
}

@Composable
internal fun ServiceReportDetailContent(report: ServiceReport, admin: Boolean, busy: Boolean,
    onState: (String) -> Unit, onMessage: (String) -> Unit, onReview: (String, String, String) -> Unit) {
    var message by rememberSaveable(report.id) { mutableStateOf("") }
    var response by rememberSaveable(report.id, report.adminResponse) { mutableStateOf(report.adminResponse) }
    var note by rememberSaveable(report.id, report.adminNote) { mutableStateOf(report.adminNote) }
    var state by rememberSaveable(report.id, report.status) { mutableStateOf(report.status) }
    var confirmReturned by remember { mutableStateOf(false) }
    LaunchedEffect(report.messages) {
        if (message.isNotBlank() && report.messages.lastOrNull()?.let { it.mine && it.body == message.trim() } == true) message = ""
    }
    if (confirmReturned) AlertDialog(onDismissRequest = { confirmReturned = false }, title = { Text("¿Ya recibiste tu objeto?") },
        text = { Text("Confirma la devolución solamente cuando tengas tu objeto. El caso quedará resuelto.") },
        confirmButton = { TextButton(onClick = { confirmReturned = false; onState("returned") }) { Text("Sí, lo recibí") } },
        dismissButton = { TextButton(onClick = { confirmReturned = false }) { Text("Todavía no") } })
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(ServiceReportCategory.label(report.category), style = MaterialTheme.typography.titleLarge)
            Text(report.statusLabel, color = MaterialTheme.colorScheme.primary)
            if (report.lostItem) Text(report.lostStateLabel)
            Text("${report.originAddress} → ${report.destinationAddress}", style = MaterialTheme.typography.bodySmall)
            if (report.vehiclePlate.isNotBlank()) Text("Vehículo: ${report.vehiclePlate}")
            if (admin) Text("${report.reporterName} (${report.reporterRole}) → ${report.reportedName}")
            Text(report.description)
        }
        if (report.adminResponse.isNotBlank()) item { Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) { Text("Respuesta del equipo", style = MaterialTheme.typography.titleSmall); Text(report.adminResponse) }
        } }
        if (report.lostItem && report.open && !admin) {
            if (report.canCheckItem && report.lostState in setOf("awaiting_check", "not_found")) item {
                Text("Revisa los asientos, el piso y los compartimentos del vehículo.")
                Button(onClick = { onState("found") }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("lost-item-found")) { Text("Encontré el objeto") }
                if (report.lostState == "awaiting_check") OutlinedButton(onClick = { onState("not_found") }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().testTag("lost-item-not-found")) { Text("Revisé y no lo encontré") }
            }
            if (report.isReporter && report.lostState == "found") item { Button(onClick = { confirmReturned = true }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag("lost-item-returned")) { Text("Confirmar que recibí mi objeto") } }
        }
        if (report.lostItem) {
            item { Text("Coordinación de la devolución", style = MaterialTheme.typography.titleMedium) }
            items(report.messages, key = { it.id }) { entry -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(if (entry.mine) "Tú" else if (entry.role == "driver") "Conductor" else "Pasajero", style = MaterialTheme.typography.labelMedium)
                    Text(entry.body)
                }
            } }
            if (report.open && !admin && (report.isReporter || report.canCheckItem)) item {
                OutlinedTextField(message, onValueChange = { message = it.take(500) }, enabled = !busy,
                    label = { Text("Mensaje para coordinar la devolución") }, modifier = Modifier.fillMaxWidth().testTag("lost-item-message"))
                Button(onClick = { onMessage(message) }, enabled = !busy && message.isNotBlank()) { Text("Enviar mensaje") }
            }
        }
        if (admin) {
            item { Text("Atención del caso", style = MaterialTheme.typography.titleMedium) }
            items(listOf("open" to "Recibido", "in_review" to "En revisión", "resolved" to "Resuelto", "dismissed" to "Cerrar sin acción")) { (value, label) ->
                Row(Modifier.fillMaxWidth().selectable(state == value, enabled = !busy, role = Role.RadioButton,
                    onClick = { state = value }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(state == value, onClick = null, enabled = !busy); Text(label)
                }
            }
            item { OutlinedTextField(response, onValueChange = { response = it.take(2000) }, enabled = !busy,
                label = { Text(if (report.lostItem) "Respuesta visible para pasajero y conductor" else "Respuesta visible para quien reportó") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(note, onValueChange = { note = it.take(2000) }, enabled = !busy,
                label = { Text("Nota interna del equipo") }, modifier = Modifier.fillMaxWidth()) }
            item { Button(onClick = { onReview(state, response, note) }, enabled = !busy) { Text(if (busy) "Guardando…" else "Guardar revisión") } }
        }
    }
}
