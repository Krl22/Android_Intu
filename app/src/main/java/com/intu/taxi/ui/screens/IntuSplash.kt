package com.intu.taxi.ui.screens

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.intu.taxi.R

/**
 * Splash animado de Intu (opción D de design/splash-opciones.html): amanece sobre los cerros de Satipo,
 * un gallito de las rocas despega de su rama, vuela al centro y se convierte en el punto de la "i".
 *
 * Todo se dibuja con las mismas formas del diseño (lienzo de 300 x 650). Los cerros se anclan abajo y el
 * logo al centro; en pantallas más bajas los cerros se achatan para no tapar el logo.
 * [onFinished] se llama cuando el logo terminó de armarse.
 */
@Composable
fun IntuSplash(onFinished: () -> Unit) {
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        time.animateTo(TOTAL_MS, tween(TOTAL_MS.toInt(), easing = LinearEasing))
        onFinished()
    }

    // Íconos claros en la barra de estado mientras el fondo es turquesa
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val lightStatus = controller?.isAppearanceLightStatusBars
        val lightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            lightStatus?.let { controller.isAppearanceLightStatusBars = it }
            lightNav?.let { controller.isAppearanceLightNavigationBars = it }
        }
    }

    val shapes = remember { SplashShapes() }
    // Ruta del vuelo según el tamaño de la pantalla (se calcula una vez)
    val flights = remember { mutableMapOf<Size, Flight>() }
    val textMeasurer = rememberTextMeasurer()
    val wordFont = remember { FontFamily(Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)) }
    val tagFont = remember { FontFamily(Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold)) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Intu, tu mototaxi a un toque" }
    ) {
        val t = time.value
        val w = size.width
        val h = size.height
        val sx = w / 300f
        val s = minOf(sx, h / 650f)
        // Grupo de abajo (cerros): ancho completo, alto según s. Grupo del centro (logo): escala s.
        val bottomTop = h - 650f * s
        val centerTop = h / 2f - 330f * s
        fun bottomY(y: Float) = bottomTop + y * s
        fun centerY(y: Float) = centerTop + y * s

        drawRect(Brush.verticalGradient(listOf(Color(0xFF0A928E), Color(0xFF06605D))))

        // Sol detrás de los cerros
        val sunP = EASE_OUT.transform(progress(t, 50f, 1500f))
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Gold.copy(alpha = .55f), Gold.copy(alpha = 0f)),
                center = Offset(150f * sx, bottomY(500f + 90f * (1 - sunP))),
                radius = 120f * s
            ),
            radius = 120f * s,
            center = Offset(150f * sx, bottomY(500f + 90f * (1 - sunP))),
            alpha = sunP
        )

        fun hillRise(start: Float) = 70f * (1 - EASE_OUT.transform(progress(t, start, 1000f)))
        fun inBottom(rise: Float, block: DrawScope.() -> Unit) = withTransform({
            translate(0f, bottomTop + rise * s)
            scale(sx, s, pivot = Offset.Zero)
        }, block)

        inBottom(hillRise(0f)) { drawPath(shapes.hill1, Color(0xFF0A7572)) }
        // Niebla que se desliza lento
        val mistAlpha = .07f * progress(t, 500f, 1200f)
        val mistX = -30f + 60f * EASE_IN_OUT.transform((t / 6000f).coerceIn(0f, 1f))
        inBottom(0f) {
            drawOval(Color.White.copy(alpha = mistAlpha), topLeft = Offset(120f + mistX - 150f, 508f), size = Size(300f, 28f))
        }
        val rise2 = hillRise(100f)
        inBottom(rise2) {
            drawPath(shapes.hill2, Color(0xFF07625F))
            drawPath(shapes.branch, Color(0xFF054A48), style = Stroke(width = 3f, cap = StrokeCap.Round))
        }
        // Gallito posado en su rama (mira hacia el centro); desaparece cuando despega
        val perchAlpha = if (t >= 720f) 0f else progress(t, 350f, 300f)
        if (perchAlpha > 0f) {
            val k = 56f / 120f * s
            withTransform({
                translate(262f * sx, bottomY(490f) + rise2 * s)
                scale(-k, k, pivot = Offset.Zero)
            }) { drawPerchedBird(shapes, perchAlpha) }
        }
        inBottom(hillRise(200f)) { drawPath(shapes.hill3, Color(0xFF054A48)) }

        // Vuelo: del cerro al lugar del punto de la "i"
        if (t in 700f..1770f) {
            val flight = flights.getOrPut(size) { flightPath(sx, ::bottomY, ::centerY) }
            val p = FLIGHT_EASE.transform(progress(t, 700f, 950f))
            // Al despegar, el cerro aún termina de subir: el vuelo parte desde donde está la rama
            val pos = flight.measure.getPosition(flight.length * p) + Offset(0f, rise2 * s * (1 - p))
            val scale = (1f - .28f * EASE_IN.transform(progress(t, 700f, 950f))) * s
            val alpha = 1f - progress(t, 1650f, 120f)
            val flapUp = ((t - 700f) % 190f) < 95f && t < 1650f
            withTransform({
                translate(pos.x, pos.y)
                scale(-scale, scale, pivot = Offset.Zero)
                translate(-30f, -22f)
            }) { drawFlyingBird(shapes, flapUp, alpha) }
        }

        // Logo: el punto aparece donde llega el gallito y la "i" crece debajo
        val cx = w / 2f
        val dotP = progress(t, 1650f, 450f)
        if (dotP > 0f) {
            val pop = if (dotP < .6f) .3f + .88f * (dotP / .6f) else 1.18f - .18f * ((dotP - .6f) / .4f)
            drawCircle(Gold, radius = 14f * s * pop, center = Offset(cx, centerY(250f)), alpha = (dotP / .6f).coerceAtMost(1f))
        }
        val barP = OVERSHOOT.transform(progress(t, 1780f, 450f))
        if (barP > 0f) {
            drawRoundRect(
                Color.White,
                topLeft = Offset(cx - 12f * s, centerY(274f)),
                size = Size(24f * s, 54f * s * barP),
                cornerRadius = CornerRadius(12f * s)
            )
        }
        drawRisingText(textMeasurer, "intu", t, 1980f, cx, centerY(394f),
            TextStyle(fontFamily = wordFont, fontWeight = FontWeight.ExtraBold, color = Color.White,
                fontSize = (46f * s).toSp(), letterSpacing = (-1.6f * s).toSp()), s)
        drawRisingText(textMeasurer, "Tu mototaxi, a un toque", t, 2120f, cx, centerY(426f),
            TextStyle(fontFamily = tagFont, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = .75f),
                fontSize = (14f * s).toSp(), letterSpacing = (.4f * s).toSp()), s)
    }
}

