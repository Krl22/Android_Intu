package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intu.taxi.updates.AppUpdateState
import com.intu.taxi.updates.PublishedAppRelease
import com.intu.taxi.ui.theme.LocalIntuDarkMode
import java.util.Locale

@Composable
internal fun AppUpdateDialog(
    state: AppUpdateState,
    installedCode: Int,
    installedName: String,
    tripActive: Boolean,
    onCheck: () -> Unit,
    onDownload: (PublishedAppRelease) -> Unit,
    onDismiss: () -> Unit,
) {
    val release = state.newerThan(installedCode)
    val dark = LocalIntuDarkMode.current
    val teal = if (dark) MaterialTheme.colorScheme.primary else Color(0xFF08817E)
    val ink = if (dark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E1F47)
    AlertDialog(
        modifier = Modifier.testTag("app-update-dialog"),
        onDismissRequest = onDismiss,
        containerColor = if (dark) MaterialTheme.colorScheme.surface else Color.White,
        titleContentColor = ink,
        textContentColor = if (dark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF536871),
        shape = RoundedCornerShape(26.dp),
        icon = { Icon(Icons.Outlined.SystemUpdate, null, tint = teal) },
        title = { Text(if (release != null) "Hay una nueva versión" else "Actualizaciones", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tu versión: $installedName")
                when {
                    state.checking -> {
                        CircularProgressIndicator(Modifier.size(24.dp), color = teal, strokeWidth = 2.dp)
                        Text("Buscando actualizaciones…")
                    }
                    release != null -> {
                        Text(release.title, style = MaterialTheme.typography.titleLarge,
                            color = ink, fontWeight = FontWeight.Bold)
                        if (release.size > 0) Text("Descarga: ${String.format(Locale.US, "%.0f", release.size / 1048576.0)} MB")
                        Text(when {
                            tripActive -> "Termina tu viaje o envío antes de actualizar."
                            release.fromPlay -> "Google Play descargará e instalará la actualización. Intu se reiniciará al terminar."
                            else -> "Se abrirá la descarga. Cuando termine, abre el archivo y confirma la actualización en Android."
                        })
                    }
                    state.error != null -> Unit
                    state.checked -> Text("Ya tienes la versión más reciente.")
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("app-update-error")) }
            }
        },
        confirmButton = {
            if (release != null) Button(
                onClick = { onDownload(release) }, enabled = !tripActive && !state.checking,
                colors = ButtonDefaults.buttonColors(containerColor = teal),
                modifier = Modifier.heightIn(min = 48.dp).testTag("app-update-download"),
            ) { Text("Actualizar") }
            else TextButton(onClick = onCheck, enabled = !state.checking) {
                Text(if (state.error != null) "Reintentar" else "Comprobar", color = teal)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (release != null) "Más tarde" else "Cerrar", color = teal) } },
    )
}
