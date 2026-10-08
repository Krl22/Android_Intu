package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intu.taxi.models.MotoOption
import com.intu.taxi.ui.components.CashIcon
import com.intu.taxi.ui.components.MotoOptionArt
import com.intu.taxi.ui.components.YapePlinIcon
import com.intu.taxi.ui.formatSoles
import com.intu.taxi.ui.map.TripRouteStyle
import java.util.Locale
import kotlin.math.ceil

/** Vehicle selection, payment and the next step stay together above the route. */
@Composable
internal fun RideOptionsSheet(
    fare: Double,
    deliveryFare: Double,
    distanceKm: Double,
    durationMinutes: Double,
    selectedOption: MotoOption?,
    paymentMethod: String,
    confirmEnabled: Boolean,
    error: String?,
    onSelect: (MotoOption) -> Unit,
    onChangePayment: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onToggleExpansion: (() -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    options: List<MotoOption> = MotoOption.entries,
    confirmLabel: String = "Elegir recojo",
    pickupLabel: String? = null,
    fareSettings: com.intu.taxi.models.FareSettings = com.intu.taxi.models.FareSettings.Default
) {
    val ink = AppearanceColors.foreground(Color(0xFF202538))
    val muted = AppearanceColors.secondary(Color(0xFF667175))
    val teal = AppearanceColors.highlight(TripRouteStyle.lineColor)
    Card(modifier.testTag("ride-options-sheet"), shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = AppearanceColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, AppearanceColors.outline)) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = if (onToggleExpansion != null) 4.dp else 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onToggleExpansion != null) Box(Modifier.fillMaxWidth().height(32.dp).testTag("moto-drawer-handle")
                .semantics { contentDescription = if (compact) "Mostrar opciones de moto" else "Ver más mapa"
                    stateDescription = if (compact) "Contraído" else "Expandido" }
                .clickable(role = Role.Button, onClick = onToggleExpansion), contentAlignment = Alignment.Center) {
                Box(Modifier.width(38.dp).height(4.dp).background(Color(0xFF9AA8A6), RoundedCornerShape(50)))
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (options == listOf(MotoOption.DELIVERY)) "Delivery en moto" else "Elige tu moto",
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = ink)
                    val routeSummary = buildList {
                        if (durationMinutes > 0) add("${ceil(durationMinutes).toInt()} min de recorrido")
                        if (distanceKm > 0) add(String.format(Locale.getDefault(), "%.1f km", distanceKm))
                    }.joinToString(" · ")
                    Text(routeSummary.ifBlank { "Tarifas estimadas según tu recorrido" },
                        style = MaterialTheme.typography.bodySmall, color = muted)
                    pickupLabel?.let { Text("Recojo: $it", style = MaterialTheme.typography.bodySmall, color = muted) }
                }
                if (compact) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedOption?.label ?: "Selecciona tu moto", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = ink)
                    selectedOption?.let { Text(formatSoles(it.fare(fare, deliveryFare, fareSettings)), color = teal, fontWeight = FontWeight.Bold) }
                }
                if (!compact) options.forEach { option ->
                    val selected = selectedOption == option
                    Row(Modifier.fillMaxWidth().testTag("moto-option-${option.code}").clip(RoundedCornerShape(16.dp))
                        .background(AppearanceColors.tint(if (selected) Color(0xFFEAF6F3) else Color(0xFFF7F9F9)))
                        .selectable(selected, role = Role.RadioButton, onClick = { onSelect(option) })
                        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MotoOptionArt(option, animate = selected, modifier = Modifier.size(width = 56.dp, height = 36.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(option.label, fontWeight = FontWeight.SemiBold, color = ink,
                                style = MaterialTheme.typography.bodyMedium)
                            Text(option.description, style = MaterialTheme.typography.bodySmall, color = muted)
                            if (LocalDensity.current.fontScale > 1.3f) Text(formatSoles(option.fare(fare, deliveryFare, fareSettings)),
                                color = teal, fontWeight = FontWeight.Bold)
                        }
                        if (LocalDensity.current.fontScale <= 1.3f) Text(formatSoles(option.fare(fare, deliveryFare, fareSettings)),
                            fontWeight = FontWeight.Bold, color = teal, style = MaterialTheme.typography.titleMedium)
                        if (selected) Icon(Icons.Default.CheckCircle, "Seleccionado", tint = teal, modifier = Modifier.size(18.dp))
                    }
                }
                if (!compact) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClickLabel = "Cambiar método de pago", onClick = onChangePayment)
                    .padding(vertical = 6.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (paymentMethod == "efectivo") CashIcon(size = 28.dp) else YapePlinIcon(size = 28.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Método de pago", style = MaterialTheme.typography.bodySmall, color = muted)
                        Text(if (paymentMethod == "efectivo") "Efectivo" else "Yape / Plin", fontWeight = FontWeight.SemiBold, color = ink)
                    }
                    Text("Cambiar", color = teal, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ChevronRight, null, tint = teal, modifier = Modifier.size(18.dp))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            Button(onConfirm, enabled = confirmEnabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = ink)) {
                Text(confirmLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
