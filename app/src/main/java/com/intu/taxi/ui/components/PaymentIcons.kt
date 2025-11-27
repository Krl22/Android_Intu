package com.intu.taxi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PaymentIcons(
    paymentMethod: String,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    when (paymentMethod.lowercase()) {
        "efectivo" -> {
            CashIcon(size = size, modifier = modifier)
        }
        "yape", "plin", "yape/plin" -> {
            YapePlinIcon(size = size, modifier = modifier)
        }
        else -> {
            CashIcon(size = size, modifier = modifier)
        }
    }
}

@Composable
fun CashIcon(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        
        // Dibujar billetes de dinero apilados
        val billWidth = width * 0.7f
        val billHeight = height * 0.35f
        val cornerRadius = CornerRadius(4.dp.toPx())
        
        // Primer billete (más abajo)
        drawRoundRect(
            color = Color(0xFF4CAF50),
            topLeft = Offset((width - billWidth) / 2, height * 0.5f),
            size = Size(billWidth, billHeight),
            cornerRadius = cornerRadius
        )
        
        // Segundo billete (encima)
        drawRoundRect(
            color = Color(0xFF388E3C),
            topLeft = Offset((width - billWidth) / 2, height * 0.35f),
            size = Size(billWidth, billHeight),
            cornerRadius = cornerRadius
        )
        
        // Tercer billete (más arriba)
        drawRoundRect(
            color = Color(0xFF2E7D32),
            topLeft = Offset((width - billWidth) / 2, height * 0.2f),
            size = Size(billWidth, billHeight),
            cornerRadius = cornerRadius
        )
        
        // Agregar detalles a los billetes
        val detailColor = Color.White
        val centerX = width / 2
        
        // Detalles en el billete del medio
        drawCircle(
            color = detailColor,
            radius = 3.dp.toPx(),
            center = Offset(centerX, height * 0.5f)
        )
        
        // Líneas decorativas
        drawLine(
            color = detailColor,
            start = Offset(centerX - 6.dp.toPx(), height * 0.5f),
            end = Offset(centerX + 6.dp.toPx(), height * 0.5f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun YapePlinIcon(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        
        // Dibujar un estilo moderno de icono de pago digital
        val circleRadius = width * 0.35f
        val centerX = width / 2
        val centerY = height / 2
        
        // Círculo principal con gradiente
        drawCircle(
            color = Color(0xFF7B1FA2),
            radius = circleRadius,
            center = Offset(centerX, centerY)
        )
        
        // Círculo interno
        drawCircle(
            color = Color(0xFF9C27B0),
            radius = circleRadius * 0.7f,
            center = Offset(centerX, centerY)
        )
        
        // Centro brillante
        drawCircle(
            color = Color(0xFFE1BEE7),
            radius = circleRadius * 0.3f,
            center = Offset(centerX, centerY)
        )
        
        // Líneas de conexión que representan transmisión de datos
        val lineLength = circleRadius * 0.6f
        for (i in 0..3) {
            val angle = (i * 90f) * (Math.PI / 180f).toFloat()
            val startX = centerX + Math.cos(angle.toDouble()).toFloat() * (circleRadius + 4.dp.toPx())
            val startY = centerY + Math.sin(angle.toDouble()).toFloat() * (circleRadius + 4.dp.toPx())
            val endX = centerX + Math.cos(angle.toDouble()).toFloat() * (circleRadius + lineLength)
            val endY = centerY + Math.sin(angle.toDouble()).toFloat() * (circleRadius + lineLength)
            
            drawLine(
                color = Color(0xFF7B1FA2),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx()
            )
            
            // Punto en el extremo
            drawCircle(
                color = Color(0xFF9C27B0),
                radius = 2.dp.toPx(),
                center = Offset(endX, endY)
            )
        }
    }
}