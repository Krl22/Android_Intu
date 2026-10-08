package com.intu.taxi.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.firestore.GeoPoint

sealed interface TestLocation {
    val label: String
    val latitude: Double
    val longitude: Double
}

data class MapTestLocation(override val latitude: Double, override val longitude: Double) : TestLocation {
    override val label: String = "Punto en el mapa"
    init {
        require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0) {
            "Elige una ubicación válida en el mapa."
        }
    }
}

enum class TestLocationPreset(override val label: String, override val latitude: Double, override val longitude: Double) : TestLocation {
    SATIPO("Satipo", -11.2521, -74.6382),
    // Centro de Río Negro, junto a la municipalidad (OSM way 956665921).
    RIO_NEGRO("Río Negro", -11.2088663, -74.6594027)
}

/** In-memory override, bound to one authenticated session; never saved to device preferences. */
class AdminLocationController(private val checkPermission: suspend () -> Boolean) {
    private val active = MutableStateFlow<TestLocation?>(null)
    val preset = active.asStateFlow()
    private val moving = MutableStateFlow(false)
    val isMoving = moving.asStateFlow()
    private var accountUid: String? = null
    private var generation = 0L

    @Synchronized fun movementRevision(): Long = generation

    fun effectiveLocation(real: GeoPoint?): GeoPoint? = active.value?.let {
        GeoPoint(it.latitude, it.longitude)
    } ?: real

    @Synchronized fun updateAccount(uid: String?) {
        if (accountUid != uid) {
            accountUid = uid
            generation++
            active.value = null
            moving.value = false
        }
    }

    suspend fun activate(preset: TestLocation) {
        val session = synchronized(this) {
            check(accountUid != null) { "Inicia sesión para simular tu ubicación." }
            generation
        }
        check(checkPermission()) { "Activa la barra de simulación en Admin → Seguridad. Los usuarios necesitan permiso del administrador." }
        synchronized(this) {
            check(generation == session && accountUid != null) { "Tu sesión cambió. Intenta de nuevo." }
            generation++
            moving.value = false
            active.value = preset
        }
    }

    /** A movement permit cannot survive manual relocation, pause, logout or permission revocation. */
    suspend fun beginMovement(): Long {
        val session = synchronized(this) {
            check(accountUid != null && active.value != null) { "Primero elige una ubicación de prueba." }
            generation
        }
        check(checkPermission()) { "No tienes permiso para simular la ubicación." }
        return synchronized(this) {
            check(generation == session && accountUid != null && active.value != null) { "Tu ubicación o sesión cambió. Intenta de nuevo." }
            generation++
            moving.value = true
            generation
        }
    }

    @Synchronized fun move(permit: Long, point: MapTestLocation): Boolean {
        if (generation != permit || !moving.value || accountUid == null) return false
        active.value = point
        return true
    }

    @Synchronized fun finishMovement(permit: Long) {
        if (generation == permit) moving.value = false
    }

    @Synchronized fun pauseMovement() {
        generation++
        moving.value = false
    }

    suspend fun recheckPermission() {
        val session = synchronized(this) { if (active.value == null) return else generation }
        val allowed = checkPermission()
        synchronized(this) { if (generation == session && !allowed) clear() }
    }

    @Synchronized fun clear() {
        // Also invalidate any activation still waiting for a server response.
        generation++
        active.value = null
        moving.value = false
    }
}
