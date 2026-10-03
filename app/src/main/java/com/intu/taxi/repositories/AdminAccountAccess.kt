package com.intu.taxi.repositories

/** Identidad leída por el servidor desde Firebase Auth, independiente del perfil de contacto. */
data class AdminAccountAccess(
    val uid: String,
    val status: String,
    val email: String = "",
    val emailVerified: Boolean = false,
    val phone: String = "",
    val disabled: Boolean = false,
    val providers: List<AdminAuthProvider> = emptyList()
) {
    val google: AdminAuthProvider? get() = providers.firstOrNull { it.id == "google.com" }
    val phoneLinked: Boolean get() = providers.any { it.id == "phone" }

    companion object {
        fun fromCallable(data: Any?): Map<String, AdminAccountAccess> {
            val rows = (data as? Map<*, *>)?.get("accounts") as? List<*>
                ?: error("No se recibieron los accesos. Actualiza el panel.")
            return rows.map { value ->
                val row = value as? Map<*, *> ?: error("Respuesta de accesos inválida.")
                val uid = row["uid"] as? String ?: error("Falta la cuenta en la respuesta de accesos.")
                val status = row["status"] as? String ?: error("Falta el estado de los accesos.")
                require(uid.isNotBlank() && status in setOf("found", "missing", "unavailable"))
                val providers = if (status == "found") {
                    (row["providers"] as? List<*>)?.map { provider ->
                        val fields = provider as? Map<*, *> ?: error("Método de acceso inválido.")
                        AdminAuthProvider(fields["id"] as? String ?: error("Falta el método de acceso."),
                            fields["email"] as? String ?: "", fields["phone"] as? String ?: "")
                    } ?: error("Faltan los métodos de acceso.")
                } else emptyList()
                AdminAccountAccess(uid, status, row["email"] as? String ?: "", row["emailVerified"] == true,
                    row["phone"] as? String ?: "", row["disabled"] == true, providers)
            }.associateBy { it.uid }
        }
    }
}

data class AdminAuthProvider(val id: String, val email: String = "", val phone: String = "")
