package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.intu.taxi.R
import com.intu.taxi.models.*
import java.util.Locale

internal fun businessImage(url: String, photo: BusinessPhoto): Any? = url.takeIf { it.isNotBlank() } ?: when (photo) {
    BusinessPhoto.CHICKEN -> R.drawable.demo_chicken
    BusinessPhoto.JUANE -> R.drawable.demo_juane
    BusinessPhoto.COFFEE -> R.drawable.demo_coffee
    BusinessPhoto.CHAUFA -> R.drawable.demo_chaufa
    BusinessPhoto.NONE -> null
}
internal fun productPrice(price: Double) = "S/ %.2f".format(Locale.US, price)

@Composable
internal fun BusinessAdCard(ad: BusinessAd, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.onSurface.luminance() > .5f
    val background = if (dark) Color(0xFF171B3D) else Color(0xFFFFFBF4)
    val photo = businessImage(ad.imageUrl, ad.demoPhoto)
    Card(onClick = onClick, modifier = modifier.testTag("business-ad-${ad.id}"),
        colors = CardDefaults.cardColors(containerColor = background), shape = RoundedCornerShape(26.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 192.dp)) {
            if (photo != null) Box(Modifier.matchParentSize()) {
                AsyncImage(photo, ad.title, contentScale = ContentScale.Crop,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.48f))
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                    0f to background, .48f to background, .67f to Color.Transparent)))
            }
            Column(Modifier.fillMaxWidth(if (photo != null) .61f else 1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("DEMO · ${ad.city.ifBlank { ad.category.label }}", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(colors.primary.copy(alpha = .12f)).padding(horizontal = 9.dp, vertical = 4.dp))
                Text(ad.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (ad.offerDetail.isNotBlank()) Text(ad.offerDetail, style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                ad.offerPrice?.let { Text(productPrice(it), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold, color = colors.onSurface) }
                Text(ad.name, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Ver menú →", color = colors.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun BusinessAdsSection(feed: BusinessFeed, error: String?, onRetry: () -> Unit, onBusiness: (BusinessAd) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.testTag("home-local-businesses"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Más de tu ciudad", color = colors.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (feed.enabled && feed.ads.isNotEmpty()) {
            Text("Sabores locales · promociones demo", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            val pager = rememberPagerState { feed.ads.size }
            HorizontalPager(pager, key = { feed.ads[it].id!! }, pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth().testTag("business-promotions-pager")) { index ->
                val ad = feed.ads[index]
                BusinessAdCard(ad, { onBusiness(ad) }, Modifier.fillMaxWidth())
            }
            if (feed.ads.size > 1) Row(Modifier.fillMaxWidth().semantics {
                contentDescription = "Promoción ${pager.currentPage + 1} de ${feed.ads.size}"
            }, horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)) {
                repeat(feed.ads.size) { index -> Box(Modifier.height(5.dp).width(if (pager.currentPage == index) 22.dp else 5.dp)
                    .clip(RoundedCornerShape(10.dp)).background(colors.primary.copy(alpha = if (pager.currentPage == index) 1f else .24f))) }
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
internal fun BusinessCartSummary(items: List<BusinessOrderItem>, modifier: Modifier = Modifier) {
    Column(modifier.testTag("business-cart-summary"), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Tu pedido demo", fontWeight = FontWeight.Bold)
        items.forEach { item -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${item.quantity} × ${item.name}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Text(productPrice(item.total.toDouble()), style = MaterialTheme.typography.bodySmall)
        } }
        Text("Productos simulados: ${productPrice(items.productsTotal())}", fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("business-products-total"))
    }
}

@Composable
internal fun BusinessAdDialog(ad: BusinessAd, onDismiss: () -> Unit, onStart: (List<BusinessOrderItem>) -> Unit) {
    // The saved cart belongs to this business and menu version only.
    var encodedQuantities by rememberSaveable(ad.id, ad.updatedAt) { mutableStateOf("") }
    val quantities = encodedQuantities.split(';').filter { it.contains(':') }.associate {
        it.substringBefore(':') to it.substringAfter(':').toInt()
    }
    val cart = businessCart(ad.menu, quantities)
    fun setQuantity(id: String, quantity: Int) {
        encodedQuantities = (quantities + (id to quantity)).filterValues { it > 0 }.entries.joinToString(";") { "${it.key}:${it.value}" }
    }
    AccountDialogLayout(eyebrow = "INTU · MENÚ DEMO", title = ad.name,
        subtitle = "${ad.city.ifBlank { ad.category.label }} · delivery en moto", onDismiss = onDismiss, footer = {
            if (cart.isNotEmpty()) Text("${cart.sumOf { it.quantity }} ${if (cart.sumOf { it.quantity } == 1) "producto" else "productos"} · ${productPrice(cart.productsTotal())}",
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 6.dp))
            Text("Productos simulados. El envío se calcula al elegir destino.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
            Button(onClick = { onStart(cart) }, enabled = cart.isNotEmpty() || ad.menu.isEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("business-start-delivery")) {
                Text(if (ad.menu.isEmpty()) "Probar delivery en moto" else if (cart.isEmpty()) "Agrega productos para continuar" else "Continuar con delivery demo")
            }
        }, content = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(ad.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(ad.description, style = MaterialTheme.typography.bodyMedium)
                if (ad.menu.isNotEmpty()) Text("Elige del menú", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ad.menu.forEach { item ->
                    val quantity = quantities[item.id] ?: 0
                    Card(Modifier.fillMaxWidth().testTag("business-product-${item.id}").intuCardBackground(),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(20.dp)) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            businessImage(item.imageUrl, item.demoPhoto)?.let { image ->
                                AsyncImage(image, item.name, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall)
                                Text(productPrice(item.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                if (!item.available) Text("No disponible", style = MaterialTheme.typography.bodySmall)
                                else Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton({ setQuantity(item.id, quantity - 1) }, enabled = quantity > 0,
                                        modifier = Modifier.testTag("business-product-minus-${item.id}")) { Icon(Icons.Outlined.Remove, "Quitar ${item.name}") }
                                    Text(quantity.toString(), modifier = Modifier.testTag("business-product-quantity-${item.id}"), fontWeight = FontWeight.Bold)
                                    IconButton({ setQuantity(item.id, quantity + 1) }, enabled = quantity < 10 && cart.sumOf { it.quantity } < 20,
                                        modifier = Modifier.testTag("business-product-plus-${item.id}")) { Icon(Icons.Outlined.Add, "Agregar ${item.name}") }
                                }
                            }
                        }
                    }
                }
                if (cart.isNotEmpty()) BusinessCartSummary(cart)
                Text("Recojo en el negocio", fontWeight = FontWeight.Bold)
                Text(ad.address)
                Text("Negocio ficticio, fotos generadas y precios de ejemplo. No se compran ni cobran productos. " +
                    "Prueba el envío de un paquete pequeño con un repartidor de prueba.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        })
}
