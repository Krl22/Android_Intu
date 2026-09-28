package com.intu.taxi.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.intu.taxi.R

/**
 * Enhanced Trips Screen with modern design principles:
 * - Glassmorphism effects
 * - Smooth animations and transitions
 * - Better visual hierarchy
 * - Consistent color scheme
 * - Improved typography
 * - Interactive elements
 */

// Enhanced data classes with more properties
sealed class TripItem {
    data class ClientTrip(
        val id: String,
        val driverName: String,
        val driverPhoto: String? = null,
        val plate: String,
        val price: Double,
        val currency: String = "$",
        val rating: Float,
        val date: String,
        val from: String,
        val to: String,
        val distance: String,
        val duration: String,
        val status: TripStatus = TripStatus.COMPLETED
    ) : TripItem()
    
    data class DriverTrip(
        val id: String,
        val passengerName: String,
        val passengerPhoto: String? = null,
        val pickup: String,
        val dropoff: String,
        val fare: Double,
        val currency: String = "$",
        val status: TripStatus,
        val eta: String,
        val distance: String,
        val duration: String
    ) : TripItem()
}

enum class TripStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED
}

// Enhanced sample data
private val enhancedClientTrips = listOf(
    TripItem.ClientTrip(
        id = "1",
        driverName = "Carlos Rodriguez",
        plate = "ABC-123",
        price = 12.50,
        rating = 4.8f,
        date = "Hoy, 10:20 AM",
        from = "Intu Plaza",
        to = "Av. Central 120",
        distance = "8.2 km",
        duration = "15 min"
    ),
    TripItem.ClientTrip(
        id = "2", 
        driverName = "Maria Gonzalez",
        plate = "XYZ-456",
        price = 15.00,
        rating = 4.7f,
        date = "Ayer, 7:05 PM",
        from = "Mercado 9",
        to = "Intu Mall",
        distance = "6.5 km",
        duration = "12 min"
    ),
    TripItem.ClientTrip(
        id = "3",
        driverName = "Juan Perez", 
        plate = "DEF-789",
        price = 11.80,
        rating = 4.6f,
        date = "Ayer, 1:40 PM",
        from = "Parque Sur",
        to = "Estación Norte",
        distance = "9.1 km",
        duration = "18 min"
    )
)

private val enhancedDriverTrips = listOf(
    TripItem.DriverTrip(
        id = "1",
        passengerName = "Ana Patricia",
        pickup = "Intu Plaza",
        dropoff = "Av. Centro 77",
        fare = 4.20,
        status = TripStatus.COMPLETED,
        eta = "—",
        distance = "3.2 km",
        duration = "8 min"
    ),
    TripItem.DriverTrip(
        id = "2",
        passengerName = "Luis Roberto",
        pickup = "Parque Norte",
        dropoff = "Clínica Central",
        fare = 3.60,
        status = TripStatus.IN_PROGRESS,
        eta = "5 min",
        distance = "2.8 km",
        duration = "6 min"
    ),
    TripItem.DriverTrip(
        id = "3",
        passengerName = "Maria Teresa",
        pickup = "Mercado 9",
        dropoff = "Intu Mall",
        fare = 5.10,
        status = TripStatus.PENDING,
        eta = "8 min",
        distance = "4.5 km",
        duration = "10 min"
    )
)

// Enhanced earnings data
private val earningsData = mapOf(
    "Día" to listOf(
        "08:00" to 5.8f, "09:00" to 12.3f, "10:00" to 9.4f,
        "11:00" to 7.1f, "12:00" to 15.2f, "13:00" to 11.0f,
        "14:00" to 8.5f, "15:00" to 13.7f, "16:00" to 10.2f
    ),
    "Semana" to listOf(
        "Lun" to 52.5f, "Mar" to 68.3f, "Mié" to 47.9f,
        "Jue" to 72.6f, "Vie" to 81.2f, "Sáb" to 95.4f, "Dom" to 63.0f
    ),
    "Mes" to listOf(
        "Sem 1" to 280f, "Sem 2" to 310f, "Sem 3" to 265f, "Sem 4" to 325f
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreenEnhanced(
    padding: PaddingValues,
    isDriver: Boolean
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedTimeRange by remember { mutableStateOf("Día") }
    
    // Animation states
    var headerVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        headerVisible = true
        kotlinx.coroutines.delay(200)
        contentVisible = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08817E).copy(alpha = 0.1f), // teal
                        Color(0xFF1E1F47).copy(alpha = 0.05f), // indigo
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Enhanced Header
            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
            ) {
                EnhancedTripsHeader(isDriver = isDriver)
            }
            
            // Content
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                if (isDriver) {
                    DriverContent(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        selectedTimeRange = selectedTimeRange,
                        onTimeRangeSelected = { selectedTimeRange = it }
                    )
                } else {
                    ClientContent()
                }
            }
        }
    }
}

