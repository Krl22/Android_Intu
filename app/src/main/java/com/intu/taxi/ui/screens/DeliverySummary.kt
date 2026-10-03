package com.intu.taxi.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intu.taxi.models.DeliveryDetails
import com.intu.taxi.models.DriverRideRequest

@Composable
internal fun DeliverySummary(details: DeliveryDetails, allowCall: Boolean = false) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Envío · ${details.description}", fontWeight = FontWeight.SemiBold)
        Text("Recibe: ${details.recipientName}", style = MaterialTheme.typography.bodyMedium)
        Text(details.recipientPhone.removePrefix("+51"), style = MaterialTheme.typography.bodySmall)
        if (details.pickupReference.isNotBlank()) Text("Recojo: ${details.pickupReference}", style = MaterialTheme.typography.bodySmall)
        if (details.deliveryReference.isNotBlank()) Text("Entrega: ${details.deliveryReference}", style = MaterialTheme.typography.bodySmall)
        Text(if (details.paymentCollected) "Transporte pagado" else details.payer.label,
            style = MaterialTheme.typography.bodySmall, color = AccountTeal)
        if (allowCall) OutlinedButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${details.recipientPhone}")))
        }) { Text("Llamar a quien recibe") }
    }
}

@Composable
internal fun DeliveryPaymentDialog(
    request: DriverRideRequest,
    pickup: Boolean,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var received by remember(request.requestId) { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (pickup) "Recoger el paquete" else "Confirmar entrega") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (pickup) "Confirma que recibiste el paquete y el pago del transporte de quien lo envía."
                    else "Confirma que entregaste el paquete a ${request.delivery?.recipientName.orEmpty()}.")
                Text("${com.intu.taxi.ui.formatSoles(request.estimatedPrice)} · ${if (request.paymentMethod == "efectivo") "Efectivo" else "Yape / Plin"}",
                    fontWeight = FontWeight.SemiBold)
                Row {
                    Checkbox(checked = received, onCheckedChange = { received = it }, enabled = !busy)
                    Text(if (request.delivery?.paymentCollected == true) "Confirmo la entrega del paquete. El transporte ya está pagado."
                        else if (pickup) "Recibí el paquete y el pago del transporte."
                        else "Entregué el paquete y recibí el pago del transporte.", modifier = Modifier.padding(top = 10.dp))
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = received && !busy) {
            Text(if (busy) "Confirmando…" else if (pickup) "Iniciar envío" else "Finalizar envío")
        } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Volver") } }
    )
}
