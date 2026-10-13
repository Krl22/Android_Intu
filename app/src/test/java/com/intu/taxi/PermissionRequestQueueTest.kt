package com.intu.taxi

import com.intu.taxi.permissions.AppPermission
import com.intu.taxi.permissions.PermissionRequestQueue
import org.junit.Assert.*
import org.junit.Test

class PermissionRequestQueueTest {
    private fun PermissionRequestQueue.next(location: Boolean = false, notifications: Boolean = false,
        auto: Boolean = true, realLocation: Boolean = true, resumed: Boolean = true) =
        nextRequest(location, notifications, auto, realLocation, resumed)

    @Test fun firstRunWaitsForLocationResultBeforeAskingForNotifications() {
        val initial = PermissionRequestQueue()
        assertEquals(AppPermission.LOCATION, initial.next())
        val locationDialog = initial.launch(AppPermission.LOCATION)
        assertNull(locationDialog.next())
        assertNull(locationDialog.next(location = true))
        val answered = locationDialog.complete(cancelled = false)
        assertEquals(AppPermission.NOTIFICATIONS, answered.next(location = true))
        assertNull(answered.next(location = true, resumed = false))
        val notificationsDialog = answered.launch(AppPermission.NOTIFICATIONS)
        assertNull(notificationsDialog.next(location = true))
        assertNull(notificationsDialog.complete(cancelled = false).next(location = true, notifications = true))
    }

    @Test fun deniedLocationDoesNotBlockNotificationsOrRepeatAfterNavigationAndRestart() {
        val locationDenied = PermissionRequestQueue().launch(AppPermission.LOCATION).complete(cancelled = false)
        assertEquals(AppPermission.NOTIFICATIONS, locationDenied.next())
        val bothDenied = locationDenied.launch(AppPermission.NOTIFICATIONS).complete(cancelled = false)
        assertNull(bothDenied.next())
        assertNull(bothDenied.next(auto = false))
        assertNull(bothDenied.resumed().next())
        val restoredFromPreferences = PermissionRequestQueue(bothDenied.locationAsked, bothDenied.notificationsAsked)
        assertNull(restoredFromPreferences.next())
        assertEquals(AppPermission.LOCATION, restoredFromPreferences.request(AppPermission.LOCATION).next())
    }

    @Test fun cancelledRequestIsRetriedAfterResumeAndNeverRecordedAsADenial() {
        val cancelled = PermissionRequestQueue().launch(AppPermission.LOCATION).complete(cancelled = true)
        assertFalse(cancelled.locationAsked)
        assertFalse(cancelled.notificationsAsked)
        assertNull(cancelled.next())
        assertNull(cancelled.resumed().next(auto = false))
        assertEquals(AppPermission.LOCATION, cancelled.resumed().next())
    }

    @Test fun manualLocationRequestWaitsBehindTheCurrentDialogAndWorksOutsideHome() {
        val notificationsDialog = PermissionRequestQueue(locationAsked = true).launch(AppPermission.NOTIFICATIONS)
        val queuedLocation = notificationsDialog.request(AppPermission.LOCATION)
        assertNull(queuedLocation.next(auto = false))
        val finished = queuedLocation.complete(cancelled = false)
        assertEquals(AppPermission.LOCATION, finished.next(auto = false))
        val locationDialog = finished.launch(AppPermission.LOCATION)
        assertEquals(locationDialog, locationDialog.request(AppPermission.LOCATION))
        assertNull(locationDialog.complete(cancelled = false).next(auto = false))
    }

    @Test fun alreadyGrantedPermissionsAndPreAndroid13NotificationsAreSkipped() {
        assertNull(PermissionRequestQueue().next(location = true, notifications = true))
        assertEquals(AppPermission.NOTIFICATIONS, PermissionRequestQueue().next(location = true))
        val locationOnly = PermissionRequestQueue().launch(AppPermission.LOCATION).complete(cancelled = false)
        assertNull(locationOnly.next(location = true, notifications = true))
    }

    @Test fun simulationSkipsGpsAndAsksForItWhenReturningToRealLocation() {
        val initial = PermissionRequestQueue()
        assertEquals(AppPermission.NOTIFICATIONS, initial.next(realLocation = false))
        val answered = initial.launch(AppPermission.NOTIFICATIONS).complete(cancelled = false)
        assertNull(answered.next(realLocation = false))
        assertEquals(AppPermission.LOCATION, answered.next(realLocation = true))
    }

    @Test fun loginAndBackgroundNeverStartAutomaticDialogs() {
        assertNull(PermissionRequestQueue().next(auto = false))
        assertNull(PermissionRequestQueue().next(resumed = false))
    }
}
