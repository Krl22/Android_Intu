package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import android.view.Gravity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.doOnLayout
import com.intu.taxi.R
import com.intu.taxi.repositories.RideHistoryItem
import com.intu.taxi.ui.formatSoles
import com.intu.taxi.ui.map.TripMap
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.intu.taxi.ui.map.createIntuMapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DetailTeal = Color(0xFF08817E)
private val DetailIndigo = Color(0xFF1E1F47)
private val DetailColors = lightColorScheme(
    primary = DetailTeal, onPrimary = Color.White,
    surface = Color.White, onSurface = Color(0xFF183236),
    background = Color(0xFFF4F8F7), onBackground = Color(0xFF183236),
    surfaceVariant = Color(0xFFEDF4F3), onSurfaceVariant = Color(0xFF637579),
    outlineVariant = Color(0xFFDCE8E6)
)
private val DetailDate = DateTimeFormatter.ofPattern("d 'de' MMMM · HH:mm", Locale("es", "PE"))

/** Guardar y volver a dibujar la geometría evita consultar Directions para cada visita al historial. */
fun storedRoutePoints(geometry: String?): List<Point> = runCatching {
    val points = LineString.fromJson(geometry ?: return emptyList()).coordinates()
    if (points.size < 2 || points.any { !it.latitude().isFinite() || !it.longitude().isFinite() || it.latitude() !in -90.0..90.0 || it.longitude() !in -180.0..180.0 }) emptyList() else points
}.getOrDefault(emptyList())

