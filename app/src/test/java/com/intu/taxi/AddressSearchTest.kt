package com.intu.taxi

import com.intu.taxi.data.*
import com.intu.taxi.repositories.buildAddressSearchUrl
import com.intu.taxi.repositories.parseAddressSearchResults
import org.junit.Assert.*
import org.junit.Test

class AddressSearchTest {
    private val place = CatalogPlace("intu", "Jirón Lima", latitude = -11.252, longitude = -74.638,
        status = "published", pickupVerified = true)
    private fun address(id: String, name: String, lat: Double = -11.2519) =
        PlaceSearchResult(id, name, "Satipo", lat, -74.638, PlaceSearchSource.MAPBOX)

    @Test fun catalogComesFirstEvenWhenMapboxHasMoreResults() {
        val street = address("mapbox:1", "Avenida Manuel Prado")
        assertEquals(listOf(place.asSearchResult(), street), mergePlaceSearchResults(listOf(place), listOf(street)))
        assertEquals(listOf(street), mergePlaceSearchResults(emptyList(), listOf(street)))
    }

    @Test fun duplicateUsesVerifiedCatalogCoordinatesWithoutHidingDistantNamesakes() {
        val duplicate = address("mapbox:1", "JIRON LIMA")
        val distant = address("mapbox:2", "Jirón Lima", lat = -12.0)
        assertEquals(listOf(place.asSearchResult(), distant), mergePlaceSearchResults(listOf(place), listOf(duplicate, distant, distant)))
    }

    @Test fun draftsAndUnverifiedPlacesNeverPrecedeAddresses() {
        val street = address("mapbox:1", "Avenida Manuel Prado")
        assertEquals(listOf(street), mergePlaceSearchResults(listOf(place.copy(status = "draft"), place.copy(pickupVerified = false)), listOf(street)))
    }

    @Test fun parsesAddressNumberLocalContextAndCoordinatesSkipsMalformedFeatures() {
        val results = parseAddressSearchResults("""{"features":[
            {"id":"address.1","text":"Jirón Lima","address":"350","center":[-74.638,-11.252],
             "context":[{"id":"place.1","text":"Satipo"},{"id":"region.1","text":"Junín"},{"id":"country.1","text":"Perú"}]},
            {"id":"address.bad","text":"Invalid","center":[-74.638,99]},
            {"id":"address.missing","text":"Missing"},
            {"id":"address.1","text":"Duplicate","center":[-74.638,-11.252]}
        ]}""")
        assertEquals(1, results.size)
        assertEquals("Jirón Lima 350", results.single().name)
        assertEquals("Satipo", results.single().subtitle)
        assertEquals(-74.638, results.single().longitude, 0.0)
        assertEquals(-11.252, results.single().latitude, 0.0)
        assertEquals(PlaceSearchSource.MAPBOX, results.single().source)
        assertTrue(parseAddressSearchResults("""{"features":[]}""").isEmpty())
    }

    @Test fun encodesQuerySupportsNoGpsAndKeepsPreviousLocalSearchArea() {
        val url = buildAddressSearchUrl("test-token", "  Jirón Lima; 350  ", null, null)
        assertEquals("Jirón Lima 350.json", url.pathSegments.last())
        assertEquals("-74.6382,-11.2521", url.queryParameter("proximity"))
        assertEquals("pe", url.queryParameter("country"))
        assertFalse(url.queryParameter("types")!!.contains("poi"))
        val bbox = url.queryParameter("bbox")!!.split(',').map { it.toDouble() }
        assertTrue(-74.6382 in bbox[0]..bbox[2] && -11.2521 in bbox[1]..bbox[3])
        assertTrue(bbox[3] - bbox[1] > 0.4)
        assertEquals("-74.63,-11.26", buildAddressSearchUrl("token", "avenida", -11.26, -74.63).queryParameter("proximity"))
        assertEquals("-74.6382,-11.2521", buildAddressSearchUrl("token", "avenida", Double.NaN, 0.0).queryParameter("proximity"))
        assertTrue(runCatching { buildAddressSearchUrl("token", "a".repeat(257), null, null) }.isFailure)
    }
}
