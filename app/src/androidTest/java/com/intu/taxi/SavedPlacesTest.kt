package com.intu.taxi

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.data.SavedPlace
import com.intu.taxi.data.SavedPlaces
import com.intu.taxi.ui.screens.SavedPlacesDialogContent
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SavedPlacesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun savedCardsAreReadableAndActionsRemainAvailableInBothThemes() {
        var dark by mutableStateOf(false)
        var places by mutableStateOf(emptyList<SavedPlace>())
        var edited: Pair<SavedPlace?, String>? = null
        var removed: String? = null
        var dismissed = false
        val work = SavedPlace("trabajo", "Trabajo", "Av. Principal, puerta lateral", -11.25, -74.63)
        compose.setContent { IntuTheme(darkTheme = dark) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                SavedPlacesDialogContent(places, { dismissed = true }, { place, name -> edited = place to name }, { removed = it })
            }
        } }
        for (darkMode in listOf(false, true)) {
            compose.runOnIdle { dark = darkMode; places = emptyList() }
            compose.onNodeWithText("0 de 8").assertIsDisplayed()
            compose.onNodeWithTag("saved-place-card-casa").performScrollTo()
            assertReadableTitle("casa", "Casa", darkMode)
            compose.onNode(hasText("Agregar") and hasAnyAncestor(hasTestTag("saved-place-card-casa")))
                .performClick()
            compose.runOnIdle { assertEquals(null to "Casa", edited) }
            captureNativeScreenshot(compose, "saved-places-empty-${if (darkMode) "dark" else "light"}.png")
            compose.runOnIdle { places = listOf(work) }
            compose.onNodeWithText("1 de 8").assertExists()
            compose.onNodeWithTag("saved-place-card-trabajo").performScrollTo()
            assertReadableTitle("trabajo", "Trabajo", darkMode)
            compose.onNode(hasText("Editar") and hasAnyAncestor(hasTestTag("saved-place-card-trabajo"))).performClick()
            compose.runOnIdle { assertEquals(work to "Trabajo", edited) }
            compose.onNodeWithContentDescription("Quitar Trabajo").performClick()
            compose.runOnIdle { assertEquals("trabajo", removed) }
            captureNativeScreenshot(compose, "saved-places-saved-${if (darkMode) "dark" else "light"}.png")
        }
        compose.onNodeWithText("Listo").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }

    @Test fun largeTextKeepsSavedPlaceActionsReachable() {
        var edited: String? = null
        var dismissed = false
        compose.setContent { IntuTheme(darkTheme = true) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                SavedPlacesDialogContent(listOf(SavedPlace("trabajo", "Trabajo", "Av. Principal, puerta lateral", -11.25, -74.63)),
                    { dismissed = true }, { _, name -> edited = name }, {})
            }
        } }
        compose.onNode(hasText("Editar") and hasAnyAncestor(hasTestTag("saved-place-card-trabajo")))
            .performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Trabajo", edited) }
        compose.onNodeWithText("Agregar otro lugar").performScrollTo().assertIsDisplayed()
        captureNativeScreenshot(compose, "saved-places-large-dark.png")
        compose.onNodeWithText("Listo").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }

    private fun assertReadableTitle(id: String, title: String, dark: Boolean) {
        val card = compose.onNodeWithTag("saved-place-card-$id").captureToImage().toPixelMap()
        // Sample the actual card surface away from the rounded edges, icon and text.
        val background = card[card.width / 2, (card.height * .04f).toInt()].luminance()
        val text = compose.onNodeWithText(title, useUnmergedTree = true).captureToImage().toPixelMap()
        var ink = background
        for (y in 0 until text.height) for (x in 0 until text.width) {
            val value = text[x, y].luminance()
            ink = if (dark) maxOf(ink, value) else minOf(ink, value)
        }
        val contrast = (maxOf(background, ink) + .05f) / (minOf(background, ink) + .05f)
        assertTrue("$title must contrast with its actual card (dark=$dark): $contrast", contrast >= 4.5f)
    }

    @Test fun placesPersistAcrossRecreationAndStaySeparateBetweenAccounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uid = "qa-places-${java.util.UUID.randomUUID()}"
        val first = SavedPlaces(context, uid)
        val other = SavedPlaces(context, "$uid-other")
        val casa = SavedPlace("casa", "Casa", "Satipo", -11.25, -74.63)
        try {
            first.save(casa)
            assertEquals(casa, SavedPlaces(context, uid).read().single())
            assertTrue(other.read().isEmpty())
            first.save(casa.copy(address = "Mi nueva casa", longitude = -74.62))
            assertEquals(1, first.read().size)
            assertEquals(-74.62, first.read().single().longitude, 0.00001)
            first.remove("casa")
            assertTrue(SavedPlaces(context, uid).read().isEmpty())
        } finally {
            context.getSharedPreferences("intu_saved_places", 0).edit().remove("places_$uid").remove("places_${uid}-other").commit()
        }
    }
}