@Composable
fun RideDetailsDialog(ride: RideHistoryItem, cache: MutableMap<String, List<Point>>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var points by remember(ride.id) { mutableStateOf<List<Point>?>(cache[ride.id]) }
    var error by remember(ride.id) { mutableStateOf<String?>(null) }
    var mapReady by remember(ride.id) { mutableStateOf(false) }
    LaunchedEffect(ride.id) {
        if (points != null) return@LaunchedEffect
        var route = storedRoutePoints(ride.routeGeometry)
        if (route.isEmpty()) {
            val origin = validHistoryPoint(ride.originLongitude, ride.originLatitude)
            val destination = validHistoryPoint(ride.destinationLongitude, ride.destinationLatitude)
            if (origin != null && destination != null) {
                route = TripMap.fetchRoute(context.getString(R.string.mapbox_access_token), origin, destination)?.points.orEmpty()
            }
        }
        if (route.isEmpty()) error = "La ruta no está disponible. Puedes consultar las direcciones del viaje."
        else cache[ride.id] = route
        points = route
    }
    val mapStyle = com.intu.taxi.ui.theme.intuMapStyle()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MaterialTheme(colorScheme = if (com.intu.taxi.ui.theme.LocalIntuDarkMode.current) MaterialTheme.colorScheme else DetailColors) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight * 0.92f),
                    shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.background,
                    shadowElevation = 16.dp) {
                    Column {
                        Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(DetailTeal, DetailIndigo)))
                            .padding(start = 22.dp, top = 18.dp, end = 10.dp, bottom = 22.dp),
                            verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (ride.serviceKind == "delivery") "TU ENVÍO" else "TU VIAJE", style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.76f), fontWeight = FontWeight.SemiBold)
                                Text(if (ride.serviceKind == "delivery") "Detalle del envío" else "Detalle del viaje", style = MaterialTheme.typography.titleLarge,
                                    color = Color.White, fontWeight = FontWeight.Bold)
                                ride.requestedAt?.let {
                                    Text(DetailDate.format(it.atZone(ZoneId.systemDefault())),
                                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.88f))
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Outlined.Close, contentDescription = "Cerrar detalle del viaje", tint = Color.White)
                            }
                        }
                        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ride.delivery?.takeIf { it.businessItems.isNotEmpty() }?.let { details ->
                                DetailCard {
                                    Text("DEMO · ${details.businessName.orEmpty()}", color = MaterialTheme.colorScheme.primary)
                                    BusinessCartSummary(details.businessItems)
                                    Text("Productos simulados, sin cobro. La tarifa corresponde al transporte.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DetailCard {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (ride.serviceKind == "delivery") "Tarifa del transporte" else "Tarifa del viaje", style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val completed = ride.status == "completed"
                                    Surface(shape = RoundedCornerShape(50),
                                        color = AppearanceColors.tint(if (completed) Color(0xFFE8F6EE) else Color(0xFFFDECEA))) {
                                        Text(if (completed && ride.serviceKind == "delivery") "Entregado" else if (completed) "Completado" else "Cancelado",
                                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AppearanceColors.highlight(if (completed) Color(0xFF067647) else Color(0xFFB42318)))
                                    }
                                }
                                Text(formatSoles(ride.fare), style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold, color = AppearanceColors.foreground(DetailIndigo))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Icon(if (ride.paymentMethod == "yape_plin") Icons.Outlined.Smartphone else Icons.Outlined.Payments,
                                        contentDescription = null, tint = AppearanceColors.highlight(DetailTeal), modifier = Modifier.size(18.dp))
                                    Text(if (ride.paymentMethod == "yape_plin") "Yape" else "Efectivo",
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            DetailCard {
                                Text("Recorrido", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                DetailAddress("Punto de recojo", ride.originAddress, Icons.Outlined.LocationOn, Color(0xFF16845D))
                                Box(Modifier.padding(start = 19.dp).width(2.dp).height(14.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                                DetailAddress("Destino", ride.destinationAddress, Icons.Outlined.Flag, Color(0xFFBF454C))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DetailMetric("Distancia estimada", "${String.format(Locale.US, "%.1f", ride.distanceMeters / 1000.0)} km",
                                    Icons.Outlined.Route, Modifier.weight(1f))
                                DetailMetric("Tiempo estimado", "${ride.durationSeconds / 60} min",
                                    Icons.Outlined.Schedule, Modifier.weight(1f))
                            }
                            val route = points.orEmpty()
                            DetailCard {
                                Text("Ruta planificada", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (points == null) {
                                    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                        Text("Cargando la ruta…", style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (error != null) {
                                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                        .padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Outlined.Info, contentDescription = null, tint = AppearanceColors.highlight(DetailTeal), modifier = Modifier.size(20.dp))
                                        Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (route.size > 1) {
                                    AndroidView(
                                        modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp))
                                            .testTag(if (mapReady) "trip-map-ready" else "trip-map-loading"),
                                        factory = { mapContext -> createIntuMapView(mapContext).apply {
                                            val ornamentMargin = 8 * resources.displayMetrics.density
                                            // Mapbox requires the wordmark and attribution to remain visible on this map.
                                            logo.updateSettings {
                                                position = Gravity.BOTTOM or Gravity.RIGHT
                                                marginRight = ornamentMargin
                                                marginBottom = ornamentMargin
                                            }
                                            attribution.updateSettings {
                                                position = Gravity.BOTTOM or Gravity.LEFT
                                                marginLeft = ornamentMargin
                                                marginBottom = ornamentMargin
                                            }
                                            mapboxMap.subscribeMapLoaded { mapReady = true }
                                            mapboxMap.loadStyleUri(mapStyle) {
                                                annotations.createPolylineAnnotationManager().create(PolylineAnnotationOptions()
                                                    .withPoints(route).withLineColor("#08817E").withLineWidth(5.0))
                                                val markers = annotations.createCircleAnnotationManager()
                                                markers.create(CircleAnnotationOptions().withPoint(route.first()).withCircleColor("#16A34A").withCircleRadius(7.0))
                                                markers.create(CircleAnnotationOptions().withPoint(route.last()).withCircleColor("#DC2626").withCircleRadius(7.0))
                                                doOnLayout {
                                                    TripMap.fitCamera(this, route, EdgeInsets(35.0, 35.0, 35.0, 35.0), animate = false)
                                                }
                                            }
                                        } },
                                        onRelease = { it.onDestroy() }
                                    )
                                    if (!mapReady) Text("Cargando el mapa…", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("La línea muestra la ruta planificada entre los puntos del viaje. No es un registro del recorrido GPS realizado.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Box(Modifier.fillMaxWidth().background(AppearanceColors.surface).padding(horizontal = 18.dp, vertical = 14.dp)) {
                            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                shape = RoundedCornerShape(16.dp)) {
                                Text("Cerrar", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = AppearanceColors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun DetailAddress(label: String, address: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).background(color.copy(alpha = 0.09f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(address.ifBlank { "Sin dirección" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = DetailTeal.copy(alpha = 0.06f)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = AppearanceColors.highlight(DetailTeal), modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppearanceColors.foreground(DetailIndigo))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun validHistoryPoint(lng: Double?, lat: Double?): Point? =
    if (lng != null && lat != null && lng.isFinite() && lat.isFinite() && lng in -180.0..180.0 && lat in -90.0..90.0) Point.fromLngLat(lng, lat) else null
