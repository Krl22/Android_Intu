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
        val perchAlpha = if (t >= 920f) 0f else progress(t, 200f, 300f)
        if (perchAlpha > 0f) {
            val k = .46f * s
            withTransform({
                translate(257.8f * sx, bottomY(487.5f) + rise2 * s)
                scale(-k, k, pivot = Offset.Zero)
            }) { drawPerchedBird(shapes, perchAlpha) }
        }
        inBottom(hillRise(200f)) { drawPath(shapes.hill3, Color(0xFF054A48)) }

        // Vuelo: del cerro al lugar del punto de la "i"
        if (t in 920f..1990f) {
            val flight = flights.getOrPut(size) { flightPath(sx, ::bottomY, ::centerY) }
            val p = FLIGHT_EASE.transform(progress(t, 920f, 950f))
            // Al despegar, el cerro aún termina de subir: el vuelo parte desde donde está la rama
            val pos = flight.measure.getPosition(flight.length * p) + Offset(0f, rise2 * s * (1 - p))
            val scale = (1f - .28f * EASE_IN.transform(progress(t, 920f, 950f))) * s
            val alpha = 1f - progress(t, 1870f, 120f)
            val flapUp = ((t - 920f) % 190f) < 95f && t < 1870f
            withTransform({
                translate(pos.x, pos.y)
                scale(-scale, scale, pivot = Offset.Zero)
                translate(-32f, -24f)
            }) { drawFlyingBird(shapes, flapUp, alpha) }
        }

        // Logo: el punto aparece donde llega el gallito y la "i" crece debajo
        val cx = w / 2f
        val dotP = progress(t, 1870f, 450f)
        if (dotP > 0f) {
            val pop = if (dotP < .6f) .3f + .88f * (dotP / .6f) else 1.18f - .18f * ((dotP - .6f) / .4f)
            drawCircle(Gold, radius = 14f * s * pop, center = Offset(cx, centerY(250f)), alpha = (dotP / .6f).coerceAtMost(1f))
        }
        val barP = OVERSHOOT.transform(progress(t, 2000f, 450f))
        if (barP > 0f) {
            drawRoundRect(
                Color.White,
                topLeft = Offset(cx - 12f * s, centerY(274f)),
                size = Size(24f * s, 54f * s * barP),
                cornerRadius = CornerRadius(12f * s)
            )
        }
        drawRisingText(textMeasurer, "intu", t, 2200f, cx, centerY(394f),
            TextStyle(fontFamily = wordFont, fontWeight = FontWeight.ExtraBold, color = Color.White,
                fontSize = (46f * s).toSp(), letterSpacing = (-1.6f * s).toSp()), s)
        drawRisingText(textMeasurer, "Tu mototaxi, a un toque", t, 2340f, cx, centerY(426f),
            TextStyle(fontFamily = tagFont, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = .75f),
                fontSize = (14f * s).toSp(), letterSpacing = (.4f * s).toSp()), s)
    }
}

private const val TOTAL_MS = 2940f
private val Gold = Color(0xFFEFB11B)
// Gallito de las rocas: rojo anaranjado, negro, gris plateado en la espalda y ojo claro
private val Scarlet = Color(0xFFEE4B1C)
private val CrestLight = Color(0xFFFF7A45)
private val BirdBlack = Color(0xFF15162B)
private val SilverGrey = Color(0xFFB9BCCB)
private val SilverLine = Color(0xFF8E92A6)
private val PaleEye = Color(0xFFF6E7A8)
private val Beak = Color(0xFFF3C757)
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

    // Gallito posado y erguido (lienzo de 100 x 120): cresta en cúpula que tapa el pico, ojo claro,
    // ala negra con la mancha gris plateada de la espalda y cola negra colgando
    val tail = path("M36,78 L27,113 Q31,117 37,115 L48,84 Z")
    val body = path("M30,70 C28,52 36,40 52,40 C66,40 74,52 72,68 C70,84 60,94 48,94 C38,94 31,84 30,70 Z")
    val head = path("M46,44 C42,26 50,8 64,6 C78,4 87,16 86,28 C85,35 82,40 77,43 C70,45 58,47 46,44 Z")
    val crestLine = path("M52,30 C52,18 60,10 68,10 C78,10 83,18 82,26")
    val wing = path("M29,60 C33,49 47,48 57,56 C63,68 60,84 49,92 C40,90 32,80 30,70 Z")
    val silver = path("M32,57 C38,50 49,51 55,58 C53,65 47,70 39,69 C34,66 31,62 32,57 Z")
    val silverLine = path("M37,60 C42,58 48,59 52,62")
    val beak = path("M77,42 L82,44 L77,45.5 Z")
    val legs = path("M50,93 L49,101 M57,92 L58,101")

    // Gallito en vuelo (lienzo de 64 x 46)
    val flyTail = path("M12,26 L0,22 L1,33 L13,31 Z")
    val flyBody = path("M10,28 C12,22 22,19 34,20 C42,21 46,25 45,29 C44,33 36,36 26,36 C18,36 11,33 10,28 Z")
    val flyHead = path("M38,23 C36,14 41,7 49,7 C57,7 61,14 60,20 C59,25 56,28 52,29 C47,28 42,27 38,23 Z")
    val flyCrestLine = path("M43,17 C44,12 47,10 50,10 C54,10 57,13 57,17")
    val wingUp = path("M19,23 C16,11 24,1 43,-3 C37,5 34,14 33,23 Z")
    val silverUp = path("M22,22 C22,16 25,12 30,11 C29,15 29,19 29,23 Z")
    val wingDown = path("M19,30 C16,41 24,50 43,53 C37,46 34,38 33,30 Z")
    val silverDown = path("M22,31 C22,36 25,40 30,41 C29,37 29,33 29,30 Z")
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
    drawPath(shapes.tail, BirdBlack, alpha)
    drawPath(shapes.body, Scarlet, alpha)
    drawPath(shapes.head, Scarlet, alpha)
    drawPath(shapes.crestLine, CrestLight, alpha, style = Stroke(width = 1.8f, cap = StrokeCap.Round))
    drawPath(shapes.wing, BirdBlack, alpha)
    drawPath(shapes.silver, SilverGrey, alpha)
    drawPath(shapes.silverLine, SilverLine, alpha, style = Stroke(width = 1.2f, cap = StrokeCap.Round))
    drawCircle(PaleEye, radius = 3.2f, center = Offset(72f, 31f), alpha = alpha)
    drawCircle(BirdBlack, radius = 1.5f, center = Offset(72.6f, 31f), alpha = alpha)
    drawPath(shapes.beak, Beak, alpha)
    drawPath(shapes.legs, BirdBlack, alpha, style = Stroke(width = 3f, cap = StrokeCap.Round))
}

private fun DrawScope.drawFlyingBird(shapes: SplashShapes, wingUp: Boolean, alpha: Float) {
    drawPath(shapes.flyTail, BirdBlack, alpha)
    drawPath(shapes.flyBody, Scarlet, alpha)
    drawPath(shapes.flyHead, Scarlet, alpha)
    drawPath(shapes.flyCrestLine, CrestLight, alpha, style = Stroke(width = 1.2f, cap = StrokeCap.Round))
    drawPath(if (wingUp) shapes.wingUp else shapes.wingDown, BirdBlack, alpha)
    drawPath(if (wingUp) shapes.silverUp else shapes.silverDown, SilverGrey, alpha)
    drawCircle(PaleEye, radius = 1.9f, center = Offset(52.5f, 18f), alpha = alpha)
    drawCircle(BirdBlack, radius = .9f, center = Offset(53f, 18f), alpha = alpha)
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
