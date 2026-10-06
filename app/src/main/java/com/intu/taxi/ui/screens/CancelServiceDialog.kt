package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.intu.taxi.data.CancelReasons
import com.intu.taxi.data.CancelRole
import com.intu.taxi.data.CancellationPreview
import com.intu.taxi.data.formatWait
import com.intu.taxi.repositories.CancellationRepository
import com.intu.taxi.ui.theme.AppearanceColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pide el motivo antes de cancelar y muestra, según el servidor, si la cancelación cuenta.
 * [onCancelled] recibe el estado del viaje después de cancelar ("cancelled" o "searching").
 */
@Composable
fun CancelServiceDialog(
    rideId: String,
    role: CancelRole,
    rideStatus: String,
    isDelivery: Boolean,
    onDismiss: () -> Unit,
    onCancelled: (newStatus: String, reason: String) -> Unit
) {
    val repository = remember { CancellationRepository() }
    val scope = rememberCoroutineScope()
    val reasons = remember(role, rideStatus, isDelivery) {
        if (role == CancelRole.DRIVER) CancelReasons.forDriver(isDelivery) else CancelReasons.forRider(rideStatus, isDelivery)
    }
    var selected by remember(rideId) { mutableStateOf<String?>(null) }
    var note by remember(rideId) { mutableStateOf("") }
    var preview by remember(rideId) { mutableStateOf<CancellationPreview?>(null) }
    var loadingPreview by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember(rideId) { mutableStateOf<String?>(null) }
    var waitLeft by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(rideId, selected, refresh) {
        val reason = selected ?: return@LaunchedEffect
        loadingPreview = true
        error = null
        try {
            val result = repository.preview(rideId, reason)
            preview = result
            waitLeft = result.waitSeconds
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { preview = null; error = e.message ?: "No se pudo revisar si esta cancelación cuenta." }
        finally { loadingPreview = false }
    }
    // La espera del "no aparece" corre aquí y se vuelve a consultar al servidor al terminar
    LaunchedEffect(preview) {
        if (preview?.outcome != "no_show_wait") return@LaunchedEffect
        while (waitLeft > 0) { delay(1_000); waitLeft-- }
        refresh++
    }

    val current = preview
    // Sin vista previa (p. ej. sin señal) igual se puede intentar: el servidor aplica las reglas
    val canConfirm = selected != null && !busy && !loadingPreview && current?.blocked != true
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (isDelivery) "¿Por qué cancelas el envío?" else "¿Por qué cancelas el viaje?") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(Modifier.selectableGroup()) {
                    reasons.forEach { reason ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                .selectable(selected = selected == reason.code, enabled = !busy, role = Role.RadioButton,
                                    onClick = { selected = reason.code })
                                .testTag("cancel-reason-${reason.code}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected == reason.code, onClick = null)
                            Text(reason.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                if (selected == CancelReasons.OTHER) {
                    OutlinedTextField(value = note, onValueChange = { note = it.take(300) }, enabled = !busy,
                        label = { Text("Cuéntanos qué pasó (opcional)") }, modifier = Modifier.fillMaxWidth())
                }
                when {
                    loadingPreview -> CircularProgressIndicator(Modifier.padding(top = 8.dp).size(20.dp), strokeWidth = 2.dp)
                    current != null -> {
                        val message = if (current.outcome == "no_show_wait")
                            "Podrás usar este motivo en ${formatWait(waitLeft)}. Espera en el punto de recojo."
                        else current.message(role)
                        if (message.isNotBlank()) Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (current.counts || current.blocked) AppearanceColors.highlight(Color(0xFFB45309))
                                else AppearanceColors.highlight(Color(0xFF067647)),
                            modifier = Modifier.padding(top = 8.dp).testTag("cancel-preview")
                        )
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(enabled = canConfirm, onClick = {
                val reason = selected ?: return@TextButton
                busy = true
                error = null
                scope.launch {
                    try {
                        val status = repository.cancel(rideId, reason, note.takeIf { reason == CancelReasons.OTHER })
                        onCancelled(status, reason)
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = e.message ?: "No se pudo cancelar. Intenta de nuevo." }
                    finally { busy = false }
                }
            }) {
                Text(if (busy) "Cancelando…" else "Cancelar servicio",
                    color = if (canConfirm) AppearanceColors.highlight(Color(0xFFB42318)) else Color.Unspecified)
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Volver") } }
    )
}
