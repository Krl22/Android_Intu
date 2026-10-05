package com.intu.taxi

import com.intu.taxi.models.*
import com.intu.taxi.repositories.businessMenuJson
import com.intu.taxi.repositories.parseBusinessMenu
import com.intu.taxi.data.deliveryDetails
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BusinessMenuTest {
    private val chicken = BusinessMenuItem(name = "Pollo + papas", price = 18.90, demoPhoto = BusinessPhoto.CHICKEN)
    private val juane = BusinessMenuItem(name = "Juane", price = 14.90, demoPhoto = BusinessPhoto.JUANE)
    @Test fun cartUsesDecimalTotalsAndMenuOrderAfterQuantityChanges() {
        val cart = businessCart(listOf(chicken, juane), mapOf(juane.id to 1, chicken.id to 2))
        assertEquals(listOf(chicken.id, juane.id), cart.map { it.itemId })
        assertEquals(52.70, cart.productsTotal(), 0.0)
        assertEquals(14.90, businessCart(listOf(chicken, juane), mapOf(chicken.id to 0, juane.id to 1)).productsTotal(), 0.0)
        assertTrue(businessCart(listOf(chicken), emptyMap()).isEmpty())
    }
    @Test fun unknownUnavailableAndExcessiveQuantitiesAreRejected() {
        listOf(mapOf("missing" to 1), mapOf(chicken.id to -1), mapOf(chicken.id to 11)).forEach { quantities ->
            assertThrows(IllegalArgumentException::class.java) { businessCart(listOf(chicken), quantities) }
        }
        assertThrows(IllegalArgumentException::class.java) { businessCart(listOf(chicken.copy(available = false)), mapOf(chicken.id to 1)) }
        val third = juane.copy(id = java.util.UUID.randomUUID().toString())
        assertThrows(IllegalArgumentException::class.java) { businessCart(listOf(chicken, juane, third), mapOf(chicken.id to 10, juane.id to 10, third.id to 1)) }
    }
    @Test fun malformedMenuCannotBeSavedAndRoundTripKeepsPhotosAndAvailability() {
        listOf(chicken.copy(price = .01), chicken.copy(price = 1.001), chicken.copy(price = Double.NaN),
            chicken.copy(name = ""), chicken.copy(id = "invalid"), chicken.copy(imageUrl = "http://example.com/photo.png")).forEach {
            assertThrows(IllegalArgumentException::class.java) { it.normalized() }
        }
        val menu = listOf(chicken, juane.copy(available = false))
        assertEquals(menu, parseBusinessMenu(businessMenuJson(menu)))
        val ad = BusinessAd(name = "Demo", title = "Título demo", description = "Descripción demo", address = "Recojo demo", menu = listOf(chicken, chicken))
        assertThrows(IllegalArgumentException::class.java) { ad.normalized() }
    }
    @Test fun historyParsesAuthoritativeItemSnapshotsAndLegacyParcels() {
        val detail = JSONObject("""{"recipient_name":"QA","recipient_phone":"+51987654321","description":"Demo","payer":"recipient",
            "business_name":"Brasa Demo","business_order_items":[{"item_id":"id","name":"Pollo","unit_price":18.9,"quantity":2}]}""")
        val parsed = JSONObject().put("delivery_details", detail).deliveryDetails()!!
        assertEquals("Brasa Demo", parsed.businessName)
        assertEquals(37.8, parsed.businessItems.productsTotal(), 0.0)
        detail.remove("business_order_items")
        assertTrue(JSONObject().put("delivery_details", detail).deliveryDetails()!!.businessItems.isEmpty())
    }
}
