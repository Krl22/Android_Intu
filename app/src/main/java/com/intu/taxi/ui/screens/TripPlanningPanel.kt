package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intu.taxi.data.PlaceSearchResult
import com.intu.taxi.data.SavedPlace
import com.intu.taxi.models.BookingContact

internal enum class PlanningField { PICKUP, DESTINATION }

@Composable
internal fun TripPlanningPanel(pickup: String, destination: String, field: PlanningField,
    contact: BookingContact?, delivery: Boolean, business: Boolean,
    savedPlaces: List<SavedPlace>, results: List<PlaceSearchResult>, loading: Boolean, error: String?,
    addressLoading: Boolean, addressError: String?, hasCatalog: Boolean,
    onBack: () -> Unit, onPerson: () -> Unit, onField: (PlanningField) -> Unit,
    onPickup: (String) -> Unit, onDestination: (String) -> Unit, onSelect: (PlaceSearchResult) -> Unit,
    onSaved: (SavedPlace) -> Unit, onPickMap: () -> Unit, onManagePlaces: () -> Unit,
    onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val pickupFocus = remember { FocusRequester() }
    val destinationFocus = remember { FocusRequester() }
    LaunchedEffect(field) {
        if (field == PlanningField.PICKUP && !business) pickupFocus.requestFocus() else destinationFocus.requestFocus()
    }
    Surface(modifier.fillMaxWidth().fillMaxHeight(0.96f).testTag("trip-planning-panel"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().imePadding()) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).size(38.dp, 4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)))
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver al inicio") }
                Text(if (business) "Planifica tu pedido" else if (delivery) "Planifica tu envío" else "Planifica tu viaje",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(onClick = {}, label = { Text("Ahora") }, leadingIcon = { Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp)) })
                AssistChip(onClick = onPerson, label = { Text(contact?.let { "Para ${it.name.substringBefore(' ')}" } ?: "Para mí") },
                    leadingIcon = { Icon(Icons.Outlined.PersonOutline, null, Modifier.size(18.dp)) },
                    trailingIcon = { Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp)) }, modifier = Modifier.testTag("booking-person"))
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF08817E), Color(0xFF1E1F47))), RoundedCornerShape(20.dp))
                .padding(2.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))) {
                TextField(pickup, onPickup, readOnly = business, singleLine = true,
                    trailingIcon = if (pickup.isNotEmpty() && !business) ({ IconButton(onClick = { onPickup("") }) { Icon(Icons.Outlined.Clear, "Limpiar recojo") } }) else null,
                    placeholder = { Text(if (contact != null && !business) "¿Dónde recogemos a ${contact.name.substringBefore(' ')}?" else "Punto de recojo") },
                    leadingIcon = { Icon(Icons.Outlined.RadioButtonChecked, "Recojo", tint = MaterialTheme.colorScheme.primary) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().testTag("planning-pickup-search").focusRequester(pickupFocus)
                        .onFocusChanged { if (it.isFocused && !business) onField(PlanningField.PICKUP) })
                HorizontalDivider(Modifier.padding(start = 52.dp, end = 16.dp))
                TextField(destination, onDestination, singleLine = true, placeholder = { Text(if (delivery) "¿Dónde entregamos?" else "¿A dónde vamos?") },
                    trailingIcon = if (destination.isNotEmpty()) ({ IconButton(onClick = { onDestination("") }) { Icon(Icons.Outlined.Clear, "Limpiar") } }) else null,
                    leadingIcon = { Icon(Icons.Outlined.Place, "Destino", tint = MaterialTheme.colorScheme.primary) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().testTag("home-destination-search").focusRequester(destinationFocus)
                        .onFocusChanged { if (it.isFocused) onField(PlanningField.DESTINATION) })
            }
            if (contact != null) Text(if (business) "${contact.name} recibirá el pedido. Tú gestionas el envío."
                else if (delivery) "${contact.name} entregará el paquete. Elige su recojo."
                else "Viaja ${contact.name}. Elige su recojo y destino.", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                DestinationSearchPanel(query = if (field == PlanningField.PICKUP) pickup else destination,
                    savedPlaces = savedPlaces, results = results, hasCatalog = hasCatalog, loading = loading, error = error,
                    addressLoading = addressLoading, addressError = addressError,
                    onSelect = onSelect, onSavedPlaceClick = onSaved, onRefresh = onRefresh,
                    onPickMap = onPickMap, onManagePlaces = onManagePlaces, showInitialSuggestions = true,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}
