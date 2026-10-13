package com.intu.taxi

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.permissions.rememberAppPermissions
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

/** Run on a dedicated emulator with location and notifications revoked before instrumentation. */
@SdkSuppress(minSdkVersion = 33)
class StartupPermissionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun locationThenNotificationsAppearWithoutMountingAMap() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS).forEach {
            assumeTrue("Revoke $it before this test", context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED)
        }
        val preferences = context.getSharedPreferences("intu_permissions", 0)
        preferences.edit().clear().commit()
        val automation = instrumentation.uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        val locationReady = AtomicBoolean()
        val dialogsFinished = AtomicBoolean()
        compose.setContent {
            val permissions = rememberAppPermissions(autoRequest = true)
            SideEffect {
                locationReady.set(permissions.locationGranted)
                dialogsFinished.set(!permissions.requestInProgress)
            }
            Text("Inicio sin mapa")
        }
        clickSystemButton("permission_allow_foreground_only_button")
        compose.waitUntil(10_000) { locationReady.get() }
        val notificationsButton = awaitSystemButton("permission_deny_button", locationDialogGone = true)
        assertTrue("Location must be granted before notifications appear", locationReady.get())
        assertTrue(notificationsButton.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil(10_000) { dialogsFinished.get() && preferences.getBoolean("notifications_asked", false) }
        assertTrue(locationReady.get())
        assertTrue(preferences.getBoolean("location_asked", false))
    }

    private fun clickSystemButton(id: String) {
        assertTrue(awaitSystemButton(id).performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun awaitSystemButton(id: String, locationDialogGone: Boolean = false): AccessibilityNodeInfo {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            val root = automation.rootInActiveWindow
            for (packageName in listOf("com.android.permissioncontroller", "com.google.android.permissioncontroller")) {
                val locationButtons = root?.findAccessibilityNodeInfosByViewId("$packageName:id/permission_allow_foreground_only_button")
                if (locationDialogGone && !locationButtons.isNullOrEmpty()) continue
                root?.findAccessibilityNodeInfosByViewId("$packageName:id/$id")?.firstOrNull()?.let { return it }
            }
            SystemClock.sleep(100)
        }
        error("System permission button did not appear: $id")
    }
}
