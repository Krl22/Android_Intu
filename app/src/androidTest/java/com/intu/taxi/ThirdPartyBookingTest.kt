package com.intu.taxi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import com.intu.taxi.models.*
import com.intu.taxi.ui.map.TripRoute
import com.intu.taxi.ui.screens.*
import com.intu.taxi.ui.theme.IntuTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/** Native planning and contact flow with injected routes; never creates a live order. */
class ThirdPartyBookingTest {
    @get:Rule val compose = createComposeRule()
    private val requested = AtomicReference<RideBooking>()
    private val consumer = AtomicReference<LocationConsumer>()
    private fun launch(dark: Boolean) {
        val gps = object : LocationProvider {
            override fun registerLocationConsumer(value: LocationConsumer) {
                consumer.set(value); value.onLocationUpdated(Point.fromLngLat(-74.6382, -11.2521))
            }
            override fun unRegisterLocationConsumer(value: LocationConsumer) = Unit
        }
        compose.setContent { IntuTheme(darkTheme = dark) {
            HomeScreen(PaddingValues(), locationProvider = gps, businessFeedLoader = { BusinessFeed() },
                routeLoader = { origin, target -> TripRoute(listOf(origin, target), 2100.0, 420.0) },
                rideRequestSender = { requested.set(it); Result.failure(IllegalStateException("QA · solicitud simulada")) })
        } }
        compose.waitUntil(60_000) { consumer.get() != null }
    }
    private fun person() {
        com.intu.taxi.data.RecentContacts.forCurrentUser(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext)
            .remember(com.intu.taxi.models.BookingContact("Ana QA", "987654321"))
        compose.onNodeWithTag("booking-person").performClick()
        compose.onNodeWithTag("booking-recent-0").performClick()
    }
    private fun chooseMap(label: String) {
        closeSoftKeyboard()
        compose.onNodeWithTag("home-pick-destination").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(label).assertIsEnabled().performClick()
    }
    @Test fun darkSearchOpensTwoFieldsAndKeepsMapBehindPlanningSheet() {
        launch(true)
        compose.onNodeWithTag("home-destination-search").performClick()
        compose.onNodeWithText("Planifica tu viaje").assertIsDisplayed()
        compose.onNodeWithTag("planning-pickup-search").assertTextContains("Mi ubicación")
        compose.onNodeWithTag("home-destination-search").assertIsFocused()
        compose.onNodeWithTag("home-map").assertExists()
        captureNativeScreenshot(compose, "trip-planning-dark.png")
        closeSoftKeyboard()
        compose.onNodeWithTag("home-pick-destination").performScrollTo().performClick()
        pressBack()
        compose.onNodeWithTag("trip-planning-panel").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver al inicio").performClick()
        compose.onNodeWithTag("commercial-home").assertIsDisplayed()
    }
    @Test fun guestMototaxiKeepsChosenPickupAfterGpsMovesAndSendsGuestContact() {
        launch(true)
        compose.onNodeWithTag("home-destination-search").performClick()
        person()
        chooseMap("Confirmar recojo")
        compose.runOnIdle { consumer.get().onLocationUpdated(Point.fromLngLat(-74.7, -11.3)) }
        captureNativeScreenshot(compose, "trip-planning-guest-dark.png")
        chooseMap("Confirmar destino")
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("moto-option-any").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Solicitar viaje").performClick()
        compose.waitUntil(20_000) { requested.get() != null }
        val booking = requested.get()
        assertEquals(BookingContact("Ana QA", "+51987654321"), booking.passenger)
        assertEquals("mototaxi", booking.rideType)
        assertNull(booking.delivery)
        assertEquals(booking.origin, booking.route.points.first())
        assertTrue("Pickup must remain near the contact's original selected point", kotlin.math.abs(booking.origin.longitude() + 74.6382) < .01)
    }
    @Test fun courierForAnotherPersonUsesSeparatePickupAndRecipientContacts() {
        launch(false)
        compose.onNodeWithTag("home-start-delivery").performScrollTo().performClick()
        person()
        chooseMap("Confirmar recojo")
        chooseMap("Confirmar destino")
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("moto-option-delivery").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Continuar con envío").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Datos del envío").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction() and hasText("¿Qué enviarás?")).performTextInput("Documentos QA")
        compose.onNode(hasSetTextAction() and hasText("Nombre de quien recibe")).performTextInput("Luis QA")
        compose.onNode(hasSetTextAction() and hasText("Celular de quien recibe")).performTextInput("988888888")
        closeSoftKeyboard()
        compose.onNodeWithText("Confirmo que es un paquete pequeño", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Solicitar envío").performClick()
        compose.waitUntil(20_000) { requested.get() != null }
        assertEquals(BookingContact("Ana QA", "+51987654321"), requested.get().delivery?.sender)
        assertEquals("Luis QA", requested.get().delivery?.recipientName)
        assertNull(requested.get().passenger)
        assertEquals("motorcycle", requested.get().rideType)
    }
    @Test fun lightPlanningAllowsPickupEditingBeforeDestinationAndBackClearsGuest() {
        launch(false)
        compose.onNodeWithTag("home-destination-search").performClick()
        captureNativeScreenshot(compose, "trip-planning-light.png")
        person()
        compose.onNodeWithTag("planning-pickup-search").assertIsFocused()
        compose.onNodeWithTag("planning-pickup-search").performTextInput("Recojo QA")
        compose.onNodeWithTag("home-destination-search").performClick().performTextInput("Destino QA")
        compose.onNodeWithTag("planning-pickup-search").assertTextContains("Recojo QA")
        closeSoftKeyboard(); pressBack()
        compose.onNodeWithTag("home-destination-search").performClick()
        compose.onNodeWithText("Para mí").assertIsDisplayed()
        compose.onNodeWithTag("planning-pickup-search").assertTextContains("Mi ubicación")
    }
    @Test fun suggestedAddressesStayFirstAndVisibleWhileProviderRefreshes() {
        val loading = mutableStateOf(false)
        val query = mutableStateOf("")
        val field = mutableStateOf(PlanningField.DESTINATION)
        val result = com.intu.taxi.data.PlaceSearchResult("qa-place", "Plaza Principal de Satipo", "Satipo",
            -11.253, -74.637, com.intu.taxi.data.PlaceSearchSource.CATALOG)
        compose.setContent { IntuTheme(darkTheme = true) {
            TripPlanningPanel("Mi ubicación", query.value, field.value, null, false, false,
                listOf(com.intu.taxi.data.SavedPlace("casa", "Casa", "Dirección QA", -11.25, -74.63)),
                listOf(result), false, null, loading.value, null, true, {}, {}, { field.value = it }, {},
                { query.value = it }, {}, {}, {}, {}, {})
        } }
        closeSoftKeyboard()
        compose.onNodeWithTag("destination-result-qa-place").assertIsDisplayed()
        assertTrue(compose.onNodeWithTag("destination-result-qa-place").fetchSemanticsNode().boundsInRoot.top <
            compose.onNodeWithTag("home-place-casa").fetchSemanticsNode().boundsInRoot.top)
        compose.runOnIdle { loading.value = true }
        compose.onNodeWithTag("destination-result-qa-place").assertIsDisplayed()
        compose.runOnIdle { loading.value = false }
        compose.onNodeWithTag("destination-result-qa-place").assertIsDisplayed()
        captureNativeScreenshot(compose, "trip-planning-suggestions-dark.png")
    }
    @Test fun systemPhonePickerReturnsOnlySelectedContactWithoutContactsPermission() {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val context = instrumentation.targetContext
        val resolver = context.contentResolver
        var rawId: Long? = null
        try {
            automation.adoptShellPermissionIdentity(android.Manifest.permission.WRITE_CONTACTS, android.Manifest.permission.READ_CONTACTS)
            val raw = resolver.insert(android.provider.ContactsContract.RawContacts.CONTENT_URI, android.content.ContentValues().apply {
                putNull("account_type"); putNull("account_name")
            })!!
            rawId = android.content.ContentUris.parseId(raw)
            resolver.insert(android.provider.ContactsContract.Data.CONTENT_URI, android.content.ContentValues().apply {
                put("raw_contact_id", rawId); put("mimetype", android.provider.ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put("data1", "Contacto QA Intu")
            })
            resolver.insert(android.provider.ContactsContract.Data.CONTENT_URI, android.content.ContentValues().apply {
                put("raw_contact_id", rawId); put("mimetype", android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                put("data1", "+51987654321"); put("data2", android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            })
            automation.dropShellPermissionIdentity()
            assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED,
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS))
            launch(true)
            compose.onNodeWithTag("home-destination-search").performClick()
            compose.onNodeWithTag("booking-person").performClick()
            compose.onNodeWithTag("booking-pick-contact").performClick()
            val until = android.os.SystemClock.uptimeMillis() + 15_000
            var selected = false
            fun findContact(node: android.view.accessibility.AccessibilityNodeInfo?): android.view.accessibility.AccessibilityNodeInfo? {
                if (node == null) return null
                if (node.text?.contains("Contacto QA Intu") == true || node.contentDescription?.contains("Contacto QA Intu") == true) return node
                for (i in 0 until node.childCount) findContact(node.getChild(i))?.let { return it }
                return null
            }
            while (!selected && android.os.SystemClock.uptimeMillis() < until) {
                val node = findContact(automation.rootInActiveWindow)
                if (node != null) {
                    var clickable: android.view.accessibility.AccessibilityNodeInfo? = node
                    while (clickable != null && !clickable.isClickable) clickable = clickable.parent
                    selected = clickable?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true
                    if (!selected) {
                        val bounds = android.graphics.Rect(); node.getBoundsInScreen(bounds)
                        val now = android.os.SystemClock.uptimeMillis()
                        val down = android.view.MotionEvent.obtain(now, now, android.view.MotionEvent.ACTION_DOWN, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                        val up = android.view.MotionEvent.obtain(now, now + 80, android.view.MotionEvent.ACTION_UP, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                        down.source = android.view.InputDevice.SOURCE_TOUCHSCREEN; up.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                        selected = automation.injectInputEvent(down, true) && automation.injectInputEvent(up, true)
                        down.recycle(); up.recycle()
                    }
                }
                if (!selected) android.os.SystemClock.sleep(200)
            }
            if (!selected) {
                val bitmap = automation.takeScreenshot()
                java.io.File(context.getExternalFilesDir(null), "contact-picker-debug.png").outputStream().use {
                    bitmap?.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                fun describe(node: android.view.accessibility.AccessibilityNodeInfo?): String = if (node == null) "no window" else
                    listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" ") +
                        (0 until node.childCount).joinToString(" | ") { describe(node.getChild(it)) }
                println("CONTACT PICKER: " + describe(automation.rootInActiveWindow))
            }
            assertTrue("System phone contact picker must show the fixture", selected)
            // Elegir de la agenda selecciona a la persona y la guarda en recientes
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Para Contacto").fetchSemanticsNodes().isNotEmpty() }
            val recent = com.intu.taxi.data.RecentContacts.forCurrentUser(context).read().first()
            assertEquals("Contacto QA Intu", recent.name)
            assertEquals("+51987654321", recent.phone)
        } finally {
            automation.adoptShellPermissionIdentity(android.Manifest.permission.WRITE_CONTACTS)
            rawId?.let { resolver.delete(android.content.ContentUris.withAppendedId(android.provider.ContactsContract.RawContacts.CONTENT_URI, it), null, null) }
            automation.dropShellPermissionIdentity()
        }
    }
}
