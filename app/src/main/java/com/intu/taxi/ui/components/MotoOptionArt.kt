package com.intu.taxi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.intu.taxi.models.MotoOption
import kotlin.math.min

/**
 * Side-view vehicles for "Elige tu moto", drawn in a 140×90 box with the Intu palette.
 * Only the selected option moves: wheels spin, the body bounces and the exhaust puffs.
 */
@Composable
fun MotoOptionArt(option: MotoOption, animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        val transition = rememberInfiniteTransition(label = "moto-${option.code}")
        val spin = transition.animateFloat(0f, 360f,
            infiniteRepeatable(tween(500, easing = LinearEasing)), label = "spin")
        val bob = transition.animateFloat(0f, -1.2f,
            infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
        val puff = transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "puff")
        Canvas(modifier) { drawVehicle(option, Motion(spin.value, bob.value, puff.value)) }
    } else Canvas(modifier) { drawVehicle(option, null) }
}

private class Motion(val spin: Float, val bob: Float, val puff: Float)

private val Teal = Color(0xFF08817E)
private val TealDark = Color(0xFF066664)
private val Ink = Color(0xFF202538)
private val Cabin = Color(0xFFF4F7F6)
private val CabinEdge = Color(0xFFAEB8C1)
private val Luggage = Color(0xFFE0A43B)
private val Strap = Color(0xFF5A3A17)
private val Rack = Color(0xFF3A404C)
private val Frame = Color(0xFF2A2F3A)
private val Engine = Color(0xFF8A929E)
private val Pipe = Color(0xFF9AA2AD)
private val SeatDark = Color(0xFF1F232B)
private val Interior = Color(0xFF39404C)
private val Glass = Color(0xFFBFE0EC)
private val Headlight = Color(0xFFFFD76A)
private val Tire = Color(0xFF22262F)
private val Rim = Color(0xFFCFD5DC)
private val Spoke = Color(0xFF6B7380)
private val Hub = Color(0xFF4A515C)
private val Smoke = Color(0xFFC3CAD1)

private fun DrawScope.drawVehicle(option: MotoOption, motion: Motion?) {
    val s = min(size.width / 140f, size.height / 90f)
    translate((size.width - 140f * s) / 2f, (size.height - 90f * s) / 2f) {
        scale(s, Offset.Zero) {
            when (option) {
                MotoOption.HONDA -> honda(motion)
                MotoOption.BAJAJ -> bajaj(motion)
                MotoOption.ANY -> anyMototaxi(motion)
                MotoOption.DELIVERY -> courier(motion)
            }
        }
    }
}

/** Bajaj fades behind a Honda: any registered brand may answer. */
private fun DrawScope.anyMototaxi(motion: Motion?) {
    withTransform({ translate(4f, -6f); scale(0.78f, Offset.Zero) }) {
        drawContext.canvas.saveLayer(Rect(-20f, -20f, 160f, 110f), Paint().apply { alpha = 0.5f })
        bajaj(motion)
        drawContext.canvas.restore()
    }
    withTransform({ translate(26f, 16f); scale(0.8f, Offset.Zero) }) { honda(motion) }
}

/** The rear luggage rack is what sets the Honda apart (and justifies its premium). */
private fun DrawScope.honda(motion: Motion?) {
    shadow(70f, 62f)
    wheel(38f, 72f, 11f, motion); wheel(114f, 70f, 13f, motion)
    translate(top = motion?.bob ?: 0f) {
        stroke("M24 72a14 14 0 0 1 28 0", Teal, 3f)
        box(3f, 35f, 15f, 21f, 2f, Luggage)
        stroke("M8 35v-3h5v3", Strap, 1.4f)
        drawLine(Strap, Offset(3f, 41f), Offset(18f, 41f), 1f)
        stroke("M18 46H4v14h14M4 53h14", Rack, 1.8f)
        box(16f, 58f, 72f, 6f, 2f, Frame)
        box(18f, 26f, 58f, 33f, 3f, Cabin)
        drawRoundRect(CabinEdge, Offset(18f, 26f), Size(58f, 33f), CornerRadius(3f), style = Stroke(0.8f))
        box(18f, 48f, 58f, 3f, 0f, Teal)
        box(23f, 31f, 20f, 13f, 2f, Glass)
        box(49f, 30f, 23f, 18f, 2f, Interior)
        box(51f, 40f, 13f, 4f, 1f, SeatDark)
        fill("M13 27V21q0-7 8-7h76q7 0 7 7v4z", Teal)
        box(13f, 25f, 91f, 3f, 0f, TealDark)
        drawLine(Rack, Offset(101f, 28f), Offset(98f, 40f), 1.6f)
        stroke("M84 64L62 67", Pipe, 2.5f)
        box(80f, 54f, 15f, 11f, 2f, Engine)
        stroke("M76 58h20l8-18", Frame, 3f)
        fill("M76 46q1-5 7-5h11q2 3 0 6H78z", SeatDark)
        fill("M92 47q2-9 10-9l5 2q0 8-6 10h-9z", Teal)
        drawLine(Pipe, Offset(105f, 40f), Offset(114f, 70f), 2.6f)
        stroke("M103 64a13 13 0 0 1 22-6", Teal, 3f)
        stroke("M104 36l-4-6h-5", Frame, 2f)
        headlight(110f, 40f, 3.8f)
    }
    puffs(60f, 67f, motion)
}

