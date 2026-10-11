package com.intu.taxi

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.repositories.TesterRequest
import com.intu.taxi.ui.screens.AdminTestersContent
import com.intu.taxi.ui.screens.AdminPanelTheme
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AdminTestersTest {
    @get:Rule val compose = createComposeRule()
    @Test fun reviewOrganizesInboxWithoutClaimingGoogleMembership() {
        var action: Pair<String,String>? = null
        val rows = listOf(TesterRequest("qa", "tester@example.com", "new", "2026-10-08T15:00:00Z"))
        compose.setContent { IntuTheme { AdminPanelTheme {
            AdminTestersContent(rows, null, null, {}, { id,status -> action = id to status })
        } } }
        compose.onNodeWithText("tester@example.com").assertIsDisplayed()
        compose.onNodeWithText("Marcar revisado").performClick()
        compose.runOnIdle { assertEquals("qa" to "reviewed", action) }
        compose.onNodeWithText("Marcar como revisado solo organiza esta bandeja.", substring=true).assertIsDisplayed()
        compose.onNodeWithText("Archivados").performClick()
        compose.onNodeWithText("No hay correos en esta sección.").assertIsDisplayed()
        captureNativeScreenshot(compose, "admin-testers.png")
    }
}
