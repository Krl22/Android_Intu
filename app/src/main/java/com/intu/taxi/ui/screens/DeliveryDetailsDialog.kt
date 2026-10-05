package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intu.taxi.auth.PhoneFormatter
import com.intu.taxi.models.DeliveryDetails
import com.intu.taxi.models.DeliveryPayer
import com.intu.taxi.ui.formatSoles

@Composable
internal fun DeliveryDetailsDialog(
    fare: Double,
    onDismiss: () -> Unit,
    onConfirm: (DeliveryDetails) -> Unit,
    initial: DeliveryDetails? = null,
    allowedPayers: List<DeliveryPayer> = DeliveryPayer.entries,
    busy: Boolean = false,
    error: String? = null,
    businessName: String? = null
) {
    var name by rememberSaveable { mutableStateOf(initial?.recipientName.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(PhoneFormatter.nationalDigits("+51", initial?.recipientPhone.orEmpty())) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var pickupReference by rememberSaveable { mutableStateOf(initial?.pickupReference.orEmpty()) }
    var deliveryReference by rememberSaveable { mutableStateOf(initial?.deliveryReference.orEmpty()) }
    var smallPackage by rememberSaveable { mutableStateOf(initial?.smallPackageConfirmed ?: false) }
    var payerCode by rememberSaveable { mutableStateOf((initial?.payer?.takeIf { it in allowedPayers } ?: allowedPayers.first()).code) }
    val details = DeliveryDetails(name, phone, description, pickupReference, deliveryReference,
        DeliveryPayer.entries.first { it.code == payerCode }, smallPackageConfirmed = smallPackage)
    val valid = runCatching { details.normalized() }.isSuccess

    AccountDialogLayout(
        eyebrow = if (businessName != null) "INTU · DELIVERY DEMO" else "INTU · MOTO LINEAL",
        title = "Datos del envío",
        subtitle = businessName?.let { "Recojo en $it. El pago es solo por transporte." }
            ?: "Indica qué enviarás y quién recibirá el paquete.",
        onDismiss = onDismiss,
        dismissEnabled = !busy,
        footer = {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Transporte estimado", style = MaterialTheme.typography.bodyMedium)
                Text(formatSoles(fare), fontWeight = FontWeight.Bold, color = AccountTeal)
            }
            Button(onClick = { onConfirm(details.normalized()) }, enabled = valid && !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(if (busy) "Solicitando…" else "Solicitar envío")
            }
        },
        content = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(description, { description = it.take(280) }, label = { Text("¿Qué enviarás?") },
                    placeholder = { Text("Ej. documentos en un sobre") }, minLines = 2, enabled = !busy,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(name, { name = it.take(100) }, label = { Text("Nombre de quien recibe") },
                    singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = PhoneFormatter.nationalDigits("+51", it).take(9) },
                    label = { Text("Celular de quien recibe") }, prefix = { Text("+51 ") },
                    supportingText = { Text("Celular peruano de 9 dígitos, empieza con 9") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !busy, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pickupReference, { pickupReference = it.take(200) },
                    label = { Text("Referencia de recojo (opcional)") }, placeholder = { Text("Ej. puerta azul junto a la farmacia") },
                    enabled = !busy, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(deliveryReference, { deliveryReference = it.take(200) },
                    label = { Text("Referencia de entrega (opcional)") }, placeholder = { Text("Ej. recepción, primer piso") },
                    enabled = !busy, modifier = Modifier.fillMaxWidth())
                Text("¿Quién pagará el transporte?", fontWeight = FontWeight.SemiBold)
                allowedPayers.forEach { payer ->
                    Row(Modifier.fillMaxWidth().selectable(selected = payerCode == payer.code, enabled = !busy,
                        onClick = { payerCode = payer.code }).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = payerCode == payer.code, onClick = null, enabled = !busy)
                        Text(payer.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().selectable(selected = smallPackage, enabled = !busy,
                    onClick = { smallPackage = !smallPackage })) {
                    Checkbox(checked = smallPackage, onCheckedChange = null, enabled = !busy)
                    Text("Confirmo que es un paquete pequeño, apto para llevar en moto. Solicito solo transporte, sin compras ni cobro de productos.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
