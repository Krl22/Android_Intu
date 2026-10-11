package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intu.taxi.repositories.AccountDeletionRequestRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AccountDeletionDialog(
    submitRequest: suspend (String) -> Unit = { AccountDeletionRequestRepository().submit(it) },
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var note by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(if (sent) "Solicitud recibida" else "Eliminar mi cuenta") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sent) {
                    Text("Tu solicitud de eliminación de cuenta y datos personales se envió al equipo de Intu. La cuenta sigue activa hasta que el equipo complete la eliminación.")
                    Text("El equipo comprobará tu identidad y atenderá la solicitud en un máximo de 30 días. Los registros de servicios e incidencias pueden conservarse hasta 12 meses para atender reclamos, según la política de privacidad.")
                } else {
                    Text("Puedes solicitar que el equipo de Intu elimine tu cuenta y los datos personales asociados. Enviar esta solicitud no elimina tu cuenta inmediatamente.")
                    Text("Una vez completada la eliminación, perderás el acceso a esta cuenta. Si tienes un viaje abierto, termina o cancela el viaje antes de que el equipo la procese.")
                    Text("La solicitud se gestiona manualmente en un máximo de 30 días. Los registros de servicios e incidencias pueden conservarse hasta 12 meses para atender reclamos, según la política de privacidad.")
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(1200); error = null },
                        label = { Text("Información adicional (opcional)") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !submitting
                    )
                    Text("No necesitas indicar un motivo. No incluyas contraseñas ni documentos. Se enviarán tu identificador de cuenta, la versión de Intu y datos básicos del equipo al panel privado de administración.", style = MaterialTheme.typography.bodySmall)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !submitting, onClick = {
                if (sent) { onDismiss(); return@TextButton }
                if (submitting) return@TextButton
                submitting = true
                error = null
                scope.launch {
                    try {
                        submitRequest(note.trim())
                        sent = true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = e.message ?: "No se pudo enviar la solicitud. Intenta de nuevo."
                    } finally {
                        submitting = false
                    }
                }
            }) { Text(if (sent) "Cerrar" else if (submitting) "Enviando…" else "Solicitar eliminación") }
        },
        dismissButton = {
            if (!sent) TextButton(enabled = !submitting, onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
