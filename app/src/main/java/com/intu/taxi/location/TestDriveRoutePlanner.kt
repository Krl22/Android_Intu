package com.intu.taxi.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** The road geometry is snapped to streets; QA movement still starts/ends at the requested pins. */
class TestDriveRoutePlanner(
    private val loadRoute: suspend (MapTestLocation, MapTestLocation) -> List<MapTestLocation>?
) {
    suspend fun plan(origin: MapTestLocation, target: MapTestLocation): List<MapTestLocation> {
        val road = try {
            withTimeoutOrNull(12_000) { loadRoute(origin, target) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
        return buildList {
            add(origin)
            road.orEmpty().forEach { if (last() != it) add(it) }
            if (last() != target || size == 1) add(target)
        }
    }
}
