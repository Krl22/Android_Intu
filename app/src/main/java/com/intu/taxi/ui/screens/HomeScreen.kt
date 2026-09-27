package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Navigation
 
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Add
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.intu.taxi.R
import com.intu.taxi.ui.map.rememberMapViewWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.style

import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.android.gestures.MoveGestureDetector

import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AndroidColor
 
import android.location.Geocoder
import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
 
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.pow
 
import com.mapbox.geojson.LineString
 
import androidx.compose.ui.layout.onGloballyPositioned
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.CoordinateBounds
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.repositories.RideRequestRepository
import com.intu.taxi.data.PaymentPreferences
import kotlinx.coroutines.tasks.await
 

private const val HEADER_SHIFT_FRACTION_KEYBOARD = 0.25f
private const val HEADER_SHIFT_FRACTION_PIN = 0.5f

// Modelo de datos para las opciones de viaje
data class RideOptionData(
    val name: String,
    val price: Double,
    val minutes: Double,
    val leadingContent: (@Composable () -> Unit)?,
    val colors: List<Color>
)

 

@Composable
private fun RideOptionSlideCard(
    option: RideOptionData,
    isSelected: Boolean,
    onClick: () -> Unit,
    index: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 0.95f,
        animationSpec = tween(durationMillis = 300),
        label = "slideScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.7f,
        animationSpec = tween(durationMillis = 300),
        label = "slideAlpha"
    )
    
    Card(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .scale(scale)
            .alpha(alpha)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (isSelected) 0.95f else 0.8f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 16.dp else 8.dp
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            brush = Brush.linearGradient(
                colors = if (isSelected) option.colors else listOf(Color(0xFFE0E0E0), Color(0xFFE0E0E0))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header con nombre y selección
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Seleccionado",
                            tint = option.colors[0],
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Text(
                    text = option.name.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1C1C1E),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            
            // Imagen del vehículo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = option.colors,
                            start = Offset(0f, 0f),
                            end = Offset(100f, 100f)
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                option.leadingContent?.invoke() ?: Icon(
                    Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            
            // Información de precio y tiempo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val priceStr = String.format(Locale.getDefault(), "$%.2f", option.price)
                val etaMin = kotlin.math.max(1.0, option.minutes)
                val etaStr = String.format(Locale.getDefault(), "~%.0f min", etaMin)
                
                Text(
                    text = priceStr,
                    style = MaterialTheme.typography.headlineSmall,
                    color = option.colors[0],
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF6E6E73),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = etaStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6E6E73)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RideOptionsSlider(
    options: List<RideOptionData>,
    selectedOptionName: String?,
    onOptionSelected: (String) -> Unit,
    initialSelectedIndex: Int = 0
) {
    val listState = rememberLazyListState()
    val snappingLayout = remember(listState) { SnapLayoutInfoProvider(listState) }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)
    val density = LocalDensity.current
    
    // Posicionar el carousel en el elemento inicial seleccionado
    LaunchedEffect(Unit) {
        if (initialSelectedIndex < options.size) {
            listState.scrollToItem(initialSelectedIndex)
            onOptionSelected(options[initialSelectedIndex].name)
        }
    }
    
    // Autoseleccionar la opción cuando el usuario se detiene en ella
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            // Obtener el índice del elemento más centrado en la pantalla
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportEndOffset / 2
            
            var closestItemIndex = 0
            var minDistance = Float.MAX_VALUE
            
            layoutInfo.visibleItemsInfo.forEach { itemInfo ->
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val distance = kotlin.math.abs(itemCenter - viewportCenter)
                
                if (distance < minDistance) {
                    minDistance = distance.toFloat()
                    closestItemIndex = itemInfo.index
                }
            }
            
            // Seleccionar el elemento más cercano al centro
            if (closestItemIndex < options.size) {
                onOptionSelected(options[closestItemIndex].name)
            }
        }
    }
    
    Column {
        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            itemsIndexed(options) { index, option ->
                RideOptionSlideCard(
                    option = option,
                    isSelected = (selectedOptionName == option.name),
                    onClick = { onOptionSelected(option.name) },
                    index = index
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            options.forEachIndexed { index, _ ->
                val isSelected = (selectedOptionName == options[index].name)
                val color by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF08817E) else Color(0xFFE0E0E0),
                    animationSpec = tween(durationMillis = 300),
                    label = "indicatorColor"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    padding: PaddingValues,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {}
) {
    val mapboxToken = stringResource(id = com.intu.taxi.R.string.mapbox_access_token)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasLocationPermission by rememberSaveable { mutableStateOf(false) }
    // No persistir el modo pin entre recomposiciones/navegaciones para no ocultar el BottomBar al iniciar
    var isSelectingDestination by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<Point?>(null) }
    var pickupLocation by remember { mutableStateOf<Point?>(null) }
    var isSelectingPickup by remember { mutableStateOf(false) }
    var destinationBeforePickup by remember { mutableStateOf<Point?>(null) }
    var moveListenerRef by remember { mutableStateOf<OnMoveListener?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var pinSearchQuery by rememberSaveable { mutableStateOf("") }
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var suggestions by remember { mutableStateOf(listOf<GeocodeSuggestion>()) }
    var pinSuggestions by remember { mutableStateOf(listOf<GeocodeSuggestion>()) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var isPinSearchFocused by remember { mutableStateOf(false) }
    var hasUserInteractedWithPinSearch by remember { mutableStateOf(false) }
 
    var routeOffsets by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var routeMoveListenerRef by remember { mutableStateOf<OnMoveListener?>(null) }
    var confirmedDestination by remember { mutableStateOf<Point?>(null) }
    var confirmedDestOffset by remember { mutableStateOf<Offset?>(null) }
    var rideOptionsPanelHeightPx by remember { mutableStateOf(0) }
    var mapViewRef by remember { mutableStateOf<com.mapbox.maps.MapView?>(null) }
    val density = LocalDensity.current
    val pinSizeDp = 42.dp
    // No persistir el panel de opciones de viaje para que el BottomBar se muestre al entrar
    var isRideOptionsVisible by remember { mutableStateOf(false) }
    var routeDistanceMeters by remember { mutableStateOf<Double?>(null) }
    var routeDurationSeconds by remember { mutableStateOf<Double?>(null) }
    var selectedRideOptionName by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Estado de direcciones para el diálogo de búsqueda
    var originAddress by remember { mutableStateOf<String>("") }
    var destinationAddress by remember { mutableStateOf<String>("") }
    var estimatedPrice by remember { mutableStateOf<Double>(0.0) }
    
    // Estado de búsqueda de conductor
    var isSearchingDriver by remember { mutableStateOf(false) }
    var currentRideRequestId by remember { mutableStateOf<String?>(null) }
    val rideRequestRepository = remember { RideRequestRepository() }
    val activeRideRepository = remember { com.intu.taxi.repositories.ActiveRideRepository() }
    var activeRide by remember { mutableStateOf<com.intu.taxi.models.ActiveRide?>(null) }
    var driverLocation by remember { mutableStateOf<com.google.firebase.firestore.GeoPoint?>(null) }
    var driverOffset by remember { mutableStateOf<Offset?>(null) }
    var showDriverIcon by remember { mutableStateOf(false) }
    var driverAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var driverAnnotation by remember { mutableStateOf<PointAnnotation?>(null) }
    var lastValidCenter by remember { mutableStateOf<Point?>(null) }
    var lastValidZoom by remember { mutableStateOf<Double?>(null) }
    var isCameraLocked by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        runCatching { activeRideRepository.findOpenRideForRider(uid) }
            .getOrNull()?.let { ride ->
                currentRideRequestId = ride.rideId
                isSearchingDriver = ride.status == "searching"
                activeRide = if (ride.status == "searching") null else ride
            }
    }

    LaunchedEffect(activeRide) {
        if (activeRide != null) {
            isCameraLocked = true
            kotlinx.coroutines.delay(1200)
            isCameraLocked = false
        }
    }
    
    // Payment preferences
    val paymentPreferences = remember { PaymentPreferences(context) }
    var selectedPaymentMethod by remember { mutableStateOf("efectivo") }
    val httpClient = remember { OkHttpClient() }
    
    // Load saved payment method
    LaunchedEffect(Unit) {
        paymentPreferences.paymentMethod.collect { method ->
            selectedPaymentMethod = method
        }
    }

    // Escuchar cambios en el estado de la solicitud de viaje
    LaunchedEffect(currentRideRequestId) {
        currentRideRequestId?.let { requestId ->
            rideRequestRepository.listenToRideRequest(requestId).collect { rideRequest ->
                if (rideRequest != null) {
                    when (rideRequest.status) {
                        "accepted" -> {
                            // El conductor aceptó el viaje, obtener el viaje activo
                            println("DEBUG: ¡Viaje aceptado por el conductor! Buscando viaje activo...")
                            isSearchingDriver = false
                            // El viaje activo se obtendrá con el listener de abajo
                        }
                        "cancelled" -> {
                            // El viaje fue cancelado
                            println("DEBUG: Viaje cancelado")
                            isSearchingDriver = false
                            currentRideRequestId = null
                        }
                    }
                } else {
                    // El ride request fue eliminado (probablemente porque el conductor lo aceptó)
                    // No limpiamos currentRideRequestId aquí porque el active ride listener necesita el requestId
                    println("DEBUG: Ride request eliminado, continuando búsqueda de active ride...")
                }
            }
        }
    }

    LaunchedEffect(currentRideRequestId) {
        currentRideRequestId?.let { requestId ->
            activeRideRepository.getActiveRideByRequestId(requestId).collect { ride ->
                activeRide = ride
                driverLocation = ride?.driverLocation
                if (ride != null) {
                    isSearchingDriver = false
                }
            }
        } ?: run {
            // Si no hay requestId, limpiar el activeRide
            activeRide = null
            driverLocation = null
        }
    }

    // Estados de animación para el header
    var headerVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    // Estado global para animar el header tanto en modo teclado como en modo pin
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val headerShiftTarget = when {
        isSelectingDestination -> HEADER_SHIFT_FRACTION_PIN
        isKeyboardVisible -> HEADER_SHIFT_FRACTION_KEYBOARD
        else -> 0f
    }
    val headerShiftFraction by animateFloatAsState(
        targetValue = headerShiftTarget,
        animationSpec = spring(
            stiffness = Spring.StiffnessVeryLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "headerShiftFractionGlobal"
    )

    // Comprobar permisos actuales
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = fine || coarse
        
        // Iniciar animaciones de entrada
        headerVisible = true
        kotlinx.coroutines.delay(300)
        contentVisible = true
    }

    // Lanzador para solicitar permisos de ubicación en tiempo de ejecución
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
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        var isStyleLoaded by remember { mutableStateOf(false) }
        LaunchedEffect(mapViewRef) {
            val view = mapViewRef
            if (view != null && !isStyleLoaded) {
                view.mapboxMap.loadStyle(style(style = Style.MAPBOX_STREETS) { }) {
                    isStyleLoaded = true
                    driverAnnotationManager = view.annotations.createPointAnnotationManager()
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                    view.location.updateSettings { enabled = hasLocationPermission }
                    view.scalebar.enabled = false
                    if (hasLocationPermission) {
                        val indicatorListener = object : OnIndicatorPositionChangedListener {
                            override fun onIndicatorPositionChanged(point: Point) {
                                val isValid = point.latitude() in -90.0..90.0 &&
                                        point.longitude() in -180.0..180.0 &&
                                        !(point.latitude() == 0.0 && point.longitude() == 0.0)
                                val target = if (isValid) point else Point.fromLngLat(-71.0589, 42.3601)
                                val zoom = if (isValid) 14.0 else 12.0
                                view.mapboxMap.setCamera(
                                    CameraOptions.Builder()
                                        .center(target)
                                        .zoom(zoom)
                                        .build()
                                )
                                userLocation = target
                                view.location.removeOnIndicatorPositionChangedListener(this)
                            }
                        }
                        view.location.addOnIndicatorPositionChangedListener(indicatorListener)
                    } else {
                        val bostonLocation = Point.fromLngLat(-71.0589, 42.3601)
                        view.mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(bostonLocation)
                                .zoom(12.0)
                                .build()
                        )
                        userLocation = bostonLocation
                    }
                }
            }
        }

        // Overlay para dibujar la ruta con efectos visuales mejorados
        // Mostrar la ruta durante opciones de viaje, búsqueda y viaje activo
        if (routeOffsets.isNotEmpty() && (isRideOptionsVisible || isSearchingDriver || activeRide != null)) {
            // Animación de gradiente para la ruta
            val routeAnimation by animateFloatAsState(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing)
                ),
                label = "routeGradientAnimation"
            )
            
            // Animación de pulso para el brillo
            val pulseAnimation by animateFloatAsState(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = LinearEasing)
                ),
                label = "routePulseAnimation"
            )
            
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path()
                val sizeMap = mapView.mapboxMap.getSize()
                val w = sizeMap.width.toFloat()
                val h = sizeMap.height.toFloat()
                var inSegment = false
                routeOffsets.forEach { offset ->
                    val visible = offset.x in 0f..w && offset.y in 0f..h
                    if (visible) {
                        if (!inSegment) {
                            path.moveTo(offset.x, offset.y)
                            inSegment = true
                        } else {
                            path.lineTo(offset.x, offset.y)
                        }
                    } else {
                        inSegment = false
                    }
                }
                
                // Dibujar línea base con gradiente animado
                val gradientColors = listOf(
                    Color(0xFF08817E), // Teal principal
                    Color(0xFF0FB9B1), // Teal claro
                    Color(0xFF1E1F47), // Índigo
                    Color(0xFF3A3B7B)  // Índigo claro
                )
                
                // Crear gradiente lineal animado
                val gradientBrush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, size.height * (1f - routeAnimation)),
                    end = Offset(size.width * routeAnimation, 0f)
                )
                
                // Línea principal con gradiente
                drawPath(
                    path = path,
                    brush = gradientBrush,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 8.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
                
                // Línea exterior con brillo
                val glowBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF08817E).copy(alpha = 0.3f * pulseAnimation),
                        Color(0xFF0FB9B1).copy(alpha = 0.5f * pulseAnimation),
                        Color(0xFF1E1F47).copy(alpha = 0.3f * pulseAnimation)
                    ),
                    start = Offset(0f, size.height * (1f - routeAnimation)),
                    end = Offset(size.width * routeAnimation, 0f)
                )
                
                drawPath(
                    path = path,
                    brush = glowBrush,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 16.dp.toPx() * pulseAnimation,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
                
                // Línea interior brillante
                val innerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color(0xFF0FB9B1).copy(alpha = 0.9f)
                    ),
                    start = Offset(0f, size.height * (1f - routeAnimation)),
                    end = Offset(size.width * routeAnimation, 0f)
                )
                
                drawPath(
                    path = path,
                    brush = innerBrush,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
                
                // Puntos de destello a lo largo de la ruta
                if (routeOffsets.size > 1) {
                    val sparkles = 5
                    for (i in 0 until sparkles) {
                        val progress = (i.toFloat() / sparkles + routeAnimation) % 1f
                        val index = (progress * (routeOffsets.size - 1)).toInt()
                        if (index < routeOffsets.size) {
                            val sparkleOffset = routeOffsets[index]
                            if (sparkleOffset.x in 0f..w && sparkleOffset.y in 0f..h) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.8f * pulseAnimation),
                                    radius = 4.dp.toPx() * pulseAnimation,
                                    center = sparkleOffset,
                                    blendMode = androidx.compose.ui.graphics.BlendMode.Screen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Icono de destino confirmado anclado al punto geo
        val confirmedOffset = confirmedDestOffset
        if (confirmedDestination != null && confirmedOffset != null && (isRideOptionsVisible || isSearchingDriver)) {
            val iconSize = 42.dp
            val xDp = with(density) { confirmedOffset.x.toDp() }
            val yDp = with(density) { confirmedOffset.y.toDp() }
            Box(modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Outlined.Place,
                    contentDescription = null,
                    tint = Color(0xFF08817E),
                    modifier = Modifier
                        .size(iconSize)
                        .offset(x = xDp - iconSize / 2, y = yDp - iconSize)
                )
                // Botón de regresar - SOLO visible durante opciones de viaje, NO durante búsqueda
                if (isRideOptionsVisible && !isSearchingDriver) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .size(40.dp)
                            .clickable {
                                // Regresar al estado inicial
                                isRideOptionsVisible = false
                                routeOffsets = emptyList()
                                routePoints = emptyList()
                                pickupLocation = null
                                routeDistanceMeters = null
                                routeDurationSeconds = null
                        confirmedDestination = null
                        confirmedDestOffset = null
                        isSelectingDestination = false
                        // Limpiar búsqueda y sugerencias
                        searchQuery = ""
                        suggestions = emptyList()
                        onBottomBarVisibilityChanged(true)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color(0xFF1C1C1E)
                        )
                    }
                }
                } // Fin del if para el botón de regresar
            }
        }

        LaunchedEffect(driverLocation, mapViewRef, activeRide, isCameraLocked) {
            val mapView = mapViewRef
            val driverLoc = driverLocation
            val clientLoc = activeRide?.clientLocation
            if (mapView != null && isStyleLoaded && !isCameraLocked && isValidGeoPoint(driverLoc)) {
                driverOffset = null
                showDriverIcon = false
                val dl = driverLoc!!
                val driverPoint = com.mapbox.geojson.Point.fromLngLat(dl.longitude, dl.latitude)
                val pam = driverAnnotationManager
                if (pam != null) {
                    val options = PointAnnotationOptions()
                        .withPoint(driverPoint)
                        .withIconImage(createDriverIcon(context))
                        .withIconSize(1.0)
                    driverAnnotation?.let { existing ->
                        try { pam.delete(existing) } catch (_: Exception) {}
                        driverAnnotation = null
                    }
                    driverAnnotation = pam.create(options)
                }
                val map = mapView.mapboxMap
                val clientValid = clientLoc != null &&
                    clientLoc.latitude in -90.0..90.0 &&
                    clientLoc.longitude in -180.0..180.0 &&
                    !(clientLoc.latitude == 0.0 && clientLoc.longitude == 0.0)
                if (clientValid) {
                    val clientPoint = com.mapbox.geojson.Point.fromLngLat(clientLoc!!.longitude, clientLoc.latitude)
                    val dLat = Math.toRadians(clientLoc.latitude - dl.latitude)
                    val dLon = Math.toRadians(clientLoc.longitude - dl.longitude)
                    val a = kotlin.math.sin(dLat/2).pow(2.0) + kotlin.math.cos(Math.toRadians(dl.latitude)) * kotlin.math.cos(Math.toRadians(clientLoc.latitude)) * kotlin.math.sin(dLon/2).pow(2.0)
                    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1-a))
                    val distMeters = 6371000.0 * c
                    val closeEnough = distMeters < 50000.0
                    if (closeEnough) {
                        val cam = map.cameraForCoordinates(
                            listOf(clientPoint, driverPoint),
                            CameraOptions.Builder().build(),
                            com.mapbox.maps.EdgeInsets(80.0, 40.0, 620.0, 40.0), // Más espacio abajo para que aparezca más arriba
                            null,
                            null
                        )
                        val z = (cam.zoom ?: 14.0).coerceAtLeast(8.0) // Zoom más lejano (menor número)
                        val center = cam.center ?: driverPoint
                        val prev = lastValidCenter
                        val distToPrev = if (prev != null) {
                            val dLat2 = Math.toRadians(center.latitude() - prev.latitude())
                            val dLon2 = Math.toRadians(center.longitude() - prev.longitude())
                            val a2 = kotlin.math.sin(dLat2/2).pow(2.0) + kotlin.math.cos(Math.toRadians(prev.latitude())) * kotlin.math.cos(Math.toRadians(center.latitude())) * kotlin.math.sin(dLon2/2).pow(2.0)
                            val c2 = 2 * kotlin.math.atan2(kotlin.math.sqrt(a2), kotlin.math.sqrt(1-a2))
                            6371000.0 * c2
                        } else 0.0
                        if (prev == null || distToPrev < 150000.0) {
                            val adjusted = CameraOptions.Builder()
                                .center(center)
                                .zoom(z)
                                .bearing(cam.bearing)
                                .pitch(cam.pitch)
                                .build()
                            map.setCamera(adjusted)
                            lastValidCenter = center
                            lastValidZoom = z
                        }
                    } else {
                        map.setCamera(
                            CameraOptions.Builder()
                                .center(driverPoint)
                                .zoom(13.0) // Zoom más lejano cuando no están cerca
                                .build()
                        )
                        lastValidCenter = driverPoint
                        lastValidZoom = 13.0
                    }
                } else {
                    map.setCamera(
                        CameraOptions.Builder()
                            .center(driverPoint)
                            .zoom(13.0) // Zoom más lejano cuando no hay cliente
                            .build()
                    )
                    lastValidCenter = driverPoint
                    lastValidZoom = 13.0
                }
            } else if (mapView != null && isStyleLoaded && !isCameraLocked && driverLoc == null) {
                // Limpiar el icono del conductor cuando no hay ubicación válida
                val pam = driverAnnotationManager
                if (pam != null && driverAnnotation != null) {
                    try { pam.delete(driverAnnotation!!) } catch (_: Exception) {}
                    driverAnnotation = null
                }
            }
        }

        // Icono de conductor ahora se maneja con PointAnnotation en el mapa

        DisposableEffect(activeRide) {
            val pam = driverAnnotationManager
            onDispose {
                try {
                    if (pam != null && driverAnnotation != null) {
                        pam.delete(driverAnnotation!!)
                        driverAnnotation = null
                    }
                } catch (_: Exception) {}
            }
        }
        
        // Limpiar icono cuando no hay activeRide o driverLocation
        LaunchedEffect(activeRide, driverLocation) {
            if (activeRide == null || driverLocation == null) {
                val pam = driverAnnotationManager
                if (pam != null && driverAnnotation != null) {
                    try { pam.delete(driverAnnotation!!) } catch (_: Exception) {}
                    driverAnnotation = null
                }
            }
        }

        // Actualizar la ruta cuando el mapa se mueve (para que se "pegue" al mapa)
        // Mantener la ruta anclada durante opciones de viaje, búsqueda y viaje activo
        LaunchedEffect(isRideOptionsVisible, isSearchingDriver, activeRide, routePoints) {
            val gestures = mapView.gestures
            routeMoveListenerRef?.let { gestures.removeOnMoveListener(it) }
            if ((isRideOptionsVisible || isSearchingDriver || activeRide != null) && routePoints.isNotEmpty()) {
                val listener = object : OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {}
                    override fun onMove(detector: MoveGestureDetector): Boolean {
                        routeOffsets = routePoints.map { p ->
                            val sc = mapView.mapboxMap.pixelForCoordinate(p)
                            Offset(sc.x.toFloat(), sc.y.toFloat())
                        }
                        confirmedDestination?.let { d ->
                            val sc2 = mapView.mapboxMap.pixelForCoordinate(d)
                            confirmedDestOffset = Offset(sc2.x.toFloat(), sc2.y.toFloat())
                        }
                        return false
                    }
                    override fun onMoveEnd(detector: MoveGestureDetector) {
                        routeOffsets = routePoints.map { p ->
                            val sc = mapView.mapboxMap.pixelForCoordinate(p)
                            Offset(sc.x.toFloat(), sc.y.toFloat())
                        }
                        confirmedDestination?.let { d ->
                            val sc2 = mapView.mapboxMap.pixelForCoordinate(d)
                            confirmedDestOffset = Offset(sc2.x.toFloat(), sc2.y.toFloat())
                        }
                    }
                }
                gestures.addOnMoveListener(listener)
                routeMoveListenerRef = listener
            } else {
                routeMoveListenerRef = null
            }
        }

        // Recalcular offsets también en cambios de cámara (zoom/tilt), para evitar saltos abruptos
        // Mantener la ruta anclada durante opciones de viaje, búsqueda y viaje activo
        DisposableEffect(isRideOptionsVisible, isSearchingDriver, activeRide, routePoints) {
            val map = mapView.mapboxMap
            val cameraListener: (com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData) -> Unit = {
                if ((isRideOptionsVisible || isSearchingDriver || activeRide != null) && routePoints.isNotEmpty()) {
                    routeOffsets = routePoints.map { p ->
                        val sc = map.pixelForCoordinate(p)
                        Offset(sc.x.toFloat(), sc.y.toFloat())
                    }
                    confirmedDestination?.let { d ->
                        val sc2 = map.pixelForCoordinate(d)
                        confirmedDestOffset = Offset(sc2.x.toFloat(), sc2.y.toFloat())
                    }
                }
            }
            map.addOnCameraChangeListener(cameraListener)
            onDispose { map.removeOnCameraChangeListener(cameraListener) }
        }

        // Calcular ruta entre conductor y pasajero durante viaje activo
        LaunchedEffect(activeRide, driverLocation) {
            val ride = activeRide
            val driverLoc = driverLocation
            val clientLoc = ride?.clientLocation
            val mapView = mapViewRef
            if (ride != null && driverLoc != null && clientLoc != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val origin = Point.fromLngLat(driverLoc.longitude, driverLoc.latitude)
                        val destination = Point.fromLngLat(clientLoc.longitude, clientLoc.latitude)
                        val directionsUrl = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/${origin.longitude()},${origin.latitude()};${destination.longitude()},${destination.latitude()}?alternatives=false&geometries=geojson&overview=full&access_token=$mapboxToken"
                        val req = Request.Builder().url(directionsUrl).get().build()
                        var body: String? = null
                        var success = false
                        httpClient.newCall(req).execute().use { resp ->
                            success = resp.isSuccessful
                            body = resp.body?.string()
                        }
                        var lineString: LineString? = null
                        if (success && body != null) {
                            val json = JSONObject(body)
                            val routes = json.optJSONArray("routes")
                            val first = routes?.optJSONObject(0)
                            val geom = first?.optJSONObject("geometry")
                            val coords = geom?.optJSONArray("coordinates")
                            if (coords != null && coords.length() > 1) {
                                val pts = mutableListOf<Point>()
                                for (i in 0 until coords.length()) {
                                    val c = coords.getJSONArray(i)
                                    val lon = c.optDouble(0)
                                    val lat = c.optDouble(1)
                                    pts.add(Point.fromLngLat(lon, lat))
                                }
                                lineString = LineString.fromLngLats(pts)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            val ls = lineString
                            if (ls != null) {
                                routePoints = ls.coordinates()
                                val view = mapView
                                if (view != null) {
                                    val offsets = ls.coordinates().map { p ->
                                        val sc = view.mapboxMap.pixelForCoordinate(p)
                                        Offset(sc.x.toFloat(), sc.y.toFloat())
                                    }
                                    routeOffsets = offsets
                                    val coords = ls.coordinates().filter { it.latitude() in -90.0..90.0 && it.longitude() in -180.0..180.0 && !(it.latitude() == 0.0 && it.longitude() == 0.0) }
                                    if (coords.size >= 2) {
                                        val map = view.mapboxMap
                                        val cs = map.cameraState
                                        val cam = map.cameraForCoordinates(
                                            coords,
                                            CameraOptions.Builder()
                                                .bearing(cs.bearing)
                                                .pitch(cs.pitch)
                                                .build(),
                                            EdgeInsets(80.0, 40.0, 100.0, 40.0),
                                            null,
                                            null
                                        )
                                        val center = cam.center
                                        val zoom = (cam.zoom ?: 14.0).coerceAtLeast(13.5)
                                        if (center != null) {
                                            val prev = lastValidCenter
                                            val dLat = Math.toRadians(center.latitude() - (prev?.latitude() ?: center.latitude()))
                                            val dLon = Math.toRadians(center.longitude() - (prev?.longitude() ?: center.longitude()))
                                            val a = kotlin.math.sin(dLat/2).pow(2.0) + kotlin.math.cos(Math.toRadians(prev?.latitude() ?: center.latitude())) * kotlin.math.cos(Math.toRadians(center.latitude())) * kotlin.math.sin(dLon/2).pow(2.0)
                                            val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1-a))
                                            val dist = 6371000.0 * c
                                            if (prev == null || dist < 150000.0) {
                                                map.setCamera(
                                                    CameraOptions.Builder()
                                                        .center(center)
                                                        .zoom(zoom)
                                                        .bearing(cam.bearing)
                                                        .pitch(cam.pitch)
                                                        .build()
                                                )
                                                lastValidCenter = center
                                                lastValidZoom = zoom
                                            }
                                        }
                                    }
                                } else {
                                    routeOffsets = emptyList()
                                }
                            }
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            routePoints = emptyList()
                            routeOffsets = emptyList()
                        }
                    }
                }
            } else if (ride == null) {
                routePoints = emptyList()
                routeOffsets = emptyList()
            }
        }

        // Actualizar destino según se mueva el mapa cuando el modo está activo
        LaunchedEffect(isSelectingDestination) {
            val gestures = mapView.gestures
            moveListenerRef?.let { gestures.removeOnMoveListener(it) }
            if (isSelectingDestination) {
                // Oculta el BottomNavbar mientras se muestra el pin
                onBottomBarVisibilityChanged(false)
                // Inicializa destino a la PUNTA del pin (centro-bottom)
                val center = mapView.mapboxMap.cameraState.center
                val centerPx = mapView.mapboxMap.pixelForCoordinate(center)
                val tipOffsetPx = with(density) { (pinSizeDp / 2).toPx() }
                val tipSC = com.mapbox.maps.ScreenCoordinate(centerPx.x, centerPx.y + tipOffsetPx)
                selectedDestination = mapView.mapboxMap.coordinateForPixel(tipSC)
                // Geocodificar dirección inicial
                scope.launch(Dispatchers.IO) {
                    val p = selectedDestination
                    val addr = try {
                        if (p != null) {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val res = withContext(Dispatchers.IO) { geocoder.getFromLocation(p.latitude(), p.longitude(), 1) }
                            res?.firstOrNull()?.getAddressLine(0)
                        } else null
                    } catch (e: Exception) { null }
                    withContext(Dispatchers.Main) { pinSearchQuery = addr ?: "${p?.latitude()}, ${p?.longitude()}" }
                }
                val listener = object : OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {}
                    override fun onMove(detector: MoveGestureDetector): Boolean {
                        val c = mapView.mapboxMap.cameraState.center
                        val cPx = mapView.mapboxMap.pixelForCoordinate(c)
                        val tipOffsetPx = with(density) { (pinSizeDp / 2).toPx() }
                        val tipSC = com.mapbox.maps.ScreenCoordinate(cPx.x, cPx.y + tipOffsetPx)
                        selectedDestination = mapView.mapboxMap.coordinateForPixel(tipSC)
                        return false
                    }
                    override fun onMoveEnd(detector: MoveGestureDetector) {
                        val c = mapView.mapboxMap.cameraState.center
                        val cPx = mapView.mapboxMap.pixelForCoordinate(c)
                        val tipOffsetPx = with(density) { (pinSizeDp / 2).toPx() }
                        val tipSC = com.mapbox.maps.ScreenCoordinate(cPx.x, cPx.y + tipOffsetPx)
                        selectedDestination = mapView.mapboxMap.coordinateForPixel(tipSC)
                        // Geocodificar al terminar el movimiento
                        scope.launch(Dispatchers.IO) {
                            val addr = try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val tip = selectedDestination
                                val res = withContext(Dispatchers.IO) {
                                    if (tip != null) geocoder.getFromLocation(tip.latitude(), tip.longitude(), 1) else null
                                }
                                res?.firstOrNull()?.getAddressLine(0)
                            } catch (e: Exception) { null }
                            withContext(Dispatchers.Main) {
                                val tip = selectedDestination
                                pinSearchQuery = addr ?: "${tip?.latitude()}, ${tip?.longitude()}"
                            }
                        }
                    }
                }
                gestures.addOnMoveListener(listener)
                moveListenerRef = listener
            } else {
                // Restaura la visibilidad del BottomNavbar (puede sobreescribirse por opciones de viaje)
                onBottomBarVisibilityChanged(true)
                moveListenerRef = null
            }
        }

        // Control centralizado de visibilidad del BottomNavbar:
        // oculto si está activo el modo pin, panel de opciones de viaje o búsqueda de conductor.
        LaunchedEffect(isSelectingDestination, isRideOptionsVisible, isSearchingDriver) {
            val visible = !(isSelectingDestination || isRideOptionsVisible || isSearchingDriver)
            onBottomBarVisibilityChanged(visible)
        }

        // Sugerencias de búsqueda cuando el header está visible (no modo pin)
        LaunchedEffect(searchQuery, isSelectingDestination, userLocation) {
            if (!isSelectingDestination && searchQuery.trim().length >= 2) {
                // Pequeño debounce para evitar múltiples llamadas rápidas
                kotlinx.coroutines.delay(300)
                val center = userLocation
                if (center != null) {
                    val bbox = computeBBoxMiles(center, 15.0)
                    val url = buildGeocodingUrl(
                        token = mapboxToken,
                        query = searchQuery.trim(),
                        center = center,
                        bbox = bbox
                    )
                    scope.launch(Dispatchers.IO) {
                        val request = Request.Builder().url(url).get().build()
                        var newSuggestions: List<GeocodeSuggestion> = emptyList()
                        try {
                            var bodyStr = "{}"
                            httpClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    bodyStr = response.body?.string() ?: "{}"
                                }
                            }
                            val json = JSONObject(bodyStr)
                            val features = json.optJSONArray("features")
                            newSuggestions = (0 until (features?.length() ?: 0)).map { i ->
                                val f = features!!.getJSONObject(i)
                                val text = f.optString("text")
                                val addressNum = f.optString("address")
                                val centerArr = f.optJSONArray("center")
                                val lon = centerArr?.optDouble(0) ?: 0.0
                                val lat = centerArr?.optDouble(1) ?: 0.0

                                val title = buildString {
                                    if (addressNum.isNotBlank()) append("$addressNum ")
                                    append(text)
                                }.trim()

                                val ctx = f.optJSONArray("context")
                                val parts = mutableListOf<String>()
                                for (j in 0 until (ctx?.length() ?: 0)) {
                                    val c = ctx!!.getJSONObject(j)
                                    val id = c.optString("id")
                                    val t = c.optString("text")
                                    val allowed = id.startsWith("place.") || id.startsWith("locality.") || id.startsWith("neighborhood.")
                                    val excluded = id.startsWith("region.") || id.startsWith("postcode.") || id.startsWith("country.") || id.startsWith("district.")
                                    if (allowed && !excluded && t.isNotBlank()) parts.add(t)
                                }
                                val subtitle = if (parts.isNotEmpty()) parts.joinToString(", ") else null

                                GeocodeSuggestion(
                                    title = title,
                                    subtitle = subtitle,
                                    point = Point.fromLngLat(lon, lat)
                                )
                            }
                        } catch (_: Exception) {
                            newSuggestions = emptyList()
                        }
                        withContext(Dispatchers.Main) { suggestions = newSuggestions }
                    }
                }
            } else {
                suggestions = emptyList()
            }
        }

        // Sugerencias de búsqueda para el modo pin
        LaunchedEffect(pinSearchQuery, isSelectingDestination, userLocation) {
            if (isSelectingDestination && pinSearchQuery.trim().length >= 2) {
                // Pequeño debounce para evitar múltiples llamadas rápidas
                kotlinx.coroutines.delay(300)
                val center = userLocation
                if (center != null) {
                    val bbox = computeBBoxMiles(center, 15.0)
                    val url = buildGeocodingUrl(
                        token = mapboxToken,
                        query = pinSearchQuery.trim(),
                        center = center,
                        bbox = bbox
                    )
                    scope.launch(Dispatchers.IO) {
                        val request = Request.Builder().url(url).get().build()
                        var newSuggestions: List<GeocodeSuggestion> = emptyList()
                        try {
                            var bodyStr = "{}"
                            httpClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    bodyStr = response.body?.string() ?: "{}"
                                }
                            }
                            val json = JSONObject(bodyStr)
                            val features = json.optJSONArray("features")
                            newSuggestions = (0 until (features?.length() ?: 0)).map { i ->
                                val f = features!!.getJSONObject(i)
                                val text = f.optString("text")
                                val addressNum = f.optString("address")
                                val centerArr = f.optJSONArray("center")
                                val lon = centerArr?.optDouble(0) ?: 0.0
                                val lat = centerArr?.optDouble(1) ?: 0.0

                                val title = buildString {
                                    if (addressNum.isNotBlank()) append("$addressNum ")
                                    append(text)
                                }.trim()

                                val ctx = f.optJSONArray("context")
                                val parts = mutableListOf<String>()
                                for (j in 0 until (ctx?.length() ?: 0)) {
                                    val c = ctx!!.getJSONObject(j)
                                    val id = c.optString("id")
                                    val t = c.optString("text")
                                    val allowed = id.startsWith("place.") || id.startsWith("locality.") || id.startsWith("neighborhood.")
                                    val excluded = id.startsWith("region.") || id.startsWith("postcode.") || id.startsWith("country.") || id.startsWith("district.")
                                    if (allowed && !excluded && t.isNotBlank()) parts.add(t)
                                }
                                val subtitle = if (parts.isNotEmpty()) parts.joinToString(", ") else null

                                GeocodeSuggestion(
                                    title = title,
                                    subtitle = subtitle,
                                    point = Point.fromLngLat(lon, lat)
                                )
                            }
                        } catch (_: Exception) {
                            newSuggestions = emptyList()
                        }
                        withContext(Dispatchers.Main) { pinSuggestions = newSuggestions }
                    }
                }
            } else {
                pinSuggestions = emptyList()
            }
        }

        if (!isSelectingDestination && !isRideOptionsVisible && !isSearchingDriver && activeRide == null) {
            // Estado inicial: mostrar header completo con gradiente, títulos y atajos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .drawBehind {
                        val teal = Color(0xFF08817E)
                        val indigo = Color(0xFF1E1F47)
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
                    visible = headerVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 45.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // Mostrar títulos solo cuando el teclado NO está visible
                    if (!isKeyboardVisible) {
                        Text(
                            text = "intu",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(30.dp))
                        Text(
                            text = "¡Hola Carlos!",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HeaderSearchBar(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            onFocusChange = { focused -> isSearchFocused = focused },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        // Dropdown de sugerencias
                        if (suggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .offset(y = 56.dp)
                                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    suggestions.take(6).forEach { s ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // Mover el mapa al punto elegido
                                                    mapView.mapboxMap.setCamera(
                                                        CameraOptions.Builder()
                                                            .center(s.point)
                                                            .zoom(14.0)
                                                            .build()
                                                    )
                                                    // Activar modo pin y fijar destino al punto seleccionado
                                                    isSelectingDestination = true
                                                    selectedDestination = s.point
                                                    // Actualizar el campo de búsqueda con el título elegido
                                                    searchQuery = s.title
                                                    // Ocultar sugerencias
                                                    suggestions = emptyList()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.Place, contentDescription = null, tint = Color(0xFF1C1C1E))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Text(text = s.title, color = Color(0xFF1C1C1E), style = MaterialTheme.typography.bodyMedium)
                                                if (s.subtitle != null) {
                                                    Text(text = s.subtitle, color = Color(0xFF6E6E73), style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    ShortcutRowHeader(
                        onPinClick = { isSelectingDestination = !isSelectingDestination },
                        isPinActive = isSelectingDestination
                    )
                }
                }
            }
        } else if (isSelectingDestination && !isSearchingDriver && activeRide == null) {
            // Modo pin: usar la misma animación global (25% de página)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .drawBehind {
                        val teal = Color(0xFF08817E)
                        val indigo = Color(0xFF1E1F47)
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
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    Column {
                        // Botón de regresar en esquina superior izquierda
                        Card(
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 16.dp, start = 16.dp)
                                .size(40.dp)
                                .clickable {
                                    // Reset al estado inicial
                                    isSelectingDestination = false
                                    selectedDestination = null
                                    searchQuery = ""
                                    pinSearchQuery = ""
                                    pinSuggestions = emptyList()
                                    hasUserInteractedWithPinSearch = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Regresar",
                                    tint = Color(0xFF1C1C1E)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HeaderSearchBar(
                                value = pinSearchQuery, 
                                onValueChange = { 
                                    pinSearchQuery = it 
                                    hasUserInteractedWithPinSearch = true
                                }, 
                                onFocusChange = { focused -> 
                                    isPinSearchFocused = focused 
                                    if (focused) hasUserInteractedWithPinSearch = true
                                },
                                modifier = Modifier.padding(horizontal = 8.dp),
                                placeholderText = "Dirección del marcador",
                                showClearButton = true,
                                onClearClick = { 
                                    pinSearchQuery = ""
                                    pinSuggestions = emptyList()
                                    hasUserInteractedWithPinSearch = false
                                },
                                showMicButton = false
                            )
                            // Dropdown de sugerencias para búsqueda de pin
                            if (pinSuggestions.isNotEmpty() && hasUserInteractedWithPinSearch) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .offset(y = 8.dp)
                                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        items(pinSuggestions) { suggestion ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        // Mover el pin a la ubicación seleccionada
                                                        selectedDestination = suggestion.point
                                                        pinSearchQuery = suggestion.title
                                                        pinSuggestions = emptyList()
                                                        hasUserInteractedWithPinSearch = false
                                                        // Centrar el mapa en el nuevo punto
                                                        mapView.mapboxMap.setCamera(
                                                            CameraOptions.Builder()
                                                                .center(suggestion.point)
                                                                .zoom(15.0)
                                                                .build()
                                                        )
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                            ) {
                                                Text(
                                                    text = suggestion.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF1C1C1E)
                                                )
                                                if (suggestion.subtitle != null) {
                                                    Text(
                                                        text = suggestion.subtitle,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF8E8E93)
                                                    )
                                                }
                                            }
                                            Divider(
                                                color = Color(0xFFE5E5EA),
                                                thickness = 1.dp,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Panel de opciones tipo Uber cuando el destino está confirmado
        AnimatedVisibility(
            visible = isRideOptionsVisible && !isSearchingDriver,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it })
        ) {
            val km = (routeDistanceMeters ?: 0.0) / 1000.0
            val minutes = (routeDurationSeconds ?: 0.0) / 60.0
            // Vista previa de la misma fórmula que Supabase vuelve a calcular al crear el viaje.
            val mototaxiFare = kotlin.math.round(maxOf(4.0, 2.5 + km + 0.1 * minutes) * 10.0) / 10.0

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 60.dp)
                        .onGloballyPositioned { coords -> rideOptionsPanelHeightPx = coords.size.height },
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF08817E).copy(alpha = 0.4f),
                                Color(0xFF1E1F47).copy(alpha = 0.4f)
                            )
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Header moderno con icono y título
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF08817E), Color(0xFF1E1F47))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Elige tu viaje",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFF1C1C1E),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Tarifa calculada por distancia y tiempo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6E6E73)
                                )
                            }
                        }
                        
                        // Crear lista de opciones con colores dinámicos
                        val optionRows = listOf(
                            RideOptionData(
                                name = "Mototaxi",
                                price = mototaxiFare,
                                minutes = minutes,
                                leadingContent = {
                                    Icon(
                                        Icons.Filled.DirectionsCar,
                                        contentDescription = "Mototaxi",
                                        tint = Color(0xFF08817E),
                                        modifier = Modifier.size(52.dp)
                                    )
                                },
                                colors = listOf(Color(0xFF08817E), Color(0xFF1E1F47))
                            )
                        )

                        // Slider horizontal de opciones
                        RideOptionsSlider(
                            options = optionRows,
                            selectedOptionName = selectedRideOptionName,
                            onOptionSelected = { selectedRideOptionName = it },
                            initialSelectedIndex = 0
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Compact payment method selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .clickable { 
                                    // Toggle payment method
                                    val newMethod = if (selectedPaymentMethod == "efectivo") "yape_plin" else "efectivo"
                                    selectedPaymentMethod = newMethod
                                    scope.launch {
                                        paymentPreferences.savePaymentMethod(newMethod)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Icono más grande para efectivo o Yape
                                if (selectedPaymentMethod == "efectivo") {
                                    com.intu.taxi.ui.components.CashIcon(
                                        size = 28.dp,
                                        modifier = Modifier
                                    )
                                } else {
                                    com.intu.taxi.ui.components.YapePlinIcon(
                                        size = 28.dp,
                                        modifier = Modifier
                                    )
                                }
                                Text(
                                    text = "Método de pago",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF6E6E73)
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (selectedPaymentMethod == "efectivo") "Efectivo" else "Yape",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF1C1C1E),
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Cambiar",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF08817E)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        val enabled = selectedRideOptionName != null && userLocation != null && confirmedDestination != null
                        println("DEBUG UI: selectedRideOptionName=$selectedRideOptionName, enabled=$enabled")
                        println("DEBUG UI: userLocation=$userLocation, confirmedDestination=$confirmedDestination")
                        println("DEBUG UI: routeDistanceMeters=$routeDistanceMeters, routeDurationSeconds=$routeDurationSeconds")
                        val phaseAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                        LaunchedEffect(selectedRideOptionName) {
                            if (selectedRideOptionName == "espera y ahorra" || selectedRideOptionName == "Intu Colectivo") {
                                while (true) {
                                    val duration = if (selectedRideOptionName == "espera y ahorra") 2400 else 3200
                                    phaseAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = duration,
                                            easing = androidx.compose.animation.core.LinearEasing
                                        )
                                    )
                                    phaseAnim.snapTo(0f)
                                }
                            } else {
                                // Reset animación cuando cambia a otra opción
                                phaseAnim.snapTo(0f)
                            }
                        }
                        val phase = phaseAnim.value
                        val brush = when (selectedRideOptionName) {
                            "Intu Honda" -> Brush.radialGradient(
                                colors = listOf(Color(0xFF0FB9B1), Color(0xFF08817E)),
                                center = Offset(0.3f, 0.3f),
                                radius = with(LocalDensity.current) { 180.dp.toPx() }
                            )
                            "Intu Bajaj" -> Brush.radialGradient(
                                colors = listOf(Color(0xFF0FB9B1), Color(0xFF08817E)),
                                center = Offset(0.3f, 0.3f),
                                radius = with(LocalDensity.current) { 180.dp.toPx() }
                            )
                            "Intu Colectivo" -> {
                                // Similar a "espera y ahorra" pero diagonal y un poco más lento
                                val startY = 60f + phase * 400f
                                val endY = startY - 240f
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF27AE60),
                                        Color(0xFF2ECC71),
                                        Color(0xFF27AE60)
                                    ),
                                    start = Offset(0f, startY),
                                    end = Offset(220f, endY)
                                )
                            }
                            "espera y ahorra" -> {
                                // Animado: banda luminosa que recorre el botón
                                val startX = 100f + phase * 500f
                                val endX = startX - 300f
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF27AE60),
                                        Color(0xFF2ECC71),
                                        Color(0xFF27AE60)
                                    ),
                                    start = Offset(startX, 0f),
                                    end = Offset(endX, 200f)
                                )
                            }
                            else -> null
                        }
                        Button(
                            onClick = {
                                // Crear solicitud de viaje en Firebase y entrar en estado de búsqueda
                                val origin = pickupLocation ?: userLocation
                                val destination = confirmedDestination
                                val rideType = selectedRideOptionName
                                val distance = routeDistanceMeters ?: 0.0
                                val duration = routeDurationSeconds ?: 0.0
                                
                                println("DEBUG: Botón confirmar clickeado")
                                println("DEBUG: origin=$origin, destination=$destination, rideType=$rideType")
                                println("DEBUG: distance=$distance, duration=$duration")
                                
                                if (origin != null && destination != null && rideType != null) {
                                    // Validar que las coordenadas sean válidas (no 0.0 y dentro de rangos razonables)
                                    val originLat = origin.latitude()
                                    val originLng = origin.longitude()
                                    val destLat = destination.latitude()
                                    val destLng = destination.longitude()
                                    
                                    if (originLat == 0.0 || originLng == 0.0 || 
                                        originLat < -90.0 || originLat > 90.0 || 
                                        originLng < -180.0 || originLng > 180.0) {
                                        Toast.makeText(context, "Error: Ubicación de origen no válida", Toast.LENGTH_LONG).show()
                                        println("DEBUG: Coordenadas de origen inválidas - lat: $originLat, lng: $originLng")
                                        return@Button
                                    }
                                    
                                    if (destLat == 0.0 || destLng == 0.0 || 
                                        destLat < -90.0 || destLat > 90.0 || 
                                        destLng < -180.0 || destLng > 180.0) {
                                        Toast.makeText(context, "Error: Ubicación de destino no válida", Toast.LENGTH_LONG).show()
                                        println("DEBUG: Coordenadas de destino inválidas - lat: $destLat, lng: $destLng")
                                        return@Button
                                    }
                                    
                                    // Verificar autenticación de Firebase
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    if (currentUser == null) {
                                        errorMessage = "Error: Usuario no autenticado. Por favor inicia sesión."
                                        println("DEBUG: Usuario no autenticado")
                                    } else {
                                        scope.launch {
                                            try {
                                                // Obtener direcciones para origen y destino
                                                val originGeocoder = Geocoder(context, Locale.getDefault())
                                                originAddress = try {
                                                    val results = withContext(Dispatchers.IO) {
                                                        originGeocoder.getFromLocation(origin.latitude(), origin.longitude(), 1)
                                                    }
                                                    results?.firstOrNull()?.getAddressLine(0) ?: "Ubicación actual"
                                                } catch (e: Exception) {
                                                    "Ubicación actual"
                                                }
                                                
                                                val destGeocoder = Geocoder(context, Locale.getDefault())
                                                destinationAddress = try {
                                                    val results = withContext(Dispatchers.IO) {
                                                        destGeocoder.getFromLocation(destination.latitude(), destination.longitude(), 1)
                                                    }
                                                    results?.firstOrNull()?.getAddressLine(0) ?: "Destino seleccionado"
                                                } catch (e: Exception) {
                                                    "Destino seleccionado"
                                                }
                                                
                                                // Calcular precio estimado (tarifa base + por km + por tiempo)
                                                val baseFare = 2.5
                                                val perKmRate = 1.0
                                                val perMinuteRate = 0.1
                                                val distanceKm = distance / 1000.0
                                                val durationMinutes = duration / 60.0
                                                estimatedPrice = kotlin.math.round(
                                                    maxOf(4.0, baseFare + (distanceKm * perKmRate) + (durationMinutes * perMinuteRate)) * 10.0
                                                ) / 10.0
                                                
                                                // Crear solicitud en Firebase
                                                println("DEBUG: Creando solicitud en Firebase...")
                                                println("DEBUG: Coordenadas a enviar - originLat: ${origin.latitude()}, originLng: ${origin.longitude()}")
                                                println("DEBUG: Coordenadas a enviar - destLat: ${destination.latitude()}, destLng: ${destination.longitude()}")
                                                val result = rideRequestRepository.createRideRequest(
                                                    originLatitude = origin.latitude(),
                                                    originLongitude = origin.longitude(),
                                                    originAddress = originAddress,
                                                    destinationLatitude = destination.latitude(),
                                                    destinationLongitude = destination.longitude(),
                                                    destinationAddress = destinationAddress,
                                                    distanceMeters = distance,
                                                    durationSeconds = duration,
                                                    estimatedPrice = estimatedPrice,
                                                    rideType = rideType,
                                                    paymentMethod = selectedPaymentMethod
                                                )
                                                
                                                result.onSuccess { requestId ->
                                                    println("DEBUG: Solicitud creada exitosamente: $requestId")
                                                    currentRideRequestId = requestId
                                                    isSearchingDriver = true
                                                    isRideOptionsVisible = false
                                                    errorMessage = null
                                                }.onFailure { error ->
                                                    // Mostrar error (podrías agregar un Snackbar aquí)
                                                    println("Error al crear solicitud: ${error.message}")
                                                    errorMessage = "Error: ${error.message}"
                                                }
                                            } catch (e: Exception) {
                                                println("Error inesperado: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (enabled) Color.Transparent else Color(0xFFC7C7CC),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFC7C7CC),
                                disabledContentColor = Color.White.copy(alpha = 0.7f)
                            ),
                            enabled = enabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .let { base ->
                                    if (enabled) base.background(
                                        brush = brush ?: Brush.linearGradient(
                                            colors = listOf(Color(0xFF1C1C1E), Color(0xFF3A3A3C)),
                                            start = Offset(0f, 0f),
                                            end = Offset(300f, 200f)
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    ) else base
                                }
                        ) {
                            Text("Confirmar viaje")
                        }
                        
                        // Mostrar mensaje de error si existe
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Ajusta la cámara para encuadrar la ruta completa cuando se muestran opciones de viaje o durante búsqueda
        LaunchedEffect(isRideOptionsVisible, isSearchingDriver, activeRide, routePoints, rideOptionsPanelHeightPx) {
            if ((isRideOptionsVisible || (isSearchingDriver && activeRide == null)) && routePoints.isNotEmpty()) {
                val minLon = routePoints.minOf { it.longitude() }
                val maxLon = routePoints.maxOf { it.longitude() }
                val minLat = routePoints.minOf { it.latitude() }
                val maxLat = routePoints.maxOf { it.latitude() }
                val bounds = CoordinateBounds(
                    Point.fromLngLat(minLon, minLat),
                    Point.fromLngLat(maxLon, maxLat),
                    false
                )
                val topPadPx = with(density) { 16.dp.toPx() }.toDouble()
                val leftPadPx = with(density) { 16.dp.toPx() }.toDouble()
                val rightPadPx = with(density) { 16.dp.toPx() }.toDouble()
                // Sumamos el alto del panel medido + el margen inferior solicitado en el Card
                val bottomMarginPx = with(density) { 40.dp.toPx() }.toDouble()
                val bottomPadPx = rideOptionsPanelHeightPx.toDouble() + bottomMarginPx
                // Extra padding para lograr un leve "zoom out"
                val extraPadPx = with(density) { 60.dp.toPx() }.toDouble()
                val map = mapView.mapboxMap
                val cs = map.cameraState
                val cam = map.cameraForCoordinates(
                    routePoints,
                    CameraOptions.Builder()
                        .bearing(cs.bearing)
                        .pitch(cs.pitch)
                        .build(),
                    EdgeInsets(
                        topPadPx + extraPadPx,
                        leftPadPx + extraPadPx,
                        bottomPadPx + extraPadPx,
                        rightPadPx + extraPadPx
                    ),
                    null,
                    null
                )
                if (activeRide == null) {
                    map.setCamera(cam)
                }
            }
        }

        // Aviso ligero cuando el modo de selección está activo
        if (isSelectingDestination) {
            // Pin centrado fijo para indicar el destino
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Place,
                    contentDescription = null,
                    tint = if (isSelectingPickup) Color(0xFF27AE60) else Color(0xFFFF3B30),
                    modifier = Modifier.size(42.dp)
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .padding(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isSelectingPickup) {
                        Button(
                            onClick = {
                                destinationBeforePickup = selectedDestination
                                isSelectingPickup = true
                                userLocation?.let { current ->
                                    mapView.mapboxMap.setCamera(CameraOptions.Builder().center(current).zoom(16.0).build())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E1F47)),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(if (pickupLocation == null) "Elegir punto de recojo" else "Cambiar punto de recojo")
                        }
                    } else {
                        Text(
                            "Mueve el mapa hasta el punto de recojo",
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    // Botón con gradient radial igual al header
                    Button(
                        onClick = {
                            if (isSelectingPickup) {
                                pickupLocation = selectedDestination
                                isSelectingPickup = false
                                destinationBeforePickup?.let { destination ->
                                    selectedDestination = destination
                                    mapView.mapboxMap.setCamera(CameraOptions.Builder().center(destination).zoom(15.0).build())
                                }
                                Toast.makeText(context, "Punto de recojo guardado", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Confirmar destino y mostrar ruta desde la ubicación actual
                            val origin = pickupLocation ?: userLocation
                            val destination = selectedDestination
                            if (origin != null && destination != null) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val directionsUrl = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/${origin.longitude()},${origin.latitude()};${destination.longitude()},${destination.latitude()}?alternatives=false&geometries=geojson&overview=full&access_token=$mapboxToken"
                                        val req = Request.Builder().url(directionsUrl).get().build()
                                        var body: String? = null
                                        var success = false
                                        httpClient.newCall(req).execute().use { resp ->
                                            success = resp.isSuccessful
                                            body = resp.body?.string()
                                        }
                                        var lineString: LineString? = null
                                        if (success && body != null) {
                                            val json = JSONObject(body)
                                            val routes = json.optJSONArray("routes")
                                            val first = routes?.optJSONObject(0)
                                            val geom = first?.optJSONObject("geometry")
                                            val coords = geom?.optJSONArray("coordinates")
                                            if (coords != null && coords.length() > 1) {
                                                val pts = mutableListOf<Point>()
                                                for (i in 0 until coords.length()) {
                                                    val c = coords.getJSONArray(i)
                                                    val lon = c.optDouble(0)
                                                    val lat = c.optDouble(1)
                                                    pts.add(Point.fromLngLat(lon, lat))
                                                }
                                                lineString = LineString.fromLngLats(pts)
                                            }
                                        }
                        withContext(Dispatchers.Main) {
                                            val ls = lineString
                                            if (ls != null) {
                                                // Convertimos a coordenadas de pantalla y dibujamos en overlay (Canvas)
                                                val offsets = ls.coordinates().map { p ->
                                                    val sc = mapView.mapboxMap.pixelForCoordinate(p)
                                                    Offset(sc.x.toFloat(), sc.y.toFloat())
                                                }
                                                routeOffsets = offsets
                                                routePoints = ls.coordinates()
                                                // Guarda destino confirmado y su posición en pantalla
                                                confirmedDestination = destination
                                                val destSC = mapView.mapboxMap.pixelForCoordinate(destination)
                                                confirmedDestOffset = Offset(destSC.x.toFloat(), destSC.y.toFloat())
                                                // Intentar leer distancia y duración
                                                try {
                                            val jsonObj = JSONObject(body!!)
                                                    val routesArr = jsonObj.optJSONArray("routes")
                                                    val firstRoute = routesArr?.optJSONObject(0)
                                                    routeDistanceMeters = firstRoute?.optDouble("distance")
                                                    routeDurationSeconds = firstRoute?.optDouble("duration")
                                                } catch (_: Exception) {}
                                                // Mostrar panel de opciones
                                                isRideOptionsVisible = true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignorar errores de red/parseo por ahora
                                    }
                                }
                            }
                            // Salir del modo selección
                            isSelectingDestination = false
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .width(220.dp)
                            .height(50.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF08817E), Color(0xFF1E1F47)),
                                    center = Offset(0.1f, 0.1f),
                                    radius = with(LocalDensity.current) { 140.dp.toPx() }
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Text(if (isSelectingPickup) "Confirmar punto de recojo" else "Confirmar destino")
                    }
                   
                }
            }
        }

        activeRide?.let { ride ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 76.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = when (ride.status) {
                            "accepted" -> "Tu mototaxi está en camino"
                            "arrived" -> "Tu conductor llegó"
                            "in_progress" -> "Viaje en curso"
                            "completed" -> "Viaje finalizado"
                            "cancelled" -> "Viaje cancelado"
                            else -> "Buscando conductor"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (ride.driverName.isNotBlank()) Text("Conductor: ${ride.driverName}")
                    if (ride.vehiclePlate.isNotBlank()) Text("Mototaxi: ${ride.vehicleDescription} · ${ride.vehiclePlate}")
                    Text("Total: S/ ${String.format("%.2f", ride.fare)}")
                    Text(
                        if (ride.paymentMethod == "yape_plin") {
                            "Pago por Yape al conductor${if (ride.driverPhone.isNotBlank()) ": ${ride.driverPhone}" else ""}"
                        } else "Pago en efectivo al conductor"
                    )
                    if (ride.status == "completed") {
                        Text("El conductor confirmó que recibió el pago.", color = Color(0xFF08817E))
                        Button(
                            onClick = {
                                currentRideRequestId = null
                                activeRide = null
                                driverLocation = null
                                routePoints = emptyList()
                                pickupLocation = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Listo") }
                    }
                }
            }
        }

        // Indicador creativo de búsqueda de conductor con animaciones de radar
        CreativeDriverSearchIndicator(
            isVisible = isSearchingDriver,
            onCancel = {
                    // Cancelar búsqueda y eliminar completamente de Firebase
                    scope.launch {
                        currentRideRequestId?.let { requestId ->
                            println("DEBUG: Cancelando búsqueda y eliminando request: $requestId")
                            rideRequestRepository.cancelRideRequest(requestId)
                                .onSuccess {
                                    println("DEBUG: Request eliminado exitosamente de Firebase")
                                }
                                .onFailure { error ->
                                    println("DEBUG: Error al eliminar request: ${error.message}")
                                    // Aunque falle la eliminación, continuamos con la limpieza local
                                }
                        }
                    
                    // Limpiar estado local independientemente del resultado de Firebase
                    isSearchingDriver = false
                    currentRideRequestId = null
                    
                    // Limpiar el campo de búsqueda y destino para nueva búsqueda
                    searchQuery = ""
                    selectedDestination = null
                    pickupLocation = null
                    confirmedDestination = null
                    confirmedDestOffset = null
                    routePoints = emptyList()
                    routeDistanceMeters = null
                    routeDurationSeconds = null
                    suggestions = emptyList()
                    
                    println("DEBUG: Estado de búsqueda reiniciado y campos limpiados")
                    println("DEBUG: El ride request permanece en Firebase con estado 'cancelled' para historial")
                }
            }
        )
    }
}

private fun buildGeocodingUrl(token: String, query: String, center: Point, bbox: String): String {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val lon = center.longitude()
    val lat = center.latitude()
    return "https://api.mapbox.com/geocoding/v5/mapbox.places/$encoded.json" +
        "?access_token=$token" +
        "&language=es" +
        "&autocomplete=true" +
        "&limit=5" +
        "&types=address,place,poi" +
        "&proximity=$lon,$lat" +
        "&bbox=$bbox"
}

private fun computeBBoxMiles(center: Point, miles: Double): String {
    val lat = center.latitude()
    val lon = center.longitude()
    val dLat = miles / 69.0
    val dLon = miles / (69.0 * cos(Math.toRadians(lat)))
    val minLon = lon - dLon
    val minLat = lat - dLat
    val maxLon = lon + dLon
    val maxLat = lat + dLat
    return "$minLon,$minLat,$maxLon,$maxLat"
}

private data class GeocodeSuggestion(
    val title: String,
    val subtitle: String?,
    val point: Point
)
@Composable
private fun HeaderSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    placeholderText: String = "¿A dónde quieres ir?",
    showClearButton: Boolean = false,
    onClearClick: () -> Unit = {},
    showMicButton: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholderText, color = Color(0xFF7A7F87)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF8E8E93)) },
            trailingIcon = {
                if (showClearButton && value.isNotEmpty()) {
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "Limpiar",
                            tint = Color(0xFF8E8E93)
                        )
                    }
                } else if (showMicButton) {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = Color(0xFF8E8E93))
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = 0.85f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.85f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) }
        )
    }
}

