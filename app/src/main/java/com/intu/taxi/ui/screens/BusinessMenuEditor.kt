package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.intu.taxi.models.*
import com.intu.taxi.repositories.*
import org.json.JSONArray

@Composable
internal fun BusinessPhotoPicker(selected: String, enabled: Boolean = true, onSelect: (String) -> Unit) {
    Text("Foto demo incluida", style = MaterialTheme.typography.labelLarge)
    Column {
        BusinessPhoto.entries.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { photo -> FilterChip(selected == photo.code, { onSelect(photo.code) }, { Text(photo.label) }, enabled = enabled) }
        } }
    }
}

@Composable
internal fun BusinessMenuEditor(initial: List<BusinessMenuItem>, onDismiss: () -> Unit, onSave: (List<BusinessMenuItem>) -> Unit) {
    var encoded by rememberSaveable { mutableStateOf(businessMenuJson(initial).toString()) }
    val menu = parseBusinessMenu(JSONArray(encoded))
    var editing by remember { mutableStateOf<BusinessMenuItem?>(null) }
    editing?.let { product ->
        BusinessProductEditor(product, { editing = null }, { saved ->
            encoded = businessMenuJson(if (menu.any { it.id == saved.id }) menu.map { if (it.id == saved.id) saved else it } else menu + saved).toString()
            editing = null
        })
        return
    }
    AccountDialogLayout("INTU · MENÚ DEMO", "Productos del negocio", "Hasta 12 productos. Los precios son de prueba.", onDismiss,
        footer = { Button({ onSave(menu) }, modifier = Modifier.fillMaxWidth()) { Text("Usar este menú") } },
        content = { Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            menu.forEach { item -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text("${productPrice(item.price)} · ${if (item.available) "Disponible" else "No disponible"}")
                Row {
                    TextButton({ editing = item }) { Text("Editar producto") }
                    TextButton({ encoded = businessMenuJson(menu.filterNot { it.id == item.id }).toString() }) { Text("Quitar") }
                }
            } } }
            Button({ editing = BusinessMenuItem() }, enabled = menu.size < 12, modifier = Modifier.testTag("admin-add-menu-product")) { Text("Agregar producto") }
            Text("Los cambios del menú se publican cuando guardas el anuncio.", style = MaterialTheme.typography.bodySmall)
        } })
}

@Composable
internal fun BusinessProductEditor(product: BusinessMenuItem, onDismiss: () -> Unit, onSave: (BusinessMenuItem) -> Unit) {
    var name by rememberSaveable(product.id) { mutableStateOf(product.name) }
    var description by rememberSaveable(product.id) { mutableStateOf(product.description) }
    var price by rememberSaveable(product.id) { mutableStateOf(if (product.price > 0) "%.2f".format(java.util.Locale.US, product.price) else "") }
    var image by rememberSaveable(product.id) { mutableStateOf(product.imageUrl) }
    var photo by rememberSaveable(product.id) { mutableStateOf(product.demoPhoto.code) }
    var available by rememberSaveable(product.id) { mutableStateOf(product.available) }
    var error by remember { mutableStateOf<String?>(null) }
    AccountDialogLayout("INTU · MENÚ DEMO", "Editar producto", "Imagen, precio y disponibilidad", onDismiss,
        footer = { Button({
            runCatching { product.copy(name = name, description = description, price = price.replace(',', '.').toDoubleOrNull() ?: Double.NaN,
                imageUrl = image, demoPhoto = businessPhoto(photo), available = available).normalized() }
                .onSuccess(onSave).onFailure { error = it.message }
        }, modifier = Modifier.fillMaxWidth()) { Text("Guardar producto") } },
        content = { Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it.take(80) }, label = { Text("Nombre del producto") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it.take(160) }, label = { Text("Descripción del producto") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(price, { price = it.take(10) }, label = { Text("Precio del producto") }, prefix = { Text("S/ ") }, modifier = Modifier.fillMaxWidth())
            BusinessPhotoPicker(photo) { photo = it }
            OutlinedTextField(image, { image = it.take(1000) }, label = { Text("Imagen HTTPS del producto (opcional)") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().toggleable(available, role = Role.Switch, onValueChange = { available = it }), verticalAlignment = Alignment.CenterVertically) {
                Text("Disponible", modifier = Modifier.weight(1f)); Switch(available, null)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } })
}
