package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// La pantalla usa el modo conductor global (proporcionado por la AccountScreen)

// Datos ficticios
data class Trip(val driver: String, val plate: String, val price: String, val rating: String, val date: String, val from: String, val to: String)
data class DriverJob(val passenger: String, val pickup: String, val dropoff: String, val fare: String, val status: String, val eta: String)

private val sampleClientTrips = listOf(
    Trip("Nofer Ravro", "R0G017", "$12.50", "4.8", "Hoy, 10:20", "Intu Plaza", "Av. Central 120"),
    Trip("Stanium", "ENS-017", "$15.00", "4.7", "Ayer, 19:05", "Mercado 9", "Intu Mall"),
    Trip("Nesmium", "ENS-019", "$11.80", "4.6", "Ayer, 13:40", "Parque Sur", "Estación Norte")
)

private val sampleDriverSummary = mapOf(
    "Viajes" to 8,
    "Ganancias" to "$62.30",
    "Horas" to "4h 15m"
)

private val sampleDriverJobs = listOf(
    DriverJob("Ana P.", "Intu Plaza", "Av. Centro 77", "$4.20", "Completado", "—"),
    DriverJob("Luis R.", "Parque Norte", "Clínica Central", "$3.60", "En curso", "5 min"),
    DriverJob("María T.", "Mercado 9", "Intu Mall", "$5.10", "Pendiente", "8 min")
)

// Datos ficticios para dashboard
private val sampleEarningsByHour = listOf(
    "08" to 5.8f,
    "09" to 12.3f,
    "10" to 9.4f,
    "11" to 7.1f,
    "12" to 15.2f,
    "13" to 11.0f
)
private val sampleJobsByType = listOf(
    Triple("Completado", 12, Color(0xFF10B981)),
    Triple("En curso", 5, Color(0xFF3B82F6)),
    Triple("Pendiente", 3, Color(0xFFF59E0B))
)

private val sampleEarningsByDay = listOf(
    "Lun" to 52.5f,
    "Mar" to 68.3f,
    "Mié" to 47.9f,
    "Jue" to 72.6f,
    "Vie" to 81.2f,
    "Sáb" to 95.4f,
    "Dom" to 63.0f
)

private val sampleEarningsByWeek = listOf(
    "Sem 1" to 280f,
    "Sem 2" to 310f,
    "Sem 3" to 265f,
    "Sem 4" to 325f
)

@Composable
fun TripsScreen(padding: PaddingValues, isDriver: Boolean) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {
        

        if (!isDriver) {
            // Vista Cliente: lista de viajes recientes con rating y detalles
            Text("Tus viajes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sampleClientTrips) { trip ->
                    ClientTripCard(trip)
                }
            }
        } else {
            // Vista Taxista: Tabs (Servicios, Dashboard)
            var driverTabIndex by rememberSaveable { mutableStateOf(0) }
            TabRow(selectedTabIndex = driverTabIndex) {
                Tab(
                    selected = driverTabIndex == 0,
                    onClick = { driverTabIndex = 0 },
                    text = { Text("Servicios") }
                )
                Tab(
                    selected = driverTabIndex == 1,
                    onClick = { driverTabIndex = 1 },
                    text = { Text("Dashboard") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (driverTabIndex == 0) {
                // Tab Servicios: lista de servicios
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sampleDriverJobs) { job ->
                        DriverJobCard(job)
                    }
                }
            } else {
                // Tab Dashboard: resumen y gráficos simples
                DriverDashboard()
            }
        }
    }
}

@Composable
private fun DriverDashboard() {
    var rangeIndex by rememberSaveable { mutableStateOf(0) }

    val rangeLabels = listOf("Día", "Semana", "Mes")
    val summaryData = when (rangeIndex) {
        0 -> mapOf("Viajes" to 8, "Ganancias" to "$62.30", "Horas" to "4h 15m")
        1 -> mapOf("Viajes" to 42, "Ganancias" to "$310.80", "Horas" to "26h")
        else -> mapOf("Viajes" to 180, "Ganancias" to "$1,280.00", "Horas" to "110h")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Navegación con flechas entre rangos (sin tabs)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { rangeIndex = (rangeIndex - 1 + rangeLabels.size) % rangeLabels.size }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Anterior")
            }
            Text("Resumen ${rangeLabels[rangeIndex].lowercase()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { rangeIndex = (rangeIndex + 1) % rangeLabels.size }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Siguiente")
            }
        }
        DriverSummaryRow(summary = summaryData)

        // Gráfico de barras: Ingresos por hora (hoy)
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                val title = when (rangeIndex) {
                    0 -> "Ingresos por hora (hoy)"
                    1 -> "Ingresos por día (semana)"
                    else -> "Ingresos por semana (mes)"
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val series = when (rangeIndex) {
                    0 -> sampleEarningsByHour
                    1 -> sampleEarningsByDay
                    else -> sampleEarningsByWeek
                }
                val maxAmount = series.maxOf { it.second }
                series.forEach { (label, amount) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, modifier = Modifier.width(60.dp), color = Color(0xFF6E6E73))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE5E7EB))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((amount / maxAmount).coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF4CA1AF), Color(0xFF2C3E50))
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$" + String.format("%.2f", amount))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // Gráfico de barras: Servicios por estado
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Servicios por estado", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val jobsSeries = when (rangeIndex) {
                    0 -> sampleJobsByType
                    1 -> listOf(
                        Triple("Completado", 65, Color(0xFF10B981)),
                        Triple("En curso", 14, Color(0xFF3B82F6)),
                        Triple("Pendiente", 9, Color(0xFFF59E0B))
                    )
                    else -> listOf(
                        Triple("Completado", 240, Color(0xFF10B981)),
                        Triple("En curso", 22, Color(0xFF3B82F6)),
                        Triple("Pendiente", 20, Color(0xFFF59E0B))
                    )
                }
                val maxCount = jobsSeries.maxOf { it.second }
                jobsSeries.forEach { (label, count, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, modifier = Modifier.width(100.dp), color = Color(0xFF6E6E73))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE5E7EB))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((count.toFloat() / maxCount).coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(count.toString())
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ClientTripCard(trip: Trip) {
    ElevatedCard(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1C1C1E))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(trip.driver, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Placa ${trip.plate}", color = Color(0xFF6E6E73))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(trip.rating)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(trip.date, color = Color(0xFF6E6E73))
            Spacer(modifier = Modifier.height(6.dp))
            Text("De: ${trip.from}")
            Text("A: ${trip.to}")
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Total ${trip.price}", fontWeight = FontWeight.SemiBold)
                // Botón para repetir viaje (demo)
                OutlinedButton(onClick = { /* TODO: repetir viaje */ }, shape = RoundedCornerShape(20.dp)) {
                    Text("Volver a pedir")
                }
            }
        }
    }
}

@Composable
private fun DriverSummaryRow(summary: Map<String, Any> = sampleDriverSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        summary.forEach { (title, value) ->
            ElevatedCard(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
                    Text(title, color = Color(0xFF6E6E73))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DriverJobCard(job: DriverJob) {
    ElevatedCard(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1C1C1E))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.passenger, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${job.pickup} → ${job.dropoff}", color = Color(0xFF6E6E73))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF6E6E73))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(job.eta)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(job.status, color = Color(0xFF1C1C1E))
                Text(job.fare, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}