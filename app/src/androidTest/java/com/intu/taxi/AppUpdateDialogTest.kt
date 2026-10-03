package com.intu.taxi

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.ui.screens.AppUpdateDialog
import com.intu.taxi.updates.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppUpdateDialogTest {
    @get:Rule val compose = createComposeRule()
    private val release = PublishedAppRelease(30, "1.29", 106000000, "a".repeat(64))

    @Test fun availableReleaseCanDownloadOrBePostponed() {
        var download: PublishedAppRelease? = null
        var closed = 0
        compose.setContent { AppUpdateDialog(AppUpdateState(release = release, checked = true), 29, "1.28", false,
            {}, { download = it }, { closed++ }) }
        compose.onNodeWithText("Hay una nueva versión").assertIsDisplayed()
        captureNativeScreenshot(compose, "app-update-available.png")
        compose.onNodeWithTag("app-update-download").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(release, download) }
        compose.onNodeWithText("Más tarde").performClick()
        compose.runOnIdle { assertEquals(1, closed) }
    }

    @Test fun currentOrNewerInstalledBuildDoesNotOfferADowngrade() {
        compose.setContent { AppUpdateDialog(AppUpdateState(release = release, checked = true), 31, "1.30", false, {}, {}, {}) }
        compose.onNodeWithText("Ya tienes la versión más reciente.").assertIsDisplayed()
        compose.onNodeWithTag("app-update-download").assertDoesNotExist()
    }

    @Test fun networkErrorCanRetryAndShowsTheRecoveredRelease() {
        var state by mutableStateOf(AppUpdateState(error = "Revisa tu conexión."))
        compose.setContent { AppUpdateDialog(state, 29, "1.28", false,
            { state = AppUpdateState(release = release, checked = true) }, {}, {}) }
        compose.onNodeWithTag("app-update-error").assertIsDisplayed()
        compose.onNodeWithText("Ya tienes la versión más reciente.").assertDoesNotExist()
        compose.onNodeWithText("Reintentar").performClick()
        compose.onNodeWithTag("app-update-download").assertIsEnabled()
    }

    @Test fun tripInProgressDisablesTheInstallAction() {
        compose.setContent { AppUpdateDialog(AppUpdateState(release = release, checked = true), 29, "1.28", true, {}, {}, {}) }
        compose.onNodeWithTag("app-update-download").assertIsNotEnabled()
        compose.onNodeWithText("Termina tu viaje o envío antes de actualizar.").assertIsDisplayed()
    }
}
