package com.intu.taxi

import com.intu.taxi.data.CatalogPlace
import com.intu.taxi.data.searchCatalog
import com.intu.taxi.data.validPlaceCoordinates
import org.junit.Assert.*
import org.junit.Test

class CatalogSearchTest {
    private fun place(id: String, name: String, aliases: List<String> = emptyList(), lat: Double = -11.252, lng: Double = -74.638) =
        CatalogPlace(id, name, aliases, category = "hospital", latitude = lat, longitude = lng, status = "published", pickupVerified = true)

    @Test fun matchesAliasesAndAccentsWithoutGps() {
        val hospital = place("1", "Hospital Manuel Ángel Higa", listOf("Hospital de apoyo"))
        assertEquals(listOf(hospital), searchCatalog("  MANUEL angel  ", listOf(hospital)))
        assertEquals(listOf(hospital), searchCatalog("hospital de apoyo", listOf(hospital)))
    }

    @Test fun exactNamePrecedesCloserPartialMatches() {
        val exact = place("far", "Plaza Principal", lat = -12.0)
        val near = place("near", "Plaza Principal Norte")
        assertEquals(listOf(exact, near), searchCatalog("plaza principal", listOf(near, exact), -11.252, -74.638))
    }

    @Test fun proximityOrdersEquallyRelevantNamesWithoutBoundingBoxExclusion() {
        val far = place("far", "Hospital Norte", lat = -12.0)
        val near = place("near", "Hospital Sur")
        assertEquals(listOf(near, far), searchCatalog("hospital", listOf(far, near), -11.252, -74.638))
    }

    @Test fun neverSuggestsDraftInactiveOrUnverifiedPlaces() {
        val hospital = place("1", "Hospital")
        assertTrue(searchCatalog("hospital", listOf(hospital.copy(status = "draft"), hospital.copy(status = "inactive"), hospital.copy(pickupVerified = false))).isEmpty())
    }

    @Test fun searchesCategoryAddressAndMultiWordQueries() {
        val place = place("1", "Manuel Higa").copy(address = "Jirón Lima 350")
        assertEquals(listOf(place), searchCatalog("hospital lima", listOf(place)))
        assertTrue(searchCatalog("lima desconocido", listOf(place)).isEmpty())
    }

    @Test fun handlesShortQueriesLimitsAndInvalidLocation() {
        val places = (1..10).map { place(it.toString(), "Hospital $it") }
        assertTrue(searchCatalog("h", places).isEmpty())
        assertEquals(6, searchCatalog("hospital", places, Double.NaN, 999.0).size)
        assertEquals(2, searchCatalog("hospital", places, limit = 2).size)
        assertFalse(validPlaceCoordinates(Double.NaN, 1.0))
        assertFalse(validPlaceCoordinates(-11.0, Double.POSITIVE_INFINITY))
        assertFalse(validPlaceCoordinates(91.0, -74.0))
    }
}
