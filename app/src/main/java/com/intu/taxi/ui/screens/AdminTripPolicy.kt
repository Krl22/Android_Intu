package com.intu.taxi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.intu.taxi.data.CancelReasons
import com.intu.taxi.data.formatBlockedUntil
import com.intu.taxi.repositories.AdminChatMessage
import com.intu.taxi.repositories.AdminChatSummary
import com.intu.taxi.repositories.CancellationOverview
import com.intu.taxi.repositories.TripPolicy
import com.intu.taxi.repositories.TripPolicyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Campos numéricos de la política: clave del servidor, etiqueta y lectura del valor actual. */
private val policyFields: List<Triple<String, String, (TripPolicy) -> Int>> = listOf(
    Triple("free_cancel_seconds", "Segundos sin penalidad tras aceptar", { it.freeCancelSeconds }),
    Triple("strike_limit", "Faltas que activan una pausa", { it.strikeLimit }),
    Triple("strike_window_days", "Días en que se cuentan las faltas", { it.strikeWindowDays }),
    Triple("excused_limit", "Excusas aceptadas en esos días", { it.excusedLimit }),
    Triple("first_block_minutes", "Primera pausa (minutos)", { it.firstBlockMinutes }),
    Triple("repeat_block_minutes", "Pausas siguientes en 30 días (minutos)", { it.repeatBlockMinutes }),
    Triple("no_show_wait_seconds", "Espera antes de \"no aparece\" (segundos)", { it.noShowWaitSeconds }),
    Triple("no_show_radius_m", "Distancia máxima al recojo (metros)", { it.noShowRadiusM }),
    Triple("max_requests_per_10_min", "Solicitudes máximas cada 10 minutos", { it.maxRequestsPer10Min })
)

internal fun cancelReasonLabel(code: String): String = when (code) {
    "no_show" -> "No apareció en el recojo"
    else -> (CancelReasons.forRider("accepted", false) + CancelReasons.forRider("searching", false) +
        CancelReasons.forDriver(false)).firstOrNull { it.code == code }?.label ?: "Otro motivo"
}

private fun adminDate(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
}.getOrDefault("")

