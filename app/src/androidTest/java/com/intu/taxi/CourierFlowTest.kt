package com.intu.taxi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.models.DeliveryDetails
import com.intu.taxi.models.DeliveryPayer
import com.intu.taxi.models.DriverRideRequest
import com.intu.taxi.ui.map.TripRoute
import com.intu.taxi.ui.screens.DeliveryDetailsDialog
import com.intu.taxi.ui.screens.DeliveryPaymentDialog
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.screens.RideBooking
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger

class CourierFlowTest {
    @get:Rule val compose = createComposeRule()
    private val scopeLabel = "Confirmo que es un paquete pequeño, apto para llevar en moto. Solicito solo transporte, sin compras ni cobro de productos."

    @Test fun senderChoosesDeliveryPickupAndRecipientBeforeRequestingTheFinalFare() {
        val requested = AtomicReference<RideBooking>()
        val calls = AtomicInteger()
        val release = CompletableDeferred<Unit>()
        val origin = Point.fromLngLat(-74.6382, -11.2521)
        val gps = object : LocationProvider {
            override fun registerLocationConsumer(consumer: LocationConsumer) { consumer.onLocationUpdated(origin) }
            override fun unRegisterLocationConsumer(consumer: LocationConsumer) = Unit
        }
        compose.setContent { IntuTheme { HomeScreen(PaddingValues(), locationProvider = gps,
            routeLoader = { pickup, destination ->
                val finalRoute = calls.incrementAndGet() > 1
                TripRoute(listOf(pickup, destination), if (finalRoute) 2100.0 else 1200.0, if (finalRoute) 420.0 else 240.0)
            },
            rideRequestSender = { requested.set(it); release.await(); Result.failure(IllegalStateException("Envío simulado para QA")) }
        ) } }
        waitFor("Marcador")
        compose.onNodeWithText("Viajar").assertDoesNotExist()
        compose.onNodeWithText("Enviar paquete").assertDoesNotExist()
        compose.onNodeWithText("Marcador").performClick()
        waitFor("Confirmar destino")
        compose.onNodeWithText("Confirmar destino").performClick()
        waitFor("Elige tu moto")
        compose.onNodeWithText("Moto para envíos").performScrollTo().performClick()
        compose.onNodeWithText("Elegir recojo").performClick()
        waitFor("Elegir punto de recojo")
        assertNull(requested.get())
        compose.onNodeWithTag("home-pickup-pin").assertIsDisplayed()
        compose.onNodeWithText("Continuar con envío").performClick()
        waitFor("Datos del envío")
        compose.onNodeWithText("Solicitar envío").assertIsNotEnabled()
        assertNull(requested.get())
        compose.onNodeWithText("¿Qué enviarás?").performScrollTo().performTextInput("Documentos en un sobre")
        compose.onNodeWithText("Nombre de quien recibe").performScrollTo().performTextInput("Ana QA")
        compose.onNodeWithText("Celular de quien recibe").performScrollTo().performTextInput("987654321")
        compose.onNodeWithText("Quien recibe · al entregar").performScrollTo().performClick()
        compose.onNodeWithText("Solicitar envío").assertIsNotEnabled()
        compose.onNodeWithText(scopeLabel).performScrollTo().performClick()
        compose.onNodeWithText("Solicitar envío").assertIsEnabled().performClick()
        compose.waitUntil(10_000) { requested.get() != null }
        val booking = requested.get()
        assertEquals("motorcycle", booking.rideType)
        assertEquals(2100.0, booking.route.distanceMeters, 0.0)
        assertEquals(420.0, booking.route.durationSeconds, 0.0)
        assertEquals(4.2, booking.estimatedPrice, 0.0001)
        assertEquals("+51987654321", booking.delivery?.recipientPhone)
        assertEquals(DeliveryPayer.RECIPIENT, booking.delivery?.payer)
        assertTrue(booking.delivery?.smallPackageConfirmed == true)
        assertEquals(2, calls.get()) // No extra Directions call when submitting the already priced pickup.
        compose.runOnIdle { release.complete(Unit) }
        waitFor("Envío simulado para QA")
    }

    @Test fun invalidRecipientPhoneAndUncheckedParcelCannotBeSubmitted() {
        val submitted = AtomicReference<DeliveryDetails>()
        compose.setContent { IntuTheme { DeliveryDetailsDialog(3.2, {}, { submitted.set(it) }) } }
        compose.onNodeWithText("¿Qué enviarás?").performScrollTo().performTextInput("Sobre")
        compose.onNodeWithText("Nombre de quien recibe").performScrollTo().performTextInput("Ana")
        compose.onNodeWithText("Celular de quien recibe").performScrollTo().performTextInput("876543210")
        compose.onNodeWithText(scopeLabel).performScrollTo().performClick()
        compose.onNodeWithText("Solicitar envío").assertIsNotEnabled()
        assertNull(submitted.get())
        compose.onNodeWithText("Celular de quien recibe").performScrollTo().performTextReplacement("987654321")
        compose.onNodeWithText("Solicitar envío").assertIsEnabled()
        compose.onNodeWithText(scopeLabel).performScrollTo().performClick()
        compose.onNodeWithText("Solicitar envío").assertIsNotEnabled()
    }

    @Test fun recipientPaymentAndDeliveryRequireExplicitConfirmation() {
        val confirmed = AtomicInteger()
        val details = DeliveryDetails("Ana", "+51987654321", "Sobre", payer = DeliveryPayer.RECIPIENT, smallPackageConfirmed = true)
        compose.setContent { IntuTheme { DeliveryPaymentDialog(
            DriverRideRequest(requestId = "qa-local", estimatedPrice = 4.2, paymentMethod = "efectivo", serviceKind = "delivery", delivery = details),
            pickup = false, busy = false, error = null, onDismiss = {}, onConfirm = { confirmed.incrementAndGet() }) } }
        compose.onNodeWithText("Finalizar envío").assertIsNotEnabled()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Finalizar envío").assertIsEnabled().performClick()
        assertEquals(1, confirmed.get())
    }

    private fun waitFor(text: String) {
        compose.waitUntil(60_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
}

