package com.intu.taxi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.intu.taxi.repositories.RideHistoryItem
import com.intu.taxi.ui.screens.TripsContent
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class TripsDesignTest {
    @get:Rule val compose = createComposeRule()

    private val completed = RideHistoryItem(
        id = "qa-completed", status = "completed", requestedAt = Instant.now(),
        originAddress = "Plaza de Satipo", destinationAddress = "Hospital de Satipo",
        fare = 4.0, paymentMethod = "yape_plin", driverName = "María Villanueva",
        driverPhotoUrl = "", vehicleDescription = "Honda", vehiclePlate = "ABC-123",
        riderName = "Carlos Villar", riderPhotoUrl = "", ratingForDriver = null, ratingForRider = null
    )

    @Test fun historyKeepsDetailsRatingsAndDriverEarningsAcrossAppearanceChanges() {
        var dark by mutableStateOf(true)
        var driver by mutableStateOf(false)
        var selected: RideHistoryItem? = null
        var rated: Pair<String, Int>? = null
        compose.setContent {
            IntuTheme(darkTheme = dark) {
                TripsContent(PaddingValues(top = 24.dp, bottom = 24.dp), driver,
                    listOf(completed, completed.copy(id = "qa-cancelled", status = "cancelled",
                        serviceKind = "delivery", requestedAt = completed.requestedAt?.minusSeconds(3600))),
                    null, 4.8 to 12, {}, { ride, stars -> rated = ride.id to stars }, { selected = it })
            }
        }
        compose.onNodeWithText("Mis viajes").assertIsDisplayed()
        val completedCard = hasAnyAncestor(hasTestTag("trip-qa-completed"))
        compose.onNodeWithTag("trips-screen").performScrollToNode(hasTestTag("trip-qa-completed"))
        compose.onNode(hasText("María Villanueva") and completedCard, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("trip-qa-completed").performClick()
        compose.runOnIdle { assertEquals(completed, selected) }
        compose.onNode(hasContentDescription("3 estrellas") and hasAnyAncestor(hasTestTag("trip-qa-completed")))
            .performScrollTo().performClick()
        compose.runOnIdle { assertEquals("qa-completed" to 3, rated) }
        compose.onNodeWithTag("trips-screen").performScrollToIndex(0)
        captureNativeScreenshot(compose, "trips-unified-dark.png")
        compose.runOnIdle { dark = false }
        compose.onNode(hasText("María Villanueva") and completedCard, useUnmergedTree = true).assertExists()
        captureNativeScreenshot(compose, "trips-unified-light.png")
        compose.runOnIdle { driver = true; dark = true }
        compose.onNodeWithText("Mis servicios").assertIsDisplayed()
        compose.onNodeWithText("Ganancias").assertExists()
        compose.onNodeWithTag("trips-screen").performScrollToNode(hasTestTag("trip-qa-completed"))
        compose.onNode(hasText("Carlos Villar") and completedCard, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Califica al pasajero").assertExists()
        compose.onNodeWithTag("trips-screen").performScrollToIndex(0)
        captureNativeScreenshot(compose, "trips-driver-unified-dark.png")
        compose.onNodeWithTag("trips-screen").performScrollToNode(hasTestTag("trip-qa-cancelled"))
        compose.onNodeWithTag("trip-qa-cancelled").assertExists()
        compose.onNode(hasContentDescription("3 estrellas") and hasAnyAncestor(hasTestTag("trip-qa-cancelled")))
            .assertDoesNotExist()
    }

    @Test fun emptyHistoryAndFailedLoadKeepRefreshAvailable() {
        var error by mutableStateOf<String?>(null)
        var refreshes = 0
        compose.setContent {
            IntuTheme(darkTheme = true) {
                TripsContent(PaddingValues(), false, emptyList(), error, null,
                    { refreshes++ }, { _, _ -> }, {})
            }
        }
        compose.onNodeWithText("Aún no tienes viajes").assertIsDisplayed()
        compose.onNodeWithContentDescription("Actualizar").performClick()
        compose.runOnIdle { assertEquals(1, refreshes); error = "Sin conexión de prueba" }
        compose.onNodeWithText("Sin conexión de prueba").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(2, refreshes) }
    }
}
