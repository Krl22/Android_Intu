package com.intu.taxi.updates

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo

/**
 * Lo que Google Play ofrece, en el mismo formato que la descarga web para reutilizar el aviso y Cuenta.
 * Sin actualización, devuelve la versión instalada: el aviso entiende que ya está al día.
 */
internal fun playRelease(available: Boolean, availableVersionCode: Int, downloadBytes: Long, installedCode: Int) =
    PublishedAppRelease(
        versionCode = if (available && availableVersionCode > installedCode) availableVersionCode else installedCode,
        versionName = "",
        size = downloadBytes,
        sha256 = "",
        fromPlay = true,
    )

/**
 * Actualizaciones de la versión publicada en Google Play. Play descarga e instala la nueva versión en
 * una pantalla propia y reinicia Intu; sin conexión con Play (p. ej. instalada fuera de la tienda),
 * la comprobación falla y el aviso muestra el error habitual.
 */
class PlayAppUpdates(context: Context, private val installedCode: Int) {
    private val manager = AppUpdateManagerFactory.create(context.applicationContext)
    @Volatile private var lastInfo: AppUpdateInfo? = null

    suspend fun latest(): PublishedAppRelease {
        val info = manager.requestAppUpdateInfo()
        lastInfo = info
        val available = when (info.updateAvailability()) {
            UpdateAvailability.UPDATE_AVAILABLE -> info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            // Una actualización que el conductor empezó y quedó a medias se retoma al tocar Actualizar
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> true
            else -> false
        }
        return playRelease(available, info.availableVersionCode(), info.totalBytesToDownload(), installedCode)
    }

    /** Abre la pantalla de Play que descarga, instala y reinicia Intu. */
    fun start(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        // Cada AppUpdateInfo sirve para un solo intento; el siguiente usa una consulta nueva
        val info = lastInfo ?: return false
        lastInfo = null
        return runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE))
        }.getOrDefault(false)
    }
}
