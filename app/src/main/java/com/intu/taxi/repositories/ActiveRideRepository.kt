package com.intu.taxi.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.models.ActiveRide
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ActiveRideRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val ACTIVE_RIDES_REF = "rides/activeRides"
    }
    
    fun getActiveRideByRequestId(requestId: String): Flow<ActiveRide?> = callbackFlow {
        val query = database.reference
            .child(ACTIVE_RIDES_REF)
            .orderByChild("requestId")
            .equalTo(requestId)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val ride = snapshot.children.firstOrNull()?.let { child ->
                    val data = child.value as? Map<String, Any>
                    data?.let { ActiveRide.fromMap(it) }
                }
                trySend(ride)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) { }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
    
    suspend fun createActiveRide(
        requestId: String,
        driverId: String,
        clientId: String,
        driverLocation: GeoPoint,
        clientLocation: GeoPoint,
        destination: GeoPoint,
        originAddress: String,
        destinationAddress: String,
        originLatitude: Double = 0.0,
        originLongitude: Double = 0.0,
        destinationLatitude: Double = 0.0,
        destinationLongitude: Double = 0.0
    ): Result<String> {
        return try {
            val rideId = database.reference.child(ACTIVE_RIDES_REF).push().key ?: return Result.failure(Exception("Failed to generate ride ID"))
            
            val activeRide = ActiveRide(
                rideId = rideId,
                requestId = requestId,
                driverId = driverId,
                clientId = clientId,
                driverLocation = driverLocation,
                clientLocation = clientLocation,
                destination = destination,
                originAddress = originAddress,
                destinationAddress = destinationAddress,
                status = "active",
                originLatitude = originLatitude,
                originLongitude = originLongitude,
                destinationLatitude = destinationLatitude,
                destinationLongitude = destinationLongitude
            )
            
            database.reference
                .child("$ACTIVE_RIDES_REF/$rideId")
                .setValue(activeRide.toMap())
                .await()
            
            Result.success(rideId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDriverLocation(rideId: String, location: GeoPoint): Result<Unit> {
        return try {
            val updates = mapOf(
                "driverLocation" to location,
                "updatedAt" to System.currentTimeMillis()
            )
            
            database.reference
                .child("$ACTIVE_RIDES_REF/$rideId")
                .updateChildren(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateClientLocation(rideId: String, location: GeoPoint): Result<Unit> {
        return try {
            val updates = mapOf(
                "clientLocation" to location,
                "updatedAt" to System.currentTimeMillis()
            )
            
            database.reference
                .child("$ACTIVE_RIDES_REF/$rideId")
                .updateChildren(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getActiveRide(rideId: String): Flow<ActiveRide?> = callbackFlow {
        val listener = database.reference
            .child("$ACTIVE_RIDES_REF/$rideId")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val ride = if (snapshot.exists()) {
                        val data = snapshot.value as? Map<String, Any>
                        data?.let { ActiveRide.fromMap(it) }
                    } else {
                        null
                    }
                    trySend(ride)
                }
                
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    // Handle error
                }
            })
        
        awaitClose {
            database.reference.child("$ACTIVE_RIDES_REF/$rideId").removeEventListener(listener)
        }
    }
    
    fun getUserActiveRide(userId: String): Flow<ActiveRide?> = callbackFlow {
        val listener = database.reference
            .child(ACTIVE_RIDES_REF)
            .orderByChild("driverId")
            .equalTo(userId)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val ride = snapshot.children.firstOrNull()?.let { child ->
                        val data = child.value as? Map<String, Any>
                        data?.let { ActiveRide.fromMap(it) }
                    }
                    trySend(ride)
                }
                
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    // Handle error
                }
            })
        
        awaitClose {
            database.reference.child(ACTIVE_RIDES_REF).removeEventListener(listener)
        }
    }
    
    suspend fun completeRide(rideId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "completed",
                "updatedAt" to System.currentTimeMillis()
            )
            
            database.reference
                .child("$ACTIVE_RIDES_REF/$rideId")
                .updateChildren(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun cancelRide(rideId: String): Result<Unit> {
        return try {
            database.reference
                .child("$ACTIVE_RIDES_REF/$rideId")
                .removeValue()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateRouteGeometry(rideId: String, geometry: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "routeGeometry" to geometry,
                "updatedAt" to System.currentTimeMillis()
            )
            
            database.reference
                .child("$ACTIVE_RIDES_REF/$rideId")
                .updateChildren(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}