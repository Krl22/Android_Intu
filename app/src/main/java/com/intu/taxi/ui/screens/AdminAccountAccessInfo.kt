package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.intu.taxi.repositories.AdminAccountAccess

@Composable
fun AdminAccountAccessInfo(uid: String, profileEmail: String, profilePhone: String,
    access: AdminAccountAccess?, loading: Boolean = false) {
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cuenta: $uid", style = MaterialTheme.typography.bodySmall)
            Text("Correo de contacto: ${profileEmail.ifBlank { "Sin registrar" }}", style = MaterialTheme.typography.bodySmall)
            Text("Teléfono de contacto: ${profilePhone.ifBlank { "Sin registrar" }}", style = MaterialTheme.typography.bodySmall)
            when (access?.status) {
                "found" -> {
                    Text("Google: ${access.google?.let { "Vinculado · ${it.email.ifBlank { access.email }.ifBlank { "Sin correo" }}" } ?: "Sin vincular"}",
                        style = MaterialTheme.typography.bodySmall)
                    val verifiedPhone = access.phone.ifBlank { access.providers.firstOrNull { it.id == "phone" }?.phone.orEmpty() }
                    Text("Acceso por SMS: ${if (access.phoneLinked) "Vinculado · ${verifiedPhone.ifBlank { "Sin número" }}" else "Sin vincular"}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Correo de la cuenta: ${access.email.ifBlank { "Sin correo" }}${if (access.email.isNotBlank()) if (access.emailVerified) " · Verificado" else " · Sin verificar" else ""}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Estado de acceso: ${if (access.disabled) "Deshabilitado" else "Habilitado"}", style = MaterialTheme.typography.bodySmall)
                    val other = access.providers.map { it.id }.filter { it !in setOf("google.com", "phone") }
                    if (other.isNotEmpty()) Text("Otros métodos: ${other.joinToString()}", style = MaterialTheme.typography.bodySmall)
                }
                "missing" -> Text("No existe una cuenta de acceso para este perfil.", style = MaterialTheme.typography.bodySmall)
                else -> Text(if (loading) "Consultando vinculación…" else "Vinculación no disponible. Actualiza el panel para volver a consultar.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
