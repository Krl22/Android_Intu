package com.intu.taxi.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.intu.taxi.R
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.driver.DriverSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import com.intu.taxi.repositories.AdminNotificationRepository
import org.json.JSONObject

/**
 * Avisos push de los viajes (conductor aceptó, llegó, viaje iniciado, terminado o cancelado).
 * Supabase los envía por FCM a los tokens que cada teléfono registra aquí.
 */
object PushNotifications {
    /** Mismo id que usa la Cloud Function ridePush y el manifest. */
    const val RIDE_CHANNEL = "ride_updates"
    const val ADMIN_CHANNEL = "admin_activity"
    const val ADMIN_TYPE_EXTRA = "intu_admin_type"
    const val ADMIN_UID_EXTRA = "intu_admin_uid"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(RIDE_CHANNEL, "Estado del viaje", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Cuando tu conductor acepta, llega, inicia o termina el viaje, o si se cancela"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(NotificationChannel(ADMIN_CHANNEL, "Actividad de Intu", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Usuarios, solicitudes, postulaciones y reportes para administradores"
            enableVibration(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        })
    }

    /** Registra este teléfono para recibir avisos de la cuenta con sesión. Requiere que el perfil exista. */
    suspend fun registerToken() {
        if (FirebaseAuth.getInstance().currentUser == null) return
        val token = FirebaseMessaging.getInstance().token.await()
        registerToken(token)
    }

    suspend fun registerToken(token: String) {
        SupabaseApi.request(
            "POST",
            "rpc/register_device_token",
            JSONObject().put("p_token", token).put("p_platform", "android")
        )
    }

    /** Al cerrar sesión: este teléfono deja de recibir los avisos de la cuenta. */
    suspend fun unregisterToken() {
        if (FirebaseAuth.getInstance().currentUser == null) return
        val token = FirebaseMessaging.getInstance().token.await()
        SupabaseApi.request("DELETE", "device_tokens?token=eq.${SupabaseApi.encode(token)}")
    }
}

class IntuMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        if (FirebaseAuth.getInstance().currentUser == null) return
        scope.launch { runCatching { PushNotifications.registerToken(token) } }
    }

    /**
     * Con la app minimizada Android muestra el aviso solo y no llama aquí. Llega aquí si la app
     * está visible (la pantalla ya muestra el estado, no se avisa) o si solo corre el servicio
     * del conductor en línea; en ese caso el aviso se muestra a mano.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["kind"] == "admin_activity") {
            val activity = AdminActivityMessage.parse(message.data) ?: return
            // Data-only FCM calls this in foreground and background. Finish inside the callback
            // lifetime; no detached coroutine that Android could kill before showing the alert.
            val allowed = runCatching { runBlocking {
                withTimeoutOrNull(8_000) {
                    canDisplayAdminActivity(activity, { FirebaseAuth.getInstance().currentUser?.uid },
                        { AdminNotificationRepository().get(timeoutMillis = 6_000) })
                } == true
            } }.getOrDefault(false)
            if (allowed) showAdminActivity(activity)
            return
        }
        if (DriverSession.appInForeground) return
        val notification = message.notification ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        PushNotifications.createChannel(this)
        val openApp = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ?: Intent()
        val built = NotificationCompat.Builder(this, PushNotifications.RIDE_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_intu)
            .setColor(ContextCompat.getColor(this, R.color.intu_teal))
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, openApp, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
        // Mismo tag que usa Android para el viaje: el nuevo estado reemplaza al anterior
        val tag = message.data["rideId"]?.takeIf { it.isNotBlank() } ?: "ride"
        runCatching { NotificationManagerCompat.from(this).notify(tag, 0, built) }
    }

    private fun showAdminActivity(activity: AdminActivityMessage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        PushNotifications.createChannel(this)
        val openApp = Intent(this, com.intu.taxi.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .setData(android.net.Uri.Builder().scheme("intu").authority("admin-activity").appendPath(activity.eventId).build())
            .putExtra(PushNotifications.ADMIN_TYPE_EXTRA, activity.type.key)
            .putExtra(PushNotifications.ADMIN_UID_EXTRA, activity.recipientUid)
        val built = NotificationCompat.Builder(this, PushNotifications.ADMIN_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_intu)
            .setColor(ContextCompat.getColor(this, R.color.intu_teal))
            .setContentTitle(activity.title).setContentText(activity.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(activity.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, openApp,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
        // Unique event tags keep separate requests visible; a repeated delivery replaces itself.
        runCatching { NotificationManagerCompat.from(this).notify("admin:${activity.eventId}", 0, built) }
    }
}
