package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import android.Manifest
import android.content.Intent
import android.app.NotificationManager
import android.os.Build
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsPaused
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.push.AdminNotificationPreferences
import com.intu.taxi.push.PushNotifications
import com.intu.taxi.repositories.AdminNotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AdminNotificationSettings(reloadKey: Int) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val repository = remember { AdminNotificationRepository() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var settings by remember(uid) { mutableStateOf<AdminNotificationPreferences?>(null) }
    var busy by remember(uid) { mutableStateOf(false) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var notificationsAllowed by remember { mutableStateOf(true) }
    fun checkPermission() {
        val channelAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.getSystemService(NotificationManager::class.java)?.getNotificationChannel(PushNotifications.ADMIN_CHANNEL)
                ?.importance != NotificationManager.IMPORTANCE_NONE else true
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            channelAllowed
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        PushNotifications.createChannel(context)
        checkPermission()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) checkPermission() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { checkPermission() }
    fun openAndroidSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, PushNotifications.ADMIN_CHANNEL)
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
        context.startActivity(intent)
    }
    LaunchedEffect(uid, reloadKey, retry) {
        settings = null; error = null
        try { settings = repository.get() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "No se pudieron cargar tus preferencias." }
    }
    AdminNotificationSettingsContent(settings, busy, error, notificationsAllowed,
        onChange = { value ->
            if (!busy) {
                busy = true; error = null
                scope.launch {
                    try { settings = repository.save(value) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = e.message ?: "No se pudo guardar. Intenta de nuevo." }
                    finally { busy = false }
                }
            }
        }, onRetry = { retry++ }, onPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else openAndroidSettings()
        }, onAndroidSettings = ::openAndroidSettings)
}

@Composable
fun AdminNotificationSettingsContent(settings: AdminNotificationPreferences?, busy: Boolean, error: String?,
    notificationsAllowed: Boolean, onChange: (AdminNotificationPreferences) -> Unit,
    onRetry: () -> Unit, onPermission: () -> Unit, onAndroidSettings: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            AdminSectionHeading("Notificaciones", "Sigue la actividad de tus testers, incluso con Intu minimizada. Estas preferencias se aplican a todos los dispositivos de tu cuenta.")
        }
        if (!notificationsAllowed) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Android está bloqueando los avisos", style = MaterialTheme.typography.titleMedium)
                    Text("Activa el permiso de notificaciones y el canal Actividad de Intu para recibirlos en este teléfono.")
                    OutlinedButton(onClick = onPermission) { Text("Habilitar en Android") }
                    TextButton(onClick = onAndroidSettings) { Text("Abrir ajustes de notificaciones") }
                }
            }
        }
        if (settings == null) item {
            if (error == null) AdminLoading() else AdminMessage(error, true, onRetry)
        } else {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (settings.enabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsPaused, null, tint = AdminTeal)
                        AdminBadge(if (settings.enabled) "Avisos activos" else "Avisos en pausa")
                    }
                    NotificationToggle("Recibir actividad", "Pausa o reanuda todos los avisos administrativos.", settings.enabled, !busy) {
                        onChange(settings.copy(enabled = it))
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("QUÉ QUIERES RECIBIR", style = MaterialTheme.typography.labelSmall, color = AppearanceColors.secondary(AdminMuted), fontWeight = FontWeight.Bold)
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column {
                        val enabled = settings.enabled && !busy
                        NotificationToggle("Usuarios nuevos", "Cuando alguien crea su perfil en Intu.", settings.newUsers, enabled) {
                            onChange(settings.copy(newUsers = it))
                        }
                        HorizontalDivider()
                        NotificationToggle("Solicitudes de viaje y envío", "Cuando alguien pide una moto o un reparto.", settings.rideRequests, enabled) {
                            onChange(settings.copy(rideRequests = it))
                        }
                        HorizontalDivider()
                        NotificationToggle("Postulaciones de conductores", "Cuando llega una solicitud para trabajar en Intu.", settings.driverApplications, enabled) {
                            onChange(settings.copy(driverApplications = it))
                        }
                        HorizontalDivider()
                        NotificationToggle("Reportes de errores", "Cuando un usuario envía un reporte desde su cuenta.", settings.bugReports, enabled) {
                            onChange(settings.copy(bugReports = it))
                        }
                    }
                }
            }
            }
            item {
                if (busy) Text("Guardando…", color = AdminTeal)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                Text("Desactivar estos avisos no cambia las notificaciones de tus propios viajes. Solo se avisa de actividad nueva; los cambios de un perfil no generan otro aviso.",
                    style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(AdminMuted))
            }
        }
    }
}

@Composable
private fun NotificationToggle(title: String, description: String, checked: Boolean, enabled: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(AdminMuted))
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = title })
    }
}
