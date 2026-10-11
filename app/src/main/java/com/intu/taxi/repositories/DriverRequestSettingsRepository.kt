package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.models.DriverRequestOrder
import com.intu.taxi.models.DriverRequestSettings
import org.json.JSONObject

class DriverRequestSettingsRepository {
    suspend fun get(): DriverRequestSettings = parse(SupabaseApi.rpc("get_driver_request_settings"))

    suspend fun save(settings: DriverRequestSettings): DriverRequestSettings = parse(SupabaseApi.rpc(
        "admin_set_driver_request_settings", JSONObject()
            .put("p_request_order", settings.order.code).put("p_timeout_seconds", settings.timeoutSeconds)))

    private fun parse(json: JSONObject) = DriverRequestSettings(
        order = DriverRequestOrder.fromCode(json.optString("request_order")),
        timeoutSeconds = json.optInt("timeout_seconds", DriverRequestSettings().timeoutSeconds)
    )
}
