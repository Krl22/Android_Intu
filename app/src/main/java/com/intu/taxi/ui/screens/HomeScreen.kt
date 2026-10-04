package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.intu.taxi.R
import com.intu.taxi.ui.map.rememberMapViewWithLifecycle
import com.intu.taxi.ui.map.PinSelectionMapController
import com.intu.taxi.ui.map.pointUnderCenterPin
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.style

import com.mapbox.maps.plugin.locationcomponent.location
import com.intu.taxi.location.AdminLocationSimulation
import com.intu.taxi.ui.map.MapLocationBinding
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
import com.intu.taxi.data.CatalogPlace
import com.intu.taxi.data.searchCatalog
import com.intu.taxi.data.mergePlaceSearchResults
import com.intu.taxi.repositories.AddressSearchRepository
import com.intu.taxi.repositories.PlaceCatalogRepository
import kotlinx.coroutines.CancellationException
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
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.unit.Dp
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.EdgeInsets
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.intu.taxi.ui.map.TripMap
import com.intu.taxi.ui.map.TripRoute
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import com.mapbox.maps.CoordinateBounds
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.repositories.RideRequestRepository
import com.intu.taxi.data.PaymentPreferences
import kotlinx.coroutines.tasks.await
 

private const val HEADER_SHIFT_FRACTION_KEYBOARD = 0.25f
private const val HEADER_SHIFT_FRACTION_PIN = 0.5f

