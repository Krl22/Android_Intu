package com.intu.taxi.auth

object PhoneFormatter {
    fun sanitizeDigits(input: String): String = input.filter { it.isDigit() }

    fun formatE164(prefijo: String, rawDigits: String): String {
        val digits = sanitizeDigits(rawDigits)
        return if (digits.isNotEmpty()) "$prefijo$digits" else prefijo
    }

    fun isValid(prefijo: String, rawDigits: String): Boolean {
        val cc = countryCodes.firstOrNull { it.prefijo == prefijo } ?: return false
        val len = sanitizeDigits(rawDigits).length
        return len in cc.nsnLength
    }
}