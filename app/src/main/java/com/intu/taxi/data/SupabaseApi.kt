package com.intu.taxi.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Inicia sesión para continuar.")
        return try {
            user.getIdToken(forceRefresh).await().token
                ?: throw IllegalStateException("Inicia sesión para continuar.")
        } catch (e: FirebaseAuthInvalidUserException) {
            // Un administrador eliminó la cuenta: se cierra la sesión y la app vuelve al inicio de sesión
            FirebaseAuth.getInstance().signOut()
            throw IllegalStateException("Tu cuenta fue eliminada. Inicia sesión de nuevo.", e)
        }
    }

    suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        prefer: String? = null,
        timeoutMillis: Long? = null
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

        val client = if (timeoutMillis != null) http.newBuilder()
            .callTimeout(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS).build() else http
        val call = client.newCall(builder.build())
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
                    spanishError(it.code, json?.str("code").orEmpty(), json?.str("message").orEmpty(), json?.str("details").orEmpty())
                )
            }
            text
        }
    }

    /** Traduce los errores de Supabase y de las funciones RPC a mensajes para el usuario. */
    internal fun spanishError(status: Int, code: String, message: String, details: String = ""): String = when {
        status == 401 || code.startsWith("PGRST3") -> "Tu sesión no es válida. Cierra sesión y vuelve a entrar."
        message == "cancellation_block" -> "Por cancelar varias veces, tu cuenta está en pausa " +
            (formatBlockedUntil(details) ?: "por un tiempo") + "."
        message == "too_many_requests" -> "Hiciste muchas solicitudes seguidas. Espera unos minutos e intenta de nuevo."
        message == "no_show_not_allowed" -> when (details.substringBefore(':')) {
            "no_show_wait" -> "Espera ${formatWait(details.substringAfter(':').toIntOrNull() ?: 0)} más en el punto de recojo."
            "no_show_far" -> "Debes estar en el punto de recojo para cancelar por este motivo."
            else -> "Primero marca que llegaste al punto de recojo."
        }
        message == "chat_closed" -> "El chat se cierra cuando termina el servicio."
        message == "driver_quick_replies_only" -> "Mientras manejas solo puedes enviar respuestas rápidas."
        message == "invalid_message" -> "Escribe un mensaje de hasta 500 caracteres."
        message == "too_many_messages" -> "Estás enviando muchos mensajes. Espera un momento."
        message == "invalid_preferences" -> "Revisa los valores e intenta de nuevo."
        message == "invalid_schedule_time" -> "Programa el viaje entre 20 minutos y 7 días desde ahora."
        message == "invalid_scheduled_ride" -> "Revisa el recojo, el destino y el pago del viaje programado."
        message == "too_many_scheduled" -> "Ya tienes 3 viajes programados. Cancela uno para programar otro."
        message == "schedule_conflict" -> "Ya tienes un viaje programado cerca de esa hora."
        message == "scheduled_ride_not_found" -> "Ese viaje programado ya no está disponible."
        message == "invalid_passenger_contact" -> "El contacto debe tener un celular peruano válido."
        message == "support_chat_disabled" -> "El asistente de ayuda no está disponible por ahora."
        message == "support_chat_limit" -> "Llegaste al límite de preguntas de hoy. Vuelve mañana o usa Reportar un error."
        message == "support_chat_knowledge_required" -> "Escribe el texto de ayuda antes de activar el asistente."
        message == "unavailable" -> "El asistente no pudo responder. Intenta de nuevo en un momento."
        "rides_one_open_per_rider" in message -> "Ya tienes un viaje en curso."
        "rides_one_pickup_per_driver" in message || message == "driver_busy" -> "Ya tienes un pasajero por recoger."
        "rides_one_trip_per_driver" in message || message == "finish_current_trip" ->
            "Termina el viaje actual antes de ir por el siguiente pasajero."
        "drivers_document_type_document_number_key" in message -> "Ese DNI ya está registrado por otro conductor."
        message == "ride_not_available" -> "Este viaje ya no está disponible."
        message == "driver_not_approved" -> "Tu cuenta de conductor aún no está aprobada."
        message == "invalid_transition" -> "El viaje ya cambió de estado. Intenta de nuevo."
        message == "not_admin" -> "Solo un administrador puede hacer esto."
        message == "business_delivery_disabled" -> "Los pedidos de negocios están desactivados. Puedes usar Enviar para un envío normal."
        message == "business_ad_unavailable" -> "Este anuncio ya no está disponible. Vuelve a Inicio y actualiza."
        message == "business_ad_changed" -> "Otro administrador cambió este anuncio. Cierra el editor y actualiza la lista."
        message == "invalid_business_ad" -> "Revisa el anuncio y el punto de recojo del negocio."
        message == "invalid_business_menu" -> "Revisa los productos, sus precios e imágenes antes de guardar."
        message == "invalid_business_order" -> "Revisa los productos y cantidades. Abre el menú actualizado desde Inicio."
        message == "business_no_test_courier" -> "Un admin debe seleccionar un repartidor de prueba en Negocios antes de solicitar este envío."
        message == "business_test_courier_required" -> "Solo los repartidores de moto seleccionados para pruebas pueden tomar estos pedidos."
        message == "pickup_not_verified" -> "Confirma el punto de recojo antes de publicar."
        message == "place_not_found" -> "Ese lugar ya no está disponible. Actualiza la lista."
        message == "place_changed" -> "Otro administrador editó este lugar. Cierra el editor y actualiza la lista."
        message == "invalid_place" -> "Revisa el nombre y el punto de recojo del lugar."
        message == "last_admin" -> "Debe quedar al menos un administrador. Haz admin a otra cuenta primero."
        message == "account_deleted" -> "Tu cuenta fue eliminada. Inicia sesión de nuevo."
        message == "user_not_found" || message == "driver_not_found" -> "No se encontró esa cuenta."
        message == "cannot_rate" -> "Ya calificaste este viaje."
        message == "invalid_rating" -> "Elige de 1 a 5 estrellas."
        message == "pin_required" -> "Ingresa el PIN del pasajero para iniciar el viaje."
        message == "pin_locked" -> "Demasiados intentos con PIN incorrecto. Cancela el viaje por seguridad."
        message == "ride_not_found" -> "No se encontró el viaje."
        message == "vehicle_type_not_available" -> "Este tipo de vehículo no está disponible."
        message == "invalid_driver_application" -> "Completa los datos del vehículo, la licencia y el DNI."
        message == "invalid_delivery_details" -> "Completa los datos del envío y el celular de quien recibe."
        message == "invalid_payment_stage" -> "Confirma el pago en el momento acordado: recojo o entrega."
        message == "delivery_payment_required" -> "Confirma primero que recibiste el pago del transporte."
        message == "delivery_in_custody" -> "Ya confirmaste el pago del envío. Contacta al cliente para resolver cualquier problema."
        message == "vehicle_change_during_ride" -> "Termina tus viajes antes de cambiar los datos del vehículo."
        "vehicles_plate_key" in message -> "Esta placa ya está registrada. Revisa el número o reporta el problema a Intu."
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

    /** Llama a una Edge Function con la sesión actual. Sus errores llegan como {"error": "código"}. */
    suspend fun invokeFunction(name: String, body: JSONObject, timeoutMillis: Long = 45_000): JSONObject =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/functions/v1/$name")
                .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer ${token()}")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val client = http.newBuilder().callTimeout(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS).build()
            val response = try {
                client.newCall(request).execute()
            } catch (e: java.io.IOException) {
                throw IllegalStateException("Sin conexión. Revisa tu internet e intenta de nuevo.", e)
            }
            response.use {
                val json = runCatching { JSONObject(it.body?.string().orEmpty()) }.getOrNull()
                if (!it.isSuccessful) throw IllegalStateException(spanishError(it.code, "", json?.str("error").orEmpty()))
                json ?: JSONObject()
            }
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
        driverMode: Boolean? = null,
        birthdate: String? = null,
        termsAccepted: Boolean? = null
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: error("Inicia sesión para continuar.")
        val body = JSONObject().put("id", user.uid)
        if (firstName.isNotBlank()) body.put("first_name", firstName)
        if (lastName.isNotBlank()) body.put("last_name", lastName)
        (user.phoneNumber ?: normalizePhone(phone))?.let { body.put("phone", it) }
        (user.email ?: email?.takeIf { it.isNotBlank() })?.let { body.put("email", it) }
        (photoUrl ?: user.photoUrl?.toString())?.let { body.put("photo_url", it) }
        if (driverMode != null) body.put("driver_mode", driverMode)
        birthdate?.trim()?.takeIf { Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it) }?.let { body.put("birthdate", it) }
        if (termsAccepted == true) body.put("terms_accepted_at", java.time.Instant.now().toString())
        upsert("profiles", body)
    }

    /** Perfil del usuario con sesión en Supabase, o null si todavía no existe. */
    suspend fun currentProfile(): JSONObject? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return rows("profiles?id=eq.${encode(uid)}&select=*&limit=1").optJSONObject(0)
    }

    /**
     * Teléfono en formato internacional (+51999888777) como exige Supabase. Acepta números con
     * espacios o guiones y, si faltan el + y el código de país, asume Perú para 9 dígitos.
     */
    fun normalizePhone(raw: String?): String? {
        return com.intu.taxi.auth.PhoneFormatter.normalizeMobile(raw)
    }

    suspend fun syncDriver(
        documentNumber: String,
        licenseNumber: String,
        vehicleType: String,
        brand: String,
        model: String,
        year: Int?,
        plate: String,
        // Registrar documentos no concede permiso para conducir.
        markDriverMode: Boolean = false
    ) {
        FirebaseAuth.getInstance().currentUser ?: error("Inicia sesión para continuar.")
        ensureCurrentProfile(driverMode = if (markDriverMode) true else null)
        rpc("submit_driver_application", JSONObject()
            .put("p_document_number", documentNumber)
            .put("p_license_number", licenseNumber)
            .put("p_vehicle_type", com.intu.taxi.auth.DriverVehicleType.requireCode(vehicleType))
            .put("p_brand", brand)
            .put("p_model", model)
            .put("p_year", year ?: JSONObject.NULL)
            .put("p_plate", plate))
    }

    /** Datos de conductor (documentos, estado y vehículo activo) del usuario con sesión, o null. */
    suspend fun currentDriver(): Pair<JSONObject, JSONObject?>? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val driver = rows("drivers?id=eq.${encode(uid)}&select=document_number,license_number,status&limit=1")
            .optJSONObject(0) ?: return null
        val vehicle = rows("vehicles?driver_id=eq.${encode(uid)}&is_active=eq.true&select=vehicle_type,brand,model,year,plate&limit=1")
            .optJSONObject(0)
        return driver to vehicle
    }

    suspend fun driverStatus(): String? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val rows = rows("drivers?id=eq.${encode(uid)}&select=status&limit=1")
        return rows.optJSONObject(0)?.optString("status")
    }

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
