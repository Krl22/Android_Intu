package com.intu.taxi.ui.screens

import com.intu.taxi.ui.map.TripRoute
import com.mapbox.geojson.Point

/** Snapshot of the final pickup and destination; the route must start at this pickup. */
data class RideBooking(
    val origin: Point,
    val destination: Point,
    val route: TripRoute,
    val rideType: String,
    val paymentMethod: String,
    val delivery: com.intu.taxi.models.DeliveryDetails? = null,
    val preferredVehicleBrand: String? = null,
    val businessAdId: String? = null,
    val businessAdUpdatedAt: String? = null
) {
    val estimatedPrice: Double get() = com.intu.taxi.models.ServiceFare.withBrandPremium(
        com.intu.taxi.models.ServiceFare.estimate(route.distanceMeters, route.durationSeconds, delivery != null),
        preferredVehicleBrand)
}
