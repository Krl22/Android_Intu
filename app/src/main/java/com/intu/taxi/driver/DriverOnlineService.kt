package com.intu.taxi.driver

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.intu.taxi.R
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import com.intu.taxi.models.ActiveRide
import com.intu.taxi.repositories.ActiveRideRepository
import com.intu.taxi.repositories.DriverAvailabilityRepository
import com.intu.taxi.ui.formatSoles
import com.intu.taxi.location.AdminLocationSimulation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/** Estado que comparten la pantalla del conductor y el servicio en segundo plano. */
object DriverSession {
    /** La app está visible: las solicitudes se ven en pantalla y no hace falta notificarlas. */
    @Volatile var appInForeground = false

    /** Viaje actual según la pantalla; el servicio también lo consulta al servidor cada 10 s. */
    private val mutableActiveRide = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val activeRide: kotlinx.coroutines.flow.StateFlow<String?> = mutableActiveRide
    var activeRideId: String?
        get() = mutableActiveRide.value
        set(value) { mutableActiveRide.value = value }

    /** Solicitudes que el conductor rechazó: no se notifican. */
    val declinedRequestIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** Cuándo vio el conductor cada solicitud: su tiempo para decidir corre desde ahí. */
    val requestFirstSeenMs: MutableMap<String, Long> = ConcurrentHashMap()

    /**
     * Última ubicación del GPS según el servicio. La pantalla la usa para la ruta y la cámara del viaje
     * aunque el mapa esté pausado (app minimizada), así al volver ya está al día.
     */
    val location = kotlinx.coroutines.flow.MutableStateFlow<GeoPoint?>(null)
}

/**
 * Mantiene al conductor en línea con la app minimizada o la pantalla apagada, como Uber:
 * envía su ubicación a Supabase y avisa con una notificación cuando llega una solicitud.
 * Corre mientras el conductor esté en línea o tenga un viaje abierto.
 */
class DriverOnlineService : Service() {

