package com.intu.taxi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.intu.taxi.data.CatalogPlace
import com.intu.taxi.location.TestLocationPreset
import com.intu.taxi.push.AdminNotificationPreferences
import com.intu.taxi.repositories.*
import com.intu.taxi.ui.screens.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Native, isolated fixtures: no accounts, rides, reports or preferences are changed on the server. */
class AdminPanelDesignTest {
    @get:Rule val compose = createComposeRule()
    private val driver = AdminDriver("qa-driver", "María Fernanda Villanueva", "999 888 777", "maria@example.test", "",
        "12345678", "B-IIc 123456", "pending", 4.8, 12, null, "Honda", "ABC-123", "mototaxi")
    private val user = AdminUser("qa-user", "Carlos Villanueva", "999 888 777", "carlos@example.test", "",
        null, true, 12, 0, AdminAccountAccess("qa-user", "found", "google@example.test", true,
            providers = listOf(AdminAuthProvider("google.com", "google@example.test"))))
    private val report = BugReport("qa-report", "No aparece mi dirección guardada",
        "Al abrir Inicio no aparece el acceso rápido a mi dirección. Ocurre después de volver a entrar.",
        "Inicio", "1.27 (28)", "Samsung · Android 16", "open", "2026-10-02T15:00:00Z", "Lucía Tester")
    private val place = CatalogPlace("qa-place", "Plaza principal de Satipo", aliases = listOf("parque central"),
        category = "square", address = "Entrada frente a la municipalidad", latitude = -11.252, longitude = -74.638,
        status = "published", pickupVerified = true)

    @Test fun panelNavigationAndActionsRemainReachableAtLargeFont() {
        var tab by mutableIntStateOf(0)
        var location by mutableStateOf(false)
        var refresh = 0
        var approvals = 0
        var accountAction: UserAction? = null
        var reportStatus: String? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                AdminPanelTheme {
                    AdminPanelLayout(PaddingValues(top = 24.dp, bottom = 24.dp), tab, true, null,
                        {}, { refresh++ }, { location = true }, { tab = it }) {
                        when (tab) {
                            0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item { AdminSectionHeading("Conductores", "Revisa postulaciones y gestiona viajes o envíos.", 1) }
                                item { AdminDriverCard(driver, false, { approvals++ }, {}, {}, {}) }
                            }
                            1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item { AdminSectionHeading("Usuarios", "Consulta perfiles, vinculación y actividad.", 1) }
                                item { AdminUserCard(user, true, false, false) { accountAction = it } }
                            }
                            2 -> AdminReportsContent(listOf(report), null, null, {}, { _, status -> reportStatus = status })
                            3 -> AdminPlacesContent(listOf(place), false, null, "all", "", {}, {}, {}, {}, {})
                            else -> AdminNotificationSettingsContent(AdminNotificationPreferences(), false, null, true, {}, {}, {}, {})
                        }
                    }
                    if (location) TestLocationDialog(TestLocationPreset.SATIPO, false, null, {}, {}, { location = false }, {})
                }
            }
        }
        screenshot("admin-conductores-large.png")
        compose.onNodeWithText("Ver documentación").performScrollTo().performClick()
        compose.onNodeWithText("B-IIc 123456").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Aprobar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, approvals) }
        compose.onNodeWithContentDescription("Actualizar").performClick()
        compose.runOnIdle { assertEquals(1, refresh) }
        compose.onNodeWithTag("admin-tab-1").performScrollTo().performClick()
        screenshot("admin-usuarios-large.png")
        compose.onNodeWithText("Ver acceso y vinculación").performScrollTo().performClick()
        compose.onNodeWithText("Google: Vinculado · google@example.test").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Acceso por SMS: Sin vincular").assertExists()
        compose.onNodeWithText("Gestionar cuenta").performScrollTo().performClick()
        compose.runOnIdle { assertNull(accountAction) }
        compose.onNodeWithText("Reiniciar conductor").assertIsNotEnabled()
        screenshot("admin-acciones-cuenta-large.png")
        compose.onNodeWithText("Eliminar cuenta").performClick()
        compose.runOnIdle { assertEquals(UserAction.Delete, accountAction) }
        compose.onNodeWithTag("admin-tab-2").performScrollTo().performClick()
        screenshot("admin-reportes-large.png")
        compose.onNodeWithText("Revisar reporte").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("in_progress", reportStatus) }
        compose.onNodeWithTag("admin-tab-3").performScrollTo().performClick()
        compose.onNodeWithText(place.name).performScrollTo().assertIsDisplayed()
        screenshot("admin-lugares-large.png")
        compose.onNodeWithTag("admin-tab-4").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Recibir actividad").performScrollTo().assertIsDisplayed()
        screenshot("admin-notificaciones-large.png")
        compose.onNodeWithText("Simular mi ubicación").performClick()
        compose.onNodeWithText("Elegir en el mapa").assertIsDisplayed()
        compose.onNodeWithText("Simular en Río Negro").performScrollTo().assertIsDisplayed()
        screenshot("admin-simulacion-large.png")
        compose.onNodeWithText("Cerrar").performClick()
    }

    @Test fun catalogFiltersAndSearchKeepEditingAndNewPlaceAvailable() {
        var filter by mutableStateOf("all")
        var query by mutableStateOf("")
        var edited: CatalogPlace? = null
        var additions = 0
        compose.setContent { AdminPanelTheme {
            AdminPlacesContent(listOf(place, place.copy(id = "draft", name = "Hospital QA", status = "draft")),
                false, null, filter, query, { filter = it }, { query = it }, {}, { additions++ }, { edited = it })
        } }
        compose.onNodeWithText("Borrador (1)").performClick()
        compose.onNodeWithText("Hospital QA").assertExists()
        compose.onNodeWithText(place.name).assertDoesNotExist()
        compose.onNodeWithText("Publicado (1)").performClick()
        compose.onNodeWithText("Buscar en el catálogo").performTextInput("parque central")
        compose.onNodeWithText(place.name).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(place, edited) }
        compose.onNodeWithText("Buscar en el catálogo").performScrollTo().performTextReplacement("desconocido")
        compose.onNodeWithText("No hay coincidencias. Puedes buscar por nombre, localidad o referencia.").assertExists()
        compose.onNodeWithContentDescription("Borrar búsqueda").performScrollTo().performClick()
        compose.onNodeWithText("Nuevo lugar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, additions) }
    }

    @Test fun reportFilteringTechnicalDetailsAndBusyActions() {
        var busy by mutableStateOf<String?>(null)
        var status: String? = null
        compose.setContent { AdminPanelTheme {
            AdminReportsContent(listOf(report, report.copy(id = "resolved", title = "Ya resuelto", status = "resolved")),
                null, busy, {}, { _, value -> status = value })
        } }
        compose.onNodeWithText("Nuevos").performClick()
        compose.onNodeWithText("Ya resuelto").assertDoesNotExist()
        compose.onNodeWithText("Ver datos técnicos").performScrollTo().performClick()
        compose.onNodeWithText("Samsung · Android 16").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { busy = report.id }
        compose.onNodeWithText("Revisar reporte").assertIsNotEnabled()
        compose.runOnIdle { assertNull(status); busy = null }
        compose.onNodeWithText("Marcar como resuelto").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("resolved", status) }
    }

    private fun screenshot(name: String) = captureNativeScreenshot(compose, name)
}
