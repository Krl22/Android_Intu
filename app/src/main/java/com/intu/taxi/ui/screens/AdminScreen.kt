package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import com.intu.taxi.data.normalizedPlaceText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.intu.taxi.location.AdminLocationSimulation
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.repositories.AdminDriver
import com.intu.taxi.repositories.AdminRepository
import com.intu.taxi.repositories.AdminUser
import com.intu.taxi.ui.components.Avatar
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RegisteredFormat = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale("es", "PE"))

private val StatusFilters = listOf(
    "pending" to "Pendientes",
    "approved" to "Aprobados",
    "suspended" to "Suspendidos",
    "rejected" to "Rechazados",
    null to "Todos"
)

/**
 * Panel de administración.
 * Conductores: aprobar, rechazar, suspender o volver a pendiente.
 * Usuarios: reiniciar o eliminar cuentas para repetir las pruebas, y dar o quitar permisos de admin.
 * [onOwnAccountDeleted] se llama si el admin elimina su propia cuenta (hay que cerrar la sesión).
 */
@Composable
fun AdminScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
    onOwnAccountDeleted: () -> Unit,
    onTestLocationSelected: () -> Unit = {}
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val testLocation by AdminLocationSimulation.preset.collectAsState()
    var isAdmin by remember(uid) { mutableStateOf(false) }
    var showTestLocation by remember { mutableStateOf(false) }
    var showTestLocationMap by remember { mutableStateOf(false) }
    var locationBusy by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uid, reloadKey) {
        isAdmin = runCatching { AdminRepository().isAdmin() }.getOrDefault(false)
        if (!isAdmin) AdminLocationSimulation.clear()
    }
    fun selectTestLocation(location: com.intu.taxi.location.TestLocation) {
        if (locationBusy) return
        locationBusy = true
        locationError = null
        showTestLocationMap = false
        scope.launch {
            try {
                AdminLocationSimulation.activate(location)
                showTestLocation = false
                Toast.makeText(context, "Simulando ubicación en ${location.label}", Toast.LENGTH_SHORT).show()
                onTestLocationSelected()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { locationError = e.message ?: "No se pudo activar la simulación." }
            finally { locationBusy = false }
        }
    }
    AdminPanelTheme {
        if (showTestLocationMap && showTestLocation && isAdmin) {
            TestLocationMapPicker(testLocation,
                onDismiss = { showTestLocationMap = false },
                onPicked = ::selectTestLocation)
        }
        if (showTestLocation && !showTestLocationMap && isAdmin) {
            TestLocationDialog(
                active = testLocation, busy = locationBusy, error = locationError,
                onSelect = ::selectTestLocation,
                onRealGps = { AdminLocationSimulation.clear(); showTestLocation = false },
                onDismiss = { showTestLocation = false },
                onChooseOnMap = { locationError = null; showTestLocationMap = true }
            )
        }

        AdminPanelLayout(padding, tab, isAdmin, testLocation?.label, onBack,
            onRefresh = { reloadKey++ }, onLocation = { locationError = null; showTestLocation = true },
            onTab = { tab = it }) {
            when (tab) {
                0 -> DriversTab(reloadKey)
                1 -> UsersTab(reloadKey, onOwnAccountDeleted)
                2 -> BugReportsTab(reloadKey)
                3 -> AdminPlacesTab(reloadKey)
                else -> AdminNotificationSettings(reloadKey)
            }
        }
    }
}

