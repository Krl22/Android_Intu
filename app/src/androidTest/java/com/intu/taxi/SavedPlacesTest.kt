package com.intu.taxi

import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.data.SavedPlace
import com.intu.taxi.data.SavedPlaces
import org.junit.Assert.*
import org.junit.Test

class SavedPlacesTest {
    @Test fun placesPersistAcrossRecreationAndStaySeparateBetweenAccounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uid = "qa-places-${java.util.UUID.randomUUID()}"
        val first = SavedPlaces(context, uid)
        val other = SavedPlaces(context, "$uid-other")
        val casa = SavedPlace("casa", "Casa", "Satipo", -11.25, -74.63)
        try {
            first.save(casa)
            assertEquals(casa, SavedPlaces(context, uid).read().single())
            assertTrue(other.read().isEmpty())
            first.save(casa.copy(address = "Mi nueva casa", longitude = -74.62))
            assertEquals(1, first.read().size)
            assertEquals(-74.62, first.read().single().longitude, 0.00001)
            first.remove("casa")
            assertTrue(SavedPlaces(context, uid).read().isEmpty())
        } finally {
            context.getSharedPreferences("intu_saved_places", 0).edit().remove("places_$uid").remove("places_${uid}-other").commit()
        }
    }
}
