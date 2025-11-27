package com.intu.taxi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MinimalSearchIndicator(
    isVisible: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val infiniteTransition = rememberInfiniteTransition(label = "search_pulse")
    
    // Animación de pulso suave para el contenedor
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Animación de escala para el ícono
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Contenedor con efecto glassmorphism moderno, posicionado en la parte inferior
        Card(
            modifier = Modifier
                .padding(bottom = 32.dp)
                .padding(horizontal = 16.dp)
                .alpha(pulseAlpha),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp,
                pressedElevation = 20.dp
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Contenedor del ícono con animación
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(iconScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Círculo de fondo con gradiente
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF08817E).copy(alpha = 0.2f),
                                        Color(0xFF08817E).copy(alpha = 0.1f)
                                    )
                                )
                            )
                    )
                    
                    // Ícono principal
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF08817E),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Columna de texto con mejor estilo
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Buscando conductor",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1C1C1E),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Indicador de actividad más elegante
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "Buscando",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6E6E73),
                            fontSize = 13.sp
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Puntos animados más sutiles
                        Row {
                            for (i in 0..2) {
                                val dotAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 0.9f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = 800,
                                            delayMillis = i * 150,
                                            easing = FastOutSlowInEasing
                                        ),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "dot_$i"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 1.dp)
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF08817E).copy(alpha = dotAlpha))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Botón cancelar con mejor estilo
                Surface(
                    onClick = onCancel,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancelar",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6E6E73),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}