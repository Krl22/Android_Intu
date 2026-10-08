package com.intu.taxi

import com.intu.taxi.updates.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AppUpdatesTest {
    private val release = PublishedAppRelease(30, "1.29", 106000000, "a".repeat(64))
    private fun metadata() = JSONObject().put("available", true).put("updateAvailable", true)
        .put("packageName", "com.intu.taxi").put("versionCode", 30).put("versionName", "1.29")
        .put("minSdk", 24).put("size", 106000000).put("sha256", "a".repeat(64))

    @Test fun comparesIntegerCodesAndPinsDownloadToThePublishedVersion() {
        val parsed = parsePublishedRelease(metadata().put("downloadUrl", "https://example.test/unsafe.apk").toString(), "com.intu.taxi", 36)
        assertEquals(release, parsed)
        assertEquals("https://viajaconintu.pages.dev/descargar?versionCode=30", parsed.downloadUrl)
        val state = AppUpdateState(release = parsed, checked = true)
        assertNotNull(state.newerThan(29))
        assertNull(state.newerThan(30))
        assertNull(state.newerThan(31))
    }

    @Test fun rejectsIncompleteIncompatibleAndForeignMetadata() {
        val invalid = listOf(
            metadata().put("updateAvailable", false), metadata().put("packageName", "another.app"),
            metadata().put("versionCode", 0), metadata().put("versionCode", 2147483648L), metadata().put("versionCode", 30.5),
            metadata().put("minSdk", 37), metadata().put("size", 0), metadata().put("sha256", "bad"),
            metadata().put("versionName", ""), JSONObject().put("available", true)
        )
        invalid.forEach { assertThrows(Exception::class.java) { parsePublishedRelease(it.toString(), "com.intu.taxi", 36) } }
    }

    @Test fun backgroundNotificationOnlyAnnouncesANewUnseenVersion() {
        assertTrue(shouldNotifyAppUpdate(release, installedCode = 29, notifiedCode = 0))
        assertFalse(shouldNotifyAppUpdate(release, installedCode = 30, notifiedCode = 0))
        assertFalse(shouldNotifyAppUpdate(release, installedCode = 31, notifiedCode = 0))
        assertFalse(shouldNotifyAppUpdate(release, installedCode = 29, notifiedCode = 30))
        assertTrue(shouldNotifyAppUpdate(release.copy(versionCode = 31), installedCode = 29, notifiedCode = 30))
    }

    @Test fun automaticChecksAreThrottledButManualCheckFindsTheNextVersion() = runBlocking {
        var requests = 0
        var time = 0L
        val controller = AppUpdateController({ requests++; release.copy(versionCode = 29 + requests) }, { time })
        controller.check()
        time += 1000
        controller.check()
        assertEquals(1, requests)
        controller.check(force = true)
        assertEquals(2, requests)
        assertEquals(31, controller.state.value.release?.versionCode)
        time += 15 * 60 * 1000L - 1
        controller.check()
        assertEquals(2, requests)
        time += 1
        controller.check()
        assertEquals(3, requests)
    }

    @Test fun connectionFailureKeepsKnownReleaseAndManualRetryRecovers() = runBlocking {
        var fail = false
        val controller = AppUpdateController({ if (fail) error("offline") else release }, { 0L })
        controller.check()
        fail = true
        controller.check(force = true)
        assertNotNull(controller.state.value.error)
        assertEquals(release, controller.state.value.release)
        assertFalse(controller.state.value.checking)
        fail = false
        controller.check(force = true)
        assertNull(controller.state.value.error)
        assertEquals(release, controller.state.value.release)
    }

    @Test fun failedFirstCheckDoesNotClaimTheInstalledVersionIsCurrent() = runBlocking {
        var requests = 0
        var time = 0L
        val controller = AppUpdateController({ requests++; error("offline") }, { time })
        controller.check()
        assertFalse(controller.state.value.checked)
        assertNotNull(controller.state.value.error)
        controller.check()
        assertEquals(1, requests)
        time += 5 * 60 * 1000L
        controller.check()
        assertEquals(2, requests)
    }

    @Test fun concurrentChecksUseOneRequestAndCancellationDoesNotLeaveItChecking() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val result = CompletableDeferred<PublishedAppRelease>()
        var requests = 0
        val controller = AppUpdateController({ requests++; started.complete(Unit); result.await() }, { 0L })
        val first = launch { controller.check() }
        started.await()
        controller.check(force = true)
        assertEquals(1, requests)
        first.cancelAndJoin()
        assertFalse(controller.state.value.checking)
        result.complete(release)
        controller.check(force = true)
        assertEquals(release, controller.state.value.release)
    }
}
