package com.intu.taxi.models

enum class ServiceReportCategory(val code: String, val label: String) {
    MISCONDUCT("misconduct", "Mal trato o conducta inapropiada"),
    HARASSMENT("harassment", "Acoso, amenazas o discriminación"),
    FRAUD("fraud", "Estafa o cobro engañoso"),
    THEFT("theft", "Robo"),
    DANGEROUS_DRIVING("dangerous_driving", "Conducción peligrosa"),
    PAYMENT_DISPUTE("payment_dispute", "Problema con el pago o cobro adicional"),
    WRONG_VEHICLE("wrong_vehicle", "Conductor o vehículo diferente"),
    DAMAGED_PACKAGE("damaged_package", "Paquete dañado"),
    MISSING_PACKAGE("missing_package", "Paquete no entregado o perdido"),
    LOST_ITEM("lost_item", "Olvidé un objeto en el vehículo"),
    OTHER("other", "Otro problema");

    companion object {
        fun options(isDriver: Boolean, delivery: Boolean, finished: Boolean) = entries.filter {
            when (it) {
                DANGEROUS_DRIVING, WRONG_VEHICLE -> !isDriver
                DAMAGED_PACKAGE, MISSING_PACKAGE -> delivery
                LOST_ITEM -> !isDriver && !delivery && finished
                else -> true
            }
        }
        fun label(code: String) = entries.find { it.code == code }?.label ?: "Otro problema"
        fun validDescription(value: String) = value.trim().length in 10..2000
    }
}

data class ServiceReportMessage(val id: String, val body: String, val role: String, val mine: Boolean, val createdAt: String)

data class ServiceReport(
    val id: String, val rideId: String, val category: String, val description: String, val status: String,
    val lostState: String?, val adminResponse: String, val adminNote: String, val reporterRole: String,
    val createdAt: String, val updatedAt: String, val isReporter: Boolean, val canCheckItem: Boolean,
    val reporterName: String, val reportedName: String, val originAddress: String, val destinationAddress: String,
    val vehiclePlate: String, val serviceKind: String, val messages: List<ServiceReportMessage> = emptyList(),
) {
    val open get() = status in setOf("open", "in_review")
    val lostItem get() = category == "lost_item"
    val statusLabel get() = when (status) { "in_review" -> "En revisión"; "resolved" -> "Resuelto"; "dismissed" -> "Cerrado por el equipo"; else -> "Recibido" }
    val lostStateLabel get() = when (lostState) {
        "found" -> "El conductor encontró el objeto"
        "not_found" -> "El conductor revisó y no lo encontró"
        "returned" -> "Devolución confirmada por el pasajero"
        else -> "Por revisar en el vehículo"
    }
}
