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
class AdminLocationController(private val checkAdmin: suspend () -> Boolean) {
    private val active = MutableStateFlow<TestLocation?>(null)
    val preset = active.asStateFlow()
    private var accountUid: String? = null
    private var generation = 0L

    fun effectiveLocation(real: GeoPoint?): GeoPoint? = active.value?.let {
        GeoPoint(it.latitude, it.longitude)
    } ?: real

    @Synchronized fun updateAccount(uid: String?) {
        if (accountUid != uid) {
            accountUid = uid
            generation++
            active.value = null
        }
    }

    suspend fun activate(preset: TestLocation) {
        val session = synchronized(this) {
            check(accountUid != null) { "Inicia sesión para simular tu ubicación." }
            generation
        }
        check(checkAdmin()) { "Esta función solo está disponible para administradores." }
        synchronized(this) {
            check(generation == session && accountUid != null) { "Tu sesión cambió. Intenta de nuevo." }
            active.value = preset
        }
    }

    suspend fun recheckPermission() {
        val session = synchronized(this) { if (active.value == null) return else generation }
        val allowed = checkAdmin()
        synchronized(this) { if (generation == session && !allowed) clear() }
    }

    @Synchronized fun clear() {
        // Also invalidate any activation still waiting for a server response.
        generation++
        active.value = null
    }
}
