package com.intu.taxi

import com.intu.taxi.guide.GuideDestination
import com.intu.taxi.push.AdminActivityMessage
import com.intu.taxi.repositories.GuideBusiness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideDestinationTest {
    @Test fun readsTheWebGuideLink() {
        val destination = GuideDestination.parse("intu://destino?lat=-11.254100&lng=-74.638200&nombre=Hostal%20El%20Caf%C3%A9%20%2B%20Bar")
        assertEquals(GuideDestination(-11.2541, -74.6382, "Hostal El Café + Bar"), destination)
    }

    @Test fun usesADefaultNameAndCleansControlCharacters() {
        assertEquals("Destino de Descubre Satipo", GuideDestination.parse("intu://destino?lat=-11.25&lng=-74.63")?.name)
        assertEquals("Línea uno dos", GuideDestination.parse("intu://destino?lat=-11.25&lng=-74.63&nombre=L%C3%ADnea%0Auno%09dos")?.name)
        assertEquals(120, GuideDestination.parse("intu://destino?lat=-11.25&lng=-74.63&nombre=" + "a".repeat(300))?.name?.length)
    }

    @Test fun rejectsOtherLinksAndPointsOutsidePeru() {
        assertNull(GuideDestination.parse(null))
        assertNull(GuideDestination.parse("https://destino?lat=-11.25&lng=-74.63"))
        assertNull(GuideDestination.parse("intu://otro?lat=-11.25&lng=-74.63"))
        assertNull(GuideDestination.parse("intu://destino?lat=40.7&lng=-74.0"))
        assertNull(GuideDestination.parse("intu://destino?lat=NaN&lng=-74.63"))
        assertNull(GuideDestination.parse("intu://destino?lat=-11.25"))
        assertNull(GuideDestination.parse("intu://destino?lat=abc&lng=-74.63"))
        // A malformed escape makes the whole link invalid.
        assertNull(GuideDestination.parse("intu://destino?lat=-11.25&lng=-74.63&nombre=%ZZ"))
        assertNull(GuideDestination.fromValues(Double.POSITIVE_INFINITY, -74.0, "x"))
    }

    @Test fun adminPointAcceptsGoogleMapsCopiesInsidePeruOnly() {
        assertEquals(-11.2541 to -74.6382, GuideBusiness.parsePoint("-11.2541, -74.6382"))
        assertEquals(-11.2541 to -74.6382, GuideBusiness.parsePoint(" (-11.2541,-74.6382) "))
        assertNull(GuideBusiness.parsePoint("-11.2541"))
        assertNull(GuideBusiness.parsePoint("40.7, -74.0"))
        assertNull(GuideBusiness.parsePoint("hola, mundo"))
    }

    @Test fun whatsappAddsPeruPrefixToLocalMobiles() {
        assertEquals("51987654321", GuideBusiness.whatsappNumber("987654321"))
        assertEquals("51987654321", GuideBusiness.whatsappNumber("+51987654321"))
        assertEquals("6412345", GuideBusiness.whatsappNumber("6412345"))
    }

    @Test fun guideNotificationsUseTheirOwnEventReferences() {
        assertTrue(AdminActivityMessage.isGuideEvent("guide:94aeb10e-c2ea-4405-93c4-4b03f28224e6"))
        assertFalse(AdminActivityMessage.isGuideEvent("tester:94aeb10e-c2ea-4405-93c4-4b03f28224e6"))
        assertFalse(AdminActivityMessage.isGuideEvent("guide:foo"))
    }
}
