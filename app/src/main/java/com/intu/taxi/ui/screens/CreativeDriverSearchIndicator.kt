package com.intu.taxi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import kotlin.math.*
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun CreativeDriverSearchIndicator(
    isVisible: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val infiniteTransition = rememberInfiniteTransition(label = "creative_search")
    
    // Animaciones principales
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "radar_rotation"
    )
    
    // Pulso del círculo central
    val centerPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "center_pulse"
    )
    
    // Ondas de búsqueda
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 0),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 800),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )
    
    val wave3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1600),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )
    
    // Animación de flotación suave
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_animation"
    )
    
    // Brillo pulsatil
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    
    // Efecto de partículas
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "particle_rotation"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Fondo con gradiente mejorado y efecto de brillo más intenso
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00FFE0).copy(alpha = 0.15f * glowAlpha), // Color más brillante
                            Color(0xFF1E1F47).copy(alpha = 0.08f * glowAlpha), // Más intenso
                            Color.Transparent
                        ),
                        radius = 0.8f // Radio más grande para efecto más amplio
                    )
                )
        )
        
        // Efecto de brillo adicional más intenso
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f * glowAlpha), // Más visible
                            Color.Transparent,
                            Color.White.copy(alpha = 0.03f * glowAlpha) // Más visible
                        )
                    )
                )
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Contenedor principal con efecto glassmorphism mejorado y animación de flotación
            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .wrapContentSize()
                    .offset(y = floatY.dp + 150.dp) // Bajar el card ligeramente
                    .alpha(0.98f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1F47).copy(alpha = 0.85f) // Fondo más oscuro para mejor contraste
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF08817E).copy(alpha = 0.8f), // Color de acento más visible
                            Color.White.copy(alpha = 0.4f),
                            Color(0xFF08817E).copy(alpha = 0.8f)
                        )
                    )
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Canvas para el efecto de radar
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                            val maxRadius = min(size.width, size.height) / 2f
                            
                            // Círculos de fondo con mejor contraste
                            drawCircle(
                                color = Color(0xFF00FFE0).copy(alpha = 0.3f), // Color más brillante
                                center = center,
                                radius = maxRadius * 0.9f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            
                            drawCircle(
                                color = Color(0xFF00FFE0).copy(alpha = 0.4f), // Más visible
                                center = center,
                                radius = maxRadius * 0.6f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            
                            // Ondas de búsqueda con colores más brillantes
                            val waveRadius1 = maxRadius * 0.7f * (1f - wave1Alpha)
                            val waveRadius2 = maxRadius * 0.7f * (1f - wave2Alpha)
                            val waveRadius3 = maxRadius * 0.7f * (1f - wave3Alpha)
                            
                            drawCircle(
                                color = Color(0xFF00FFE0).copy(alpha = wave1Alpha * 0.7f), // Más visible
                                center = center,
                                radius = waveRadius1,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            drawCircle(
                                color = Color(0xFF00FFE0).copy(alpha = wave2Alpha * 0.7f), // Más visible
                                center = center,
                                radius = waveRadius2,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            drawCircle(
                                color = Color(0xFF00FFE0).copy(alpha = wave3Alpha * 0.7f), // Más visible
                                center = center,
                                radius = waveRadius3,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            // Radar sweep con mejor contraste
                            val radarAngle = Math.toRadians(radarRotation.toDouble())
                            val radarLength = maxRadius * 0.8f
                            
                            drawLine(
                                color = Color(0xFF00FFE0).copy(alpha = 1f), // Color brillante sin transparencia
                                start = center,
                                end = androidx.compose.ui.geometry.Offset(
                                    x = center.x + cos(radarAngle).toFloat() * radarLength,
                                    y = center.y + sin(radarAngle).toFloat() * radarLength
                                ),
                                strokeWidth = 4.dp.toPx(), // Más grueso
                                cap = StrokeCap.Round
                            )
                            
                            // Círculo central con pulso mejorado
                            val pulseRadius = maxRadius * 0.15f * centerPulse
                            drawCircle(
                                color = Color(0xFF00FFE0), // Color brillante
                                center = center,
                                radius = pulseRadius,
                                style = Fill
                            )
                            
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f), // Más opaco
                                center = center,
                                radius = pulseRadius * 0.6f,
                                style = Fill
                            )
                            
                            // Partículas alrededor más brillantes
                            val particleCount = 8
                            val particleAngle = Math.toRadians(particleOffset.toDouble())
                            val particleRadius = maxRadius * 0.85f
                            
                            for (i in 0 until particleCount) {
                                val angle = particleAngle + (i * Math.PI * 2 / particleCount)
                                val particleX = center.x + cos(angle).toFloat() * particleRadius
                                val particleY = center.y + sin(angle).toFloat() * particleRadius
                                
                                drawCircle(
                                    color = Color(0xFF00FFE0).copy(alpha = 0.8f), // Más brillante
                                    center = androidx.compose.ui.geometry.Offset(particleX, particleY),
                                    radius = 3.dp.toPx(), // Más grande
                                    style = Fill
                                )
                            }
                        }
                        
                        // Icono de búsqueda en el centro
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF08817E),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Texto principal
                    Text(
                        text = "Buscando conductor",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFFE8F8F7), // Color más brillante para mejor contraste
                        fontWeight = FontWeight.Bold, // Más peso para mejor legibilidad
                        fontSize = 24.sp // Tamaño aumentado
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Texto secundario con animación de puntos
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Escaneando área",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF88D8D5), // Color de acento más brillante
                            fontSize = 16.sp, // Tamaño aumentado
                            fontWeight = FontWeight.Medium // Peso medio para mejor legibilidad
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Puntos animados
                        Row {
                            for (i in 0..2) {
                                val dotAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = 1000,
                                            delayMillis = i * 200,
                                            easing = FastOutSlowInEasing
                                        ),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "text_dot_$i"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 1.dp)
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = dotAlpha))
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Botón de cancelar con estilo premium mejorado
                    Surface(
                        onClick = onCancel,
                        modifier = Modifier
                            .height(48.dp) // Más alto para mejor accesibilidad
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp), // Más redondeado
                        color = Color(0xFF08817E).copy(alpha = 0.4f), // Color de acento más visible
                        border = BorderStroke(
                            width = 2.dp, // Borde más grueso
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00FFE0).copy(alpha = 0.8f),
                                    Color.White.copy(alpha = 0.6f),
                                    Color(0xFF00FFE0).copy(alpha = 0.8f)
                                )
                            )
                        ),
                        shadowElevation = 4.dp // Sombra para mejor visibilidad
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(6.dp))
                            
                            Text(
                                text = "Cancelar búsqueda",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE8F8F7), // Color más brillante
                                fontWeight = FontWeight.SemiBold, // Más peso
                                fontSize = 16.sp // Tamaño aumentado
                            )
                        }
                    }
                }
            }
            
            
        }
    }
}