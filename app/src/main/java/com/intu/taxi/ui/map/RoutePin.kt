package com.intu.taxi.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

object RoutePinStyle {
    val destinationColor = Color(0xFF453C86)
    val pickupColor = Color(0xFFBE6A25)
}

/** Open center preserves the route; a white halo and contrasting outline sit above it. */
@Composable
internal fun RoutePin(description: String, modifier: Modifier = Modifier, pickup: Boolean = false) {
    Canvas(modifier.semantics { contentDescription = description }) {
        val ink = if (pickup) RoutePinStyle.pickupColor else RoutePinStyle.destinationColor
        val w = size.width
        val h = size.height
        val tip = h - 3.dp.toPx()
        val pin = Path().apply {
            moveTo(w * .5f, tip)
            cubicTo(w * .44f, h * .83f, w * .13f, h * .53f, w * .13f, h * .35f)
            cubicTo(w * .13f, h * .01f, w * .87f, h * .01f, w * .87f, h * .35f)
            cubicTo(w * .87f, h * .53f, w * .56f, h * .83f, w * .5f, tip)
            close()
        }
        translate(top = 1.dp.toPx()) { drawPath(pin, Color.Black.copy(alpha = .16f),
            style = Stroke(8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        drawPath(pin, Color.White, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(pin, ink, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color.White, 4.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w * .5f, h * .35f))
        drawCircle(ink, 2.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w * .5f, h * .35f))
    }
}
