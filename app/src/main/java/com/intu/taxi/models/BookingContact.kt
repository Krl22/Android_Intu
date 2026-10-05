package com.intu.taxi.models

import com.intu.taxi.auth.PhoneFormatter

/** Only the explicitly chosen phone contact is attached to this service. */
data class BookingContact(val name: String, val phone: String) {
    fun normalized(): BookingContact {
        require(name.trim().length in 2..100) { "Ingresa el nombre de la persona (2 a 100 caracteres)." }
        val mobile = PhoneFormatter.normalizeMobile(phone)
        require(mobile != null && mobile.matches(Regex("\\+519[0-9]{8}"))) { "Ingresa un celular peruano válido." }
        return copy(name = name.trim(), phone = mobile)
    }
}
