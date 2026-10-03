package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import org.json.JSONObject

data class RideSecuritySettings(val pinEnabled: Boolean)

class RideSecurityRepository {
    suspend fun get(): RideSecuritySettings = parse(SupabaseApi.request("POST", "rpc/admin_get_ride_security_settings"))

    suspend fun save(enabled: Boolean): RideSecuritySettings = parse(
        SupabaseApi.request("POST", "rpc/admin_set_ride_security_settings", JSONObject().put("p_pin_enabled", enabled)))

    private fun parse(text: String) = RideSecuritySettings(JSONObject(text).getBoolean("pin_enabled"))
}
