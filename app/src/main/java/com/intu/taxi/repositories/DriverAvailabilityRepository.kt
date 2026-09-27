package com.intu.taxi.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.data.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class DriverAvailabilityRepository {
    suspend fun createAvailableDriver(location: GeoPoint, driverId: String? = FirebaseAuth.getInstance().currentUser?.uid) {
        require(driverId != null) { "Usuario no autenticado" }; setLocation(location, true)
    }
    suspend fun updateDriverLocation(location: GeoPoint, driverId: String? = FirebaseAuth.getInstance().currentUser?.uid) {
        require(driverId != null) { "Usuario no autenticado" }; setLocation(location, true)
    }
    suspend fun removeAvailableDriver(driverId: String? = FirebaseAuth.getInstance().currentUser?.uid) {
        if (driverId == null) return
        val row = SupabaseApi.rows("driver_locations?driver_id=eq.${SupabaseApi.encode(driverId)}&select=latitude,longitude&limit=1").optJSONObject(0) ?: return
        setLocation(GeoPoint(row.getDouble("latitude"), row.getDouble("longitude")), false)
    }
    suspend fun isDriverAvailable(driverId: String? = FirebaseAuth.getInstance().currentUser?.uid): Boolean {
        if (driverId == null) return false
        return SupabaseApi.rows("driver_locations?driver_id=eq.${SupabaseApi.encode(driverId)}&select=is_available&limit=1")
            .optJSONObject(0)?.optBoolean("is_available") == true
    }
    fun startLocationUpdates(location: GeoPoint, driverId: String? = FirebaseAuth.getInstance().currentUser?.uid) {
        if (driverId != null) CoroutineScope(Dispatchers.IO).launch { runCatching { updateDriverLocation(location, driverId) } }
    }
    private suspend fun setLocation(location: GeoPoint, available: Boolean) {
        SupabaseApi.rpc("set_driver_location", JSONObject().put("p_latitude", location.latitude)
            .put("p_longitude", location.longitude).put("p_heading", JSONObject.NULL).put("p_is_available", available))
    }
}
