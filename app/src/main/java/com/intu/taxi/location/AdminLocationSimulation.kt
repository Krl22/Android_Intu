package com.intu.taxi.location

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.repositories.LocationSimulationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/** App-specific QA location. It does not change the phone's GPS or other apps. */
object AdminLocationSimulation {
    private val controller = AdminLocationController { canSimulate() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val preset = controller.preset
    val isMoving = controller.isMoving
    val automatic = AutomaticRideSimulation()
    private var driveJob: Job? = null
    private var initialized = false
    private var ownerUid: String? = null

    fun initialize() {
        if (initialized) return
        initialized = true
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (ownerUid != auth.currentUser?.uid) {
                pauseDrive()
                TestLocationMapSelection.cancel()
                com.intu.taxi.driver.DriverSession.location.value = null
            }
            ownerUid = auth.currentUser?.uid
            controller.updateAccount(ownerUid)
        }
        scope.launch {
            // Movement updates coordinates every two seconds; do not restart the permission timer.
            preset.map { it != null }.distinctUntilChanged().collectLatest { active ->
                if (!active) return@collectLatest
                while (true) {
                    delay(15_000)
                    try { controller.recheckPermission() }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { clear() }
                    if (controller.preset.value == null) automatic.stop()
                }
            }
        }
    }

    suspend fun activate(preset: TestLocation) {
        pauseDrive()
        initialize()
        // Firebase delivers its initial auth listener asynchronously.
        ownerUid = FirebaseAuth.getInstance().currentUser?.uid
        controller.updateAccount(ownerUid)
        controller.activate(preset)
    }

    /** The same server permission applies in both debug and release builds. */
    suspend fun canSimulate(): Boolean = LocationSimulationRepository().get().showControls

    fun movementRevision(): Long = controller.movementRevision()

    suspend fun startDrive(points: List<MapTestLocation>, speedKmh: Int, keepAutomatic: Boolean = false,
        expectedRevision: Long? = null) {
        check(expectedRevision == null || expectedRevision == controller.movementRevision()) {
            "La simulación se pausó o tu ubicación cambió. Inicia el recorrido de nuevo."
        }
        require(speedKmh in 5..120) { "Elige una velocidad entre 5 y 120 km/h." }
        val current = currentPreset() ?: error("Primero elige una ubicación de prueba.")
        val path = TestLocationPath(points)
        check(TestLocationPath.distance(current, points.first()) < 40) { "La ruta está actualizándose. Intenta de nuevo." }
        pauseDrive(keepAutomatic = keepAutomatic)
        val permit = controller.beginMovement()
        driveJob = scope.launch {
            try {
                var meters = 0.0
                while (meters < path.lengthMeters) {
                    delay(2_000)
                    meters = (meters + speedKmh / 3.6 * 2).coerceAtMost(path.lengthMeters)
                    if (!controller.move(permit, path.pointAt(meters))) return@launch
                }
            } finally { controller.finishMovement(permit) }
        }
    }

    fun pauseDrive(keepAutomatic: Boolean = false) {
        if (!keepAutomatic) automatic.stop()
        controller.pauseMovement()
        driveJob?.cancel()
        driveJob = null
    }

    fun clear() {
        pauseDrive()
        TestLocationMapSelection.cancel()
        controller.clear()
    }

    fun currentPreset(): TestLocation? = preset.value.takeIf {
        ownerUid != null && ownerUid == FirebaseAuth.getInstance().currentUser?.uid
    }

    fun effectiveLocation(real: GeoPoint?): GeoPoint? = if (currentPreset() != null) controller.effectiveLocation(real) else real
}