@Composable
fun HomeScreen(
    padding: PaddingValues,
    routeLoader: (suspend (Point, Point) -> TripRoute?)? = null,
    rideRequestSender: (suspend (RideBooking) -> Result<String>)? = null,
    locationProvider: com.mapbox.maps.plugin.locationcomponent.LocationProvider? = null,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {}
) {
    val mapboxToken = stringResource(id = com.intu.taxi.R.string.mapbox_access_token)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    var hasLocationPermission by rememberSaveable { mutableStateOf(false) }
    // No persistir el modo pin entre recomposiciones/navegaciones para no ocultar el BottomBar al iniciar
    var isSelectingDestination by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<Point?>(null) }
    var pickupLocation by remember { mutableStateOf<Point?>(null) }
    var showPickupPicker by remember { mutableStateOf(false) }
    var selectedPickup by remember { mutableStateOf<Point?>(null) }
    var isCreatingRideRequest by remember { mutableStateOf(false) }
    var isCalculatingDestinationRoute by remember { mutableStateOf(false) }
    var pendingDestination by remember { mutableStateOf<Point?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val placesUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val savedPlacesStore = remember(placesUid) {
        placesUid.takeIf { it.isNotBlank() }?.let { com.intu.taxi.data.SavedPlaces(context, it) }
    }
    val savedPlacesFlow = remember(savedPlacesStore) {
        savedPlacesStore?.changes ?: kotlinx.coroutines.flow.flowOf(emptyList<com.intu.taxi.data.SavedPlace>())
    }
    val savedPlaces by savedPlacesFlow.collectAsState(initial = savedPlacesStore?.read().orEmpty())
    val catalogRepository = remember { PlaceCatalogRepository(context) }
    var catalog by remember { mutableStateOf(catalogRepository.cached()) }
    var catalogLoading by remember { mutableStateOf(false) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var catalogRefresh by remember { mutableStateOf(0) }
    LaunchedEffect(placesUid, catalogRefresh) {
        catalogLoading = true
        catalogError = null
        try { catalog = catalogRepository.sync(force = catalogRefresh > 0) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { catalogError = e.message ?: "No se pudieron actualizar los lugares." }
        finally { catalogLoading = false }
    }
    var showSavedPlaces by remember { mutableStateOf(false) }
    var pinSearchQuery by rememberSaveable { mutableStateOf("") }
    val testLocation by AdminLocationSimulation.preset.collectAsState()
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var suggestions by remember { mutableStateOf(listOf<CatalogPlace>()) }
    var pinSuggestions by remember { mutableStateOf(listOf<CatalogPlace>()) }
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
    val showingRideOptions = isRideOptionsVisible && !showPickupPicker
    val isSelectingPoint = isSelectingDestination || showPickupPicker
    val selectedPoint = if (showPickupPicker) selectedPickup else selectedDestination
    var routeDistanceMeters by remember { mutableStateOf<Double?>(null) }
    var routeDurationSeconds by remember { mutableStateOf<Double?>(null) }
    var selectedMotoOptionCode by rememberSaveable { mutableStateOf<String?>(com.intu.taxi.models.MotoOption.ANY.code) }
    var isDelivery by rememberSaveable { mutableStateOf(false) }
    var pendingDeliveryOrigin by remember { mutableStateOf<Point?>(null) }
    var pendingDeliveryRoute by remember { mutableStateOf<TripRoute?>(null) }
    var deliveryDraft by remember { mutableStateOf<com.intu.taxi.models.DeliveryDetails?>(null) }
    
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
    // Alto de la tarjeta del viaje, para que la cámara no ponga la ruta detrás de ella
    var rideCardHeightPx by remember { mutableStateOf(0) }
    // Último gesto del pasajero sobre el mapa; mientras explora, la cámara no lo interrumpe
    var lastUserGestureMs by remember { mutableStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCancelRideDialog by remember { mutableStateOf(false) }
    // PIN de seguridad que el pasajero le dicta al conductor al subir
    var ridePin by remember { mutableStateOf<String?>(null) }
    var isCancellingRide by remember { mutableStateOf(false) }
    var greetingName by rememberSaveable { mutableStateOf("") }

    // Nombre para el saludo: perfil guardado o, si no hay, el nombre de la cuenta de Google
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@LaunchedEffect
        val profileName = runCatching { com.intu.taxi.auth.AuthRepository().getUserProfile(user.uid)?.firstName }.getOrNull()
        greetingName = (profileName?.takeIf { it.isNotBlank() } ?: user.displayName.orEmpty())
            .trim()
            .substringBefore(' ')
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        runCatching { activeRideRepository.findOpenRideForRider(uid) }
            .getOrNull()?.let { ride ->
                currentRideRequestId = ride.rideId
                isDelivery = ride.isDelivery
                isSearchingDriver = ride.status == "searching"
                activeRide = if (ride.status == "searching") null else ride
            }
    }

    // Payment preferences
    val paymentPreferences = remember { PaymentPreferences(context) }
    var selectedPaymentMethod by remember { mutableStateOf("efectivo") }
    val httpClient = remember { OkHttpClient() }
    val addressSearchRepository = remember(mapboxToken) { AddressSearchRepository(mapboxToken) }

    // Descarta solo la preparación del viaje; nunca cancela una solicitud ya enviada.
    fun returnHomeFromPreparation() {
        isRideOptionsVisible = false
        isSelectingDestination = false
        showPickupPicker = false
        selectedPickup = null
        pendingDestination = null
        isCalculatingDestinationRoute = false
        selectedDestination = null
        confirmedDestination = null
        confirmedDestOffset = null
        pickupLocation = null
        routePoints = emptyList()
        routeOffsets = emptyList()
        routeDistanceMeters = null
        routeDurationSeconds = null
        rideOptionsPanelHeightPx = 0
        selectedMotoOptionCode = com.intu.taxi.models.MotoOption.ANY.code
        isDelivery = false
        pendingDeliveryOrigin = null
        pendingDeliveryRoute = null
        deliveryDraft = null
        originAddress = ""
        destinationAddress = ""
        estimatedPrice = 0.0
        searchQuery = ""
        pinSearchQuery = ""
        suggestions = emptyList()
        pinSuggestions = emptyList()
        isSearchFocused = false
        isPinSearchFocused = false
        hasUserInteractedWithPinSearch = false
        errorMessage = null
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        onBottomBarVisibilityChanged(true)
        userLocation?.let { point ->
            mapViewRef?.mapboxMap?.setCamera(CameraOptions.Builder()
                .center(point).zoom(14.0).bearing(0.0).pitch(0.0)
                .padding(EdgeInsets(0.0, 0.0, 0.0, 0.0)).build())
        }
    }

    BackHandler(enabled = (isRideOptionsVisible || isSelectingPoint || isCalculatingDestinationRoute ||
        isSearchFocused || searchQuery.isNotBlank()) && !showSavedPlaces &&
        !isCreatingRideRequest && !isSearchingDriver && currentRideRequestId == null && activeRide == null) {
        returnHomeFromPreparation()
    }

    suspend fun loadBookingRoute(origin: Point, destination: Point): TripRoute? =
        if (routeLoader != null) routeLoader(origin, destination) else TripMap.fetchRoute(mapboxToken, origin, destination)

    fun confirmDestination(destination: Point) {
        if (isCalculatingDestinationRoute || isCreatingRideRequest) return
        errorMessage = null
        selectedDestination = destination
        isCalculatingDestinationRoute = true
        pendingDestination = destination
    }

    LaunchedEffect(pendingDestination, userLocation != null) {
        val destination = pendingDestination ?: return@LaunchedEffect
        val origin = pickupLocation ?: userLocation ?: return@LaunchedEffect
        try {
            val route = loadBookingRoute(origin, destination)
                ?: throw IllegalStateException("No se pudo calcular la ruta. Revisa tu conexión e intenta de nuevo.")
            // A route that finishes after Back must not reopen the discarded draft.
            if (pendingDestination != destination) return@LaunchedEffect
            routePoints = route.points
            routeOffsets = mapViewRef?.mapboxMap?.let { map -> route.points.map { point ->
                val pixel = map.pixelForCoordinate(point)
                Offset(pixel.x.toFloat(), pixel.y.toFloat())
            } }.orEmpty()
            routeDistanceMeters = route.distanceMeters
            routeDurationSeconds = route.durationSeconds
            confirmedDestination = destination
            mapViewRef?.mapboxMap?.pixelForCoordinate(destination)?.let { pixel ->
                confirmedDestOffset = Offset(pixel.x.toFloat(), pixel.y.toFloat())
            }
            isSelectingDestination = false
            isRideOptionsVisible = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = e.message ?: "No se pudo calcular la ruta. Intenta de nuevo."
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        } finally {
            if (pendingDestination == destination) {
                pendingDestination = null
                isCalculatingDestinationRoute = false
            }
        }
    }

    if (isCalculatingDestinationRoute && !isSelectingDestination) AlertDialog(
        onDismissRequest = ::returnHomeFromPreparation,
        title = { Text("Preparando tu viaje") },
        text = { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(if (userLocation == null) "Buscando tu ubicación…" else "Calculando la ruta…")
        } },
        confirmButton = {},
        dismissButton = { TextButton(onClick = ::returnHomeFromPreparation) { Text("Cancelar") } }
    )

    fun requestRideFromPickup(origin: Point, delivery: com.intu.taxi.models.DeliveryDetails? = null, routeSnapshot: TripRoute? = null) {
        val destination = confirmedDestination ?: return
        val option = com.intu.taxi.models.MotoOption.fromCode(selectedMotoOptionCode) ?: return
        val rideType = option.vehicleType
        if (isCreatingRideRequest || currentRideRequestId != null) return
        isCreatingRideRequest = true
        errorMessage = null
        scope.launch {
            try {
                val route = routeSnapshot ?: loadBookingRoute(origin, destination)
                    ?: throw IllegalStateException("No se pudo calcular la ruta desde el punto de recojo. Intenta de nuevo.")
                val booking = RideBooking(origin, destination, route, rideType, selectedPaymentMethod, delivery, option.preferredBrand)
                pickupLocation = origin
                routePoints = route.points
                routeDistanceMeters = route.distanceMeters
                routeDurationSeconds = route.durationSeconds
                estimatedPrice = booking.estimatedPrice
                val result = if (rideRequestSender != null) rideRequestSender(booking) else {
                    check(FirebaseAuth.getInstance().currentUser != null) { "Inicia sesión para solicitar el viaje." }
                    originAddress = readableAddress(context, httpClient, mapboxToken, origin) ?: "Punto de recojo en el mapa"
                    destinationAddress = readableAddress(context, httpClient, mapboxToken, destination) ?: "Destino en el mapa"
                    rideRequestRepository.createRideRequest(
                        originLatitude = origin.latitude(), originLongitude = origin.longitude(), originAddress = originAddress,
                        destinationLatitude = destination.latitude(), destinationLongitude = destination.longitude(), destinationAddress = destinationAddress,
                        distanceMeters = route.distanceMeters, durationSeconds = route.durationSeconds, estimatedPrice = booking.estimatedPrice,
                        rideType = rideType, paymentMethod = booking.paymentMethod,
                        routeGeometry = LineString.fromLngLats(route.points).toJson(), delivery = delivery, preferredVehicleBrand = option.preferredBrand
                    )
                }
                result.onSuccess { requestId ->
                    currentRideRequestId = requestId
                    isSearchingDriver = true
                    isRideOptionsVisible = false
                }.onFailure { error -> errorMessage = error.message ?: "No se pudo solicitar el viaje. Intenta de nuevo." }
                showPickupPicker = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = e.message ?: "No se pudo solicitar el viaje. Intenta de nuevo."
                showPickupPicker = false
            } finally { isCreatingRideRequest = false }
        }
    }

    LaunchedEffect(pendingDeliveryOrigin) {
        val origin = pendingDeliveryOrigin ?: return@LaunchedEffect
        val destination = confirmedDestination ?: return@LaunchedEffect
        try {
            pendingDeliveryRoute = loadBookingRoute(origin, destination)
                ?: error("No se pudo calcular la ruta del envío. Intenta de nuevo.")
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            errorMessage = e.message ?: "No se pudo preparar el envío."
            pendingDeliveryOrigin = null
        }
    }
    val deliveryOrigin = pendingDeliveryOrigin
    val deliveryRoute = pendingDeliveryRoute
    if (deliveryOrigin != null && deliveryRoute == null) AlertDialog(
        onDismissRequest = { pendingDeliveryOrigin = null }, title = { Text("Preparando envío") },
        text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.size(24.dp)); Text("Calculando la tarifa desde el recojo…")
        } }, confirmButton = {}, dismissButton = { TextButton(onClick = { pendingDeliveryOrigin = null }) { Text("Cancelar") } }
    )
    if (deliveryOrigin != null && deliveryRoute != null) DeliveryDetailsDialog(
        fare = com.intu.taxi.models.ServiceFare.estimate(deliveryRoute.distanceMeters, deliveryRoute.durationSeconds, true),
        initial = deliveryDraft,
        onDismiss = { pendingDeliveryOrigin = null; pendingDeliveryRoute = null },
        onConfirm = { details ->
            deliveryDraft = details
            pendingDeliveryOrigin = null
            pendingDeliveryRoute = null
            requestRideFromPickup(deliveryOrigin, details, deliveryRoute)
        }
    )

    fun beginPickupSelection() {
        val initial = pickupLocation ?: userLocation ?: return
        errorMessage = null
        selectedPickup = initial
        pinSearchQuery = ""
        pinSuggestions = emptyList()
        hasUserInteractedWithPinSearch = false
        isPinSearchFocused = false
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        showPickupPicker = true
        mapViewRef?.mapboxMap?.setCamera(CameraOptions.Builder().center(initial).zoom(16.0)
            .bearing(0.0).pitch(0.0).padding(EdgeInsets(0.0, 0.0, 0.0, 0.0)).build())
    }

    if (showPickupPicker && isCreatingRideRequest) AlertDialog(
        onDismissRequest = {},
        title = { Text(if (isDelivery) "Solicitando envío" else "Solicitando viaje") },
        text = { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Calculando la ruta desde tu punto de recojo…")
        } },
        confirmButton = {}
    )
    
    if (showSavedPlaces && placesUid.isNotBlank()) SavedPlacesDialog(onDismiss = { showSavedPlaces = false })

    // Load saved payment method
    LaunchedEffect(Unit) {
        paymentPreferences.paymentMethod.collect { method ->
            selectedPaymentMethod = method
        }
    }

    // El PIN se pide una vez por cambio de estado (no en cada sondeo) y solo mientras sirve
    LaunchedEffect(activeRide?.rideId, activeRide?.status) {
        val ride = activeRide
        ridePin = if (ride != null && ride.status in setOf("accepted", "arrived")) {
            activeRideRepository.startPin(ride.rideId)
        } else null
    }

    // Un solo sondeo del viaje: decide entre la animación de búsqueda y la tarjeta del viaje
    LaunchedEffect(currentRideRequestId) {
        currentRideRequestId?.let { requestId ->
            // Mientras haya viaje abierto, un servicio mantiene la app al día aunque esté minimizada
            com.intu.taxi.rider.RiderTrip.searching(requestId, isDelivery)
            com.intu.taxi.rider.RideTrackingService.start(context)
            activeRideRepository.getActiveRideByRequestId(requestId).collect { ride ->
                if (ride == null) return@collect
                isDelivery = ride.isDelivery
                com.intu.taxi.rider.RiderTrip.update(ride)
                when (ride.status) {
                    // Sin conductor todavía, o el conductor canceló y la solicitud volvió a abrirse
                    "searching" -> {
                        activeRide = null
                        driverLocation = null
                        isSearchingDriver = true
                    }
                    // Cancelado fuera de esta pantalla (p. ej. nadie aceptó en 5 minutos)
                    "cancelled" -> {
                        if (isSearchingDriver && !isCancellingRide) {
                            Toast.makeText(context, "No encontramos un conductor disponible. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                        }
                        activeRide = null
                        driverLocation = null
                        isSearchingDriver = false
                        currentRideRequestId = null
                    }
                    else -> {
                        activeRide = ride
                        driverLocation = ride.driverLocation
                        isSearchingDriver = false
                    }
                }
            }
        } ?: run {
            // Si no hay requestId, limpiar el activeRide (el aviso fijo del viaje se quita al terminar
            // o cancelarse; aquí no, porque al volver de otra pestaña el id se recarga un instante después)
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
        isSelectingPoint -> HEADER_SHIFT_FRACTION_PIN
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

    val isPreparingTrip = isSelectingPoint || isRideOptionsVisible || isSearchingDriver || activeRide != null ||
        isCalculatingDestinationRoute || isCreatingRideRequest || currentRideRequestId != null
    val searchActive = isSearchFocused || isKeyboardVisible || searchQuery.isNotBlank()
    val showTripMap = isPreparingTrip || searchActive

    Box(modifier = Modifier.fillMaxSize()) {
        val mapView = rememberMapViewWithLifecycle(accessToken = mapboxToken)
        mapViewRef = mapView
        // Para saber si la app está visible (con la app minimizada la cámara se mueve sin animación)
        val lifecycleOwner = LocalLifecycleOwner.current
        // Keep GPS and route state ready, but present an opaque landing instead of a map at rest.
        AndroidView(factory = { mapView }, update = { view ->
            view.visibility = if (showTripMap) android.view.View.VISIBLE else android.view.View.INVISIBLE
        }, modifier = Modifier.fillMaxSize()
            .testTag(if (showTripMap) "home-map" else "home-location-engine"))

        val mapStyle = com.intu.taxi.ui.theme.intuMapStyle()
        var isStyleLoaded by remember { mutableStateOf(false) }
        LaunchedEffect(mapViewRef, mapStyle) {
            val view = mapViewRef
            if (view != null) {
                view.mapboxMap.loadStyle(style(style = mapStyle) { }) {
                    isStyleLoaded = true
                    if (driverAnnotationManager == null) driverAnnotationManager = view.annotations.createPointAnnotationManager()
                }
            }
        }
        LaunchedEffect(isStyleLoaded, testLocation) {
            if (isStyleLoaded && !hasLocationPermission && testLocation == null && locationProvider == null) {
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
        MapLocationBinding(mapView, isStyleLoaded, hasLocationPermission || locationProvider != null,
            if (locationProvider == null) testLocation else null, realLocationProvider = locationProvider) { point, first ->
            val replacingLocation = userLocation != null
            userLocation = point
            if (first) {
                if (activeRide == null && !isSearchingDriver) {
                    if (replacingLocation) {
                        // A draft route must not retain a pickup from the previous test city.
                        pickupLocation = null
                        selectedDestination = null
                        confirmedDestination = null
                        confirmedDestOffset = null
                        routePoints = emptyList()
                        routeOffsets = emptyList()
                        routeDistanceMeters = null
                        routeDurationSeconds = null
                        isRideOptionsVisible = false
                        isSelectingDestination = false
                        showPickupPicker = false
                        originAddress = ""
                        onBottomBarVisibilityChanged(true)
                    }
                    mapView.mapboxMap.setCamera(CameraOptions.Builder().center(point)
                        .zoom(if (hasLocationPermission || testLocation != null) 14.0 else 12.0).build())
                }
            }
        }

        // Overlay para dibujar la ruta con efectos visuales mejorados
        // Mostrar la ruta durante opciones de viaje, búsqueda y viaje activo
        if (routeOffsets.isNotEmpty() && (showingRideOptions || isSearchingDriver || activeRide != null)) {
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
                
                // A subtle casing keeps the route readable without glow or sparkles.
                drawPath(
                    path = path,
                    color = com.intu.taxi.ui.map.TripRouteStyle.casingColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = com.intu.taxi.ui.map.TripRouteStyle.casingWidthDp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
                drawPath(
                    path = path,
                    color = com.intu.taxi.ui.map.TripRouteStyle.lineColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = com.intu.taxi.ui.map.TripRouteStyle.lineWidthDp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }
        }
        // Icono de destino confirmado anclado al punto geo
        val confirmedOffset = confirmedDestOffset
        if (confirmedDestination != null && confirmedOffset != null && (showingRideOptions || isSearchingDriver)) {
            val iconSize = 42.dp
            val xDp = with(density) { confirmedOffset.x.toDp() }
            val yDp = with(density) { confirmedOffset.y.toDp() }
            Box(modifier = Modifier.fillMaxSize()) {
                com.intu.taxi.ui.map.RoutePin(
                    description = "Destino confirmado",
                    modifier = Modifier.size(iconSize)
                        .offset(x = xDp - iconSize / 2, y = yDp - iconSize)
                        .testTag("home-confirmed-destination-pin")
                )
                // Botón de regresar - SOLO visible durante opciones de viaje, NO durante búsqueda
                if (showingRideOptions && !isSearchingDriver) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .size(40.dp)
                            .clickable(onClick = ::returnHomeFromPreparation),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppearanceColors.surface.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = AppearanceColors.foreground(Color(0xFF1C1C1E))
                        )
                    }
                }
                } // Fin del if para el botón de regresar
            }
        }

        // Desliza el marcador del conductor hasta [target] en 1.8 s
        suspend fun animateDriverMarker(marker: PointAnnotation, pam: PointAnnotationManager, target: Point) {
            val start = marker.point
            val durationMs = 1800f
            val startTime = withFrameMillis { it }
            while (true) {
                val t = ((withFrameMillis { it } - startTime) / durationMs).coerceIn(0f, 1f)
                // El viaje pudo terminar y el marcador borrarse a mitad de la animación
                if (driverAnnotation !== marker) break
                marker.point = Point.fromLngLat(
                    start.longitude() + (target.longitude() - start.longitude()) * t,
                    start.latitude() + (target.latitude() - start.latitude()) * t
                )
                if (runCatching { pam.update(marker) }.isFailure) break
                if (t >= 1f) break
            }
        }

        // Marcador del conductor: se crea una vez y luego se desliza hasta cada nueva posición.
        // El conductor envía su ubicación cada 2 s; la animación dura casi lo mismo para que el
        // movimiento se vea continuo en vez de saltar. Se escucha el estado directamente (no con
        // claves del efecto) porque Compose deja de recomponer con la app minimizada: así el marcador
        // sigue al conductor en segundo plano (ahí salta sin animación) y al volver ya está en su lugar.
        LaunchedEffect(mapView) {
            snapshotFlow { Triple(driverLocation, isStyleLoaded, driverAnnotationManager) }
                .collectLatest { (dl, loaded, pam) ->
                    if (pam == null || !loaded) return@collectLatest
                    if (!isValidGeoPoint(dl)) {
                        driverAnnotation?.let { runCatching { pam.delete(it) } }
                        driverAnnotation = null
                        return@collectLatest
                    }
                    val target = Point.fromLngLat(dl!!.longitude, dl.latitude)
                    val marker = driverAnnotation
                    if (marker == null) {
                        driverAnnotation = pam.create(
                            PointAnnotationOptions()
                                .withPoint(target)
                                .withIconImage(createDriverIcon(context))
                                .withIconSize(1.0)
                        )
                        return@collectLatest
                    }
                    if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        marker.point = target
                        runCatching { pam.update(marker) }
                        return@collectLatest
                    }
                    animateDriverMarker(marker, pam, target)
                }
        }

        // Si el pasajero mueve el mapa con el dedo, la cámara deja de seguir el viaje unos segundos
        DisposableEffect(mapView) {
            val gestureListener = object : OnMoveListener {
                override fun onMoveBegin(detector: MoveGestureDetector) { lastUserGestureMs = System.currentTimeMillis() }
                override fun onMove(detector: MoveGestureDetector): Boolean = false
                override fun onMoveEnd(detector: MoveGestureDetector) { lastUserGestureMs = System.currentTimeMillis() }
            }
            mapView.gestures.addOnMoveListener(gestureListener)
            onDispose { mapView.gestures.removeOnMoveListener(gestureListener) }
        }

        // Ruta y cámara del viaje: la ruta va del conductor al punto objetivo (recojo o destino) y la
        // cámara encuadra conductor, objetivo y ruta dejando libre la tarjeta del viaje.
        // Corre fuera de la recomposición, que Compose pausa con la app minimizada: así todo sigue al
        // día en segundo plano (el servicio del viaje mantiene viva la app) y al volver ya está listo.
        // La ruta se pide a Mapbox al cambiar de estado o si el conductor se sale de ella; mientras la
        // sigue, solo se recorta el tramo ya recorrido. Como mucho se procesa un cambio por segundo.
        LaunchedEffect(mapView) {
            var routeLeg: String? = null
            snapshotFlow {
                RiderTripMapInput(
                    rideId = activeRide?.rideId,
                    status = activeRide?.status,
                    target = activeRide?.clientLocation,
                    driver = driverLocation,
                    styleLoaded = isStyleLoaded,
                    cardHeightPx = rideCardHeightPx
                )
            }
                .distinctUntilChanged()
                .conflate()
                .collect { input ->
                    if (input.status !in setOf("accepted", "arrived", "in_progress")) {
                        if (routeLeg != null) {
                            routePoints = emptyList()
                            routeOffsets = emptyList()
                            routeLeg = null
                        }
                        return@collect
                    }
                    val driver = input.driver?.takeIf { isValidGeoPoint(it) }?.let { Point.fromLngLat(it.longitude, it.latitude) }
                    val target = input.target?.let { Point.fromLngLat(it.longitude, it.latitude) }
                    val leg = "${input.rideId}:${input.status}"
                    if (driver != null && target != null) {
                        val trimmed = if (leg == routeLeg) TripMap.trimRoute(routePoints, driver) else null
                        val route = trimmed ?: TripMap.fetchRoute(mapboxToken, driver, target)?.points
                        if (route != null) {
                            routePoints = route
                            routeLeg = leg
                        }
                    }
                    if (input.styleLoaded && System.currentTimeMillis() - lastUserGestureMs >= 8_000) {
                        fun px(dp: Dp) = with(density) { dp.toPx().toDouble() }
                        val mapHeight = mapView.height.toDouble()
                        val bottom = (input.cardHeightPx + px(padding.calculateBottomPadding() + 40.dp))
                            .coerceAtMost(mapHeight * 0.6)
                        TripMap.fitCamera(
                            mapView,
                            listOfNotNull(driver, target) + routePoints,
                            EdgeInsets(px(110.dp), px(56.dp), bottom, px(56.dp)),
                            animate = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                        )
                    }
                    // La ruta se dibuja sobre el mapa en píxeles; con la cámara quieta se recalculan aquí
                    routeOffsets = routePoints.map { p ->
                        val sc = mapView.mapboxMap.pixelForCoordinate(p)
                        Offset(sc.x.toFloat(), sc.y.toFloat())
                    }
                    delay(1_000)
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
        LaunchedEffect(showingRideOptions, isSearchingDriver, activeRide, routePoints) {
            val gestures = mapView.gestures
            routeMoveListenerRef?.let { gestures.removeOnMoveListener(it) }
            if ((showingRideOptions || isSearchingDriver || activeRide != null) && routePoints.isNotEmpty()) {
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
        DisposableEffect(showingRideOptions, isSearchingDriver, activeRide, routePoints) {
            val map = mapView.mapboxMap
            val cameraListener: (com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData) -> Unit = {
                if ((showingRideOptions || isSearchingDriver || activeRide != null) && routePoints.isNotEmpty()) {
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

        // La punta del pin y el centro de los gestos comparten el mismo píxel del mapa.
        DisposableEffect(mapView, isSelectingPoint, showPickupPicker) {
            val controller = if (isSelectingPoint) PinSelectionMapController(mapView) { point ->
                val previous = if (showPickupPicker) selectedPickup else selectedDestination
                if (previous == null || TripMap.metersBetween(previous, point) > 0.1) {
                    if (showPickupPicker) selectedPickup = point else selectedDestination = point
                }
            } else null
            onDispose { controller?.close() }
        }

        // Esperar a que termine el movimiento, incluida la inercia, antes de resolver la dirección.
        LaunchedEffect(isSelectingPoint, selectedPoint, isPinSearchFocused, hasUserInteractedWithPinSearch) {
            val point = selectedPoint
            if (isSelectingPoint && point != null && !isPinSearchFocused && !hasUserInteractedWithPinSearch) {
                kotlinx.coroutines.delay(500)
                pinSearchQuery = readableAddress(context, httpClient, mapboxToken, point) ?: "Ubicación seleccionada"
            }
        }

        // Control centralizado de visibilidad del BottomNavbar:
        // oculto si está activo el modo pin, panel de opciones de viaje, búsqueda de conductor o un viaje.
        val hasActiveRide = activeRide != null
        LaunchedEffect(isSelectingPoint, isRideOptionsVisible, isSearchingDriver, hasActiveRide) {
            val visible = !(isSelectingPoint || isRideOptionsVisible || isSearchingDriver || hasActiveRide)
            onBottomBarVisibilityChanged(visible)
        }

        // Local catalog results appear immediately, before the debounced Mapbox address results.
        LaunchedEffect(searchQuery, isSelectingPoint, userLocation, catalog) {
            suggestions = if (!isSelectingPoint) searchCatalog(searchQuery, catalog.places,
                userLocation?.latitude(), userLocation?.longitude()) else emptyList()
        }
        LaunchedEffect(pinSearchQuery, isSelectingPoint, userLocation, catalog) {
            pinSuggestions = if (isSelectingPoint) searchCatalog(pinSearchQuery, catalog.places,
                userLocation?.latitude(), userLocation?.longitude()) else emptyList()
        }
        val searchProximity = userLocation?.let { it.latitude() to it.longitude() }
        val addressSearch = rememberAddressSearch(searchQuery,
            isSearchFocused && !isSelectingPoint && !isRideOptionsVisible && !isSearchingDriver && activeRide == null,
            proximity = searchProximity) {
            addressSearchRepository.search(it, userLocation?.latitude(), userLocation?.longitude())
        }
        val pinAddressSearch = rememberAddressSearch(pinSearchQuery,
            isSelectingPoint && isPinSearchFocused && hasUserInteractedWithPinSearch,
            proximity = searchProximity) {
            addressSearchRepository.search(it, userLocation?.latitude(), userLocation?.longitude())
        }

        if (!isPreparingTrip) {
            CommercialHome(
                padding = padding,
                greetingName = greetingName,
                searchActive = searchActive,
                onTravel = {
                    selectedMotoOptionCode = com.intu.taxi.models.MotoOption.ANY.code
                    isDelivery = false
                    isSelectingDestination = true
                },
                onDelivery = {
                    selectedMotoOptionCode = com.intu.taxi.models.MotoOption.DELIVERY.code
                    isDelivery = true
                    isSelectingDestination = true
                },
                searchContent = {
                    HeaderSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        onFocusChange = { focused -> isSearchFocused = focused },
                        placeholderText = "¿A dónde vamos?",
                        modifier = Modifier.testTag("home-destination-search"),
                        shape = RoundedCornerShape(20.dp),
                        elevation = 0.dp,
                        showClearButton = true,
                        onClearClick = { searchQuery = "" }
                    )
                    if (searchActive) {
                        DestinationSearchPanel(
                            query = searchQuery,
                            savedPlaces = savedPlaces,
                            results = mergePlaceSearchResults(suggestions, addressSearch.results),
                            hasCatalog = catalog.places.isNotEmpty(),
                            loading = catalogLoading,
                            error = catalogError,
                            addressLoading = addressSearch.loading,
                            addressError = addressSearch.error,
                            onSelect = { place ->
                                focusManager.clearFocus()
                                keyboard?.hide()
                                val point = Point.fromLngLat(place.longitude, place.latitude)
                                mapView.mapboxMap.setCamera(CameraOptions.Builder().center(point).zoom(16.0).build())
                                selectedDestination = point
                                searchQuery = place.name
                                suggestions = emptyList()
                                isSelectingDestination = true
                            },
                            onSavedPlaceClick = { place ->
                                focusManager.clearFocus()
                                keyboard?.hide()
                                searchQuery = place.name
                                suggestions = emptyList()
                                confirmDestination(Point.fromLngLat(place.longitude, place.latitude))
                            },
                            onRefresh = { catalogRefresh++ },
                            onPickMap = {
                                focusManager.clearFocus()
                                keyboard?.hide()
                                searchQuery = ""
                                isSelectingDestination = true
                            },
                            onManagePlaces = {
                                keyboard?.hide()
                                showSavedPlaces = true
                            },
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            )
        } else if (isSelectingPoint && !isSearchingDriver && activeRide == null) {
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
                                .clickable(enabled = !isCreatingRideRequest, onClick = ::returnHomeFromPreparation),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AppearanceColors.surface.copy(alpha = 0.9f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Regresar",
                                    tint = AppearanceColors.foreground(Color(0xFF1C1C1E))
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(if (showPickupPicker) "Elegir punto de recojo" else "Elige tu destino",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
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
                                placeholderText = if (showPickupPicker) "Dirección de recojo" else "Dirección del marcador",
                                showClearButton = true,
                                onClearClick = { 
                                    pinSearchQuery = ""
                                    pinSuggestions = emptyList()
                                    hasUserInteractedWithPinSearch = false
                                }
                            )
                            if (hasUserInteractedWithPinSearch && pinSearchQuery.trim().length >= 2) {
                                PlaceSearchPanel(mergePlaceSearchResults(pinSuggestions, pinAddressSearch.results), catalog.places.isNotEmpty(), catalogLoading, catalogError,
                                    onSelect = { place ->
                                        val point = Point.fromLngLat(place.longitude, place.latitude)
                                        if (showPickupPicker) selectedPickup = point else selectedDestination = point
                                        pinSearchQuery = place.name
                                        pinSuggestions = emptyList()
                                        hasUserInteractedWithPinSearch = false
                                        mapView.mapboxMap.setCamera(CameraOptions.Builder().center(point).zoom(16.0).build())
                                    }, onRefresh = { catalogRefresh++ },
                                    onPickMap = { pinSearchQuery = ""; hasUserInteractedWithPinSearch = false },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    addressLoading = pinAddressSearch.loading, addressError = pinAddressSearch.error)
                            }
                        }
                    }
                }
            }
        }

        // The drawer has two visible positions; it never dismisses the booking step.
        AnimatedVisibility(
            visible = showingRideOptions && !isSearchingDriver,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it })
        ) {
            val km = (routeDistanceMeters ?: 0.0) / 1000.0
            val minutes = (routeDurationSeconds ?: 0.0) / 60.0
            RideOptionsDrawer(
                fare = com.intu.taxi.models.ServiceFare.estimate(km * 1000.0, minutes * 60.0),
                deliveryFare = com.intu.taxi.models.ServiceFare.estimate(km * 1000.0, minutes * 60.0, true),
                distanceKm = km, durationMinutes = minutes,
                selectedOption = com.intu.taxi.models.MotoOption.fromCode(selectedMotoOptionCode),
                paymentMethod = selectedPaymentMethod,
                confirmEnabled = selectedMotoOptionCode != null && userLocation != null && confirmedDestination != null,
                error = errorMessage,
                onSelect = { selectedMotoOptionCode = it.code; isDelivery = it.delivery; errorMessage = null },
                onChangePayment = {
                    val newMethod = if (selectedPaymentMethod == "efectivo") "yape_plin" else "efectivo"
                    selectedPaymentMethod = newMethod
                    scope.launch { paymentPreferences.savePaymentMethod(newMethod) }
                },
                onConfirm = ::beginPickupSelection,
                onVisibleHeightChanged = { rideOptionsPanelHeightPx = it },
                modifier = Modifier.fillMaxSize()
            )
        }
        // Ajusta la cámara para encuadrar la ruta completa cuando se muestran opciones de viaje o durante búsqueda
        LaunchedEffect(showingRideOptions, isSearchingDriver, activeRide, routePoints, rideOptionsPanelHeightPx) {
            if ((showingRideOptions || (isSearchingDriver && activeRide == null)) && routePoints.isNotEmpty()) {
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
                // The measured panel includes its lower margin and navigation bar inset.
                val bottomMarginPx = with(density) { 12.dp.toPx() }.toDouble()
                val bottomPadPx = rideOptionsPanelHeightPx.toDouble() + bottomMarginPx
                // Extra padding para lograr un leve "zoom out"
                val extraPadPx = with(density) { 60.dp.toPx() }.toDouble()
                val map = mapView.mapboxMap
                val cs = map.cameraState
                // Camera padding reserves the panel's viewport; coordinatesPadding only adds
                // breathing room around the route and does not move the map's principal point.
                val viewportPadding = if (showingRideOptions)
                    EdgeInsets(topPadPx, leftPadPx, bottomPadPx, rightPadPx)
                else EdgeInsets(0.0, 0.0, 0.0, 0.0)
                val coordinatesPadding = if (showingRideOptions)
                    EdgeInsets(extraPadPx, extraPadPx, extraPadPx, extraPadPx)
                else EdgeInsets(topPadPx + extraPadPx, leftPadPx + extraPadPx,
                    bottomPadPx + extraPadPx, rightPadPx + extraPadPx)
                val cam = map.cameraForCoordinates(
                    routePoints,
                    CameraOptions.Builder()
                        .bearing(cs.bearing)
                        .pitch(cs.pitch)
                        .padding(viewportPadding)
                        .build(),
                    coordinatesPadding,
                    16.0,
                    null
                )
                if (activeRide == null) {
                    map.setCamera(cam)
                }
            }
        }

        // Aviso ligero cuando el modo de selección está activo
        if (isSelectingPoint) {
            // The same full-screen map and center pin choose destination and pickup.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                com.intu.taxi.ui.map.RoutePin(
                    description = if (showPickupPicker) "Punto de recojo" else "Destino seleccionado",
                    pickup = showPickupPicker,
                    modifier = Modifier.size(pinSizeDp).offset(y = -(pinSizeDp / 2))
                        .testTag(if (showPickupPicker) "home-pickup-pin" else "home-destination-pin")
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
                    // Botón con gradient radial igual al header
                    Button(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboard?.hide()
                            val point = mapView.pointUnderCenterPin() ?: selectedPoint
                            if (showPickupPicker && isDelivery) point?.let {
                                pendingDeliveryRoute = null
                                pendingDeliveryOrigin = it
                            }
                            else if (showPickupPicker) point?.let { requestRideFromPickup(it) }
                            else point?.let(::confirmDestination)
                        },
                        enabled = selectedPoint != null && !isCalculatingDestinationRoute && !isCreatingRideRequest,
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
                        Text(when {
                            isCreatingRideRequest -> if (isDelivery) "Solicitando envío…" else "Solicitando viaje…"
                            showPickupPicker -> if (isDelivery) "Continuar con envío" else "Solicitar viaje"
                            isCalculatingDestinationRoute -> "Calculando ruta…"
                            else -> "Confirmar destino"
                        })
                    }
                   
                }
            }
        }

        // Deja el mapa listo para pedir otro viaje
        fun resetRideState() {
            isDelivery = false
            selectedMotoOptionCode = com.intu.taxi.models.MotoOption.ANY.code
            deliveryDraft = null
            pendingDeliveryOrigin = null
            pendingDeliveryRoute = null
            showPickupPicker = false
            pendingDestination = null
            isCalculatingDestinationRoute = false
            isSearchingDriver = false
            currentRideRequestId = null
            activeRide = null
            driverLocation = null
            searchQuery = ""
            selectedDestination = null
            pickupLocation = null
            confirmedDestination = null
            confirmedDestOffset = null
            routePoints = emptyList()
            routeDistanceMeters = null
            routeDurationSeconds = null
            suggestions = emptyList()
        }

        // El pasajero puede cancelar mientras busca, o mientras el conductor viene o ya llegó
        fun cancelCurrentRide() {
            val rideId = currentRideRequestId ?: return
            if (isCancellingRide) return
            isCancellingRide = true
            scope.launch {
                rideRequestRepository.cancelRideRequest(rideId)
                    .onSuccess {
                        com.intu.taxi.rider.RiderTrip.clear()
                        resetRideState()
                        Toast.makeText(context, "Viaje cancelado", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(context, "No se pudo cancelar. Revisa tu conexión e intenta de nuevo.", Toast.LENGTH_LONG).show()
                    }
                isCancellingRide = false
            }
        }

        activeRide?.let { ride ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // Encima de la barra de navegación del sistema, sea de gestos o de 3 botones
                    .padding(start = 16.dp, end = 16.dp, bottom = padding.calculateBottomPadding() + 16.dp)
                    .onGloballyPositioned { rideCardHeightPx = it.size.height },
                colors = CardDefaults.cardColors(containerColor = AppearanceColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = when (ride.status) {
                            "accepted" -> if (ride.isDelivery) "Tu repartidor está en camino" else "Tu mototaxi está en camino"
                            "arrived" -> if (ride.isDelivery) "Tu repartidor llegó al recojo" else "Tu conductor llegó"
                            "in_progress" -> if (ride.isDelivery) "Tu paquete está en camino" else "Viaje en curso"
                            "completed" -> if (ride.isDelivery) "Envío entregado" else "Viaje finalizado"
                            else -> "Buscando conductor"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (ride.status == "accepted" && ride.driverOnOtherTrip) {
                        Text(
                            if (ride.isDelivery) "Tu repartidor está terminando otro servicio y luego irá por tu paquete."
                            else "Tu conductor está terminando un viaje cercano y luego irá por ti.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppearanceColors.highlight(Color(0xFFB45309))
                        )
                    }
                    ridePin?.let { pin ->
                        if (ride.status == "accepted" || ride.status == "arrived") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppearanceColors.tint(Color(0xFFE6F4F3)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("PIN de seguridad", fontWeight = FontWeight.SemiBold, color = AppearanceColors.highlight(Color(0xFF08817E)))
                                    Text(
                                        if (ride.isDelivery) "Díselo al repartidor al entregar el paquete. No lo compartas antes."
                                        else "Díselo al conductor al subir. No lo compartas antes.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppearanceColors.secondary(Color(0xFF5F6570))
                                    )
                                }
                                Text(
                                    pin,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 6.sp,
                                    color = AppearanceColors.foreground(Color(0xFF1E1F47))
                                )
                            }
                        }
                    }
                    ride.delivery?.let { DeliverySummary(it) }
                    if (ride.driverName.isNotBlank() || ride.vehiclePlate.isNotBlank()) {
                        // Foto del conductor para reconocerlo al llegar; tocarla la agranda
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.intu.taxi.ui.components.Avatar(
                                url = ride.driverPhotoUrl,
                                size = 56.dp,
                                zoomable = true,
                                contentDescription = "Foto del conductor"
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                if (ride.driverName.isNotBlank()) {
                                    Text(ride.driverName, fontWeight = FontWeight.SemiBold)
                                }
                                if (ride.vehiclePlate.isNotBlank()) {
                                    Text(
                                        "${if (ride.isDelivery) "Moto lineal" else "Mototaxi"} ${listOf(ride.vehicleDescription, ride.vehiclePlate).filter { it.isNotBlank() }.joinToString(" · ")}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppearanceColors.secondary(Color(0xFF5F6570))
                                    )
                                }
                            }
                        }
                    }
                    Text("Total: ${com.intu.taxi.ui.formatSoles(ride.fare)}")
                    val yapeNumber = com.intu.taxi.ui.peruLocalPhone(ride.driverPhone)
                    if (ride.paymentMethod == "yape_plin" && yapeNumber.isNotBlank()) {
                        // Número del conductor sin +51; al tocarlo se copia para pegarlo en Yape o Plin
                        val clipboard = LocalClipboardManager.current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppearanceColors.tint(Color(0xFFF3EEFB)))
                                .clickable(onClickLabel = "Copiar número de Yape") {
                                    clipboard.setText(AnnotatedString(yapeNumber))
                                    Toast.makeText(context, "Número copiado. Pégalo en Yape o Plin", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Paga por Yape o Plin a", style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(Color(0xFF5F6570)))
                                Text(
                                    com.intu.taxi.ui.formatPeruPhone(ride.driverPhone),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AppearanceColors.foreground(Color(0xFF1E1F47))
                                )
                                Text("Toca para copiar el número", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6F2DBD))
                            }
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = null,
                                tint = Color(0xFF6F2DBD)
                            )
                        }
                    } else {
                        Text(if (ride.paymentMethod == "yape_plin") "Pago por Yape o Plin al conductor" else "Pago en efectivo al conductor")
                    }
                    if (ride.status == "accepted" || ride.status == "arrived") {
                        OutlinedButton(
                            onClick = { showCancelRideDialog = true },
                            enabled = !isCancellingRide && ride.delivery?.paymentCollected != true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppearanceColors.highlight(Color(0xFFB42318)))
                        ) { Text(if (isCancellingRide) "Cancelando…" else if (ride.isDelivery) "Cancelar envío" else "Cancelar viaje") }
                    }
                    if (ride.status == "completed") {
                        Text("El conductor confirmó que recibió el pago.", color = AppearanceColors.highlight(Color(0xFF08817E)))
                        // Calificar al conductor (también se puede después en la pestaña Viajes)
                        var givenStars by remember(ride.rideId) { mutableStateOf(0) }
                        Text(if (ride.isDelivery) "¿Cómo estuvo tu envío?" else "¿Cómo estuvo tu viaje?", fontWeight = FontWeight.SemiBold)
                        com.intu.taxi.ui.components.StarRating(
                            stars = givenStars,
                            size = 36.dp,
                            onRate = if (givenStars == 0) { stars ->
                                scope.launch {
                                    runCatching { com.intu.taxi.repositories.RideHistoryRepository().rate(ride.rideId, stars) }
                                        .onSuccess {
                                            givenStars = stars
                                            Toast.makeText(context, "¡Gracias por calificar!", Toast.LENGTH_SHORT).show()
                                        }
                                        .onFailure {
                                            Toast.makeText(context, it.message ?: "No se pudo guardar la calificación", Toast.LENGTH_LONG).show()
                                        }
                                }
                            } else null
                        )
                        Button(
                            onClick = { resetRideState() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Listo") }
                    }
                }
            }
        }

        if (showCancelRideDialog) {
            AlertDialog(
                onDismissRequest = { showCancelRideDialog = false },
                title = { Text(if (isDelivery) "¿Cancelar el envío?" else "¿Cancelar el viaje?") },
                text = { Text(if (isDelivery) "Tu repartidor ya aceptó el envío." else "Tu conductor ya aceptó el viaje y va en camino.") },
                confirmButton = {
                    TextButton(onClick = {
                        showCancelRideDialog = false
                        cancelCurrentRide()
                    }) { Text("Sí, cancelar", color = AppearanceColors.highlight(Color(0xFFB42318))) }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelRideDialog = false }) { Text("No") }
                }
            )
        }

        // Indicador creativo de búsqueda de conductor con animaciones de radar
        CreativeDriverSearchIndicator(
            isVisible = isSearchingDriver,
            delivery = isDelivery,
            isCancelling = isCancellingRide,
            onCancel = { cancelCurrentRide() }
        )
    }
}

