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
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.models.FareSettings
import com.intu.taxi.repositories.FareSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
internal fun AdminFareSettings(reloadKey: Int) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val repository = remember { FareSettingsRepository() }
    val scope = rememberCoroutineScope()
    var settings by remember(uid) { mutableStateOf<FareSettings?>(null) }
    var busy by remember(uid) { mutableStateOf(false) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var saved by remember(uid) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(uid, reloadKey, retry) {
        settings = null; error = null; saved = false
        try { settings = repository.get() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = "No se pudieron cargar las tarifas. Intenta de nuevo." }
    }
    AdminFareSettingsContent(settings, busy, error, saved, onRetry = { retry++ }, onSave = { values ->
        if (!busy) scope.launch {
            busy = true; error = null; saved = false
            try { settings = repository.save(values); saved = true }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "No se pudieron guardar las tarifas. Intenta de nuevo." }
            finally { busy = false }
        }
    }, onEdit = { saved = false; error = null })
}

@Composable
internal fun AdminFareSettingsContent(settings: FareSettings?, busy: Boolean, error: String?, saved: Boolean,
    onRetry: () -> Unit, onSave: (FareSettings) -> Unit, onEdit: () -> Unit = {}) {
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val labels = listOf("Base por viaje (S/)", "Por kilómetro (S/)", "Por minuto estimado (S/)",
        "Tarifa mínima (S/)", "Recargo Honda (%)", "Redondeo del precio (S/)")
    var fields by remember(settings) { mutableStateOf(settings?.let {
        listOf(it.baseFare, it.perKm, it.perMinute, it.minimumFare, it.hondaPremiumPercent, it.roundingStep)
            .map { value -> BigDecimal.valueOf(value).stripTrailingZeros().toPlainString() }
    }.orEmpty()) }
    var validationError by remember(settings) { mutableStateOf<String?>(null) }
    var offersEnabled by remember(settings) { mutableStateOf(settings?.driverPriceOffersEnabled ?: false) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AdminSectionHeading("Tarifas de mototaxi", "Configura el precio de los viajes en Intu.")
        if (settings == null) {
            if (error == null) CircularProgressIndicator()
            else { Text(error, color = MaterialTheme.colorScheme.error); Button(onRetry) { Text("Reintentar") } }
        } else {
            AdminCard {
                labels.forEachIndexed { index, label ->
                    OutlinedTextField(value = fields[index], onValueChange = { value ->
                        fields = fields.toMutableList().also { it[index] = value }
                        validationError = null; onEdit()
                    }, label = { Text(label) }, enabled = !busy, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("fare-field-$index"))
                }
                Text("Se suma la base, la distancia y el tiempo estimado, respetando el mínimo. El recargo Honda se aplica después. " +
                    "El redondeo indica el incremento del precio: 0.10 equivale a 10 céntimos.",
                    style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Permitir propuestas de precio", style = MaterialTheme.typography.titleMedium)
                        Text("El conductor propone otro importe y el pasajero debe aceptarlo antes de asignar el viaje.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(offersEnabled, onCheckedChange = { offersEnabled = it; onEdit() }, enabled = !busy,
                        modifier = Modifier.testTag("fare-driver-offers"))
                }
                val parsed = runCatching { FareSettings.parse(fields) }.getOrNull()
                parsed?.let {
                    val fare = com.intu.taxi.models.ServiceFare.estimate(3000.0, 600.0, settings = it)
                    Text("Ejemplo: 3 km y 10 min · Bajaj / cualquier moto: ${com.intu.taxi.ui.formatSoles(fare)} · " +
                        "Honda: ${com.intu.taxi.ui.formatSoles(com.intu.taxi.models.ServiceFare.withBrandPremium(fare, "honda", it))}",
                        modifier = Modifier.testTag("fare-preview"), style = MaterialTheme.typography.bodyMedium)
                }
                (validationError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (saved) Text("Tarifas guardadas", color = MaterialTheme.colorScheme.primary)
                Button(onClick = {
                    try {
                        val values = FareSettings.parse(fields).copy(driverPriceOffersEnabled = offersEnabled)
                        focus.clearFocus(); keyboard?.hide(); onSave(values)
                    }
                    catch (e: IllegalArgumentException) { validationError = e.message }
                }, enabled = !busy && !saved, modifier = Modifier.fillMaxWidth().testTag("fare-save")) {
                    Text(if (busy) "Guardando…" else "Guardar tarifas")
                }
            }
            Text("Los cambios aplican a nuevas solicitudes. Los viajes ya solicitados conservan su importe. " +
                "Los viajes programados usan la tarifa vigente al iniciar la búsqueda de conductor. Las tarifas de envíos se mantienen independientes.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
