package com.intu.taxi.models

import com.google.firebase.database.PropertyName
import com.google.firebase.database.Exclude
import com.mapbox.geojson.Point

data class RideRequest(
    @PropertyName("requestId")
    val requestId: String = "",
    
    @PropertyName("userId")
    val userId: String = "",
    
    @PropertyName("userName")
    val userName: String = "",
    
    @PropertyName("userPhone")
    val userPhone: String = "",
    
    @PropertyName("userPhotoUrl")
    val userPhotoUrl: String? = null,
    
    @PropertyName("originLatitude")
    val originLatitude: Double = 0.0,
    
    @PropertyName("originLongitude")
    val originLongitude: Double = 0.0,
    
    @PropertyName("originAddress")
    val originAddress: String = "",
    
    @PropertyName("destinationLatitude")
    val destinationLatitude: Double = 0.0,
    
    @PropertyName("destinationLongitude")
    val destinationLongitude: Double = 0.0,
    
    @PropertyName("destinationAddress")
    val destinationAddress: String = "",
    
    @PropertyName("distanceMeters")
    val distanceMeters: Double = 0.0,
    
    @PropertyName("durationSeconds")
    val durationSeconds: Double = 0.0,
    
    @PropertyName("estimatedPrice")
    val estimatedPrice: Double = 0.0,
    
    @PropertyName("rideType")
    val rideType: String = "",
    
    @PropertyName("paymentMethod")
    val paymentMethod: String = "efectivo", // efectivo, yape_plin
    
    @PropertyName("status")
    val status: String = "searching", // searching, accepted, cancelled, completed
    
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @PropertyName("driverId")
    val driverId: String? = null,
    
    @PropertyName("driverName")
    val driverName: String? = null,
    
    @PropertyName("driverPhone")
    val driverPhone: String? = null,
    
    @PropertyName("driverPhotoUrl")
    val driverPhotoUrl: String? = null,
    
    @PropertyName("driverLatitude")
    val driverLatitude: Double? = null,
    
    @PropertyName("driverLongitude")
    val driverLongitude: Double? = null
) {
    // Helper function to get origin as Point
    @Exclude
    fun getOriginPoint(): Point = Point.fromLngLat(originLongitude, originLatitude)
    
    // Helper function to get destination as Point
    @Exclude
    fun getDestinationPoint(): Point = Point.fromLngLat(destinationLongitude, destinationLatitude)
    
    // Helper function to get driver location as Point
    @Exclude
    fun getDriverPoint(): Point? = if (driverLatitude != null && driverLongitude != null) {
        Point.fromLngLat(driverLongitude, driverLatitude)
    } else null
    
    // Convert to Map for Firebase serialization
    @Exclude
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