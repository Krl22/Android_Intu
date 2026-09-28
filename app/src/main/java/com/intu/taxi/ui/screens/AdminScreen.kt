package com.intu.taxi.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

private val AdminTeal = Color(0xFF08817E)
private val AdminIndigo = Color(0xFF1E1F47)
private val AdminMuted = Color(0xFF5F6570)
private val AdminRed = Color(0xFFB42318)
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
fun AdminScreen(padding: PaddingValues, onBack: () -> Unit, onOwnAccountDeleted: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var reloadKey by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7F9))
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = AdminIndigo)
            }
            Text(
                "Administración",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AdminIndigo,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { reloadKey++ }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = AdminTeal)
            }
        }
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = AdminTeal,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(positions[tab]), color = AdminTeal)
            }
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Conductores") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Usuarios") })
        }
        if (tab == 0) DriversTab(reloadKey) else UsersTab(reloadKey, onOwnAccountDeleted)
    }
}

@Composable
private fun DriversTab(reloadKey: Int) {
    val repo = remember { AdminRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf<String?>("pending") }
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

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        StatusFilters.forEach { (value, label) ->
            FilterChip(
                selected = filter == value,
                onClick = { filter = value },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AdminTeal,
                    selectedLabelColor = Color.White
                )
            )
        }
    }

    val list = drivers
    when {
        list == null -> AdminLoading()
        loadError != null -> AdminMessage(loadError.orEmpty())
        list.isEmpty() -> AdminMessage(
            if (filter == "pending") "No hay conductores esperando aprobación." else "No hay conductores en esta lista."
        )
        else -> LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list, key = { it.id }) { driver ->
                AdminDriverCard(
                    driver = driver,
                    busy = busyDriverId == driver.id,
                    onApprove = { changeStatus(driver, "approved") },
                    onReject = { pendingConfirmation = driver to "rejected" },
                    onSuspend = { pendingConfirmation = driver to "suspended" },
                    onBackToPending = { changeStatus(driver, "pending") }
                )
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
                }) { Text(if (rejecting) "Rechazar" else "Suspender", color = AdminRed) }
            },
            dismissButton = { TextButton(onClick = { pendingConfirmation = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun AdminDriverCard(
    driver: AdminDriver,
    busy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onSuspend: () -> Unit,
    onBackToPending: () -> Unit
) {
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = driver.photoUrl, size = 52.dp, zoomable = true, contentDescription = "Foto del conductor")
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(driver.fullName.ifBlank { "Sin nombre" }, fontWeight = FontWeight.SemiBold, color = AdminIndigo)
                if (driver.phone.isNotBlank()) Text(driver.phone, style = MaterialTheme.typography.bodySmall, color = AdminMuted)
                if (driver.email.isNotBlank()) Text(driver.email, style = MaterialTheme.typography.bodySmall, color = AdminMuted)
            }
            AdminStatusChip(driver.status)
        }
        Spacer(Modifier.height(10.dp))
        AdminDetail("DNI", driver.documentNumber)
        AdminDetail("Licencia", driver.licenseNumber)
        AdminDetail("Mototaxi", listOf(driver.vehicle, driver.plate).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Sin vehículo" })
        driver.registeredAt?.let {
            AdminDetail("Registrado", it.atZone(ZoneId.systemDefault()).format(RegisteredFormat))
        }
        if (driver.ratingCount > 0) {
            AdminDetail("Calificación", String.format(Locale.US, "%.2f (%d)", driver.rating, driver.ratingCount))
        }
        Spacer(Modifier.height(12.dp))
        when {
            busy -> AdminLoading()
            driver.status == "pending" -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminRed)
                ) { Text("Rechazar") }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminTeal)
                ) { Text("Aprobar") }
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBackToPending, modifier = Modifier.weight(1f)) {
                    Text("Pasar a pendiente")
                }
                if (driver.status == "approved") {
                    OutlinedButton(
                        onClick = onSuspend,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminRed)
                    ) { Text("Suspender") }
                } else {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminTeal)
                    ) { Text("Aprobar") }
                }
            }
        }
    }
}

/** Acciones sobre una cuenta que se confirman antes de hacerlas. */
private enum class UserAction { ResetDriver, ResetAccount, MakeAdmin, RemoveAdmin, Delete }

