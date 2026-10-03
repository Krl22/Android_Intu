package com.intu.taxi.repositories

import com.google.firebase.auth.FirebaseAuth

/** Solicitud autenticada que el administrador atiende desde Reportes y Usuarios. */
class AccountDeletionRequestRepository {
    suspend fun submit(note: String) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Inicia sesión para solicitar la eliminación de tu cuenta.")
        val description = buildString {
            append("Solicito eliminar mi cuenta de Intu y los datos personales asociados.\n")
            append("Cuenta solicitante: ${user.uid}\n")
            append("Esta solicitud requiere revisión y confirmación de eliminación por el equipo de Intu.")
            if (note.isNotBlank()) append("\nInformación adicional: ${note.trim().take(1200)}")
        }
        BugReportRepository().submit("Solicitud de eliminación de cuenta", description, "Cuenta / Eliminar cuenta")
    }
}
