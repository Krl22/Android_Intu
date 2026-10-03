package com.intu.taxi

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.repositories.AdminAccountAccess
import com.intu.taxi.repositories.AdminAuthProvider
import com.intu.taxi.ui.screens.AdminAccountAccessInfo
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Rule
import org.junit.Test

class AdminAccountAccessInfoTest {
    @get:Rule val compose = createComposeRule()

    @Test fun contactEmailDoesNotClaimGoogleIsLinked() {
        compose.setContent { IntuTheme { AdminAccountAccessInfo("phone-user", "contact@example.com", "+51999999999",
            AdminAccountAccess("phone-user", "found", phone = "+51987654321", providers = listOf(AdminAuthProvider("phone")))) } }
        compose.onNodeWithText("Google: Sin vincular").assertIsDisplayed()
        compose.onNodeWithText("Correo de contacto: contact@example.com").assertIsDisplayed()
        compose.onNodeWithText("Acceso por SMS: Vinculado · +51987654321").assertIsDisplayed()
        compose.onNodeWithText("Correo de la cuenta: Sin correo").assertIsDisplayed()
    }

    @Test fun showsBothLinkedMethodsAndTheActualGoogleEmail() {
        compose.setContent { IntuTheme { AdminAccountAccessInfo("linked-user", "other@example.com", "",
            AdminAccountAccess("linked-user", "found", "google@example.com", true, "+51987654321",
                providers = listOf(AdminAuthProvider("google.com", "google@example.com"), AdminAuthProvider("phone")))) } }
        compose.onNodeWithText("Cuenta: linked-user").assertIsDisplayed()
        compose.onNodeWithText("Google: Vinculado · google@example.com").assertIsDisplayed()
        compose.onNodeWithText("Correo de la cuenta: google@example.com · Verificado").assertIsDisplayed()
    }

    @Test fun failedLookupShowsUnknownInsteadOfUnlinked() {
        compose.setContent { IntuTheme { AdminAccountAccessInfo("user", "", "", null) } }
        compose.onNodeWithText("Google: Sin vincular").assertDoesNotExist()
        compose.onNodeWithText("Vinculación no disponible. Actualiza el panel para volver a consultar.").assertIsDisplayed()
    }
}
