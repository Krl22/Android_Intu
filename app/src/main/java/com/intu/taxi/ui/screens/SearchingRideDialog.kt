package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search

@Composable
fun SearchingRideDialog(
    isVisible: Boolean,
    rideType: String,
    originAddress: String,
    destinationAddress: String,
    estimatedPrice: Double,
    estimatedTime: Double,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    var animationPhase by remember { mutableStateOf(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "searching_animation")
    
    // Animación de pulso para el círculo
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Animación de rotación para los puntos
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing)
        ),
        label = "rotation_angle"
    )
    
    // Animación de parpadeo para el texto
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_alpha"
    )

    Dialog(
        onDismissRequest = { /* No permitir dismiss tocando fuera */ },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título con icono de búsqueda
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Círculo de fondo con gradiente
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF3498DB),
                                Color(0xFF2980B9),
                                Color(0xFF3498DB)
                            ),
                            center = Offset(size.width / 2, size.height / 2)
                        )
                        
                        drawCircle(
                            brush = brush,
                            radius = size.minDimension / 2,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    
                    // Icono de búsqueda con animación
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscando",
                        modifier = Modifier
                            .size(32.dp)
                            .scale(pulseScale)
                            .alpha(textAlpha),
                        tint = Color(0xFF3498DB)
                    )
                }
                
                // Título
                Text(
                    text = "Buscando conductor",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                )
                
                // Tipo de viaje
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Origen",
                                tint = Color(0xFF27AE60),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = originAddress,
                                fontSize = 14.sp,
                                color = Color(0xFF2C3E50),
                                maxLines = 1
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Destino",
                                tint = Color(0xFFE74C3C),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = destinationAddress,
                                fontSize = 14.sp,
                                color = Color(0xFF2C3E50),
                                maxLines = 1
                            )
                        }
                        
                        Divider(color = Color(0xFFE5E5EA))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Tipo",
                                    fontSize = 12.sp,
                                    color = Color(0xFF7F8C8D)
                                )
                                Text(
                                    text = rideType,
                                    fontSize = 14.sp,
                                    color = Color(0xFF2C3E50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Column {
                                Text(
                                    text = "Precio estimado",
                                    fontSize = 12.sp,
                                    color = Color(0xFF7F8C8D)
                                )
                                Text(
                                    text = com.intu.taxi.ui.formatSoles(estimatedPrice),
                                    fontSize = 14.sp,
                                    color = Color(0xFF27AE60),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                // Tiempo estimado
                Text(
                    text = "Tiempo estimado: ${String.format("%.0f", estimatedTime)} min",
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D),
                    textAlign = TextAlign.Center
                )
                
                // Indicador de búsqueda animado
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val dotCount = 3
                    repeat(dotCount) { index ->
                        val delay = index * 200
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, delayMillis = delay),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot_$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3498DB).copy(alpha = alpha))
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Botón cancelar
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE74C3C)
                    )
                ) {
                    Text(
                        text = "Cancelar búsqueda",
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
