package com.intu.taxi.auth

object PhoneFormatter {
    fun sanitizeDigits(input: String): String = input.filter { it in '0'..'9' }

    /** Accept a local number or a pasted international number without duplicating the prefix. */
    fun nationalDigits(prefijo: String, input: String): String {
        val digits = sanitizeDigits(input)
        val cc = prefijo.removePrefix("+")
        val international = if (digits.startsWith("00")) digits.drop(2) else digits
        val maxLength = countryCodes.firstOrNull { it.prefijo == prefijo }?.nsnLength?.last ?: 15
        return if (international.startsWith(cc) && (input.trim().startsWith("+") || digits.startsWith("00") || international.length > maxLength))
            international.drop(cc.length) else digits
    }

    fun formatE164(prefijo: String, rawDigits: String): String {
        val digits = nationalDigits(prefijo, rawDigits)
        return if (digits.isNotEmpty()) "$prefijo$digits" else prefijo
    }

    fun isValid(prefijo: String, rawDigits: String): Boolean {
        val cc = countryCodes.firstOrNull { it.prefijo == prefijo } ?: return false
        val trimmed = rawDigits.trim()
        if (trimmed.any { it !in '0'..'9' && !it.isWhitespace() && it !in "+()- ." }) return false
        if (trimmed.startsWith("+") && !trimmed.startsWith(prefijo)) return false
        if (trimmed.startsWith("00") && !sanitizeDigits(trimmed).startsWith("00${prefijo.drop(1)}")) return false
        val digits = nationalDigits(prefijo, rawDigits)
        return digits.length in cc.nsnLength && (prefijo != "+51" || digits.matches(Regex("9[0-9]{8}")))
    }

    fun normalizeMobile(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        if (text.any { it !in '0'..'9' && !it.isWhitespace() && it !in "+()- ." }) return null
        if (text.count { it == '+' } > 1 || ('+' in text && !text.startsWith("+"))) return null
        val digits = sanitizeDigits(text)
        val e164 = when {
            text.startsWith("+") -> "+$digits"
            digits.startsWith("00") -> "+${digits.drop(2)}"
            digits.length == 9 -> "+51$digits"
            digits.length == 11 && digits.startsWith("51") -> "+$digits"
            else -> return null
        }
        if (!Regex("^\\+[1-9][0-9]{6,14}$").matches(e164)) return null
        if (e164.startsWith("+51") && !e164.matches(Regex("\\+519[0-9]{8}"))) return null
        return e164
    }
}
