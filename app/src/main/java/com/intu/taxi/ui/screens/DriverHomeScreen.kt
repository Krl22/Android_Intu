package com.intu.taxi.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.intu.taxi.R
import com.intu.taxi.ui.map.rememberMapViewWithLifecycle
import com.intu.taxi.repositories.DriverAvailabilityRepository
import com.intu.taxi.repositories.DriverRideRequestRepository
import com.intu.taxi.repositories.ActiveRideRepository
import com.intu.taxi.ui.components.IncomingRideRequestCard
import com.intu.taxi.models.DriverRideRequest
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.geojson.LineString
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import androidx.compose.animation.fadeOut
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlin.math.max
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

@Composable
fun DriverHomeScreen(
    padding: PaddingValues,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {}
) {
    val mapboxToken = stringResource(id = R.string.mapbox_access_token)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driverAvailabilityRepository = remember { DriverAvailabilityRepository() }
    val driverRideRequestRepository = remember { DriverRideRequestRepository() }
    val activeRideRepository = remember { ActiveRideRepository() }

    var hasLocationPermission by rememberSaveable { mutableStateOf(false) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var incomingRideRequests by remember { mutableStateOf<List<DriverRideRequest>>(emptyList()) }
    var activeRideRequest by remember { mutableStateOf<DriverRideRequest?>(null) }
    var activeRideId by remember { mutableStateOf<String?>(null) }
    var activeRideStatus by remember { mutableStateOf("accepted") }
    var clientLocationMarker by remember { mutableStateOf<GeoPoint?>(null) }
    var clientMarkerAnnotation by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotation?>(null) }
    var mapViewRef by remember { mutableStateOf<com.mapbox.maps.MapView?>(null) }
    var polylineAnnotationManager by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager?>(null) }
    
    // Variables para la ruta calculada
    var routeGeometry by remember { mutableStateOf<String?>(null) }
    var routeDistance by remember { mutableStateOf(0.0) }
    var routeDuration by remember { mutableStateOf(0.0) }
    var isCalculatingRoute by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        runCatching { activeRideRepository.findOpenRideForDriver(uid) }
            .getOrNull()?.let { ride ->
                activeRideId = ride.rideId
                activeRideStatus = ride.status
                activeRideRequest = DriverRideRequest(
                    requestId = ride.rideId,
                    userId = ride.clientId,
                    userName = ride.riderName,
                    userPhone = ride.riderPhone,
                    originLatitude = ride.originLatitude,
                    originLongitude = ride.originLongitude,
                    originAddress = ride.originAddress,
                    destinationLatitude = ride.destinationLatitude,
                    destinationLongitude = ride.destinationLongitude,
                    destinationAddress = ride.destinationAddress,
                    estimatedPrice = ride.fare,
                    paymentMethod = ride.paymentMethod,
                    status = ride.status
                )
            }
    }

    // Función para limpiar la ruta y el marcador del pasajero del mapa
    val clearRouteAndPassengerMarker = remember {
        {
            try {
                println("DEBUG: Limpiando ruta y marcador del pasajero")
                
                // Limpiar la polilínea (ruta) - eliminar todas las anotaciones del manager
                polylineAnnotationManager?.let { manager ->
                    // Obtener todas las anotaciones y eliminarlas
                    val annotations = manager.annotations
                    if (annotations.isNotEmpty()) {
                        manager.deleteAll()
                        println("DEBUG: Ruta eliminada del mapa")
                    }
                }
                
                // Limpiar el marcador del pasajero
                pointAnnotationManager?.let { manager ->
                    clientMarkerAnnotation?.let { annotation ->
                        manager.delete(annotation)
                        println("DEBUG: Marcador del pasajero eliminado")
                    }
                }
                
                // Limpiar referencias
                clientMarkerAnnotation = null
                clientLocationMarker = null
                
            } catch (e: Exception) {
                println("DEBUG: Error al limpiar ruta y marcador: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Función para calcular distancia entre dos puntos geográficos (en metros)
    fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
        val earthRadius = 6371000.0 // Radio de la Tierra en metros
        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLon = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = sin(deltaLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }

    // Función para calcular ruta real usando Mapbox Directions API
    suspend fun calculateRouteWithDirectionsAPI(driverLocation: GeoPoint, clientLocation: GeoPoint): String? {
        return withContext(Dispatchers.IO) {
            try {
                val origin = Point.fromLngLat(driverLocation.longitude, driverLocation.latitude)
                val destination = Point.fromLngLat(clientLocation.longitude, clientLocation.latitude)
                
                val directionsUrl = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
                        "${origin.longitude()},${origin.latitude()};" +
                        "${destination.longitude()},${destination.latitude()}" +
                        "?alternatives=false&geometries=geojson&overview=full&access_token=$mapboxToken"
                
                val request = Request.Builder()
                    .url(directionsUrl)
                    .get()
                    .build()
                
                val response = OkHttpClient().newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    val routes = json.optJSONArray("routes")
                    val firstRoute = routes?.optJSONObject(0)
                    
                    if (firstRoute != null) {
                        // Obtener distancia y duración
                        routeDistance = firstRoute.optDouble("distance", 0.0) / 1000.0 // Convertir a km
                        routeDuration = firstRoute.optDouble("duration", 0.0) / 60.0 // Convertir a minutos
                        
                        val geometry = firstRoute.optJSONObject("geometry")
                        val coordinates = geometry?.optJSONArray("coordinates")
                        
                        if (coordinates != null && coordinates.length() > 0) {
                            val points = mutableListOf<Point>()
                            for (i in 0 until coordinates.length()) {
                                val coord = coordinates.getJSONArray(i)
                                val lon = coord.getDouble(0)
                                val lat = coord.getDouble(1)
                                points.add(Point.fromLngLat(lon, lat))
                            }
                            
                            // Guardar la geometría para dibujar
                            routeGeometry = geometry.toString()
                            
                            // Actualizar el viaje activo con la ruta
                            activeRideId?.let { rideId ->
                                activeRideRepository.updateRouteGeometry(rideId, geometry.toString())
                            }
                            
                            println("DEBUG: Ruta calculada exitosamente - Distancia: ${String.format("%.1f", routeDistance)}km, Duración: ${String.format("%.0f", routeDuration)}min")
                            return@withContext geometry.toString()
                        }
                    }
                }
                
                println("DEBUG: Error al calcular ruta - Código: ${response.code}, Body: $responseBody")
                return@withContext null
                
            } catch (e: Exception) {
                println("DEBUG: Excepción al calcular ruta: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    // Función fallback para dibujar línea recta
    fun drawSimpleRoute(mapView: com.mapbox.maps.MapView, driverLocation: GeoPoint, clientLocation: GeoPoint) {
        try {
            val originPoint = Point.fromLngLat(driverLocation.longitude, driverLocation.latitude)
            val destinationPoint = Point.fromLngLat(clientLocation.longitude, clientLocation.latitude)
            
            val annotationPlugin = mapView.annotations
            polylineAnnotationManager = annotationPlugin.createPolylineAnnotationManager()
            pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
            
            val polylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(listOf(originPoint, destinationPoint))
                .withLineColor("#FF0000") // Color rojo brillante para prueba
                .withLineWidth(6.0)
            
            polylineAnnotationManager?.create(polylineAnnotationOptions)
            
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(destinationPoint)
                .withIconImage(createPassengerIcon(context))
                .withIconSize(1.0)
            
            val annotation = pointAnnotationManager?.create(pointAnnotationOptions)
            clientMarkerAnnotation = annotation
            
            // Centrar cámara para mostrar la línea completa más arriba y con más zoom out
            val centerLat = (driverLocation.latitude + clientLocation.latitude) / 2
            val centerLng = (driverLocation.longitude + clientLocation.longitude) / 2
            
            // Aplicar offset vertical para mover la línea hacia arriba en la pantalla
            val latOffset = 0.018  // 2 km fijos hacia el norte
            val adjustedCenterLat = centerLat + latOffset
            val centerPoint = Point.fromLngLat(centerLng, adjustedCenterLat)
            
            // Calcular distancia para ajustar zoom con más zoom out
            val distance = calculateDistance(driverLocation, clientLocation)
            val zoomLevel = when {
                distance < 500 -> 15.0   // Distancia corta (más zoom out)
                distance < 1000 -> 14.0  // Distancia media (más zoom out)
                distance < 2000 -> 13.0  // Distancia larga (más zoom out)
                else -> 12.0             // Distancia muy larga (más zoom out)
            }
            
            println("DEBUG: Línea recta - Distancia: ${distance}m, Zoom: $zoomLevel, Offset: ${String.format("%.1f", latOffset * 111000)}m")
            
            val cameraOptions = CameraOptions.Builder()
                .center(centerPoint)
                .zoom(zoomLevel)
                .build()
            mapView.mapboxMap.setCamera(cameraOptions)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Función para calcular y mostrar ruta hacia el cliente
    fun drawRouteToClient(mapView: com.mapbox.maps.MapView, driverLocation: GeoPoint, clientLocation: GeoPoint) {
        scope.launch {
            try {
                // Actualizar la ubicación del cliente para el marcador
                clientLocationMarker = clientLocation
                isCalculatingRoute = true
                
                // Calcular ruta real usando Directions API
                val routeGeoJson = calculateRouteWithDirectionsAPI(driverLocation, clientLocation)
                
                if (routeGeoJson != null) {
                    // Parsear la geometría de la ruta
                    val json = JSONObject(routeGeoJson)
                    val coordinates = json.optJSONArray("coordinates")
                    
                    if (coordinates != null && coordinates.length() > 0) {
                        val routePoints = mutableListOf<Point>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lon = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            routePoints.add(Point.fromLngLat(lon, lat))
                        }
                        
                        // Centrar cámara para mostrar la ruta completa más arriba y con más zoom out
                        if (routePoints.isNotEmpty()) {
                            // Calcular límites de la ruta
                            val minLat = routePoints.minOf { it.latitude() }
                            val maxLat = routePoints.maxOf { it.latitude() }
                            val minLng = routePoints.minOf { it.longitude() }
                            val maxLng = routePoints.maxOf { it.longitude() }
                            
                            // Calcular centro de la ruta con offset vertical para mostrar más arriba
                            val centerLat = (minLat + maxLat) / 2
                            val centerLng = (minLng + maxLng) / 2
                            
                            // Aplicar offset vertical para mover la ruta hacia arriba en la pantalla
                            // Offset de ~200-300 metros hacia el norte para que aparezca más arriba
                            val latOffset = (maxLat - minLat) * 0.2 // 20% del alto de la ruta
                            val adjustedCenterLat = centerLat + latOffset
                            val centerPoint = Point.fromLngLat(centerLng, adjustedCenterLat)
                            
                            // Calcular dimensiones de la ruta
                            val routeWidth = calculateDistance(
                                GeoPoint(minLat, minLng), 
                                GeoPoint(minLat, maxLng)
                            )
                            val routeHeight = calculateDistance(
                                GeoPoint(minLat, minLng), 
                                GeoPoint(maxLat, minLng)
                            )
                            val maxDimension = maxOf(routeWidth, routeHeight)
                            
                            // Ajustar zoom con más zoom out para mejor visibilidad
                            val zoomLevel = when {
                                maxDimension < 500 -> 15.0   // Ruta muy corta (más zoom out)
                                maxDimension < 1000 -> 14.0  // Ruta corta (más zoom out)
                                maxDimension < 2000 -> 13.0  // Ruta media (más zoom out)
                                maxDimension < 5000 -> 12.0  // Ruta larga (más zoom out)
                                else -> 11.0                    // Ruta muy larga (más zoom out)
                            }
                            
                            println("DEBUG: Route dimensions - Width: ${String.format("%.1f", routeWidth)}km, Height: ${String.format("%.1f", routeHeight)}km, Zoom: $zoomLevel, Offset: ${String.format("%.1f", latOffset * 111000)}m")
                            
                            val cameraOptions = CameraOptions.Builder()
                                .center(centerPoint)
                                .zoom(zoomLevel)
                                .build()
                            mapView.mapboxMap.setCamera(cameraOptions)
                        }
                        
                        // Usar el plugin de anotaciones para dibujar la ruta y el marcador
                        try {
                            val annotationPlugin = mapView.annotations
                            
                            // Limpiar managers anteriores si existen
                            polylineAnnotationManager?.let {
                                // No hay método delete directo para polyline, pero podemos crear uno nuevo
                            }
                            pointAnnotationManager?.let {
                                clientMarkerAnnotation?.let { annotation ->
                                    it.delete(annotation)
                                }
                            }
                            
                            // Crear nuevos managers
                            polylineAnnotationManager = annotationPlugin.createPolylineAnnotationManager()
                            pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
                            
                            // Crear opciones para la polilínea con estilo mejorado (similar a HomeScreen)
                            println("DEBUG: Creando polilínea con color turquesa #0FB9B1")
                            val polylineAnnotationOptions = PolylineAnnotationOptions()
                                .withPoints(routePoints)
                                .withLineColor("#FF0000") // Color rojo brillante para prueba
                                .withLineWidth(6.0)
                            
                            // Crear la polilínea con la ruta real
                            val createdPolyline = polylineAnnotationManager?.create(polylineAnnotationOptions)
                            println("DEBUG: Polilínea creada con ID: ${createdPolyline?.id} y color: #FF0000 (rojo de prueba)")
                            
                            // Crear nuevo marcador del cliente en el destino
                            val destinationPoint = routePoints.last()
                            
                            // Crear icono de pasajero como bitmap drawable
                            val passengerIcon = createPassengerIcon(context)
                            
                            val pointAnnotationOptions = PointAnnotationOptions()
                                .withPoint(destinationPoint)
                                .withIconImage(passengerIcon)
                                .withIconSize(1.0)
                            
                            val annotation = pointAnnotationManager?.create(pointAnnotationOptions)
                            clientMarkerAnnotation = annotation
                            
                            println("DEBUG: Ruta calculada y marcador del cliente dibujados exitosamente - Distancia: ${String.format("%.1f", routeDistance)}km, Duración: ${String.format("%.0f", routeDuration)}min")
                        } catch (e: Exception) {
                            println("Error al agregar ruta al mapa: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } else {
                    // Fallback: dibujar línea recta si falla la API
                    println("DEBUG: Falló cálculo de ruta, usando línea recta")
                    drawSimpleRoute(mapView, driverLocation, clientLocation)
                }
                
                isCalculatingRoute = false
                
            } catch (e: Exception) {
                isCalculatingRoute = false
                e.printStackTrace()
            }
        }
    }

    // Funciones para manejar solicitudes
    fun handleAcceptRideRequest(request: DriverRideRequest) {
        scope.launch {
            try {
                val result = driverRideRequestRepository.acceptRideRequest(request.requestId)
                result.onSuccess { rideId ->
                    activeRideRequest = request
                    activeRideId = rideId
                    activeRideStatus = "accepted"
                    incomingRideRequests = emptyList()
                    // Dejar de buscar nuevas solicitudes
                    isSearching = false
                    // NO ocultar el BottomNavigationBar durante viajes activos
                    // onBottomBarVisibilityChanged(false)
                    
                    // Dibujar ruta hacia el cliente
                    currentLocation?.let { driverLoc ->
                        val clientLoc = com.google.firebase.firestore.GeoPoint(request.originLatitude, request.originLongitude)
                        // Llamar a drawRouteToClient inmediatamente si el mapView está disponible
                        val mapView = mapViewRef
                        if (mapView != null) {
                            drawRouteToClient(mapView, driverLoc, clientLoc)
                        }
                    }
                    
                    Toast.makeText(context, "Viaje aceptado exitosamente", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    println("DEBUG: Error al aceptar viaje - ${error.message}")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                println("DEBUG: Excepción al aceptar viaje - ${e.message}")
            }
        }
    }

    fun handleDeclineRideRequest(request: DriverRideRequest) {
        incomingRideRequests = incomingRideRequests.filter { it.requestId != request.requestId }
    }

    // Estados de animación para el header
    var headerVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    // Animación del fondo del header: cuando se está buscando, sube 25% de pantalla.
    // Como el header ocupa 50% de la pantalla, 25% de pantalla equivale a 0.5 del alto del header.
    val headerShiftFraction by animateFloatAsState(
        targetValue = if (isSearching) 0.5f else if (activeRideRequest != null) 0.3f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "headerShiftFraction"
    )

    // Animación de posición del botón: desde el header hacia el fondo.
    val buttonTravelFraction by animateFloatAsState(
        targetValue = if (isSearching) 0.95f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "buttonTravelFraction"
    )

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = fine || coarse
        
        // Iniciar animaciones de entrada
        headerVisible = true
        kotlinx.coroutines.delay(300)
        contentVisible = true
    }

    // Actualizar header cuando cambie el estado del viaje activo
    LaunchedEffect(activeRideRequest) {
        // Forzar actualización del header cuando cambie el estado
        headerVisible = true
        
        // Controlar visibilidad del BottomNavigationBar basado en el estado del viaje
        if (activeRideRequest != null) {
            // Hay un viaje activo - ocultar BottomNavigationBar
            onBottomBarVisibilityChanged(false)
        } else {
            // No hay viaje activo - mostrar BottomNavigationBar solo si no está buscando
            if (!isSearching) {
                onBottomBarVisibilityChanged(true)
            }
        }
    }

    // Listener para solicitudes entrantes cuando esté buscando
    LaunchedEffect(isSearching) {
        if (isSearching) {
            // Asegurar que BottomNavigationBar esté oculto cuando está buscando
            onBottomBarVisibilityChanged(false)
            
            // Iniciar escucha de solicitudes
            val job = scope.launch {
                driverRideRequestRepository.getActiveRideRequests().collect { requests ->
                    incomingRideRequests = requests
                    println("DEBUG: Se encontraron ${requests.size} solicitudes activas")
                }
            }
            // Esperar a que se detenga la búsqueda
            awaitCancellation()
            job.cancel()
        } else {
            incomingRideRequests = emptyList()
            // Solo mostrar BottomNavigationBar si no hay viaje activo
            if (activeRideRequest == null) {
                onBottomBarVisibilityChanged(true)
            }
        }
    }

    // Listener para viaje activo
    LaunchedEffect(activeRideId) {
        activeRideId?.let { rideId ->
            val job = scope.launch {
                activeRideRepository.getActiveRide(rideId).collect { activeRide ->
                    if (activeRide != null) {
                        activeRideStatus = activeRide.status
                        // Actualizar ubicación del cliente si cambia
                        activeRide.clientLocation?.let { clientLoc ->
                            currentLocation?.let { driverLoc ->
                                // Actualizar marcador del cliente en tiempo real
                                val previousClientLoc = clientLocationMarker
                                clientLocationMarker = clientLoc
                                
                                // Actualizar la ruta y el marcador si el mapView está disponible
                                mapViewRef?.let { mapView ->
                                    // Solo redibujar si la ubicación cambió significativamente (más de 10 metros)
                                    if (previousClientLoc == null ||
                                        calculateDistance(previousClientLoc, clientLoc) > 10) {
                                        drawRouteToClient(mapView, driverLoc, clientLoc)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            awaitCancellation()
            job.cancel()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val mapView = rememberMapViewWithLifecycle(accessToken = mapboxToken)
        mapViewRef = mapView
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { view ->
            view.mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                if (!hasLocationPermission) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                // Habilitar puck si hay permiso y personalizar icono de geolocalización
                view.location.updateSettings {
                    enabled = hasLocationPermission
                    locationPuck = LocationPuck2D(
                        bearingImage = ImageHolder.from(R.drawable.ic_moto)
                    )
                }
                // Ocultar regla de escala para un look limpio
                view.scalebar.enabled = false

                if (hasLocationPermission) {
                    // Listener para centrar la cámara inicialmente (solo una vez)
                    val initialPositionListener = object : OnIndicatorPositionChangedListener {
                        override fun onIndicatorPositionChanged(point: Point) {
                            view.mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(point)
                                    .zoom(14.0)
                                    .build()
                            )
                            // Guardar ubicación inicial
                            currentLocation = GeoPoint(point.latitude(), point.longitude())
                            
                            // Si hay un viaje activo, dibujar ruta hacia el cliente
                            if (activeRideRequest != null && currentLocation != null) {
                                val clientLoc = com.google.firebase.firestore.GeoPoint(
                                    activeRideRequest!!.originLatitude,
                                    activeRideRequest!!.originLongitude
                                )
                                drawRouteToClient(view, currentLocation!!, clientLoc)
                            }
                            
                            // Solo una vez para centrar cámara
                            view.location.removeOnIndicatorPositionChangedListener(this)
                        }
                    }
                    
                    // Listener continuo para actualizar ubicación cuando está buscando o en viaje activo
                    val continuousPositionListener = object : OnIndicatorPositionChangedListener {
                        override fun onIndicatorPositionChanged(point: Point) {
                            // Actualizar ubicación actual
                            currentLocation = GeoPoint(point.latitude(), point.longitude())
                            
                            // Si está buscando clientes, actualizar ubicación en tiempo real
                            if (isSearching) {
                                scope.launch {
                                    try {
                                        currentLocation?.let { location ->
                                            driverAvailabilityRepository.updateDriverLocation(location)
                                        }
                                    } catch (e: Exception) {
                                        // Silencioso para no interrumpir la experiencia del usuario
                                        e.printStackTrace()
                                    }
                                }
                            }
                            
                            // Si hay un viaje activo, actualizar ubicación del conductor en el viaje
                            if (activeRideRequest != null && activeRideId != null) {
                                scope.launch {
                                    try {
                                        currentLocation?.let { location ->
                                            activeRideRepository.updateDriverLocation(activeRideId!!, location)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                    
                    view.location.addOnIndicatorPositionChangedListener(initialPositionListener)
                    view.location.addOnIndicatorPositionChangedListener(continuousPositionListener)
                } else {
                    // Fallback: centrar en una ubicación por defecto
                    view.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(-73.9857, 40.7484))
                            .zoom(12.0)
                            .build()
                    )
                }
            }
        }

        // Header superior con el mismo fondo de gradiente + transparencia de HomeScreen
        AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn() + slideInVertically { -it / 2 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(if (activeRideRequest != null) 0.2f else 0.5f)
                    .drawBehind {
                        val teal = Color(0xFF08817E)
                        val indigo = Color(0xFF1E1F47)
                        // Translate el dibujo hacia arriba en función de la animación
                        val shiftY = size.height * headerShiftFraction
                        withTransform({ translate(left = 0f, top = -shiftY) }) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(teal, indigo),
                                    center = Offset(0.1f, 0.1f),
                                    radius = size.height * 0.9f
                                ),
                                size = Size(width = size.width, height = size.height)
                            )
                            withTransform({
                                scale(scaleX = 1.6f, scaleY = 1.0f, pivot = Offset.Zero)
                            }) {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colorStops = arrayOf(
                                            0.00f to Color.White.copy(alpha = 1.0f),
                                            0.70f to Color.White.copy(alpha = 1.0f),
                                            0.75f to Color.White.copy(alpha = 0.95f),
                                            0.80f to Color.White.copy(alpha = 0.85f),
                                            0.85f to Color.White.copy(alpha = 0.70f),
                                            0.90f to Color.White.copy(alpha = 0.45f),
                                            0.95f to Color.White.copy(alpha = 0.25f),
                                            1.00f to Color.Transparent
                                        ),
                                        center = Offset(0f, 0f),
                                        radius = max(size.width, size.height)
                                    ),
                                    size = Size(width = size.width, height = size.height),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn() + slideInVertically { -it / 4 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                activeRideRequest != null -> "Viaje en curso"
                                isSearching -> "Buscando clientes cerca…"
                                else -> "Modo conductor"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        
                        // Mostrar solicitudes entrantes cuando esté buscando (ahora arriba)
                        if (isSearching && incomingRideRequests.isNotEmpty() && activeRideRequest == null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Mostrar todas las solicitudes disponibles, apiladas verticalmente
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                incomingRideRequests.forEach { request ->
                                    IncomingRideRequestCard(
                                        request = request,
                                        currentLatitude = currentLocation?.latitude ?: 0.0,
                                        currentLongitude = currentLocation?.longitude ?: 0.0,
                                        onAccept = { 
                                            handleAcceptRideRequest(request)
                                        },
                                        onDecline = { 
                                            handleDeclineRideRequest(request)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Botón principal: aparece inicialmente en el header y viaja hacia el fondo al iniciar búsqueda
        AnimatedVisibility(
            visible = activeRideRequest == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val headerHeight = maxHeight * 0.5f
                val initialY = headerHeight * 0.35f
                val finalY = maxHeight - 172.dp - 56.dp
                val animatedY = lerp(initialY, finalY, buttonTravelFraction)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = animatedY)
                ) {
                    AnimatedGradientButton(
                        isSearching = isSearching,
                        onClick = {
                            scope.launch {
                                try {
                                    if (!isSearching) {
                                        // El conductor quiere empezar a buscar
                                        currentLocation?.let { location ->
                                            driverAvailabilityRepository.createAvailableDriver(location)
                                            isSearching = true
                                            onBottomBarVisibilityChanged(false) // OCULTAR BottomNavigationBar al buscar
                                            Toast.makeText(context, "Buscando clientes cerca...", Toast.LENGTH_SHORT).show()
                                        } ?: run {
                                            Toast.makeText(context, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        // El conductor quiere dejar de buscar
                                        driverAvailabilityRepository.removeAvailableDriver()
                                        isSearching = false
                                        onBottomBarVisibilityChanged(true) // MOSTRAR BottomNavigationBar al detener búsqueda
                                        Toast.makeText(context, "Búsqueda detenida", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        // Mostrar viaje activo si existe
        activeRideRequest?.let { request ->
            // Guardar referencia del mapView cuando esté disponible
            LaunchedEffect(Unit) {
                if (mapViewRef == null) {
                    // Obtener la referencia del mapView desde el AndroidView
                    // Esto se ejecutará después de que el mapView se cree
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // Encima de la barra de navegación del sistema, sea de gestos o de 3 botones
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                EnhancedActiveRideCard(
                    request = request,
                    status = activeRideStatus,
                    distance = routeDistance,
                    duration = routeDuration,
                    isCalculatingRoute = isCalculatingRoute,
                    onArrived = {
                        scope.launch {
                            val rideId = activeRideId ?: return@launch
                            val nextStatus = when (activeRideStatus) {
                                "accepted" -> "arrived"
                                "arrived" -> "in_progress"
                                "in_progress" -> "completed"
                                else -> return@launch
                            }
                            activeRideRepository.advanceRide(rideId, nextStatus)
                                .onSuccess {
                                    activeRideStatus = nextStatus
                                    val message = when (nextStatus) {
                                        "arrived" -> "Llegada confirmada"
                                        "in_progress" -> "Viaje iniciado"
                                        else -> "Pago confirmado. Viaje finalizado"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    if (nextStatus == "completed") {
                                        activeRideRequest = null
                                        activeRideId = null
                                        clearRouteAndPassengerMarker()
                                        onBottomBarVisibilityChanged(true)
                                    }
                                }
                                .onFailure { Toast.makeText(context, it.message ?: "No se pudo actualizar el viaje", Toast.LENGTH_LONG).show() }
                        }
                    },
                    onCancel = {
                        scope.launch {
                            activeRideId?.let { rideId ->
                                try {
                                    // Cancelar el viaje en Supabase
                                    activeRideRepository.cancelRide(rideId)
                                    println("DEBUG: Viaje activo $rideId cancelado en Supabase")
                                    
                                    // 3. Limpiar estado local
                                    activeRideRequest = null
                                    activeRideId = null
                                    routeDistance = 0.0
                                    routeDuration = 0.0
                                    routeGeometry = null
                                    
                                    // 4. Limpiar mapa
                                    clearRouteAndPassengerMarker()
                                    
                                    // 5. Mostrar BottomNavigationBar
                                    onBottomBarVisibilityChanged(true)
                                    
                                    Toast.makeText(context, "Viaje cancelado", Toast.LENGTH_SHORT).show()
                                    
                                } catch (e: Exception) {
                                    println("DEBUG: Error al cancelar viaje: ${e.message}")
                                    Toast.makeText(context, "Error al cancelar viaje: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AnimatedGradientButton(
    isSearching: Boolean,
    onClick: () -> Unit
) {
    // Animamos el ángulo del gradiente
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSearching) 6000 else 8000,
                easing = LinearEasing
            )
        ),
        label = "gradientShift"
    )

    // Progreso del radar (0..1), repetitivo
    val radarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing)
        ),
        label = "radarProgress"
    )

    val gradientColors = if (isSearching) {
        listOf(Color(0xFFFF5A5A), Color(0xFFD32F2F), Color(0xFFFF8A80))
    } else {
        listOf(Color(0xFF00E5C3), Color(0xFF00BFA5), Color(0xFF00695C))
    }

    val angleInRad = gradientShift * PI.toFloat() / 180f
    val startOffset = Offset(
        x = cos(angleInRad) * 300f,
        y = sin(angleInRad) * 300f
    )
    val endOffset = Offset(
        x = -cos(angleInRad) * 300f,
        y = -sin(angleInRad) * 300f
    )

    val pulseScale by animateFloatAsState(
        targetValue = if (isSearching) 1.1f else 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "pulseScale"
    )

    // Contenedor externo para dibujar el radar por fuera del botón
    Box(
        modifier = Modifier
            .size(140.dp)
            .drawBehind {
                if (isSearching) {
                    val center = this.center
                    val maxR = size.minDimension / 2f
                    val ringColor = Color(0xFFFF5A5A).copy(alpha = 0.45f)

                    // Tres anillos con desfase de fase para efecto radar
                    val phases = listOf(0f, 0.33f, 0.66f)
                    phases.forEach { phase ->
                        val p = ((radarProgress + phase) % 1f)
                        val radius = 6f + p * maxR
                        val alpha = (1f - p).coerceIn(0f, 1f) * 0.45f
                        drawCircle(
                            color = ringColor.copy(alpha = alpha),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 4f)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Botón circular
        Box(
            modifier = Modifier
                .size(90.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    shadowElevation = 12f
                    shape = CircleShape
                    clip = true
                }
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = startOffset,
                        end = endOffset
                    ),
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSearching) "Parar" else "Buscar",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// Función para crear icono de pasajero con diseño moderno similar a HomeScreen
fun createPassengerIcon(context: android.content.Context): android.graphics.Bitmap {
    val size = 100 // Tamaño más compacto del bitmap en píxeles
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Dibujar sombra suave
    paint.color = 0x20000000 // Negro con 12% de opacidad
    canvas.drawCircle(size * 0.5f, size * 0.85f, size * 0.08f, paint)
    
    // Crear pin de mapa estilo HomeScreen (más limpio y moderno)
    // Cuerpo principal del pin
    paint.color = 0xFF0FB9B1.toInt() // Color turquesa brillante como en HomeScreen
    val pinPath = android.graphics.Path()
    
    // Círculo superior
    pinPath.addCircle(size * 0.5f, size * 0.35f, size * 0.18f, android.graphics.Path.Direction.CW)
    
    // Punta inferior del pin
    pinPath.moveTo(size * 0.5f, size * 0.53f)
    pinPath.lineTo(size * 0.42f, size * 0.68f)
    pinPath.lineTo(size * 0.58f, size * 0.68f)
    pinPath.close()
    
    canvas.drawPath(pinPath, paint)
    
    // Dibujar borde blanco elegante
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = size * 0.04f
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawPath(pinPath, paint)
    
    // Dibujar círculo interno blanco
    paint.style = Paint.Style.FILL
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawCircle(size * 0.5f, size * 0.35f, size * 0.1f, paint)
    
    // Dibujar punto central turquesa
    paint.color = 0xFF0FB9B1.toInt()
    canvas.drawCircle(size * 0.5f, size * 0.35f, size * 0.05f, paint)
    
    // Añadir pequeño detalle de brillo
    paint.color = 0x40FFFFFF.toInt()
    canvas.drawCircle(size * 0.45f, size * 0.30f, size * 0.03f, paint)
    
    return bitmap
}

@Composable
fun EnhancedActiveRideCard(
    request: DriverRideRequest,
    status: String,
    distance: Double,
    duration: Double,
    isCalculatingRoute: Boolean,
    onArrived: () -> Unit,
    onCancel: () -> Unit
) {
    var isMinimized by remember { mutableStateOf(true) } // Inicialmente minimizado
    val primaryAction = when (status) {
        "arrived" -> "Iniciar viaje"
        "in_progress" -> "Confirmar pago y finalizar"
        else -> "Llegué"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isMinimized) 12.dp else 20.dp)
        ) {
            // Header con estado del viaje y botón de minimizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isMinimized) 32.dp else 50.dp)
                        .background(
                            color = Color(0xFF08817E).copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color(0xFF08817E),
                        modifier = Modifier.size(if (isMinimized) 16.dp else 24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(if (isMinimized) 8.dp else 12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (status) {
                            "arrived" -> "En el punto de recojo"
                            "in_progress" -> "Viaje en curso"
                            else -> "Viaje aceptado"
                        },
                        style = if (isMinimized) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1F47)
                    )
                    if (!isMinimized) {
                        Text(
                            text = when (status) {
                                "arrived" -> "Recoge al pasajero e inicia el viaje"
                                "in_progress" -> "Al terminar, confirma el pago recibido"
                                else -> "Dirígete al punto de recogida"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                // Botón de minimizar/maximizar
                Icon(
                    imageVector = if (isMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isMinimized) "Expandir" else "Minimizar",
                    tint = Color(0xFF08817E),
                    modifier = Modifier
                        .size(if (isMinimized) 20.dp else 24.dp)
                        .clickable { isMinimized = !isMinimized }
                )
            }
            
            // Contenido expandido
            if (!isMinimized) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Información de la ruta
                if (isCalculatingRoute) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF08817E)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calculando ruta...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                } else if (distance > 0 && duration > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Distancia
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = Color(0xFF08817E),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.1f", distance)} km",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E1F47)
                            )
                            Text(
                                text = "Distancia",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        
                        // Duración
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF08817E),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.0f", duration)} min",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E1F47)
                            )
                            Text(
                                text = "Tiempo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dirección de destino
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF08817E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (status == "in_progress") request.destinationAddress else request.originAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1E1F47),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Cobrar ${com.intu.taxi.ui.formatSoles(request.estimatedPrice)} · ${if (request.paymentMethod == "yape_plin") "Yape" else "Efectivo"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF08817E)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Botones de acción (siempre visibles)
            if (isMinimized) {
                // Solo botón de llegar cuando está minimizado - diseño más compacto
                Button(
                    onClick = onArrived,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF08817E),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = primaryAction,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // Botones completos cuando está expandido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onArrived,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF08817E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = primaryAction,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5252).copy(alpha = 0.1f),
                            contentColor = Color(0xFFFF5252)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text(
                            text = "Cancelar",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
