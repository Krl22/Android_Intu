package com.intu.taxi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.intu.taxi.repositories.SupportChatAdmin
import com.intu.taxi.repositories.SupportChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

@Composable
internal fun ColumnScope.AdminSupportChatSection(reloadKey: Int) {
    val repository = remember { SupportChatRepository() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var state by remember { mutableStateOf<SupportChatAdmin?>(null) }
    var knowledge by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }

    fun show(loaded: SupportChatAdmin) {
        state = loaded
        knowledge = loaded.knowledge
        limit = loaded.dailyLimitPerUser.toString()
    }
    LaunchedEffect(reloadKey, retry) {
        error = null
        try { show(repository.adminGet()) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No se pudo cargar el asistente." }
    }
    fun save(changes: JSONObject) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try { show(repository.adminSave(changes)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No se pudo guardar. Intenta de nuevo." }
            finally { busy = false }
        }
    }

    AdminSectionHeading("Asistente de ayuda", "Responde dudas de los usuarios en Cuenta → Ayuda con Intu, usando solo este texto.")
    val current = state
    if (current == null) {
        if (error == null) CircularProgressIndicator()
        else {
            Text(error.orEmpty(), color = colors.error)
            Button(onClick = { retry++ }) { Text("Reintentar") }
        }
        return
    }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().testTag("admin-support-chat")
            .toggleable(value = current.enabled, enabled = !busy, role = Role.Switch,
                onValueChange = { save(JSONObject().put("enabled", it)) })
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Mostrar a los usuarios", style = MaterialTheme.typography.titleMedium)
                Text(if (current.enabled) "Activado" else "Desactivado: nadie ve la opción",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Switch(checked = current.enabled, onCheckedChange = null, enabled = !busy)
        }
    }
    Text("Últimos 30 días: ${current.questions30d} preguntas de ${current.people30d} personas" +
        (if (current.failed30d > 0) " (${current.failed30d} sin respuesta)" else "") +
        " · costo estimado US$ ${String.format(Locale.US, "%.2f", current.estimatedCostUsd30d)}. " +
        "Se cobra en tu saldo de la API de Anthropic, no en la suscripción de Claude.",
        style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    OutlinedTextField(value = limit, onValueChange = { limit = it.filter(Char::isDigit).take(3) }, enabled = !busy,
        label = { Text("Preguntas por persona cada 24 horas") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = knowledge, onValueChange = { knowledge = it.take(60_000) }, enabled = !busy,
        label = { Text("Texto de ayuda") }, minLines = 8, maxLines = 18,
        supportingText = { Text("${knowledge.length} de 60.000 caracteres. Revisa precios y reglas antes de activarlo.") },
        modifier = Modifier.fillMaxWidth().testTag("admin-support-knowledge"))
    val limitValue = limit.toIntOrNull()
    val changed = knowledge != current.knowledge || limitValue != current.dailyLimitPerUser
    Button(enabled = !busy && changed && limitValue != null, modifier = Modifier.fillMaxWidth(), onClick = {
        save(JSONObject().put("knowledge", knowledge).put("daily_limit_per_user", limitValue))
    }) { Text(if (busy) "Guardando…" else "Guardar texto y límite") }
    error?.let { Text(it, color = colors.error) }
}
