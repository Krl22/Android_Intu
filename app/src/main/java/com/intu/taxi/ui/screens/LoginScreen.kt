package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource

@Composable
fun LoginScreen(
    onGoogleClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onShowTerms: () -> Unit
) {
    val accepted = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF08817E), Color(0xFF1E1F47))
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bienvenido a Intu", style = MaterialTheme.typography.titleLarge)
                Text("Elige tu método de inicio de sesión", color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                // Botón Google estilizado
                Button(
                    onClick = { if (accepted.value) onGoogleClick() },
                    enabled = accepted.value,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E), contentColor = Color.White)
                ) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(
                            painter = painterResource(id = com.intu.taxi.R.drawable.ic_google_logo),
                            contentDescription = "Google logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(22.dp)
                        )
                        Text("Continuar con Google")
                    }
                }

                // Botón Teléfono con icono y estilo elevado
                ElevatedButton(
                    onClick = { if (accepted.value) onPhoneClick() },
                    enabled = accepted.value,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46), contentColor = Color.White)
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Phone, contentDescription = null)
                        Text("Continuar con Teléfono")
                    }
                }

                Spacer(Modifier.height(8.dp))
                RowConsent(accepted = accepted.value, onToggle = { accepted.value = it }, onShowTerms = onShowTerms)
            }
        }
    }
}

@Composable
private fun RowConsent(accepted: Boolean, onToggle: (Boolean) -> Unit, onShowTerms: () -> Unit) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = accepted, onCheckedChange = onToggle)
        Text(
            text = "Acepto Términos y Privacidad",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onShowTerms() }
        )
    }
}