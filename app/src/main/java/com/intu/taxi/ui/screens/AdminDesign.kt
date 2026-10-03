package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

internal val AdminTeal = Color(0xFF087F7A)
internal val AdminInk = Color(0xFF163C40)
internal val AdminMuted = Color(0xFF607477)
internal val AdminRed = Color(0xFFB42318)
private val AdminColors = lightColorScheme(
    primary = AdminTeal, onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF2EE), onPrimaryContainer = AdminInk,
    secondary = AdminMuted, onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9F1F0), onSecondaryContainer = AdminInk,
    tertiary = Color(0xFF99641C), tertiaryContainer = Color(0xFFFFF2DB),
    background = Color(0xFFF2F6F5), onBackground = AdminInk,
    surface = Color.White, onSurface = AdminInk,
    surfaceVariant = Color(0xFFE9F1F0), onSurfaceVariant = AdminMuted,
    outline = Color(0xFF7E9696), outlineVariant = Color(0xFFDCE7E4),
    error = AdminRed, errorContainer = Color(0xFFFFEDE9), onErrorContainer = AdminRed,
    surfaceContainer = Color.White, surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFF5F9F8)
)

/** Scoped to the admin panel: passenger and driver screens keep their own theme. */
@Composable
internal fun AdminPanelTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (com.intu.taxi.ui.theme.LocalIntuDarkMode.current) MaterialTheme.colorScheme else AdminColors, typography = MaterialTheme.typography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp)), content = content)
}

@Composable
internal fun AdminPanelLayout(padding: PaddingValues, tab: Int, isAdmin: Boolean, locationLabel: String?,
    onBack: () -> Unit, onRefresh: () -> Unit, onLocation: () -> Unit, onTab: (Int) -> Unit,
    content: @Composable () -> Unit) {
    val darkMode = com.intu.taxi.ui.theme.LocalIntuDarkMode.current
    val appearance = com.intu.taxi.ui.theme.LocalAppearanceController.current
    val latestDark by rememberUpdatedState(appearance.darkMode)
    val view = LocalView.current
    DisposableEffect(view, darkMode) {
        val controller = (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = !darkMode
        controller?.isAppearanceLightNavigationBars = !darkMode
        onDispose {
            if (appearance.managed) {
                controller?.isAppearanceLightStatusBars = !latestDark
                controller?.isAppearanceLightNavigationBars = !latestDark
            } else {
                previousStatus?.let { controller.isAppearanceLightStatusBars = it }
                previousNavigation?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                    Column(Modifier.weight(1f)) {
                        Text("INTU · ADMIN", color = AppearanceColors.highlight(AdminTeal), style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Administración", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalIconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Actualizar") }
                }
                if (isAdmin) {
                    Surface(onClick = onLocation, shape = RoundedCornerShape(14.dp),
                        color = if (locationLabel == null) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.MyLocation, null, tint = AppearanceColors.highlight(AdminTeal), modifier = Modifier.size(20.dp))
                            Column(Modifier.weight(1f)) {
                                Text(locationLabel?.let { "Prueba en $it" } ?: "Simular mi ubicación",
                                    style = MaterialTheme.typography.labelLarge)
                                Text(if (locationLabel == null) "GPS real · elige un punto para testear" else "Ubicación simulada · tocar para cambiar",
                                    style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(AdminMuted))
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = AppearanceColors.secondary(AdminMuted))
                        }
                    }
                }
                val sections = listOf("Conductores" to Icons.Outlined.TwoWheeler, "Usuarios" to Icons.Outlined.People,
                    "Reportes" to Icons.Outlined.BugReport, "Lugares" to Icons.Outlined.Place,
                    "Notificaciones" to Icons.Outlined.Notifications)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sections.forEachIndexed { index, (label, icon) ->
                        FilterChip(selected = tab == index, onClick = { onTab(index) },
                            label = { Text(label) }, leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin-tab-$index"),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminTeal, selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White), shape = RoundedCornerShape(12.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

@Composable
internal fun AdminSectionHeading(title: String, description: String, count: Int? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (count != null) AdminBadge(count.toString())
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = AppearanceColors.secondary(AdminMuted))
    }
}

@Composable
internal fun AdminSearchField(query: String, onQuery: (String) -> Unit, label: String) {
    OutlinedTextField(query, onQuery, label = { Text(label) }, singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Outlined.Close, "Borrar búsqueda") } },
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = AppearanceColors.surface, focusedContainerColor = AppearanceColors.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant))
}

@Composable
internal fun AdminFilters(options: List<Pair<String?, String>>, selected: String?, onSelect: (String?) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminTeal, selectedLabelColor = Color.White),
                shape = RoundedCornerShape(10.dp))
        }
    }
}

@Composable
internal fun AdminCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppearanceColors.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
internal fun AdminBadge(label: String, color: Color = AdminTeal, background: Color = Color(0xFFE3F2EE)) {
    Text(label, color = AppearanceColors.highlight(color), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(AppearanceColors.tint(background), RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 5.dp))
}

@Composable
internal fun AdminDetail(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppearanceColors.secondary(AdminMuted))
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun AdminLoading() {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(28.dp), color = AppearanceColors.highlight(AdminTeal), strokeWidth = 3.dp)
        Text("Cargando…", color = AppearanceColors.secondary(AdminMuted), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun AdminMessage(text: String, isError: Boolean = false, onRetry: (() -> Unit)? = null) {
    AdminCard {
        Icon(if (isError) Icons.Outlined.CloudOff else Icons.Outlined.Inbox, null,
            tint = if (isError) AdminRed else AdminTeal, modifier = Modifier.size(28.dp))
        Text(if (isError) "No se pudo completar" else "Todo al día", style = MaterialTheme.typography.titleMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AppearanceColors.secondary(AdminMuted))
        if (onRetry != null) TextButton(onClick = onRetry) { Text("Reintentar") }
    }
}
