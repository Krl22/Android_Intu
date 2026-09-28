package com.intu.taxi.models

import com.mapbox.geojson.Point

data class RideRequest(
    val requestId: String = "",
    
    val userId: String = "",
    
    val userName: String = "",
    
    val userPhone: String = "",
    
    val userPhotoUrl: String? = null,
    
    val originLatitude: Double = 0.0,
    
    val originLongitude: Double = 0.0,
    
    val originAddress: String = "",
    
    val destinationLatitude: Double = 0.0,
    
    val destinationLongitude: Double = 0.0,
    
    val destinationAddress: String = "",
    
    val distanceMeters: Double = 0.0,
    
    val durationSeconds: Double = 0.0,
    
    val estimatedPrice: Double = 0.0,
    
    val rideType: String = "",
    
    val paymentMethod: String = "efectivo", // efectivo, yape_plin
    
    val status: String = "searching", // searching, accepted, cancelled, completed
    
    val createdAt: Long = System.currentTimeMillis(),
    
    val updatedAt: Long = System.currentTimeMillis(),
    
    val driverId: String? = null,
    
    val driverName: String? = null,
    
    val driverPhone: String? = null,
    
    val driverPhotoUrl: String? = null,
    
    val driverLatitude: Double? = null,
    
    val driverLongitude: Double? = null
) {
    // Helper function to get origin as Point
    fun getOriginPoint(): Point = Point.fromLngLat(originLongitude, originLatitude)
    
    // Helper function to get destination as Point
    fun getDestinationPoint(): Point = Point.fromLngLat(destinationLongitude, destinationLatitude)
    
    // Helper function to get driver location as Point
    fun getDriverPoint(): Point? = if (driverLatitude != null && driverLongitude != null) {
        Point.fromLngLat(driverLongitude, driverLatitude)
    } else null
    
    // Convert to Map for Firebase serialization
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "requestId" to requestId,
            "userId" to userId,
            "userName" to userName,
            "userPhone" to userPhone,
            "userPhotoUrl" to userPhotoUrl,
            "originLatitude" to originLatitude,
            "originLongitude" to originLongitude,
            "originAddress" to originAddress,
            "destinationLatitude" to destinationLatitude,
            "destinationLongitude" to destinationLongitude,
            "destinationAddress" to destinationAddress,
            "distanceMeters" to distanceMeters,
            "durationSeconds" to durationSeconds,
            "estimatedPrice" to estimatedPrice,
            "rideType" to rideType,
            "paymentMethod" to paymentMethod,
            "status" to status,
            "driverId" to driverId,
            "driverName" to driverName,
            "driverPhone" to driverPhone,
            "driverPhotoUrl" to driverPhotoUrl,
            "driverLatitude" to driverLatitude,
            "driverLongitude" to driverLongitude,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
    
    companion object {
        fun fromMap(map: Map<String, Any>): RideRequest {
            return RideRequest(
                requestId = map["requestId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                userPhone = map["userPhone"] as? String ?: "",
                userPhotoUrl = map["userPhotoUrl"] as? String,
                originLatitude = map["originLatitude"] as? Double ?: 0.0,
                originLongitude = map["originLongitude"] as? Double ?: 0.0,
                originAddress = map["originAddress"] as? String ?: "",
                destinationLatitude = map["destinationLatitude"] as? Double ?: 0.0,
                destinationLongitude = map["destinationLongitude"] as? Double ?: 0.0,
                destinationAddress = map["destinationAddress"] as? String ?: "",
                distanceMeters = map["distanceMeters"] as? Double ?: 0.0,
                durationSeconds = map["durationSeconds"] as? Double ?: 0.0,
                estimatedPrice = map["estimatedPrice"] as? Double ?: 0.0,
                rideType = map["rideType"] as? String ?: "",
                paymentMethod = map["paymentMethod"] as? String ?: "efectivo",
                status = map["status"] as? String ?: "searching",
                createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = map["updatedAt"] as? Long ?: System.currentTimeMillis(),
                driverId = map["driverId"] as? String,
                driverName = map["driverName"] as? String,
                driverPhone = map["driverPhone"] as? String,
                driverPhotoUrl = map["driverPhotoUrl"] as? String,
                driverLatitude = map["driverLatitude"] as? Double,
                driverLongitude = map["driverLongitude"] as? Double
            )
        }
    }
}