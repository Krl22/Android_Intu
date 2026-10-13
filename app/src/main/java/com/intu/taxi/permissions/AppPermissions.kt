package com.intu.taxi.permissions

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class AppPermissions(
    val locationGranted: Boolean,
    val locationEnabled: Boolean,
    val requestInProgress: Boolean,
    val requestLocation: ((Boolean) -> Unit) -> Unit,
    val enableLocation: () -> Unit,
)

val LocalAppPermissions = staticCompositionLocalOf<AppPermissions?> { null }

private data class PermissionSnapshot(
    val locationGranted: Boolean,
    val notificationsGranted: Boolean,
    val locationEnabled: Boolean,
)

private fun readPermissions(context: Context): PermissionSnapshot {
    fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    val manager = context.getSystemService(LocationManager::class.java)
    return PermissionSnapshot(
        granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION),
        Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS),
        manager != null && LocationManagerCompat.isLocationEnabled(manager),
    )
}

private val QueueSaver = listSaver<PermissionRequestQueue, Any>(
    save = { listOf(it.locationAsked, it.notificationsAsked, it.inFlight?.ordinal ?: -1,
        it.queued?.ordinal ?: -1, it.waitForResume, it.manualRequest) },
    restore = { PermissionRequestQueue(it[0] as Boolean, it[1] as Boolean,
        AppPermission.entries.getOrNull(it[2] as Int), AppPermission.entries.getOrNull(it[3] as Int),
        it[4] as Boolean, it[5] as Boolean) },
)

/** One activity-level launcher, independent of map loading and push token registration. */
@Composable
fun rememberAppPermissions(autoRequest: Boolean, needsLocation: Boolean = true): AppPermissions {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val preferences = remember(context) { context.getSharedPreferences("intu_permissions", Context.MODE_PRIVATE) }
    var queue by rememberSaveable(stateSaver = QueueSaver) {
        mutableStateOf(PermissionRequestQueue(preferences.getBoolean("location_asked", false),
            preferences.getBoolean("notifications_asked", false)))
    }
    var snapshot by remember { mutableStateOf(readPermissions(context)) }
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var returningFromSettings by rememberSaveable { mutableStateOf(false) }
    val locationCallbacks = remember { mutableListOf<(Boolean) -> Unit>() }

    fun complete(cancelled: Boolean) {
        val permission = queue.inFlight
        snapshot = readPermissions(context)
        queue = queue.complete(cancelled)
        if (!cancelled) {
            preferences.edit().putBoolean("location_asked", queue.locationAsked)
                .putBoolean("notifications_asked", queue.notificationsAsked).apply()
            if (permission == AppPermission.LOCATION) {
                val callbacks = locationCallbacks.toList()
                locationCallbacks.clear()
                callbacks.forEach { it(snapshot.locationGranted) }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        complete(cancelled = result.isEmpty())
    }
    val currentComplete by rememberUpdatedState<(Boolean) -> Unit> { complete(it) }
    DisposableEffect(context, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                snapshot = readPermissions(context)
                queue = queue.resumed()
                resumed = true
                if (returningFromSettings) {
                    returningFromSettings = false
                    currentComplete(false)
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) resumed = false
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receivedContext: Context?, intent: Intent?) { snapshot = readPermissions(context) }
        }
        lifecycle.addObserver(observer)
        ContextCompat.registerReceiver(context, receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION).apply {
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { lifecycle.removeObserver(observer); context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(queue, snapshot, resumed, autoRequest, needsLocation) {
        val permission = queue.nextRequest(snapshot.locationGranted, snapshot.notificationsGranted,
            autoRequest, needsLocation, resumed) ?: return@LaunchedEffect
        queue = queue.launch(permission)
        if ((permission == AppPermission.LOCATION && snapshot.locationGranted) ||
            (permission == AppPermission.NOTIFICATIONS && snapshot.notificationsGranted)) {
            complete(cancelled = false)
        } else if (permission == AppPermission.LOCATION && queue.locationAsked && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Settings are opened only for an explicit retry after a previous answer.
            returningFromSettings = true
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")))
        } else {
            launcher.launch(if (permission == AppPermission.LOCATION)
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            else arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    return AppPermissions(snapshot.locationGranted, snapshot.locationEnabled,
        queue.inFlight != null || queue.queued != null,
        requestLocation = { onResult ->
            snapshot = readPermissions(context)
            if (snapshot.locationGranted) onResult(true)
            else {
                locationCallbacks.add(onResult)
                queue = queue.request(AppPermission.LOCATION)
            }
        },
        enableLocation = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
    )
}

/** Screens mounted directly by UI tests observe permissions without opening system dialogs. */
@Composable
fun rememberLocationPermissionGranted(): Boolean {
    LocalAppPermissions.current?.let { return it.locationGranted }
    return rememberAppPermissions(autoRequest = false, needsLocation = false).locationGranted
}
