package com.intu.taxi.models

import com.intu.taxi.auth.PhoneFormatter

enum class DeliveryPayer(val code: String, val label: String) {
    SENDER("sender", "Quien envía · al recoger"),
    RECIPIENT("recipient", "Quien recibe · al entregar")
}

data class DeliveryDetails(
    val recipientName: String,
    val recipientPhone: String,
    val description: String,
    val pickupReference: String = "",
    val deliveryReference: String = "",
    val payer: DeliveryPayer = DeliveryPayer.SENDER,
    val paymentCollected: Boolean = false,
    val smallPackageConfirmed: Boolean = false,
    val businessName: String? = null,
    val businessItems: List<BusinessOrderItem> = emptyList(),
    val sender: BookingContact? = null
) {
    fun normalized(): DeliveryDetails {
        require(recipientName.trim().length in 2..100) { "Ingresa el nombre de quien recibe el paquete." }
        val phone = PhoneFormatter.normalizeMobile(recipientPhone)
        require(phone != null && phone.matches(Regex("\\+519[0-9]{8}"))) { "Ingresa un celular peruano válido para quien recibe." }
        require(description.trim().length in 3..280) { "Describe brevemente qué enviarás (3 a 280 caracteres)." }
        require(pickupReference.length <= 200 && deliveryReference.length <= 200) { "La referencia debe tener hasta 200 caracteres." }
        require(smallPackageConfirmed) { "Confirma que es un paquete pequeño y que solicitas solo transporte." }
        return copy(recipientName = recipientName.trim(), recipientPhone = phone, description = description.trim(),
            pickupReference = pickupReference.trim(), deliveryReference = deliveryReference.trim(), sender = sender?.normalized())
    }
}

object ServiceFare {
    /** Same rounding as Supabase: discount applies to all coefficients and the minimum. */
    fun estimate(distanceMeters: Double, durationSeconds: Double, delivery: Boolean = false): Double {
        val factor = if (delivery) java.math.BigDecimal("0.8") else java.math.BigDecimal.ONE
        val distance = java.math.BigDecimal.valueOf(distanceMeters.toInt().toLong()).divide(java.math.BigDecimal("1000"))
        val minutes = java.math.BigDecimal.valueOf(durationSeconds.toInt().toLong())
            .divide(java.math.BigDecimal("60"), 12, java.math.RoundingMode.HALF_UP)
        val fare = (java.math.BigDecimal("2.5") + distance + java.math.BigDecimal("0.1") * minutes) * factor
        return fare.max(java.math.BigDecimal("4") * factor).setScale(1, java.math.RoundingMode.HALF_UP).toDouble()
    }

    /** Honda mototaxis carry luggage: 12% over the already rounded fare, rounded again like Supabase. */
    fun withBrandPremium(fare: Double, preferredBrand: String?): Double =
        if (preferredBrand != "honda") fare
        else java.math.BigDecimal.valueOf(fare).multiply(java.math.BigDecimal("1.12"))
            .setScale(1, java.math.RoundingMode.HALF_UP).toDouble()
}
