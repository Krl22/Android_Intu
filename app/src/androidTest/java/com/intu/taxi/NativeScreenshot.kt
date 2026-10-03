package com.intu.taxi

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

internal fun captureNativeScreenshot(compose: ComposeTestRule, name: String) {
    compose.waitForIdle()
    // Compose idleness can precede the first SurfaceFlinger presentation on this emulator.
    SystemClock.sleep(1000)
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val bitmap = instrumentation.uiAutomation.takeScreenshot()
    try {
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    } finally { bitmap.recycle() }
}
