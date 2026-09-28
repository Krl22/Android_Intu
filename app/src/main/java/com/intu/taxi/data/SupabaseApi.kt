package com.intu.taxi.data

import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Como optString, pero devuelve [fallback] si la clave falta o es null en el JSON.
 * optString convierte JSONObject.NULL en el texto "null", que terminaba en pantalla.
 */
fun JSONObject.str(key: String, fallback: String = ""): String =
    if (isNull(key)) fallback else optString(key, fallback)

/** Minimal Supabase Data API client backed by the current Firebase ID token. */
object SupabaseApi {
    private val http = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun token(forceRefresh: Boolean = false): String {
        return FirebaseAuth.getInstance().currentUser
            ?.getIdToken(forceRefresh)?.await()?.token
            ?: throw IllegalStateException("Inicia sesión para continuar.")
    }

    suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        prefer: String? = null
    ): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/rest/v1/$path")
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Authorization", "Bearer ${token()}")
            .header("Accept", "application/json")
        if (prefer != null) builder.header("Prefer", prefer)

        val requestBody = (body?.toString() ?: "{}").toRequestBody(jsonMediaType)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody)
            "PATCH" -> builder.patch(requestBody)
            "DELETE" -> builder.delete()
            else -> error("Método HTTP no soportado: $method")
        }

        val call = http.newCall(builder.build())
        val response = try {
            call.execute()
        } catch (e: java.io.IOException) {
            throw IllegalStateException("Sin conexión. Revisa tu internet e intenta de nuevo.", e)
        }
        response.use {
            val text = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                val json = runCatching { JSONObject(text) }.getOrNull()
                throw IllegalStateException(
                    spanishError(it.code, json?.str("code").orEmpty(), json?.str("message").orEmpty())
                )
            }
            text
        }
    }

    /** Traduce los errores de Supabase y de las funciones RPC a mensajes para el usuario. */
    private fun spanishError(status: Int, code: String, message: String): String = when {
        status == 401 || code.startsWith("PGRST3") -> "Tu sesión no es válida. Cierra sesión y vuelve a entrar."
        "rides_one_open_per_rider" in message -> "Ya tienes un viaje en curso."
        "rides_one_open_per_driver" in message || message == "driver_busy" -> "Ya estás atendiendo otro viaje."
        "drivers_document_type_document_number_key" in message -> "Ese DNI ya está registrado por otro conductor."
        message == "ride_not_available" -> "Este viaje ya no está disponible."
        message == "driver_not_approved" -> "Tu cuenta de conductor aún no está aprobada."
        message == "invalid_transition" -> "El viaje ya cambió de estado. Intenta de nuevo."
        message == "ride_not_found" -> "No se encontró el viaje."
        message == "vehicle_type_not_available" -> "Este tipo de vehículo no está disponible."
        message == "not_authenticated" -> "Inicia sesión para continuar."
        code == "42501" -> "No tienes permiso para realizar esta acción."
        code == "23514" -> "Algunos datos no son válidos. Revísalos e intenta de nuevo."
        else -> "Ocurrió un error ($status). Intenta de nuevo."
    }

    suspend fun rpc(name: String, body: JSONObject = JSONObject()): JSONObject {
        val text = request("POST", "rpc/$name", body, "return=representation")
        if (text.isBlank()) return JSONObject()
        return if (text.trimStart().startsWith("[")) JSONArray(text).optJSONObject(0) ?: JSONObject()
        else JSONObject(text)
    }

    suspend fun rpcRows(name: String, body: JSONObject = JSONObject()): JSONArray {
        val text = request("POST", "rpc/$name", body)
        return if (text.isBlank()) JSONArray() else JSONArray(text)
    }

    suspend fun rows(path: String): JSONArray {
        val text = request("GET", path)
        return if (text.isBlank()) JSONArray() else JSONArray(text)
    }

    suspend fun upsert(table: String, body: JSONObject, conflict: String = "id") {
        request(
            "POST",
            "$table?on_conflict=${encode(conflict)}",
            body,
            "resolution=merge-duplicates,return=minimal"
        )
    }

    suspend fun ensureCurrentProfile(
        firstName: String = "",
        lastName: String = "",
        phone: String? = null,
        email: String? = null,
        photoUrl: String? = null,
        driverMode: Boolean? = null
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: error("Usuario no autenticado")
        val body = JSONObject().put("id", user.uid)
        if (firstName.isNotBlank()) body.put("first_name", firstName)
        if (lastName.isNotBlank()) body.put("last_name", lastName)
        (phone ?: user.phoneNumber)?.takeIf { it.startsWith("+") }?.let { body.put("phone", it) }
        (email ?: user.email)?.let { body.put("email", it) }
        (photoUrl ?: user.photoUrl?.toString())?.let { body.put("photo_url", it) }
        if (driverMode != null) body.put("driver_mode", driverMode)
        upsert("profiles", body)
    }

    suspend fun syncDriver(
        documentNumber: String,
        licenseNumber: String,
        vehicleType: String,
        brand: String,
        model: String,
        year: Int?,
        plate: String
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Usuario no autenticado")
        ensureCurrentProfile(driverMode = true)
        upsert("drivers", JSONObject()
            .put("id", uid)
            .put("document_type", "dni")
            .put("document_number", documentNumber)
            .put("license_number", licenseNumber))

        val existing = rows("vehicles?driver_id=eq.${encode(uid)}&is_active=eq.true&select=id")
        val vehicle = JSONObject()
            .put("driver_id", uid)
            .put("vehicle_type", vehicleType)
            .put("brand", brand)
            .put("model", model)
            .put("year", year ?: JSONObject.NULL)
            .put("plate", plate)
            .put("is_active", true)
        if (existing.length() > 0) {
            val id = existing.getJSONObject(0).getString("id")
            request("PATCH", "vehicles?id=eq.${encode(id)}", vehicle, "return=minimal")
        } else {
            request("POST", "vehicles", vehicle, "return=minimal")
        }
    }

    suspend fun driverStatus(): String? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val rows = rows("drivers?id=eq.${encode(uid)}&select=status&limit=1")
        return rows.optJSONObject(0)?.optString("status")
    }

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
