package com.intu.taxi

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.intu.taxi.auth.UserProfile
import com.intu.taxi.ui.screens.ProfileCompletionScreen
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProfileCompletionTest {
    @get:Rule val compose = createComposeRule()
    private val completeDetails = UserProfile(firstName = "Ana", lastName = "QA", birthdate = "1995-01-10")

    private fun awaitForm() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Teléfono").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun googleProfileKeepsVerifiedPhoneAndAccountEmail() {
        var submitted: UserProfile? = null
        compose.setContent {
            IntuTheme {
                ProfileCompletionScreen("+51987654321", false, { submitted = it }, prefilledEmail = "tester@example.com", initialProfile = completeDetails)
            }
        }
        awaitForm()
        compose.onNodeWithText("Correo electrónico").assertDoesNotExist()
        compose.onNodeWithText("Verificar correo").assertDoesNotExist()
        compose.onNodeWithText("Teléfono").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
        compose.onNodeWithText("Guardar y continuar").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("+51987654321", submitted?.number)
            assertEquals("tester@example.com", submitted?.email)
        }
    }

    @Test
    fun phoneLoginKeepsVerifiedPhoneWithoutRequiringEmailOrFakeEmailVerification() {
        compose.setContent {
            IntuTheme { ProfileCompletionScreen("+51987654321", false, {}, initialProfile = completeDetails) }
        }
        awaitForm()
        compose.onNodeWithText("Teléfono").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
        compose.onNodeWithText("Guardar y continuar").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Verificar correo").assertDoesNotExist()
    }

    @Test
    fun incompletePhoneShowsErrorInsteadOfSubmitting() {
        var submitted: UserProfile? = null
        compose.setContent {
            IntuTheme {
                ProfileCompletionScreen(null, false, { submitted = it }, prefilledEmail = "tester@example.com", initialProfile = completeDetails)
            }
        }
        awaitForm()
        compose.onNodeWithText("Teléfono").performScrollTo().performTextInput("987")
        compose.onNodeWithText("Guardar y continuar").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa un teléfono válido, por ejemplo 987 654 321.").performScrollTo()
        compose.runOnIdle { assertNull(submitted) }
    }

    @Test
    fun internationalPhoneKeepsItsCountryCode() {
        var submitted: UserProfile? = null
        compose.setContent {
            IntuTheme {
                ProfileCompletionScreen(null, false, { submitted = it }, prefilledEmail = "tester@example.com", initialProfile = completeDetails)
            }
        }
        awaitForm()
        compose.onNodeWithText("Teléfono").performScrollTo().performTextInput("+1 555 123 4567")
        compose.onNodeWithText("Guardar y continuar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("+15551234567", submitted?.number) }
    }
}
