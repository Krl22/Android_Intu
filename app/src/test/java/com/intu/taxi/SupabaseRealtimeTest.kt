package com.intu.taxi

import com.intu.taxi.data.PostgresChangeFilter
import com.intu.taxi.data.SupabaseRealtime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueba de integración contra el Supabase real: conecta por WebSocket, se suscribe a un canal
 * y espera la respuesta del servidor. Sin token (anónimo) no recibe filas, pero valida el protocolo.
 */
class SupabaseRealtimeTest {
    @Test
    fun serverRepliesToChannelJoin() = runBlocking {
        SupabaseRealtime.accessTokenProvider = { null }
        val reply = withTimeout(20_000) {
            SupabaseRealtime.changes("prueba", listOf(PostgresChangeFilter("rides"))).first()
        }
        println("Respuesta de Supabase Realtime: $reply")
        assertTrue(
            "No hubo respuesta del servidor: $reply",
            reply.type == "joined" || (reply.type == "error" && reply.message != "desconectado")
        )
    }

    /** Los mismos filtros que usa la app para seguir un viaje y la ubicación de su conductor. */
    @Test
    fun serverAcceptsRideAndDriverLocationFilters() = runBlocking {
        SupabaseRealtime.accessTokenProvider = { null }
        val reply = withTimeout(20_000) {
            SupabaseRealtime.changes(
                "prueba-filtros",
                listOf(
                    PostgresChangeFilter("rides", "id=eq.00000000-0000-0000-0000-000000000000"),
                    PostgresChangeFilter("driver_locations", "driver_id=eq.prueba")
                )
            ).first()
        }
        println("Respuesta con filtros: $reply")
        assertTrue("El servidor rechazó los filtros: $reply", reply.type == "joined")
    }
}
