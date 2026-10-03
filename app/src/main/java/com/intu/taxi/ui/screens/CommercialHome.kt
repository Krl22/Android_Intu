package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intu.taxi.models.MotoOption
import com.intu.taxi.ui.components.MotoOptionArt

/** Brand-first landing; booking, saved places and search remain owned by HomeScreen. */
@Composable
internal fun CommercialHome(
    padding: PaddingValues,
    greetingName: String,
    searchActive: Boolean,
    searchContent: @Composable () -> Unit,
    shortcuts: @Composable () -> Unit,
    onTravel: () -> Unit,
    onDelivery: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().background(colors.background).testTag("commercial-home")
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
            .imePadding().verticalScroll(rememberScrollState())
    ) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(
                listOf(Color(0xFF08817E), Color(0xFF1E1F47))))
                .padding(top = if (searchActive) 16.dp else 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!searchActive) {
                Text("intu", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text(if (greetingName.isBlank()) "¡Hola!" else "¡Hola, $greetingName!",
                    color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(24.dp))
            }
            searchContent()
            if (!searchActive) {
                Spacer(Modifier.height(20.dp))
                shortcuts()
            }
        }
        if (!searchActive) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
                    shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("INTU TE CONECTA", color = colors.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Tu día se mueve\ncon Intu.", color = colors.onPrimaryContainer,
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Viaja y envía lo que necesitas.\nMás cerca de tu ciudad.",
                            color = colors.onPrimaryContainer, style = MaterialTheme.typography.bodyLarge)
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            MotoOptionArt(MotoOption.HONDA, animate = false, modifier = Modifier.size(148.dp, 94.dp))
                        }
                    }
                }
                Text("¿Qué necesitas hoy?", color = colors.onBackground,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeServiceCard(MotoOption.ANY, "Viajar", "En mototaxi", onTravel, Modifier.weight(1f))
                    HomeServiceCard(MotoOption.DELIVERY, "Enviar", "Paquetes pequeños", onDelivery, Modifier.weight(1f))
                }
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
