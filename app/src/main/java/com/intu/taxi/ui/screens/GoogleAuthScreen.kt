package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.intu.taxi.auth.AuthRepository
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.intu.taxi.ui.theme.IntuTheme

@Composable
fun GoogleAuthScreen(
    repo: AuthRepository,
    onSignedIn: (com.google.firebase.auth.FirebaseUser) -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit
) {
    val ctx = LocalContext.current
    val loading = remember { mutableStateOf(false) }
    val launched = remember { mutableStateOf(false) }
    val statusMsg = remember { mutableStateOf<String?>(null) }
    
    // Estados de animación
    val cardVisible = remember { mutableStateOf(false) }
    val titleVisible = remember { mutableStateOf(false) }
    val contentVisible = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        loading.value = true
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            // Sign in to Firebase with Google account
            scope.launch {
                try {
                    val user = repo.signInWithGoogleAccount(account)
                    if (user != null) {
                        onSignedIn(user)
                    } else {
                        statusMsg.value = "Usuario inválido tras Google Sign-In"
                        onError(statusMsg.value!!)
                    }
                } catch (t: Throwable) {
                    statusMsg.value = t.message ?: "Error al conectar con Firebase"
                    onError(statusMsg.value!!)
                } finally {
                    loading.value = false
                }
            }
        } catch (e: ApiException) {
            loading.value = false
            val code = e.statusCode
            val readable = com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.getStatusCodeString(code)
            if (code == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                onCancel()
            } else {
                val guidance = if (code == 10) "Verifica SHA-1/256 en Firebase y el default_web_client_id." else ""
                statusMsg.value = "Google Sign-In: ${readable} (${code}). ${guidance}".trim()
                onError(statusMsg.value!!)
            }
        } catch (t: Throwable) {
            loading.value = false
            statusMsg.value = t.message ?: "Fallo inesperado en Google Sign-In"
            onError(statusMsg.value!!)
        }
    }

    LaunchedEffect(Unit) {
        if (!launched.value) {
            launched.value = true
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(ctx.getString(com.intu.taxi.R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(ctx, gso)
            launcher.launch(client.signInIntent)
        }
        
        // Iniciar animaciones secuenciales
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
                .verticalScroll(rememberScrollState())
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
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Logo o icono principal
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
                                imageVector = Icons.Default.Email,
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
                                    "Bienvenido a Intu",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1F47),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Conecta con Google para continuar",
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
                                if (loading.value) {
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
                                        "Conectando con Google...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF08817E)
                                    )
                                }

                                statusMsg.value?.let { msg ->
                                    val isError = msg.contains("ERROR", ignoreCase = true) || 
                                                 msg.contains("Fallo", ignoreCase = true) ||
                                                 msg.contains("inválido", ignoreCase = true)
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

                                GoogleAuthActions(
                                    onCancel = onCancel,
                                    onRetry = {
                                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(ctx.getString(com.intu.taxi.R.string.default_web_client_id))
                                            .requestEmail()
                                            .build()
                                        val client = GoogleSignIn.getClient(ctx, gso)
                                        launcher.launch(client.signInIntent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleAuthActions(onCancel: () -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08817E), contentColor = Color.White)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reintentar", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5F6570))
        ) {
            Text("Cancelar", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }
    }
}

@Preview(name = "Google: botones en ancho estrecho", widthDp = 220, showBackground = true)
@Composable
private fun GoogleAuthActionsNarrowPreview() {
    IntuTheme { GoogleAuthActions(onCancel = {}, onRetry = {}) }
}

@Preview(name = "Google: botones con texto ampliado", widthDp = 220, fontScale = 2f, showBackground = true)
@Composable
private fun GoogleAuthActionsLargeTextPreview() {
    IntuTheme {
        // Also applies the enlarged text when this preview is opened on a QA device.
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
            Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                Column(Modifier.width(220.dp)) { GoogleAuthActions(onCancel = {}, onRetry = {}) }
            }
        }
    }
}