@Composable
private fun RideOptionCard(
    name: String,
    price: Double,
    minutes: Double,
    leadingContent: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val priceStr = String.format(Locale.getDefault(), "$%.2f", price)
    val etaMin = kotlin.math.max(1.0, minutes)
    val etaStr = String.format(Locale.getDefault(), "~%.0f min", etaMin)
    
    // Colores dinámicos basados en el tipo de viaje
    val cardColors = when (name) {
        "Intu Honda" -> listOf(Color(0xFF08817E), Color(0xFF0FB9B1))
        "Intu Bajaj" -> listOf(Color(0xFF1E1F47), Color(0xFF3A3B7B))
        "Intu Colectivo" -> listOf(Color(0xFF27AE60), Color(0xFF2ECC71))
        "espera y ahorra" -> listOf(Color(0xFFF39C12), Color(0xFFE67E22))
        "entrega de paquete" -> listOf(Color(0xFF8E44AD), Color(0xFF9B59B6))
        else -> listOf(Color(0xFF08817E), Color(0xFF1E1F47))
    }
    
    // Animación de escala cuando está seleccionado
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "cardScale"
    )
    
    // Efecto de brillo cuando está seleccionado
    val glowAlpha by animateFloatAsState(
        targetValue = if (selected) 0.3f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "glowAlpha"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (selected) 0.95f else 0.8f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 12.dp else 4.dp
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            brush = Brush.linearGradient(
                colors = if (selected) cardColors else listOf(Color(0xFFE0E0E0), Color(0xFFE0E0E0))
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cardColors[0].copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(0.8f, 0.2f),
                        radius = 200f
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sección izquierda con imagen y detalles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Contenedor de imagen con efecto glassmorphism
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = cardColors,
                                    start = Offset(0f, 0f),
                                    end = Offset(100f, 100f)
                                )
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.6f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (leadingContent != null) {
                            leadingContent()
                        } else {
                            Icon(
                                Icons.Outlined.Place,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Información del servicio
                    Column {
                        Text(
                            text = name.replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1C1C1E),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF6E6E73),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = etaStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6E6E73)
                            )
                        }
                    }
                }
                
                // Sección derecha con precio
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = priceStr,
                        style = MaterialTheme.typography.titleLarge,
                        color = cardColors[0],
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selected) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Seleccionado",
                                tint = cardColors[0],
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Seleccionado",
                                style = MaterialTheme.typography.labelSmall,
                                color = cardColors[0],
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutRowHeader(onPinClick: () -> Unit, isPinActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        ShortcutCardHeader(
            icon = { Icon(Icons.Outlined.Home, contentDescription = null, tint = Color.White) },
            label = "Casa",
            isActive = true
        )
        ShortcutCardHeader(
            icon = { Icon(Icons.Outlined.Work, contentDescription = null, tint = Color.White) },
            label = "Trabajo"
        )
        ShortcutCardHeader(
            icon = { Icon(Icons.Outlined.Place, contentDescription = null, tint = Color.White) },
            label = "Marcador",
            isActive = isPinActive,
            onClick = onPinClick
        )
       
    }
}

@Composable
private fun ShortcutCardHeader(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .size(width = 76.dp, height = 68.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                icon()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .offset(x = 6.dp, y = 6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5F8AFE))
                )
            }
        }
    }
}
fun createDriverIcon(context: android.content.Context): Bitmap {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_driver_car)
    if (drawable == null) {
        val fallbackSize = 64
        val bmp = Bitmap.createBitmap(fallbackSize, fallbackSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = AndroidColor.parseColor("#1C1C1E")
        paint.style = Paint.Style.FILL
        val path = Path()
        path.moveTo(fallbackSize * 0.5f, fallbackSize * 0.12f)
        path.lineTo(fallbackSize * 0.72f, fallbackSize * 0.72f)
        path.lineTo(fallbackSize * 0.28f, fallbackSize * 0.72f)
        path.close()
        canvas.drawPath(path, paint)
        return bmp
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 64
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 64
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp
}
private fun isValidGeoPoint(g: com.google.firebase.firestore.GeoPoint?): Boolean {
    if (g == null) return false
    val lat = g.latitude
    val lon = g.longitude
    if (lat == 0.0 && lon == 0.0) return false
    if (lat !in -90.0..90.0) return false
    if (lon !in -180.0..180.0) return false
    return true
}
