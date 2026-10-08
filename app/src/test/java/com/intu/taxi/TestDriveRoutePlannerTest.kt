package com.intu.taxi

import com.intu.taxi.location.AdminLocationController
import com.intu.taxi.location.MapTestLocation
import com.intu.taxi.location.TestDriveRoutePlanner
import com.intu.taxi.location.TestLocationPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TestDriveRoutePlannerTest {
    private val origin = MapTestLocation(-11.252, -74.638)
    private val pickup = MapTestLocation(-11.249, -74.631)
    private val destination = MapTestLocation(-11.245, -74.629)

    @Test fun snappedRoadEndpointsDoNotBlockMovementAndArriveAtTheExactPickup() = runBlocking {
        val snappedStart = MapTestLocation(-11.251, -74.638)
        val roadCorner = MapTestLocation(-11.250, -74.633)
        val snappedEnd = MapTestLocation(-11.2495, -74.631)
        assertTrue(TestLocationPath.distance(origin, snappedStart) > 40)
        assertTrue(TestLocationPath.distance(pickup, snappedEnd) > 5)
        val route = TestDriveRoutePlanner { a, b ->
            assertEquals(origin, a)
            assertEquals(pickup, b)
            listOf(snappedStart, roadCorner, snappedEnd)
        }.plan(origin, pickup)
        assertEquals(listOf(origin, snappedStart, roadCorner, snappedEnd, pickup), route)
        val path = TestLocationPath(route)
        assertEquals(origin, path.pointAt(0.0))
        assertEquals(pickup, path.pointAt(path.lengthMeters))
    }

    @Test fun unavailableRoutesStillAllowQaMovementInsteadOfWaitingForever() = runBlocking {
        for (fail in listOf(false, true)) {
            val route = TestDriveRoutePlanner { _, _ ->
                if (fail) error("Mapbox no disponible") else null
            }.plan(origin, pickup)
            assertEquals(listOf(origin, pickup), route)
        }
        assertEquals(listOf(origin, origin), TestDriveRoutePlanner { _, _ -> emptyList() }.plan(origin, origin))
    }

    @Test fun destinationLegRequestsANewRouteInsteadOfReusingPickupGeometry() = runBlocking {
        val requested = mutableListOf<Pair<MapTestLocation, MapTestLocation>>()
        val planner = TestDriveRoutePlanner { a, b -> requested += a to b; listOf(a, b) }
        assertEquals(pickup, planner.plan(origin, pickup).last())
        assertEquals(destination, planner.plan(pickup, destination).last())
        assertEquals(listOf(origin to pickup, pickup to destination), requested)
    }

    @Test fun cancellingRoutePreparationDoesNotFallBackAndStartAnUnwantedDrive() = runBlocking {
        val result = runCatching { TestDriveRoutePlanner { _, _ -> throw CancellationException("paused") }.plan(origin, pickup) }
        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test fun pausingWhileARouteLoadsInvalidatesThePendingStartEvenAtTheSameLocation() = runBlocking {
        val controller = AdminLocationController { true }
        controller.updateAccount("admin")
        controller.activate(origin)
        val revision = controller.movementRevision()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<List<MapTestLocation>>()
        val planning = async { TestDriveRoutePlanner { _, _ -> started.complete(Unit); response.await() }.plan(origin, pickup) }
        started.await()
        controller.pauseMovement()
        response.complete(listOf(origin, pickup))
        planning.await()
        assertNotEquals(revision, controller.movementRevision())
        assertEquals(origin, controller.preset.value)
        assertFalse(controller.isMoving.value)
    }
}
