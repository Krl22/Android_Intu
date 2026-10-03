package com.intu.taxi.ui.map

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.intu.taxi.location.TestLocationPreset
import com.intu.taxi.location.TestLocation
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.locationcomponent.DefaultLocationProvider
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.maps.plugin.locationcomponent.location

class FixedLocationProvider(private val point: Point) : LocationProvider {
    override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
        // Jump directly to the test city; never animate an intermediate location from Lima.
        locationConsumer.onLocationUpdated(point, options = { duration = 0L })
        locationConsumer.onHorizontalAccuracyRadiusUpdated(0.0, options = { duration = 0L })
    }
    override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) = Unit
}

/** App state follows source coordinates, never an old puck position or its animation frames. */
private class ReportingLocationProvider(
    private val source: LocationProvider,
    private val onLocation: (Point) -> Unit
) : LocationProvider, AutoCloseable {
    private val consumers = mutableMapOf<LocationConsumer, LocationConsumer>()
    private var closed = false

    override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
        if (closed) return
        var first = true
        val proxy = object : LocationConsumer by locationConsumer {
            override fun onLocationUpdated(vararg location: Point, options: (ValueAnimator.() -> Unit)?) {
                if (closed) return
                val initial = first
                location.lastOrNull()?.let { onLocation(it); first = false }
                locationConsumer.onLocationUpdated(*location, options = if (initial) ({ duration = 0L }) else options)
            }
        }
        consumers.put(locationConsumer, proxy)?.let { source.unRegisterLocationConsumer(it) }
        source.registerLocationConsumer(proxy)
    }

    override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) {
        consumers.remove(locationConsumer)?.let { source.unRegisterLocationConsumer(it) }
    }

    override fun close() {
        closed = true
        consumers.values.toList().forEach { source.unRegisterLocationConsumer(it) }
        consumers.clear()
    }
}

/** Owns one listener and switches the puck and app coordinates together, including restoring GPS. */
@Composable
fun MapLocationBinding(
    mapView: MapView,
    styleLoaded: Boolean,
    hasPermission: Boolean,
    preset: TestLocation?,
    realLocationProvider: LocationProvider? = null,
    onLocation: (Point, Boolean) -> Unit
) {
    val context = LocalContext.current
    val realProvider = remember(mapView, realLocationProvider) { realLocationProvider ?: DefaultLocationProvider(context) }
    val callback = rememberUpdatedState(onLocation)
    DisposableEffect(mapView, styleLoaded, hasPermission, preset) {
        if (!styleLoaded) return@DisposableEffect onDispose {}
        var first = true
        val fixed = preset?.let { Point.fromLngLat(it.longitude, it.latitude) }
        fun deliver(point: Point) {
            if (point.latitude() !in -90.0..90.0 || point.longitude() !in -180.0..180.0 ||
                (fixed == null && point.latitude() == 0.0 && point.longitude() == 0.0)) return
            callback.value(point, first)
            first = false
        }
        val provider = ReportingLocationProvider(fixed?.let { FixedLocationProvider(it) } ?: realProvider) { point -> deliver(point) }
        mapView.location.setLocationProvider(provider)
        mapView.location.updateSettings { enabled = hasPermission || fixed != null }
        if (fixed != null) deliver(fixed)
        else if (!hasPermission) deliver(Point.fromLngLat(TestLocationPreset.SATIPO.longitude, TestLocationPreset.SATIPO.latitude))
        onDispose { provider.close() }
    }
}
