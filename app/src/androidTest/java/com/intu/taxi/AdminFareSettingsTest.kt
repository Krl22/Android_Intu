package com.intu.taxi

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.models.FareSettings
import com.intu.taxi.ui.screens.AdminFareSettingsContent
import com.intu.taxi.ui.screens.AdminPanelTheme
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AdminFareSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allRatesAndDriverOfferSwitchCanBeSavedTogether() {
        var saved: FareSettings? = null
        compose.setContent { IntuTheme { AdminPanelTheme {
            AdminFareSettingsContent(FareSettings.Default, false, null, false, {}, { saved = it })
        } } }
        listOf("2", "1,25", "0,2", "3,03", "20", "0,5").forEachIndexed { index, value ->
            compose.onNodeWithTag("fare-field-$index").performScrollTo().performTextReplacement(value)
        }
        compose.onNodeWithTag("fare-driver-offers").performScrollTo().assertIsOff().performClick()
        compose.onNodeWithTag("fare-preview").performScrollTo().assertTextContains("S/ 9.50", substring = true)
        compose.onNodeWithTag("fare-save").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(FareSettings(2.0,1.25,0.2,3.03,20.0,0.5,true),saved) }
        captureNativeScreenshot(compose, "admin-editable-fares.png")
    }
    @Test fun invalidValuesCannotBeSavedAndBusyFormDisablesChanges() {
        var saves = 0
        val busy = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent { IntuTheme { AdminPanelTheme {
            AdminFareSettingsContent(FareSettings.Default, busy.value, null, false, {}, { saves++ })
        } } }
        compose.onNodeWithTag("fare-field-0").performTextReplacement("-1")
        compose.onNodeWithTag("fare-save").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0,saves); busy.value = true }
        compose.onNodeWithTag("fare-save").assertIsNotEnabled()
        compose.onNodeWithTag("fare-driver-offers").performScrollTo().assertIsNotEnabled()
    }
}
