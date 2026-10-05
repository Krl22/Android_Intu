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
                .put("lat", a.latitude).put("lng", a.longitude).put("published", a.published).put("sort_order", a.sortOrder)
                .put("city", a.city).put("offer_detail", a.offerDetail).put("offer_price", a.offerPrice ?: JSONObject.NULL)
                .put("demo_photo", a.demoPhoto.code).put("menu_items", businessMenuJson(a.menu)))))
    }
    private fun parse(data: JSONObject) = AdminBusinessState(data.getBoolean("enabled"), ads(data.optJSONArray("ads")),
        data.optJSONArray("couriers")?.let { rows -> (0 until rows.length()).map { i ->
            rows.getJSONObject(i).let { BusinessTestCourier(it.getString("id"), it.str("name"), it.str("plate"), it.getBoolean("selected")) }
        } }.orEmpty())
    private fun ads(rows: JSONArray?) = rows?.let { (0 until it.length()).map { i ->
        it.getJSONObject(i).let { a -> BusinessAd(a.getString("id"), a.getString("name"),
            BusinessCategory.entries.first { category -> category.code == a.getString("category") },
            a.getString("title"), a.getString("description"), a.str("image_url"), a.getString("address"),
            a.getDouble("lat"), a.getDouble("lng"), a.getBoolean("published"), a.getInt("sort_order"), a.getString("updated_at"),
            city = a.str("city"), offerDetail = a.str("offer_detail"), offerPrice = if (a.isNull("offer_price")) null else a.optDouble("offer_price").takeIf { it.isFinite() },
            demoPhoto = businessPhoto(a.str("demo_photo")), menu = parseBusinessMenu(a.optJSONArray("menu_items"))) }
    } }.orEmpty()
}

internal fun businessPhoto(code: String) = BusinessPhoto.entries.firstOrNull { it.code == code } ?: BusinessPhoto.NONE
internal fun businessMenuJson(menu: List<BusinessMenuItem>) = JSONArray().apply { menu.forEach { item ->
    put(JSONObject().put("id", item.id).put("name", item.name).put("description", item.description).put("price", item.price)
        .put("image_url", item.imageUrl).put("demo_photo", item.demoPhoto.code).put("available", item.available))
} }
internal fun parseBusinessMenu(rows: JSONArray?): List<BusinessMenuItem> = rows?.let { (0 until it.length()).map { i ->
    rows.getJSONObject(i).let { BusinessMenuItem(it.getString("id"), it.getString("name"), it.str("description"), it.getDouble("price"),
        it.str("image_url"), businessPhoto(it.str("demo_photo")), it.optBoolean("available", true)) }
} }.orEmpty()
