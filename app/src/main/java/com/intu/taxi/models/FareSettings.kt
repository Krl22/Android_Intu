package com.intu.taxi.models

import java.math.BigDecimal
import java.math.RoundingMode

data class FareSettings(
    val baseFare: Double = 1.5,
    val perKm: Double = 1.0,
    val perMinute: Double = 0.1,
    val minimumFare: Double = 3.0,
    val hondaPremiumPercent: Double = 12.0,
    val roundingStep: Double = 0.1,
    val driverPriceOffersEnabled: Boolean = false
) {
    fun validated(): FareSettings {
        require(listOf(baseFare, perKm, perMinute, minimumFare).all { it.isFinite() && it in 0.0..9999.99 && decimalPlaces(it) <= 2 }) {
            "Los importes deben estar entre 0 y 9999.99, con hasta dos decimales."
        }
        require(hondaPremiumPercent.isFinite() && hondaPremiumPercent in 0.0..100.0 && decimalPlaces(hondaPremiumPercent) <= 2) {
            "El recargo Honda debe estar entre 0 y 100 %, con hasta dos decimales."
        }
        require(roundingStep.isFinite() && roundingStep in 0.01..10.0 && decimalPlaces(roundingStep) <= 2) {
            "El redondeo debe estar entre S/ 0.01 y S/ 10.00, con hasta dos decimales."
        }
        return this
    }

    fun round(amount: BigDecimal): BigDecimal = amount.divide(BigDecimal.valueOf(roundingStep), 0, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(roundingStep)).setScale(2, RoundingMode.HALF_UP)

    companion object {
        val Default = FareSettings()
        fun parse(values: List<String>): FareSettings {
            require(values.size == 6)
            val numbers = values.map { it.trim().replace(',', '.').toDoubleOrNull()
                ?: throw IllegalArgumentException("Completa todos los campos con números válidos.") }
            return FareSettings(numbers[0], numbers[1], numbers[2], numbers[3], numbers[4], numbers[5]).validated()
        }
        private fun decimalPlaces(value: Double) = BigDecimal.valueOf(value).stripTrailingZeros().scale()
    }
}
