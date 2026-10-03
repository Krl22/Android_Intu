package com.intu.taxi

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthProvider
import com.intu.taxi.ui.screens.PhoneAuthForm
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PhoneAuthFlowTest {
    @get:Rule val compose = createComposeRule()
    private fun token(): PhoneAuthProvider.ForceResendingToken = PhoneAuthProvider.ForceResendingToken::class.java
        .getDeclaredConstructor().apply { isAccessible = true }.newInstance()

    @Test fun fullPeruvianNumberIsSentOnceWithoutDuplicateCountryCode() {
        var sent = ""
        var sends = 0
        compose.setContent { IntuTheme { PhoneAuthForm("", false, {}, {}, { phone, _, callbacks ->
            sent = phone; sends++; callbacks.onCodeSent("qa-session", token())
        }) } }
        compose.onNodeWithText("Enviar código").assertIsNotEnabled()
        compose.onNodeWithTag("phone-number").performTextInput("123456789")
        compose.onNodeWithText("Enviar código").assertIsNotEnabled()
        compose.onNodeWithTag("phone-number").performTextReplacement("+51 987 654 321")
        compose.onNodeWithText("Enviar código").performScrollTo().performClick()
        compose.onNodeWithText("Código enviado a +51987654321").assertIsDisplayed()
        assertEquals("+51987654321", sent)
        assertEquals(1, sends)
        compose.onNodeWithText("Verificar código").assertIsNotEnabled()
        compose.onNodeWithTag("phone-resend").performScrollTo().assertIsNotEnabled()
    }

    @Test fun codeEntryAndCooldownSurviveStateRestoreWithoutAnotherSms() {
        var sends = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent { IntuTheme { PhoneAuthForm("+51987654321", false, {}, {}, { _, _, callbacks ->
            sends++; callbacks.onCodeSent("restored-qa-session", token())
        }) } }
        compose.onNodeWithText("Enviar código").performClick()
        compose.onNodeWithTag("phone-otp").performTextInput("123456")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Código enviado a +51987654321").assertExists()
        compose.onNodeWithTag("phone-otp").assertExists()
        compose.onNodeWithText("Verificar código").assertIsNotEnabled()
        compose.onNodeWithTag("phone-resend").performScrollTo().assertIsNotEnabled()
        assertEquals(1, sends)
    }

    @Test fun invalidOtpShowsReadableErrorAndAllowsRetryWithoutAnotherSms() {
        var checks = 0
        var sends = 0
        compose.setContent { IntuTheme { PhoneAuthForm("+51987654321", true, {
            checks++
            if (checks == 1) throw FirebaseAuthInvalidCredentialsException("ERROR_INVALID_VERIFICATION_CODE", "Invalid code")
        }, {}, { _, _, callbacks -> sends++; callbacks.onCodeSent("qa-session", token()) }) } }
        compose.onNodeWithText("Enviar código").performClick()
        compose.onNodeWithTag("phone-otp").performTextInput("123")
        compose.onNodeWithText("Verificar código").assertIsNotEnabled()
        compose.onNodeWithTag("phone-otp").performTextReplacement("123456")
        compose.onNodeWithText("Verificar código").performScrollTo().performClick()
        compose.onNodeWithTag("phone-auth-error").performScrollTo().assertTextContains("El código no es correcto.", substring = true)
        compose.onNodeWithTag("phone-otp").performScrollTo().performTextReplacement("654321")
        compose.onNodeWithText("Verificar código").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, checks); assertEquals(1, sends) }
    }

    @Test fun resendingSameVerificationIdRestartsCooldownAndOldCallbackIsIgnored() {
        val callbacks = mutableListOf<PhoneAuthProvider.OnVerificationStateChangedCallbacks>()
        compose.setContent { IntuTheme { PhoneAuthForm("+51987654321", false, {}, {}, { _, _, cb ->
            callbacks.add(cb); cb.onCodeSent("same-qa-session", token())
        }, resendDelayMillis = 1200) } }
        compose.onNodeWithText("Enviar código").performClick()
        compose.onNodeWithTag("phone-resend").performScrollTo().assertIsNotEnabled()
        compose.waitUntil(5_000) {
            !compose.onNodeWithTag("phone-resend").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
        }
        compose.onNodeWithTag("phone-resend").assertIsEnabled()
        compose.onNodeWithTag("phone-resend").performClick()
        compose.onNodeWithTag("phone-resend").assertIsNotEnabled()
        assertEquals(2, callbacks.size)
        compose.onNodeWithText("Cambiar número").performScrollTo().performClick()
        compose.runOnIdle { callbacks.last().onCodeSent("obsolete-qa-session", token()) }
        compose.onNodeWithTag("phone-number").assertExists()
        compose.onNodeWithTag("phone-otp").assertDoesNotExist()
    }
}
