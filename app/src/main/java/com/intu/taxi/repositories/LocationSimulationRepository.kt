package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LocationSimulationAccess(
    val usersEnabled: Boolean,
    val allowed: Boolean,
    val isAdmin: Boolean = false,
    val adminBarEnabled: Boolean = false,
) {
    val showControls: Boolean get() = allowed && (!isAdmin || adminBarEnabled)
}

class LocationSimulationRepository {
    companion object {
        private val revision = MutableStateFlow(0L)
        val changes = revision.asStateFlow()
    }

    suspend fun get(): LocationSimulationAccess = parse(
        SupabaseApi.request("POST", "rpc/location_simulation_access"))

    suspend fun save(usersEnabled: Boolean): LocationSimulationAccess = parse(
        SupabaseApi.request("POST", "rpc/admin_set_location_simulation", JSONObject().put("p_users_enabled", usersEnabled)))
        .also { revision.update { it + 1 } }

    suspend fun saveAdminBar(enabled: Boolean): LocationSimulationAccess = parse(
        SupabaseApi.request("POST", "rpc/admin_set_simulation_bar", JSONObject().put("p_enabled", enabled)))
        .also { revision.update { it + 1 } }

    private fun parse(text: String) = JSONObject(text).let {
        LocationSimulationAccess(it.getBoolean("users_enabled"), it.getBoolean("allowed"),
            it.getBoolean("is_admin"), it.getBoolean("admin_bar_enabled"))
    }
}
