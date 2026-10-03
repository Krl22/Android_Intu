package com.intu.taxi.repositories

import android.content.Context
import com.intu.taxi.BuildConfig
import com.intu.taxi.data.CatalogPlace
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

data class CatalogSnapshot(val revision: Long = -1, val places: List<CatalogPlace> = emptyList(), val checkedAt: Long = 0)

/** Public published catalog, separate from personal saved addresses. No keys or tokens are cached. */
class PlaceCatalogRepository(context: Context, cacheName: String = "intu_place_catalog") {
    private val preferences = context.applicationContext.getSharedPreferences(cacheName, Context.MODE_PRIVATE)
    private val cacheKey = "v1_${BuildConfig.SUPABASE_URL}"

    fun cached(): CatalogSnapshot = runCatching {
        val json = JSONObject(preferences.getString(cacheKey, null) ?: return CatalogSnapshot())
        CatalogSnapshot(json.getLong("revision"), parsePlaces(json.getJSONArray("places")), json.optLong("checked_at"))
    }.getOrDefault(CatalogSnapshot())

    suspend fun sync(
        force: Boolean = false,
        fetch: suspend (Long) -> JSONObject = { SupabaseApi.rpc("places_catalog_sync", JSONObject().put("p_revision", it)) }
    ): CatalogSnapshot = syncMutex.withLock {
        val old = cached()
        val now = System.currentTimeMillis()
        if (!force && old.revision >= 0 && now - old.checkedAt in 0 until 300_000) return@withLock old
        val response = fetch(old.revision)
        val changed = response.getBoolean("changed")
        val revision = response.getLong("revision")
        require(changed || revision == old.revision) { "Respuesta del catálogo incompleta." }
        val places = if (changed) parsePlaces(response.getJSONArray("places")) else old.places
        // Replace, never merge: unpublished and inactive places disappear on synchronization.
        preferences.edit().putString(cacheKey, JSONObject().put("revision", revision).put("checked_at", now)
            .put("places", JSONArray().apply { places.forEach { put(it.toJson()) } }).toString()).apply()
        CatalogSnapshot(revision, places, now)
    }

    suspend fun listAdmin(): List<CatalogPlace> = parsePlaces(SupabaseApi.rpc("admin_places_catalog").getJSONArray("places"))

    suspend fun save(place: CatalogPlace, isNew: Boolean): CatalogPlace {
        val json = place.toJson()
        if (isNew) json.remove("id")
        val result = parsePlace(SupabaseApi.rpc("admin_save_place", JSONObject().put("p_place", json)))
        // Re-check immediately when returning to Home after editing in this installation.
        preferences.edit().putString(cacheKey, JSONObject(preferences.getString(cacheKey, "{}") ?: "{}")
            .put("checked_at", 0).toString()).apply()
        return result
    }

    private companion object { val syncMutex = Mutex() }
}

private fun parsePlaces(rows: JSONArray): List<CatalogPlace> = (0 until rows.length()).map { parsePlace(rows.getJSONObject(it)) }

private fun parsePlace(row: JSONObject): CatalogPlace = CatalogPlace(
    id = row.getString("id"), name = row.getString("name"),
    aliases = row.optJSONArray("aliases")?.let { list -> (0 until list.length()).map { list.getString(it) } }.orEmpty(),
    category = row.str("category", "place"), locality = row.str("locality", "Satipo"), address = row.str("address"),
    latitude = row.getDouble("latitude"), longitude = row.getDouble("longitude"), status = row.str("status", "draft"),
    pickupVerified = row.optBoolean("pickup_verified"), source = row.str("source", "manual"),
    sourceUrl = row.str("source_url"), attribution = row.str("attribution"), license = row.str("license"),
    coordinateKind = row.str("coordinate_kind", "manual"), updatedAt = row.str("updated_at")
)

private fun CatalogPlace.toJson(): JSONObject = JSONObject().put("id", id).put("name", name)
    .put("aliases", JSONArray(aliases)).put("category", category).put("locality", locality).put("address", address)
    .put("latitude", latitude).put("longitude", longitude).put("status", status).put("pickup_verified", pickupVerified)
    .put("source", source).put("source_url", sourceUrl).put("attribution", attribution).put("license", license)
    .put("coordinate_kind", coordinateKind).put("updated_at", updatedAt)
