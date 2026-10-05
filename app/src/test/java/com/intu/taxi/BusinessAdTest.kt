package com.intu.taxi

import com.intu.taxi.models.BusinessAd
import org.junit.Assert.*
import org.junit.Test

class BusinessAdTest {
    private val valid = BusinessAd(name = " Demo ", title = " Un anuncio ", description = " Un negocio ficticio ", address = " Calle QA ")
    @Test fun adTrimsCopyAndAllowsAnImageWithoutChangingPickup() {
        val ad = valid.copy(imageUrl = " https://example.com/demo.png ").normalized()
        assertEquals("Demo", ad.name)
        assertEquals("https://example.com/demo.png", ad.imageUrl)
        assertEquals(valid.point, ad.point)
        assertTrue(valid.normalized().imageUrl.isEmpty())
    }
    @Test fun invalidCopyPickupAndImageCannotBePublished() {
        listOf(valid.copy(name = ""), valid.copy(title = "ab"), valid.copy(description = ""), valid.copy(address = ""),
            valid.copy(latitude = Double.NaN), valid.copy(longitude = 181.0), valid.copy(sortOrder = -1),
            valid.copy(imageUrl = "http://example.com/image.png"), valid.copy(imageUrl = "https://"),
            valid.copy(imageUrl = "https://user:pass@example.com/image.png")).forEach {
            assertThrows(IllegalArgumentException::class.java) { it.normalized() }
        }
    }
}
