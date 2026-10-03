package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intu.taxi.models.MotoOption
import com.intu.taxi.data.SavedPlace
import com.intu.taxi.ui.components.MotoOptionArt

/** Brand-first landing; booking, saved places and search remain owned by HomeScreen. */
@Composable
internal fun CommercialHome(
    padding: PaddingValues,
    greetingName: String,
    searchActive: Boolean,
    searchContent: @Composable () -> Unit,
    destinations: @Composable () -> Unit,
    onPickMap: () -> Unit,
    onTravel: () -> Unit,
    onDelivery: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val glowHeight = with(LocalDensity.current) { 420.dp.toPx() }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(colors.primaryContainer, colors.background), endY = glowHeight)).testTag("commercial-home")
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
            .imePadding().verticalScroll(rememberScrollState())
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .padding(top = if (searchActive) 16.dp else 24.dp, bottom = 8.dp)
        ) {
            if (!searchActive) {
                Text("intu", color = colors.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Text(if (greetingName.isBlank()) "¡Hola!" else "¡Hola, $greetingName!",
                    color = colors.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Tu día se mueve\ncon Intu.", color = colors.onBackground,
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Viaja y envía lo que necesitas. Más cerca de tu ciudad.",
                        modifier = Modifier.weight(1f), color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium)
                    MotoOptionArt(MotoOption.HONDA, animate = false, modifier = Modifier.size(112.dp, 72.dp))
                }
                Spacer(Modifier.height(24.dp))
            }
            searchContent()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onPickMap, modifier = Modifier.testTag("home-pick-destination")) {
                    Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Elegir en mapa")
                }
            }
        }
        if (!searchActive) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("¿Qué necesitas hoy?", color = colors.onBackground,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeServiceCard(MotoOption.ANY, "Viajar", "En mototaxi", onTravel, Modifier.weight(1f))
                    HomeServiceCard(MotoOption.DELIVERY, "Enviar", "Paquetes pequeños", onDelivery, Modifier.weight(1f))
                }
                destinations()
                Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                    shape = RoundedCornerShape(24.dp), modifier = Modifier.testTag("home-local-businesses")) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Storefront, contentDescription = null, tint = colors.primary)
                        Text("Más de tu ciudad", color = colors.onSurface,
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Pronto, descubre negocios locales con Intu.", color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeDestinations(
    places: List<SavedPlace>,
    onSavedPlaceClick: (SavedPlace) -> Unit,
    onConfigurePlace: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tus lugares", style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onConfigurePlace, modifier = Modifier.testTag("home-manage-places")) { Text("Gestionar") }
        }
        Card(shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
            val destinations = listOf("casa" to "Casa", "trabajo" to "Trabajo") +
                places.filterNot { it.id == "casa" || it.id == "trabajo" }.map { it.id to it.name }
            destinations.forEachIndexed { index, (id, name) ->
                val place = places.find { it.id == id }
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(place?.address?.takeIf { it.isNotBlank() } ?: "Agregar dirección") },
                    leadingContent = { Icon(when (id) {
                        "casa" -> Icons.Outlined.Home
                        "trabajo" -> Icons.Outlined.Work
                        else -> Icons.Outlined.Place
                    }, contentDescription = null, tint = colors.primary) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                        tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    colors = ListItemDefaults.colors(containerColor = colors.surfaceContainer,
                        headlineColor = colors.onSurface, supportingColor = colors.onSurfaceVariant),
                    modifier = Modifier.testTag("home-place-$id").clickable {
                        if (place != null) onSavedPlaceClick(place) else onConfigurePlace()
                    }
                )
                if (index < destinations.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.outlineVariant)
            }
        }
    }
}

@Composable
private fun HomeServiceCard(option: MotoOption, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(onClick = onClick, modifier = modifier.testTag(if (option.delivery) "home-start-delivery" else "home-start-trip"),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MotoOptionArt(option, animate = false, modifier = Modifier.fillMaxWidth().height(70.dp))
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.primary)
        }
    }
}