@Composable
internal fun ColumnScope.AdminTripPolicySection(reloadKey: Int) {
    val repository = remember { TripPolicyRepository() }
    val scope = rememberCoroutineScope()
    var policy by remember { mutableStateOf<TripPolicy?>(null) }
    var overview by remember { mutableStateOf<CancellationOverview?>(null) }
    var chats by remember { mutableStateOf<List<AdminChatSummary>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var openChat by remember { mutableStateOf<String?>(null) }
    val drafts = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(reloadKey, retry) {
        error = null
        try {
            val loaded = repository.get()
            policy = loaded
            policyFields.forEach { (key, _, read) -> drafts[key] = read(loaded).toString() }
            overview = repository.overview()
            chats = repository.recentChats()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No se pudieron cargar las reglas de servicio." }
    }

    fun save(changes: JSONObject) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val saved = repository.save(changes)
                policy = saved
                policyFields.forEach { (key, _, read) -> drafts[key] = read(saved).toString() }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No se pudo guardar. Intenta de nuevo." }
            finally { busy = false }
        }
    }

    val current = policy
    AdminSectionHeading("Calificaciones", "Muestra las estrellas del otro participante durante el servicio.")
    if (current == null) {
        if (error == null) CircularProgressIndicator()
        else {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            Button(onClick = { retry++ }) { Text("Reintentar") }
        }
        return
    }
    PolicyCard {
        PolicySwitch("Estrellas del conductor", "El pasajero las ve cuando un conductor acepta", current.showDriverRating, busy,
            "admin-show-driver-rating") { save(JSONObject().put("show_driver_rating", it)) }
        HorizontalDivider()
        PolicySwitch("Estrellas del pasajero", "El conductor las ve en cada solicitud", current.showRiderRating, busy,
            "admin-show-rider-rating") { save(JSONObject().put("show_rider_rating", it)) }
    }
    PolicyNote("Con menos de 3 calificaciones se muestra «Nuevo». El cambio aplica a nuevas solicitudes y aceptaciones.")

    AdminSectionHeading("Cancelaciones", "Siempre se pide el motivo. Las faltas y pausas solo se aplican con la protección activada.")
    PolicyCard {
        PolicySwitch("Protección contra abusos",
            if (current.penaltiesEnabled) "Activada: las cancelaciones tardías cuentan como falta"
            else "Desactivada: solo se registran los motivos",
            current.penaltiesEnabled, busy, "admin-cancellation-penalties") { save(JSONObject().put("cancellation_penalties_enabled", it)) }
    }
    PolicyNote("Actívala cuando la mayoría tenga la versión que muestra los motivos. Cancelar es gratis mientras se busca conductor, " +
        "en los primeros segundos tras aceptar, si el conductor está terminando otro viaje o si el servidor confirma que llega tarde " +
        "o no se acerca. Culpar al otro (\"me pidió cancelar\", seguridad, problema del vehículo) se acepta pocas veces y queda como reporte.")
    PolicyCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            policyFields.forEach { (key, label, _) ->
                OutlinedTextField(
                    value = drafts[key].orEmpty(), onValueChange = { value -> drafts[key] = value.filter(Char::isDigit).take(5) },
                    label = { Text(label) }, singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("admin-policy-$key")
                )
            }
            val changed = policyFields.filter { (key, _, read) -> drafts[key]?.toIntOrNull() != read(current) }
            Button(enabled = !busy && changed.isNotEmpty() && changed.all { (key, _, _) -> drafts[key]?.toIntOrNull() != null },
                onClick = { save(JSONObject().apply { changed.forEach { (key, _, _) -> put(key, drafts[key]!!.toInt()) } }) },
                modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Guardando…" else "Guardar valores") }
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

    val people = overview?.people.orEmpty()
    AdminSectionHeading("Quién cancela más", "Últimos 7 días. Las faltas cuentan solo con la protección activada.", people.size)
    if (people.isEmpty()) PolicyNote("Aún no hay cancelaciones registradas.")
    people.forEach { person ->
        PolicyCard {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${person.name} · ${if (person.role == "driver") "Conductor" else "Pasajero"}",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${person.counted} cuentan · ${person.excused} con excusa · ${person.total} en total · ${person.reports} reportes en su contra",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                person.blockedUntil?.let { until ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("En pausa ${formatBlockedUntil(until) ?: ""}", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                try { repository.liftBlock(person.userId, person.role); overview = repository.overview() }
                                catch (e: CancellationException) { throw e }
                                catch (e: Exception) { error = e.message ?: "No se pudo quitar la pausa." }
                                finally { busy = false }
                            }
                        }) { Text("Quitar pausa") }
                    }
                }
            }
        }
    }

    val reports = overview?.reports.orEmpty()
    if (reports.isNotEmpty()) {
        AdminSectionHeading("Reportes en cancelaciones", "Cuando alguien culpa a la otra persona al cancelar.", reports.size)
        reports.forEach { report ->
            PolicyCard {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${report.userName} → ${report.reportedName}", style = MaterialTheme.typography.titleSmall)
                        Text(cancelReasonLabel(report.reason) + (if (report.note.isNotBlank()) ": ${report.note}" else ""),
                            style = MaterialTheme.typography.bodySmall)
                        Text(adminDate(report.createdAt), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { openChat = report.rideId }) { Text("Ver chat") }
                }
            }
        }
    }

    val chatList = chats.orEmpty()
    AdminSectionHeading("Chats de viajes", "Se guardan 30 días. Ábrelos solo para revisar un reclamo.", chatList.size)
    if (chatList.isEmpty()) PolicyNote("No hay chats en los últimos 30 días.")
    chatList.forEach { chat ->
        PolicyCard {
            Column(Modifier.fillMaxWidth().clickable { openChat = chat.rideId }.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${chat.riderName} y ${chat.driverName}", style = MaterialTheme.typography.titleSmall)
                Text("${chat.messages} mensajes · ${adminDate(chat.lastMessageAt)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    openChat?.let { rideId -> AdminChatDialog(rideId, repository) { openChat = null } }
}

@Composable
private fun AdminChatDialog(rideId: String, repository: TripPolicyRepository, onDismiss: () -> Unit) {
    var messages by remember(rideId) { mutableStateOf<List<AdminChatMessage>?>(null) }
    var error by remember(rideId) { mutableStateOf<String?>(null) }
    LaunchedEffect(rideId) {
        try { messages = repository.chat(rideId) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No se pudo cargar el chat." }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat del viaje") },
        text = {
            val list = messages
            when {
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                list == null -> CircularProgressIndicator()
                list.isEmpty() -> Text("Este viaje no tiene mensajes.")
                else -> LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list) { message ->
                        Column {
                            Text("${message.senderName.ifBlank { "Sin nombre" }} · " +
                                "${if (message.role == "driver") "Conductor" else "Pasajero"} · ${adminDate(message.createdAt)}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(message.body)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun PolicyCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun PolicySwitch(title: String, subtitle: String, checked: Boolean, busy: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag)
        .toggleable(value = checked, enabled = !busy, role = Role.Switch, onValueChange = onChange)
        .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = !busy)
    }
}

@Composable
private fun PolicyNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
