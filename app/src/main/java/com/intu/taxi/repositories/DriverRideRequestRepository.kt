package com.intu.taxi.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.intu.taxi.models.DriverRideRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DriverRideRequestRepository {
    
    private val database = FirebaseDatabase.getInstance("https://intu-e8403-default-rtdb.firebaseio.com/")
    private val rideRequestsRef = database.getReference("rides/requests")
    private val auth = FirebaseAuth.getInstance()
    private val activeRideRepository = ActiveRideRepository()
    
    // Obtener solicitudes de viaje activas en tiempo real
    fun getActiveRideRequests(): Flow<List<DriverRideRequest>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = mutableListOf<DriverRideRequest>()
                
                for (childSnapshot in snapshot.children) {
                    val requestMap = childSnapshot.value as? Map<String, Any>
                    requestMap?.let {
                        val request = DriverRideRequest.fromMap(it)
                        // Solo incluir solicitudes con status "searching" y que no hayan sido aceptadas
                        if (request.status == "searching") {
                            requests.add(request)
                        }
                    }
                }
                
                // Ordenar por distancia (las más cercanas primero)
                val sortedRequests = requests.sortedBy { it.createdAt }
                trySend(sortedRequests)
            }
            
            override fun onCancelled(error: DatabaseError) {
                // Manejar error si es necesario
                println("DEBUG: Error al obtener solicitudes activas: ${error.message}")
            }
        }
        
        // Escuchar solo solicitudes con status "searching"
        rideRequestsRef.orderByChild("status").equalTo("searching")
            .addValueEventListener(listener)
        
        awaitClose {
            rideRequestsRef.removeEventListener(listener)
        }
    }
    
    // Aceptar una solicitud de viaje
    suspend fun acceptRideRequest(requestId: String): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Usuario no autenticado"))
            }
            
            // Verificar que la solicitud aún esté disponible
            val snapshot = rideRequestsRef.child(requestId).get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("La solicitud ya no está disponible"))
            }
            
            val requestData = snapshot.value as? Map<String, Any>
            val currentStatus = requestData?.get("status") as? String
            
            if (currentStatus != "searching") {
                return Result.failure(Exception("La solicitud ya fue aceptada por otro conductor"))
            }
            
            // Obtener datos de la solicitud
            val clientId = requestData?.get("userId") as? String ?: return Result.failure(Exception("Cliente no encontrado"))
            
            // Validar coordenadas de origen con logging detallado
            val originLat = requestData["originLatitude"] as? Double
            val originLng = requestData["originLongitude"] as? Double
            if (originLat == null || originLng == null || originLat == 0.0) {
                // Logging completo con ID completo para debugging
                println("DEBUG: Error en origen - requestId: $requestId, originLatitude: $originLat, originLongitude: $originLng, requestData: $requestData")
                // Mensaje corto para Toast (máximo 8 caracteres del ID)
                val shortId = requestId.take(8)
                return Result.failure(Exception("Origen no válido – lat/lng faltantes en request $shortId"))
            }
            
            // Validar coordenadas de destino
            val destLat = requestData["destinationLatitude"] as? Double
            val destLng = requestData["destinationLongitude"] as? Double
            if (destLat == null || destLng == null || destLat == 0.0) {
                // Logging completo con ID completo para debugging
                println("DEBUG: Error en destino - requestId: $requestId, destLat: $destLat, destLng: $destLng, requestData: $requestData")
                // Mensaje corto para Toast (máximo 8 caracteres del ID)
                val shortId = requestId.take(8)
                return Result.failure(Exception("Destino no válido – lat/lng faltantes en request $shortId"))
            }
            val originAddress = requestData["originAddress"] as? String ?: ""
            val destinationAddress = requestData["destinationAddress"] as? String ?: ""
            
            // Actualizar la solicitud con la información del conductor
            val updates = mapOf(
                "status" to "accepted",
                "driverId" to currentUser.uid,
                "driverName" to (currentUser.displayName ?: "Conductor"),
                "driverPhone" to (currentUser.phoneNumber ?: ""),
                "updatedAt" to System.currentTimeMillis()
            )
            
            rideRequestsRef.child(requestId).updateChildren(updates).await()
            
            // Crear viaje activo para tracking en tiempo real
            val driverLocation = com.google.firebase.firestore.GeoPoint(0.0, 0.0) // Se actualizará con ubicación real
            val clientLocation = com.google.firebase.firestore.GeoPoint(originLat, originLng)
            val destination = com.google.firebase.firestore.GeoPoint(destLat, destLng)
            
            val createRideResult = activeRideRepository.createActiveRide(
                requestId = requestId,
                driverId = currentUser.uid,
                clientId = clientId,
                driverLocation = driverLocation,
                clientLocation = clientLocation,
                destination = destination,
                originAddress = originAddress,
                destinationAddress = destinationAddress,
                originLatitude = originLat,
                originLongitude = originLng,
                destinationLatitude = destLat,
                destinationLongitude = destLng
            )
            
            // Si el activeRide se creó exitosamente, limpiar los datos relacionados
            if (createRideResult.isSuccess) {
                println("DEBUG: ActiveRide creado exitosamente, limpiando datos relacionados...")
                
                // 1. Eliminar el request del cliente (ya fue aceptado)
                try {
                    rideRequestsRef.child(requestId).removeValue().await()
                    println("DEBUG: Request $requestId eliminado exitosamente")
                } catch (e: Exception) {
                    println("DEBUG: Error al eliminar request: ${e.message}")
                }
                
                // 2. Eliminar el AvailableDriver del conductor (ya está ocupado)
                try {
                    val availableDriversRef = database.getReference("rides/AvailableDrivers")
                    availableDriversRef.child(currentUser.uid).removeValue().await()
                    println("DEBUG: AvailableDriver ${currentUser.uid} eliminado exitosamente")
                } catch (e: Exception) {
                    println("DEBUG: Error al eliminar AvailableDriver: ${e.message}")
                }
            }
            
            createRideResult
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Rechazar una solicitud de viaje (simplemente dejarla disponible para otros conductores)
    suspend fun declineRideRequest(requestId: String): Result<Unit> {
        return Result.success(Unit) // No hacemos nada, la solicitud sigue disponible
    }
}