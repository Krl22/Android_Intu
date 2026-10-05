package com.intu.taxi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import com.intu.taxi.models.*
import com.intu.taxi.ui.map.TripRoute
import com.intu.taxi.ui.map.pointUnderCenterPin
import com.intu.taxi.ui.map.TripMap
import com.intu.taxi.ui.screens.*
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Local UI fixtures; no live orders, announcements or courier identities are changed. */
class BusinessDeliveryTest {
    @get:Rule val compose = createComposeRule()
    private val ad = BusinessAd("qa-business", "Cocina Demo", BusinessCategory.FOOD, "Algo rico, cerca de ti",
        "Un negocio ficticio para probar un envío.", address = "Recojo demo en Satipo", latitude = -11.254, longitude = -74.640,
        published = true, updatedAt = "2026-10-05T05:00:00Z")

    private fun launch(dark: Boolean, requested: AtomicReference<RideBooking>) {
        val ready = AtomicBoolean()
        val gps = object : LocationProvider {
            override fun registerLocationConsumer(consumer: LocationConsumer) {
                consumer.onLocationUpdated(Point.fromLngLat(-74.6382, -11.2521)); ready.set(true)
            }
            override fun unRegisterLocationConsumer(consumer: LocationConsumer) = Unit
        }
        compose.setContent { IntuTheme(darkTheme = dark) {
            HomeScreen(PaddingValues(), locationProvider = gps, businessFeedLoader = { BusinessFeed(true, listOf(ad)) },
                routeLoader = { origin, target -> TripRoute(listOf(origin, target), 2100.0, 420.0) },
                rideRequestSender = { booking -> requested.set(booking); Result.failure(IllegalStateException("QA · solicitud simulada")) })
        } }
        compose.waitUntil(60_000) { ready.get() && compose.onAllNodesWithTag("business-ad-qa-business").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun lightHomePlacesDemoAdsAboveServicesAndOpensBusiness() {
        launch(false, AtomicReference())
        compose.onNodeWithTag("business-ad-qa-business").assertIsDisplayed()
        assertTrue(compose.onNodeWithTag("home-local-businesses").fetchSemanticsNode().boundsInRoot.top <
            compose.onNodeWithTag("home-start-delivery").fetchSemanticsNode().boundsInRoot.top)
        captureNativeScreenshot(compose, "business-home-light.png")
        compose.onNodeWithTag("business-ad-qa-business").performClick()
        compose.onNodeWithText("Recojo demo en Satipo").assertIsDisplayed()
        compose.onNodeWithTag("business-start-delivery").assertIsEnabled()
        pressBack()
        compose.onNodeWithTag("commercial-home").assertIsDisplayed()
        compose.onNodeWithTag("home-map").assertDoesNotExist()
    }

    @Test fun demoOrderUsesFixedBusinessPickupMotorcycleAndRecipientPaymentAndBackClearsDraft() {
        val requested = AtomicReference<RideBooking>()
        launch(true, requested)
        captureNativeScreenshot(compose, "business-home-dark.png")
        compose.onNodeWithTag("business-ad-qa-business").performClick()
        captureNativeScreenshot(compose, "business-details-dark.png")
        compose.onNodeWithTag("business-start-delivery").performClick()
        compose.onNodeWithText("Confirmar destino").performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("moto-option-delivery").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("moto-option-delivery").assertIsSelected()
        compose.onNodeWithTag("moto-option-any").assertDoesNotExist()
        compose.onNodeWithTag("moto-option-honda").assertDoesNotExist()
        compose.onNodeWithText("Continuar con pedido demo").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Datos del envío").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home-pickup-pin").assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("¿Qué enviarás?")).performTextInput("Paquete demo")
        compose.onNode(hasSetTextAction() and hasText("Nombre de quien recibe")).performTextInput("Persona QA")
        compose.onNode(hasSetTextAction() and hasText("Celular de quien recibe")).performTextInput("987654321")
        closeSoftKeyboard()
        compose.onNodeWithText(DeliveryPayer.RECIPIENT.label).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(DeliveryPayer.SENDER.label).assertDoesNotExist()
        compose.onNodeWithText("Confirmo que es un paquete pequeño", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Solicitar envío").assertIsEnabled().performClick()
        compose.waitUntil(20_000) { requested.get() != null }
        val booking = requested.get()
        assertEquals(ad.id, booking.businessAdId)
        assertEquals(ad.updatedAt, booking.businessAdUpdatedAt)
        assertEquals(ad.point, booking.origin)
        assertEquals(ad.point, booking.route.points.first())
        assertEquals("motorcycle", booking.rideType)
        assertEquals(DeliveryPayer.RECIPIENT, booking.delivery?.payer)
        assertEquals(4.2, booking.estimatedPrice, .001)
        assertNull(booking.preferredVehicleBrand)
        pressBack()
        compose.onNodeWithTag("home-start-delivery").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar destino").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("moto-option-any").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Elegir recojo").performClick()
        compose.onNodeWithTag("home-pickup-pin").assertIsDisplayed()
    }

