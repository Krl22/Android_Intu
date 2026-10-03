package com.intu.taxi.ui.map

import android.content.Context
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar

/** Defaults for every Intu map: no scale, visible attribution, zoom about the viewport center. */
fun createIntuMapView(context: Context): MapView = MapView(context).apply {
    scalebar.enabled = false
    gestures.pinchScrollEnabled = false
    addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        if (view.width > 0 && view.height > 0) {
            gestures.focalPoint = ScreenCoordinate(view.width / 2.0, view.height / 2.0)
        }
    }
}

fun MapView.pointUnderCenterPin(): Point? = if (width > 0 && height > 0) {
    mapboxMap.coordinateForPixel(ScreenCoordinate(width / 2.0, height / 2.0))
} else null
