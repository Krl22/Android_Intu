package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val PlaceFormTeal = Color(0xFF08817E)
private val PlaceFormInk = Color(0xFF1E1F47)
private val PlaceFormColors = lightColorScheme(
    primary = PlaceFormTeal, onPrimary = Color.White,
    surface = Color.White, onSurface = PlaceFormInk,
    surfaceVariant = Color(0xFFF0F6F5), onSurfaceVariant = Color(0xFF586D70),
    outline = Color(0xFF91A6A6), outlineVariant = Color(0xFFDDE8E6),
    error = Color(0xFFB3261E)
)

/** Presentation only; SavedPlaceEditorDialog owns coordinates, persistence and validation. */
@Composable
internal fun SavedPlaceFormDialog(
    name: String,
    address: String,
    error: String?,
    saveEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onChangeLocation: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        MaterialTheme(colorScheme = if (com.intu.taxi.ui.theme.LocalIntuDarkMode.current) MaterialTheme.colorScheme else PlaceFormColors) {
            val focus = LocalFocusManager.current
            BoxWithConstraints(
                Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val compact = maxHeight < 460.dp || LocalDensity.current.fontScale > 1.3f
                Surface(
                    Modifier.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = maxHeight * .94f)
                        .testTag("saved-place-form"),
                    shape = RoundedCornerShape(26.dp), color = AppearanceColors.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 12.dp
                ) {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!compact) Surface(shape = RoundedCornerShape(14.dp), color = PlaceFormTeal.copy(alpha = .08f)) {
                                Box(Modifier.size(44.dp).background(Brush.linearGradient(listOf(PlaceFormTeal, PlaceFormInk))),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.LocationOn, null, tint = Color.White, modifier = Modifier.size(25.dp))
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Guardar lugar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (!compact) Text("Tus destinos, a un toque.", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Outlined.Close, "Cerrar Guardar lugar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(
                            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp).padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedTextField(
                                name, onNameChange, modifier = Modifier.fillMaxWidth().testTag("saved-place-name"),
                                label = { Text("Nombre del lugar") }, singleLine = true, shape = RoundedCornerShape(14.dp),
                                supportingText = { Text("Por ejemplo: Casa, Trabajo o Gimnasio") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
                            )
                            OutlinedTextField(
                                address, onAddressChange, modifier = Modifier.fillMaxWidth().testTag("saved-place-reference"),
                                label = { Text("Dirección o referencia") }, minLines = 2, maxLines = 3,
                                shape = RoundedCornerShape(14.dp),
                                supportingText = { Text("Opcional. Ejemplo: entrada junto al parque.") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
                            )
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Outlined.LocationOn, null, tint = PlaceFormTeal,
                                            modifier = Modifier.size(22.dp).padding(top = 1.dp))
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text("Ubicación elegida", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            Text("Se guardará el punto seleccionado en el mapa.",
                                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    OutlinedButton(onClick = onChangeLocation, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        border = BorderStroke(1.dp, PlaceFormTeal.copy(alpha = .4f))) {
                                        Icon(Icons.Outlined.Map, null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Cambiar ubicación en el mapa", modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                            if (error != null) Text(error, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("saved-place-error"))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                            if (maxWidth >= 280.dp && LocalDensity.current.fontScale <= 1.3f) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                                        shape = RoundedCornerShape(14.dp)) { Text("Cancelar") }
                                    Button(onClick = onSave, enabled = saveEnabled,
                                        modifier = Modifier.weight(1f).heightIn(min = 50.dp), shape = RoundedCornerShape(14.dp)) {
                                        Text("Guardar", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(onClick = onSave, enabled = saveEnabled,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(14.dp)) {
                                        Text("Guardar", fontWeight = FontWeight.SemiBold)
                                    }
                                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancelar") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
