package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import org.json.JSONObject

/** Comercio registrado desde el formulario web de «Descubre Satipo». */
data class GuideBusiness(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val address: String,
    val locality: String,
    val latitude: Double?,
    val longitude: Double?,
    val contactName: String,
    val phone: String,
    val showPhone: Boolean,
    val email: String,
    val socialUrl: String,
    val interest: String,
    val status: String,
    val featured: Boolean,
    val adminNote: String,
    val createdAt: String,
) {
    val hasPoint: Boolean get() = latitude != null && longitude != null

    companion object {
        val CATEGORY_LABELS = linkedMapOf("hospedaje" to "Hospedaje", "restaurante" to "Comida", "turismo" to "Turismo",
            "tienda" to "Tienda", "servicios" to "Servicios", "otro" to "Otro")
        val INTEREST_LABELS = mapOf("destacado" to "Quiere destacado (pago)", "gratis" to "Listado básico", "info" to "Solo información")

        fun fromJson(row: JSONObject): GuideBusiness {
            fun number(key: String): Double? = if (row.isNull(key)) null else row.optDouble(key).takeIf { it.isFinite() }
            return GuideBusiness(
                id = row.str("id"), name = row.str("name"), category = row.str("category"),
                description = row.str("description"), address = row.str("address"), locality = row.str("locality"),
                latitude = number("latitude"), longitude = number("longitude"), contactName = row.str("contact_name"),
                phone = row.str("phone"), showPhone = row.optBoolean("show_phone"), email = row.str("email"),
                socialUrl = row.str("social_url"), interest = row.str("interest"), status = row.str("status"),
                featured = row.optBoolean("featured"), adminNote = row.str("admin_note"), createdAt = row.str("created_at"),
            )
        }

        /**
         * Acepta «-11.2541, -74.6382» (lo que copia Google Maps al mantener presionado un punto).
         * Devuelve null si no son dos números dentro del Perú.
         */
        fun parsePoint(text: String): Pair<Double, Double>? {
            val parts = text.trim().removePrefix("(").removeSuffix(")").split(Regex("[,;\\s]+")).filter { it.isNotBlank() }
            if (parts.size != 2) return null
            val lat = parts[0].toDoubleOrNull() ?: return null
            val lng = parts[1].toDoubleOrNull() ?: return null
            if (!lat.isFinite() || !lng.isFinite() || lat !in -18.5..0.1 || lng !in -81.5..-68.5) return null
            return lat to lng
        }

        /** Número para wa.me: los celulares peruanos de 9 dígitos llevan el prefijo 51. */
        fun whatsappNumber(phone: String): String {
            val digits = phone.removePrefix("+")
            return if (digits.length == 9 && digits.startsWith("9")) "51$digits" else digits
        }
    }
}

class GuideBusinessRepository {
    suspend fun list(): List<GuideBusiness> {
        val rows = SupabaseApi.rpc("admin_guide_businesses").optJSONArray("businesses") ?: return emptyList()
        return (0 until rows.length()).map { GuideBusiness.fromJson(rows.getJSONObject(it)) }
    }

    /** Cambio parcial: solo se envían las claves presentes en [patch]. */
    suspend fun update(id: String, patch: JSONObject) {
        SupabaseApi.rpc("admin_update_guide_business", JSONObject().put("p_id", id).put("p_patch", patch))
    }

    suspend fun delete(id: String) {
        SupabaseApi.rpc("admin_delete_guide_business", JSONObject().put("p_id", id))
    }
}
