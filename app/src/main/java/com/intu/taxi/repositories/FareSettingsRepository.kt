package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.models.FareSettings
import org.json.JSONObject

class FareSettingsRepository {
    suspend fun get(): FareSettings = parse(SupabaseApi.request("POST", "rpc/get_fare_settings"))

    suspend fun save(settings: FareSettings): FareSettings {
        settings.validated()
        return parse(SupabaseApi.request("POST", "rpc/admin_set_fare_settings", JSONObject().put("p_settings", JSONObject()
            .put("base_fare", settings.baseFare).put("per_km", settings.perKm).put("per_minute", settings.perMinute)
            .put("min_fare", settings.minimumFare).put("honda_premium_percent", settings.hondaPremiumPercent)
            .put("rounding_step", settings.roundingStep).put("driver_price_offers_enabled", settings.driverPriceOffersEnabled))))
    }

    private fun parse(text: String): FareSettings = JSONObject(text).let {
        FareSettings(it.getDouble("base_fare"), it.getDouble("per_km"), it.getDouble("per_minute"),
            it.getDouble("min_fare"), it.getDouble("honda_premium_percent"), it.getDouble("rounding_step"),
            it.getBoolean("driver_price_offers_enabled")).validated()
    }
}
