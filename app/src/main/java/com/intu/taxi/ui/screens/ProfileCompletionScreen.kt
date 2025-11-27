package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.intu.taxi.auth.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCompletionScreen(
    prefilledPhoneE164: String?,
    requireEmail: Boolean,
    onSubmit: (UserProfile) -> Unit,
    onVerifyEmail: (() -> Unit)? = null
) {
    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val birthdate = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val phone = remember { mutableStateOf(prefilledPhoneE164 ?: "") }
    val showDatePicker = remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val todayMillis = remember { System.currentTimeMillis() }

    // Estados de animación
    val cardVisible = remember { mutableStateOf(false) }
    val titleVisible = remember { mutableStateOf(false) }
    val contentVisible = remember { mutableStateOf(false) }

    // Estado del DatePicker con fecha inicial si ya hay texto
    @OptIn(ExperimentalMaterial3Api::class)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = birthdate.value.takeIf { it.isNotBlank() }?.let {
            try {
                val ld = LocalDate.parse(it)
                ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) { null }
        }
    )

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
                                imageVector = Icons.Default.Person,
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
                                    "Completa tu perfil",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1F47),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Cuéntanos un poco sobre ti",
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
                                // Campo de nombre
                                TextField(
                                    value = firstName.value,
                                    onValueChange = { firstName.value = it },
                                    label = { Text("Nombre") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF08817E)) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color(0xFF08817E),
                                        unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f),
                                        focusedLeadingIconColor = Color(0xFF08817E),
                                        unfocusedLeadingIconColor = Color(0xFF08817E).copy(alpha = 0.7f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Campo de apellido
                                TextField(
                                    value = lastName.value,
                                    onValueChange = { lastName.value = it },
                                    label = { Text("Apellido") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF08817E)) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color(0xFF08817E),
                                        unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f),
                                        focusedLeadingIconColor = Color(0xFF08817E),
                                        unfocusedLeadingIconColor = Color(0xFF08817E).copy(alpha = 0.7f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Fecha de nacimiento con selector de calendario
                                TextField(
                                    value = birthdate.value,
                                    onValueChange = {},
                                    label = { Text("Fecha de nacimiento") },
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF08817E)) },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color(0xFF08817E),
                                        unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f),
                                        focusedLeadingIconColor = Color(0xFF08817E),
                                        unfocusedLeadingIconColor = Color(0xFF08817E).copy(alpha = 0.7f)
                                    ),
                                    trailingIcon = {
                                        Button(
                                            onClick = { showDatePicker.value = true },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF08817E).copy(alpha = 0.1f),
                                                contentColor = Color(0xFF08817E)
                                            ),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = "Elegir fecha", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )

                                // Diálogo con DatePicker Material3
                                if (showDatePicker.value) {
                                    DatePickerDialog(
                                        onDismissRequest = { showDatePicker.value = false },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    val millis = datePickerState.selectedDateMillis
                                                    if (millis != null) {
                                                        // Evitar fechas futuras
                                                        val safeMillis = kotlin.math.min(millis, todayMillis)
                                                        val ld = Instant.ofEpochMilli(safeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                                                        birthdate.value = ld.format(formatter)
                                                    }
                                                    showDatePicker.value = false
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF08817E),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Confirmar")
                                            }
                                        },
                                        dismissButton = {
                                            Button(
                                                onClick = { showDatePicker.value = false },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF1C1C1E).copy(alpha = 0.8f),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text("Cancelar")
                                            }
                                        }
                                    ) {
                                        DatePicker(state = datePickerState)
                                    }
                                }

                                // Campo de teléfono (solo lectura)
                                TextField(
                                    value = phone.value,
                                    onValueChange = {},
                                    label = { Text("Teléfono") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF08817E)) },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color(0xFF08817E),
                                        unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f),
                                        focusedLeadingIconColor = Color(0xFF08817E),
                                        unfocusedLeadingIconColor = Color(0xFF08817E).copy(alpha = 0.7f),
                                        disabledIndicatorColor = Color(0xFF08817E).copy(alpha = 0.3f),
                                        disabledLeadingIconColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                    )
                                )

                                // Campo de correo
                                TextField(
                                    value = email.value,
                                    onValueChange = { email.value = it },
                                    label = { Text("Correo electrónico") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF08817E)) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color(0xFF08817E),
                                        unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f),
                                        focusedLeadingIconColor = Color(0xFF08817E),
                                        unfocusedLeadingIconColor = Color(0xFF08817E).copy(alpha = 0.7f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                RowActions(requireEmail = requireEmail, email = email.value, onVerifyEmail = onVerifyEmail) {
                                    val profile = UserProfile(
                                        firstName = firstName.value.trim(),
                                        lastName = lastName.value.trim(),
                                        birthdate = birthdate.value.trim(),
                                        number = phone.value.trim(),
                                        email = email.value.trim().ifEmpty { null },
                                        termsAccepted = true
                                    )
                                    onSubmit(profile)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowActions(requireEmail: Boolean, email: String, onVerifyEmail: (() -> Unit)?, onContinue: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onContinue,
            enabled = !requireEmail || email.isNotBlank(),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF08817E),
                contentColor = Color.White
            )
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Guardar y continuar")
        }
        if (onVerifyEmail != null && email.isNotBlank()) {
            Button(
                onClick = onVerifyEmail,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF065F46),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Verificar correo")
            }
        }
    }
}