package com.intu.taxi.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intu.taxi.ui.theme.AppearanceColors
import com.intu.taxi.models.DriverRideRequest

@Composable
fun IncomingRideRequestCard(
    request: DriverRideRequest,
    currentLatitude: Double,
    currentLongitude: Double,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val distance = request.calculateDistanceFrom(currentLatitude, currentLongitude)
    val distanceInKm = distance / 1000.0
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppearanceColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header con información del pasajero
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(
                        url = request.userPhotoUrl,
                        size = 44.dp,
                        zoomable = true,
                        contentDescription = "Foto del pasajero"
                    )

                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.userName.ifBlank { if (request.isDelivery) "Quien envía" else "Pasajero" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppearanceColors.ink
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", distanceInKm)} km de distancia",
                            fontSize = 12.sp,
                            color = AppearanceColors.secondary(Color.Gray)
                        )
                    }
                    
                    // Método de pago
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(
                            color = AppearanceColors.tint(Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(8.dp)
                        ).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        PaymentIcons(
                            paymentMethod = request.paymentMethod,
                            size = 16.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (request.paymentMethod == "efectivo") "Efectivo" else "Yape",
                            fontSize = 12.sp,
                            color = AppearanceColors.secondary(Color.DarkGray)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (request.isDelivery) Text("Envío en moto lineal", fontWeight = FontWeight.SemiBold,
                    color = AppearanceColors.highlight(Color(0xFF08817E)), modifier = Modifier.padding(bottom = 8.dp))
                // Información del viaje
                Column {
                    // Origen
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Origen",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = request.originAddress,
                            fontSize = 14.sp,
                            color = AppearanceColors.secondary(Color.DarkGray),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Destino
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Destino",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = request.destinationAddress,
                            fontSize = 14.sp,
                            color = AppearanceColors.secondary(Color.DarkGray),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Información adicional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", request.distanceMeters / 1000.0)} km",
                        fontSize = 12.sp,
                        color = AppearanceColors.secondary(Color.Gray)
                    )
                    Text(
                        text = "${kotlin.math.max(1, kotlin.math.round(request.durationSeconds / 60).toInt())} min",
                        fontSize = 12.sp,
                        color = AppearanceColors.secondary(Color.Gray)
                    )
                    Text(
                        text = com.intu.taxi.ui.formatSoles(request.estimatedPrice),
                        fontSize = 12.sp,
                        color = AppearanceColors.highlight(Color(0xFF08817E)),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón Rechazar
                    Button(
                        onClick = {
                            isVisible = false
                            onDecline()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "Rechazar",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    // Botón Aceptar
                    Button(
                        onClick = {
                            isVisible = false
                            onAccept()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF08817E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "Aceptar",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
