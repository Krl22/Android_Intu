package com.intu.taxi.auth

/** La solicitud y el modo de uso son estados distintos; completar datos no aprueba al conductor. */
data class DriverAccess(
    val status: String?,
    val completeProfile: Boolean,
    val serviceAvailable: Boolean = true,
    val vehicleType: DriverVehicleType? = null
) {
    val canDrive: Boolean get() = status == "approved" && completeProfile && serviceAvailable
    val canApply: Boolean get() = status == null
    val message: String? get() = when (status) {
        "pending" -> "Solicitud en revisión. Puedes seguir usando Intu como pasajero."
        "rejected" -> "Solicitud no aprobada. Contacta al equipo de Intu desde Soporte."
        "suspended" -> "Tu acceso como conductor está suspendido. Contacta al equipo de Intu."
        "approved" -> when {
            !completeProfile -> "Tu solicitud está aprobada, pero faltan datos del vehículo."
            !serviceAvailable && vehicleType == DriverVehicleType.MOTORCYCLE ->
                "Tu solicitud de repartidor está aprobada. El servicio de reparto todavía no está habilitado."
            !serviceAvailable -> "Tu solicitud está aprobada. Este servicio todavía no está habilitado."
            else -> null
        }
        null -> null
        else -> "Tu acceso como conductor no está disponible."
    }
}
