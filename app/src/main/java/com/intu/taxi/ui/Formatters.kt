package com.intu.taxi.ui

import java.util.Locale
import com.intu.taxi.auth.countryCodes

/** Número para mostrar en Cuenta, sin el prefijo de los países admitidos por la app. */
fun nationalPhoneForDisplay(phone: String): String {
    val trimmed = phone.trim()
    val digits = trimmed.filter(Char::isDigit)
    if (digits.isEmpty()) return ""
    // El perfil antiguo puede tener un celular peruano sin el signo +.
    if (!trimmed.startsWith("+")) return peruLocalPhone(trimmed)
    val prefix = countryCodes.asSequence().map { it.prefijo.removePrefix("+") }.distinct()
        .sortedByDescending { it.length }
        .firstOrNull { digits.startsWith(it) && digits.length > it.length }
    return prefix?.let { digits.removePrefix(it) } ?: trimmed
}

/** Monto en soles peruanos con punto decimal, p. ej. "S/ 9.10". */
fun formatSoles(amount: Double): String = "S/ " + String.format(Locale.US, "%.2f", amount)

/**
 * Celular peruano sin el código de país, solo dígitos: "+51 987 654 321" -> "987654321".
 * Es lo que se pega en Yape o Plin. Números de otros países se devuelven solo con sus dígitos.
 */
fun peruLocalPhone(phone: String): String {
    val digits = phone.filter(Char::isDigit)
    return if (digits.length == 11 && digits.startsWith("51")) digits.drop(2) else digits
}

/** Celular peruano para mostrar, sin +51 y en grupos: "987 654 321". */
fun formatPeruPhone(phone: String): String {
    val local = peruLocalPhone(phone)
    return if (local.length == 9) local.chunked(3).joinToString(" ") else local
}
