package com.intu.taxi

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.updates.AppUpdateNotifications
import com.intu.taxi.updates.PublishedAppRelease
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.FileInputStream

/** Run separately after revoking permission from adb; revoking during a test kills its process. */
@SdkSuppress(minSdkVersion = 33)
class AppUpdateNotificationPermissionTest {
    @Test fun deniedPermissionDoesNotLoseTheFutureNotification() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue("Run after adb shell pm revoke com.intu.taxi android.permission.POST_NOTIFICATIONS",
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
        val preferences = context.getSharedPreferences("intu_app_updates", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val manager = context.getSystemService(NotificationManager::class.java)
        val notices = AppUpdateNotifications(context)
        val release = PublishedAppRelease(BuildConfig.VERSION_CODE + 1, "Prueba", 1000, "a".repeat(64))
        try {
            assertFalse(notices.show(release, BuildConfig.VERSION_CODE))
            assertEquals(0, preferences.getInt(AppUpdateNotifications.NOTIFIED_CODE, 0))
            instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS"
            ).use { descriptor -> FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
            assertTrue(notices.show(release, BuildConfig.VERSION_CODE))
        } finally {
            manager.cancel(AppUpdateNotifications.TAG, 0)
            preferences.edit().clear().commit()
        }
    }
}
