package com.intu.taxi.ui.map

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sqrt

/** Ruta de Mapbox Directions: puntos, distancia (m) y duración (s). */
data class TripRoute(val points: List<Point>, val distanceMeters: Double, val durationSeconds: Double)

/**
 * Ruta y cámara del viaje en curso, compartidas por la pantalla del pasajero y la del conductor.
 * Se usan desde corrutinas que siguen corriendo con la app minimizada, así que no dependen de Compose.
 */
object TripMap {
    private val http = OkHttpClient()

    suspend fun fetchRoute(token: String, origin: Point, target: Point): TripRoute? = withContext(Dispatchers.IO) {
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
            "${origin.longitude()},${origin.latitude()};${target.longitude()},${target.latitude()}" +
            "?alternatives=false&geometries=geojson&overview=full&access_token=$token"
        runCatching {
            http.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val route = JSONObject(resp.body?.string().orEmpty()).optJSONArray("routes")?.optJSONObject(0)
                    ?: return@use null
                val coords = route.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return@use null
                val points = (0 until coords.length()).map { i ->
                    val c = coords.getJSONArray(i)
                    Point.fromLngLat(c.optDouble(0), c.optDouble(1))
                }
                if (points.size < 2) null
                else TripRoute(points, route.optDouble("distance", 0.0), route.optDouble("duration", 0.0))
            }
        }.getOrNull()
    }

    /**
     * Quita el tramo ya recorrido: la ruta empieza en [position] y sigue desde el segmento más cercano.
     * Devuelve null si [position] está a más de [maxOffRouteMeters] de la ruta (hay que pedir otra).
     */
    fun trimRoute(points: List<Point>, position: Point, maxOffRouteMeters: Double = 40.0): List<Point>? {
        if (points.size < 2) return null
        val cosLat = cos(Math.toRadians(position.latitude()))
        fun x(p: Point) = p.longitude() * 111_320.0 * cosLat
        fun y(p: Point) = p.latitude() * 110_540.0
        val px = x(position)
        val py = y(position)
        var bestIndex = 0
        var bestDistance = Double.MAX_VALUE
        for (i in 0 until points.size - 1) {
            val ax = x(points[i]); val ay = y(points[i])
            val dx = x(points[i + 1]) - ax; val dy = y(points[i + 1]) - ay
            val lengthSq = dx * dx + dy * dy
            val t = if (lengthSq == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / lengthSq).coerceIn(0.0, 1.0)
            val ex = ax + t * dx - px; val ey = ay + t * dy - py
            val distance = sqrt(ex * ex + ey * ey)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = i
            }
        }
        if (bestDistance > maxOffRouteMeters) return null
        return listOf(position) + points.subList(bestIndex + 1, points.size)
    }

    /** Largo de la ruta en metros. */
    fun lengthMeters(points: List<Point>): Double =
        points.zipWithNext { a, b -> metersBetween(a, b) }.sum()

    fun metersBetween(a: Point, b: Point): Double {
        val cosLat = cos(Math.toRadians((a.latitude() + b.latitude()) / 2))
        val dx = (b.longitude() - a.longitude()) * 111_320.0 * cosLat
        val dy = (b.latitude() - a.latitude()) * 110_540.0
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Encuadra [points] dejando libre [padding] (encabezado y tarjeta del viaje), un poco más lejos que
     * el encuadre exacto para ver el entorno. Con la app minimizada mueve la cámara sin animación: al
     * volver, el mapa ya está en su lugar.
     */
    fun fitCamera(mapView: MapView, points: List<Point>, padding: EdgeInsets, animate: Boolean, maxZoom: Double = 16.0) {
        val valid = points.filter {
            it.latitude() in -90.0..90.0 && it.longitude() in -180.0..180.0 &&
                !(it.latitude() == 0.0 && it.longitude() == 0.0)
        }
        if (valid.isEmpty() || mapView.width <= 0 || mapView.height <= 0) return
        val map = mapView.mapboxMap
        val fitted = runCatching {
            map.cameraForCoordinates(valid, CameraOptions.Builder().build(), padding, maxZoom, null)
        }.getOrNull() ?: return
        val center = fitted.center ?: return
        val camera = CameraOptions.Builder()
            .center(center)
            .zoom(((fitted.zoom ?: 15.0) - 0.4).coerceAtLeast(3.0))
            .bearing(0.0)
            .pitch(0.0)
            .build()
        if (animate) map.easeTo(camera, MapAnimationOptions.mapAnimationOptions { duration(900L) })
        else map.setCamera(camera)
    }
}
