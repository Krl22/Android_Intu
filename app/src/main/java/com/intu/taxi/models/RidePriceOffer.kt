package com.intu.taxi.models

import java.math.BigDecimal

data class RidePriceOffer(val id: String, val rideId: String, val amount: Double, val appFare: Double,
    val status: String, val driverName: String, val driverPhoto: String?, val vehicle: String)

fun parseDriverOffer(text: String, appFare: Double): Double {
    val amount = text.trim().replace(',', '.').toBigDecimalOrNull()
    require(amount != null && amount >= BigDecimal("0.01") && amount <= BigDecimal("9999.99") &&
        amount.stripTrailingZeros().scale() <= 2) { "Ingresa un precio entre S/ 0.01 y S/ 9999.99, con hasta dos decimales." }
    require(amount.compareTo(BigDecimal.valueOf(appFare)) != 0) { "La propuesta debe ser diferente al precio de la app." }
    return amount.toDouble()
}
