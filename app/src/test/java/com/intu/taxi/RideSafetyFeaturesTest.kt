package com.intu.taxi

import com.intu.taxi.data.CancelReasons
import com.intu.taxi.data.CancelRole
import com.intu.taxi.data.CancellationPreview
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.formatBlockedUntil
import com.intu.taxi.data.formatMinutes
import com.intu.taxi.data.formatWait
import com.intu.taxi.models.ParticipantRating
import com.intu.taxi.ui.screens.canTypeInRideChat
import com.intu.taxi.ui.screens.cancelReasonLabel
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RideSafetyFeaturesTest {
    private fun preview(outcome: String, counts: Boolean = false, enabled: Boolean = true, strikes: Int = 0,
                        wouldBlock: Boolean = false, wait: Int = 0) =
        CancellationPreview(outcome, counts, enabled, strikes, 3, 7, wouldBlock, 30, wait)

    @Test fun ratingIsHiddenUntilTheServerSendsItAndNewUsersShowNuevo() {
        assertNull(ParticipantRating.from(JSONObject("""{"driver_rating":null,"driver_rating_count":null}"""), "driver"))
        assertNull(ParticipantRating.from(JSONObject("{}"), "rider"))
        val fresh = ParticipantRating.from(JSONObject("""{"rider_rating":null,"rider_rating_count":0}"""), "rider")!!
        assertEquals("Nuevo", fresh.label)
        assertEquals("Nuevo", ParticipantRating(5.0, 2).label)
        assertEquals("4.8", ParticipantRating.from(JSONObject("""{"driver_rating":4.75,"driver_rating_count":12}"""), "driver")!!.label)
    }

    @Test fun previewExplainsWhetherTheCancellationCounts() {
        assertEquals("Esta cancelación no cuenta en tu contra.", preview("free").message(CancelRole.RIDER))
        assertEquals("Esta cancelación cuenta (2 de 3 en 7 días).", preview("counted", counts = true, strikes = 1).message(CancelRole.RIDER))
        assertTrue(preview("counted", counts = true, strikes = 2, wouldBlock = true).message(CancelRole.DRIVER)
            .contains("no podrás recibir solicitudes por 30 minutos"))
        assertTrue(preview("excused").message(CancelRole.RIDER).startsWith("Esta vez no cuenta"))
        // Sin la protección activada no se promete ni se amenaza con faltas
        assertEquals("", preview("counted", counts = false, enabled = false).message(CancelRole.RIDER))
        assertEquals("", preview("free", enabled = false).message(CancelRole.RIDER))
    }

    @Test fun noShowNeedsArrivalWaitAndProximity() {
        assertTrue(preview("no_show_unavailable").blocked)
        assertTrue(preview("no_show_wait", wait = 75).blocked)
        assertTrue(preview("no_show_far").blocked)
        assertFalse(preview("no_show").blocked)
        assertFalse(preview("counted").blocked)
        assertTrue(preview("no_show_wait", wait = 75, enabled = false).message(CancelRole.DRIVER).contains("1:15"))
        assertEquals("Primero marca que llegaste al punto de recojo.",
            SupabaseApi.spanishError(400, "P0001", "no_show_not_allowed", "no_show_unavailable:0"))
        assertEquals("Espera 4:05 más en el punto de recojo.",
            SupabaseApi.spanishError(400, "P0001", "no_show_not_allowed", "no_show_wait:245"))
    }

    @Test fun reasonsDependOnRoleAndStage() {
        val searching = CancelReasons.forRider("searching", delivery = false).map { it.code }
        assertFalse("driver_asked" in searching)
        assertEquals("other", searching.last())
        val accepted = CancelReasons.forRider("accepted", delivery = true)
        assertTrue(accepted.any { it.code == "driver_late" && it.label.contains("repartidor") })
        assertEquals(CancelReasons.RIDER_NO_SHOW, CancelReasons.forDriver(false).first().code)
        assertEquals("No apareció en el recojo", cancelReasonLabel("no_show"))
        assertEquals("Otro motivo", cancelReasonLabel("cancelled_by_rider"))
    }

    @Test fun pauseDurationsAndTimesReadNaturally() {
        assertEquals("30 minutos", formatMinutes(30))
        assertEquals("1 hora", formatMinutes(60))
        assertEquals("24 horas", formatMinutes(1440))
        assertEquals("3 días", formatMinutes(4320))
        assertEquals("0:09", formatWait(9))
        val lima = ZoneId.of("America/Lima")
        assertEquals("hasta las 10:30", formatBlockedUntil("2026-10-05T15:30:00Z", lima, LocalDate.of(2026, 10, 5)))
        assertEquals("hasta el 06/10 a las 10:30", formatBlockedUntil("2026-10-06T15:30:00.5+00:00", lima, LocalDate.of(2026, 10, 5)))
        assertNull(formatBlockedUntil("mañana", lima))
        assertEquals("Por cancelar varias veces, tu cuenta está en pausa por un tiempo.",
            SupabaseApi.spanishError(400, "P0001", "cancellation_block", ""))
    }

    @Test fun supportAssistantCostUsesHaikuPrices() {
        // 1M de entrada (US$1) + 200k de salida (US$1) + 1M leídos de caché (US$0.10) + 100k escritos (US$0.125)
        assertEquals(2.225, com.intu.taxi.repositories.haikuCostUsd(1_000_000, 200_000, 1_000_000, 100_000), 1e-9)
        assertEquals("Llegaste al límite de preguntas de hoy. Vuelve mañana o usa Reportar un error.",
            SupabaseApi.spanishError(429, "", "support_chat_limit"))
    }

    @Test fun driverTypesOnlyWhenStoppedAtPickup() {
        assertFalse(canTypeInRideChat(CancelRole.DRIVER, "accepted"))
        assertTrue(canTypeInRideChat(CancelRole.DRIVER, "arrived"))
        assertFalse(canTypeInRideChat(CancelRole.DRIVER, "in_progress"))
        assertTrue(canTypeInRideChat(CancelRole.RIDER, "accepted"))
        assertTrue(canTypeInRideChat(CancelRole.RIDER, "in_progress"))
        assertFalse(canTypeInRideChat(CancelRole.RIDER, "completed"))
    }
}
