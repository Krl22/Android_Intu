package com.intu.taxi.rider

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.intu.taxi.R
import com.intu.taxi.models.ActiveRide
import com.intu.taxi.repositories.ActiveRideRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Texto del aviso fijo del viaje del pasajero. */
data class TripNotice(val rideId: String, val title: String, val text: String)

/**
 * Viaje abierto del pasajero, compartido entre la pantalla de inicio y [RideTrackingService].
 * null cuando no hay viaje (o terminó).
 */
object RiderTrip {
    private val _notice = MutableStateFlow<TripNotice?>(null)
    val notice: StateFlow<TripNotice?> = _notice

    /** Viaje recién pedido (o retomado): si ya se sigue ese viaje, conserva su estado actual. */
    fun searching(rideId: String, delivery: Boolean = false) {
        if (_notice.value?.rideId == rideId) return
        _notice.value = TripNotice(rideId, if (delivery) "Buscando repartidor" else "Buscando conductor",
            if (delivery) "Te avisamos apenas un repartidor acepte tu envío." else "Te avisamos apenas un conductor acepte tu viaje.")
    }

    /** Actualiza el aviso con el estado del viaje; al terminar o cancelarse lo quita. */
    fun update(ride: ActiveRide) {
        val driver = ride.driverName.ifBlank { if (ride.isDelivery) "Tu repartidor" else "Tu conductor" }
        val plate = ride.vehiclePlate.takeIf { it.isNotBlank() }?.let { " · Placa $it" }.orEmpty()
        _notice.value = when (ride.status) {
            "searching" -> TripNotice(ride.rideId, if (ride.isDelivery) "Buscando repartidor" else "Buscando conductor",
                if (ride.isDelivery) "Te avisamos apenas un repartidor acepte tu envío." else "Te avisamos apenas un conductor acepte tu viaje.")
            "accepted" -> TripNotice(ride.rideId, if (ride.isDelivery) "Tu repartidor va en camino" else "Tu conductor va en camino", "$driver$plate")
            "arrived" -> TripNotice(ride.rideId, if (ride.isDelivery) "Tu repartidor llegó al recojo" else "Tu conductor llegó", "$driver te espera en el punto de recojo$plate")
            "in_progress" -> TripNotice(
                ride.rideId, if (ride.isDelivery) "Tu paquete está en camino" else "Viaje en curso",
                ride.destinationAddress.takeIf { it.isNotBlank() }?.let { if (ride.isDelivery) "Entrega en $it" else "Vas a $it" }
                    ?: if (ride.isDelivery) "Tu repartidor va hacia el punto de entrega" else "Vas camino a tu destino"
            )
            else -> null
        }
    }

    fun clear() {
        _notice.value = null
    }
}

/**
 * Mantiene activa la app del pasajero mientras tiene un viaje abierto, como Uber: así el viaje, la
 * ubicación del conductor, la ruta y la cámara del mapa siguen al día con la app minimizada, y al
 * volver no hay que esperar a que se sincronice. Muestra un aviso fijo con el estado del viaje.
 *
 * Se inicia desde la pantalla de inicio (con la app visible, como exige Android) y se detiene sola
 * cuando el viaje termina o se cancela.
 */
class RideTrackingService : Service() {

    companion object {
        private const val CHANNEL = "ride_tracking"
        private const val NOTIFICATION_ID = 1002

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, RideTrackingService::class.java))
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val rides = ActiveRideRepository()
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL, "Viaje en curso", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Aviso fijo mientras tienes un viaje abierto"
                    setShowBadge(false)
                }
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        // Android exige llamar a startForeground aunque el viaje ya haya terminado; luego se detiene
        val started = runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(RiderTrip.notice.value), type)
        }.isSuccess
        if (!started || RiderTrip.notice.value == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!running) {
            running = true
            scope.launch {
                RiderTrip.notice.collect { notice ->
                    if (notice == null) {
                        ServiceCompat.stopForeground(this@RideTrackingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        runCatching { NotificationManagerCompat.from(this@RideTrackingService).notify(NOTIFICATION_ID, notification(notice)) }
                    }
                }
            }
            // Respaldo si la pantalla de inicio no está abierta (p. ej. en otra pestaña): revisa el viaje
            // cada 15 s para que el aviso no quede desactualizado y el servicio se detenga al terminar
            scope.launch {
                while (isActive) {
                    delay(15_000)
                    val rideId = RiderTrip.notice.value?.rideId ?: break
                    runCatching { rides.getRide(rideId) }.getOrNull()?.let { ride ->
                        if (RiderTrip.notice.value?.rideId == ride.rideId) RiderTrip.update(ride)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(notice: TripNotice?): android.app.Notification {
        val openApp = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ?: Intent()
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_intu)
            .setColor(ContextCompat.getColor(this, R.color.intu_teal))
            .setContentTitle(notice?.title ?: "Intu")
            .setContentText(notice?.text ?: "Siguiendo tu viaje")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(PendingIntent.getActivity(this, 0, openApp, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
    }
}