private const val TOTAL_MS = 2720f
private val Gold = Color(0xFFEFB11B)
private val Orange = Color(0xFFE8601C)
private val Indigo = Color(0xFF1E1F47)
private val EASE_OUT = CubicBezierEasing(.2f, .8f, .2f, 1f)
private val EASE_IN = CubicBezierEasing(.42f, 0f, 1f, 1f)
private val EASE_IN_OUT = CubicBezierEasing(.42f, 0f, .58f, 1f)
private val FLIGHT_EASE = CubicBezierEasing(.4f, .05f, .3f, 1f)
private val OVERSHOOT = CubicBezierEasing(.3f, 1.4f, .5f, 1f)

/** Avance de 0 a 1 de un tramo que empieza en [start] ms y dura [duration] ms. */
private fun progress(t: Float, start: Float, duration: Float) = ((t - start) / duration).coerceIn(0f, 1f)

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

/** Formas del diseño, en las unidades del SVG. */
private class SplashShapes {
    val hill1 = path("M0,520 C55,485 105,498 150,480 C200,462 245,485 300,470 L300,720 L0,720 Z")
    val hill2 = path("M0,560 C48,538 95,552 140,534 C188,516 236,548 300,530 L300,720 L0,720 Z")
    val hill3 = path("M0,600 C65,578 112,600 178,584 C225,573 262,590 300,583 L300,720 L0,720 Z")
    val branch = path("M196,538 Q232,528 272,540")

