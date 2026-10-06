package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TermsDialog(onDismiss: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val termsScroll = rememberScrollState()
    val privacyScroll = rememberScrollState()
    AccountDialogLayout(
        eyebrow = "INFORMACIÓN DE INTU",
        title = "Términos y privacidad",
        subtitle = "Conoce el servicio y cómo usamos tus datos.",
        onDismiss = onDismiss,
        footer = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp)) { Text("Cerrar", fontWeight = FontWeight.SemiBold) }
        }
    ) {
        TabRow(selectedTabIndex = selected, containerColor = AppearanceColors.surface, contentColor = AppearanceColors.highlight(AccountTeal)) {
            listOf("Términos", "Privacidad").forEachIndexed { index, label ->
                Tab(selected = selected == index, onClick = { selected = index },
                    text = { Text(label, fontWeight = FontWeight.SemiBold) })
            }
        }
        Column(Modifier.weight(1f, fill = false).verticalScroll(if (selected == 0) termsScroll else privacyScroll)
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = AccountTeal.copy(alpha = .08f)) {
                Text("Versión de prueba · Borrador para revisión", Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium, color = AppearanceColors.highlight(AccountTeal))
            }
            if (selected == 0) {
                LegalSection("Uso de Intu", "Intu permite solicitar viajes en mototaxi y conectar pasajeros con conductores aprobados. La disponibilidad depende de los conductores conectados y de la cobertura. La ubicación, el tiempo de llegada y la tarifa mostrada son estimaciones; revisa el recojo, el destino y el importe antes de confirmar.")
                LegalSection("Cuenta, conductores y seguridad", "Usa datos propios y correctos. No compartas tu acceso ni suplantes a otras personas. Enviar documentos de conductor no autoriza a prestar servicios: Intu debe aprobar la solicitud. Mantén actualizados tus documentos y los datos del vehículo. Pasajeros y conductores deben tratarse con respeto y cumplir las reglas de tránsito. La app permite cancelar y reportar problemas; no es un servicio de emergencias.")
                LegalSection("Pagos y viajes", "Los medios disponibles son efectivo y Yape. El pago se acuerda con el conductor según el importe del viaje; Intu no almacena tus credenciales bancarias. Las calificaciones deben reflejar tu experiencia real y pueden mostrarse a la otra persona durante el servicio. Evita solicitudes falsas, acoso y uso fraudulento.")
                LegalSection("Cancelaciones y chat", "Al cancelar te pediremos el motivo. Cancelar mientras se busca conductor o poco después de que acepte no tiene penalidad. Cancelar repetidamente después, o no presentarte en el punto de recojo, puede pausar por un tiempo tu cuenta para pedir o aceptar servicios. El conductor puede cancelar si el pasajero no aparece tras esperar en el punto de recojo. El chat del servicio es solo para coordinar el recojo y la entrega; mientras maneja, el conductor solo puede enviar respuestas rápidas.")
            } else {
                LegalSection("Datos que usamos", "Tratamos los datos de tu perfil y acceso, foto, solicitudes, origen y destino, estados de viaje, pagos seleccionados, calificaciones, motivos de cancelación y mensajes del chat del servicio para operar Intu, prevenir abusos y atender problemas. Los mensajes del chat se guardan 30 días para revisar reclamos; solo el equipo administrador puede consultarlos y luego se borran. Para solicitudes de conductor también tratamos documentos y datos del vehículo. Los reportes incluyen tu descripción, cuenta, versión de Intu y datos básicos del equipo.")
                LegalSection("Ubicación y permisos", "La ubicación permite definir el recojo y seguir el viaje. Cuando un conductor está en línea, su ubicación puede actualizarse con la app minimizada. El seguimiento del pasajero mantiene actualizado su viaje abierto. Los participantes ven la información necesaria de la otra persona durante el servicio. La cámara sirve para tu foto y las notificaciones para avisos de viajes; puedes gestionar estos permisos en Android.")
                LegalSection("Proveedores y lugares guardados", "Firebase proporciona acceso, fotos y notificaciones; Supabase almacena cuentas, solicitudes, viajes y reportes; Mapbox proporciona mapas y rutas; Anthropic (Claude) responde las preguntas que escribes en Ayuda con Intu. Estos proveedores pueden procesar los datos necesarios para esas funciones. Intu no guarda el texto de tus preguntas al asistente; no escribas en él datos personales ni de pago. Los lugares favoritos se guardan por cuenta en este teléfono y puedes quitarlos desde Direcciones guardadas.")
                LegalSection("Tus datos y soporte", "Para solicitar la eliminación de tu cuenta y los datos personales asociados, abre Cuenta → Eliminar cuenta → Solicitar eliminación. Recibirás una confirmación de envío cuando la solicitud llegue al panel privado del equipo de Intu. Durante las pruebas se atiende manualmente; enviar la solicitud no significa que la cuenta ya se haya eliminado. El equipo informará del plazo de atención y de los datos que deban conservarse antes de completarla.", "Puedes solicitar acceso, rectificación, cancelación u oposición al tratamiento de tus datos desde Cuenta → Reportar un error, con el título «Solicitud sobre mis datos». El equipo administrador la recibirá para atenderla. Evita contraseñas o información de otras personas.")
                LegalSection("Sobre este borrador", "Antes de publicar la política final deben definirse el responsable y domicilio, el contacto de soporte y los plazos de conservación. Este texto de pruebas no establece todavía una política final de conservación ni garantiza que los registros de viajes queden anonimizados al eliminar una cuenta.")
            }
        }
    }
}

@Composable
private fun LegalSection(title: String, vararg paragraphs: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            paragraphs.forEach { paragraph ->
                Text(paragraph, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