@Composable
private fun EnhancedTripsHeader(isDriver: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isDriver) "Mis Servicios" else "Mis Viajes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = "${if (isDriver) "12 servicios" else "3 viajes"} este mes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF08817E), // teal
                                Color(0xFF1E1F47)  // indigo
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDriver) Icons.Default.DirectionsCar else Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun ClientContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(enhancedClientTrips) { trip ->
            EnhancedClientTripCard(trip as TripItem.ClientTrip)
        }
    }
}

@Composable
private fun DriverContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    selectedTimeRange: String,
    onTimeRangeSelected: (String) -> Unit
) {
    Column {
        // Enhanced Tab Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.1f)
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF08817E), // teal
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF08817E), Color(0xFF1E1F47)) // teal to indigo
                                ),
                                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    text = {
                        Text(
                            "Servicios",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    text = {
                        Text(
                            "Dashboard",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (selectedTab) {
            0 -> DriverServicesTab()
            1 -> DriverDashboardTab(selectedTimeRange, onTimeRangeSelected)
        }
    }
}

@Composable
private fun DriverServicesTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(enhancedDriverTrips) { trip ->
            EnhancedDriverTripCard(trip as TripItem.DriverTrip)
        }
    }
}

@Composable
private fun DriverDashboardTab(
    selectedTimeRange: String,
    onTimeRangeSelected: (String) -> Unit
) {
    val timeRanges = listOf("Día", "Semana", "Mes")
    val currentData = earningsData[selectedTimeRange] ?: emptyList()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time Range Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                timeRanges.forEach { range ->
                    Surface(
                        selected = selectedTimeRange == range,
                        onClick = { onTimeRangeSelected(range) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTimeRange == range) {
                            Color(0xFF667eea).copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Text(
                            text = range,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = if (selectedTimeRange == range) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTimeRange == range) Color(0xFF08817E) else Color(0xFF6B7280) // teal
                        )
                    }
                }
            }
        }
        
        // Summary Cards
        DriverSummaryCards(selectedTimeRange)
        
        // Earnings Chart
        EnhancedEarningsChart(currentData, selectedTimeRange)
        
        // Trip Status Distribution
        TripStatusDistribution(selectedTimeRange)
    }
}

@Composable
private fun DriverSummaryCards(timeRange: String) {
    val summaryData = when (timeRange) {
        "Día" -> listOf("Viajes" to "8", "Ganancias" to "S/ 62.30", "Horas" to "4h 15m", "Promedio" to "S/ 7.79")
        "Semana" -> listOf("Viajes" to "42", "Ganancias" to "S/ 310.80", "Horas" to "26h", "Promedio" to "S/ 7.40")
        else -> listOf("Viajes" to "180", "Ganancias" to "S/ 1,280.00", "Horas" to "110h", "Promedio" to "S/ 7.11")
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        summaryData.take(2).forEach { (label, value) ->
            SummaryCard(
                label = label,
                value = value,
                modifier = Modifier.weight(1f),
                icon = when (label) {
                    "Viajes" -> Icons.Default.DirectionsCar
                    "Ganancias" -> Icons.Default.AttachMoney
                    "Horas" -> Icons.Default.AccessTime
                    else -> Icons.Default.TrendingUp
                }
            )
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        summaryData.takeLast(2).forEach { (label, value) ->
            SummaryCard(
                label = label,
                value = value,
                modifier = Modifier.weight(1f),
                icon = when (label) {
                    "Viajes" -> Icons.Default.DirectionsCar
                    "Ganancias" -> Icons.Default.AttachMoney
                    "Horas" -> Icons.Default.AccessTime
                    else -> Icons.Default.TrendingUp
                }
            )
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF08817E).copy(alpha = 0.1f)), // teal
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF08817E), // teal
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun EnhancedEarningsChart(data: List<Pair<String, Float>>, timeRange: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ingresos por ${timeRange.lowercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Total: ${com.intu.taxi.ui.formatSoles(data.sumOf { it.second.toDouble() })}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF08817E), // teal
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val maxValue = data.maxOf { it.second }
            
            data.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        modifier = Modifier.width(60.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((value / maxValue).coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF08817E), Color(0xFF1E1F47)) // teal to indigo
                                    )
                                )
                                .animateContentSize()
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        com.intu.taxi.ui.formatSoles(value.toDouble()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1C1C1E)
                    )
                }
            }
        }
    }
}

