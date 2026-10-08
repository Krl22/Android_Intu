package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.intu.taxi.repositories.LocationSimulationAccess
import com.intu.taxi.repositories.LocationSimulationRepository
import com.intu.taxi.location.AdminLocationSimulation
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ColumnScope.AdminLocationSimulationSettingsSection(reloadKey: Int) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val repository = remember { LocationSimulationRepository() }
    val scope = rememberCoroutineScope()
    var access by remember(uid) { mutableStateOf<LocationSimulationAccess?>(null) }
    var busy by remember(uid) { mutableStateOf(false) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(uid, reloadKey, retry) {
        access = null; error = null
        try { access = repository.get() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = "No se pudo cargar el permiso de simulación." }
    }
    AdminLocationSimulationSettingsContent(access, busy, error, onRetry = { retry++ }, onChange = { enabled ->
        if (!busy && access != null) scope.launch {
            busy = true; error = null
            try { access = repository.save(enabled) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudo guardar el permiso. Intenta de nuevo." }
            finally { busy = false }
        }
    }, onAdminBarChange = { enabled ->
        if (!busy && access != null) scope.launch {
            busy = true; error = null
            try {
                access = repository.saveAdminBar(enabled)
                if (access?.showControls == false) AdminLocationSimulation.clear()
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudo guardar tu preferencia. Intenta de nuevo." }
            finally { busy = false }
        }
    })
}

@Composable
internal fun ColumnScope.AdminLocationSimulationSettingsContent(access: LocationSimulationAccess?, busy: Boolean,
    error: String?, onRetry: () -> Unit, onChange: (Boolean) -> Unit,
    onAdminBarChange: (Boolean) -> Unit = {}) {
    AdminSectionHeading("Simulación de ubicación", "Los administradores pueden simular su ubicación para realizar pruebas.")
    if (access == null) {
        if (error == null) CircularProgressIndicator()
        else { Text(error, color = MaterialTheme.colorScheme.error); Button(onRetry) { Text("Reintentar") } }
    } else {
        if (access.isAdmin) AdminCard {
            Row(Modifier.fillMaxWidth().testTag("admin-location-simulation-own-bar")
                .toggleable(access.adminBarEnabled, enabled = !busy, role = Role.Switch, onValueChange = onAdminBarChange),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mostrar barra de simulación en mi cuenta", style = MaterialTheme.typography.titleMedium)
                    Text(if (access.adminBarEnabled) "Visible para realizar pruebas" else "Oculta para usar la app normalmente",
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(access.adminBarEnabled, onCheckedChange = null, enabled = !busy)
            }
            Text("Empieza oculta. Esta preferencia se guarda para tu cuenta en todos tus dispositivos. " +
                "Al ocultarla, se detiene la simulación y se vuelve al GPS real. Los demás administradores conservan su propia preferencia.",
                style = MaterialTheme.typography.bodySmall)
        }
        AdminCard {
            Row(Modifier.fillMaxWidth().testTag("admin-location-simulation-users")
                .toggleable(access.usersEnabled, enabled = !busy, role = Role.Switch, onValueChange = onChange),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Permitir simulación a usuarios", style = MaterialTheme.typography.titleMedium)
                    Text(if (access.usersEnabled) "Activada para pasajeros y conductores con sesión"
                        else "Desactivada · solo administradores", style = MaterialTheme.typography.bodySmall)
                }
                Switch(access.usersEnabled, onCheckedChange = null, enabled = !busy)
            }
            Text("Al activarlo, los usuarios verán la barra de pruebas de ubicación. No les da acceso al panel admin. " +
                "Al desactivarlo, sus simulaciones se detienen y la app vuelve al GPS real al sincronizar el permiso (hasta 15 segundos con conexión).",
                style = MaterialTheme.typography.bodySmall)
            if (busy) Text("Guardando…", color = MaterialTheme.colorScheme.primary)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
