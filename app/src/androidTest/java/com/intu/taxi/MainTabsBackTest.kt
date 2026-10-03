package com.intu.taxi

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/** Navigates the existing signed-in session; it does not change accounts or submit rides. */
class MainTabsBackTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun accountAndTripsBackReturnToHomeAndKeepTheSession() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        assertNotNull("Requiere la sesión de QA existente", uid)
        waitForTab("Inicio")
        compose.onNode(hasText("Inicio") and isSelectable()).performClick()
        listOf("Cuenta", "Viajes").forEach { tab ->
            compose.onNode(hasText(tab) and isSelectable()).performClick()
            compose.onNode(hasText(tab) and isSelectable()).assertIsSelected()
            pressBack()
            compose.onNode(hasText("Inicio") and isSelectable()).assertIsSelected()
            assertEquals(uid, FirebaseAuth.getInstance().currentUser?.uid)
        }
    }

    private fun waitForTab(text: String) {
        compose.waitUntil(60_000) {
            compose.onAllNodes(hasText(text) and isSelectable()).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
