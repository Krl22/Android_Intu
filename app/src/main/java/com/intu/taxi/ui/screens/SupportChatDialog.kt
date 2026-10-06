package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.intu.taxi.repositories.SupportChatRepository
import com.intu.taxi.repositories.SupportTurn
import com.intu.taxi.ui.theme.AppearanceColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val starterQuestions = listOf(
    "¿Cómo se calcula el precio?", "¿Cómo pago por Yape?", "¿Puedo cancelar sin penalidad?",
    "¿Cómo envío un paquete?", "¿Cómo me registro como conductor?"
)

/** Preguntas frecuentes con el asistente. La conversación no se guarda: se pierde al cerrar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatDialog(onDismiss: () -> Unit, onReportProblem: () -> Unit) {
    val repository = remember { SupportChatRepository() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var turns by remember { mutableStateOf<List<SupportTurn>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var waiting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var remaining by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(turns.size, waiting) { if (turns.isNotEmpty()) listState.animateScrollToItem(turns.lastIndex + if (waiting) 1 else 0) }

    fun ask(question: String) {
        val text = question.trim().take(1000)
        if (text.isEmpty() || waiting) return
        val conversation = turns + SupportTurn(fromUser = true, text = text)
        turns = conversation
        draft = ""
        error = null
        waiting = true
        scope.launch {
            try {
                val reply = repository.ask(conversation)
                turns = conversation + SupportTurn(fromUser = false, text = reply.text)
                remaining = reply.remainingToday
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                // La pregunta vuelve al campo para reintentar sin escribirla de nuevo
                turns = conversation.dropLast(1)
                draft = text
                error = e.message ?: "El asistente no pudo responder. Intenta de nuevo."
            } finally { waiting = false }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppearanceColors.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text("Ayuda con Intu", style = MaterialTheme.typography.titleMedium,
                    color = AppearanceColors.foreground(Color(0xFF1E1F47)))
                Text("Respuestas automáticas a partir de la ayuda de Intu. Pueden equivocarse; no veo tus viajes ni tu cuenta.",
                    style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(Color(0xFF5F6570)))
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 400.dp), state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (turns.isEmpty()) item {
                    Text("Hola, ¿en qué te ayudo? Elige una pregunta o escribe la tuya.",
                        color = AppearanceColors.secondary(Color(0xFF5F6570)), modifier = Modifier.padding(vertical = 12.dp))
                }
                itemsIndexed(turns) { _, turn ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (turn.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
                        Text(turn.text,
                            color = if (turn.fromUser) Color.White else AppearanceColors.foreground(Color(0xFF1E1F47)),
                            modifier = Modifier.widthIn(max = 300.dp)
                                .background(if (turn.fromUser) Color(0xFF08817E) else AppearanceColors.tint(Color(0xFFF0F2F5)),
                                    RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
                if (waiting) item { CircularProgressIndicator(Modifier.padding(8.dp).size(20.dp), strokeWidth = 2.dp) }
            }
            if (turns.isEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    starterQuestions.forEach { question ->
                        SuggestionChip(onClick = { ask(question) }, enabled = !waiting, label = { Text(question) })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = draft, onValueChange = { draft = it.take(1000) }, enabled = !waiting,
                    placeholder = { Text("Escribe tu pregunta") }, maxLines = 4,
                    modifier = Modifier.weight(1f).testTag("support-chat-input"))
                IconButton(onClick = { ask(draft) }, enabled = !waiting && draft.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar pregunta",
                        tint = AppearanceColors.highlight(Color(0xFF08817E)))
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(remaining?.let { "Te quedan $it preguntas hoy." } ?: "¿Problema con un viaje?",
                    style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(Color(0xFF5F6570)),
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onReportProblem) { Text("Reportar un problema") }
            }
        }
    }
}
