package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import com.intu.taxi.models.*
import org.json.JSONArray
import org.json.JSONObject

class BusinessRepository {
    suspend fun feed(): BusinessFeed {
        val data = SupabaseApi.rpc("business_feed")
        return BusinessFeed(data.getBoolean("enabled"), ads(data.optJSONArray("ads")))
    }
    suspend fun adminState(): AdminBusinessState = parse(SupabaseApi.rpc("admin_business_state"))
    suspend fun setEnabled(enabled: Boolean) = parse(SupabaseApi.rpc("admin_set_business_enabled", JSONObject().put("p_enabled", enabled)))
    suspend fun setCourier(id: String, selected: Boolean) = parse(SupabaseApi.rpc("admin_set_business_courier",
        JSONObject().put("p_driver_id", id).put("p_selected", selected)))
    suspend fun archive(ad: BusinessAd) = parse(SupabaseApi.rpc("admin_archive_business_ad",
        JSONObject().put("p_id", ad.id).put("p_updated_at", ad.updatedAt)))
    suspend fun save(ad: BusinessAd): AdminBusinessState {
        val a = ad.normalized()
        return parse(SupabaseApi.rpc("admin_save_business_ad", JSONObject()
            .put("p_id", a.id ?: JSONObject.NULL).put("p_updated_at", a.updatedAt ?: JSONObject.NULL)
            .put("p_ad", JSONObject().put("name", a.name).put("category", a.category.code).put("title", a.title)
                .put("description", a.description).put("image_url", a.imageUrl).put("address", a.address)
                .put("lat", a.latitude).put("lng", a.longitude).put("published", a.published).put("sort_order", a.sortOrder))))
    }
    private fun parse(data: JSONObject) = AdminBusinessState(data.getBoolean("enabled"), ads(data.optJSONArray("ads")),
        data.optJSONArray("couriers")?.let { rows -> (0 until rows.length()).map { i ->
            rows.getJSONObject(i).let { BusinessTestCourier(it.getString("id"), it.str("name"), it.str("plate"), it.getBoolean("selected")) }
        } }.orEmpty())
    private fun ads(rows: JSONArray?) = rows?.let { (0 until it.length()).map { i ->
        it.getJSONObject(i).let { a -> BusinessAd(a.getString("id"), a.getString("name"),
            BusinessCategory.entries.first { category -> category.code == a.getString("category") },
            a.getString("title"), a.getString("description"), a.str("image_url"), a.getString("address"),
            a.getDouble("lat"), a.getDouble("lng"), a.getBoolean("published"), a.getInt("sort_order"), a.getString("updated_at")) }
    } }.orEmpty()
}
