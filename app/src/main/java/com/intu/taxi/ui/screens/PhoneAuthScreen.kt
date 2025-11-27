package com.intu.taxi.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.intu.taxi.auth.AuthRepository
import com.intu.taxi.auth.PhoneFormatter
import com.intu.taxi.auth.countryCodes
import com.intu.taxi.auth.defaultCountry
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    activity: Activity,
    repo: AuthRepository,
    onOtpSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken?) -> Unit,
    onVerified: (PhoneAuthCredential) -> Unit,
    onError: (String) -> Unit
) {
    val prefijo = remember { mutableStateOf(defaultCountry().prefijo) }
    val numero = remember { mutableStateOf("") }
    val verificationId = remember { mutableStateOf<String?>(null) }
    val resendToken = remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    val otp = remember { mutableStateOf("") }
    val canResend = remember { mutableStateOf(false) }
    val sending = remember { mutableStateOf(false) }
    val statusMsg = remember { mutableStateOf<String?>(null) }
    
    // Estados de animación
    val cardVisible = remember { mutableStateOf(false) }
    val titleVisible = remember { mutableStateOf(false) }
    val contentVisible = remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Iniciar animaciones secuenciales
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        cardVisible.value = true
        kotlinx.coroutines.delay(300)
        titleVisible.value = true
        kotlinx.coroutines.delay(200)
        contentVisible.value = true
    }

    // Fondo con gradiente animado
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E1F47), // Índigo oscuro
                        Color(0xFF08817E), // Teal
                        Color(0xFF0FB9B1)  // Teal claro
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // Patrón de fondo con burbujas animadas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.1f)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size((100 + index * 50).dp)
                        .offset(
                            x = (50 + index * 100).dp,
                            y = (100 + index * 150).dp
                        )
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = cardVisible.value,
                enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(800)
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icono principal
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF08817E),
                                            Color(0xFF0FB9B1)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = titleVisible.value,
                            enter = fadeIn(animationSpec = tween(600, delayMillis = 200))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Verifica tu número",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1F47),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Te enviaremos un código de 6 dígitos",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF08817E),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = contentVisible.value,
                            enter = fadeIn(animationSpec = tween(600, delayMillis = 400))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Campo de número de teléfono
                                if (verificationId.value == null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        var expanded = remember { mutableStateOf(false) }
                                        var selectedCountry = remember { mutableStateOf(defaultCountry()) }
                                        ExposedDropdownMenuBox(
                                            expanded = expanded.value,
                                            onExpandedChange = { expanded.value = !expanded.value }
                                        ) {
                                            OutlinedTextField(
                                                value = selectedCountry.value.prefijo,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Prefijo") },
                                                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Seleccionar país") },
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent,
                                                    focusedIndicatorColor = Color(0xFF08817E),
                                                    unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier
                                                    .menuAnchor()
                                                    .width(96.dp)
                                            )
                                            DropdownMenu(
                                                expanded = expanded.value,
                                                onDismissRequest = { expanded.value = false }
                                            ) {
                                                countryCodes.forEach { country ->
                                                    DropdownMenuItem(
                                                        text = { Text("${country.prefijo} • ${country.nombre}") },
                                                        onClick = {
                                                            selectedCountry.value = country
                                                            prefijo.value = country.prefijo
                                                            expanded.value = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        OutlinedTextField(
                                            value = numero.value,
                                            onValueChange = { numero.value = PhoneFormatter.sanitizeDigits(it) },
                                            label = { Text("Número") },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color(0xFF08817E),
                                                unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Campo de código OTP
                                if (verificationId.value != null) {
                                    OutlinedTextField(
                                        value = otp.value,
                                        onValueChange = { otp.value = PhoneFormatter.sanitizeDigits(it) },
                                        label = { Text("Código OTP") },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color(0xFF08817E),
                                            unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Botones
                                if (verificationId.value == null) {
                                    Button(
                                        onClick = {
                                            try {
                                                if (!PhoneFormatter.isValid(prefijo.value, numero.value)) {
                                                    val msg = "Número inválido para ese prefijo"
                                                    statusMsg.value = msg
                                                    onError(msg)
                                                } else {
                                                    val e164 = PhoneFormatter.formatE164(prefijo.value, numero.value)
                                                    sending.value = true
                                                    repo.startPhoneVerification(
                                                        activity = activity,
                                                        phoneE164 = e164,
                                                        timeoutMinutes = 10,
                                                        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                                                sending.value = false
                                                                statusMsg.value = "Verificación automática completada"
                                                                onVerified(credential)
                                                            }
                                                            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                                                                sending.value = false
                                                                val msg = e.message ?: "Error de verificación"
                                                                statusMsg.value = msg
                                                                onError(msg)
                                                            }
                                                            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                                                                sending.value = false
                                                                verificationId.value = id
                                                                resendToken.value = token
                                                                onOtpSent(id, token)
                                                                canResend.value = false
                                                                statusMsg.value = "Código enviado"
                                                            }
                                                        }
                                                    )
                                                }
                                            } catch (t: Throwable) {
                                                sending.value = false
                                                val msg = t.message ?: "Error al iniciar verificación"
                                                statusMsg.value = msg
                                                onError(msg)
                                            }
                                        },
                                        enabled = !sending.value,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF08817E),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Enviar código")
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val vid = verificationId.value ?: return@Button
                                                val credential = PhoneAuthProvider.getCredential(vid, otp.value)
                                                onVerified(credential)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF065F46),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Verificar")
                                        }
                                        Button(
                                            onClick = {
                                                try {
                                                    val e164 = PhoneFormatter.formatE164(prefijo.value, numero.value)
                                                    sending.value = true
                                                    repo.startPhoneVerification(
                                                        activity = activity,
                                                        phoneE164 = e164,
                                                        timeoutMinutes = 10,
                                                        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                                            override fun onVerificationCompleted(credential: PhoneAuthCredential) { sending.value = false; onVerified(credential) }
                                                            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                                                                sending.value = false
                                                                val msg = e.message ?: "Error de verificación"
                                                                statusMsg.value = msg
                                                                onError(msg)
                                                            }
                                                            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                                                                sending.value = false
                                                                verificationId.value = id
                                                                resendToken.value = token
                                                                onOtpSent(id, token)
                                                                statusMsg.value = "Código reenviado"
                                                            }
                                                        },
                                                        forceResendingToken = resendToken.value
                                                    )
                                                } catch (t: Throwable) {
                                                    sending.value = false
                                                    val msg = t.message ?: "Error al reenviar código"
                                                    statusMsg.value = msg
                                                    onError(msg)
                                                }
                                            },
                                            enabled = canResend.value,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF1C1C1E).copy(alpha = 0.8f),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reenviar")
                                        }
                                    }
                                }

                                // Estado de carga
                                if (sending.value) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(
                                                Color(0xFF08817E).copy(alpha = 0.1f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF08817E),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                    Text(
                                        "Enviando código...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF08817E)
                                    )
                                }

                                // Mensajes de estado
                                statusMsg.value?.let { msg ->
                                    val isError = msg.contains("Error", ignoreCase = true) || 
                                                 msg.contains("inválido", ignoreCase = true) ||
                                                 msg.contains("Fallo", ignoreCase = true)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isError) 
                                                Color(0xFFB00020).copy(alpha = 0.1f) 
                                            else 
                                                Color(0xFF08817E).copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Text(
                                            msg,
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isError) Color(0xFFB00020) else Color(0xFF08817E),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Cooldown de reenvío: 60s tras recibir el primer código
    LaunchedEffect(verificationId.value) {
        if (verificationId.value != null) {
            canResend.value = false
            delay(60_000)
            canResend.value = true
        }
    }
}