@Composable
private fun UsersTab(reloadKey: Int, onOwnAccountDeleted: () -> Unit) {
    val repo = remember { AdminRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid }
    var users by remember { mutableStateOf<List<AdminUser>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var localReload by remember { mutableIntStateOf(0) }
    var busyUserId by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<Pair<AdminUser, UserAction>?>(null) }

    LaunchedEffect(reloadKey, localReload) {
        users = null
        loadError = null
        runCatching { repo.listUsers() }
            .onSuccess { users = it }
            .onFailure {
                loadError = it.message ?: "No se pudo cargar la lista"
                users = emptyList()
            }
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

    Text(
        "Reinicia una cuenta para repetir las pruebas, o elimínala por completo (también su inicio de sesión) " +
            "para entrar como usuario nuevo. Los viajes se conservan. " +
            "La persona debe cerrar y volver a abrir la app para ver el cambio.",
        style = MaterialTheme.typography.bodySmall,
        color = AdminMuted,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )

    val list = users
    when {
        list == null -> AdminLoading()
        loadError != null -> AdminMessage(loadError.orEmpty())
        list.isEmpty() -> AdminMessage("No hay cuentas.")
        else -> LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list, key = { it.id }) { user ->
                AdminUserCard(
                    user = user,
                    isMe = user.id == myUid,
                    busy = busyUserId == user.id,
                    onAction = { action -> pendingAction = user to action }
                )
            }
        }
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
                }) { Text(confirm, color = if (action == UserAction.MakeAdmin) AdminTeal else AdminRed) }
            },
            dismissButton = { TextButton(onClick = { pendingAction = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun AdminUserCard(
    user: AdminUser,
    isMe: Boolean,
    busy: Boolean,
    onAction: (UserAction) -> Unit
) {
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = user.photoUrl, size = 48.dp, zoomable = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    user.fullName.ifBlank { "Perfil sin completar" } + if (isMe) " (tú)" else "",
                    fontWeight = FontWeight.SemiBold,
                    color = AdminIndigo
                )
                if (user.phone.isNotBlank()) Text(user.phone, style = MaterialTheme.typography.bodySmall, color = AdminMuted)
                if (user.email.isNotBlank()) Text(user.email, style = MaterialTheme.typography.bodySmall, color = AdminMuted)
            }
            if (user.isAdmin) {
                Text(
                    "Admin",
                    style = MaterialTheme.typography.labelMedium,
                    color = AdminIndigo,
                    modifier = Modifier
                        .background(Color(0xFFEEF0FB), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        AdminDetail(
            "Conductor",
            when (user.driverStatus) {
                null -> "No registrado"
                "approved" -> "Aprobado"
                "pending" -> "Pendiente"
                "suspended" -> "Suspendido"
                else -> "Rechazado"
            }
        )
        AdminDetail("Viajes", "${user.ridesAsRider} como pasajero · ${user.ridesAsDriver} como conductor")
        Spacer(Modifier.height(12.dp))
        if (busy) {
            AdminLoading()
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onAction(UserAction.ResetDriver) },
                    enabled = user.driverStatus != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Reiniciar conductor") }
                OutlinedButton(
                    onClick = { onAction(UserAction.ResetAccount) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminRed)
                ) { Text("Reiniciar cuenta") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onAction(if (user.isAdmin) UserAction.RemoveAdmin else UserAction.MakeAdmin) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminIndigo)
                ) { Text(if (user.isAdmin) "Quitar admin" else "Hacer admin") }
                Button(
                    onClick = { onAction(UserAction.Delete) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminRed)
                ) { Text("Eliminar cuenta") }
            }
        }
    }
}

@Composable
private fun AdminCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun AdminDetail(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, color = AdminMuted)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1C1C1E))
    }
}

@Composable
private fun AdminStatusChip(status: String) {
    val (label, fg, bg) = when (status) {
        "approved" -> Triple("Aprobado", Color(0xFF067647), Color(0xFFE8F6EE))
        "pending" -> Triple("Pendiente", Color(0xFFB45309), Color(0xFFFEF3E2))
        "suspended" -> Triple("Suspendido", AdminRed, Color(0xFFFDECEA))
        else -> Triple("Rechazado", AdminRed, Color(0xFFFDECEA))
    }
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun AdminLoading() {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AdminTeal, strokeWidth = 3.dp)
    }
}

@Composable
private fun AdminMessage(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = AdminMuted)
    }
}
