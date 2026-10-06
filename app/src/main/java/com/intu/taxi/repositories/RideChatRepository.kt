package com.intu.taxi.repositories

import com.intu.taxi.data.PostgresChangeFilter
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.SupabaseRealtime
import com.intu.taxi.data.str
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime

data class RideMessage(val id: String, val senderId: String, val body: String, val createdAt: Long)

/** Respuestas rápidas: el servidor guarda su texto fijo. El conductor en camino solo usa estas. */
object QuickReplies {
    val driver = listOf(
        "on_my_way" to "Voy en camino", "few_minutes" to "Llego en unos minutos", "traffic" to "Hay tráfico",
        "arrived" to "Ya llegué", "where_are_you" to "¿Dónde estás?", "ok" to "Entendido"
    )
    val rider = listOf(
        "coming_out" to "Ya salgo", "waiting" to "Te espero", "where_are_you" to "¿Dónde estás?",
        "two_minutes" to "Voy 2 min tarde", "ok" to "Entendido"
    )
}

class RideChatRepository {
    /**
     * Mensajes del viaje. Uno nuevo llega al instante por Supabase Realtime (la RLS solo deja ver
     * la conversación a sus dos participantes); además se consulta cada 10 s como respaldo, o cada
     * 3 s si Realtime no está conectado.
     */
    fun messages(rideId: String): Flow<List<RideMessage>> = channelFlow {
        var realtimeReady = false
        val refresh = Channel<Unit>(Channel.CONFLATED)
        launch {
            SupabaseRealtime.changes("ride-chat-$rideId",
                listOf(PostgresChangeFilter("ride_messages", "ride_id=eq.$rideId", "INSERT")))
                .collect { event ->
                    when (event.type) {
                        "joined" -> realtimeReady = true
                        "error" -> realtimeReady = false
                        "change" -> refresh.trySend(Unit)
                    }
                }
        }
        while (currentCoroutineContext().isActive) {
            runCatching {
                val rows = SupabaseApi.rows("ride_messages?ride_id=eq.${SupabaseApi.encode(rideId)}" +
                    "&select=id,sender_id,body,created_at&order=created_at.asc&limit=300")
                (0 until rows.length()).map { rows.getJSONObject(it).toMessage() }
            }.onSuccess { send(it) }
            withTimeoutOrNull(if (realtimeReady) 10_000L else 3_000L) { refresh.receive() }
        }
    }

    suspend fun send(rideId: String, text: String): RideMessage =
        SupabaseApi.rpc("send_ride_message", JSONObject().put("p_ride_id", rideId).put("p_body", text)).toMessage()

    suspend fun sendQuickReply(rideId: String, code: String): RideMessage =
        SupabaseApi.rpc("send_ride_message", JSONObject().put("p_ride_id", rideId).put("p_quick_reply", code)).toMessage()

    private fun JSONObject.toMessage() = RideMessage(
        id = str("id"), senderId = str("sender_id"), body = str("body"),
        createdAt = str("created_at").let { value ->
            runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrDefault(System.currentTimeMillis())
        }
    )
}