private fun DrawScope.bajaj(motion: Motion?) {
    shadow(66f, 58f)
    wheel(34f, 73f, 10f, motion); wheel(112f, 74f, 9f, motion)
    translate(top = motion?.bob ?: 0f) {
        fill("M8 38V23q0-11 12-11h70q8 0 8 10v2H30v14z", Ink)
        box(12f, 25f, 13f, 8f, 1.5f, Glass)
        fill("M8 62V40q0-4 4-4h74l2 4h16q8 2 12 12l2 6q0 4-4 4z", Teal)
        box(32f, 24f, 54f, 22f, 2f, Interior)
        box(64f, 24f, 3f, 22f, 0f, Teal)
        box(36f, 38f, 22f, 5f, 1.5f, SeatDark)
        box(70f, 38f, 10f, 4f, 1f, SeatDark)
        fill("M86 24h12l6 16H88z", Glass)
        stroke("M86 24h12l6 16H88z", TealDark, 1.2f)
        drawLine(Frame, Offset(92f, 38f), Offset(96f, 28f), 1f)
        drawLine(Color.White.copy(alpha = 0.85f), Offset(8f, 53f), Offset(115f, 53f), 2.2f)
        stroke("M101 72a11 11 0 0 1 22 0", TealDark, 3f)
        box(104f, 42f, 4f, 3f, 1f, Luggage)
        headlight(113f, 47f, 3.4f)
        drawLine(Frame, Offset(98f, 22f), Offset(103f, 18f), 1.4f)
        drawCircle(Frame, 1.8f, Offset(104f, 17.5f))
    }
    puffs(6f, 64f, motion)
}

/** Motorcycle courier with helmet and an Intu delivery box on their back. */
private fun DrawScope.courier(motion: Motion?) {
    shadow(74f, 52f)
    wheel(36f, 70f, 13f, motion); wheel(112f, 70f, 13f, motion)
    translate(top = motion?.bob ?: 0f) {
        stroke("M64 62L36 70", Frame, 3.2f)
        stroke("M62 66L30 62", Pipe, 3f)
        fill("M26 52l22-8 2 6-22 6z", Ink)
        box(58f, 51f, 18f, 14f, 3f, Engine)
        stroke("M66 60L90 42l12-6", Frame, 3f)
        fill("M44 47q2-7 10-7l20 2-2 6H46z", SeatDark)
        fill("M72 48q3-14 16-14l8 4q0 8-8 10z", Ink)
        drawLine(Pipe, Offset(102f, 36f), Offset(112f, 70f), 3f)
        stroke("M101 64a13 13 0 0 1 23-5", Ink, 3f)
        stroke("M101 33l-5-7h-5", Frame, 2f)
        headlight(107f, 39f, 4f)
        stroke("M60 44l13 8-2 11", Color(0xFF2B3550), 5f)
        box(45f, 17f, 17f, 19f, 2.5f, Teal)
        box(49.5f, 25f, 8f, 2.4f, 1.2f, Color.White)
        stroke("M60 42l10-18", TealDark, 9f)
        stroke("M70 28l12 4 12-2", TealDark, 4.5f)
        drawCircle(Cabin, 7f, Offset(73f, 15f))
        drawCircle(Ink, 7f, Offset(73f, 15f), style = Stroke(1f))
        box(75f, 11f, 5.5f, 5f, 0f, SeatDark)
    }
    puffs(27f, 62f, motion)
}

private val pathCache = HashMap<String, Path>()
private fun path(d: String) = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

private fun DrawScope.fill(d: String, color: Color) = drawPath(path(d), color)

private fun DrawScope.stroke(d: String, color: Color, width: Float) =
    drawPath(path(d), color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))

private fun DrawScope.box(x: Float, y: Float, w: Float, h: Float, r: Float, color: Color) =
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r))

private fun DrawScope.shadow(cx: Float, halfWidth: Float) =
    drawOval(Color.Black.copy(alpha = 0.08f), Offset(cx - halfWidth, 81.5f), Size(halfWidth * 2f, 5f))

private fun DrawScope.headlight(x: Float, y: Float, r: Float) {
    drawCircle(Headlight, r, Offset(x, y))
    drawCircle(Frame, r, Offset(x, y), style = Stroke(1f))
}

private fun DrawScope.wheel(cx: Float, cy: Float, r: Float, motion: Motion?) {
    val center = Offset(cx, cy)
    drawCircle(Tire, r, center)
    drawCircle(Rim, r * 0.62f, center)
    val k = r * 0.58f
    rotate(motion?.spin ?: 0f, center) {
        repeat(4) { i ->
            rotate(i * 45f, center) { drawLine(Spoke, Offset(cx - k, cy), Offset(cx + k, cy), 1.2f) }
        }
    }
    drawCircle(Hub, r * 0.2f, center)
}

private fun DrawScope.puffs(x: Float, y: Float, motion: Motion?) {
    val phase = motion?.puff ?: return
    repeat(3) { i ->
        val p = (phase + i / 3f) % 1f
        drawCircle(Smoke.copy(alpha = 0.85f * (1f - p)), 2.6f * (0.6f + 1.1f * p), Offset(x - 14f * p, y - 6f * p))
    }
}
