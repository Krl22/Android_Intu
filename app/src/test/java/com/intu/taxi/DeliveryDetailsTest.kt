package com.intu.taxi

import com.intu.taxi.models.DeliveryDetails
import com.intu.taxi.models.MotoOption
import com.intu.taxi.models.ServiceFare
import org.junit.Assert.*
import org.junit.Test

class DeliveryDetailsTest {
    @Test fun recipientPeruvianPhoneIsNormalizedWithoutDuplicatingCountryCode() {
        listOf("987 654 321", "+51 987654321", "51987654321").forEach {
            assertEquals("+51987654321", DeliveryDetails(" Ana ", it, " Un sobre ", smallPackageConfirmed = true).normalized().recipientPhone)
        }
        assertEquals("Un sobre", DeliveryDetails("Ana", "987654321", " Un sobre ", smallPackageConfirmed = true).normalized().description)
    }
    @Test fun incompleteOrUnsupportedContactCannotProduceAnOrder() {
        listOf("987", "876543210", "+1 2345678901", "texto").forEach {
            assertThrows(IllegalArgumentException::class.java) { DeliveryDetails("Ana", it, "Sobre").normalized() }
        }
        assertThrows(IllegalArgumentException::class.java) { DeliveryDetails("", "987654321", "Sobre").normalized() }
        assertThrows(IllegalArgumentException::class.java) { DeliveryDetails("Ana", "987654321", "").normalized() }
    }
    @Test fun hondaPremiumIsTwelvePercentOverTheRoundedFare() {
        assertEquals(4.8, ServiceFare.withBrandPremium(ServiceFare.estimate(1950.0, 480.0), "honda"), 0.0)
        assertEquals(3.4, ServiceFare.withBrandPremium(ServiceFare.estimate(0.0, 0.0), "honda"), 0.0)
        assertEquals(4.7, MotoOption.HONDA.fare(4.2, 3.3), 0.0)
        assertEquals(4.2, MotoOption.BAJAJ.fare(4.2, 3.3), 0.0)
        assertEquals(4.2, MotoOption.ANY.fare(4.2, 3.3), 0.0)
        assertEquals(3.3, MotoOption.DELIVERY.fare(4.2, 3.3), 0.0)
    }
    @Test fun discountedDeliveryFareKeepsMinimumAndServerRounding() {
        assertEquals(3.2, ServiceFare.estimate(0.0, 0.0, true), 0.0)
        assertEquals(4.2, ServiceFare.estimate(2100.0, 420.0, true), 0.0)
        assertEquals(4.3, ServiceFare.estimate(2100.0, 420.0), 0.0)
        assertEquals(4.3, ServiceFare.estimate(1950.0, 480.0), 0.0)
        assertEquals(ServiceFare.estimate(1950.0, 480.0), ServiceFare.estimate(1950.9, 480.9), 0.0)
    }
    @Test fun parcelScopeRequiresExplicitAcknowledgement() {
        assertThrows(IllegalArgumentException::class.java) { DeliveryDetails("Ana", "987654321", "Sobre").normalized() }
        assertTrue(DeliveryDetails("Ana", "987654321", "Sobre", smallPackageConfirmed = true).normalized().smallPackageConfirmed)
    }
}
