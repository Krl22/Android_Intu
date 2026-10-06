package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import org.json.JSONArray
import org.json.JSONObject

/** Turno de la conversación con el asistente; solo vive en la memoria del teléfono. */
data class SupportTurn(val fromUser: Boolean, val text: String)

data class SupportReply(val text: String, val remainingToday: Int)

data class SupportChatAdmin(
    val enabled: Boolean,
    val dailyLimitPerUser: Int,
    val knowledge: String,
    val questions30d: Int,
    val people30d: Int,
    val failed30d: Int,
    val estimatedCostUsd30d: Double
)

/** Precios de Claude Haiku 4.5 por millón de tokens (US$), para la estimación del admin. */
internal fun haikuCostUsd(input: Long, output: Long, cacheRead: Long, cacheWrite: Long): Double =
    (input * 1.0 + output * 5.0 + cacheRead * 0.10 + cacheWrite * 1.25) / 1_000_000

class SupportChatRepository {
    /** El asistente está activo para usuarios si un admin lo prendió. */
    suspend fun isEnabled(): Boolean = SupabaseApi.rpc("support_chat_status").optBoolean("enabled", false)

    suspend fun ask(conversation: List<SupportTurn>): SupportReply {
        val messages = JSONArray()
        conversation.forEach { messages.put(JSONObject().put("role", if (it.fromUser) "user" else "assistant").put("content", it.text)) }
        val json = SupabaseApi.invokeFunction("support-chat", JSONObject().put("messages", messages))
        return SupportReply(json.str("reply"), json.optInt("remaining_today", 0))
    }

    suspend fun adminGet(): SupportChatAdmin = parseAdmin(SupabaseApi.rpc("admin_get_support_chat"))

    suspend fun adminSave(changes: JSONObject): SupportChatAdmin =
        parseAdmin(SupabaseApi.rpc("admin_set_support_chat", JSONObject().put("p_settings", changes)))

    private fun parseAdmin(json: JSONObject): SupportChatAdmin {
        val usage = json.optJSONObject("usage_30d") ?: JSONObject()
        return SupportChatAdmin(
            enabled = json.optBoolean("enabled"),
            dailyLimitPerUser = json.optInt("daily_limit_per_user", 20),
            knowledge = json.str("knowledge"),
            questions30d = usage.optInt("questions"),
            people30d = usage.optInt("people"),
            failed30d = usage.optInt("failed"),
            estimatedCostUsd30d = haikuCostUsd(usage.optLong("input_tokens"), usage.optLong("output_tokens"),
                usage.optLong("cache_read_tokens"), usage.optLong("cache_write_tokens"))
        )
    }
}
