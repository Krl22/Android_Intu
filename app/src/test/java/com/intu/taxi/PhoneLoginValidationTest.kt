package com.intu.taxi

import com.intu.taxi.auth.PhoneFormatter
import org.junit.Assert.*
import org.junit.Test

class PhoneLoginValidationTest {
    @Test fun peruLocalAndPastedInternationalNumbersResolveToSameMobile() {
        listOf("987654321", "987 654 321", "+51 987 654 321", "51987654321", "0051 987654321", "+51 (987) 654-321").forEach {
            assertTrue(it, PhoneFormatter.isValid("+51", it))
            assertEquals(it, "+51987654321", PhoneFormatter.formatE164("+51", it))
            assertEquals(it, "+51987654321", PhoneFormatter.normalizeMobile(it))
        }
    }
    @Test fun peruRejectsFixedLinesWrongLengthsAndNonPhoneText() {
        listOf("123456789", "98765432", "9876543210", "+51123456789", "+519876543210", "987abc654321", "++51987654321", "").forEach {
            assertFalse(it, PhoneFormatter.isValid("+51", it))
            assertNull(it, PhoneFormatter.normalizeMobile(it))
        }
    }
    @Test fun countryPickerNeverTreatsAnotherCountryAsPeru() {
        assertFalse(PhoneFormatter.isValid("+51", "+1 857 123 4567"))
        assertTrue(PhoneFormatter.isValid("+1", "+1 857 123 4567"))
        assertEquals("+18571234567", PhoneFormatter.formatE164("+1", "+1 857 123 4567"))
        assertEquals("+18571234567", PhoneFormatter.normalizeMobile("+1 857 123 4567"))
    }
    @Test fun smsCodeInputUsesAsciiDigitsOnly() {
        assertEquals("123456", PhoneFormatter.sanitizeDigits("12 34-56"))
        assertEquals("", PhoneFormatter.sanitizeDigits("١٢٣abc"))
    }
}
