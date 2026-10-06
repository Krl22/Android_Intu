package com.intu.taxi.ui.screens

import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intu.taxi.data.RecentContacts
import com.intu.taxi.models.BookingContact
import com.intu.taxi.ui.formatPeruPhone

/** ACTION_PICK grants access to one selected phone row without READ_CONTACTS. */
@Composable
internal fun rememberPhoneContactPicker(onPicked: (BookingContact) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            runCatching {
                context.contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) currentOnPicked(BookingContact(
                        cursor.getString(cursor.getColumnIndexOrThrow(Phone.DISPLAY_NAME)).orEmpty(),
                        cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER)).orEmpty()))
                }
            }.onFailure { android.widget.Toast.makeText(context, "No se pudo leer el contacto. Intenta de nuevo.", android.widget.Toast.LENGTH_LONG).show() }
        }
    }
    return {
        runCatching { launcher.launch(Intent(Intent.ACTION_PICK).setType(Phone.CONTENT_TYPE)) }
            .onFailure { android.widget.Toast.makeText(context, "No se encontró la app de contactos del teléfono.", android.widget.Toast.LENGTH_LONG).show() }
    }
}

/**
 * Para quién es el servicio. Solo se elige de la agenda del teléfono (nunca datos escritos a mano);
 * los contactos usados antes quedan a un toque.
 */
@Composable
internal fun BookingContactDialog(initial: BookingContact?, delivery: Boolean, business: Boolean,
    onDismiss: () -> Unit, onSelect: (BookingContact?) -> Unit) {
    val context = LocalContext.current
    val recentStore = remember { RecentContacts.forCurrentUser(context) }
    var recents by remember { mutableStateOf(recentStore.read()) }
    var error by remember { mutableStateOf<String?>(null) }
    fun choose(contact: BookingContact) {
        val valid = runCatching { contact.normalized() }.getOrNull()
        if (valid == null) {
            error = "${contact.name.ifBlank { "Ese contacto" }} no tiene un celular peruano válido (9 dígitos que empiezan con 9)."
            return
        }
        recentStore.remember(valid)
        onSelect(valid)
    }
    val pick = rememberPhoneContactPicker(::choose)
    AccountDialogLayout(eyebrow = "INTU · PARA OTRA PERSONA", title = "¿Para quién es?",
        subtitle = if (business) "Elige de tus contactos quién recibirá el pedido."
            else if (delivery) "Elige de tus contactos quién entregará el paquete al repartidor. Después indicarás quién lo recibe."
            else "Elige de tus contactos quién viajará y luego indica su recojo y destino.",
        onDismiss = onDismiss,
        footer = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { error = null; pick() }, modifier = Modifier.fillMaxWidth().testTag("booking-pick-contact")) {
                Icon(Icons.Outlined.Contacts, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Elegir de mis contactos")
            }
            TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth().testTag("booking-for-me")) { Text("Para mí") }
        } }, content = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (recents.isEmpty()) {
                    Text("Cuando elijas a alguien de tu agenda, aparecerá aquí para la próxima vez.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Usados recientemente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    recents.forEachIndexed { index, contact ->
                        val selected = contact.phone == initial?.phone
                        OutlinedCard(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().clickable { choose(contact) }.testTag("booking-recent-$index")
                        ) {
                            Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.PersonOutline, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(contact.name, fontWeight = FontWeight.SemiBold)
                                    Text(formatPeruPhone(contact.phone), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { recentStore.forget(contact.phone); recents = recentStore.read() }) {
                                    Icon(Icons.Outlined.Close, "Quitar a ${contact.name} de recientes")
                                }
                            }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        })
}