/**
 * Dirección legible de un punto, para el pasajero y para el chofer. Nunca coordenadas: devuelve
 * null y quien llama pone un texto genérico.
 *
 * El geocodificador de Android trae el número de casa en la ciudad ("Jirón Augusto Hilser N° 465"),
 * pero en carreteras devuelve códigos de ruta ("5S 3859"). En esos casos se usa Mapbox, que da
 * el nombre de la vía y el pueblo ("Carretera Longitudinal de la Selva Sur, Río Negro").
 */
private suspend fun readableAddress(
    context: android.content.Context,
    http: OkHttpClient,
    token: String,
    point: Point
): String? = withContext(Dispatchers.IO) {
    val fromAndroid = runCatching {
        Geocoder(context, Locale("es", "PE"))
            .getFromLocation(point.latitude(), point.longitude(), 1)
            ?.firstOrNull()?.getAddressLine(0)
    }.getOrNull()?.let(::cleanAddress)
    if (fromAndroid != null && isUsefulAddress(fromAndroid)) return@withContext fromAndroid

    val fromMapbox = runCatching {
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${point.longitude()},${point.latitude()}.json" +
            "?access_token=$token&language=es&limit=1&types=address,neighborhood,locality,place"
        http.newCall(Request.Builder().url(url).get().build()).execute().use { res ->
            if (!res.isSuccessful) null
            else JSONObject(res.body?.string().orEmpty())
                .optJSONArray("features")?.optJSONObject(0)?.optString("place_name")
        }
    }.getOrNull()?.let(::cleanAddress)
    fromMapbox?.takeIf { it.isNotBlank() } ?: fromAndroid?.takeIf { it.isNotBlank() }
}

