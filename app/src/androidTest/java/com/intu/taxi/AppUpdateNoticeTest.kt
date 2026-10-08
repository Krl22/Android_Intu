package com.intu.taxi

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.ui.screens.AppUpdateNotice
import com.intu.taxi.ui.screens.AppUpdateSettingsItem
import com.intu.taxi.updates.AppUpdateState
import com.intu.taxi.updates.PublishedAppRelease
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppUpdateNoticeTest {
    @get:Rule val compose = createComposeRule()
    private val release = PublishedAppRelease(34, "1.33", 106000000, "a".repeat(64))

    @Test fun fetchedUpdateWaitsForASafeScreenAndTheEndOfTheTrip() {
        var state by mutableStateOf(AppUpdateState())
        var canPrompt by mutableStateOf(false)
        var tripActive by mutableStateOf(true)
        compose.setContent {
            AppUpdateNotice(state, 33, "1.32", canPrompt, tripActive, false, {}, { true }, {})
        }
        compose.runOnIdle { state = AppUpdateState(release = release, checked = true) }
        compose.onNodeWithTag("app-update-dialog").assertDoesNotExist()
        compose.runOnIdle { canPrompt = true }
        compose.onNodeWithTag("app-update-dialog").assertDoesNotExist()
        compose.runOnIdle { tripActive = false }
        compose.onNodeWithText("Hay una nueva versión").assertIsDisplayed()
        compose.runOnIdle { tripActive = true }
        compose.onNodeWithTag("app-update-dialog").assertDoesNotExist()
    }

    @Test fun postponementKeepsAccountNoticeAndManualAccessWhileANewerReleasePromptsAgain() {
        var state by mutableStateOf(AppUpdateState(release = release, checked = true))
        var requested by mutableStateOf(false)
        compose.setContent {
            AppUpdateNotice(state, 33, "1.32", true, false, requested, {}, { true }, { requested = false })
            AppUpdateSettingsItem(state.newerThan(33)) { requested = true }
        }
        compose.onNodeWithText("Más tarde").performClick()
        compose.onNodeWithTag("app-update-dialog").assertDoesNotExist()
        compose.onNodeWithText("Intu 1.33 disponible").assertIsDisplayed()
        compose.onNodeWithTag("account-app-updates").performClick()
        compose.onNodeWithTag("app-update-dialog").assertIsDisplayed()
        compose.onNodeWithText("Más tarde").performClick()
        compose.runOnIdle { state = state.copy(release = release.copy(versionCode = 35, versionName = "1.34")) }
        compose.onNodeWithText("Intu 1.34").assertIsDisplayed()
    }

    @Test fun failedDownloadRemainsAvailableAndSuccessfulDownloadPostponesTheNotice() {
        var opens = false
        var attempts = 0
        compose.setContent {
            AppUpdateNotice(AppUpdateState(release = release, checked = true), 33, "1.32",
                true, false, false, {}, { attempts++; opens }, {})
        }
        compose.onNodeWithTag("app-update-download").performClick()
        compose.onNodeWithTag("app-update-dialog").assertIsDisplayed()
        compose.runOnIdle { opens = true }
        compose.onNodeWithTag("app-update-download").performClick()
        compose.onNodeWithTag("app-update-dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, attempts) }
    }

    @Test fun currentBuildAndConnectionErrorsDoNotTriggerAutomaticNotices() {
        var state by mutableStateOf(AppUpdateState(error = "Sin conexión"))
        compose.setContent { AppUpdateNotice(state, 34, "1.33", true, false, false, {}, { true }, {}) }
        compose.onNodeWithTag("app-update-dialog").assertDoesNotExist()
        compose.runOnIdle { state = AppUpdateState(release = release, checked = true) }
        compose.onNodeWithTag("app-update-dialog").assertDoesNotExist()
    }
}