    // Gallito posado (lienzo de 120 x 100): cresta en medialuna que tapa el pico, ala y cola negras
    val tail = path("M36,70 L16,90 Q20,95 26,93 L44,76 Z")
    val body = path("M32,66 C32,52 44,44 60,46 C74,48 82,58 80,70 C78,80 66,86 52,85 C40,84 32,77 32,66 Z")
    val head = path("M58,54 C54,36 62,18 80,14 C96,11 108,22 108,36 C108,42 105,47 100,50 C92,51 86,53 80,56 C72,59 62,59 58,54 Z")
    val wing = path("M40,62 C50,54 66,56 76,66 C70,76 54,78 38,72 Z")
    val legs = path("M52,84 L50,92 M60,84 L60,92")

    // Gallito en vuelo (lienzo de 60 x 44)
    val flyTail = path("M13,21 L1,16 L2,27 L13,25 Z")
    val flyHead = path("M32,20 C30,10 36,4 44,4 C52,4 57,10 56,16 C55,19 53,21 50,22 C45,23 38,24 32,20 Z")
    val wingUp = path("M18,18 C16,6 26,-2 36,1 C31,7 29,13 29,19 Z")
    val wingDown = path("M18,24 C16,36 26,43 36,40 C31,34 29,29 29,24 Z")
}

private class Flight(val measure: PathMeasure, val length: Float)

/** Ruta del vuelo: sale del cerro (anclado abajo) y termina en el punto de la "i" (anclado al centro). */
private fun flightPath(sx: Float, bottomY: (Float) -> Float, centerY: (Float) -> Float): Flight {
    val route = Path().apply {
        moveTo(234f * sx, bottomY(516f))
        cubicTo(236f * sx, bottomY(440f), 226f * sx, centerY(360f), 196f * sx, centerY(300f))
        cubicTo(180f * sx, centerY(270f), 162f * sx, centerY(254f), 150f * sx, centerY(250f))
    }
    val measure = PathMeasure().apply { setPath(route, false) }
    return Flight(measure, measure.length)
}

private fun DrawScope.drawPerchedBird(shapes: SplashShapes, alpha: Float) {
    drawPath(shapes.tail, Indigo, alpha)
    drawPath(shapes.body, Orange, alpha)
    drawPath(shapes.head, Orange, alpha)
    drawPath(shapes.wing, Indigo, alpha)
    drawCircle(Indigo, radius = 2.6f, center = Offset(91f, 41f), alpha = alpha)
    drawPath(shapes.legs, Indigo, alpha, style = Stroke(width = 3f, cap = StrokeCap.Round))
}

private fun DrawScope.drawFlyingBird(shapes: SplashShapes, wingUp: Boolean, alpha: Float) {
    drawPath(shapes.flyTail, Indigo, alpha)
    drawOval(Orange, topLeft = Offset(12f, 14f), size = Size(28f, 16f), alpha = alpha)
    drawPath(shapes.flyHead, Orange, alpha)
    drawCircle(Indigo, radius = 1.3f, center = Offset(48f, 13f), alpha = alpha)
    drawPath(if (wingUp) shapes.wingUp else shapes.wingDown, Indigo, alpha)
}

/** Texto centrado en [cx] con su línea base en [baseline]; sube 14 unidades y aparece desde [start] ms. */
private fun DrawScope.drawRisingText(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    t: Float,
    start: Float,
    cx: Float,
    baseline: Float,
    style: TextStyle,
    s: Float
) {
    val p = EASE_OUT.transform(progress(t, start, 600f))
    if (p <= 0f) return
    val layout = measurer.measure(text, style)
    translate(top = 14f * s * (1 - p)) {
        drawText(
            layout,
            topLeft = Offset(cx - layout.size.width / 2f, baseline - layout.firstBaseline),
            alpha = p
        )
    }
}
