package com.intu.taxi.updates

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
import com.intu.taxi.MainActivity
import com.intu.taxi.R

internal fun shouldNotifyAppUpdate(release: PublishedAppRelease, installedCode: Int, notifiedCode: Int): Boolean =
    release.versionCode > installedCode && release.versionCode > notifiedCode

internal class AppUpdateNotifications(private val context: Context) {
    private val preferences = context.getSharedPreferences("intu_app_updates", Context.MODE_PRIVATE)
    private val manager = NotificationManagerCompat.from(context)

    fun enabled(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!manager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
            ?.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun clearInstalled(installedCode: Int) {
        val notifiedCode = preferences.getInt(NOTIFIED_CODE, 0)
        if (notifiedCode > 0 && notifiedCode <= installedCode) manager.cancel(TAG, 0)
    }

    /** Remember a version only after posting succeeds, so denied permission can be retried later. */
    fun show(release: PublishedAppRelease, installedCode: Int): Boolean {
        if (!shouldNotifyAppUpdate(release, installedCode, preferences.getInt(NOTIFIED_CODE, 0)) || !enabled()) return false
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL, "Actualizaciones de Intu", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Avisos cuando hay una nueva versión de Intu disponible"
                })
        }
        val openApp = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .setAction("com.intu.taxi.OPEN_APP_UPDATE")
            .putExtra(OPEN_UPDATE_EXTRA, true)
        val body = "Intu ${release.versionName} está disponible. Toca para actualizar."
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_intu)
            .setColor(ContextCompat.getColor(context, R.color.intu_teal))
            .setContentTitle("Nueva versión de Intu")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
        return try {
            manager.notify(TAG, 0, notification)
            preferences.edit().putInt(NOTIFIED_CODE, release.versionCode).apply()
            true
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }

    companion object {
        const val OPEN_UPDATE_EXTRA = "intu_open_app_update"
        internal const val CHANNEL = "app_updates"
        internal const val TAG = "app-update"
        internal const val NOTIFIED_CODE = "notified_version_code"
    }
}
