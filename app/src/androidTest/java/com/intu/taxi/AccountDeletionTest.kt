package com.intu.taxi

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.ui.screens.AccountDeletionDialog
import com.intu.taxi.ui.theme.IntuTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Callbacks locales: no crea solicitudes reales ni elimina cuentas. */
class AccountDeletionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun confirmsReceiptOnlyAfterSuccessAndPreventsDoubleSubmission() {
        val pending = CompletableDeferred<Unit>()
        val calls = AtomicInteger()
        val dismissed = AtomicBoolean()
        compose.setContent { IntuTheme {
            AccountDeletionDialog(submitRequest = { note ->
                assertEquals("", note)
                calls.incrementAndGet()
                pending.await()
            }, onDismiss = { dismissed.set(true) })
        } }
        compose.onNodeWithText("Solicitar eliminación").performClick()
        compose.waitUntil(5000) { calls.get() == 1 }
        compose.onNodeWithText("Enviando…").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").assertIsNotEnabled()
        compose.onNodeWithText("Solicitud recibida").assertDoesNotExist()
        compose.runOnIdle { pending.complete(Unit) }
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("Solicitud recibida").fetchSemanticsNodes().isNotEmpty()
        }
        assertFalse(dismissed.get())
        compose.onNodeWithText("Cerrar").performClick()
        assertTrue(dismissed.get())
        assertEquals(1, calls.get())
    }

    @Test fun failurePreservesNoteAndAllowsRetry() {
        val calls = AtomicInteger()
        compose.setContent { IntuTheme {
            AccountDeletionDialog(submitRequest = { note ->
                assertEquals("Por favor confirmar por mi correo", note)
                if (calls.incrementAndGet() == 1) error("Sin conexión de prueba")
            }, onDismiss = {})
        } }
        compose.onNodeWithText("Información adicional (opcional)").performScrollTo()
            .performTextInput("Por favor confirmar por mi correo")
        compose.onNodeWithText("Solicitar eliminación").performClick()
        compose.waitUntil(5000) { calls.get() == 1 }
        compose.onNodeWithText("Sin conexión de prueba").assertExists()
        compose.onNodeWithText("Solicitud recibida").assertDoesNotExist()
        compose.onNodeWithText("Por favor confirmar por mi correo").assertExists()
        compose.onNodeWithText("Solicitar eliminación").assertIsEnabled().performClick()
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("Solicitud recibida").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(2, calls.get())
    }

    @Test fun cancelDoesNotSubmitAnything() {
        val calls = AtomicInteger()
        val dismissed = AtomicBoolean()
        compose.setContent { IntuTheme {
            AccountDeletionDialog(submitRequest = { calls.incrementAndGet() },
                onDismiss = { dismissed.set(true) })
        } }
        compose.onNodeWithText("Cancelar").performClick()
        assertTrue(dismissed.get())
        assertEquals(0, calls.get())
    }
}
