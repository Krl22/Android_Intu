package com.intu.taxi

import com.intu.taxi.ui.screens.storedRoutePoints
import org.junit.Assert.*
import org.junit.Test

class HistoryRouteTest {
    @Test fun aStoredGeoJsonRouteCanBeReusedWithoutDirections() {
        val points = storedRoutePoints("""{"type":"LineString","coordinates":[[-74.63,-11.25],[-74.62,-11.24]]}""")
        assertEquals(2, points.size)
        assertEquals(-74.63, points.first().longitude(), 0.00001)
        assertEquals(-11.24, points.last().latitude(), 0.00001)
    }
    @Test fun missingAndCorruptGeometryCanFallBackToDirections() {
        listOf(null, "", "broken", """{"type":"LineString","coordinates":[[-74,120],[-73,-11]]}""").forEach {
            assertTrue(storedRoutePoints(it).isEmpty())
        }
    }
}