    companion object {
        private const val ONLINE_CHANNEL = "driver_online"
        private const val REQUESTS_CHANNEL = "ride_requests"
        private const val ONLINE_NOTIFICATION_ID = 1001

        @Volatile
        var isRunning = false
            private set

        /** Se inicia desde la pantalla del conductor (con la app visible, como exige Android). */
        fun start(context: Context) {
            if (!hasLocationPermission(context)) return
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, DriverOnlineService::class.java))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DriverOnlineService::class.java))
        }

        fun hasLocationPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val rideRepository = ActiveRideRepository()
    private val availabilityRepository = DriverAvailabilityRepository()
    private val prefs by lazy { getSharedPreferences("intu_driver", MODE_PRIVATE) }
    private var locationManager: LocationManager? = null

    @Volatile private var openRides: List<ActiveRide> = emptyList()
    private var lastRealLocation: GeoPoint? = null
    private val locationPublisher = DriverLocationPublisher(
        location = {
            if (FirebaseAuth.getInstance().currentUser == null) null
            else AdminLocationSimulation.effectiveLocation(lastRealLocation)
        },
        activeRide = { DriverSession.activeRideId ?: openRides.firstOrNull()?.rideId },
        online = { prefs.getBoolean("online", false) },
        send = { point, rideId ->
            if (rideId != null) rideRepository.updateDriverLocation(rideId, point)
            else availabilityRepository.updateDriverLocation(point)
        },
        clock = android.os.SystemClock::elapsedRealtime
    )
    private val notifiedRequestIds = mutableSetOf<String>()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onLocation(location)
        // Implementados para Android 9 o anterior, donde no tienen implementación por defecto
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Solo para versiones antiguas de Android")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AdminLocationSimulation.initialize()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        // Si Android no permite iniciarlo (sin permiso de ubicación o reiniciado desde segundo plano), se detiene
        val started = runCatching {
            ServiceCompat.startForeground(this, ONLINE_NOTIFICATION_ID, onlineNotification(), type)
        }.isSuccess
        if (!started) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!isRunning) {
            isRunning = true
            startLocationUpdates()
            scope.launch {
                AdminLocationSimulation.preset.collect {
                    publishLocation(force = true)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(this@DriverOnlineService, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        runCatching { NotificationManagerCompat.from(this@DriverOnlineService)
                            .notify(ONLINE_NOTIFICATION_ID, onlineNotification()) }
                    }
                }
            }
            scope.launch {
                // A fixed test location has no GPS movement callbacks; keep the normal heartbeat.
                while (isActive) {
                    if (AdminLocationSimulation.currentPreset() != null) publishLocation()
                    delay(2_000)
                }
            }
            scope.launch { loop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        runCatching { locationManager?.removeUpdates(locationListener) }
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun loop() {
        var lastRidesRefresh = 0L
        while (currentCoroutineContext().isActive) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                stopSelf()
                return
            }
            val now = System.currentTimeMillis()
            if (now - lastRidesRefresh >= 10_000) {
                runCatching { rideRepository.findOpenRidesForDriver(uid) }.onSuccess { openRides = it }
                lastRidesRefresh = now
            }
            val online = prefs.getBoolean("online", false)
            if (!online && openRides.isEmpty() && DriverSession.activeRideId == null) {
                stopSelf()
                return
            }
            // Igual que en la pantalla: libre, o llevando a un pasajero sin siguiente viaje aceptado
            val canReceive = online &&
                (openRides.isEmpty() || (openRides.size == 1 && openRides[0].status == "in_progress"))
            if (canReceive && !DriverSession.appInForeground) notifyNewRequests()
            delay(4_000)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission(this)) return
        val manager = getSystemService(LOCATION_SERVICE) as? LocationManager ?: return
        locationManager = manager
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            if (runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)) {
                runCatching {
                    manager.requestLocationUpdates(provider, 2_000L, 5f, locationListener, Looper.getMainLooper())
                }
            }
        }
    }

    /** Cada 2 s en viaje (el pasajero lo sigue en el mapa) y cada 10 s en línea sin viaje. */
    private fun onLocation(location: Location) {
        lastRealLocation = GeoPoint(location.latitude, location.longitude)
        publishLocation()
    }

    private fun publishLocation(force: Boolean = false) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        DriverSession.location.value = AdminLocationSimulation.effectiveLocation(lastRealLocation)
        scope.launch {
            if (FirebaseAuth.getInstance().currentUser?.uid == uid) runCatching { locationPublisher.publish(force) }
        }
    }

    private suspend fun notifyNewRequests() {
        val rows = runCatching {
            SupabaseApi.rows(
                "rides?status=eq.searching&select=id,estimated_fare,origin_address,destination_address,rider_name,payment_method,service_kind" +
                    "&order=requested_at.desc&limit=10"
            )
        }.getOrNull() ?: return
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val id = row.str("id")
            if (id.isBlank() || id in notifiedRequestIds || id in DriverSession.declinedRequestIds) continue
            notifiedRequestIds += id
            showRequestNotification(id, row)
        }
    }

    private fun showRequestNotification(rideId: String, row: JSONObject) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val fare = formatSoles(row.optDouble("estimated_fare", 0.0))
        val origin = row.str("origin_address", "Punto de recojo")
        val destination = row.str("destination_address", "Destino")
        val payment = if (row.str("payment_method") == "yape_plin") "Yape" else "Efectivo"
        val delivery = row.str("service_kind") == "delivery"
        val rider = row.str("rider_name", if (delivery) "Quien envía" else "Pasajero")
        val notification = NotificationCompat.Builder(this, REQUESTS_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_intu)
            .setContentTitle("${if (delivery) "Nuevo envío" else "Nueva solicitud"} · $fare")
            .setContentText("$origin → $destination")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$rider\nRecojo: $origin\nDestino: $destination\n$fare · $payment")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setTimeoutAfter(90_000)
            .setContentIntent(openAppIntent())
            .build()
        runCatching { NotificationManagerCompat.from(this).notify(rideId.hashCode(), notification) }
    }

    private fun onlineNotification() = NotificationCompat.Builder(this, ONLINE_CHANNEL)
        .setSmallIcon(R.drawable.ic_stat_intu)
        .setContentTitle("Estás en línea")
        .setContentText(AdminLocationSimulation.currentPreset()?.let { "Ubicación de prueba: ${it.label}" }
            ?: "Intu te avisará cuando haya solicitudes de viaje")
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(openAppIntent())
        .build()

    /** Abre la app donde estaba (sin crear otra pantalla encima). */
    private fun openAppIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ?: Intent()
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(ONLINE_CHANNEL, "Conductor en línea", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Aviso fijo mientras estás en línea como conductor"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(REQUESTS_CHANNEL, "Solicitudes de viaje", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Nuevas solicitudes cuando la app está en segundo plano"
                enableVibration(true)
            }
        )
    }
}
