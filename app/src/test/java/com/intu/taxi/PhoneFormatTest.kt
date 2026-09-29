package com.intu.taxi

import com.intu.taxi.ui.formatPeruPhone
import com.intu.taxi.ui.peruLocalPhone
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneFormatTest {
    @Test
    fun peruLocalPhone_dropsCountryCode() {
        assertEquals("987654321", peruLocalPhone("+51987654321"))
        assertEquals("987654321", peruLocalPhone("+51 987 654 321"))
        assertEquals("987654321", peruLocalPhone("987654321"))
    }

    @Test
    fun formatPeruPhone_groupsInThrees() {
        assertEquals("987 654 321", formatPeruPhone("+51987654321"))
    }

    @Test
    fun otherCountriesKeepTheirDigits() {
        assertEquals("15551234567", peruLocalPhone("+1 555 123 4567"))
    }
}
