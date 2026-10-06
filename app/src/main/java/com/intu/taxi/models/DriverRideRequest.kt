package com.intu.taxi.models

data class DriverRideRequest(
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
    val paymentMethod: String = "",
    val status: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val serviceKind: String = "passenger",
    val delivery: DeliveryDetails? = null,
    val riderRating: ParticipantRating? = null,
    // Viaje pedido para otra persona: el chat llega a la cuenta que lo pidió
    val bookedForOther: Boolean = false
) {
    val isDelivery: Boolean get() = serviceKind == "delivery"
    // Función para calcular la distancia desde la ubicación actual del conductor
    fun calculateDistanceFrom(driverLatitude: Double, driverLongitude: Double): Double {
        val R = 6371e3 // Radio de la Tierra en metros
        val φ1 = Math.toRadians(driverLatitude)
        val φ2 = Math.toRadians(originLatitude)
        val Δφ = Math.toRadians(originLatitude - driverLatitude)
        val Δλ = Math.toRadians(originLongitude - driverLongitude)

        val a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                Math.cos(φ1) * Math.cos(φ2) *
                Math.sin(Δλ / 2) * Math.sin(Δλ / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

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
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): DriverRideRequest {
            return DriverRideRequest(
                requestId = map["requestId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                userPhone = map["userPhone"] as? String ?: "",
                userPhotoUrl = map["userPhotoUrl"] as? String,
                originLatitude = (map["originLatitude"] as? Number)?.toDouble() ?: 0.0,
                originLongitude = (map["originLongitude"] as? Number)?.toDouble() ?: 0.0,
                originAddress = map["originAddress"] as? String ?: "",
                destinationLatitude = (map["destinationLatitude"] as? Number)?.toDouble() ?: 0.0,
                destinationLongitude = (map["destinationLongitude"] as? Number)?.toDouble() ?: 0.0,
                destinationAddress = map["destinationAddress"] as? String ?: "",
                distanceMeters = (map["distanceMeters"] as? Number)?.toDouble() ?: 0.0,
                durationSeconds = (map["durationSeconds"] as? Number)?.toDouble() ?: 0.0,
                estimatedPrice = (map["estimatedPrice"] as? Number)?.toDouble() ?: 0.0,
                rideType = map["rideType"] as? String ?: "",
                paymentMethod = map["paymentMethod"] as? String ?: "",
                status = map["status"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
