package com.intu.taxi.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intu.taxi.models.DriverRideRequest
import com.intu.taxi.models.QueuedDriverRequest
import com.intu.taxi.ui.formatSoles
import com.intu.taxi.ui.theme.AppearanceColors
import kotlinx.coroutines.delay
import java.util.Locale

private val Teal = Color(0xFF0F6E56)
private val TimerAmber = Color(0xFFBA7517)
private const val MAX_TABS = 3

private fun km(meters: Double) = String.format(Locale.US, "%.1f km", meters / 1000.0)

/**
 * Solicitudes del conductor como una pila: la del frente se ve completa y detrás asoman las demás
 * («lomos») con su precio y distancia, para comparar sin tocar nada. Tocar un lomo trae esa
 * solicitud al frente. [requests] llega ya ordenada, la primera es la del frente.
 */
@Composable
fun DriverRequestStack(
    requests: List<QueuedDriverRequest>,
    timeoutSeconds: Int,
    onSelect: (String) -> Unit,
    onAccept: (DriverRideRequest) -> Unit,
    onPass: (DriverRideRequest) -> Unit,
    offerAction: (DriverRideRequest) -> (() -> Unit)?,
    offerStatus: (DriverRideRequest) -> String?,
    modifier: Modifier = Modifier
) {
    val front = requests.firstOrNull() ?: return
    val behind = requests.drop(1)
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (behind.size > MAX_TABS) {
            Text("+${behind.size - MAX_TABS} más", style = MaterialTheme.typography.labelMedium, color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp)
                    .background(Color(0xFF1E1F47).copy(alpha = 0.8f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 2.dp))
        }
        // La más lejana del frente queda arriba y más angosta, como pestañas de carpeta
        val tabs = behind.take(MAX_TABS)
        tabs.asReversed().forEachIndexed { index, queued ->
            RequestTab(queued, inset = ((tabs.size - index) * 8).dp) { onSelect(queued.request.requestId) }
        }
        AnimatedContent(targetState = front, contentKey = { it.request.requestId },
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "frontRequest") { queued ->
            FrontRequestCard(queued, timeoutSeconds, squareTop = tabs.isNotEmpty(), onAccept = onAccept, onPass = onPass,
                onOfferPrice = offerAction(queued.request), offerStatus = offerStatus(queued.request))
        }
    }
}

@Composable
private fun RequestTab(queued: QueuedDriverRequest, inset: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val request = queued.request
    Row(
        Modifier.fillMaxWidth().padding(horizontal = inset)
            .background(AppearanceColors.tint(Color(0xFFF1F4F4)), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .border(BorderStroke(0.5.dp, AppearanceColors.outline), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .clickable(onClickLabel = "Ver esta solicitud", onClick = onClick)
            .testTag("request-tab-${request.requestId}")
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(formatSoles(request.estimatedPrice), fontWeight = FontWeight.Bold, color = AppearanceColors.ink, fontSize = 14.sp)
        Text(if (request.isDelivery) "Envío · ${request.originAddress}" else request.originAddress,
            style = MaterialTheme.typography.bodySmall, color = AppearanceColors.muted,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        queued.pickupMeters?.let { Text(km(it), style = MaterialTheme.typography.bodySmall, color = AppearanceColors.muted) }
        queued.secondsLeft?.let {
            Text("$it s", style = MaterialTheme.typography.labelSmall, color = AppearanceColors.highlight(TimerAmber))
        }
    }
}

@Composable
private fun FrontRequestCard(
    queued: QueuedDriverRequest,
    timeoutSeconds: Int,
    squareTop: Boolean,
    onAccept: (DriverRideRequest) -> Unit,
    onPass: (DriverRideRequest) -> Unit,
    onOfferPrice: (() -> Unit)?,
    offerStatus: String?
) {
    val request = queued.request
    // Si la solicitud del frente cambia (llegó una que paga más), Aceptar espera un instante
    // para que el conductor no acepte por error la que acaba de aparecer bajo su dedo.
    var armed by remember(request.requestId) { mutableStateOf(false) }
    LaunchedEffect(request.requestId) { delay(700); armed = true }
    val corner = if (squareTop) 6.dp else 20.dp
    Surface(
        shape = RoundedCornerShape(topStart = corner, topEnd = corner, bottomStart = 20.dp, bottomEnd = 20.dp),
        color = AppearanceColors.surface, shadowElevation = 6.dp,
        border = if (squareTop) BorderStroke(1.5.dp, AppearanceColors.highlight(Teal)) else null,
        modifier = Modifier.fillMaxWidth().testTag("request-front-${request.requestId}")
    ) {
        Column {
            queued.secondsLeft?.let { secondsLeft ->
                val progress by animateFloatAsState((secondsLeft - 1).coerceAtLeast(0) / timeoutSeconds.toFloat(),
                    tween(1000, easing = LinearEasing), label = "requestTimer")
                LinearProgressIndicator(progress = { progress }, color = TimerAmber,
                    trackColor = AppearanceColors.outline, modifier = Modifier.fillMaxWidth().height(4.dp))
            }
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (request.isDelivery) Text("Envío en moto lineal", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, color = AppearanceColors.highlight(Color(0xFF08817E)))
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        PaymentIcons(paymentMethod = request.paymentMethod, size = 16.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(if (request.paymentMethod == "efectivo") "Efectivo" else "Yape",
                            style = MaterialTheme.typography.labelMedium, color = AppearanceColors.muted)
                    }
                    Spacer(Modifier.weight(1f))
                    queued.secondsLeft?.let {
                        Text("$it s", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                            color = AppearanceColors.highlight(TimerAmber))
                    }
                }
                Avatar(url = request.userPhotoUrl, size = 56.dp, zoomable = true, contentDescription = "Foto del pasajero")
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(request.userName.ifBlank { if (request.isDelivery) "Quien envía" else "Pasajero" },
                        fontWeight = FontWeight.SemiBold, color = AppearanceColors.ink, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    request.riderRating?.let { Spacer(Modifier.width(6.dp)); RatingBadge(it) }
                }
                Text(formatSoles(request.estimatedPrice), fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    color = AppearanceColors.ink, modifier = Modifier.padding(vertical = 4.dp))
                val trip = "Viaje ${km(request.distanceMeters)} · ${kotlin.math.max(1, kotlin.math.round(request.durationSeconds / 60).toInt())} min"
                Text(listOfNotNull(queued.pickupMeters?.let { "Recojo a ${km(it)}" }, trip).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = AppearanceColors.muted)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().background(AppearanceColors.tint(Color(0xFFF5F7F7)), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AddressLine(Color(0xFF1D9E75), request.originAddress)
                    AddressLine(Color(0xFFD85A30), request.destinationAddress)
                }
                offerStatus?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onAccept(request) }, enabled = armed,
                    colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("request-accept")) {
                    Text("Aceptar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onPass(request) }, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 46.dp).testTag("request-pass")) {
                        Text("Pasar", color = AppearanceColors.ink)
                    }
                    onOfferPrice?.let { propose ->
                        OutlinedButton(onClick = propose, shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 46.dp)) {
                            Text("Otro precio", color = AppearanceColors.ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressLine(dot: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(dot, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(Color.DarkGray),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
