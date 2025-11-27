package com.intu.taxi.models

import com.google.firebase.firestore.GeoPoint

/**
 * Modelo para representar un conductor disponible en Realtime Database
 * Se almacena en rides/AvailableDrivers/{driverId}
 */
data class AvailableDriver(
    val driverId: String = "",
    val location: GeoPoint? = null,
    val latitude: Double? = null, // Alternativa para location
    val longitude: Double? = null, // Alternativa para location
    val vehicleType: String = "",
    val vehicleModel: String = "",
    val vehiclePlate: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val rating: Double = 0.0,
    val isAvailable: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Constructor alternativo para facilitar la creación desde Firebase
     */
    constructor() : this(
        driverId = "",
        location = null,
        vehicleType = "",
        vehicleModel = "",
        vehiclePlate = "",
        driverName = "",
        driverPhone = "",
        rating = 0.0,
        isAvailable = true,
        timestamp = System.currentTimeMillis()
    )
    
    /**
     * Método para convertir a Map para Firebase
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "driverId" to driverId,
            "latitude" to location?.latitude,
            "longitude" to location?.longitude,
            "vehicleType" to vehicleType,
            "vehicleModel" to vehicleModel,
            "vehiclePlate" to vehiclePlate,
            "driverName" to driverName,
            "driverPhone" to driverPhone,
            "rating" to rating,
            "isAvailable" to isAvailable,
            "timestamp" to timestamp
        )
    }
    
    /**
     * Verificar si el conductor está activo (no expirado)
     * Por defecto, consideramos expirado después de 5 minutos sin actualización
     */
    fun isActive(): Boolean {
        val fiveMinutes = 5 * 60 * 1000 // 5 minutos en milisegundos
        return (System.currentTimeMillis() - timestamp) < fiveMinutes
    }
}