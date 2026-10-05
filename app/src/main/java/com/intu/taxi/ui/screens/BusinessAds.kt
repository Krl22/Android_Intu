package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.intu.taxi.models.BusinessAd
import com.intu.taxi.models.BusinessCategory
import com.intu.taxi.models.BusinessFeed

@Composable
internal fun BusinessAdCard(ad: BusinessAd, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(onClick = onClick, modifier = modifier.testTag("business-ad-${ad.id}").intuCardBackground(emphasized = true),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(24.dp)) {
        if (ad.imageUrl.isNotBlank()) AsyncImage(model = ad.imageUrl, contentDescription = ad.name,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(100.dp))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(when (ad.category) {
                    BusinessCategory.FOOD -> Icons.Outlined.Restaurant
                    BusinessCategory.SHOP -> Icons.Outlined.Storefront
                    BusinessCategory.PHARMACY -> Icons.Outlined.LocalPharmacy
                }, null, tint = colors.primary)
                Text(ad.category.label, color = colors.primary, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text("DEMO", color = colors.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Text(ad.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(ad.name, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Ver negocio →", color = colors.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun BusinessAdsSection(feed: BusinessFeed, error: String?, onRetry: () -> Unit, onBusiness: (BusinessAd) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.testTag("home-local-businesses"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Más de tu ciudad", color = colors.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (feed.enabled && feed.ads.isNotEmpty()) {
            Text("Publicidad de prueba · delivery en moto", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(feed.ads, key = { it.id!! }) { ad -> BusinessAdCard(ad, { onBusiness(ad) }, Modifier.width(280.dp)) }
            }
            if (error != null) TextButton(onClick = onRetry) { Text("No se pudo actualizar · Reintentar") }
        } else Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().intuCardBackground(emphasized = true)) {
            Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Storefront, null, tint = colors.primary, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (error != null) "No se pudieron cargar los negocios." else if (feed.enabled)
                        "Pronto habrá novedades por aquí." else "Pronto, descubre negocios locales con Intu.", color = colors.onSurfaceVariant)
                    if (error != null) TextButton(onClick = onRetry) { Text("Reintentar") }
                }
            }
        }
    }
}

@Composable
internal fun BusinessAdDialog(ad: BusinessAd, onDismiss: () -> Unit, onStart: () -> Unit) {
    AccountDialogLayout(eyebrow = "INTU · NEGOCIO DEMO", title = ad.name, subtitle = ad.category.label,
        onDismiss = onDismiss, footer = {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("business-start-delivery")) {
                Text("Probar delivery en moto")
            }
        }, content = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BusinessAdCard(ad, {}, Modifier.fillMaxWidth())
                Text(ad.description, style = MaterialTheme.typography.bodyLarge)
                Text("Recojo en el negocio", fontWeight = FontWeight.Bold)
                Text(ad.address)
                Text("Este negocio es ficticio. Puedes probar el transporte de un paquete pequeño con un repartidor de prueba. " +
                    "El pago corresponde solo al envío; no se compran ni cobran productos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        })
}
