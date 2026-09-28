package com.intu.taxi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.auth.AuthRepository
import com.google.firebase.auth.PhoneAuthCredential
import com.intu.taxi.ui.theme.IntuTheme
import com.intu.taxi.ui.BottomBar
import com.intu.taxi.ui.NavItem
import com.intu.taxi.ui.screens.AccountScreenEnhanced
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.screens.DriverHomeScreen
import com.intu.taxi.ui.screens.TripsScreenEnhanced
import com.intu.taxi.ui.screens.LoginScreen
import com.intu.taxi.ui.screens.PhoneAuthScreen
import com.intu.taxi.ui.screens.ProfileCompletionScreen
import com.intu.taxi.ui.screens.DriverDataCollectionScreen
import com.intu.taxi.auth.UserProfile
import com.intu.taxi.ui.screens.GoogleAuthScreen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
// Removed HomeScreen2 import; using HomeScreen as the start page

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntuTheme(darkTheme = false) {
                IntuApp()
            }
        }
    }
}

@Composable
fun IntuApp() {
    val navController = rememberNavController()
    val items = listOf(NavItem.Home, NavItem.Trips, NavItem.Account)
    var bottomBarVisible by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }
    // Visibilidad específica de Home, controlada por HomeScreen (pin/ruta)
    var homeBarVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    var isDriverMode by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val repo = AuthRepository()
    val scope = rememberCoroutineScope()
    // Ruta actual para decidir visibilidad combinada
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Cargar el estado del conductor desde Firestore cuando el usuario esté autenticado
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                isDriverMode = repo.getDriverMode(uid)
            } catch (e: Exception) {
                // Si hay error, mantener el estado por defecto (false)
                isDriverMode = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // En Home, la visibilidad depende SOLO de homeBarVisible (controlado por HomeScreen).
            // En otras rutas, depende del estado global bottomBarVisible.
            val effectiveVisible = if (currentRoute == NavItem.Home.route) homeBarVisible else bottomBarVisible
            BottomBar(navController = navController, items = items, visible = effectiveVisible)
        }
    ) { innerPadding ->
        val startDest = "splash"
        NavHost(
            navController = navController,
            startDestination = startDest
        ) {
            // Splash: decide destino inicial según autenticación y perfil completo
            composable("splash") {
                bottomBarVisible = false
                val ctx = LocalContext.current
                val current = auth.currentUser
                androidx.compose.runtime.LaunchedEffect(current) {
                    if (current == null) {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        scope.launch {
                            try {
                                val uid = current.uid
                                val existing = try { repo.getUserProfile(uid) } catch (_: Exception) { null }
                                val isComplete = existing?.let {
                                    it.firstName.isNotBlank() && it.lastName.isNotBlank() && it.birthdate.isNotBlank() && it.number.isNotBlank()
                                } ?: false
                                if (isComplete) {
                                    // Mantiene el nombre al día en Supabase para los viajes; si falla, no bloquea
                                    scope.launch { runCatching { repo.syncProfileToSupabase(uid) } }
                                    bottomBarVisible = true
                                    navController.navigate(NavItem.Home.route) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("profile_completion_phone") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            } catch (t: Throwable) {
                                android.widget.Toast.makeText(
                                    ctx,
                                    t.message ?: "Error al verificar perfil",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                bottomBarVisible = true
                                navController.navigate(NavItem.Home.route) {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            // Pantalla de login
            composable("login") {
                bottomBarVisible = false
                LoginScreen(
                    onGoogleClick = {
                        navController.navigate("google_auth")
                    },
                    onPhoneClick = { navController.navigate("phone_auth") },
                    onShowTerms = { /* podría mostrar TermsDialog en esta pantalla si se requiere */ }
                )
            }
            // Flujo de Google
            composable("google_auth") {
                bottomBarVisible = false
                val ctx = LocalContext.current
                GoogleAuthScreen(
                    repo = repo,
                    onSignedIn = { user: com.google.firebase.auth.FirebaseUser ->
                        scope.launch {
                            try {
                                val uid = user.uid
                                val existing = try { repo.getUserProfile(uid) } catch (_: Exception) { null }
                                val isComplete = existing?.let {
                                    it.firstName.isNotBlank() && it.lastName.isNotBlank() && it.birthdate.isNotBlank() && it.number.isNotBlank()
                                } ?: false
                                if (isComplete) {
                                    bottomBarVisible = true
                                    navController.navigate(NavItem.Home.route) {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("profile_completion_phone") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            } catch (t: Throwable) {
                                android.widget.Toast.makeText(
                                    ctx,
                                    t.message ?: "Error tras iniciar sesión",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onError = { msg ->
                        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            // Flujo de teléfono (OTP)
            composable("phone_auth") {
                bottomBarVisible = false
                val ctx = LocalContext.current
                val activity = ctx as android.app.Activity
                PhoneAuthScreen(
                    activity = activity,
                    repo = repo,
                    onOtpSent = { _: String, _: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? -> },
                    onVerified = { credential: PhoneAuthCredential ->
                        // Inicia sesión con teléfono y decide si saltar completar perfil según Firestore
                        scope.launch {
                            try {
                                val user = repo.signInWithPhoneCredential(credential)
                                val uid = user?.uid
                                if (uid != null) {
                                    val existing = try { repo.getUserProfile(uid) } catch (_: Exception) { null }
                                    // Considerar perfil completo si tiene nombre, apellido, nacimiento y número
                                    val isComplete = existing != null &&
                                        existing.firstName.isNotBlank() &&
                                        existing.lastName.isNotBlank() &&
                                        existing.birthdate.isNotBlank() &&
                                        existing.number.isNotBlank()
                                    if (isComplete) {
                                        // Perfil existente: ir directo a Inicio
                                        bottomBarVisible = true
                                        navController.navigate(NavItem.Home.route) {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        // Sin perfil: completar perfil
                                        navController.navigate("profile_completion_phone") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                } else {
                                    // Fallback: pedir completar perfil
                                    navController.navigate("profile_completion_phone") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                // Evitar crash en errores de verificación (código inválido, expirado, etc.)
                                try {
                                    android.widget.Toast.makeText(ctx, e.message ?: "Error de verificación", android.widget.Toast.LENGTH_LONG).show()
                                } catch (_: Exception) { }
                            }
                        }
                    },
                    onError = { _: String -> /* TODO mostrar snackbar */ }
                )
            }
            // Completar perfil tras registro por teléfono (email requerido)
            composable("profile_completion_phone") {
                bottomBarVisible = false
                val user = auth.currentUser
                ProfileCompletionScreen(
                    prefilledPhoneE164 = user?.phoneNumber,
                    requireEmail = true,
                    onSubmit = { profile: UserProfile ->
                        scope.launch {
                            val uid = auth.currentUser?.uid ?: return@launch
                            repo.saveUserProfile(uid, profile)
                            navController.navigate(NavItem.Home.route) {
                                popUpTo("login") { inclusive = true }
                            }
                            bottomBarVisible = true
                        }
                    },
                    onVerifyEmail = { repo.sendEmailVerification() }
                )
            }
            // Driver data collection screen
            composable("driver_data_collection") {
                bottomBarVisible = false
                val context = LocalContext.current
                
                // Restore bottom bar visibility when leaving this screen
                DisposableEffect(Unit) {
                    onDispose {
                        bottomBarVisible = true
                    }
                }
                
                DriverDataCollectionScreen(
                    isConversion = true,
                    onSubmit = { driverProfile ->
                        // Save driver profile and navigate back to account
                        scope.launch {
                            val uid = auth.currentUser?.uid
                            if (uid != null) {
                                try {
                                    AuthRepository().saveDriverProfile(uid, driverProfile)
                                    // También establecer isDriver = true cuando se complete el perfil
                                    AuthRepository().setDriverMode(uid, true)
                                    // Actualizar el estado local
                                    isDriverMode = true
                                    navController.navigate(NavItem.Account.route) {
                                        popUpTo(NavItem.Account.route) { inclusive = true }
                                    }
                                    bottomBarVisible = true
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context, 
                                        "Error al guardar perfil de conductor: ${e.message}", 
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
            // Inicio: en modo conductor usa DriverHomeScreen; cliente usa HomeScreen
            composable(NavItem.Home.route) {
                // Asegurar que el BottomBar esté visible solo al entrar a Inicio (una vez)
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    bottomBarVisible = true
                    homeBarVisible = true
                }
                if (isDriverMode) {
                    DriverHomeScreen(
                        padding = innerPadding,
                        onBottomBarVisibilityChanged = { visible -> homeBarVisible = visible }
                    )
                } else {
                    HomeScreen(
                        padding = innerPadding,
                        onBottomBarVisibilityChanged = { visible -> homeBarVisible = visible }
                    )
                }
            }
            // Viajes vuelve a su pantalla original
            composable(NavItem.Trips.route) { TripsScreenEnhanced(padding = innerPadding, isDriver = isDriverMode) }
            composable(NavItem.Account.route) {
                // Ensure bottom bar is visible when entering account screen
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    bottomBarVisible = true
                }
                AccountScreenEnhanced(
                    padding = innerPadding,
                    isDriver = isDriverMode,
                    onDriverChange = { newDriverMode ->
                        isDriverMode = newDriverMode
                        // Guardar el estado en Firestore
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            scope.launch {
                                try {
                                    repo.setDriverMode(uid, newDriverMode)
                                } catch (e: Exception) {
                                    // Si hay error al guardar, revertir el cambio
                                    isDriverMode = !newDriverMode
                                    // El manejo de errores se puede mejorar con un Snackbar o Toast
                                    // Por ahora, solo revertimos el cambio sin mostrar mensaje
                                }
                            }
                        }
                    },
                    onLogout = {
                        bottomBarVisible = false
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    },
                    onNavigateToDriverDataCollection = {
                        navController.navigate("driver_data_collection")
                    }
                )
            }
        }
    }
}