    @Test fun adminSwitchUsesSavedValueAndCourierControlsOnlyConfirmedState() {
        val courier = BusinessTestCourier("qa-courier", "Repartidor QA", "QABIZ01", false)
        val state = mutableStateOf(AdminBusinessState(false, listOf(ad), listOf(courier)))
        val busy = mutableStateOf(false)
        var selected: String? = null
        var enabled: Boolean? = null
        compose.setContent { IntuTheme(darkTheme = true) { AdminPanelTheme {
            AdminPanelLayout(PaddingValues(), 6, true, null, {}, {}, {}, {}) {
                AdminBusinessesContent(state.value, busy.value, null, {}, { enabled = it }, { selected = it.id }, {}, {}, {})
            }
        } } }
        compose.onNodeWithTag("admin-business-enabled").assertIsOff().performClick()
        compose.runOnIdle { assertEquals(true, enabled); busy.value = true }
        compose.onNodeWithTag("admin-business-enabled").assertIsOff().assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(enabled = true); busy.value = false }
        compose.onNodeWithTag("admin-business-enabled").assertIsOn()
        compose.onNodeWithTag("business-courier-qa-courier").performScrollTo().assertIsOff().performClick()
        compose.runOnIdle { assertEquals(courier.id, selected) }
        compose.onNodeWithTag("admin-business-enabled").performScrollTo()
        captureNativeScreenshot(compose, "business-admin-dark.png")
    }

    @Test fun adminEditorValidatesAndProducesPublishedAdWithPickup() {
        val saved = AtomicReference<BusinessAd>()
        compose.setContent { IntuTheme(darkTheme = false) { BusinessAdEditor(ad, false, null, {}, saved::set) } }
        compose.onNode(hasSetTextAction() and hasText("URL HTTPS de imagen (opcional)")).performScrollTo().performTextInput("http://example.com/image.png")
        closeSoftKeyboard()
        compose.onNodeWithText("Guardar anuncio").performClick()
        assertNull(saved.get())
        compose.onNodeWithText("La imagen debe usar una dirección HTTPS válida.").performScrollTo().assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("URL HTTPS de imagen (opcional)")).performScrollTo().performTextClearance()
        closeSoftKeyboard()
        compose.onNodeWithText("Guardar anuncio").performClick()
        compose.runOnIdle { assertEquals(ad.point, saved.get().point); assertTrue(saved.get().published); assertEquals(ad.name, saved.get().name) }
    }

    @Test fun editorMapPreservesCopyAndSavesTheSelectedBusinessPickup() {
        val saved = AtomicReference<BusinessAd>()
        val map = AtomicReference<MapView>()
        compose.setContent { IntuTheme(darkTheme = true) { BusinessAdEditor(ad, false, null, {}, saved::set) } }
        compose.onNode(hasSetTextAction() and hasText("Título del anuncio")).performTextReplacement("Anuncio editado QA")
        closeSoftKeyboard()
        compose.onNodeWithText("Elegir recojo en mapa").performScrollTo().performClick()
        compose.onNodeWithText("Recojo del negocio demo").assertIsDisplayed()
        compose.onNodeWithText("Editar negocio").assertDoesNotExist()
        compose.waitUntil(60_000) { compose.onAllNodesWithTag("place-map-ready").fetchSemanticsNodes().isNotEmpty() }
        onView(isAssignableFrom(MapView::class.java)).check { view, noMatch ->
            if (noMatch != null) throw noMatch
            map.set(view as MapView)
        }
        NativeMapGestures(map.get()).pan()
        android.os.SystemClock.sleep(1000)
        val picked = compose.runOnIdle { map.get().pointUnderCenterPin()!! }
        assertTrue(TripMap.metersBetween(ad.point, picked) > 1.0)
        compose.onNodeWithText("Confirmar punto").performClick()
        compose.onNodeWithText("Guardar anuncio").performClick()
        compose.runOnIdle { assertEquals(picked, saved.get().point); assertEquals("Anuncio editado QA", saved.get().title) }
    }
}
