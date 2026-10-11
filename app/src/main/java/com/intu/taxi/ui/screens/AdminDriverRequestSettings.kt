package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.intu.taxi.models.DriverRequestOrder
import com.intu.taxi.models.DriverRequestSettings
import com.intu.taxi.repositories.DriverRequestSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ColumnScope.AdminDriverRequestSection(reloadKey: Int) {
    val repository = remember { DriverRequestSettingsRepository() }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf<DriverRequestSettings?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(reloadKey, retry) {
        settings = null; error = null; saved = false
        try { settings = repository.get() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = "No se pudieron cargar los ajustes de solicitudes. Intenta de nuevo." }
    }
    AdminDriverRequestContent(settings, busy, error, saved, onRetry = { retry++ }, onEdit = { saved = false; error = null },
        onSave = { values ->
            if (!busy) scope.launch {
                busy = true; error = null; saved = false
                try { settings = repository.save(values); saved = true }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = "No se pudieron guardar los ajustes. Intenta de nuevo." }
                finally { busy = false }
            }
        })
}

@Composable
internal fun ColumnScope.AdminDriverRequestContent(settings: DriverRequestSettings?, busy: Boolean, error: String?, saved: Boolean,
    onRetry: () -> Unit, onEdit: () -> Unit, onSave: (DriverRequestSettings) -> Unit) {
    AdminSectionHeading("Solicitudes para conductores", "Cómo ve el conductor varias solicitudes a la vez.")
    if (settings == null) {
        if (error == null) CircularProgressIndicator()
        else { Text(error, color = MaterialTheme.colorScheme.error); Button(onRetry) { Text("Reintentar") } }
        return
    }
    var order by remember(settings) { mutableStateOf(settings.order) }
    var seconds by remember(settings) { mutableStateOf(settings.timeoutSeconds.toString()) }
    val parsedSeconds = seconds.toIntOrNull()?.takeIf {
        it in DriverRequestSettings.MIN_TIMEOUT_SECONDS..DriverRequestSettings.MAX_TIMEOUT_SECONDS }
    AdminCard {
        Text("Primero en la pila", style = MaterialTheme.typography.titleMedium)
        listOf(
            Triple(DriverRequestOrder.FARE, "La que paga más", "Si llega una que paga más, pasa al frente. Con el mismo precio, la que llegó antes."),
            Triple(DriverRequestOrder.DISTANCE, "La más cercana", "La de recojo más cerca del conductor va al frente.")
        ).forEach { (value, title, description) ->
            Row(Modifier.fillMaxWidth().testTag("admin-request-order-${value.code}")
                .selectable(selected = order == value, enabled = !busy, role = Role.RadioButton) { order = value; onEdit() },
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = order == value, onClick = null, enabled = !busy)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedTextField(value = seconds, onValueChange = { seconds = it.filter(Char::isDigit).take(3); onEdit() },
            label = { Text("Segundos para decidir cada solicitud") }, singleLine = true, enabled = !busy,
            isError = parsedSeconds == null,
            supportingText = { Text("Entre ${DriverRequestSettings.MIN_TIMEOUT_SECONDS} y ${DriverRequestSettings.MAX_TIMEOUT_SECONDS} segundos") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("admin-request-timeout"))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (saved) Text("Ajustes guardados", color = MaterialTheme.colorScheme.primary)
        val changed = order != settings.order || parsedSeconds != settings.timeoutSeconds
        Button(onClick = { parsedSeconds?.let { onSave(DriverRequestSettings(order, it)) } },
            enabled = !busy && changed && parsedSeconds != null,
            modifier = Modifier.fillMaxWidth().testTag("admin-request-save")) {
            Text(if (busy) "Guardando…" else "Guardar")
        }
    }
    Text("Si el conductor no decide a tiempo, la solicitud se pasa sola y sigue disponible para otros conductores. " +
        "Si el conductor propuso otro precio, no vence mientras espera la respuesta. Los conductores en línea reciben el cambio en menos de un minuto.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
