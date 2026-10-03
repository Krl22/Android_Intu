package com.intu.taxi

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.auth.DriverProfile
import com.intu.taxi.ui.screens.DriverDataCollectionScreen
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DriverApplicationTest {
    @get:Rule val compose = createComposeRule()

    private fun openForm(onSubmit: (DriverProfile) -> Unit = {}) {
        compose.setContent { IntuTheme { DriverDataCollectionScreen(onSubmit, {}) } }
        compose.mainClock.advanceTimeBy(2500)
        compose.waitForIdle()
    }

    private fun select(label: String, value: String) {
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.onNodeWithText(value).performClick()
    }

    private fun fillIdentity() {
        select("Año del vehículo", java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString())
        compose.onNodeWithText("Placa del vehículo").performScrollTo().performTextInput("qat123")
        compose.onNodeWithText("DNI (8 dígitos)").performScrollTo().performTextInput("99161001")
        compose.onNodeWithText("Licencia de conducir").performScrollTo().performTextInput("QA-Licencia")
    }

    @Test fun motorcycleApplicationSubmitsCourierTypeAndStaysUnapproved() {
        var submitted: DriverProfile? = null
        openForm { submitted = it }
        select("Tipo de vehículo", "Moto lineal")
        compose.onNodeWithText("Courier / repartidor. Tu solicitud será revisada por Intu antes de recibir pedidos de paquetes pequeños.")
            .assertExists()
        select("Marca del vehículo", "Yamaha")
        select("Modelo del vehículo", "MT 15")
        fillIdentity()
        compose.onNodeWithText("Enviar solicitud").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals("motorcycle", submitted?.vehicleType)
            assertEquals("Yamaha", submitted?.vehicleBrand)
            assertEquals("MT 15", submitted?.vehicleModel)
            assertEquals("QAT123", submitted?.licensePlate)
            assertEquals("99161001", submitted?.documentNumber)
            assertFalse(submitted?.isApproved ?: true)
        }
    }

    @Test fun changingVehicleClearsIncompatibleBrandAndModel() {
        openForm()
        select("Tipo de vehículo", "Moto lineal")
        select("Marca del vehículo", "Yamaha")
        select("Modelo del vehículo", "MT 15")
        select("Tipo de vehículo", "Mototaxi")
        assertEquals("", compose.onNodeWithText("Marca del vehículo").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        assertEquals("", compose.onNodeWithText("Modelo del vehículo").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNodeWithText("Enviar solicitud").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Marca del vehículo").performScrollTo().performClick()
        compose.onNodeWithText("Yamaha").assertDoesNotExist()
        compose.onNodeWithText("Bajaj").assertExists()
    }

    @Test fun mototaxiRegistrationStillSubmitsTheOriginalType() {
        var submitted: DriverProfile? = null
        openForm { submitted = it }
        select("Marca del vehículo", "Bajaj")
        select("Modelo del vehículo", "Boxer 150")
        fillIdentity()
        compose.onNodeWithText("Enviar solicitud").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("mototaxi", submitted?.vehicleType) }
    }
}
