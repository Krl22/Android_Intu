package com.intu.taxi

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.models.DriverRideRequest
import com.intu.taxi.models.RidePriceOffer
import com.intu.taxi.ui.screens.DriverPriceOfferDialog
import com.intu.taxi.ui.screens.PassengerPriceOffers
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RidePriceOffersTest {
    @get:Rule val compose = createComposeRule()
    private val offer = RidePriceOffer("offer-qa", "ride-qa", 7.25, 5.5, "pending", "Ana", null, "Honda roja")

    @Test fun passengerMustConfirmExactPriceBeforeAssignment() {
        var response: Pair<String, Boolean>? = null
        var accepted = false
        compose.setContent { IntuTheme {
            PassengerPriceOffers("ride-qa", {}, { accepted = true },
                offersLoader = { listOf(offer) }, offerResponder = { id, yes -> response = id to yes })
        } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("accept-offer-offer-qa").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("accept-offer-offer-qa").performClick()
        compose.runOnIdle { assertNull(response); assertFalse(accepted) }
        compose.onNodeWithText("Tu viaje con Ana costará S/ 7.25. Al confirmar se asignará a este conductor.").assertIsDisplayed()
        captureNativeScreenshot(compose, "passenger-confirm-price-offer.png")
        compose.onNodeWithTag("confirm-offer").performClick()
        compose.waitUntil(5000) { accepted }
        compose.runOnIdle { assertEquals("offer-qa" to true, response) }
    }
    @Test fun rejectionDoesNotAssignDriver() {
        var response: Pair<String, Boolean>? = null
        var accepted = false
        compose.setContent { IntuTheme {
            PassengerPriceOffers("ride-qa", {}, { accepted = true },
                offersLoader = { listOf(offer) }, offerResponder = { id, yes -> response = id to yes })
        } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("reject-offer-offer-qa").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("reject-offer-offer-qa").performClick()
        compose.waitUntil(5000) { response != null }
        compose.runOnIdle { assertEquals("offer-qa" to false,response); assertFalse(accepted) }
    }
    @Test fun driverSendsProposedAmountAndInvalidPriceStaysInForm() {
        var sent: Pair<String, Double>? = null
        var finished = false
        compose.setContent { IntuTheme {
            DriverPriceOfferDialog(DriverRideRequest(requestId = "ride-qa",estimatedPrice = 5.5), {}, { finished = true },
                offerSender = { id, amount -> sent = id to amount })
        } }
        compose.onNodeWithTag("driver-offer-price").performTextReplacement("5,50")
        compose.onNodeWithTag("driver-send-offer").performClick()
        compose.runOnIdle { assertNull(sent); assertFalse(finished) }
        compose.onNodeWithTag("driver-offer-price").performTextReplacement("7,25")
        compose.onNodeWithTag("driver-send-offer").performClick()
        compose.waitUntil(5000) { finished }
        compose.runOnIdle { assertEquals("ride-qa" to 7.25,sent) }
    }
}
