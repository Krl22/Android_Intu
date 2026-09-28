package com.intu.taxi.repositories

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime

/** Conductor registrado, tal como lo ve el panel de administración. */
data class AdminDriver(
    val id: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val photoUrl: String,
    val documentNumber: String,
    val licenseNumber: String,
    val status: String,
    val rating: Double,
    val ratingCount: Int,
    val registeredAt: Instant?,
    val vehicle: String,
    val plate: String
)

/** Cuenta de la app (pasajero, conductor o admin), para reiniciarla, eliminarla o hacerla admin. */
data class AdminUser(
    val id: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val photoUrl: String,
    val driverStatus: String?,
    val isAdmin: Boolean,
    val ridesAsRider: Int,
    val ridesAsDriver: Int
)

/** Funciones del panel de administración. El servidor rechaza todo si la cuenta no es admin. */
class AdminRepository {
    suspend fun isAdmin(): Boolean {
        val text = SupabaseApi.request("POST", "rpc/is_admin", JSONObject())
        return text.trim() == "true"
    }

    /** Conductores, opcionalmente filtrados por estado: pending, approved, rejected o suspended. */
    suspend fun listDrivers(status: String? = null): List<AdminDriver> {
        val body = JSONObject().put("p_status", status ?: JSONObject.NULL)
        val rows = SupabaseApi.rpcRows("admin_list_drivers", body)
        return (0 until rows.length()).map { rows.getJSONObject(it).toAdminDriver() }
    }

    suspend fun setDriverStatus(driverId: String, status: String) {
        SupabaseApi.rpc("admin_set_driver_status", JSONObject().put("p_driver_id", driverId).put("p_status", status))
    }

    suspend fun listUsers(): List<AdminUser> {
        val rows = SupabaseApi.rpcRows("admin_list_users")
        return (0 until rows.length()).map { i ->
            val row = rows.getJSONObject(i)
            AdminUser(
                id = row.str("id"),
                fullName = listOf(row.str("first_name"), row.str("last_name")).filter { it.isNotBlank() }.joinToString(" "),
                phone = row.str("phone"),
                email = row.str("email"),
                photoUrl = row.str("photo_url"),
                driverStatus = row.str("driver_status").ifBlank { null },
                isAdmin = row.optBoolean("is_admin", false),
                ridesAsRider = row.optInt("rides_as_rider", 0),
                ridesAsDriver = row.optInt("rides_as_driver", 0)
            )
        }
    }

    /**
     * Reinicia una cuenta para repetir las pruebas. [scope] "driver" borra solo los datos de conductor;
     * "account" además vacía el perfil. El historial de viajes se conserva.
     */
    suspend fun resetUser(userId: String, scope: String) {
        SupabaseApi.request("POST", "rpc/admin_reset_user", JSONObject().put("p_user_id", userId).put("p_scope", scope))
    }

    /** Da o quita permisos de administrador. El servidor no deja quitar al último admin. */
    suspend fun setAdmin(userId: String, isAdmin: Boolean) {
        SupabaseApi.request("POST", "rpc/admin_set_admin", JSONObject().put("p_user_id", userId).put("p_is_admin", isAdmin))
    }

    /**
     * Elimina la cuenta por completo con la Cloud Function adminDeleteUser: datos en Supabase,
     * cuenta de inicio de sesión de Firebase y foto. Los viajes quedan anónimos para la otra persona.
     */
    suspend fun deleteUser(userId: String) {
        try {
            FirebaseFunctions.getInstance()
                .getHttpsCallable("adminDeleteUser")
                .call(mapOf("userId" to userId))
                .await()
        } catch (e: FirebaseFunctionsException) {
            // Estos errores ya traen el mensaje en español desde la función
            val ownMessage = e.code in setOf(
                FirebaseFunctionsException.Code.PERMISSION_DENIED,
                FirebaseFunctionsException.Code.FAILED_PRECONDITION,
                FirebaseFunctionsException.Code.INVALID_ARGUMENT,
                FirebaseFunctionsException.Code.UNAUTHENTICATED,
                FirebaseFunctionsException.Code.INTERNAL
            ) && !e.message.isNullOrBlank() && e.message != "INTERNAL"
            throw IllegalStateException(
                if (ownMessage) e.message else "No se pudo eliminar la cuenta. Revisa tu internet e intenta de nuevo.",
                e
            )
        } catch (e: Exception) {
            throw IllegalStateException("No se pudo eliminar la cuenta. Revisa tu internet e intenta de nuevo.", e)
        }
    }

    private fun JSONObject.toAdminDriver() = AdminDriver(
        id = str("id"),
        fullName = listOf(str("first_name"), str("last_name")).filter { it.isNotBlank() }.joinToString(" "),
        phone = str("phone"),
        email = str("email"),
        photoUrl = str("photo_url"),
        documentNumber = str("document_number"),
        licenseNumber = str("license_number"),
        status = str("status"),
        rating = optDouble("rating", 5.0),
        ratingCount = optInt("rating_count", 0),
        registeredAt = runCatching { OffsetDateTime.parse(str("created_at")).toInstant() }.getOrNull(),
        vehicle = listOf(str("brand"), str("model"), if (isNull("year")) "" else optInt("year").toString(), str("color"))
            .filter { it.isNotBlank() }.joinToString(" "),
        plate = str("plate")
    )
}
