package com.intu.taxi.models

/** Brand preference is separate from the service's stable vehicle type. */
enum class MotoOption(val code: String, val label: String, val description: String,
    val vehicleType: String, val preferredBrand: String? = null, val delivery: Boolean = false) {
    HONDA("honda", "Mototaxi Honda", "Con maletero para equipaje", "mototaxi", "honda"),
    BAJAJ("bajaj", "Mototaxi Bajaj", "Para pasajeros", "mototaxi", "bajaj"),
    ANY("any", "Cualquier mototaxi", "Honda, Bajaj u otra marca", "mototaxi"),
    DELIVERY("delivery", "Moto para envíos", "Paquetes pequeños", "motorcycle", delivery = true);

    /** Price shown for this option; the Honda premium matches Supabase's rides_before_insert. */
    fun fare(mototaxiFare: Double, deliveryFare: Double, settings: FareSettings = FareSettings.Default): Double =
        if (delivery) deliveryFare else ServiceFare.withBrandPremium(mototaxiFare, preferredBrand, settings)

    companion object { fun fromCode(code: String?): MotoOption? = entries.firstOrNull { it.code == code } }
}
