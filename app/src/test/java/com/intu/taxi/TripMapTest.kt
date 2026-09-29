package com.intu.taxi

import com.intu.taxi.ui.map.TripMap
import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TripMapTest {
    // Ruta en Satipo: tres tramos hacia el norte, ~110 m cada uno
    private val route = listOf(
        Point.fromLngLat(-74.6380, -11.2560),
        Point.fromLngLat(-74.6380, -11.2550),
        Point.fromLngLat(-74.6380, -11.2540),
        Point.fromLngLat(-74.6380, -11.2530)
    )

    @Test
    fun trimRoute_dropsTheTravelledPart() {
        // A mitad del segundo tramo, 5 m al costado de la pista
        val position = Point.fromLngLat(-74.63805, -11.2545)
        val trimmed = assertNotNullAndGet(TripMap.trimRoute(route, position))
        assertEquals(position, trimmed.first())
        assertEquals(listOf(route[2], route[3]), trimmed.drop(1))
    }

    @Test
    fun trimRoute_returnsNullWhenOffRoute() {
        // ~150 m al este de la ruta: hay que pedir una nueva
        assertNull(TripMap.trimRoute(route, Point.fromLngLat(-74.6366, -11.2545)))
    }

    @Test
    fun lengthMeters_addsEverySegment() {
        assertEquals(332.0, TripMap.lengthMeters(route), 3.0)
    }

    private fun <T> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
