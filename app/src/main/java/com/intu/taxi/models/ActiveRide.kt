package com.intu.taxi.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.database.PropertyName

data class ActiveRide(
    @PropertyName("rideId")
    val rideId: String = "",
    
    @PropertyName("driverId")
    val driverId: String = "",
    
    @PropertyName("clientId")
    val clientId: String = "",
    
    @PropertyName("requestId")
    val requestId: String = "",
    
    @PropertyName("driverLocation")
    val driverLocation: GeoPoint? = null,
    
    @PropertyName("clientLocation")
    val clientLocation: GeoPoint? = null,
    
    @PropertyName("destination")
    val destination: GeoPoint? = null,
    
    @PropertyName("originAddress")
    val originAddress: String = "",
    
    @PropertyName("destinationAddress")
    val destinationAddress: String = "",
    
    @PropertyName("status")
    val status: String = "active", // active, completed, cancelled
    
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @PropertyName("routeGeometry")
    val routeGeometry: String? = null,
    
    @PropertyName("originLatitude")
    val originLatitude: Double = 0.0,
    
    @PropertyName("originLongitude")
    val originLongitude: Double = 0.0,
    
    @PropertyName("destinationLatitude")
    val destinationLatitude: Double = 0.0,
    
    @PropertyName("destinationLongitude")
    val destinationLongitude: Double = 0.0,
    val paymentMethod: String = "efectivo",
    val fare: Double = 0.0,
    val driverName: String = "",
    val driverPhone: String = "",
    val riderName: String = "",
    val riderPhone: String = "",
    val vehiclePlate: String = "",
    val vehicleDescription: String = "",
    val paymentConfirmed: Boolean = false
) {
    companion object {
        private fun readGeoPoint(value: Any?): GeoPoint? {
            return when (value) {
                is GeoPoint -> value
                is Map<*, *> -> {
                    val latAny = value["latitude"] ?: value["lat"]
                    val lonAny = value["longitude"] ?: value["lng"]
                    val lat = (latAny as? Number)?.toDouble()
                    val lon = (lonAny as? Number)?.toDouble()
                    if (lat != null && lon != null) GeoPoint(lat, lon) else null
                }
                else -> null
            }
        }
        fun fromMap(map: Map<String, Any>): ActiveRide {
            return ActiveRide(
                rideId = map["rideId"] as? String ?: "",
                driverId = map["driverId"] as? String ?: "",
                clientId = map["clientId"] as? String ?: "",
                requestId = map["requestId"] as? String ?: "",
                driverLocation = readGeoPoint(map["driverLocation"]),
                clientLocation = readGeoPoint(map["clientLocation"]),
                destination = readGeoPoint(map["destination"]),
                originAddress = map["originAddress"] as? String ?: "",
                destinationAddress = map["destinationAddress"] as? String ?: "",
                status = map["status"] as? String ?: "active",
                createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = map["updatedAt"] as? Long ?: System.currentTimeMillis(),
                routeGeometry = map["routeGeometry"] as? String,
                originLatitude = map["originLatitude"] as? Double ?: 0.0,
                originLongitude = map["originLongitude"] as? Double ?: 0.0,
                destinationLatitude = map["destinationLatitude"] as? Double ?: 0.0,
                destinationLongitude = map["destinationLongitude"] as? Double ?: 0.0
            )
        }
    }
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "rideId" to rideId,
            "driverId" to driverId,
            "clientId" to clientId,
            "requestId" to requestId,
            "driverLocation" to (driverLocation ?: ""),
            "clientLocation" to (clientLocation ?: ""),
            "destination" to (destination ?: ""),
            "originAddress" to originAddress,
            "destinationAddress" to destinationAddress,
            "status" to status,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "routeGeometry" to (routeGeometry ?: ""),
            "originLatitude" to originLatitude,
            "originLongitude" to originLongitude,
            "destinationLatitude" to destinationLatitude,
            "destinationLongitude" to destinationLongitude
        )
    }
}
