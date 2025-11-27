package com.intu.taxi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
// Personas y vehículo
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.auth.AuthRepository
import com.intu.taxi.auth.UserProfile
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.intu.taxi.R
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun AccountScreen(
    padding: PaddingValues,
    isDriver: Boolean,
    onDriverChange: (Boolean) -> Unit,
    onLogout: (() -> Unit)? = null
) {
    // Estado del perfil de usuario cargado desde Firestore
    val repo = remember { AuthRepository() }
    val auth = FirebaseAuth.getInstance()
    val authUser = auth.currentUser
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loadingProfile by remember { mutableStateOf(true) }
    var profileError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val linkLoading = remember { mutableStateOf(false) }
    var googleLinked by remember {
        mutableStateOf(
            authUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
        )
    }
    val googleLinkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            linkLoading.value = true
            scope.launch {
                try {
                    val linked = repo.linkWithGoogleAccount(account)
                    if (linked != null) {
                        android.widget.Toast.makeText(context, "Google vinculado", android.widget.Toast.LENGTH_SHORT).show()
                        // Actualiza estado de proveedor para ocultar opción de vincular
                        try {
                            auth.currentUser?.reload()
                        } catch (_: Exception) { }
                        googleLinked = true
                        // Opcional: recargar el perfil para reflejar email de Google si falta
                        val uid = authUser?.uid
                        if (uid != null) {
                            try { profile = repo.getUserProfile(uid) } catch (_: Exception) { }
                        }
                    } else {
                        android.widget.Toast.makeText(context, "No se pudo vincular", android.widget.Toast.LENGTH_LONG).show()
                    }
                } catch (e: FirebaseAuthUserCollisionException) {
                    android.widget.Toast.makeText(context, "El correo de Google ya está usado en otra cuenta", android.widget.Toast.LENGTH_LONG).show()
                } catch (t: Throwable) {
                    android.widget.Toast.makeText(context, t.message ?: "Error al vincular Google", android.widget.Toast.LENGTH_LONG).show()
                } finally {
                    linkLoading.value = false
                }
            }
        } catch (e: ApiException) {
            val code = e.statusCode
            val readable = com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.getStatusCodeString(code)
            if (code == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                android.widget.Toast.makeText(context, "Vinculación cancelada", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                val guidance = if (code == 10) "Verifica SHA-1/256 en Firebase y el default_web_client_id." else ""
                android.widget.Toast.makeText(context, "Google Sign-In: ${readable} (${code}). ${guidance}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(authUser?.uid) {
        loadingProfile = true
        profileError = null
        val uid = authUser?.uid
        if (uid != null) {
            try {
                profile = repo.getUserProfile(uid)
            } catch (e: Exception) {
                profileError = e.message
            }
        }
        // Revisa proveedores vinculados
        googleLinked = auth.currentUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
        loadingProfile = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
    ) {
        // Header con gradiente inspirado en HomeScreen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .drawBehind {
                    val teal = Color(0xFF08817E)
                    val indigo = Color(0xFF1E1F47)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(teal, indigo),
                            center = Offset(0.1f, 0.1f),
                            radius = size.height * 0.9f
                        ),
                        size = Size(width = size.width, height = size.height)
                    )
                    withTransform({
                        scale(scaleX = 1.6f, scaleY = 1.0f, pivot = Offset.Zero)
                    }) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.White.copy(alpha = 1.0f),
                                    0.70f to Color.White.copy(alpha = 1.0f),
                                    0.75f to Color.White.copy(alpha = 0.95f),
                                    0.80f to Color.White.copy(alpha = 0.85f),
                                    0.85f to Color.White.copy(alpha = 0.70f),
                                    0.90f to Color.White.copy(alpha = 0.45f),
                                    0.95f to Color.White.copy(alpha = 0.25f),
                                    1.00f to Color.Transparent
                                ),
                                center = Offset(0f, 0f),
                                radius = max(size.width, size.height)
                            ),
                            size = Size(width = size.width, height = size.height),
                            blendMode = BlendMode.DstIn
                        )
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Toggle Modo conductor (esquina superior derecha)
            DriverModeToggle(
                checked = isDriver,
                onCheckedChange = onDriverChange,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 45.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val context = LocalContext.current
                var profileUri by remember { mutableStateOf<Uri?>(null) }
                val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> profileUri = uri }
                val profileBitmap: ImageBitmap? = remember(profileUri) {
                    profileUri?.let {
                        try {
                            context.contentResolver.openInputStream(it)?.use { input ->
                                BitmapFactory.decodeStream(input)?.asImageBitmap()
                            }
                        } catch (_: Exception) { null }
                    }
                }
                if (!isDriver) {
                    // CLIENTE: avatar centrado + progreso
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clickable { pickImageLauncher.launch("image/*") }
                            .clip(CircleShape)
                            .background(Color(0xFFEFF2F7))
                    ) {
                        profileBitmap?.let { bmp ->
                            Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    when {
                        loadingProfile -> {
                            CircularProgressIndicator(color = Color.White)
                        }
                        profileError != null -> {
                            Text("Error al cargar perfil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {
                            val displayName = listOfNotNull(profile?.firstName, profile?.lastName)
                                .joinToString(" ").ifBlank { authUser?.displayName ?: "Mi cuenta" }
                            val phoneLabel = profile?.number ?: authUser?.phoneNumber ?: "Sin número"
                            Text(displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Número: $phoneLabel", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                } else {
                    // CONDUCTOR: estilo claro similar al cliente + estado
                    var isDriverOnline by rememberSaveable { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clickable { pickImageLauncher.launch("image/*") }
                            .clip(CircleShape)
                            .background(Color(0xFFEFF2F7))
                    ) {
                        profileBitmap?.let { bmp ->
                            Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    when {
                        loadingProfile -> {
                            CircularProgressIndicator(color = Color.White)
                        }
                        profileError != null -> {
                            Text("Error al cargar perfil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {
                            val displayName = listOfNotNull(profile?.firstName, profile?.lastName)
                                .joinToString(" ").ifBlank { authUser?.displayName ?: "Mi cuenta" }
                            val phoneLabel = profile?.number ?: authUser?.phoneNumber ?: "Sin número"
                            Text(displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Conductor • Número: $phoneLabel", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

      

        // Bloques específicos de conductor: ganancias, rating y vehículo
        if (isDriver) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Hoy", color = Color(0xFF6B7280), style = MaterialTheme.typography.labelMedium)
                        Text("$ 120.00", color = Color(0xFF1C1C1E), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Semana: $ 540.50", color = Color(0xFF6B7280), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Star, null, tint = Color(0xFFFFB703))
                            Spacer(Modifier.width(6.dp))
                            Text("4.8", color = Color(0xFF1C1C1E), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text("250 viajes", color = Color(0xFF6B7280), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = Color(0xFF1C1C1E))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ABC-123 • Toyota Corolla", color = Color(0xFF1C1C1E))
                        Text("Docs: Verificados", color = Color(0xFF059669), style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Abrir", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { /* TODO */ }.padding(8.dp))
                }
            }
        }

        // Contenido de opciones
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mostrar correo y botón de verificación opcional
            val user = authUser
            val email = profile?.email ?: user?.email ?: "Sin correo"
            val emailVerificado = user?.isEmailVerified == true
            SettingRow(
                label = "Correo: $email",
                trailing = {
                    when {
                        user == null || user.email.isNullOrEmpty() -> {
                            Text(
                                text = "Agregar",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { /* Navegar a completar correo si se requiere */ }
                                    .padding(8.dp)
                            )
                        }
                        !emailVerificado -> {
                            Button(
                                onClick = { user?.sendEmailVerification() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) { Text("Verificar") }
                        }
                        else -> {
                            Text(text = "Verificado", color = Color(0xFF059669))
                        }
                    }
                }
            )
            // Acción/estado de Google
            SettingRow(
                label = "Cuenta de Google",
                trailing = {
                    if (googleLinked) {
                        Text(text = "Vinculado", color = Color(0xFF059669))
                    } else {
                        Button(
                            onClick = {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(context.getString(R.string.default_web_client_id))
                                    .requestEmail()
                                    .build()
                                val client = GoogleSignIn.getClient(context, gso)
                                googleLinkLauncher.launch(client.signInIntent)
                            },
                            enabled = authUser != null && !linkLoading.value,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E), contentColor = Color.White)
                        ) { Text(if (linkLoading.value) "Vinculando..." else "Vincular") }
                    }
                }
            )
            // El modo oscuro se elimina; la app usa tema claro por defecto.
            // Se mantienen el resto de ajustes.
            SettingRow(label = "Métodos de pago", actionText = "Gestionar")
            SettingRow(label = "Direcciones guardadas", actionText = "Editar")
            SettingRow(label = if (isDriver) "Historial de viajes (conductor)" else "Historial de viajes", actionText = "Ver")
            SettingRow(label = "Idioma", actionText = "Español")
            SettingRow(label = "Notificaciones", actionText = "Configurar")
            SettingRow(label = "Soporte", actionText = "Contactar")
            SettingRow(label = "Términos y privacidad", actionText = "Abrir")
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cerrar sesión",
                    modifier = Modifier
                        .clickable {
                            // Cerrar sesión de Firebase y ejecutar callback de navegación si existe
                            try {
                                FirebaseAuth.getInstance().signOut()
                            } catch (_: Exception) { }
                            onLogout?.invoke()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


@Composable
private fun DriverModeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val animFrac by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(400, easing = LinearEasing),
        label = "driverThumb"
    )
    val trackWidth = 140.dp
    val trackHeight = 48.dp
    val padding = 6.dp
    val thumbSize = trackHeight - padding * 2

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (checked) listOf(Color(0xFF0D9488), Color(0xFF115E59)) else listOf(Color(0xFFEFF2F7), Color(0xFFE2E8F0))
                )
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        val offsetX = with(LocalDensity.current) { ((trackWidth - thumbSize - padding * 2) * animFrac).toPx() }
        Box(
            modifier = Modifier
                .padding(padding)
                .size(thumbSize)
                .graphicsLayer {
                    translationX = offsetX
                    shadowElevation = 10f
                    shape = CircleShape
                    clip = true
                }
                .background(if (checked) Color(0xFF065F46) else Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = Color.White)
            } else {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF1C1C1E))
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    actionText: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f))
            when {
                trailing != null -> trailing()
                actionText != null -> Text(actionText, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ShortcutCardAccount(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String
) {
    Card(
        modifier = modifier
            .size(width = 76.dp, height = 68.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                icon()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}