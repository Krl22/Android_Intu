package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.intu.taxi.models.DriverRideRequest
import com.intu.taxi.models.RidePriceOffer
import com.intu.taxi.models.parseDriverOffer
import com.intu.taxi.repositories.RidePriceOfferRepository
import com.intu.taxi.ui.formatSoles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.lifecycle.repeatOnLifecycle

@Composable
internal fun DriverPriceOfferDialog(request: DriverRideRequest, onDismiss: () -> Unit, onSent: () -> Unit,
    offerSender: (suspend (String, Double) -> Unit)? = null) {
    val repository = remember { RidePriceOfferRepository() }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var price by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Proponer otro precio") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Precio de la app: ${formatSoles(request.estimatedPrice)}")
            OutlinedTextField(price, { price = it; error = null }, label = { Text("Tu propuesta (S/)") },
                singleLine = true, enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.testTag("driver-offer-price"))
            Text("El pasajero debe aceptar tu propuesta. El viaje seguirá disponible hasta que se asigne a un conductor.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }, confirmButton = { TextButton(enabled = !busy, modifier = Modifier.testTag("driver-send-offer"), onClick = {
            val amount = try { parseDriverOffer(price, request.estimatedPrice) }
                catch (e: IllegalArgumentException) { error = e.message; return@TextButton }
            scope.launch {
                busy = true; error = null
                try { if (offerSender != null) offerSender(request.requestId, amount) else repository.propose(request.requestId, amount); onSent() }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = "No se pudo enviar. Revisa si la solicitud sigue disponible o si el administrador desactivó las propuestas." }
                finally { busy = false }
            }
        }) { Text(if (busy) "Enviando…" else "Enviar propuesta") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
internal fun PassengerPriceOffers(rideId: String, onCancelRide: () -> Unit, onAccepted: () -> Unit,
    offersLoader: (suspend (String) -> List<RidePriceOffer>)? = null,
    offerResponder: (suspend (String, Boolean) -> Unit)? = null) {
    val repository = remember { RidePriceOfferRepository() }
    var offers by remember(rideId) { mutableStateOf(emptyList<RidePriceOffer>()) }
    var error by remember(rideId) { mutableStateOf<String?>(null) }
    var hidden by remember(rideId) { mutableStateOf(false) }
    var busy by remember(rideId) { mutableStateOf(false) }
    var selected by remember(rideId) { mutableStateOf<RidePriceOffer?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(rideId, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                try { offers = offersLoader?.invoke(rideId) ?: repository.list(rideId) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { /* Keep the last proposals; responses always validate availability on the server. */ }
                kotlinx.coroutines.delay(2500)
            }
        }
    }
    LaunchedEffect(offers.map { it.id }) { hidden = false }
    fun respond(offer: RidePriceOffer, accept: Boolean) {
        if (busy) return
        scope.launch {
            busy = true; error = null
            try {
                if (offerResponder != null) offerResponder(offer.id, accept) else repository.respond(offer.id, accept)
                selected = null
                offers = offers.filter { it.id != offer.id }
                if (accept) onAccepted()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                error = "No se pudo confirmar la propuesta. Revisa tu conexión y si el conductor sigue disponible."; selected = null
                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
            }
            finally { busy = false }
        }
    }
    if (offers.isNotEmpty()) {
        if (hidden) Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
            Button({ hidden = false }, modifier = Modifier.padding(top = 70.dp)) { Text("Ver propuestas de precio (${offers.size})") }
        } else if (selected == null) AlertDialog(onDismissRequest = { if (!busy) hidden = true }, title = { Text("Propuestas de conductores") },
            text = { Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Puedes aceptar otro precio o continuar buscando al precio de la app.")
                offers.forEach { offer ->
                    Card(Modifier.fillMaxWidth().testTag("passenger-offer-${offer.id}")) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(offer.driverName, style = MaterialTheme.typography.titleMedium)
                            Text(offer.vehicle)
                            Text("Propuesta: ${formatSoles(offer.amount)} · App: ${formatSoles(offer.appFare)}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton({ respond(offer, false) }, enabled = !busy,
                                    modifier = Modifier.testTag("reject-offer-${offer.id}")) { Text("Rechazar") }
                                Button({ selected = offer }, enabled = !busy,
                                    modifier = Modifier.testTag("accept-offer-${offer.id}")) { Text("Aceptar precio") }
                            }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } }, confirmButton = { TextButton({ hidden = true }, enabled = !busy) { Text("Seguir buscando") } },
            dismissButton = { TextButton(onCancelRide, enabled = !busy) { Text("Cancelar viaje") } })
    }
    selected?.let { offer -> AlertDialog(onDismissRequest = { if (!busy) selected = null },
        title = { Text("Confirmar precio") }, text = { Text("Tu viaje con ${offer.driverName} costará ${formatSoles(offer.amount)}. Al confirmar se asignará a este conductor.") },
        confirmButton = { TextButton({ respond(offer, true) }, enabled = !busy, modifier = Modifier.testTag("confirm-offer")) {
            Text(if (busy) "Confirmando…" else "Confirmar ${formatSoles(offer.amount)}")
        } }, dismissButton = { TextButton({ selected = null }, enabled = !busy) { Text("Volver") } }) }
}
