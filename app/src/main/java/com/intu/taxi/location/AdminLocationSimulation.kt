package com.intu.taxi.location

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.repositories.AdminRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** App-specific QA location. It does not change the phone's GPS or other apps. */
object AdminLocationSimulation {
    private val controller = AdminLocationController { AdminRepository().isAdmin() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val preset = controller.preset
    private var initialized = false
    private var ownerUid: String? = null

    fun initialize() {
        if (initialized) return
        initialized = true
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (ownerUid != auth.currentUser?.uid) com.intu.taxi.driver.DriverSession.location.value = null
            ownerUid = auth.currentUser?.uid
            controller.updateAccount(ownerUid)
        }
        scope.launch {
            preset.collectLatest { active ->
                if (active == null) return@collectLatest
                while (true) {
                    delay(60_000)
                    try { controller.recheckPermission() }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { controller.clear() }
                }
            }
        }
    }

    suspend fun activate(preset: TestLocation) {
        initialize()
        // Firebase delivers its initial auth listener asynchronously.
        ownerUid = FirebaseAuth.getInstance().currentUser?.uid
        controller.updateAccount(ownerUid)
        controller.activate(preset)
    }

    fun clear() = controller.clear()

    fun currentPreset(): TestLocation? = preset.value.takeIf {
        ownerUid != null && ownerUid == FirebaseAuth.getInstance().currentUser?.uid
    }

    fun effectiveLocation(real: GeoPoint?): GeoPoint? = if (currentPreset() != null) controller.effectiveLocation(real) else real
}
