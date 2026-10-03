package com.intu.taxi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.repositories.RideSecurityRepository
import com.intu.taxi.repositories.RideSecuritySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AdminRideSecuritySettings(reloadKey: Int) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val repository = remember { RideSecurityRepository() }
    val scope = rememberCoroutineScope()
    var settings by remember(uid) { mutableStateOf<RideSecuritySettings?>(null) }
    var busy by remember(uid) { mutableStateOf(false) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(uid, reloadKey, retry) {
        settings = null
        error = null
        try { settings = repository.get() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No se pudo cargar la seguridad de los servicios." }
    }
    AdminRideSecurityContent(settings, busy, error, onRetry = { retry++ }, onChange = { enabled ->
        if (!busy && settings != null) scope.launch {
            busy = true
            error = null
            try { settings = repository.save(enabled) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No se pudo guardar. Intenta de nuevo." }
            finally { busy = false }
        }
    })
}

@Composable
internal fun AdminRideSecurityContent(settings: RideSecuritySettings?, busy: Boolean, error: String?,
    onRetry: () -> Unit, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AdminSectionHeading("Seguridad de los servicios", "Controla el PIN de los viajes y envíos de Intu.")
        if (settings == null) {
            if (error == null) CircularProgressIndicator()
            else {
                Text(error, color = colors.error)
                Button(onClick = onRetry) { Text("Reintentar") }
            }
        } else {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.outlineVariant)) {
                Row(Modifier.fillMaxWidth().testTag("admin-ride-pin")
                    .toggleable(value = settings.pinEnabled, enabled = !busy, role = Role.Switch, onValueChange = onChange)
                    .padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Shield, null, tint = colors.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("PIN de seguridad", style = MaterialTheme.typography.titleMedium)
                        Text(if (settings.pinEnabled) "Activado para nuevas solicitudes" else "Desactivado para nuevas solicitudes",
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Switch(checked = settings.pinEnabled, onCheckedChange = null, enabled = !busy)
                }
            }
            Text("Al activarlo, el conductor pedirá un PIN de 4 dígitos antes de iniciar el viaje o recoger el paquete. " +
                "El cambio aplica a nuevas solicitudes; los servicios ya solicitados conservan su configuración.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (busy) Text("Guardando…", color = colors.primary)
            error?.let { Text(it, color = colors.error) }
        }
    }
}
