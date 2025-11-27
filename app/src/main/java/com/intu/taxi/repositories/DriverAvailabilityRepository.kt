package com.intu.taxi.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.models.AvailableDriver
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DriverAvailabilityRepository {
    private val realtimeDb = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val AVAILABLE_DRIVERS_REF = "rides/AvailableDrivers"
        private const val USERS_COLLECTION = "users"
    }
    
    suspend fun createAvailableDriver(
        location: GeoPoint,
        driverId: String? = auth.currentUser?.uid
    ) {
        if (driverId == null) return
        try {
            // Obtener información del conductor desde Firestore
            val driverDoc = firestore.collection(USERS_COLLECTION).document(driverId).get().await()
            
            if (!driverDoc.exists()) {
                throw Exception("Driver document not found")
            }
            
            val driverData = driverDoc.data ?: return
            
            // Crear objeto AvailableDriver
            val availableDriver = AvailableDriver(
                driverId = driverId,
                location = location,
                vehicleType = driverData["vehicleType"] as? String ?: "",
                vehicleModel = driverData["vehicleModel"] as? String ?: "",
                vehiclePlate = driverData["vehiclePlate"] as? String ?: "",
                driverName = driverData["name"] as? String ?: "",
                driverPhone = driverData["phone"] as? String ?: "",
                rating = driverData["rating"] as? Double ?: 0.0,
                isAvailable = true,
                timestamp = System.currentTimeMillis()
            )
            
            realtimeDb.getReference(AVAILABLE_DRIVERS_REF)
                .child(driverId)
                .setValue(availableDriver.toMap())
                .await()
                
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun updateDriverLocation(
        location: GeoPoint,
        driverId: String? = auth.currentUser?.uid
    ) {
        if (driverId == null) return
        try {
            val updates = hashMapOf<String, Any>(
                "location" to hashMapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude
                ),
                "timestamp" to System.currentTimeMillis()
            )
            
            realtimeDb.getReference(AVAILABLE_DRIVERS_REF)
                .child(driverId)
                .updateChildren(updates)
                .await()
                
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun removeAvailableDriver(
        driverId: String? = auth.currentUser?.uid
    ) {
        if (driverId == null) return
        try {
            realtimeDb.getReference(AVAILABLE_DRIVERS_REF)
                .child(driverId)
                .removeValue()
                .await()
                
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun isDriverAvailable(
        driverId: String? = auth.currentUser?.uid
    ): Boolean {
        if (driverId == null) return false
        return try {
            val snapshot = realtimeDb.getReference(AVAILABLE_DRIVERS_REF)
                .child(driverId)
                .get()
                .await()
            
            snapshot.exists() && snapshot.child("isAvailable").getValue(Boolean::class.java) == true
        } catch (e: Exception) {
            false
        }
    }
    
    fun startLocationUpdates(
        location: GeoPoint,
        driverId: String? = auth.currentUser?.uid
    ) {
        if (driverId == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateDriverLocation(location, driverId)
            } catch (e: Exception) {
                // Manejar error de actualización
                e.printStackTrace()
            }
        }
    }
}