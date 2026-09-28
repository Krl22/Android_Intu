package com.intu.taxi.data

import com.intu.taxi.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Tabla (y filtro opcional, p. ej. "id=eq.123") de la que se quieren recibir cambios. */
data class PostgresChangeFilter(val table: String, val filter: String? = null, val event: String = "*")

/** Evento de un canal: "joined" al suscribirse, "change" con la fila nueva, "error" si el servidor la rechazó. */
data class RealtimeEvent(val type: String, val table: String? = null, val record: JSONObject? = null, val message: String? = null)

/**
 * Cliente mínimo de Supabase Realtime (protocolo Phoenix sobre WebSocket con OkHttp).
 * Una sola conexión para toda la app; cada suscripción es un canal con sus filtros.
 * Se reconecta solo y renueva el token de Firebase, que vence cada hora.
 */
object SupabaseRealtime {
    /** Token con el que Supabase aplica RLS a los cambios. En pruebas puede ser null (anónimo). */
    var accessTokenProvider: suspend () -> String? = {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.let { user ->
            user.getIdToken(false).await().token
        }
    }

    private val http = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ref = AtomicInteger(0)
    private val channels = ConcurrentHashMap<String, Channel>()

    @Volatile private var socket: WebSocket? = null
    @Volatile private var connected = false
    private var heartbeatJob: Job? = null
    private var tokenJob: Job? = null
    private var reconnectAttempts = 0

    private class Channel(
        val topic: String,
        val changes: List<PostgresChangeFilter>,
        val onEvent: (RealtimeEvent) -> Unit
    ) {
        @Volatile var joinRef: String? = null
    }

    /**
     * Cambios de las tablas indicadas mientras se recolecta el Flow. Al cancelarlo sale del canal.
     * Emite "joined" cuando el servidor acepta la suscripción; desde ahí llegan los "change".
     */
    fun changes(name: String, filters: List<PostgresChangeFilter>): Flow<RealtimeEvent> = callbackFlow {
        val topic = "realtime:$name-${UUID.randomUUID().toString().take(8)}"
        val channel = Channel(topic, filters) { trySend(it) }
        channels[topic] = channel
        ensureConnected()
        if (connected) scope.launch { join(channel) }
        awaitClose {
            channels.remove(topic)
            if (connected) push(topic, "phx_leave", JSONObject(), nextRef())
            if (channels.isEmpty()) scope.launch {
                // Se cierra si en un rato no se suscribe nadie más
                delay(10_000)
                if (channels.isEmpty()) disconnect()
            }
        }
    }

    @Synchronized
    private fun ensureConnected() {
        if (socket != null) return
        val url = BuildConfig.SUPABASE_URL.replaceFirst("https://", "wss://") +
            "/realtime/v1/websocket?apikey=${BuildConfig.SUPABASE_PUBLISHABLE_KEY}&vsn=1.0.0"
        socket = http.newWebSocket(Request.Builder().url(url).build(), listener)
    }

    @Synchronized
    private fun disconnect() {
        heartbeatJob?.cancel()
        tokenJob?.cancel()
        socket?.close(1000, null)
        socket = null
        connected = false
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            connected = true
            reconnectAttempts = 0
            channels.values.forEach { channel -> scope.launch { join(channel) } }
            heartbeatJob?.cancel()
            heartbeatJob = scope.launch {
                while (isActive) {
                    delay(25_000)
                    push("phoenix", "heartbeat", JSONObject(), nextRef())
                }
            }
            tokenJob?.cancel()
            tokenJob = scope.launch {
                while (isActive) {
                    delay(30 * 60_000L)
                    val token = runCatching { accessTokenProvider() }.getOrNull() ?: continue
                    channels.values.forEach { push(it.topic, "access_token", JSONObject().put("access_token", token), nextRef()) }
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val message = runCatching { JSONObject(text) }.getOrNull() ?: return
            val channel = channels[message.optString("topic")] ?: return
            val payload = message.optJSONObject("payload") ?: JSONObject()
            when (message.optString("event")) {
                "phx_reply" -> if (message.optString("ref") == channel.joinRef) {
                    if (payload.optString("status") == "ok") {
                        channel.onEvent(RealtimeEvent("joined"))
                    } else {
                        channel.onEvent(RealtimeEvent("error", message = payload.optJSONObject("response")?.toString()))
                    }
                }
                "postgres_changes" -> {
                    val data = payload.optJSONObject("data") ?: return
                    val record = data.optJSONObject("record") ?: data.optJSONObject("new")
                    channel.onEvent(RealtimeEvent("change", table = data.optString("table"), record = record))
                }
                "phx_error", "phx_close" -> channel.onEvent(RealtimeEvent("error", message = message.optString("event")))
                "system" -> if (payload.optString("status") == "error") {
                    channel.onEvent(RealtimeEvent("error", message = payload.optString("message")))
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = onDisconnected()
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = onDisconnected()
    }

    private fun onDisconnected() {
        synchronized(this) {
            heartbeatJob?.cancel()
            tokenJob?.cancel()
            socket = null
            connected = false
        }
        channels.values.forEach { it.onEvent(RealtimeEvent("error", message = "desconectado")) }
        if (channels.isEmpty()) return
        // Reintenta con espera creciente (2, 4, 8… hasta 30 s)
        val wait = minOf(30_000L, 2_000L shl minOf(reconnectAttempts, 4))
        reconnectAttempts++
        scope.launch {
            delay(wait)
            if (channels.isNotEmpty()) ensureConnected()
        }
    }

    private suspend fun join(channel: Channel) {
        val token = runCatching { accessTokenProvider() }.getOrNull()
        val postgresChanges = JSONArray()
        channel.changes.forEach { change ->
            postgresChanges.put(
                JSONObject()
                    .put("event", change.event)
                    .put("schema", "public")
                    .put("table", change.table)
                    .apply { change.filter?.let { put("filter", it) } }
            )
        }
        val payload = JSONObject()
            .put(
                "config",
                JSONObject()
                    .put("broadcast", JSONObject().put("ack", false).put("self", false))
                    .put("presence", JSONObject().put("key", ""))
                    .put("postgres_changes", postgresChanges)
                    .put("private", false)
            )
            .apply { token?.let { put("access_token", it) } }
        val joinRef = nextRef()
        channel.joinRef = joinRef
        push(channel.topic, "phx_join", payload, joinRef, joinRef)
    }

    private fun push(topic: String, event: String, payload: JSONObject, ref: String, joinRef: String? = null) {
        val message = JSONObject()
            .put("topic", topic)
            .put("event", event)
            .put("payload", payload)
            .put("ref", ref)
            .apply { joinRef?.let { put("join_ref", it) } }
        socket?.send(message.toString())
    }

    private fun nextRef() = ref.incrementAndGet().toString()
}
