package com.intu.taxi.auth

data class CountryCode(val nombre: String, val iso2: String, val prefijo: String, val nsnLength: IntRange)

val countryCodes: List<CountryCode> = listOf(
    CountryCode("Perú", "PE", "+51", 9..9),
    CountryCode("Estados Unidos", "US", "+1", 10..10),
    CountryCode("México", "MX", "+52", 10..10),
    CountryCode("Colombia", "CO", "+57", 10..10),
    CountryCode("Chile", "CL", "+56", 9..9),
    CountryCode("Argentina", "AR", "+54", 10..10),
    CountryCode("España", "ES", "+34", 9..9),
    CountryCode("Ecuador", "EC", "+593", 9..9),
    CountryCode("Bolivia", "BO", "+591", 8..8),
    CountryCode("Uruguay", "UY", "+598", 8..8),
    CountryCode("Paraguay", "PY", "+595", 9..9),
    CountryCode("Brasil", "BR", "+55", 10..11),
    CountryCode("Canadá", "CA", "+1", 10..10),
    CountryCode("Venezuela", "VE", "+58", 10..10),
    CountryCode("Panamá", "PA", "+507", 8..8),
    CountryCode("Costa Rica", "CR", "+506", 8..8),
    CountryCode("Guatemala", "GT", "+502", 8..8),
    CountryCode("Honduras", "HN", "+504", 8..8),
    CountryCode("El Salvador", "SV", "+503", 8..8),
    CountryCode("Nicaragua", "NI", "+505", 8..8)
)

fun findCountryByIso2(iso2: String): CountryCode? = countryCodes.firstOrNull { it.iso2.equals(iso2, ignoreCase = true) }
fun defaultCountry(): CountryCode = countryCodes.first() // Perú por defecto