package com.intu.taxi.driver

import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes heartbeats and resolves the current location when sending, including stationary QA points. */
class DriverLocationPublisher(
    private val location: () -> GeoPoint?,
    private val activeRide: () -> String?,
    private val online: () -> Boolean,
    private val send: suspend (GeoPoint, String?) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()
    private var lastAttemptMs: Long? = null

    suspend fun publish(force: Boolean = false) = mutex.withLock {
        val point = location() ?: return@withLock
        val rideId = activeRide()
        if (rideId == null && !online()) return@withLock
        val now = clock()
        val interval = if (rideId != null) 2_000L else 10_000L
        if (!force && lastAttemptMs?.let { now - it < interval } == true) return@withLock
        lastAttemptMs = now
        send(point, rideId)
    }
}