@Composable
private fun DriversTab(reloadKey: Int) {
    val repo = remember { AdminRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf<String?>("pending") }
    var query by rememberSaveable { mutableStateOf("") }
    var drivers by remember { mutableStateOf<List<AdminDriver>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var localReload by remember { mutableIntStateOf(0) }
    var busyDriverId by remember { mutableStateOf<String?>(null) }
    // Rechazar o suspender se confirma antes: (conductor, nuevo estado)
    var pendingConfirmation by remember { mutableStateOf<Pair<AdminDriver, String>?>(null) }

    LaunchedEffect(filter, reloadKey, localReload) {
        drivers = null
        loadError = null
        runCatching { repo.listDrivers(filter) }
            .onSuccess { drivers = it }
            .onFailure {
                loadError = it.message ?: "No se pudo cargar la lista"
                drivers = emptyList()
            }
    }

    fun changeStatus(driver: AdminDriver, status: String) {
        busyDriverId = driver.id
        scope.launch {
            runCatching { repo.setDriverStatus(driver.id, status) }
                .onSuccess {
                    val done = when (status) {
                        "approved" -> "aprobado"
                        "rejected" -> "rechazado"
                        "pending" -> "pasó a pendiente"
                        else -> "suspendido"
                    }
                    Toast.makeText(context, "${driver.fullName.ifBlank { "Conductor" }}: $done", Toast.LENGTH_SHORT).show()
                    localReload++
                }
                .onFailure {
                    Toast.makeText(context, it.message ?: "No se pudo actualizar", Toast.LENGTH_LONG).show()
                }
            busyDriverId = null
        }
    }

    val list = drivers
    val needle = normalizedPlaceText(query)
    val visible = list.orEmpty().filter { needle in normalizedPlaceText("${it.fullName} ${it.phone} ${it.email} ${it.plate} ${it.vehicle}") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AdminSectionHeading("Conductores", "Revisa las postulaciones y gestiona quién puede recibir viajes o envíos.", list?.size) }
        item { AdminSearchField(query, { query = it }, "Nombre, teléfono o placa") }
        item { AdminFilters(StatusFilters, filter, { filter = it }) }
        when {
            list == null -> item { AdminLoading() }
            loadError != null -> item { AdminMessage(loadError.orEmpty(), true) { localReload++ } }
            visible.isEmpty() -> item { AdminMessage(if (query.isNotBlank()) "No hay coincidencias. Prueba con otro nombre o placa."
                else if (filter == "pending") "No hay conductores esperando aprobación." else "No hay conductores en esta lista.") }
            else -> items(visible, key = { it.id }) { driver ->
                AdminDriverCard(driver, busyDriverId == driver.id,
                    onApprove = { changeStatus(driver, "approved") },
                    onReject = { pendingConfirmation = driver to "rejected" },
                    onSuspend = { pendingConfirmation = driver to "suspended" },
                    onBackToPending = { changeStatus(driver, "pending") })
            }
        }
    }

    pendingConfirmation?.let { (driver, status) ->
        val rejecting = status == "rejected"
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = { Text(if (rejecting) "¿Rechazar conductor?" else "¿Suspender conductor?") },
            text = {
                Text(
                    "${driver.fullName.ifBlank { "Este conductor" }} no podrá recibir viajes" +
                        if (rejecting) "." else " hasta que lo vuelvas a aprobar."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingConfirmation = null
                    changeStatus(driver, status)
                }) { Text(if (rejecting) "Rechazar" else "Suspender", color = AppearanceColors.highlight(AdminRed)) }
            },
            dismissButton = { TextButton(onClick = { pendingConfirmation = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
internal fun AdminDriverCard(
    driver: AdminDriver, busy: Boolean, onApprove: () -> Unit, onReject: () -> Unit,
    onSuspend: () -> Unit, onBackToPending: () -> Unit
) {
    var expanded by rememberSaveable(driver.id) { mutableStateOf(false) }
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = driver.photoUrl, size = 48.dp, zoomable = true, contentDescription = "Foto del conductor")
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(driver.fullName.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                AdminStatusChip(driver.status)
            }
        }
        if (driver.phone.isNotBlank()) Text(driver.phone, style = MaterialTheme.typography.bodyMedium)
        if (driver.email.isNotBlank()) Text(driver.email, style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(AdminMuted))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val vehicleType = com.intu.taxi.auth.DriverVehicleType.from(driver.vehicleType)
        AdminDetail("Vehículo", listOf(vehicleType?.label ?: driver.vehicleType, driver.vehicle, driver.plate)
            .filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Sin vehículo" })
        vehicleType?.let { AdminDetail("Servicio", it.serviceLabel) }
        AdminDetail("DNI", driver.documentNumber)
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (expanded) "Ocultar documentación" else "Ver documentación")
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminDetail("Licencia", driver.licenseNumber)
                driver.registeredAt?.let { AdminDetail("Registrado", it.atZone(ZoneId.systemDefault()).format(RegisteredFormat)) }
                if (driver.ratingCount > 0) AdminDetail("Calificación", String.format(Locale.US, "%.2f (%d)", driver.rating, driver.ratingCount))
            }
        }
        when {
            busy -> AdminLoading()
            driver.status == "pending" -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppearanceColors.highlight(AdminRed))) { Text("Rechazar") }
                Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Aprobar") }
            }
            else -> Column(Modifier.fillMaxWidth()) {
                if (driver.status == "approved") OutlinedButton(onClick = onSuspend, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppearanceColors.highlight(AdminRed))) { Text("Suspender conductor") }
                else Button(onClick = onApprove, modifier = Modifier.fillMaxWidth()) { Text("Aprobar conductor") }
                TextButton(onClick = onBackToPending, modifier = Modifier.fillMaxWidth()) { Text("Pasar a pendiente") }
            }
        }
    }
}

