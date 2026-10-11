package com.intu.taxi

import com.intu.taxi.models.DriverRequestOrder
import com.intu.taxi.models.DriverRequestQueue
import com.intu.taxi.models.DriverRequestSettings
import com.intu.taxi.models.DriverRideRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriverRequestQueueTest {
    // Conductor en Pucallpa; cada solicitud se aleja un poco más hacia el norte
    private val driverLat = -8.3791
    private val driverLng = -74.5539

    private fun request(id: String, fare: Double, northMeters: Double) = DriverRideRequest(
        requestId = id, estimatedPrice = fare,
        originLatitude = driverLat + northMeters / 111_320.0, originLongitude = driverLng
    )

    private val near = request("near", fare = 5.0, northMeters = 300.0)
    private val rich = request("rich", fare = 12.0, northMeters = 2_000.0)
    private val middle = request("middle", fare = 8.0, northMeters = 900.0)

    private fun ids(order: DriverRequestOrder, seen: Map<String, Long>, now: Long = 10_000L,
        requests: List<DriverRideRequest> = listOf(near, rich, middle)) =
        DriverRequestQueue.arrange(requests, seen, DriverRequestSettings(order, 30), now, driverLat, driverLng)
            .visible.map { it.request.requestId }

    @Test
    fun fareOrderPutsHigherPayingRequestFirstEvenIfItArrivedLater() {
        val seen = mapOf("near" to 1_000L, "middle" to 2_000L, "rich" to 9_000L)
        assertEquals(listOf("rich", "middle", "near"), ids(DriverRequestOrder.FARE, seen))
    }

    @Test
    fun equalFaresKeepArrivalOrder() {
        val first = request("first", 8.0, 1_500.0)
        val second = request("second", 8.0, 100.0)
        val seen = mapOf("first" to 1_000L, "second" to 2_000L)
        assertEquals(listOf("first", "second"),
            ids(DriverRequestOrder.FARE, seen, requests = listOf(second, first)))
    }

    @Test
    fun distanceOrderPutsClosestPickupFirst() {
        val seen = mapOf("near" to 9_000L, "middle" to 1_000L, "rich" to 2_000L)
        assertEquals(listOf("near", "middle", "rich"), ids(DriverRequestOrder.DISTANCE, seen))
    }

    @Test
    fun requestsExpireAfterTheTimeoutCountedFromWhenTheDriverSawThem() {
        val seen = mapOf("near" to 0L, "middle" to 20_000L, "rich" to 25_000L)
        val result = DriverRequestQueue.arrange(listOf(near, rich, middle), seen,
            DriverRequestSettings(DriverRequestOrder.FARE, 30), 30_000L, driverLat, driverLng)
        assertEquals(listOf("near"), result.expiredIds)
        assertEquals(listOf("rich" to 25, "middle" to 20), result.visible.map { it.request.requestId to it.secondsLeft })
    }

    @Test
    fun unseenRequestsStartWithTheFullTime() {
        val result = DriverRequestQueue.arrange(listOf(near), emptyMap(),
            DriverRequestSettings(DriverRequestOrder.FARE, 45), 50_000L, driverLat, driverLng)
        assertEquals(45, result.visible.single().secondsLeft)
    }

    @Test
    fun requestsWithPendingOfferDoNotExpire() {
        val result = DriverRequestQueue.arrange(listOf(near), mapOf("near" to 0L),
            DriverRequestSettings(DriverRequestOrder.FARE, 30), 90_000L, driverLat, driverLng, noTimeoutIds = setOf("near"))
        assertEquals(emptyList<String>(), result.expiredIds)
        assertNull(result.visible.single().secondsLeft)
    }

    @Test
    fun withoutDriverLocationDistanceOrderFallsBackToArrival() {
        val seen = mapOf("near" to 3_000L, "middle" to 1_000L, "rich" to 2_000L)
        val result = DriverRequestQueue.arrange(listOf(near, rich, middle), seen,
            DriverRequestSettings(DriverRequestOrder.DISTANCE, 30), 4_000L, null, null)
        assertEquals(listOf("middle", "rich", "near"), result.visible.map { it.request.requestId })
    }

    @Test
    fun unknownOrderCodeDefaultsToFare() {
        assertEquals(DriverRequestOrder.FARE, DriverRequestOrder.fromCode("newest"))
    }
}
