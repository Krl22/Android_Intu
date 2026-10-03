package com.intu.taxi

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.intu.taxi.repositories.RideHistoryItem
import com.intu.taxi.ui.screens.BugReportDialog
import com.intu.taxi.ui.screens.RideDetailsDialog
import com.intu.taxi.ui.screens.TermsDialog
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AccountToolsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bugReportExplainsInvalidFieldsAndSubmitsAValidDraftOnce() {
        val pending = CompletableDeferred<Unit>()
        val calls = AtomicInteger()
        val dismissed = AtomicBoolean()
        val description = "Al enviar mi solicitud aparece el modo de conductor antes de la aprobación."
        compose.setContent { IntuTheme { BugReportDialog(submitReport = { title, body, screen ->
            org.junit.Assert.assertEquals("Falla al cambiar de modo", title)
            org.junit.Assert.assertEquals(description, body)
            org.junit.Assert.assertEquals("", screen)
            calls.incrementAndGet()
            pending.await()
        }, onDismiss = { dismissed.set(true) }) } }
        screenshot("qa-report-empty.png")
        compose.onNodeWithText("Enviar reporte").assertIsEnabled().performClick()
        compose.onNodeWithText("Escribe un título de al menos 5 caracteres.").assertExists()
        compose.onNodeWithText("Describe el problema en al menos 15 caracteres.").assertExists()
        org.junit.Assert.assertEquals(0, calls.get())
        compose.onNodeWithText("Título").performTextInput("GPS")
        compose.onNodeWithText("¿Qué ocurrió?").performScrollTo().performTextInput(description)
        compose.onNodeWithText("Enviar reporte").assertIsDisplayed()
        compose.onNodeWithText("Enviar reporte").performClick()
        org.junit.Assert.assertEquals(0, calls.get())
        compose.onNodeWithText("Título").performScrollTo().performTextReplacement("  Falla al cambiar de modo  ")
        screenshot("qa-report-form.png")
        compose.onNodeWithText("Enviar reporte").performClick()
        compose.waitUntil(5000) { calls.get() == 1 }
        compose.onNodeWithText("Enviando…").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").assertIsNotEnabled()
        compose.runOnIdle { pending.complete(Unit) }
        compose.waitUntil(5000) { dismissed.get() }
        org.junit.Assert.assertEquals(1, calls.get())
    }

    @Test fun bugReportPreservesDraftAfterFailureAndAllowsRetry() {
        val calls = AtomicInteger()
        val dismissed = AtomicBoolean()
        compose.setContent { IntuTheme { BugReportDialog(submitReport = { title, body, _ ->
            org.junit.Assert.assertEquals("Error en Cuenta", title)
            org.junit.Assert.assertEquals("No funciona el botón de enviar el reporte.", body)
            if (calls.incrementAndGet() == 1) throw java.io.IOException("Sin conexión de prueba")
        }, onDismiss = { dismissed.set(true) }) } }
        compose.onNodeWithText("Título").performTextInput("Error en Cuenta")
        compose.onNodeWithText("¿Qué ocurrió?").performScrollTo().performTextInput("No funciona el botón de enviar el reporte.")
        compose.onNodeWithText("Enviar reporte").performClick()
        compose.waitUntil(5000) { calls.get() == 1 }
        compose.onNodeWithText("Sin conexión de prueba").assertExists()
        compose.onNodeWithText("Error en Cuenta").assertExists()
        compose.onNodeWithText("No funciona el botón de enviar el reporte.").assertExists()
        compose.onNodeWithText("Enviar reporte").assertIsEnabled().performClick()
        compose.waitUntil(5000) { dismissed.get() }
        org.junit.Assert.assertEquals(2, calls.get())
    }

    @Test fun storedRouteOpensAMapInTripDetails() {
        val ride = RideHistoryItem("qa-ride", "completed", Instant.parse("2026-09-30T20:00:00Z"),
            "Plaza de Satipo", "Hospital de Satipo", 4.0, "efectivo", "Conductor QA", "", "Mototaxi", "QA-001", "Pasajero QA", "", null, null,
            -11.2521, -74.6382, -11.2502, -74.6344,
            """{"type":"LineString","coordinates":[[-74.6382,-11.2521],[-74.637,-11.251],[-74.6344,-11.2502]]}""", 800, 240)
        compose.setContent { IntuTheme { RideDetailsDialog(ride, mutableMapOf()) {} } }
        compose.waitUntil(timeoutMillis = 15000) { compose.onAllNodesWithText("Ruta planificada").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ruta planificada").performScrollTo().assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 60000) { compose.onAllNodesWithTag("trip-map-ready").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("trip-map-ready").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cerrar").assertIsEnabled()
        screenshot("qa-trip-details.png")
    }

    @Test fun legalTabsKeepContentAccessibleAndClose() {
        val dismissed = AtomicBoolean()
        compose.setContent { IntuTheme { TermsDialog { dismissed.set(true) } } }
        android.os.SystemClock.sleep(3500) // Allow the previous fake submission toast to finish.
        compose.onNodeWithText("Uso de Intu").assertIsDisplayed()
        screenshot("qa-terms.png")
        compose.onNodeWithText("Pagos y viajes").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Privacidad").performClick()
        compose.onNodeWithText("Datos que usamos").assertIsDisplayed()
        screenshot("qa-privacy.png")
        compose.onNodeWithText("Tus datos y soporte").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sobre este borrador").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Términos").performClick()
        compose.onNodeWithText("Pagos y viajes").assertIsDisplayed()
        compose.onNodeWithText("Cerrar").assertIsEnabled().performClick()
        compose.waitUntil(5000) { dismissed.get() }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(350) // Capture after native dialog / IME transitions.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
