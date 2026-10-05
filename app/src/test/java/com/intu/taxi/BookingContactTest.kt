package com.intu.taxi

import com.intu.taxi.models.BookingContact
import com.intu.taxi.models.DeliveryDetails
import org.junit.Assert.*
import org.junit.Test

class BookingContactTest {
    @Test fun selectedContactNormalizesPeruvianMobile() {
        assertEquals(BookingContact("Ana", "+51987654321"), BookingContact(" Ana ", "987 654 321").normalized())
    }
    @Test fun landlinesAndInvalidNamesCannotBecomeGuestContacts() {
        listOf(BookingContact("A", "987654321"), BookingContact("Ana", "123456789"), BookingContact("Ana", ""))
            .forEach { assertTrue(runCatching { it.normalized() }.isFailure) }
    }
    @Test fun courierSenderAndRecipientRemainDifferentPeople() {
        val details = DeliveryDetails(" Ana ", "987654321", " Sobre ", smallPackageConfirmed = true,
            sender = BookingContact(" Luis ", "988888888")).normalized()
        assertEquals("Ana", details.recipientName)
        assertEquals(BookingContact("Luis", "+51988888888"), details.sender)
        assertTrue(runCatching { details.copy(sender = BookingContact("Luis", "987")).normalized() }.isFailure)
    }
}
