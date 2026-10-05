package com.intu.taxi.ui.screens

import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.intu.taxi.auth.PhoneFormatter
import com.intu.taxi.models.BookingContact

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
            }.onFailure { android.widget.Toast.makeText(context, "No se pudo leer el contacto. Puedes escribir sus datos.", android.widget.Toast.LENGTH_LONG).show() }
        }
    }
    return {
        runCatching { launcher.launch(Intent(Intent.ACTION_PICK).setType(Phone.CONTENT_TYPE)) }
            .onFailure { android.widget.Toast.makeText(context, "Puedes escribir el nombre y celular aquí.", android.widget.Toast.LENGTH_LONG).show() }
    }
}

@Composable
internal fun BookingContactDialog(initial: BookingContact?, delivery: Boolean, business: Boolean,
    onDismiss: () -> Unit, onSelect: (BookingContact?) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(initial?.phone.orEmpty()) }
    val contact = BookingContact(name, phone)
    val valid = runCatching { contact.normalized() }.isSuccess
    val pick = rememberPhoneContactPicker { name = it.name; phone = it.phone }
    AccountDialogLayout(eyebrow = "INTU · PARA OTRA PERSONA", title = "¿Para quién es?",
        subtitle = if (business) "Elige quién recibirá el pedido." else if (delivery) "Elige quién entregará el paquete al repartidor. Después indicarás quién lo recibe."
            else "Elige quién viajará y luego indica su recojo y destino.",
        onDismiss = onDismiss,
        footer = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSelect(contact.normalized()) }, enabled = valid,
                modifier = Modifier.fillMaxWidth().testTag("booking-contact-confirm")) { Text("Usar esta persona") }
            TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) { Text("Para mí") }
        } }, content = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedButton(onClick = pick, modifier = Modifier.fillMaxWidth().testTag("booking-pick-contact")) { Text("Elegir de mis contactos") }
                OutlinedTextField(name, { name = it.take(100) }, label = { Text("Nombre") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("booking-contact-name"))
                OutlinedTextField(phone, { phone = it.take(30) }, label = { Text("Celular") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    supportingText = { Text("Celular peruano · +51") }, modifier = Modifier.fillMaxWidth().testTag("booking-contact-phone"))
            }
        })
}