/** Acciones sobre una cuenta que se confirman antes de hacerlas. */
internal enum class UserAction { ResetDriver, ResetAccount, MakeAdmin, RemoveAdmin, Delete }

@Composable
private fun UsersTab(reloadKey: Int, onOwnAccountDeleted: () -> Unit) {
    val repo = remember { AdminRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid }
    var query by rememberSaveable { mutableStateOf("") }
    var users by remember { mutableStateOf<List<AdminUser>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var accessLoading by remember { mutableStateOf(false) }
    var accessError by remember { mutableStateOf<String?>(null) }
    var localReload by remember { mutableIntStateOf(0) }
    var busyUserId by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<Pair<AdminUser, UserAction>?>(null) }

    LaunchedEffect(reloadKey, localReload) {
        users = null
        loadError = null
        accessError = null
        accessLoading = true
        try {
            val profiles = repo.listUsers()
            users = profiles
            try {
                val identities = repo.listAccountAccess(profiles.map { it.id })
                users = profiles.map { it.copy(accountAccess = identities[it.id]) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { accessError = "No se pudo consultar la vinculación. Usa Actualizar para volver a intentarlo." }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            loadError = e.message ?: "No se pudo cargar la lista"
            users = emptyList()
        } finally { accessLoading = false }
    }

    fun run(user: AdminUser, action: UserAction) {
        busyUserId = user.id
        scope.launch {
            runCatching {
                when (action) {
                    UserAction.ResetDriver -> repo.resetUser(user.id, "driver")
                    UserAction.ResetAccount -> repo.resetUser(user.id, "account")
                    UserAction.MakeAdmin -> repo.setAdmin(user.id, true)
                    UserAction.RemoveAdmin -> repo.setAdmin(user.id, false)
                    UserAction.Delete -> repo.deleteUser(user.id)
                }
            }.onSuccess {
                val name = user.fullName.ifBlank { "La cuenta" }
                val done = when (action) {
                    UserAction.ResetDriver -> "Datos de conductor reiniciados"
                    UserAction.ResetAccount -> "Cuenta reiniciada"
                    UserAction.MakeAdmin -> "$name ahora es administrador"
                    UserAction.RemoveAdmin -> "$name ya no es administrador"
                    UserAction.Delete -> "Cuenta eliminada"
                }
                Toast.makeText(context, done, Toast.LENGTH_SHORT).show()
                if (action == UserAction.Delete && user.id == myUid) {
                    onOwnAccountDeleted()
                } else {
                    localReload++
                }
            }.onFailure {
                Toast.makeText(context, it.message ?: "No se pudo completar la acción", Toast.LENGTH_LONG).show()
            }
            busyUserId = null
        }
    }

    val list = users
    val needle = normalizedPlaceText(query)
    val visible = list.orEmpty().filter { needle in normalizedPlaceText("${it.fullName} ${it.phone} ${it.email} ${it.id}") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AdminSectionHeading("Usuarios", "Consulta perfiles, vinculación y actividad. Abre Gestionar cuenta para administrar permisos o repetir pruebas.", list?.size) }
        item { AdminSearchField(query, { query = it }, "Nombre, teléfono o correo") }
        accessError?.let { item { AdminMessage(it, true) { localReload++ } } }
        when {
            list == null -> item { AdminLoading() }
            loadError != null -> item { AdminMessage(loadError.orEmpty(), true) { localReload++ } }
            visible.isEmpty() -> item { AdminMessage(if (query.isBlank()) "No hay cuentas." else "No hay usuarios que coincidan con tu búsqueda.") }
            else -> items(visible, key = { it.id }) { user ->
                AdminUserCard(user, user.id == myUid, busyUserId == user.id, accessLoading,
                    onAction = { action -> pendingAction = user to action })
            }
        }
        item { Text("Para ver un reinicio, la persona debe cerrar y volver a abrir la app. Los viajes terminados se conservan.",
            style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(AdminMuted)) }
    }

    pendingAction?.let { (user, action) ->
        val name = user.fullName.ifBlank { "esta cuenta" }
        val isMe = user.id == myUid
        val (title, message, confirm) = when (action) {
            UserAction.ResetDriver -> Triple(
                "¿Reiniciar conductor?",
                "Se borrarán los datos de conductor y el vehículo de $name. " +
                    "Podrá registrarse de nuevo como conductor y pasar por la aprobación. Sus viajes abiertos como conductor se cancelan.",
                "Reiniciar"
            )
            UserAction.ResetAccount -> Triple(
                "¿Reiniciar cuenta?",
                "Se borrarán el perfil de $name (nombre, fecha de nacimiento, teléfono) y sus datos de conductor. " +
                    "Al abrir la app tendrá que completar su perfil de nuevo. Sus viajes abiertos se cancelan.",
                "Reiniciar"
            )
            UserAction.MakeAdmin -> Triple(
                "¿Hacer administrador?",
                "$name podrá entrar a este panel: aprobar conductores, reiniciar y eliminar cuentas, y dar o quitar permisos de administrador.",
                "Hacer admin"
            )
            UserAction.RemoveAdmin -> Triple(
                "¿Quitar administrador?",
                if (isMe) "Dejarás de ver este panel. Otro administrador tendrá que devolverte el permiso."
                else "$name ya no podrá entrar a este panel.",
                "Quitar"
            )
            UserAction.Delete -> Triple(
                if (isMe) "¿Eliminar tu propia cuenta?" else "¿Eliminar cuenta?",
                (if (isMe) "Es la cuenta con la que estás usando la app: se cerrará tu sesión y perderás el acceso a este panel. " else "") +
                    "Se borrarán por completo el perfil, los datos de conductor, la foto y el inicio de sesión de $name. " +
                    "Si vuelve a entrar con el mismo teléfono o Google, será un usuario nuevo. " +
                    "Sus viajes abiertos se cancelan; los terminados se conservan como \"Cuenta eliminada\". Esto no se puede deshacer.",
                "Eliminar"
            )
        }
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    pendingAction = null
                    run(user, action)
                }) { Text(confirm, color = AppearanceColors.highlight(if (action == UserAction.MakeAdmin) AdminTeal else AdminRed)) }
            },
            dismissButton = { TextButton(onClick = { pendingAction = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
internal fun AdminUserCard(user: AdminUser, isMe: Boolean, busy: Boolean, accessLoading: Boolean,
    onAction: (UserAction) -> Unit) {
    var expanded by rememberSaveable(user.id) { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = user.photoUrl, size = 48.dp, zoomable = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.fullName.ifBlank { "Perfil sin completar" } + if (isMe) " (tú)" else "",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (user.isAdmin) AdminBadge("Administrador")
            }
        }
        if (user.email.isNotBlank()) Text(user.email, style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(AdminMuted))
        if (user.phone.isNotBlank()) Text(user.phone, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminUserStat("Como pasajero", user.ridesAsRider, Modifier.weight(1f))
            AdminUserStat("Como conductor", user.ridesAsDriver, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Conductor", style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(AdminMuted))
            if (user.driverStatus != null) AdminStatusChip(user.driverStatus) else AdminBadge("No registrado", AdminMuted, Color(0xFFF0F4F3))
        }
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (expanded) "Ocultar acceso y vinculación" else "Ver acceso y vinculación")
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
        AnimatedVisibility(expanded) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))
                AdminAccountAccessInfo(user.id, user.email, user.phone, user.accountAccess, accessLoading)
            }
        }
        if (busy) AdminLoading() else Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Settings, null, Modifier.padding(end = 8.dp))
                Text("Gestionar cuenta")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                val actions = listOf(UserAction.ResetDriver to "Reiniciar conductor", UserAction.ResetAccount to "Reiniciar cuenta",
                    (if (user.isAdmin) UserAction.RemoveAdmin else UserAction.MakeAdmin) to (if (user.isAdmin) "Quitar admin" else "Hacer admin"),
                    UserAction.Delete to "Eliminar cuenta")
                actions.forEach { (action, label) ->
                    if (action == UserAction.Delete) HorizontalDivider()
                    DropdownMenuItem(text = { Text(label, color = if (action == UserAction.Delete) AppearanceColors.highlight(AdminRed) else Color.Unspecified) },
                        enabled = action != UserAction.ResetDriver || user.driverStatus != null,
                        onClick = { menu = false; onAction(action) })
                }
            }
        }
    }
}

@Composable
private fun AdminUserStat(label: String, count: Int, modifier: Modifier) {
    androidx.compose.material3.Surface(modifier, shape = RoundedCornerShape(12.dp), color = AppearanceColors.tint(Color(0xFFF0F6F4))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = AppearanceColors.secondary(AdminMuted))
        }
    }
}

@Composable
internal fun AdminStatusChip(status: String) {
    val (label, fg, bg) = when (status) {
        "approved" -> Triple("Aprobado", Color(0xFF067647), Color(0xFFE8F6EE))
        "pending" -> Triple("Pendiente", Color(0xFF99641C), Color(0xFFFFF2DB))
        "suspended" -> Triple("Suspendido", AdminRed, Color(0xFFFFEDE9))
        else -> Triple("Rechazado", AdminRed, Color(0xFFFFEDE9))
    }
    AdminBadge(label, fg, bg)
}
