package com.intu.taxi.ui.map

import android.view.View
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView

/** Reports the geographic point under the fixed pin; Intu's map factory anchors all zoom gestures. */
class PinSelectionMapController(
    private val mapView: MapView,
    private val onSelectionChanged: (Point) -> Unit
) : AutoCloseable {
    private val cameraSubscription = mapView.mapboxMap.subscribeCameraChanged { updateSelection() }
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        updateSelection()
    }

    init {
        mapView.addOnLayoutChangeListener(layoutListener)
        updateSelection()
    }

    private fun updateSelection() {
        mapView.pointUnderCenterPin()?.let(onSelectionChanged)
    }

    override fun close() {
        mapView.removeOnLayoutChangeListener(layoutListener)
        cameraSubscription.cancel()
    }
}
