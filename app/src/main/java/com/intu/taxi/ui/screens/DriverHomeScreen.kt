package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.intu.taxi.ui.map.TripMap
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.runtime.collectAsState
import com.intu.taxi.location.AdminLocationSimulation
import com.intu.taxi.ui.map.MapLocationBinding
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
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
    // "En línea": se mantiene entre viajes y al reabrir la app, hasta que el conductor pulse "Parar"
    val driverPrefs = remember { context.getSharedPreferences("intu_driver", android.content.Context.MODE_PRIVATE) }
    var isSearching by rememberSaveable { mutableStateOf(driverPrefs.getBoolean("online", false)) }
    LaunchedEffect(isSearching) { driverPrefs.edit().putBoolean("online", isSearching).apply() }
    val testLocation by AdminLocationSimulation.preset.collectAsState()
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var incomingRideRequests by remember { mutableStateOf<List<DriverRideRequest>>(emptyList()) }
    // Solicitudes que el conductor rechazó: no se le vuelven a mostrar
    var declinedRequestIds by remember { mutableStateOf(setOf<String>()) }
    var activeRideRequest by remember { mutableStateOf<DriverRideRequest?>(null) }
    var activeRideId by remember { mutableStateOf<String?>(null) }
    var activeRideStatus by remember { mutableStateOf("accepted") }
    // Viaje recién terminado, para que el conductor califique al pasajero
    var rideToRate by remember { mutableStateOf<Pair<String, DriverRideRequest>?>(null) }
    // Siguiente viaje, aceptado mientras lleva a otro pasajero (como Uber)
    var queuedRideRequest by remember { mutableStateOf<DriverRideRequest?>(null) }
    var queuedRideId by remember { mutableStateOf<String?>(null) }
    // Último envío de ubicación a Supabase (sin estado de Compose para no recomponer en cada GPS)
    val lastLocationSentMs = remember { longArrayOf(0L) }
    // PIN de seguridad que el pasajero le dicta al conductor para iniciar el viaje
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isVerifyingPin by remember { mutableStateOf(false) }
    var isCheckingStartPin by remember { mutableStateOf(false) }
    var showDeliveryPaymentConfirmation by remember { mutableStateOf(false) }
    var deliveryActionBusy by remember { mutableStateOf(false) }
    var deliveryActionError by remember { mutableStateOf<String?>(null) }
    var deliveryConfirmationStatus by remember { mutableStateOf("") }
    // Punto objetivo del viaje: recojo del pasajero o, ya en viaje, el destino
    var clientLocationMarker by remember { mutableStateOf<GeoPoint?>(null) }
    var clientMarkerAnnotation by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotation?>(null) }
    var routeAnnotation by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation?>(null) }
    var mapViewRef by remember { mutableStateOf<com.mapbox.maps.MapView?>(null) }
    var polylineAnnotationManager by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager?>(null) }
    var isStyleLoaded by remember { mutableStateOf(false) }
    // Alto de la tarjeta del viaje, para que la cámara no deje la ruta debajo de ella
    var tripCardHeightPx by remember { mutableStateOf(0) }
    // Si el conductor mueve el mapa con el dedo, la cámara deja de seguir el viaje unos segundos
    val lastUserGestureMs = remember { longArrayOf(0L) }

    // Ruta calculada: puntos, distancia (km) y tiempo (min) restantes
    var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var routeDistance by remember { mutableStateOf(0.0) }
    var routeDuration by remember { mutableStateOf(0.0) }
    var isCalculatingRoute by remember { mutableStateOf(false) }

    // Al abrir la app retoma los viajes abiertos: el actual y, si lo hay, el siguiente en espera
    LaunchedEffect(Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        val rides = runCatching { activeRideRepository.findOpenRidesForDriver(uid) }.getOrDefault(emptyList())
        val current = rides.firstOrNull { it.status == "in_progress" } ?: rides.firstOrNull() ?: return@LaunchedEffect
        activeRideId = current.rideId
        activeRideStatus = current.status
        activeRideRequest = current.toDriverRequest()
        if (current.status == "in_progress") {
            rides.firstOrNull { it.rideId != current.rideId }?.let { next ->
                queuedRideId = next.rideId
                queuedRideRequest = next.toDriverRequest()
            }
        }
    }

    // Quita del mapa la ruta y el marcador del pasajero (viaje terminado o cancelado)
    val clearRouteAndPassengerMarker = remember {
        {
            runCatching { polylineAnnotationManager?.deleteAll() }
            runCatching { clientMarkerAnnotation?.let { pointAnnotationManager?.delete(it) } }
            routeAnnotation = null
            clientMarkerAnnotation = null
            clientLocationMarker = null
            routePoints = emptyList()
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

    // Fija el punto objetivo del viaje (recojo o destino). La ruta, el marcador del pasajero y la
    // cámara los dibuja el seguimiento del mapa (más abajo), también con la app minimizada.
    fun setTripTarget(target: GeoPoint) {
        clientLocationMarker = target
    }

    // Funciones para manejar solicitudes
    // Sin viaje: el aceptado pasa a ser el actual. Con un pasajero a bordo: queda como siguiente viaje.
    // El conductor sigue en línea en ambos casos.
    fun handleAcceptRideRequest(request: DriverRideRequest) {
        scope.launch {
            driverRideRequestRepository.acceptRideRequest(request.requestId)
                .onSuccess { rideId ->
                    incomingRideRequests = emptyList()
                    if (activeRideRequest == null) {
                        activeRideRequest = request
                        activeRideId = rideId
                        activeRideStatus = "accepted"
                        setTripTarget(GeoPoint(request.originLatitude, request.originLongitude))
                        Toast.makeText(context, "Viaje aceptado", Toast.LENGTH_SHORT).show()
                    } else {
                        queuedRideRequest = request
                        queuedRideId = rideId
                        Toast.makeText(context, "Siguiente viaje aceptado. Irás por este pasajero al terminar el viaje actual.", Toast.LENGTH_LONG).show()
                    }
                }
                .onFailure { error ->
                    Toast.makeText(context, error.message ?: "No se pudo aceptar el viaje", Toast.LENGTH_LONG).show()
                }
        }
    }

    fun handleDeclineRideRequest(request: DriverRideRequest) {
        declinedRequestIds = declinedRequestIds + request.requestId
        com.intu.taxi.driver.DriverSession.declinedRequestIds += request.requestId
        incomingRideRequests = incomingRideRequests.filter { it.requestId != request.requestId }
    }

    // Termina el viaje actual en pantalla: pasa al siguiente en espera o queda libre (y en línea)
    fun moveToNextRideOrClear() {
        showDeliveryPaymentConfirmation = false
        clearRouteAndPassengerMarker()
        routeDistance = 0.0
        routeDuration = 0.0
        val next = queuedRideRequest
        if (next != null) {
            activeRideRequest = next
            activeRideId = queuedRideId
            activeRideStatus = "accepted"
            queuedRideRequest = null
            queuedRideId = null
            setTripTarget(GeoPoint(next.originLatitude, next.originLongitude))
        } else {
            activeRideRequest = null
            activeRideId = null
        }
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

    // Barra inferior solo fuera de línea y sin viaje. El servicio en segundo plano corre mientras
    // esté en línea o en un viaje: mantiene el GPS y avisa de solicitudes con la app minimizada.
    val hasActiveRide = activeRideRequest != null
    LaunchedEffect(isSearching, hasActiveRide) {
        headerVisible = true
        onBottomBarVisibilityChanged(!isSearching && !hasActiveRide)
        if (isSearching || hasActiveRide) {
            com.intu.taxi.driver.DriverOnlineService.start(context)
        } else {
            com.intu.taxi.driver.DriverOnlineService.stop(context)
        }
    }
    LaunchedEffect(activeRideId) { com.intu.taxi.driver.DriverSession.activeRideId = activeRideId }

    // Recibe solicitudes mientras está en línea y libre, o llevando a un pasajero sin siguiente viaje
    val canReceiveRequests = isSearching &&
        (!hasActiveRide || (activeRideStatus == "in_progress" && queuedRideRequest == null))
    LaunchedEffect(canReceiveRequests) {
        if (!canReceiveRequests) {
            incomingRideRequests = emptyList()
            return@LaunchedEffect
        }
        driverRideRequestRepository.getActiveRideRequests().collect { requests ->
            incomingRideRequests = requests.filter { it.requestId !in declinedRequestIds }
        }
    }

    // El siguiente viaje en espera: si el pasajero cancela, se quita
    LaunchedEffect(queuedRideId) {
        val rideId = queuedRideId ?: return@LaunchedEffect
        activeRideRepository.watchRide(rideId).collect { ride ->
            if (ride != null && ride.status in setOf("cancelled", "searching")) {
                queuedRideRequest = null
                queuedRideId = null
                Toast.makeText(context, if (ride.isDelivery) "El siguiente envío fue cancelado" else "El pasajero del siguiente viaje canceló", Toast.LENGTH_LONG).show()
            } else if (ride != null) {
                queuedRideRequest = ride.toDriverRequest()
            }
        }
    }

    // Seguimiento del viaje actual. Se recolecta dentro del efecto para que se detenga al cambiar
    // de viaje (antes se lanzaba aparte y seguía consultando Supabase para viajes ya terminados).
    LaunchedEffect(activeRideId) {
        val rideId = activeRideId ?: return@LaunchedEffect
        activeRideRepository.getActiveRide(rideId).collect { activeRide ->
            if (activeRide == null) return@collect
            if (activeRide.status == "cancelled") {
                // El pasajero canceló: pasa al siguiente viaje o queda libre
                moveToNextRideOrClear()
                Toast.makeText(context, if (activeRide.isDelivery) "Quien envía canceló el pedido" else "El pasajero canceló el viaje", Toast.LENGTH_LONG).show()
                return@collect
            }
            activeRideStatus = activeRide.status
            activeRideRequest = activeRide.toDriverRequest()
            // Punto objetivo: recojo mientras va por el pasajero, destino durante el viaje
            activeRide.clientLocation?.let { target ->
                val previous = clientLocationMarker
                if (previous == null || calculateDistance(previous, target) > 10) setTripTarget(target)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }

    // Android 13+: sin este permiso no se ven los avisos de solicitudes con la app minimizada
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Sin notificaciones no verás solicitudes con la app minimizada", Toast.LENGTH_LONG).show()
        }
    }
    fun askNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val mapView = rememberMapViewWithLifecycle(accessToken = mapboxToken)
        mapViewRef = mapView
        // Para saber si la app está visible (con la app minimizada la cámara se mueve sin animación)
        val lifecycleOwner = LocalLifecycleOwner.current
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        val mapStyle = com.intu.taxi.ui.theme.intuMapStyle()
        LaunchedEffect(mapView, mapStyle) {
            mapView.mapboxMap.loadStyleUri(mapStyle) {
                val view = mapView
                if (!isStyleLoaded && !hasLocationPermission) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                // Habilitar puck si hay permiso y personalizar icono de geolocalización
                view.location.updateSettings {
                    locationPuck = LocationPuck2D(
                        bearingImage = ImageHolder.from(R.drawable.ic_moto)
                    )
                }
                // Ruta y marcador del pasajero (la ruta primero, para que el marcador quede encima)
                if (polylineAnnotationManager == null) polylineAnnotationManager = view.annotations.createPolylineAnnotationManager()
                if (pointAnnotationManager == null) pointAnnotationManager = view.annotations.createPointAnnotationManager()
                isStyleLoaded = true

            }
        }
        MapLocationBinding(mapView, isStyleLoaded, hasLocationPermission, testLocation) { point, first ->
            val location = GeoPoint(point.latitude(), point.longitude())
            currentLocation = location
            if (first) {
                lastLocationSentMs[0] = 0L
                mapView.mapboxMap.setCamera(CameraOptions.Builder().center(point).zoom(14.0).build())
                if (activeRideRequest != null && clientLocationMarker == null) {
                    setTripTarget(GeoPoint(activeRideRequest!!.originLatitude, activeRideRequest!!.originLongitude))
                }
            }
            val rideId = activeRideId
            val interval = if (rideId != null) 2_000L else 10_000L
            val now = System.currentTimeMillis()
            if ((hasLocationPermission || testLocation != null) &&
                !com.intu.taxi.driver.DriverOnlineService.isRunning &&
                (rideId != null || isSearching) && now - lastLocationSentMs[0] >= interval) {
                lastLocationSentMs[0] = now
                scope.launch {
                    runCatching {
                        val effective = AdminLocationSimulation.effectiveLocation(location) ?: return@runCatching
                        if (rideId != null) activeRideRepository.updateDriverLocation(rideId, effective)
                        else driverAvailabilityRepository.updateDriverLocation(effective)
                    }
                }
            }
        }

        // Con la app minimizada el mapa se pausa y deja de dar la ubicación: se usa la del servicio en
        // segundo plano, que sigue leyendo el GPS
        LaunchedEffect(Unit) {
            com.intu.taxi.driver.DriverSession.location.collect { location ->
                if (location != null && !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    currentLocation = AdminLocationSimulation.effectiveLocation(location)
                }
            }
        }

        DisposableEffect(mapView) {
            val gestureListener = object : OnMoveListener {
                override fun onMoveBegin(detector: MoveGestureDetector) { lastUserGestureMs[0] = System.currentTimeMillis() }
                override fun onMove(detector: MoveGestureDetector): Boolean = false
                override fun onMoveEnd(detector: MoveGestureDetector) { lastUserGestureMs[0] = System.currentTimeMillis() }
            }
            mapView.gestures.addOnMoveListener(gestureListener)
            onDispose { mapView.gestures.removeOnMoveListener(gestureListener) }
        }

        // Ruta, marcador del pasajero y cámara del viaje, como en la app del pasajero: la ruta va del
        // conductor al objetivo (recojo o destino) y la cámara encuadra ambos dejando libres el
        // encabezado y la tarjeta. Corre fuera de la recomposición, que Compose pausa con la app
        // minimizada, así todo sigue al día en segundo plano y al volver no hay que esperar.
        // La ruta se pide a Mapbox al cambiar de objetivo o si el conductor se sale de ella; mientras
        // la sigue, solo se recorta lo recorrido. Como mucho se procesa un cambio por segundo.
        LaunchedEffect(mapView) {
            var routeLeg: String? = null
            var secondsPerMeter = 0.0
            snapshotFlow {
                DriverTripMapInput(
                    rideId = activeRideId,
                    status = activeRideStatus,
                    target = clientLocationMarker,
                    driver = currentLocation,
                    styleLoaded = isStyleLoaded,
                    cardHeightPx = tripCardHeightPx
                )
            }
                .distinctUntilChanged()
                .conflate()
                .collect { input ->
                    val lines = polylineAnnotationManager
                    val markers = pointAnnotationManager
                    val target = input.target?.let { Point.fromLngLat(it.longitude, it.latitude) }
                    if (input.rideId == null || target == null || !input.styleLoaded || lines == null || markers == null) {
                        routeLeg = null
                        return@collect
                    }
                    val driver = input.driver?.let { Point.fromLngLat(it.longitude, it.latitude) }
                    val leg = "${input.rideId}:${target.latitude()},${target.longitude()}"

                    if (driver != null) {
                        val trimmed = if (leg == routeLeg) TripMap.trimRoute(routePoints, driver) else null
                        if (trimmed != null) {
                            routePoints = trimmed
                        } else {
                            isCalculatingRoute = true
                            val route = TripMap.fetchRoute(mapboxToken, driver, target)
                            isCalculatingRoute = false
                            if (route != null) {
                                routePoints = route.points
                                secondsPerMeter = if (route.distanceMeters > 0) route.durationSeconds / route.distanceMeters else 0.0
                            } else {
                                // Sin conexión con Mapbox: línea recta hasta el objetivo
                                routePoints = listOf(driver, target)
                                secondsPerMeter = 0.0
                            }
                            routeLeg = leg
                        }
                        val remaining = TripMap.lengthMeters(routePoints)
                        routeDistance = remaining / 1000.0
                        routeDuration = remaining * secondsPerMeter / 60.0
                    }

                    // Dibuja o mueve la ruta y el marcador del pasajero
                    if (routePoints.size > 1) {
                        val line = routeAnnotation
                        if (line == null || lines.annotations.none { it.id == line.id }) {
                            routeAnnotation = lines.create(
                                PolylineAnnotationOptions()
                                    .withPoints(routePoints)
                                    .withLineColor(com.intu.taxi.ui.map.TripRouteStyle.lineColorHex)
                                    .withLineWidth(com.intu.taxi.ui.map.TripRouteStyle.lineWidth)
                            )
                        } else {
                            line.points = routePoints
                            runCatching { lines.update(line) }
                        }
                    }
                    val marker = clientMarkerAnnotation
                    if (marker == null || markers.annotations.none { it.id == marker.id }) {
                        clientMarkerAnnotation = markers.create(
                            PointAnnotationOptions()
                                .withPoint(target)
                                .withIconImage(createPassengerIcon(context))
                                .withIconSize(1.0)
                        )
                    } else if (marker.point != target) {
                        marker.point = target
                        runCatching { markers.update(marker) }
                    }

                    if (System.currentTimeMillis() - lastUserGestureMs[0] >= 8_000) {
                        val density = context.resources.displayMetrics.density.toDouble()
                        val mapHeight = mapView.height.toDouble()
                        TripMap.fitCamera(
                            mapView,
                            listOfNotNull(driver, target) + routePoints,
                            EdgeInsets(
                                mapHeight * 0.2 + 24 * density,
                                48 * density,
                                (input.cardHeightPx + 24 * density).coerceAtMost(mapHeight * 0.6),
                                48 * density
                            ),
                            animate = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                        )
                    }
                    delay(1_000)
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
                        if (canReceiveRequests && incomingRideRequests.isNotEmpty() && activeRideRequest == null) {
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
                                            askNotificationPermissionIfNeeded()
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
                    .onGloballyPositioned { tripCardHeightPx = it.size.height }
            ) {
                EnhancedActiveRideCard(
                    request = request,
                    status = activeRideStatus,
                    distance = routeDistance,
                    duration = routeDuration,
                    isCalculatingRoute = isCalculatingRoute,
                    onArrived = {
                        if (activeRideStatus == "arrived") {
                            val rideId = activeRideId
                            if (!isCheckingStartPin && rideId != null) scope.launch {
                                isCheckingStartPin = true
                                try {
                                    val requiresPin = activeRideRepository.requiresStartPin(rideId)
                                    check(activeRideId == rideId && activeRideStatus == "arrived") {
                                        "El servicio cambió de estado. Actualiza e intenta de nuevo."
                                    }
                                    if (requiresPin) {
                                        pinInput = ""
                                        pinError = null
                                        showPinDialog = true
                                    } else if (request.isDelivery &&
                                        request.delivery?.payer == com.intu.taxi.models.DeliveryPayer.SENDER &&
                                        request.delivery?.paymentCollected != true) {
                                        deliveryActionError = null
                                        deliveryConfirmationStatus = activeRideStatus
                                        showDeliveryPaymentConfirmation = true
                                    } else {
                                        val updated = activeRideRepository.advanceRide(rideId, "in_progress").getOrThrow()
                                        activeRideStatus = updated.status
                                        if (request.isDelivery) {
                                            setTripTarget(GeoPoint(request.destinationLatitude, request.destinationLongitude))
                                        }
                                        Toast.makeText(context, if (request.isDelivery) "Envío iniciado" else "Viaje iniciado",
                                            Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "No se pudo iniciar el servicio", Toast.LENGTH_LONG).show()
                                } finally { isCheckingStartPin = false }
                            }
                        } else if (request.isDelivery && activeRideStatus == "in_progress") {
                            deliveryActionError = null
                            deliveryConfirmationStatus = activeRideStatus
                            showDeliveryPaymentConfirmation = true
                        } else scope.launch {
                            val rideId = activeRideId ?: return@launch
                            val nextStatus = when (activeRideStatus) {
                                "accepted" -> "arrived"
                                "in_progress" -> "completed"
                                else -> return@launch
                            }
                            activeRideRepository.advanceRide(rideId, nextStatus)
                                .onSuccess {
                                    activeRideStatus = nextStatus
                                    val message = when {
                                        nextStatus == "arrived" -> "Llegada confirmada"
                                        queuedRideRequest != null -> "Pago confirmado. Ahora ve por tu siguiente pasajero"
                                        else -> "Pago confirmado. Viaje finalizado"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    // Al terminar sigue en línea: pasa al siguiente viaje o vuelve a recibir solicitudes
                                    if (nextStatus == "completed") {
                                        activeRideRequest?.let { rideToRate = rideId to it }
                                        moveToNextRideOrClear()
                                    }
                                }
                                .onFailure { Toast.makeText(context, it.message ?: "No se pudo actualizar el viaje", Toast.LENGTH_LONG).show() }
                        }
                    },
                    onCancel = {
                        scope.launch {
                            activeRideId?.let { rideId ->
                                // cancelRide devuelve Result: solo se limpia la pantalla si el servidor aceptó
                                activeRideRepository.cancelRide(rideId)
                                    .onSuccess {
                                        moveToNextRideOrClear()
                                        Toast.makeText(context, "Viaje cancelado", Toast.LENGTH_SHORT).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(context, it.message ?: "No se pudo cancelar el viaje", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                    }
                )
            }
        }

        // Durante un viaje: solicitud disponible para el siguiente viaje, o el siguiente ya aceptado
        if (hasActiveRide) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val next = queuedRideRequest
                if (next != null) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .background(AppearanceColors.surface, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.intu.taxi.ui.components.Avatar(url = next.userPhotoUrl, size = 36.dp, zoomable = true)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Siguiente: ${next.userName.ifBlank { "Pasajero" }}",
                                fontWeight = FontWeight.SemiBold,
                                color = AppearanceColors.foreground(Color(0xFF1E1F47))
                            )
                            Text(
                                next.originAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppearanceColors.secondary(Color.Gray),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = {
                            val rideId = queuedRideId ?: return@TextButton
                            scope.launch {
                                activeRideRepository.cancelRide(rideId)
                                    .onSuccess {
                                        queuedRideRequest = null
                                        queuedRideId = null
                                        Toast.makeText(context, "Siguiente viaje cancelado", Toast.LENGTH_SHORT).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(context, it.message ?: "No se pudo cancelar", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }) { Text("Cancelar", color = AppearanceColors.highlight(Color(0xFFB42318))) }
                    }
                } else if (canReceiveRequests && incomingRideRequests.isNotEmpty()) {
                    Text(
                        "Solicitud para tu siguiente viaje",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .background(Color(0xFF1E1F47).copy(alpha = 0.85f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    val request = incomingRideRequests.first()
                    IncomingRideRequestCard(
                        request = request,
                        currentLatitude = currentLocation?.latitude ?: 0.0,
                        currentLongitude = currentLocation?.longitude ?: 0.0,
                        onAccept = { handleAcceptRideRequest(request) },
                        onDecline = { handleDeclineRideRequest(request) }
                    )
                }
            }
        }

        // Al terminar un viaje: calificar al pasajero (se puede omitir y hacerlo luego en Viajes)
        rideToRate?.let { (ratedRideId, rider) ->
            AlertDialog(
                onDismissRequest = { rideToRate = null },
                title = { Text("¿Cómo fue el pasajero?") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        com.intu.taxi.ui.components.Avatar(url = rider.userPhotoUrl, size = 64.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(rider.userName.ifBlank { "Pasajero" }, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        com.intu.taxi.ui.components.StarRating(stars = 0, size = 40.dp, onRate = { stars ->
                            rideToRate = null
                            scope.launch {
                                runCatching { com.intu.taxi.repositories.RideHistoryRepository().rate(ratedRideId, stars) }
                                    .onSuccess { Toast.makeText(context, "¡Gracias por calificar!", Toast.LENGTH_SHORT).show() }
                                    .onFailure { Toast.makeText(context, it.message ?: "No se pudo guardar la calificación", Toast.LENGTH_LONG).show() }
                            }
                        })
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { rideToRate = null }) { Text("Omitir") } }
            )
        }

        val deliveryRequest = activeRideRequest
        if (showDeliveryPaymentConfirmation && deliveryRequest?.isDelivery == true) {
            DeliveryPaymentDialog(
                request = deliveryRequest, pickup = deliveryConfirmationStatus == "arrived",
                busy = deliveryActionBusy, error = deliveryActionError,
                onDismiss = { showDeliveryPaymentConfirmation = false },
                onConfirm = {
                    val rideId = activeRideId
                    if (!deliveryActionBusy && rideId != null) scope.launch {
                        deliveryActionBusy = true
                        deliveryActionError = null
                        try {
                            check(activeRideStatus == deliveryConfirmationStatus) { "El envío cambió de estado. Cierra este diálogo y actualiza." }
                            if (deliveryRequest.delivery?.paymentCollected != true) {
                                activeRideRepository.confirmDeliveryPayment(rideId).getOrThrow()
                            }
                            val nextStatus = if (deliveryConfirmationStatus == "arrived") "in_progress" else "completed"
                            val updated = activeRideRepository.advanceRide(rideId, nextStatus).getOrThrow()
                            activeRideStatus = updated.status
                            showDeliveryPaymentConfirmation = false
                            if (nextStatus == "completed") {
                                rideToRate = rideId to deliveryRequest
                                moveToNextRideOrClear()
                            } else {
                                setTripTarget(com.google.firebase.firestore.GeoPoint(deliveryRequest.destinationLatitude, deliveryRequest.destinationLongitude))
                            }
                        } catch (e: Exception) {
                            deliveryActionError = e.message ?: "No se pudo confirmar. Intenta de nuevo."
                        } finally { deliveryActionBusy = false }
                    }
                }
            )
        }

        // PIN de seguridad: el viaje solo inicia si el pasajero le dicta al conductor el PIN correcto
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { if (!isVerifyingPin) showPinDialog = false },
                title = { Text("PIN de seguridad") },
                text = {
                    Column {
                        Text(if (activeRideRequest?.isDelivery == true)
                            "Pide el PIN de 4 dígitos a quien envía el paquete antes de recibirlo. Lo ve en su app."
                            else "Pídele al pasajero su PIN de 4 dígitos. Lo ve en su app.")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { value ->
                                pinInput = value.filter(Char::isDigit).take(4)
                                pinError = null
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center,
                                letterSpacing = 12.sp
                            ),
                            isError = pinError != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        pinError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = AppearanceColors.highlight(Color(0xFFB42318)), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = pinInput.length == 4 && !isVerifyingPin &&
                            (activeRideRequest?.isDelivery != true || activeRideRequest?.delivery != null),
                        onClick = {
                            val rideId = activeRideId ?: return@TextButton
                            isVerifyingPin = true
                            scope.launch {
                                activeRideRepository.verifyStartPin(rideId, pinInput)
                                    .onSuccess { (verified, attemptsLeft) ->
                                        if (verified && activeRideRequest?.isDelivery == true &&
                                            activeRideRequest?.delivery?.payer == com.intu.taxi.models.DeliveryPayer.SENDER &&
                                            activeRideRequest?.delivery?.paymentCollected != true) {
                                            showPinDialog = false
                                            deliveryActionError = null
                                            deliveryConfirmationStatus = activeRideStatus
                                            showDeliveryPaymentConfirmation = true
                                        } else if (verified) {
                                            activeRideRepository.advanceRide(rideId, "in_progress")
                                                .onSuccess {
                                                    activeRideStatus = "in_progress"
                                                    showPinDialog = false
                                                    Toast.makeText(context, "Viaje iniciado", Toast.LENGTH_SHORT).show()
                                                }
                                                .onFailure { pinError = it.message ?: "No se pudo iniciar el viaje" }
                                        } else {
                                            pinInput = ""
                                            pinError = if (attemptsLeft > 0) {
                                                "PIN incorrecto. Te ${if (attemptsLeft == 1) "queda 1 intento" else "quedan $attemptsLeft intentos"}."
                                            } else {
                                                "PIN bloqueado por demasiados intentos. Cancela el viaje por seguridad."
                                            }
                                        }
                                    }
                                    .onFailure { pinError = it.message ?: "No se pudo verificar el PIN" }
                                isVerifyingPin = false
                            }
                        }
                    ) { Text(if (isVerifyingPin) "Verificando…" else if (activeRideRequest?.isDelivery == true) "Recoger paquete" else "Iniciar viaje") }
                },
                dismissButton = {
                    TextButton(enabled = !isVerifyingPin, onClick = { showPinDialog = false }) { Text("Cancelar") }
                }
            )
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
            // Dos líneas para que "Empezar ahora" quepa dentro del círculo de 90 dp
            Text(
                text = if (isSearching) "Parar" else "Empezar\nahora",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

/** Datos de un viaje abierto en el formato de solicitud que usan las tarjetas del conductor. */
private fun com.intu.taxi.models.ActiveRide.toDriverRequest() = DriverRideRequest(
    requestId = rideId,
    userId = clientId,
    userName = passengerName,
    userPhone = passengerPhone,
    userPhotoUrl = if (passenger == null) riderPhotoUrl.ifBlank { null } else null,
    originLatitude = originLatitude,
    originLongitude = originLongitude,
    originAddress = originAddress,
    destinationLatitude = destinationLatitude,
    destinationLongitude = destinationLongitude,
    destinationAddress = destinationAddress,
    estimatedPrice = fare,
    paymentMethod = paymentMethod,
    status = status,
    rideType = vehicleType,
    serviceKind = serviceKind,
    delivery = delivery
)

// Función para crear icono de pasajero con diseño moderno similar a HomeScreen
/** Lo que decide la ruta y la cámara del viaje en el mapa del conductor. */
private data class DriverTripMapInput(
    val rideId: String?,
    val status: String,
    val target: GeoPoint?,
    val driver: GeoPoint?,
    val styleLoaded: Boolean,
    val cardHeightPx: Int
)

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
        "arrived" -> if (request.isDelivery) "Recoger paquete" else "Iniciar viaje"
        "in_progress" -> if (request.isDelivery) "Confirmar entrega" else "Confirmar pago y finalizar"
        else -> "Llegué"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppearanceColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isMinimized) 12.dp else 20.dp)
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header con estado del viaje y botón de minimizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Foto del pasajero para reconocerlo en el punto de recojo; tocarla la agranda
                com.intu.taxi.ui.components.Avatar(
                    url = request.userPhotoUrl,
                    size = if (isMinimized) 40.dp else 56.dp,
                    zoomable = true,
                    contentDescription = if (request.isDelivery) "Foto de quien envía" else "Foto del pasajero"
                )

                Spacer(modifier = Modifier.width(if (isMinimized) 8.dp else 12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (status) {
                            "arrived" -> "En el punto de recojo"
                            "in_progress" -> if (request.isDelivery) "Envío en curso" else "Viaje en curso"
                            else -> if (request.isDelivery) "Envío aceptado" else "Viaje aceptado"
                        },
                        style = if (isMinimized) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppearanceColors.foreground(Color(0xFF1E1F47))
                    )
                    Text(
                        text = request.userName.ifBlank { if (request.isDelivery) "Quien envía" else "Pasajero" },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppearanceColors.secondary(Color(0xFF5F6570))
                    )
                    if (!isMinimized) {
                        Text(
                            text = when (status) {
                                "arrived" -> if (request.isDelivery) "Verifica el PIN para recibir el paquete" else "Recoge al pasajero e inicia el viaje"
                                "in_progress" -> if (request.isDelivery) "Entrega el paquete a quien lo recibe" else "Al terminar, confirma el pago recibido"
                                else -> "Dirígete al punto de recogida"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AppearanceColors.secondary(Color.Gray)
                        )
                    }
                }
                
                // Botón de minimizar/maximizar
                Icon(
                    imageVector = if (isMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isMinimized) "Expandir" else "Minimizar",
                    tint = AppearanceColors.highlight(Color(0xFF08817E)),
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
                            color = AppearanceColors.highlight(Color(0xFF08817E))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calculando ruta...",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppearanceColors.secondary(Color.Gray)
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
                                tint = AppearanceColors.highlight(Color(0xFF08817E)),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.1f", distance)} km",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppearanceColors.foreground(Color(0xFF1E1F47))
                            )
                            Text(
                                text = "Distancia",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppearanceColors.secondary(Color.Gray)
                            )
                        }
                        
                        // Duración
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = AppearanceColors.highlight(Color(0xFF08817E)),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.0f", duration)} min",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppearanceColors.foreground(Color(0xFF1E1F47))
                            )
                            Text(
                                text = "Tiempo",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppearanceColors.secondary(Color.Gray)
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
                            tint = AppearanceColors.highlight(Color(0xFF08817E)),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (status == "in_progress") request.destinationAddress else request.originAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppearanceColors.foreground(Color(0xFF1E1F47)),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                if (request.isDelivery) {
                    request.delivery?.let { DeliverySummary(it, allowCall = true) }
                    if (request.userPhone.isNotBlank()) {
                        val context = LocalContext.current
                        OutlinedButton(onClick = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL,
                            android.net.Uri.parse("tel:${request.userPhone}"))) }) { Text("Llamar a quien solicitó") }
                    }
                }

                if (!request.isDelivery && request.userPhone.isNotBlank()) {
                    val context = LocalContext.current
                    OutlinedButton(onClick = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:${request.userPhone}"))) }) { Text("Llamar al pasajero") }
                }

                Text(
                    text = "${if (request.delivery?.paymentCollected == true) "Transporte pagado" else "Cobrar"} ${com.intu.taxi.ui.formatSoles(request.estimatedPrice)} · ${if (request.paymentMethod == "yape_plin") "Yape" else "Efectivo"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppearanceColors.highlight(Color(0xFF08817E))
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
                        enabled = !request.isDelivery || (status != "in_progress" && request.delivery?.paymentCollected != true),
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
