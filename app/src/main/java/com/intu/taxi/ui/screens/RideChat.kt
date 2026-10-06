package com.intu.taxi.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.data.CancelRole
import com.intu.taxi.repositories.QuickReplies
import com.intu.taxi.repositories.RideChatRepository
import com.intu.taxi.repositories.RideMessage
import com.intu.taxi.ui.theme.AppearanceColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val chatStatuses = setOf("accepted", "arrived", "in_progress")

/** El conductor solo escribe detenido en el recojo; en movimiento usa respuestas rápidas. */
internal fun canTypeInRideChat(role: CancelRole, rideStatus: String): Boolean =
    rideStatus in chatStatuses && (role == CancelRole.RIDER || rideStatus == "arrived")

/**
 * Botón de mensajes del viaje con los no leídos. Mientras está en pantalla escucha la conversación
 * y avisa con un mensaje breve si llega algo con el chat cerrado.
 */
@Composable
fun RideChatButton(
    rideId: String,
    rideStatus: String,
    role: CancelRole,
    otherName: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (rideStatus !in chatStatuses) return
    val context = LocalContext.current
    val repository = remember { RideChatRepository() }
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    var messages by remember(rideId) { mutableStateOf<List<RideMessage>?>(null) }
    var open by remember(rideId) { mutableStateOf(false) }
    var seenFromOther by remember(rideId) { mutableIntStateOf(-1) }

    LaunchedEffect(rideId) { repository.messages(rideId).collect { messages = it } }
    val fromOther = messages?.filter { it.senderId != myUid }.orEmpty()
    LaunchedEffect(fromOther.size, open) {
        val loaded = messages ?: return@LaunchedEffect
        when {
            // Lo que ya había al abrir la tarjeta no cuenta como nuevo
            seenFromOther < 0 && !open -> seenFromOther = loaded.count { it.senderId != myUid }
            open -> seenFromOther = fromOther.size
            fromOther.size > seenFromOther -> fromOther.lastOrNull()?.let {
                Toast.makeText(context, "$otherName: ${it.body}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val unread = if (open || seenFromOther < 0) 0 else (fromOther.size - seenFromOther).coerceAtLeast(0)

    if (compact) {
        IconButton(onClick = { open = true }, modifier = modifier.testTag("ride-chat-button")) {
            BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Mensajes con $otherName",
                    tint = AppearanceColors.highlight(Color(0xFF08817E)))
            }
        }
    } else {
        OutlinedButton(onClick = { open = true }, modifier = modifier.testTag("ride-chat-button")) {
            BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(if (unread > 0) "Mensajes nuevos" else "Enviar mensaje")
        }
    }

    if (open) RideChatSheet(rideId, rideStatus, role, otherName, myUid, messages.orEmpty(), repository) { open = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideChatSheet(
    rideId: String,
    rideStatus: String,
    role: CancelRole,
    otherName: String,
    myUid: String,
    messages: List<RideMessage>,
    repository: RideChatRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draft by remember(rideId) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Lo enviado aparece al instante, antes de que vuelva por Realtime
    var pending by remember(rideId) { mutableStateOf<List<RideMessage>>(emptyList()) }
    val shown = remember(messages, pending) {
        (messages + pending.filter { p -> messages.none { it.id == p.id } }).sortedBy { it.createdAt }
    }
    val canType = canTypeInRideChat(role, rideStatus)
    val time = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(shown.size) { if (shown.isNotEmpty()) listState.animateScrollToItem(shown.lastIndex) }

    fun send(action: suspend () -> RideMessage) {
        if (sending) return
        sending = true
        error = null
        scope.launch {
            try {
                val sent = action()
                pending = pending + sent
                draft = ""
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No se pudo enviar. Intenta de nuevo." }
            finally { sending = false }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppearanceColors.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text("Chat con $otherName", style = MaterialTheme.typography.titleMedium,
                    color = AppearanceColors.foreground(Color(0xFF1E1F47)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Por seguridad, Intu guarda estos mensajes 30 días.", style = MaterialTheme.typography.bodySmall,
                    color = AppearanceColors.secondary(Color(0xFF5F6570)))
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 360.dp), state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (shown.isEmpty()) item {
                    Text("Aún no hay mensajes.", style = MaterialTheme.typography.bodyMedium,
                        color = AppearanceColors.secondary(Color(0xFF5F6570)), modifier = Modifier.padding(vertical = 24.dp))
                }
                items(shown, key = { it.id }) { message ->
                    val mine = message.senderId == myUid
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                        Column(
                            Modifier.widthIn(max = 280.dp)
                                .background(
                                    if (mine) Color(0xFF08817E) else AppearanceColors.tint(Color(0xFFF0F2F5)),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(message.body, color = if (mine) Color.White else AppearanceColors.foreground(Color(0xFF1E1F47)))
                            Text(time.format(Date(message.createdAt)), style = MaterialTheme.typography.labelSmall,
                                color = if (mine) Color.White.copy(alpha = 0.8f) else AppearanceColors.secondary(Color(0xFF5F6570)),
                                modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (if (role == CancelRole.DRIVER) QuickReplies.driver else QuickReplies.rider).forEach { (code, label) ->
                    SuggestionChip(onClick = { send { repository.sendQuickReply(rideId, code) } }, enabled = !sending,
                        label = { Text(label) }, modifier = Modifier.testTag("quick-reply-$code"))
                }
            }
            if (canType) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = draft, onValueChange = { draft = it.take(500) }, enabled = !sending,
                        placeholder = { Text("Escribe un mensaje") }, maxLines = 4, modifier = Modifier.weight(1f).testTag("ride-chat-input"))
                    IconButton(onClick = { val text = draft.trim(); if (text.isNotEmpty()) send { repository.send(rideId, text) } },
                        enabled = !sending && draft.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar",
                            tint = AppearanceColors.highlight(Color(0xFF08817E)))
                    }
                }
            } else {
                Text("Mientras manejas, usa las respuestas rápidas. Podrás escribir al llegar al punto de recojo.",
                    style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(Color(0xFF5F6570)))
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
