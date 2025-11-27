# Arquitectura de Datos: Realtime DB vs PostgreSQL

## 📊 Visión General

Esta aplicación utiliza una arquitectura híbrida que combina Firebase Realtime Database para operaciones en tiempo real y PostgreSQL para almacenamiento persistente e histórico.

## 🔥 Firebase Realtime Database

### **Propósito**: Estado en tiempo real y comunicación

**Datos almacenados:**
- ✅ **Ride Requests** (Solicitudes de viaje) - Estado temporal
- ✅ **Ubicación en tiempo real** de conductores
- ✅ **Estado del viaje** (searching → accepted → in_progress → completed)
- ✅ **Comunicación driver-rider** (mensajes, notificaciones)

**Ciclo de vida:**
```
Creado → Searching → Accepted → In Progress → Completed → Migrado a PostgreSQL
```

**Estructura:**
```json
{
  "rides": {
    "requests": {
      "requestId": {
        "requestId": "uuid",
        "userId": "user123",
        "status": "searching|accepted|in_progress|completed|cancelled",
        "originLatitude": -33.4489,
        "originLongitude": -70.6693,
        "destinationLatitude": -33.4597,
        "destinationLongitude": -70.6292,
        "estimatedPrice": 2500.0,
        "createdAt": 1640995200000,
        "updatedAt": 1640995200000,
        "driverId": "driver456", // Solo cuando accepted
        "driverLocation": { // Actualización en tiempo real
          "latitude": -33.4500,
          "longitude": -70.6600
        }
      }
    }
  }
}
```

## 🐘 PostgreSQL (Futura implementación)

### **Propósito**: Historial persistente y análisis

**Datos almacenados:**
- ✅ **Historial completo de viajes** (incluyendo cancelados)
- ✅ **Análisis y reportes** (ganancias, frecuencia, rutas populares)
- ✅ **Datos de facturación** y pagos
- ✅ **Calificaciones y reseñas** históricas
- ✅ **Patrones de uso** y estadísticas

**Tablas principales:**
```sql
-- Viajes completados
CREATE TABLE rides (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255),
    driver_id VARCHAR(255),
    origin_lat DECIMAL(10,8),
    origin_lng DECIMAL(11,8),
    destination_lat DECIMAL(10,8),
    destination_lng DECIMAL(11,8),
    status VARCHAR(50),
    estimated_price DECIMAL(10,2),
    final_price DECIMAL(10,2),
    distance_meters INTEGER,
    duration_seconds INTEGER,
    created_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP
);

-- Historial de ubicaciones
CREATE TABLE location_history (
    id UUID PRIMARY KEY,
    ride_id UUID REFERENCES rides(id),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    timestamp TIMESTAMP
);

-- Pagos y facturación
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    ride_id UUID REFERENCES rides(id),
    amount DECIMAL(10,2),
    payment_method VARCHAR(50),
    status VARCHAR(50),
    transaction_id VARCHAR(255),
    created_at TIMESTAMP
);
```

## 🔄 Flujo de Migración

### **1. Crear viaje en Realtime DB**
```kotlin
// Cuando usuario confirma viaje
rideRequestRepository.createRideRequest(...)
```

### **2. Estado en tiempo real**
```kotlin
// Driver acepta
rideRequestRepository.acceptRideRequest(requestId, driverId, ...)

// Viaje en progreso
rideRequestRepository.updateRideRequestStatus(requestId, "in_progress")

// Viaje completado
rideRequestRepository.updateRideRequestStatus(requestId, "completed")
```

### **3. Cancelación (sin eliminar)**
```kotlin
// Usuario cancela
rideRequestRepository.cancelRideRequest(requestId) // Marca como "cancelled"
// Datos permanecen en Firebase para debugging
```

### **4. Migración a PostgreSQL**
```kotlin
// Servicio periódico (ej: cada hora)
// 1. Obtener rides completados/cancelados antiguos
// 2. Insertar en PostgreSQL
// 3. Eliminar de Firebase (opcional)
```

## 🧹 Limpieza de Datos

### **Función de limpieza incluida:**
```kotlin
// Limpia rides antiguos de Realtime DB
rideRequestRepository.cleanupOldRideRequests(olderThanMillis = 24 * 60 * 60 * 1000)
```

**Recomendaciones:**
- Ejecutar limpieza diaria de rides con más de 24h
- Mantener en Firebase solo viajes activos o recientes
- PostgreSQL es la fuente de verdad para historial

## 🎯 Ventajas de esta Arquitectura

### **Realtime DB:**
- ⚡ **Baja latencia** para estado en tiempo real
- 🔄 **Sincronización automática** entre dispositivos
- 📱 **Funciona offline** y sincroniza después
- 💰 **Económico** para datos pequeños y frecuentes

### **PostgreSQL:**
- 📊 **Consultas complejas** y análisis
- 💾 **Integridad referencial** y transacciones
- 📈 **Escalabilidad** para grandes volúmenes
- 🔍 **Búsquedas avanzadas** e índices

## 🚀 Próximos Pasos

1. **Implementar PostgreSQL backend** con FastAPI
2. **Crear servicio de migración** de Firebase a PostgreSQL
3. **Agregar triggers** para limpieza automática
4. **Implementar dashboard** de analytics con datos de PostgreSQL

## 💡 Notas de Implementación

- Los rides cancelados permanecen en Firebase con estado "cancelled"
- La limpieza es opcional y puede ser programada
- PostgreSQL será la fuente principal para reportes y análisis
- Firebase mantiene la experiencia en tiempo real del usuario