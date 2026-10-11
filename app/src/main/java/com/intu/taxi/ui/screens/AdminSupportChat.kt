package com.intu.taxi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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

/** Pestaña "Asistente" del panel admin. */
@Composable
internal fun AdminSupportChatTab(reloadKey: Int) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AdminSupportChatSection(reloadKey)
    }
}

@Composable
internal fun ColumnScope.AdminSupportChatSection(reloadKey: Int) {
    val repository = remember { SupportChatRepository() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var state by remember { mutableStateOf<SupportChatAdmin?>(null) }
    var knowledge by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var webLimit by remember { mutableStateOf("") }
    var webTotal by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }

    fun show(loaded: SupportChatAdmin) {
        state = loaded
        knowledge = loaded.knowledge
        limit = loaded.dailyLimitPerUser.toString()
        webLimit = loaded.webDailyLimitPerVisitor.toString()
        webTotal = loaded.webDailyTotal.toString()
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

    AdminSectionHeading("Asistente de ayuda", "Responde dudas en Cuenta → Ayuda con Intu y, si lo eliges, en la página web, usando solo este texto. Aparece cuando lo prendes.")
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
        HorizontalDivider(color = colors.outlineVariant)
        Row(Modifier.fillMaxWidth().testTag("admin-support-chat-web")
            .toggleable(value = current.webEnabled, enabled = !busy, role = Role.Switch,
                onValueChange = { save(JSONObject().put("web_enabled", it)) })
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Mostrar en la página web", style = MaterialTheme.typography.titleMedium)
                Text(when {
                    !current.webEnabled -> "Desactivado: la web no muestra el chat"
                    !current.enabled -> "Se mostrará cuando prendas el asistente"
                    else -> "Activado: botón «¿Dudas?» en viajaconintu.pages.dev"
                }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Switch(checked = current.webEnabled, onCheckedChange = null, enabled = !busy)
        }
    }
    Text("Últimos 30 días: ${current.questions30d} preguntas de ${current.people30d} personas" +
        (if (current.webQuestions30d > 0) ", ${current.webQuestions30d} desde la web" else "") +
        (if (current.failed30d > 0) " (${current.failed30d} sin respuesta)" else "") +
        " · costo estimado US$ ${String.format(Locale.US, "%.2f", current.estimatedCostUsd30d)}. " +
        "Se cobra en tu saldo de la API de Anthropic, no en la suscripción de Claude.",
        style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    OutlinedTextField(value = limit, onValueChange = { limit = it.filter(Char::isDigit).take(3) }, enabled = !busy,
        label = { Text("Preguntas por persona cada 24 horas") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = webLimit, onValueChange = { webLimit = it.filter(Char::isDigit).take(2) }, enabled = !busy,
            label = { Text("Web: por visitante") }, singleLine = true, supportingText = { Text("1 a 50 al día") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        OutlinedTextField(value = webTotal, onValueChange = { webTotal = it.filter(Char::isDigit).take(4) }, enabled = !busy,
            label = { Text("Web: total diario") }, singleLine = true, supportingText = { Text("1 a 5000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
    }
    OutlinedTextField(value = knowledge, onValueChange = { knowledge = it.take(60_000) }, enabled = !busy,
        label = { Text("Texto de ayuda") }, minLines = 8, maxLines = 18,
        supportingText = { Text("${knowledge.length} de 60.000 caracteres. Revisa precios y reglas antes de activarlo.") },
        modifier = Modifier.fillMaxWidth().testTag("admin-support-knowledge"))
    val limitValue = limit.toIntOrNull()
    val webLimitValue = webLimit.toIntOrNull()?.takeIf { it in 1..50 }
    val webTotalValue = webTotal.toIntOrNull()?.takeIf { it in 1..5000 }
    val changed = knowledge != current.knowledge || limitValue != current.dailyLimitPerUser ||
        webLimitValue != current.webDailyLimitPerVisitor || webTotalValue != current.webDailyTotal
    Button(enabled = !busy && changed && limitValue != null && webLimitValue != null && webTotalValue != null,
        modifier = Modifier.fillMaxWidth(), onClick = {
        save(JSONObject().put("knowledge", knowledge).put("daily_limit_per_user", limitValue)
            .put("web_daily_limit_per_visitor", webLimitValue).put("web_daily_total", webTotalValue))
    }) { Text(if (busy) "Guardando…" else "Guardar texto y límites") }
    error?.let { Text(it, color = colors.error) }
}
