package com.intu.taxi

import com.intu.taxi.models.*
import org.junit.Assert.*
import org.junit.Test

class FareSettingsTest {
    @Test fun newDefaultsAndDeliveryRemainIndependent() {
        assertEquals(3.0, ServiceFare.estimate(0.0, 0.0), 0.0)
        assertEquals(5.5, ServiceFare.estimate(3000.0, 600.0), 0.0)
        assertEquals(6.2, ServiceFare.withBrandPremium(5.5, "honda"), 0.0)
        assertEquals(3.2, ServiceFare.estimate(0.0, 0.0, true), 0.0)
        assertEquals(5.2, ServiceFare.estimate(3000.0, 600.0, true, FareSettings(baseFare = 100.0)), 0.0)
    }
    @Test fun allSixCoefficientsChangeThePriceAndKeepTheMinimum() {
        val rates = FareSettings(2.0, 1.25, 0.2, 3.03, 20.0, 0.5)
        assertEquals(3.5, ServiceFare.estimate(0.0, 0.0, settings = rates), 0.0)
        assertEquals(8.0, ServiceFare.estimate(3000.0, 600.0, settings = rates), 0.0)
        assertEquals(9.5, MotoOption.HONDA.fare(8.0, 5.2, rates), 0.0)
        assertEquals(8.0, MotoOption.BAJAJ.fare(8.0, 5.2, rates), 0.0)
        assertEquals(8.0, MotoOption.ANY.fare(8.0, 5.2, rates), 0.0)
        assertEquals(5.2, MotoOption.DELIVERY.fare(8.0, 5.2, rates), 0.0)
    }
    @Test fun decimalStepsUseHalfUpAndHondaRoundsTwice() {
        val rates = FareSettings(1.52, 1.0, 0.1, 0.0, 12.0, 0.05)
        assertEquals(4.3, ServiceFare.estimate(1955.0, 480.0, settings = rates), 0.0)
        assertEquals(4.8, ServiceFare.withBrandPremium(4.3, "honda", rates), 0.0)
    }
    @Test fun decimalCommaAndInvalidInputs() {
        assertEquals(FareSettings.Default, FareSettings.parse(listOf("1,5","1","0,1","3","12","0,10")))
        listOf("", "NaN", "Infinity", "-1", "10000", "1.005").forEach { bad ->
            assertThrows(IllegalArgumentException::class.java) { FareSettings.parse(listOf(bad,"1","0.1","3","12","0.10")) }
        }
        assertThrows(IllegalArgumentException::class.java) { FareSettings(hondaPremiumPercent = 100.01).validated() }
        assertThrows(IllegalArgumentException::class.java) { FareSettings(roundingStep = 0.0).validated() }
    }
    @Test fun driverOfferIsAnExactAmountAndMustDifferFromAppFare() {
        assertEquals(7.25, parseDriverOffer("7,25",5.5), 0.0)
        listOf("5.50","0","-1","1.001","10000","NaN","").forEach {
            assertThrows(IllegalArgumentException::class.java) { parseDriverOffer(it,5.5) }
        }
    }
}