@Composable
private fun TripStatusDistribution(timeRange: String) {
    val statusData = when (timeRange) {
        "Día" -> listOf(
            Triple("Completado", 5, Color(0xFF08817E)), // teal
            Triple("En curso", 2, Color(0xFF1E1F47)), // indigo
            Triple("Pendiente", 1, Color(0xFF08817E)) // teal
        )
        "Semana" -> listOf(
            Triple("Completado", 32, Color(0xFF08817E)), // teal
            Triple("En curso", 7, Color(0xFF1E1F47)), // indigo
            Triple("Pendiente", 3, Color(0xFF08817E)) // teal
        )
        else -> listOf(
            Triple("Completado", 145, Color(0xFF08817E)), // teal
            Triple("En curso", 25, Color(0xFF1E1F47)), // indigo
            Triple("Pendiente", 10, Color(0xFF08817E)) // teal
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Distribución de servicios",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val total = statusData.sumOf { it.second }
            
            statusData.forEach { (status, count, color) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        status,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                    
                    Text(
                        "$count (${"%.1f".format(count * 100f / total)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1C1C1E)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedClientTripCard(trip: TripItem.ClientTrip) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (expanded) 12.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with driver info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Driver avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF08817E), // teal
                                    Color(0xFF1E1F47)  // indigo
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        trip.driverName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        "Placa: ${trip.plate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
                
                // Price
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "${trip.currency} ${"%.2f".format(trip.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.StarHalf,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            "${trip.rating}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Trip details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TripDetailItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Fecha",
                    value = trip.date
                )
                TripDetailItem(
                    icon = Icons.Default.SocialDistance,
                    label = "Distancia",
                    value = trip.distance
                )
                TripDetailItem(
                    icon = Icons.Default.AccessTime,
                    label = "Duración",
                    value = trip.duration
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Route
            EnhancedRouteDisplay(trip.from, trip.to)
            
            // Expanded content
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFE5E7EB))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { /* Repeat trip */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF08817E), // teal
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Repetir viaje")
                        }
                        
                        OutlinedButton(
                            onClick = { /* Report issue */ },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Report,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reportar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedDriverTripCard(trip: TripItem.DriverTrip) {
    var expanded by remember { mutableStateOf(false) }
    
    val statusColor = when (trip.status) {
        TripStatus.PENDING -> Color(0xFF08817E) // teal
        TripStatus.IN_PROGRESS -> Color(0xFF1E1F47) // indigo
        TripStatus.COMPLETED -> Color(0xFF08817E) // teal
        TripStatus.CANCELLED -> Color(0xFF6B7280) // gray
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (expanded) 12.dp else 4.dp
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with passenger info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Passenger avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        trip.passengerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            trip.status.name.replace("_", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "${trip.currency} ${"%.2f".format(trip.fare)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    if (trip.status != TripStatus.COMPLETED) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                trip.eta,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Trip details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TripDetailItem(
                    icon = Icons.Default.SocialDistance,
                    label = "Distancia",
                    value = trip.distance
                )
                TripDetailItem(
                    icon = Icons.Default.AccessTime,
                    label = "Duración",
                    value = trip.duration
                )
                TripDetailItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Estado",
                    value = trip.status.name.replace("_", " ")
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Route
            EnhancedRouteDisplay(trip.pickup, trip.dropoff)
            
            // Expanded content
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFE5E7EB))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (trip.status) {
                            TripStatus.PENDING -> {
                                Button(
                                    onClick = { /* Accept trip */ },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF08817E) // teal
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Aceptar")
                                }
                                
                                OutlinedButton(
                                    onClick = { /* Reject trip */ },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rechazar")
                                }
                            }
                            TripStatus.IN_PROGRESS -> {
                                Button(
                                    onClick = { /* Complete trip */ },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E1F47) // indigo
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Completar viaje")
                                }
                            }
                            TripStatus.COMPLETED -> {
                                OutlinedButton(
                                    onClick = { /* View details */ },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver detalles")
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripDetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF667eea),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1C1C1E)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun EnhancedRouteDisplay(from: String, to: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF08817E)) // teal
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    from,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Box(
                modifier = Modifier
                    .padding(start = 3.dp)
                    .width(2.dp)
                    .height(8.dp)
                    .background(Color(0xFFE5E7EB))
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6B7280)) // gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    to,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}