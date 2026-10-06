package com.intu.taxi.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.intu.taxi.repositories.ScheduleWindow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Día y hora del viaje programado. "Ahora" vuelve al pedido inmediato. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleRideDialog(initial: Instant?, onDismiss: () -> Unit, onNow: () -> Unit, onSchedule: (Instant) -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    // Por defecto, la primera media hora en punto que deja al menos 30 minutos de margen
    val suggested = remember {
        initial?.atZone(zone) ?: java.time.ZonedDateTime.now(zone).plusMinutes(30).withSecond(0).withNano(0).let {
            if (it.minute <= 30) it.withMinute(30) else it.plusHours(1).withMinute(0)
        }
    }
    val days = remember { (0L..7L).map { today.plusDays(it) } }
    var day by remember { mutableStateOf(suggested.toLocalDate()) }
    val time = rememberTimePickerState(initialHour = suggested.hour, initialMinute = suggested.minute, is24Hour = true)
    val at = day.atTime(LocalTime.of(time.hour, time.minute)).atZone(zone).toInstant()
    val problem = ScheduleWindow.problem(at)
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEE d", Locale.forLanguageTag("es-PE")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cuándo es el viaje?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    days.forEach { option ->
                        val label = when (option) {
                            today -> "Hoy"
                            today.plusDays(1) -> "Mañana"
                            else -> option.format(dayFormat).replace(".", "")
                        }
                        FilterChip(selected = option == day, onClick = { day = option }, label = { Text(label) })
                    }
                }
                TimePicker(state = time, modifier = Modifier.testTag("schedule-time"))
                Text(problem ?: "Empezaremos a buscar conductor ${ScheduleWindow.DISPATCH_MINUTES_BEFORE} minutos antes. " +
                    "Te avisaremos si no encontramos a nadie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (problem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(enabled = problem == null, onClick = { onSchedule(at) }, modifier = Modifier.testTag("schedule-confirm")) {
                Text("Programar para ${ScheduleWindow.label(at, zone)}")
            }
        },
        dismissButton = { TextButton(onClick = onNow) { Text("Ahora") } }
    )
}
