package com.intu.taxi

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.maps.MapView

/** Sends real pointer events to the SDK, including a pinch deliberately away from the pin. */
internal class NativeMapGestures(private val mapView: MapView) {
    /** Dispatch through the window, including Compose overlays, using real elapsed time. */
    fun panThroughWindow() {
        val location = IntArray(2)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { mapView.getLocationOnScreen(location) }
        val startX = location[0] + mapView.width * .3f
        val y = location[1] + mapView.height * .2f
        val downTime = SystemClock.uptimeMillis()
        fun send(action: Int, x: Float) {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try { InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(event, true) }
            finally { event.recycle() }
        }
        send(MotionEvent.ACTION_DOWN, startX)
        for (step in 1..15) {
            SystemClock.sleep(25)
            send(MotionEvent.ACTION_MOVE, startX + mapView.width * .3f * step / 15f)
        }
        send(MotionEvent.ACTION_UP, startX + mapView.width * .3f)
    }

    fun pinchOut() {
        val midpointX = mapView.width * 0.6f
        val midpointY = mapView.height * 0.65f
        val downTime = SystemClock.uptimeMillis()
        // Start wide enough to stay above the SDK's minimum span while actually zooming.
        val initialHalfSpan = mapView.width * 0.35f
        touch(downTime, MotionEvent.ACTION_DOWN, midpointX - initialHalfSpan, midpointY)
        touch(downTime, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            midpointX - initialHalfSpan, midpointY, midpointX + initialHalfSpan, midpointY)
        for (step in 1..20) {
            SystemClock.sleep(20)
            val span = initialHalfSpan * (1f - 0.8f * step / 20f)
            touch(downTime, MotionEvent.ACTION_MOVE, midpointX - span, midpointY, midpointX + span, midpointY)
        }
        val lastSpan = initialHalfSpan * 0.2f
        touch(downTime, MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            midpointX - lastSpan, midpointY, midpointX + lastSpan, midpointY)
        touch(downTime, MotionEvent.ACTION_UP, midpointX - lastSpan, midpointY)
    }

    fun pan() {
        val startX = mapView.width * 0.4f
        val y = mapView.height * 0.5f
        val downTime = SystemClock.uptimeMillis()
        touch(downTime, MotionEvent.ACTION_DOWN, startX, y)
        for (step in 1..10) {
            SystemClock.sleep(20)
            touch(downTime, MotionEvent.ACTION_MOVE, startX + mapView.width * 0.02f * step, y)
        }
        touch(downTime, MotionEvent.ACTION_UP, startX + mapView.width * 0.2f, y)
    }

    private fun touch(downTime: Long, action: Int, vararg positions: Float) {
        val count = positions.size / 2
        val properties = Array(count) { index -> MotionEvent.PointerProperties().apply {
            id = index; toolType = MotionEvent.TOOL_TYPE_FINGER
        } }
        val coordinates = Array(count) { index -> MotionEvent.PointerCoords().apply {
            x = positions[index * 2]; y = positions[index * 2 + 1]; pressure = 1f; size = 1f
        } }
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, count, properties,
            coordinates, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { mapView.dispatchTouchEvent(event) }
        } finally { event.recycle() }
    }
}
