package com.intu.taxi

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.intu.taxi.models.*
import com.intu.taxi.ui.screens.*
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AdCarouselTest {
    @get:Rule val compose = createComposeRule()
    private val ads = listOf(
        BusinessAd(id = "chicken", name = "Brasa Satipo · Demo", title = "1/4 de pollo + papas", city = "Satipo",
            offerDetail = "Papas, ensalada y ají", offerPrice = 18.90, demoPhoto = BusinessPhoto.CHICKEN),
        BusinessAd(id = "juane", name = "Sazón Río Negro · Demo", title = "Juane tradicional con todos los sabores de la selva central",
            city = "Río Negro", offerDetail = "Pollo, arroz y sabor de la selva para compartir en familia", offerPrice = 14.90, demoPhoto = BusinessPhoto.JUANE),
        BusinessAd(id = "short", name = "Café Satipo · Demo", title = "Café")
    )

    private fun sameHeights(dark: Boolean, largeText: Boolean = false) {
        compose.setContent { IntuTheme(darkTheme = dark) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.5f else 1f)) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    ads.forEach { BusinessAdCard(it, {}, Modifier.fillMaxWidth()) }
                }
            }
        } }
        val heights = ads.map {
            compose.onNodeWithTag("business-ad-${it.id}").performScrollTo().fetchSemanticsNode().boundsInRoot.height
        }
        heights.forEach { assertEquals(heights.first(), it, 1f) }
        compose.onAllNodesWithText("Ver menú →", useUnmergedTree = true).assertCountEquals(3)
        compose.onNodeWithTag("business-ad-chicken").performScrollTo()
        captureNativeScreenshot(compose, "ads-${if (dark) "dark" else "light"}${if (largeText) "-large" else ""}.png")
    }

    @Test fun cardsHaveEqualHeightInLightMode() = sameHeights(false)
    @Test fun cardsHaveEqualHeightInDarkMode() = sameHeights(true)
    @Test fun cardsKeepEqualHeightWithLargeText() = sameHeights(true, true)

    private fun waitPage(page: Int, count: Int = 3, timeout: Long = 20_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodesWithContentDescription("Promoción $page de $count").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun defaultIntervalWaitsThreeSecondsAndManualSwipeRestartsIt() {
        val enabled = mutableStateOf(false)
        compose.setContent { IntuTheme {
            BusinessAdsSection(BusinessFeed(true, ads), null, {}, {}, enabled.value)
        } }
        waitPage(1)
        compose.runOnIdle { enabled.value = true }
        SystemClock.sleep(1400)
        compose.onNodeWithContentDescription("Promoción 1 de 3").assertExists()
        waitPage(2)
        compose.onNodeWithTag("business-promotions-pager").performTouchInput { swipeLeft() }
        waitPage(3)
        SystemClock.sleep(1400)
        compose.onNodeWithContentDescription("Promoción 3 de 3").assertExists()
        waitPage(1)
    }

    @Test fun customIntervalLoopsAndPausesWhileViewingBusiness() {
        val enabled = mutableStateOf(false)
        compose.setContent { IntuTheme(darkTheme = true) {
            BusinessAdsSection(BusinessFeed(true, ads, 1), null, {}, {}, enabled.value)
        } }
        waitPage(1)
        SystemClock.sleep(1500)
        compose.onNodeWithContentDescription("Promoción 1 de 3").assertExists()
        compose.runOnIdle { enabled.value = true }
        waitPage(2); waitPage(3); waitPage(1)
        compose.runOnIdle { enabled.value = false }
        SystemClock.sleep(1500)
        compose.onNodeWithContentDescription("Promoción 1 de 3").assertExists()
    }

    @Test fun removingAdsDuringRotationKeepsRemainingCardAndDisabledFeedHidesIt() {
        val feed = mutableStateOf(BusinessFeed(true, ads, 1))
        val enabled = mutableStateOf(false)
        compose.setContent { IntuTheme { BusinessAdsSection(feed.value, null, {}, {}, enabled.value) } }
        waitPage(1)
        compose.runOnIdle { enabled.value = true }
        waitPage(2)
        compose.runOnIdle { feed.value = BusinessFeed(true, listOf(ads.last()), 1) }
        compose.onNodeWithTag("business-ad-short").assertIsDisplayed()
        SystemClock.sleep(1500)
        compose.onNodeWithTag("business-ad-short").assertIsDisplayed()
        compose.runOnIdle { feed.value = BusinessFeed(false, emptyList(), 1) }
        compose.onNodeWithTag("business-promotions-pager").assertDoesNotExist()
    }

    @Test fun adminValidatesIntervalAndOnlyShowsConfirmedSavedValue() {
        val saved = mutableIntStateOf(3)
        val busy = mutableStateOf(false)
        var submitted = 0
        compose.setContent { IntuTheme(darkTheme = true) {
            AdRotationSetting(saved.intValue, busy.value) { submitted = it }
        } }
        compose.onNodeWithTag("admin-save-ad-interval").assertIsNotEnabled()
        listOf("", "0", "61", "invalid").forEach {
            compose.onNodeWithTag("admin-ad-interval").performTextReplacement(it)
            compose.onNodeWithTag("admin-save-ad-interval").assertIsNotEnabled()
        }
        compose.onNodeWithTag("admin-ad-interval").performTextReplacement("7")
        compose.onNodeWithTag("admin-save-ad-interval").performClick()
        compose.runOnIdle { assertEquals(7, submitted) }
        compose.onNodeWithText("Entre 1 y 60 segundos · guardado: 3 s").assertExists()
        compose.runOnIdle { busy.value = true }
        compose.onNodeWithTag("admin-ad-interval").assertIsNotEnabled()
        compose.onNodeWithTag("admin-save-ad-interval").assertIsNotEnabled()
        compose.runOnIdle { saved.intValue = 7; busy.value = false }
        compose.onNodeWithText("Entre 1 y 60 segundos · guardado: 7 s").assertExists()
        compose.onNodeWithTag("admin-save-ad-interval").assertIsNotEnabled()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        captureNativeScreenshot(compose, "ads-admin-interval.png")
    }
}
