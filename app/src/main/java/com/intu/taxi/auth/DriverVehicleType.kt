package com.intu.taxi.auth

/** Stable database codes; display labels must never be sent as vehicle_type. */
enum class DriverVehicleType(val code: String, val label: String, val serviceLabel: String) {
    MOTOTAXI("mototaxi", "Mototaxi", "Transporte de pasajeros"),
    MOTORCYCLE("motorcycle", "Moto lineal", "Courier / repartidor");

    companion object {
        fun from(value: String?): DriverVehicleType? = when (value?.trim()?.lowercase(java.util.Locale.ROOT)) {
            "mototaxi" -> MOTOTAXI
            "motorcycle", "moto", "moto lineal" -> MOTORCYCLE
            else -> null
        }

        fun requireCode(value: String): String =
            requireNotNull(from(value)) { "Selecciona un tipo de vehículo válido." }.code
    }
}
