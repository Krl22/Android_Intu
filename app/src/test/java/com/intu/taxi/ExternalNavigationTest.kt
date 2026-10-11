package com.intu.taxi

import com.intu.taxi.driver.ExternalNavigation
import com.intu.taxi.driver.NavigationApp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class ExternalNavigationTest {
    private lateinit var previous: Locale

    // Teléfono en español de Perú: el formato normal usaría coma decimal y el mapa no lo entendería
    @Before fun spanishPhone() { previous = Locale.getDefault(); Locale.setDefault(Locale.forLanguageTag("es-PE")) }
    @After fun restore() = Locale.setDefault(previous)

    @Test
    fun googleMapsStartsTurnByTurnNavigation() {
        assertEquals("google.navigation:q=-8.379100,-74.553900&mode=d",
            ExternalNavigation.url(NavigationApp.GOOGLE_MAPS, -8.3791, -74.5539))
    }

    @Test
    fun wazeStartsNavigation() {
        assertEquals("https://waze.com/ul?ll=-8.379100,-74.553900&navigate=yes",
            ExternalNavigation.url(NavigationApp.WAZE, -8.3791, -74.5539))
    }

    @Test
    fun browserFallbackUsesGoogleMapsDirections() {
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=-8.379100,-74.553900&travelmode=driving",
            ExternalNavigation.webUrl(-8.3791, -74.5539))
    }
}