/** Distancia aproximada en metros entre dos puntos (fórmula de haversine). */
/** Lo que decide la ruta y la cámara del viaje en el mapa del pasajero. */
private data class RiderTripMapInput(
    val rideId: String?,
    val status: String?,
    val target: com.google.firebase.firestore.GeoPoint?,
    val driver: com.google.firebase.firestore.GeoPoint?,
    val styleLoaded: Boolean,
    val cardHeightPx: Int
)

private fun metersBetween(a: Point, b: Point): Double {
    val dLat = Math.toRadians(b.latitude() - a.latitude())
    val dLon = Math.toRadians(b.longitude() - a.longitude())
    val h = kotlin.math.sin(dLat / 2).pow(2.0) +
        kotlin.math.cos(Math.toRadians(a.latitude())) * kotlin.math.cos(Math.toRadians(b.latitude())) *
        kotlin.math.sin(dLon / 2).pow(2.0)
    return 6_371_000.0 * 2 * kotlin.math.atan2(kotlin.math.sqrt(h), kotlin.math.sqrt(1 - h))
}

/** Falso si la primera parte es un código de ruta ("5S 3859"), un número suelto o una calle sin nombre. */
private fun isUsefulAddress(address: String): Boolean {
    val first = address.substringBefore(',').trim()
    return first.isNotBlank() &&
        !Regex("^\\d+[A-Za-z]?(\\s+\\d+)?$").matches(first) &&
        !first.equals("Unnamed Road", ignoreCase = true) &&
        !first.equals("Calle sin nombre", ignoreCase = true)
}

