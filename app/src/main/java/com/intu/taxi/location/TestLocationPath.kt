package com.intu.taxi.location

import kotlin.math.cos
import kotlin.math.sqrt

/** Walks a road geometry by distance, including duplicate vertices and an exact final point. */
class TestLocationPath(private val points: List<MapTestLocation>) {
    init { require(points.size >= 2) { "La ruta aún no está disponible." } }
    private val segments = points.zipWithNext { a, b -> distance(a, b) }
    val lengthMeters = segments.sum()

    fun pointAt(meters: Double): MapTestLocation {
        require(meters.isFinite() && meters >= 0)
        if (meters >= lengthMeters) return points.last()
        var remaining = meters
        segments.forEachIndexed { index, length ->
            if (length > 0 && remaining < length) {
                val a = points[index]
                val b = points[index + 1]
                val fraction = remaining / length
                return MapTestLocation(a.latitude + (b.latitude - a.latitude) * fraction,
                    a.longitude + (b.longitude - a.longitude) * fraction)
            }
            remaining -= length
        }
        return points.last()
    }

    companion object {
        fun distance(a: TestLocation, b: TestLocation): Double {
            val dx = (b.longitude - a.longitude) * 111_320 * cos(Math.toRadians((a.latitude + b.latitude) / 2))
            val dy = (b.latitude - a.latitude) * 110_540
            return sqrt(dx * dx + dy * dy)
        }
    }
}
