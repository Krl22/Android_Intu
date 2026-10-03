package com.intu.taxi

import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.driver.DriverLocationPublisher
import com.intu.taxi.location.AdminLocationController
import com.intu.taxi.location.TestLocationPreset
import com.intu.taxi.location.MapTestLocation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DriverTestLocationTest {
    @Test fun customPointIsPublishedWhileOnlineAndIsReplacedByGpsWhenCleared() = runBlocking {
        val controller = AdminLocationController { true }
        controller.updateAccount("admin")
        val real = GeoPoint(-12.0464, -77.0428)
        val sent = mutableListOf<GeoPoint>()
        val publisher = DriverLocationPublisher({ controller.effectiveLocation(real) }, { null }, { true },
            { point, _ -> sent += point })
        val custom = MapTestLocation(-11.2459123, -74.6267251)
        controller.activate(custom)
        publisher.publish(force = true)
        assertEquals(GeoPoint(custom.latitude, custom.longitude), sent.last())
        controller.activate(MapTestLocation(-11.247, -74.631))
        publisher.publish(force = true)
        assertEquals(GeoPoint(-11.247, -74.631), sent.last())
        controller.clear()
        publisher.publish(force = true)
        assertEquals(real, sent.last())
    }
    @Test fun stationarySimulationKeepsOnlineHeartbeatsIgnoresGpsAndRestoresRealLocation() = runBlocking {
        val controller = AdminLocationController { true }
        controller.updateAccount("admin")
        var gps: GeoPoint? = GeoPoint(-12.0464, -77.0428)
        var now = 0L
        var ride: String? = null
        var online = true
        val sent = mutableListOf<Pair<GeoPoint, String?>>()
        val publisher = DriverLocationPublisher({ controller.effectiveLocation(gps) }, { ride }, { online },
            { point, rideId -> sent += point to rideId }, { now })
        controller.activate(TestLocationPreset.SATIPO)
        val satipo = GeoPoint(TestLocationPreset.SATIPO.latitude, TestLocationPreset.SATIPO.longitude)
        publisher.publish()
        gps = GeoPoint(-12.0500, -77.0500) // The phone is still moving in Lima.
        now = 9_999
        publisher.publish()
        assertEquals(1, sent.size)
        now = 10_000
        publisher.publish() // Heartbeat without any GPS movement callback.
        assertEquals(listOf(satipo to null, satipo to null), sent)
        ride = "test-ride"
        now = 12_000
        publisher.publish()
        assertEquals(satipo to ride, sent.last())
        controller.activate(TestLocationPreset.RIO_NEGRO)
        publisher.publish(force = true)
        assertEquals(GeoPoint(TestLocationPreset.RIO_NEGRO.latitude, TestLocationPreset.RIO_NEGRO.longitude), sent.last().first)
        controller.clear()
        publisher.publish(force = true)
        assertEquals(gps, sent.last().first)
        val count = sent.size
        ride = null
        online = false
        now = 30_000
        publisher.publish(force = true)
        assertEquals("Offline must not advertise availability", count, sent.size)
    }

    @Test fun queuedHeartbeatResolvesTheNewCityAfterAnInFlightRequestFinishes() = runBlocking {
        val controller = AdminLocationController { true }
        controller.updateAccount("admin")
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<Unit>()
        val sent = mutableListOf<GeoPoint>()
        val publisher = DriverLocationPublisher({ controller.effectiveLocation(null) }, { null }, { true },
            { point, _ ->
                sent += point
                if (sent.size == 1) { started.complete(Unit); response.await() }
            })
        controller.activate(TestLocationPreset.SATIPO)
        val first = async { publisher.publish(force = true) }
        started.await()
        val queued = async(start = CoroutineStart.UNDISPATCHED) { publisher.publish(force = true) }
        controller.activate(TestLocationPreset.RIO_NEGRO)
        response.complete(Unit)
        first.await()
        queued.await()
        assertEquals(GeoPoint(TestLocationPreset.SATIPO.latitude, TestLocationPreset.SATIPO.longitude), sent[0])
        assertEquals(GeoPoint(TestLocationPreset.RIO_NEGRO.latitude, TestLocationPreset.RIO_NEGRO.longitude), sent[1])
    }
}
