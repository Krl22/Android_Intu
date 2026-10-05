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
    private val product = BusinessMenuItem(name = "1/4 de pollo + papas", description = "Papas, ensalada y ají", price = 18.90, demoPhoto = BusinessPhoto.CHICKEN)
    private val ad = BusinessAd("qa-business", "Brasa Satipo · Demo", BusinessCategory.FOOD, "1/4 de pollo + papas",
        "Un negocio ficticio para probar un envío.", address = "Recojo demo en Satipo", latitude = -11.254, longitude = -74.640,
        published = true, updatedAt = "2026-10-05T05:00:00Z", city = "Satipo", offerDetail = "Papas, ensalada y ají", offerPrice = 18.90,
        demoPhoto = BusinessPhoto.CHICKEN, menu = listOf(product,
            BusinessMenuItem(name = "Chaufa de pollo", price = 15.90, demoPhoto = BusinessPhoto.CHAUFA)))

    private fun launch(dark: Boolean, requested: AtomicReference<RideBooking>) {
        val ready = AtomicBoolean()
        val gps = object : LocationProvider {
            override fun registerLocationConsumer(consumer: LocationConsumer) {
                consumer.onLocationUpdated(Point.fromLngLat(-74.6382, -11.2521)); ready.set(true)
            }
            override fun unRegisterLocationConsumer(consumer: LocationConsumer) = Unit
        }
        compose.setContent { IntuTheme(darkTheme = dark) {
            HomeScreen(PaddingValues(), locationProvider = gps, businessFeedLoader = { BusinessFeed(true, listOf(ad,
                ad.copy(id = "qa-rio-negro", name = "Sazón Río Negro · Demo", title = "Juane tradicional", city = "Río Negro",
                    offerDetail = "Pollo, arroz y sabor de la selva", offerPrice = 14.90, demoPhoto = BusinessPhoto.JUANE),
                ad.copy(id = "qa-cafe", name = "Café Satipo · Demo", title = "Café + sánguche", offerDetail = "Tu pausa de media mañana",
                    offerPrice = 9.90, demoPhoto = BusinessPhoto.COFFEE))) },
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
        compose.onNodeWithTag("business-start-delivery").assertIsNotEnabled()
        compose.onNodeWithTag("business-product-plus-${product.id}").performScrollTo().performClick()
        compose.onNodeWithText("Recojo demo en Satipo").performScrollTo().assertIsDisplayed()
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
        compose.onNodeWithTag("business-product-plus-${product.id}").performScrollTo().performClick()
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
        assertEquals(product.id, booking.delivery?.businessItems?.single()?.itemId)
        assertEquals(18.90, booking.delivery!!.businessItems.productsTotal(), .001)
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

    @Test fun promotionPagerSurvivesFeedRefreshAndOpensTheVisibleBusiness() {
        val second = ad.copy(id = "qa-rio-negro", name = "Sazón Río Negro · Demo", title = "Juane tradicional", city = "Río Negro",
            offerPrice = 14.90, demoPhoto = BusinessPhoto.JUANE)
        val feed = mutableStateOf(BusinessFeed(true, listOf(ad, second)))
        val selected = AtomicReference<BusinessAd>()
        compose.setContent { IntuTheme(darkTheme = true) { BusinessAdsSection(feed.value, null, {}, selected::set) } }
        compose.onNodeWithContentDescription("Promoción 1 de 2").assertExists()
        compose.onNodeWithTag("business-promotions-pager").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("Promoción 2 de 2").assertExists()
        compose.runOnIdle { feed.value = feed.value.copy(ads = listOf(ad.copy(description = "Actualizado QA"), second)) }
        compose.onNodeWithContentDescription("Promoción 2 de 2").assertExists()
        compose.onNodeWithTag("business-ad-qa-rio-negro").performClick()
        compose.runOnIdle { assertEquals(second.id, selected.get().id) }
        captureNativeScreenshot(compose, "business-rio-negro-dark.png")
    }

    @Test fun menuTotalsFollowQuantitiesAndCannotContinueAfterRemovingAllItems() {
        val selected = AtomicReference<List<BusinessOrderItem>>()
        compose.setContent { IntuTheme(darkTheme = false) { BusinessAdDialog(ad, {}, selected::set) } }
        compose.onNodeWithTag("business-start-delivery").assertIsNotEnabled()
        repeat(2) { compose.onNodeWithTag("business-product-plus-${product.id}").performScrollTo().performClick() }
        val second = ad.menu[1]
        compose.onNodeWithTag("business-product-plus-${second.id}").performScrollTo().performClick()
        compose.onNodeWithTag("business-products-total").performScrollTo().assertTextEquals("Productos simulados: S/ 53.70")
        compose.onNodeWithTag("business-start-delivery").performClick()
        compose.runOnIdle { assertEquals(53.70, selected.get().productsTotal(), .001); assertEquals(3, selected.get().sumOf { it.quantity }) }
        compose.onNodeWithTag("business-product-minus-${second.id}").performScrollTo().performClick()
        repeat(2) { compose.onNodeWithTag("business-product-minus-${product.id}").performScrollTo().performClick() }
        compose.onNodeWithTag("business-start-delivery").assertIsNotEnabled()
        compose.onNodeWithTag("business-products-total").assertDoesNotExist()
        captureNativeScreenshot(compose, "business-menu-light.png")
    }

    @Test fun adminMenuEditsAreRetainedUntilSavingTheAnnouncement() {
        val saved = AtomicReference<BusinessAd>()
        compose.setContent { IntuTheme(darkTheme = true) { BusinessAdEditor(ad, false, null, {}, saved::set) } }
        compose.onNodeWithTag("admin-edit-business-menu").performScrollTo().performClick()
        compose.onAllNodesWithText("Editar producto")[0].performClick()
        compose.onNode(hasSetTextAction() and hasText("Precio del producto")).performTextReplacement("20.901")
        closeSoftKeyboard()
        compose.onNodeWithText("Guardar producto").performClick()
        compose.onNodeWithText("El precio debe ser de S/ 0.10 a S/ 999.99, con hasta dos decimales.").performScrollTo().assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("Precio del producto")).performScrollTo().performTextReplacement("20.90")
        closeSoftKeyboard()
        compose.onNodeWithText("Guardar producto").performClick()
        compose.onNodeWithText("Usar este menú").performClick()
        compose.onNodeWithText("Guardar anuncio").performClick()
        compose.runOnIdle { assertEquals(20.90, saved.get().menu.first().price, .001); assertEquals(product.id, saved.get().menu.first().id) }
    }

    @Test fun largeTextMenuKeepsQuantityControlsAndContinueAccessible() {
        // Run this case with the emulator's system font_scale=1.4; Dialog has its own density.
        compose.setContent { IntuTheme(darkTheme = true) { BusinessAdDialog(ad, {}, {}) } }
        compose.onNodeWithTag("business-product-plus-${product.id}").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("business-start-delivery").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("business-products-total").performScrollTo().assertIsDisplayed()
        captureNativeScreenshot(compose, "business-menu-large-text.png")
    }

    @Test fun completedBusinessOrderKeepsProductsSeparateFromTransportInHistory() {
        val ride = com.intu.taxi.repositories.RideHistoryItem("qa-completed", "completed", null, ad.address, "Destino QA", 4.20,
            "efectivo", "Repartidor QA", "", "Moto", "QAMENU1", "Persona QA", "", null, null,
            serviceKind = "delivery", delivery = DeliveryDetails("Persona QA", "+51987654321", "Pedido demo", businessName = ad.name,
                businessItems = businessCart(ad.menu, mapOf(product.id to 2))))
        compose.setContent { IntuTheme(darkTheme = true) { RideDetailsDialog(ride, mutableMapOf(), {}) } }
        compose.onNodeWithTag("business-products-total").assertTextEquals("Productos simulados: S/ 37.80").assertIsDisplayed()
        compose.onNodeWithText("Tarifa del transporte").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(com.intu.taxi.ui.formatSoles(4.20)).assertIsDisplayed()
        captureNativeScreenshot(compose, "business-history-dark.png")
    }
}
