package com.intu.taxi.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun TermsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Términos y Privacidad") },
        text = {
            Text(
                "Al registrarte aceptas que Intu procese tus datos para brindar " +
                    "servicios de movilidad, soporte y seguridad. Recopilamos tu nombre, " +
                    "apellido, fecha de nacimiento, correo (opcional) y número de teléfono " +
                    "para crear tu perfil y autenticarte. Puedes solicitar la eliminación y " +
                    "rectificación de tus datos contactando soporte. El uso está sujeto a " +
                    "políticas de seguridad, antifraude y verificación."
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}