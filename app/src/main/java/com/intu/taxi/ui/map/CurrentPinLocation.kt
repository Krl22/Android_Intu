package com.intu.taxi.ui.map

import android.animation.ValueAnimator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.intu.taxi.permissions.LocalAppPermissions
import com.intu.taxi.permissions.rememberAppPermissions
import com.intu.taxi.location.AdminLocationSimulation
import com.mapbox.common.location.LocationError
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.DefaultLocationProvider
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

data class PinStartLocation(val point: Point? = null, val message: String? = null)

/** Resolves once before showing the map, so a late GPS update never moves a user's chosen pin. */
@Composable
fun rememberPinStartLocation(
    fallback: Point,
    useCurrentLocation: Boolean,
    locationProvider: LocationProvider? = null
): PinStartLocation {
    val context = LocalContext.current
    val preset = remember { AdminLocationSimulation.currentPreset() }
    val permissions = LocalAppPermissions.current ?: rememberAppPermissions(autoRequest = false, needsLocation = false)
    val permitted = permissions.locationGranted
    var permissionRequested by rememberSaveable(useCurrentLocation) { mutableStateOf(false) }
    var permissionResolved by rememberSaveable(useCurrentLocation) {
        mutableStateOf(!useCurrentLocation || permitted || preset != null || locationProvider != null)
    }
    var start by remember(fallback, useCurrentLocation) {
        mutableStateOf(if (useCurrentLocation) PinStartLocation() else PinStartLocation(fallback))
    }
    LaunchedEffect(useCurrentLocation, permissions.requestInProgress, permitted) {
        if (useCurrentLocation && !permissionResolved) {
            if (permissionRequested) {
                // After recreation the activity still owns the result, but the old callback is gone.
                if (!permissions.requestInProgress) permissionResolved = true
            } else {
                permissionRequested = true
                permissions.requestLocation { permissionResolved = true }
            }
        }
    }
    LaunchedEffect(fallback, useCurrentLocation, permissionResolved) {
        if (!useCurrentLocation || !permissionResolved) return@LaunchedEffect
        val resolved = try {
            when {
                preset != null -> Point.fromLngLat(preset.longitude, preset.latitude)
                permitted || locationProvider != null -> firstPinLocation(locationProvider ?: DefaultLocationProvider(context))
                else -> null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { null }
        start = PinStartLocation(resolved ?: fallback,
            if (resolved == null) "No pudimos obtener tu ubicación. Elige el punto en el mapa." else null)
    }
    return start
}

/** Stop location updates on success, error, timeout, or dismissal. */
suspend fun firstPinLocation(provider: LocationProvider, timeoutMillis: Long = 10_000): Point? =
    withTimeoutOrNull(timeoutMillis) {
        val result = CompletableDeferred<Point?>()
        val consumer = object : LocationConsumer {
            override fun onLocationUpdated(vararg location: Point, options: (ValueAnimator.() -> Unit)?) {
                location.lastOrNull()?.takeIf {
                    it.latitude().isFinite() && it.longitude().isFinite() &&
                        it.latitude() in -90.0..90.0 && it.longitude() in -180.0..180.0 &&
                        (it.latitude() != 0.0 || it.longitude() != 0.0)
                }?.let { result.complete(it) }
            }
            override fun onError(error: LocationError) { result.complete(null) }
            override fun onBearingUpdated(vararg bearing: Double, options: (ValueAnimator.() -> Unit)?) = Unit
            override fun onHorizontalAccuracyRadiusUpdated(vararg radius: Double, options: (ValueAnimator.() -> Unit)?) = Unit
            override fun onPuckLocationAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) = Unit
            override fun onPuckBearingAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) = Unit
            override fun onPuckAccuracyRadiusAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) = Unit
        }
        try {
            provider.registerLocationConsumer(consumer)
            result.await()
        } finally { provider.unRegisterLocationConsumer(consumer) }
    }
