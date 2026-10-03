package com.intu.taxi.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject

data class SavedPlace(val id: String, val name: String, val address: String, val latitude: Double, val longitude: Double) {
    init { require(name.isNotBlank() && latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0) }
}

/** Lugares elegidos por el usuario, separados por cuenta y guardados en este teléfono. */
class SavedPlaces(context: Context, uid: String) {
    private val prefs = context.getSharedPreferences("intu_saved_places", Context.MODE_PRIVATE)
    private val key = "places_$uid"
    init { require(uid.isNotBlank()) }
    fun read(): List<SavedPlace> = runCatching {
        val rows = JSONArray(prefs.getString(key, "[]"))
        (0 until rows.length()).map { i ->
            val r = rows.getJSONObject(i)
            SavedPlace(r.getString("id"), r.getString("name"), r.getString("address"), r.getDouble("lat"), r.getDouble("lng"))
        }
    }.getOrDefault(emptyList())
    val changes = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed -> if (changed == key) trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun save(place: SavedPlace) = write(read().filterNot { it.id == place.id } + place)
    fun remove(id: String) = write(read().filterNot { it.id == id })
    private fun write(places: List<SavedPlace>) {
        val rows = JSONArray()
        places.forEach { rows.put(JSONObject().put("id", it.id).put("name", it.name).put("address", it.address).put("lat", it.latitude).put("lng", it.longitude)) }
        prefs.edit().putString(key, rows.toString()).apply()
    }
}
