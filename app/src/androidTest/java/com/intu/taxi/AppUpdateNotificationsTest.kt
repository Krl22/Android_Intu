package com.intu.taxi

import android.app.NotificationManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import androidx.work.WorkManager
import androidx.work.await
import com.intu.taxi.updates.AppUpdateNotifications
import com.intu.taxi.updates.AppUpdateWorker
import com.intu.taxi.updates.PublishedAppRelease
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.FileInputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

@SdkSuppress(minSdkVersion = 33)
class AppUpdateNotificationsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notices = AppUpdateNotifications(context)
    private val release = PublishedAppRelease(BuildConfig.VERSION_CODE + 1, "Prueba", 1000, "a".repeat(64))

    private fun awaitNotificationCount(expected: Int) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 3_000
        while (manager.activeNotifications.count { it.tag == AppUpdateNotifications.TAG } != expected &&
            android.os.SystemClock.elapsedRealtime() < deadline) android.os.SystemClock.sleep(50)
        assertEquals(expected, manager.activeNotifications.count { it.tag == AppUpdateNotifications.TAG })
    }

    private fun permission(granted: Boolean) {
        instrumentation.uiAutomation.executeShellCommand(
            "pm ${if (granted) "grant" else "revoke"} ${context.packageName} android.permission.POST_NOTIFICATIONS"
        ).use { descriptor -> FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
    }

    @Before fun prepare() {
        permission(true)
        manager.cancel(AppUpdateNotifications.TAG, 0)
        context.getSharedPreferences("intu_app_updates", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun cleanUp() {
        permission(true)
        manager.cancel(AppUpdateNotifications.TAG, 0)
        context.getSharedPreferences("intu_app_updates", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun postsOncePerVersionAndInstalledUpdateClearsTheNotification() {
        assertTrue(notices.show(release, BuildConfig.VERSION_CODE))
        awaitNotificationCount(1)
        val notification = manager.activeNotifications.single { it.tag == AppUpdateNotifications.TAG }.notification
        assertEquals("Nueva versión de Intu", notification.extras.getCharSequence("android.title"))
        assertEquals(AppUpdateNotifications.CHANNEL, notification.channelId)
        assertNotNull(notification.contentIntent)
        assertEquals(context.packageName, notification.contentIntent.creatorPackage)
        assertFalse(AppUpdateNotifications(context).show(release, BuildConfig.VERSION_CODE))
        assertTrue(notices.show(release.copy(versionCode = release.versionCode + 1), BuildConfig.VERSION_CODE))
        assertEquals(1, manager.activeNotifications.count { it.tag == AppUpdateNotifications.TAG })
        notices.clearInstalled(release.versionCode + 1)
        awaitNotificationCount(0)
    }

    @Test fun tappingTheNotificationOpensTheAppWithAnUpdateRequest() {
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        try {
            assertTrue(notices.show(release, BuildConfig.VERSION_CODE))
            awaitNotificationCount(1)
            manager.activeNotifications.single { it.tag == AppUpdateNotifications.TAG }
                .notification.contentIntent.send()
            val activity = monitor.waitForActivityWithTimeout(10_000)
            assertNotNull("The notification must open Intu", activity)
            try {
                assertTrue(activity.intent.getBooleanExtra(AppUpdateNotifications.OPEN_UPDATE_EXTRA, false))
            } finally {
                instrumentation.runOnMainSync { activity.finish() }
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test fun repeatedAppStartupKeepsOneBackgroundCheck() = runBlocking {
        withTimeout(10_000) {
            AppUpdateWorker.schedule(context).await()
            val manager = WorkManager.getInstance(context)
            val first = manager.getWorkInfosForUniqueWorkFlow(AppUpdateWorker.WORK_NAME).first().single().id
            AppUpdateWorker.schedule(context).await()
            val second = manager.getWorkInfosForUniqueWorkFlow(AppUpdateWorker.WORK_NAME).first().single().id
            assertEquals(first, second)
        }
    }
}
