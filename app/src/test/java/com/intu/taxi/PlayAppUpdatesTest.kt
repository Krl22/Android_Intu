package com.intu.taxi

import com.intu.taxi.updates.AppUpdateState
import com.intu.taxi.updates.PublishedAppRelease
import com.intu.taxi.updates.playRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayAppUpdatesTest {
    @Test
    fun availablePlayUpdateIsOfferedWithoutWebDownloadDetails() {
        val release = playRelease(available = true, availableVersionCode = 38, downloadBytes = 35_000_000, installedCode = 37)
        val offered = AppUpdateState(release = release, checked = true).newerThan(37)
        assertEquals(38, offered?.versionCode)
        assertTrue(offered!!.fromPlay)
        assertEquals("Nueva versión de Intu", offered.title)
    }

    @Test
    fun noPlayUpdateMeansAlreadyUpToDate() {
        val release = playRelease(available = false, availableVersionCode = 0, downloadBytes = 0, installedCode = 37)
        assertNull(AppUpdateState(release = release, checked = true).newerThan(37))
    }

    @Test
    fun playNeverOffersAnOlderOrSameVersion() {
        val release = playRelease(available = true, availableVersionCode = 37, downloadBytes = 1, installedCode = 37)
        assertNull(AppUpdateState(release = release, checked = true).newerThan(37))
    }

    @Test
    fun webReleasesKeepTheirVersionName() {
        assertEquals("Intu 1.37", PublishedAppRelease(38, "1.37", 1, "a".repeat(64)).title)
    }
}
