package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intu.taxi.repositories.EarningsSummary
import com.intu.taxi.repositories.RideHistoryItem
import com.intu.taxi.repositories.RideHistoryRepository
import com.intu.taxi.ui.components.Avatar
import com.intu.taxi.ui.components.StarRating
import com.intu.taxi.ui.formatSoles
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateLabel = DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm", Locale("es", "PE"))

/**
 * Pestaña Viajes con datos reales de Supabase.
 * Pasajero: sus viajes y calificar al conductor. Conductor: ganancias, calificación e historial.
 */
@Composable
fun TripsScreenEnhanced(padding: PaddingValues, isDriver: Boolean) {
    val repo = remember { RideHistoryRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var rides by remember { mutableStateOf<List<RideHistoryItem>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var driverRating by remember { mutableStateOf<Pair<Double, Int>?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var selectedRide by remember { mutableStateOf<RideHistoryItem?>(null) }
    val routeCache = remember { mutableMapOf<String, List<com.mapbox.geojson.Point>>() }
    selectedRide?.let { ride ->
        RideDetailsDialog(ride, routeCache, onDismiss = { selectedRide = null })
    }

    LaunchedEffect(isDriver, reloadKey) {
        loadError = null
        runCatching { if (isDriver) repo.driverHistory() else repo.riderHistory() }
            .onSuccess { rides = it }
            .onFailure {
                loadError = it.message ?: "No se pudieron cargar tus viajes"
                if (rides == null) rides = emptyList()
            }
        if (isDriver) driverRating = runCatching { repo.driverRating() }.getOrNull()
    }

    fun rate(ride: RideHistoryItem, stars: Int) {
        scope.launch {
            runCatching { repo.rate(ride.id, stars) }
                .onSuccess {
                    rides = rides?.map {
                        when {
                            it.id != ride.id -> it
                            isDriver -> it.copy(ratingForRider = stars)
                            else -> it.copy(ratingForDriver = stars)
                        }
                    }
                    Toast.makeText(context, "¡Gracias por calificar!", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(context, it.message ?: "No se pudo guardar la calificación", Toast.LENGTH_LONG).show()
                }
        }
    }

    // Viajes programados del pasajero (los conductores no programan)
    val scheduledRepo = remember { com.intu.taxi.repositories.ScheduledRideRepository() }
    var scheduled by remember { mutableStateOf<List<com.intu.taxi.repositories.ScheduledRide>>(emptyList()) }
    LaunchedEffect(isDriver, reloadKey) {
        scheduled = if (isDriver) emptyList() else runCatching { scheduledRepo.list() }.getOrDefault(scheduled)
    }
    fun cancelScheduled(ride: com.intu.taxi.repositories.ScheduledRide) {
        scope.launch {
            runCatching { scheduledRepo.cancel(ride.id) }
                .onSuccess {
                    scheduled = scheduled.filterNot { it.id == ride.id }
                    if (ride.status == "scheduled") Toast.makeText(context, "Viaje programado cancelado", Toast.LENGTH_SHORT).show()
                }
                .onFailure { Toast.makeText(context, it.message ?: "No se pudo cancelar", Toast.LENGTH_LONG).show() }
        }
    }

    TripsContent(padding, isDriver, rides, loadError, driverRating,
        onRefresh = { reloadKey++ }, onRate = ::rate, onDetails = { selectedRide = it },
        scheduled = scheduled, onCancelScheduled = ::cancelScheduled)
}

/** Viaje programado: cuándo, a dónde y para quién; o por qué no se pudo iniciar. */
@Composable
private fun ScheduledRideCard(ride: com.intu.taxi.repositories.ScheduledRide, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val failed = ride.status == "failed"
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth().testTag("scheduled-ride-${ride.id}")) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(com.intu.taxi.repositories.ScheduleWindow.label(ride.scheduledFor), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(ride.destinationAddress, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(listOfNotNull(ride.passengerName?.let { "Para $it" }, com.intu.taxi.ui.formatSoles(ride.fare)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(if (failed) when (ride.failureReason) {
                        "open_ride" -> "No se inició: tenías otro viaje en curso."
                        "cancellation_block" -> "No se inició: tu cuenta estaba en pausa."
                        "expired" -> "No se inició a tiempo. Pide uno nuevo."
                        else -> "No se pudo iniciar. Pide el viaje desde Inicio."
                    } else "Buscaremos conductor ${com.intu.taxi.repositories.ScheduleWindow.DISPATCH_MINUTES_BEFORE} minutos antes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (failed) colors.error else colors.onSurfaceVariant)
            }
            androidx.compose.material3.TextButton(onClick = onCancel) { Text(if (failed) "Ocultar" else "Cancelar") }
        }
    }
}

/** Presentation shared by the live screen and isolated UI verification. */
@Composable
internal fun TripsContent(
    padding: PaddingValues,
    isDriver: Boolean,
    rides: List<RideHistoryItem>?,
    loadError: String?,
    driverRating: Pair<Double, Int>?,
    onRefresh: () -> Unit,
    onRate: (RideHistoryItem, Int) -> Unit,
    onDetails: (RideHistoryItem) -> Unit,
    scheduled: List<com.intu.taxi.repositories.ScheduledRide> = emptyList(),
    onCancelScheduled: (com.intu.taxi.repositories.ScheduledRide) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .intuPageBackground().testTag("trips-screen"),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 24.dp,
            bottom = padding.calculateBottomPadding() + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            IntuPageHeading(
                if (isDriver) "Mis servicios" else "Mis viajes",
                if (isDriver) "Tus ganancias y tu actividad con Intu." else "Cada viaje y envío, en un solo lugar."
            ) {
                FilledTonalIconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }
        }

        if (isDriver) {
            item { EarningsCard(rides.orEmpty(), driverRating) }
        }

        if (scheduled.isNotEmpty()) {
            item { Text("Programados", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant) }
            items(scheduled, key = { "scheduled-${it.id}" }) { ride ->
                ScheduledRideCard(ride, onCancel = { onCancelScheduled(ride) })
            }
        }

        val list = rides
        when {
            list == null -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
            loadError != null && list.isEmpty() -> item {
                MessageCard(
                    title = "No se pudieron cargar tus viajes",
                    body = loadError.orEmpty(),
                    actionLabel = "Reintentar",
                    onAction = onRefresh
                )
            }
            list.isEmpty() -> item {
                MessageCard(
                    title = if (isDriver) "Aún no tienes servicios" else "Aún no tienes viajes",
                    body = if (isDriver) {
                        "Pulsa \"Empezar ahora\" en Inicio para recibir solicitudes."
                    } else {
                        "Tus viajes y envíos aparecerán aquí cuando terminen."
                    }
                )
            }
            else -> {
                item {
                    Text("Historial · Toca un viaje para ver la ruta", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
                }
                items(list, key = { it.id }) { ride ->
                    RideHistoryCard(ride = ride, isDriver = isDriver, onRate = { stars -> onRate(ride, stars) }, onDetails = { onDetails(ride) })
                }
            }
        }
    }
}

/** Ganancias del conductor (solo viajes completados) y su calificación promedio. */
@Composable
private fun EarningsCard(rides: List<RideHistoryItem>, rating: Pair<Double, Int>?) {
    val (today, week, month) = RideHistoryRepository.earnings(rides)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().intuCardBackground(emphasized = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("Ganancias", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EarningsTile("Hoy", today, Modifier.weight(1f))
                EarningsTile("7 días", week, Modifier.weight(1f))
                EarningsTile("Este mes", month, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF5A524), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        rating == null || rating.second == 0 -> "Aún no tienes calificaciones"
                        else -> String.format(Locale.US, "%.2f", rating.first) +
                            " · ${rating.second} ${if (rating.second == 1) "calificación" else "calificaciones"}"
                    },
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Cuenta solo servicios completados. El pago lo recibes directamente de quien paga el transporte.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EarningsTile(label: String, summary: EarningsSummary, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(
            formatSoles(summary.total),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
        Text(
            "${summary.rides} ${if (summary.rides == 1) "viaje" else "viajes"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun RideHistoryCard(ride: RideHistoryItem, isDriver: Boolean, onRate: (Int) -> Unit, onDetails: () -> Unit) {
    val completed = ride.status == "completed"
    Card(
        onClick = onDetails,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().testTag("trip-${ride.id}").intuCardBackground()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ride.requestedAt?.atZone(ZoneId.systemDefault())?.format(DateLabel).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(completed, ride.serviceKind == "delivery")
            }
            if (ride.serviceKind == "delivery") Text("Envío · Moto lineal", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(10.dp))
            RoutePoint(Color(0xFF16A34A), ride.originAddress.ifBlank { "Punto de recojo" })
            Spacer(Modifier.height(4.dp))
            RoutePoint(Color(0xFFDC2626), ride.destinationAddress.ifBlank { "Destino" })
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    url = if (isDriver && ride.passenger != null) "" else if (isDriver) ride.riderPhotoUrl else ride.driverPhotoUrl,
                    size = 36.dp,
                    zoomable = true
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    ride.passenger?.let { Text(if (isDriver) "Solicitó: ${ride.riderName}" else "Para ${it.name}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    Text(
                        (if (isDriver) ride.passenger?.name ?: ride.riderName else ride.driverName)
                            .ifBlank { if (isDriver && ride.serviceKind == "delivery") "Quien envía" else if (isDriver) "Pasajero" else "Sin conductor" },
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isDriver && ride.vehiclePlate.isNotBlank()) {
                        Text(
                            "${if (ride.serviceKind == "delivery") "Moto lineal" else "Mototaxi"} ${listOf(ride.vehicleDescription, ride.vehiclePlate).filter { it.isNotBlank() }.joinToString(" · ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatSoles(ride.fare),
                        fontWeight = FontWeight.Bold,
                        color = if (completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (ride.paymentMethod == "yape_plin") "Yape" else "Efectivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (completed) {
                val myRating = if (isDriver) ride.ratingForRider else ride.ratingForDriver
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            myRating != null -> "Tu calificación"
                            isDriver && (ride.serviceKind == "delivery" || ride.passenger != null) -> "Califica a quien solicitó"
                            isDriver -> "Califica al pasajero"
                            else -> "Califica a tu conductor"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    StarRating(
                        stars = myRating ?: 0,
                        size = 24.dp,
                        onRate = if (myRating == null) onRate else null
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(completed: Boolean, delivery: Boolean = false) {
    Text(
        if (completed && delivery) "Entregado" else if (completed) "Completado" else "Cancelado",
        style = MaterialTheme.typography.labelMedium,
        color = AppearanceColors.highlight(if (completed) Color(0xFF067647) else Color(0xFFB42318)),
        modifier = Modifier
            .background(
                AppearanceColors.tint(if (completed) Color(0xFFE8F6EE) else Color(0xFFFDECEA)),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun RoutePoint(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MessageCard(title: String, body: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().intuCardBackground()
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text(actionLabel)
                }
            }
        }
    }
}
