package com.intu.taxi.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.intu.taxi.models.RideRequest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class RideRequestRepository {
    
    private val database = FirebaseDatabase.getInstance("https://intu-e8403-default-rtdb.firebaseio.com/")
    private val rideRequestsRef = database.getReference("rides/requests")
    private val auth = FirebaseAuth.getInstance()
    
    suspend fun createRideRequest(
        originLatitude: Double,
        originLongitude: Double,
        originAddress: String,
        destinationLatitude: Double,
        destinationLongitude: Double,
        destinationAddress: String,
        distanceMeters: Double,
        durationSeconds: Double,
        estimatedPrice: Double,
        rideType: String,
        paymentMethod: String = "efectivo"
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                println("DEBUG REPO: ERROR - Usuario no autenticado")
                return Result.failure(Exception("Usuario no autenticado"))
            }
            
            println("DEBUG REPO: Usuario autenticado: ${currentUser.uid}")
            val requestId = UUID.randomUUID().toString()
            println("DEBUG REPO: Creando solicitud con ID: $requestId")
            
            val rideRequest = RideRequest(
                requestId = requestId,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Usuario",
                userPhone = currentUser.phoneNumber ?: "",
                userPhotoUrl = currentUser.photoUrl?.toString(),
                originLatitude = originLatitude,
                originLongitude = originLongitude,
                originAddress = originAddress,
                destinationLatitude = destinationLatitude,
                destinationLongitude = destinationLongitude,
                destinationAddress = destinationAddress,
                distanceMeters = distanceMeters,
                durationSeconds = durationSeconds,
                estimatedPrice = estimatedPrice,
                rideType = rideType,
                paymentMethod = paymentMethod,
                status = "searching",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Create the ride request in Firebase
            println("DEBUG REPO: Guardando en Firebase RTDB...")
            val rideRequestMap = rideRequest.toMap()
            println("DEBUG REPO: RideRequest data COMPLETO: $rideRequestMap")
            println("DEBUG REPO: Coordenadas específicas - originLat: ${rideRequestMap["originLatitude"]}, originLng: ${rideRequestMap["originLongitude"]}")
            println("DEBUG REPO: Coordenadas destino - destLat: ${rideRequestMap["destinationLatitude"]}, destLng: ${rideRequestMap["destinationLongitude"]}")
            try {
                rideRequestsRef.child(requestId).setValue(rideRequestMap).await()
                println("DEBUG REPO: Solicitud guardada exitosamente en Firebase")
                Result.success(requestId)
            } catch (e: Exception) {
                println("DEBUG REPO: ERROR de Firebase: ${e.message}")
                println("DEBUG REPO: Tipo de error: ${e.javaClass.simpleName}")
                throw e // Re-lanzar para que sea capturado por el catch externo
            }
        } catch (e: Exception) {
            println("DEBUG REPO: ERROR en createRideRequest: ${e.message}")
            Result.failure(e)
        }
    }

    // Escuchar cambios en una solicitud específica
    fun listenToRideRequest(requestId: String): Flow<RideRequest?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val data = snapshot.value as? Map<String, Any>
                    val rideRequest = data?.let { RideRequest.fromMap(it) }
                    trySend(rideRequest)
                } else {
                    trySend(null)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                println("DEBUG: Error al escuchar solicitud: ${error.message}")
                trySend(null)
            }
        }
        
        rideRequestsRef.child(requestId).addValueEventListener(listener)
        
        awaitClose {
            rideRequestsRef.child(requestId).removeEventListener(listener)
        }
    }
    
    suspend fun updateRideRequestStatus(requestId: String, status: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to status,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            
            rideRequestsRef.child(requestId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun cancelRideRequest(requestId: String): Result<Unit> {
        return try {
            println("DEBUG REPO: Cancelando ride request: $requestId")
            
            // Eliminar completamente el documento de Realtime Database
            rideRequestsRef.child(requestId).removeValue().await()
            println("DEBUG REPO: Solicitud eliminada exitosamente de Firebase RTDB")
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG REPO: Error al cancelar ride request: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun acceptRideRequest(requestId: String, driverId: String, driverName: String, driverPhone: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "accepted",
                "driverId" to driverId,
                "driverName" to driverName,
                "driverPhone" to driverPhone,
                "updatedAt" to ServerValue.TIMESTAMP
            )
            
            rideRequestsRef.child(requestId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Función para limpiar rides antiguos de Realtime DB (útil cuando migres a PostgreSQL)
    suspend fun cleanupOldRideRequests(olderThanMillis: Long = 24 * 60 * 60 * 1000): Result<Int> {
        return try {
            println("DEBUG REPO: Limpiando rides antiguos...")
            val cutoffTime = System.currentTimeMillis() - olderThanMillis
            val snapshot = rideRequestsRef.orderByChild("updatedAt").endAt(cutoffTime.toDouble()).get().await()
            
            var deletedCount = 0
            snapshot.children.forEach { child ->
                child.ref.removeValue().await()
                deletedCount++
            }
            
            println("DEBUG REPO: Se eliminaron $deletedCount rides antiguos")
            Result.success(deletedCount)
        } catch (e: Exception) {
            println("DEBUG REPO: Error al limpiar rides antiguos: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getRideRequestReference(requestId: String) = rideRequestsRef.child(requestId)
}