package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.repositories.TesterRequest
import com.intu.taxi.repositories.TesterRequestRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminTestersDialog(onDismiss: () -> Unit) {
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.96f).fillMaxHeight(.92f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                TextButton(onDismiss) { Text("Cerrar") }
                AdminTestersTab(0)
            }
        }
    }
}

@Composable
fun AdminTestersTab(reloadKey: Int) {
    val repository = remember { TesterRequestRepository() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var requests by remember(uid) { mutableStateOf<List<TesterRequest>?>(null) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var busyId by remember(uid) { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(uid, reloadKey, revision, lifecycle) {
        if (uid == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                try {
                    val loaded = repository.list()
                    if (FirebaseAuth.getInstance().currentUser?.uid == uid) { requests = loaded; error = null }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { requests = null; error = e.message ?: "No se pudo cargar la lista." }
                delay(20_000)
            }
        }
    }
    AdminTestersContent(requests, error, busyId, onRefresh = { revision++ }, onReview = { id, status ->
        if (busyId == null) {
            busyId = id
            scope.launch {
                try { repository.review(id, status); revision++ }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "No se pudo guardar." }
                finally { busyId = null }
            }
        }
    })
}

@Composable
internal fun AdminTestersContent(requests: List<TesterRequest>?, error: String?, busyId: String?,
    onRefresh: () -> Unit, onReview: (String, String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("new") }
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminSectionHeading("Testers de Google Play", "Correos registrados desde la web. El usuario se une libremente al grupo y acepta la prueba en Play.", requests?.count { it.status == "new" })
        Text("Marcar como revisado solo organiza esta bandeja. No confirma la incorporación a Google Groups ni la instalación de Intu.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("new" to "Nuevos", "reviewed" to "Revisados", "archived" to "Archivados", "all" to "Todos").forEach { (key, label) ->
                FilterChip(filter == key, onClick = { filter = key }, label = { Text(label) })
            }
        }
        TextButton(onRefresh, enabled = busyId == null) { Text("Actualizar") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("tester-error")) }
        if (requests == null && error == null) CircularProgressIndicator()
        val visible = requests.orEmpty().filter { filter == "all" || it.status == filter }
        if (requests != null && visible.isEmpty()) Text("No hay correos en esta sección.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(visible, key = { it.id }) { request ->
                OutlinedCard(Modifier.fillMaxWidth().testTag("tester-${request.id}")) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(request.email, style = MaterialTheme.typography.titleMedium)
                        Text(runCatching { java.time.OffsetDateTime.parse(request.createdAt).atZoneSameInstant(java.time.ZoneId.of("America/Lima"))
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) }.getOrDefault(request.createdAt), style = MaterialTheme.typography.bodySmall)
                        TextButton({ clipboard.setText(AnnotatedString(request.email)) }) { Text("Copiar correo") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (request.status == "new") OutlinedButton({ onReview(request.id, "reviewed") }, enabled = busyId == null) { Text("Marcar revisado") }
                            else TextButton({ onReview(request.id, "new") }, enabled = busyId == null) { Text("Volver a nuevos") }
                            if (request.status != "archived") TextButton({ onReview(request.id, "archived") }, enabled = busyId == null) { Text("Archivar") }
                        }
                    }
                }
            }
        }
    }
}