/** Quita país, departamento y códigos postales ("Satipo 12261, Peru" → "Satipo"); máximo 3 partes. */
private fun cleanAddress(raw: String): String = raw.split(",")
    .map { it.replace(Regex("\\b\\d{5}\\b"), "").trim() }
    .filter {
        it.isNotBlank() &&
            !it.equals("Perú", ignoreCase = true) && !it.equals("Peru", ignoreCase = true) &&
            !it.startsWith("Departamento de", ignoreCase = true) && !it.startsWith("Provincia de", ignoreCase = true)
    }
    .take(3)
    .joinToString(", ")

@Composable
private fun HeaderSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    placeholderText: String = "¿A dónde quieres ir?",
    showClearButton: Boolean = false,
    onClearClick: () -> Unit = {},
    shape: RoundedCornerShape = RoundedCornerShape(50),
    elevation: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, shape)
            .clip(shape)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholderText, color = AppearanceColors.secondary(Color(0xFF7A7F87))) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AppearanceColors.secondary(Color(0xFF8E8E93))) },
            trailingIcon = {
                if (showClearButton && value.isNotEmpty()) {
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "Limpiar",
                            tint = AppearanceColors.secondary(Color(0xFF8E8E93))
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = AppearanceColors.surface.copy(alpha = 0.85f),
                unfocusedContainerColor = AppearanceColors.surface.copy(alpha = 0.85f)
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
    val priceStr = com.intu.taxi.ui.formatSoles(price)
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
            containerColor = AppearanceColors.surface.copy(alpha = if (selected) 0.95f else 0.8f)
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
                            color = AppearanceColors.foreground(Color(0xFF1C1C1E)),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = AppearanceColors.secondary(Color(0xFF6E6E73)),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = etaStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppearanceColors.secondary(Color(0xFF6E6E73))
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
