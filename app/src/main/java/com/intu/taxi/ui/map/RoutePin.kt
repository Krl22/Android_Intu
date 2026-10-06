package com.intu.taxi.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

object RoutePinStyle {
    val destinationColor = Color(0xFF50D7C9)
    val pickupColor = Color(0xFFF5B86B)
    val headColor = Color(0xFF22253A)
    val anchorInset = 2.5.dp
}

/** Elevated circle (proposal B). The center of the white foot anchors the selected map point. */
@Composable
internal fun RoutePin(description: String, modifier: Modifier = Modifier, pickup: Boolean = false) {
    Canvas(modifier.semantics { contentDescription = description }) {
        val ink = if (pickup) RoutePinStyle.pickupColor else RoutePinStyle.destinationColor
        val radius = minOf(size.width, size.height) * .30f
        val head = Offset(size.width / 2, radius + 1.dp.toPx())
        val footRadius = RoutePinStyle.anchorInset.toPx()
        val foot = Offset(head.x, size.height - footRadius)
        val stemStart = Offset(head.x, head.y + radius - 1.dp.toPx())

        translate(top = 1.dp.toPx()) {
            drawLine(Color.Black.copy(alpha = .18f), stemStart, foot,
                strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Color.Black.copy(alpha = .18f), radius + 1.dp.toPx(), head)
        }
        drawLine(Color.White, stemStart, foot, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Color.White, footRadius, foot)
        drawCircle(Color.White, radius, head)
        drawCircle(RoutePinStyle.headColor, radius * .68f, head)
        drawCircle(ink, radius * .26f, head)
    }
}

/** The same pin as a bitmap for a map layer; anchor it at the bottom so the foot marks the point. */
fun routePinBitmap(context: android.content.Context, pickup: Boolean): android.graphics.Bitmap {
    val dp = context.resources.displayMetrics.density
    val size = 42 * dp
    val bitmap = android.graphics.Bitmap.createBitmap(size.toInt(), size.toInt(), android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { strokeCap = android.graphics.Paint.Cap.ROUND }
    val radius = size * .30f
    val headX = size / 2
    val headY = radius + 1 * dp
    val footRadius = RoutePinStyle.anchorInset.value * dp
    val footY = size - footRadius
    val stemStartY = headY + radius - 1 * dp

    paint.color = Color.Black.copy(alpha = .18f).toArgb()
    paint.strokeWidth = 5 * dp
    canvas.drawLine(headX, stemStartY + 1 * dp, headX, footY + 1 * dp, paint)
    canvas.drawCircle(headX, headY + 1 * dp, radius + 1 * dp, paint)
    paint.color = android.graphics.Color.WHITE
    paint.strokeWidth = 3 * dp
    canvas.drawLine(headX, stemStartY, headX, footY, paint)
    canvas.drawCircle(headX, footY, footRadius, paint)
    canvas.drawCircle(headX, headY, radius, paint)
    paint.color = RoutePinStyle.headColor.toArgb()
    canvas.drawCircle(headX, headY, radius * .68f, paint)
    paint.color = (if (pickup) RoutePinStyle.pickupColor else RoutePinStyle.destinationColor).toArgb()
    canvas.drawCircle(headX, headY, radius * .26f, paint)
    return bitmap
}
