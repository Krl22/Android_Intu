package com.intu.taxi.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import com.intu.taxi.data.normalizedPlaceText
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.intu.taxi.repositories.BugReport
import com.intu.taxi.repositories.BugReportRepository
import com.intu.taxi.data.BugReportValidation
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun BugReportDialog(
    submitReport: suspend (String, String, String) -> Unit = { title, description, screen ->
        BugReportRepository().submit(title, description, screen)
    },
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var screen by rememberSaveable { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempted by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val descriptionFocus = remember { FocusRequester() }
    val titleError = if (attempted) BugReportValidation.titleError(title) else null
    val descriptionError = if (attempted) BugReportValidation.descriptionError(description) else null
    AccountDialogLayout(
        eyebrow = "AYÚDANOS A MEJORAR",
        title = "Reportar un error",
        subtitle = "Cuéntanos qué pasó. Te leemos.",
        onDismiss = onDismiss,
        dismissEnabled = !submitting,
        footer = {
            Button(enabled = !submitting, onClick = {
                if (submitting) return@Button
                attempted = true
                error = null
                when {
                    BugReportValidation.titleError(title) != null -> { titleFocus.requestFocus(); return@Button }
                    BugReportValidation.descriptionError(description) != null -> { descriptionFocus.requestFocus(); return@Button }
                }
                keyboard?.hide()
                submitting = true
                scope.launch {
                    try {
                        submitReport(title.trim(), description.trim(), screen.trim())
                        Toast.makeText(context, "Reporte enviado. Gracias por ayudarnos a mejorar Intu.", Toast.LENGTH_LONG).show()
                        onDismiss()
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        error = e.message ?: "No se pudo enviar. Tu texto se conserva; intenta de nuevo."
                    } finally { submitting = false }
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(16.dp)) {
                if (submitting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                else Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (submitting) "Enviando…" else "Enviar reporte", fontWeight = FontWeight.SemiBold)
            }
            TextButton(enabled = !submitting, onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
        }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = AccountTeal.copy(alpha = .08f)) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = AccountTeal, modifier = Modifier.size(20.dp))
                    Text("Tu reporte llega al panel del equipo de Intu. Incluye los pasos para que podamos revisar el problema.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Detalles del problema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(title, { title = BugReportValidation.limit(it, BugReportValidation.MAX_TITLE); error = null },
                modifier = Modifier.fillMaxWidth().focusRequester(titleFocus), label = { Text("Título") },
                placeholder = { Text("Ej. No puedo guardar mi dirección") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), enabled = !submitting, isError = titleError != null,
                supportingText = { Text(titleError ?: "Mínimo ${BugReportValidation.MIN_TITLE} caracteres · ${BugReportValidation.length(title)}/${BugReportValidation.MAX_TITLE}") })
            OutlinedTextField(screen, { screen = BugReportValidation.limit(it, BugReportValidation.MAX_SCREEN); error = null },
                modifier = Modifier.fillMaxWidth(), label = { Text("Pantalla (opcional)") },
                placeholder = { Text("Ej. Inicio, Cuenta o Viajes") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), enabled = !submitting)
            OutlinedTextField(description, { description = BugReportValidation.limit(it, BugReportValidation.MAX_DESCRIPTION); error = null },
                modifier = Modifier.fillMaxWidth().focusRequester(descriptionFocus), label = { Text("¿Qué ocurrió?") },
                placeholder = { Text("¿Qué hiciste? ¿Qué pasó? ¿Qué esperabas que pasara?") }, minLines = 4,
                shape = RoundedCornerShape(14.dp), enabled = !submitting, isError = descriptionError != null,
                supportingText = { Text(descriptionError ?: "Mínimo ${BugReportValidation.MIN_DESCRIPTION} caracteres · ${BugReportValidation.length(description)}/${BugReportValidation.MAX_DESCRIPTION}") })
            if (error != null) Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No pudimos enviarlo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("Tu texto se conserva. Puedes volver a intentarlo.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Text("Se incluirán la versión de Intu, el modelo del equipo y la versión de Android. Evita incluir contraseñas o datos de otras personas.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BugReportsTab(reloadKey: Int) {
    val repo = remember { BugReportRepository() }
    val scope = rememberCoroutineScope()
    var reports by remember { mutableStateOf<List<BugReport>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var localReload by remember { mutableIntStateOf(0) }
    var updatingId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(reloadKey, localReload) {
        error = null
        try { reports = repo.list() } catch (e: Exception) { error = e.message ?: "No se pudieron cargar los reportes." }
    }
    AdminReportsContent(reports, error, updatingId, onRetry = { localReload++ }, onStatus = { report, status ->
        updatingId = report.id
        scope.launch {
            try { repo.setStatus(report.id, status); localReload++ }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No se pudo actualizar." }
            finally { updatingId = null }
        }
    })
}

@Composable
internal fun AdminReportsContent(reports: List<BugReport>?, error: String?, updatingId: String?,
    onRetry: () -> Unit, onStatus: (BugReport, String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val statuses = listOf(null to "Todos", "open" to "Nuevos", "in_progress" to "En revisión", "resolved" to "Resueltos")
    val needle = normalizedPlaceText(query)
    val visible = reports.orEmpty().filter { (filter == null || it.status == filter) &&
        needle in normalizedPlaceText("${it.title} ${it.description} ${it.reporterName} ${it.screen}") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AdminSectionHeading("Reportes de errores", "Escucha a tus testers, revisa lo que ocurre y marca lo que ya está resuelto.", reports?.size) }
        item { AdminSearchField(query, { query = it }, "Buscar un reporte") }
        item { AdminFilters(statuses, filter, { filter = it }) }
        if (error != null) item { AdminMessage(error, true, onRetry) }
        if (reports == null && error == null) item { AdminLoading() }
        if (reports != null && visible.isEmpty()) item { AdminMessage(if (query.isBlank()) "No hay reportes en esta lista. Los enviados desde Cuenta aparecerán aquí."
            else "No hay reportes que coincidan con tu búsqueda.") }
        items(visible, key = { it.id }) { report ->
            AdminReportCard(report, updatingId != null, { status -> onStatus(report, status) })
        }
    }
}

@Composable
internal fun AdminReportCard(report: BugReport, busy: Boolean, onStatus: (String) -> Unit) {
    var expanded by rememberSaveable(report.id) { mutableStateOf(false) }
    AdminCard {
        val status = when (report.status) { "resolved" -> "Resuelto"; "in_progress" -> "En revisión"; else -> "Nuevo" }
        AdminBadge(status, if (report.status == "open") androidx.compose.ui.graphics.Color(0xFF99641C) else AdminTeal,
            if (report.status == "open") androidx.compose.ui.graphics.Color(0xFFFFF2DB) else androidx.compose.ui.graphics.Color(0xFFE3F2EE))
        Text(report.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("${report.reporterName.ifBlank { "Usuario" }} · ${report.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = AdminMuted)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(report.description, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (expanded) "Ocultar datos técnicos" else "Ver datos técnicos")
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminDetail("Pantalla", report.screen.ifBlank { "Sin pantalla indicada" })
                AdminDetail("Versión", "Intu ${report.appVersion}")
                AdminDetail("Dispositivo", report.deviceInfo)
            }
        }
        val primaryStatus = if (report.status == "open") "in_progress" else if (report.status == "in_progress") "resolved" else "open"
        Button(onClick = { onStatus(primaryStatus) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(when (primaryStatus) { "in_progress" -> "Revisar reporte"; "resolved" -> "Marcar como resuelto"; else -> "Reabrir reporte" })
        }
        if (report.status == "open") TextButton(onClick = { onStatus("resolved") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Marcar como resuelto")
        }
        if (report.status == "in_progress") TextButton(onClick = { onStatus("open") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Reabrir reporte")
        }
    }
}
