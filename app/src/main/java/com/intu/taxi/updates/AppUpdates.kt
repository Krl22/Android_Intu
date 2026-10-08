package com.intu.taxi.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val UPDATE_SITE = "https://viajaconintu.pages.dev"

data class PublishedAppRelease(
    val versionCode: Int,
    val versionName: String,
    val size: Long,
    val sha256: String,
) {
    // The server cannot supply an arbitrary download host or path.
    val downloadUrl: String get() = "$UPDATE_SITE/descargar?versionCode=$versionCode"
}

/** Reject incomplete metadata, another app and incompatible Android releases. */
internal fun parsePublishedRelease(json: String, packageName: String, sdk: Int): PublishedAppRelease {
    val value = JSONObject(json)
    require(value.optBoolean("available") && value.optBoolean("updateAvailable"))
    require(value.getString("packageName") == packageName)
    val code = value.getLong("versionCode")
    require(value.get("versionCode") is Number && value.getDouble("versionCode") == code.toDouble())
    val name = value.getString("versionName")
    val minimumSdk = value.getInt("minSdk")
    val size = value.getLong("size")
    val hash = value.getString("sha256")
    require(code in 1..Int.MAX_VALUE.toLong())
    require(name.isNotBlank() && name.length <= 40)
    require(minimumSdk in 1..sdk && size > 0)
    require(hash.matches(Regex("[a-fA-F0-9]{64}")))
    return PublishedAppRelease(code.toInt(), name, size, hash.lowercase())
}

class AppUpdateRepository(private val packageName: String, private val sdk: Int) {
    private val http = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build()

    suspend fun latest(): PublishedAppRelease = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$UPDATE_SITE/api/apk")
            .cacheControl(CacheControl.FORCE_NETWORK).build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Update metadata unavailable" }
            val body = checkNotNull(response.body)
            require(body.contentLength() <= 32_768)
            val source = body.source()
            source.request(32_769)
            require(source.buffer.size <= 32_768)
            parsePublishedRelease(source.buffer.readUtf8(), packageName, sdk)
        }
    }
}

data class AppUpdateState(
    val checking: Boolean = false,
    val release: PublishedAppRelease? = null,
    val checked: Boolean = false,
    val error: String? = null,
) {
    fun newerThan(installedCode: Int): PublishedAppRelease? = release?.takeIf { it.versionCode > installedCode }
}

/** Foreground checks only; manual checks bypass the fifteen-minute successful-check interval. */
class AppUpdateController(
    private val fetch: suspend () -> PublishedAppRelease,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val mutableState = MutableStateFlow(AppUpdateState())
    val state: StateFlow<AppUpdateState> = mutableState
    private val mutex = Mutex()
    private var lastCheck: Long? = null
    private var retryAfter = 0L

    suspend fun check(force: Boolean = false) {
        if (!mutex.tryLock()) return
        try {
            val now = clock()
            if (!force && lastCheck?.let { now - it < retryAfter } == true) return
            val previous = mutableState.value
            mutableState.value = previous.copy(checking = true, error = null)
            try {
                val release = fetch()
                mutableState.value = AppUpdateState(release = release, checked = true)
                retryAfter = TimeUnit.MINUTES.toMillis(15)
            } catch (cancelled: CancellationException) {
                mutableState.value = previous
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = previous.copy(checking = false,
                    error = "No pudimos comprobar las actualizaciones. Revisa tu conexión e intenta de nuevo.")
                retryAfter = TimeUnit.MINUTES.toMillis(5)
            }
            lastCheck = clock()
        } finally { mutex.unlock() }
    }
}

fun openPublishedUpdate(context: Context, release: PublishedAppRelease): Boolean = runCatching {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl)))
}.isSuccess
