package com.intu.taxi.repositories

import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.auth.AuthRepository
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import com.intu.taxi.models.RideRequest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.time.Instant

class RideRequestRepository {
    suspend fun createRideRequest(
        originLatitude: Double, originLongitude: Double, originAddress: String,
        destinationLatitude: Double, destinationLongitude: Double, destinationAddress: String,
        distanceMeters: Double, durationSeconds: Double, estimatedPrice: Double,
        rideType: String, paymentMethod: String, routeGeometry: String? = null,
        delivery: com.intu.taxi.models.DeliveryDetails? = null,
        preferredVehicleBrand: String? = null,
        businessAdId: String? = null,
        businessAdUpdatedAt: String? = null
    ): Result<String> = runCatching {
        val vehicleType = com.intu.taxi.auth.DriverVehicleType.requireCode(rideType)
        check((vehicleType == "motorcycle") == (delivery != null)) { "Completa los datos del envío antes de solicitarlo." }
        require(businessAdId == null || delivery != null) { "Los negocios requieren envío en moto." }
        require(businessAdId == null || !businessAdUpdatedAt.isNullOrBlank()) { "Actualiza el anuncio antes de solicitar el envío." }
        require(preferredVehicleBrand == null || (vehicleType == "mototaxi" && preferredVehicleBrand in setOf("honda", "bajaj"))) {
            "Selecciona una opción de moto válida."
        }
        // El nombre del pasajero se copia del perfil de Supabase al crear el viaje
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Inicia sesión para continuar.")
        AuthRepository().syncProfileToSupabase(uid)
        if (delivery != null) {
            val details = delivery.normalized()
            val body = JSONObject()
                .put("p_destination_lat", destinationLatitude).put("p_destination_lng", destinationLongitude).put("p_destination_address", destinationAddress)
                .put("p_distance_meters", distanceMeters.toInt()).put("p_duration_seconds", durationSeconds.toInt())
                .put("p_route_polyline", routeGeometry ?: JSONObject.NULL)
                .put("p_payment_method", if (paymentMethod == "yape_plin") "yape_plin" else "efectivo")
                .put("p_details", JSONObject().put("recipient_name", details.recipientName).put("recipient_phone", details.recipientPhone)
                    .put("description", details.description).put("pickup_reference", details.pickupReference)
                    .put("delivery_reference", details.deliveryReference).put("payer", details.payer.code)
                    .put("small_package_confirmed", details.smallPackageConfirmed)
                    .put("business_ad_updated_at", businessAdUpdatedAt ?: JSONObject.NULL))
            // The business RPC owns the pickup; clients cannot substitute an address or point.
            if (businessAdId != null) body.put("p_ad_id", businessAdId)
            else body.put("p_origin_lat", originLatitude).put("p_origin_lng", originLongitude).put("p_origin_address", originAddress)
            return@runCatching SupabaseApi.rpc(if (businessAdId != null) "create_business_delivery_request" else "create_delivery_request", body).getString("id")
        }
        val body = JSONObject()
            .put("vehicle_type", vehicleType)
            .put("preferred_vehicle_brand", preferredVehicleBrand ?: JSONObject.NULL)
            .put("origin_lat", originLatitude).put("origin_lng", originLongitude)
            .put("origin_address", originAddress)
            .put("destination_lat", destinationLatitude).put("destination_lng", destinationLongitude)
            .put("destination_address", destinationAddress)
            .put("distance_meters", distanceMeters.toInt())
            .put("duration_seconds", durationSeconds.toInt())
            .put("payment_method", if (paymentMethod == "yape_plin") "yape_plin" else "efectivo")
        routeGeometry?.let { body.put("route_polyline", it) }
        val response = SupabaseApi.request("POST", "rides", body, "return=representation")
        org.json.JSONArray(response).getJSONObject(0).getString("id")
    }

    fun listenToRideRequest(requestId: String): Flow<RideRequest?> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val rows = SupabaseApi.rows("rides?id=eq.${SupabaseApi.encode(requestId)}&select=*&limit=1")
                emit(rows.optJSONObject(0)?.toRideRequest())
            } catch (_: Exception) { }
            delay(1_500)
        }
    }

    suspend fun cancelRideRequest(requestId: String): Result<Unit> = runCatching {
        SupabaseApi.rpc("cancel_ride", JSONObject().put("p_ride_id", requestId).put("p_reason", "cancelled_by_rider"))
        Unit
    }

    private fun JSONObject.toRideRequest() = RideRequest(
        requestId = getString("id"), userId = str("rider_id"), userName = str("rider_name"),
        userPhone = str("rider_phone"), userPhotoUrl = nullable("rider_photo_url"),
        originLatitude = optDouble("origin_lat"), originLongitude = optDouble("origin_lng"),
        originAddress = str("origin_address"), destinationLatitude = optDouble("destination_lat"),
        destinationLongitude = optDouble("destination_lng"), destinationAddress = str("destination_address"),
        distanceMeters = optDouble("distance_meters"), durationSeconds = optDouble("duration_seconds"),
        estimatedPrice = optDouble("estimated_fare"), rideType = str("vehicle_type"),
        paymentMethod = str("payment_method", "efectivo"), status = str("status", "searching"),
        createdAt = millis(nullable("requested_at")), updatedAt = millis(nullable("updated_at")),
        driverId = nullable("driver_id"), driverName = nullable("driver_name"),
        driverPhone = nullable("driver_phone"), driverPhotoUrl = nullable("driver_photo_url")
    )

    private fun JSONObject.nullable(key: String): String? = str(key).ifBlank { null }
    private fun millis(value: String?): Long =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
}
