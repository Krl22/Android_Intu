package com.intu.taxi.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intu.taxi.driver.ExternalNavigation
import com.intu.taxi.driver.NavigationApp
import com.intu.taxi.ui.theme.AppearanceColors

/**
 * Abre Google Maps o Waze con la ruta hasta [latitude], [longitude]. Usa la app que el conductor
 * eligió antes; si tiene las dos y aún no eligió, pregunta una vez. Sin ninguna, abre el navegador.
 */
@Composable
fun NavigateButton(latitude: Double, longitude: Double, compact: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var choosing by remember { mutableStateOf<List<NavigationApp>?>(null) }

    fun open(app: NavigationApp?) {
        if (ExternalNavigation.launch(context, app, latitude, longitude)) {
            Toast.makeText(context, "Al llegar, vuelve a Intu para continuar el viaje", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No se pudo abrir el mapa. Instala Google Maps o Waze.", Toast.LENGTH_LONG).show()
        }
    }

    fun navigate() {
        val installed = ExternalNavigation.installedApps(context)
        val preferred = ExternalNavigation.preferredApp(context)?.takeIf { it in installed }
        when {
            preferred != null -> open(preferred)
            installed.size == 1 -> open(installed.single())
            installed.isEmpty() -> open(null)
            else -> choosing = installed
        }
    }

    val teal = AppearanceColors.highlight(Color(0xFF08817E))
    Column(modifier) {
        OutlinedButton(
            onClick = ::navigate,
            shape = RoundedCornerShape(12.dp),
            contentPadding = if (compact) PaddingValues(vertical = 8.dp, horizontal = 12.dp) else PaddingValues(vertical = 12.dp, horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().testTag("driver-navigate")
        ) {
            Icon(Icons.Default.Navigation, contentDescription = null, tint = teal, modifier = Modifier.size(if (compact) 14.dp else 18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Navegar", color = teal, fontWeight = FontWeight.SemiBold, fontSize = if (compact) 12.sp else 14.sp)
        }
        // Con las dos apps instaladas, el conductor puede cambiar la que eligió
        if (!compact && ExternalNavigation.installedApps(context).size > 1 && ExternalNavigation.preferredApp(context) != null) {
            TextButton(onClick = { choosing = ExternalNavigation.installedApps(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Cambiar app de mapas", style = MaterialTheme.typography.bodySmall, color = AppearanceColors.muted)
            }
        }
    }

    choosing?.let { apps ->
        AlertDialog(
            onDismissRequest = { choosing = null },
            title = { Text("¿Con qué app quieres navegar?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    apps.forEach { app ->
                        OutlinedButton(onClick = {
                            ExternalNavigation.rememberApp(context, app)
                            choosing = null
                            open(app)
                        }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(app.label) }
                    }
                    Text("La usaremos siempre. Puedes cambiarla desde la tarjeta del viaje.",
                        style = MaterialTheme.typography.bodySmall, color = AppearanceColors.muted)
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosing = null }) { Text("Cancelar") } }
        )
    }
}
