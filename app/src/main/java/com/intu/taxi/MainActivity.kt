package com.intu.taxi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import com.intu.taxi.location.AdminLocationSimulation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.repeatOnLifecycle
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
    private var adminNotificationUid by mutableStateOf<String?>(null)
    private var updateNotificationRequested by mutableStateOf(false)

    private fun readNotificationIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(com.intu.taxi.updates.AppUpdateNotifications.OPEN_UPDATE_EXTRA, false) == true) {
            updateNotificationRequested = true
        }
        if (com.intu.taxi.push.AdminActivityType.fromKey(intent?.getStringExtra(
                com.intu.taxi.push.PushNotifications.ADMIN_TYPE_EXTRA)) != null) {
            adminNotificationUid = intent?.getStringExtra(com.intu.taxi.push.PushNotifications.ADMIN_UID_EXTRA)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readNotificationIntent(intent)
    }
    // Con la app visible, las solicitudes se ven en pantalla y el servicio no las notifica
    override fun onStart() {
        super.onStart()
        com.intu.taxi.driver.DriverSession.appInForeground = true
        com.intu.taxi.updates.AppUpdateNotifications(this).clearInstalled(BuildConfig.VERSION_CODE)
    }

    override fun onStop() {
        com.intu.taxi.driver.DriverSession.appInForeground = false
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // El tema de arranque (turquesa, sin ícono) solo cubre el instante antes del splash animado
        setTheme(R.style.Theme_Intu)
        super.onCreate(savedInstanceState)
        readNotificationIntent(intent)
        AdminLocationSimulation.initialize()
        com.intu.taxi.push.PushNotifications.createChannel(this)
        enableEdgeToEdge()
        setContent {
            com.intu.taxi.ui.theme.IntuAppearanceHost {
                IntuApp(
                    adminNotificationUid = adminNotificationUid,
                    onAdminNotificationConsumed = { adminNotificationUid = null },
                    updateNotificationRequested = updateNotificationRequested,
                    onUpdateNotificationConsumed = {
                        updateNotificationRequested = false
                        intent?.removeExtra(com.intu.taxi.updates.AppUpdateNotifications.OPEN_UPDATE_EXTRA)
                    },
                )
            }
        }
    }
}

@Composable
fun IntuApp(
    adminNotificationUid: String? = null,
    onAdminNotificationConsumed: () -> Unit = {},
    updateNotificationRequested: Boolean = false,
    onUpdateNotificationConsumed: () -> Unit = {},
) {
    val activity = checkNotNull(LocalActivity.current)
    val navController = rememberNavController()
    val items = listOf(NavItem.Home, NavItem.Trips, NavItem.Account)
    var bottomBarVisible by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }
    // Visibilidad específica de Home, controlada por HomeScreen (pin/ruta)
    var homeBarVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    var isDriverMode by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var notificationPermissionAsked by rememberSaveable { mutableStateOf(false) }
    var showTerms by rememberSaveable { mutableStateOf(false) }
    if (showTerms) com.intu.taxi.ui.screens.TermsDialog(onDismiss = { showTerms = false })
    val auth = FirebaseAuth.getInstance()
    val repo = AuthRepository()
    val scope = rememberCoroutineScope()
    val testLocation by AdminLocationSimulation.preset.collectAsState()
    // Ruta actual para decidir visibilidad combinada
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val updateContext = LocalContext.current
    val updater = remember {
        val repository = com.intu.taxi.updates.AppUpdateRepository(
            updateContext.packageName, android.os.Build.VERSION.SDK_INT)
        com.intu.taxi.updates.AppUpdateController(repository::latest)
    }
    val updateState by updater.state.collectAsState()
    var updateDialogRequested by rememberSaveable { mutableStateOf(false) }
    val riderNotice by com.intu.taxi.rider.RiderTrip.notice.collectAsState()
    val driverRideId by com.intu.taxi.driver.DriverSession.activeRide.collectAsState()
    val tripActive = riderNotice != null || driverRideId != null
    val newerRelease = updateState.newerThan(BuildConfig.VERSION_CODE)
    val mayShowUpdate = !showTerms && !tripActive && (
        currentRoute == "login" || currentRoute == NavItem.Account.route ||
            (currentRoute == NavItem.Home.route && homeBarVisible))
    LaunchedEffect(updateNotificationRequested) {
        if (updateNotificationRequested) updater.check(force = true)
    }
    LaunchedEffect(activity, updater) {
        val lifecycle = (activity as androidx.lifecycle.LifecycleOwner).lifecycle
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                updater.check()
                delay(60_000)
            }
        }
    }
    com.intu.taxi.ui.screens.AppUpdateNotice(
        updateState, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME,
        canPrompt = mayShowUpdate, tripActive = tripActive,
        requested = updateDialogRequested || (updateNotificationRequested && mayShowUpdate),
        onCheck = { scope.launch { updater.check(force = true) } },
        onDownload = { release ->
            // Recheck at the tap: a ride may have started since the dialog was composed.
            if (com.intu.taxi.rider.RiderTrip.notice.value != null ||
                com.intu.taxi.driver.DriverSession.activeRideId != null) false
            else com.intu.taxi.updates.openPublishedUpdate(updateContext, release).also { opened ->
                if (!opened) android.widget.Toast.makeText(updateContext,
                    "No se pudo abrir la descarga. Revisa que tengas un navegador instalado.",
                    android.widget.Toast.LENGTH_LONG).show()
            }
        },
        onRequestConsumed = {
            updateDialogRequested = false
            onUpdateNotificationConsumed()
        },
    )

    // Top-level tabs return to Home instead of exiting or visiting another tab.
    BackHandler(enabled = auth.currentUser != null && !showTerms &&
        (currentRoute == NavItem.Trips.route || currentRoute == NavItem.Account.route)) {
        navController.navigate(NavItem.Home.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Cuenta con sesión; cambia al cerrar sesión, al entrar con otra cuenta o si un admin la elimina
    var currentUid by androidx.compose.runtime.remember { mutableStateOf(auth.currentUser?.uid) }
    LaunchedEffect(adminNotificationUid, currentUid, currentRoute) {
        val target = adminNotificationUid ?: return@LaunchedEffect
        if (target != currentUid) { onAdminNotificationConsumed(); return@LaunchedEffect }
        if (currentRoute !in setOf(NavItem.Home.route, NavItem.Trips.route, NavItem.Account.route, "admin")) return@LaunchedEffect
        val allowed = runCatching { com.intu.taxi.repositories.AdminRepository().isAdmin() }.getOrDefault(false)
        if (allowed && auth.currentUser?.uid == target) {
            navController.navigate("admin") { launchSingleTop = true }
        }
        onAdminNotificationConsumed()
    }
    val authRoutes = setOf("splash", "login", "google_auth", "phone_auth")
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUid = firebaseAuth.currentUser?.uid
            // Sin sesión ya no hay viaje que seguir: se quita el aviso fijo del pasajero
            if (firebaseAuth.currentUser == null) com.intu.taxi.rider.RiderTrip.clear()
            // Sin sesión fuera del inicio de sesión: volver al login
            val route = navController.currentBackStackEntry?.destination?.route
            if (firebaseAuth.currentUser == null && route != null && route !in authRoutes) {
                isDriverMode = false
                bottomBarVisible = false
                navController.navigate("login") {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Cargar el modo conductor de la cuenta con sesión
    LaunchedEffect(currentUid) {
        val uid = currentUid
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
        topBar = {
            testLocation?.let { preset ->
                com.intu.taxi.ui.screens.TestLocationBanner(
                    preset, onRealGps = { AdminLocationSimulation.clear() }, modifier = Modifier.statusBarsPadding()
                )
            }
        },
        bottomBar = {
            // En Home, la visibilidad depende SOLO de homeBarVisible (controlado por HomeScreen).
            // En otras rutas, depende del estado global bottomBarVisible.
            val effectiveVisible = if (currentRoute == NavItem.Home.route) homeBarVisible else bottomBarVisible
            BottomBar(navController = navController, items = items, visible = effectiveVisible)
        }
    ) { scaffoldPadding ->
        val innerPadding = PaddingValues(
            top = if (testLocation == null) scaffoldPadding.calculateTopPadding() else 0.dp,
            bottom = scaffoldPadding.calculateBottomPadding()
        )
        val startDest = "splash"
        NavHost(
            navController = navController,
            modifier = Modifier.padding(top = if (testLocation != null) scaffoldPadding.calculateTopPadding() else 0.dp),
            startDestination = startDest
        ) {
            // Splash: decide destino inicial según autenticación y perfil completo
            composable("splash") {
                bottomBarVisible = false
                val ctx = LocalContext.current
                // Mientras corre la animación se revisa la sesión; se navega cuando ambas cosas terminaron
                var destination by remember { mutableStateOf<String?>(null) }
                var splashDone by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    val current = auth.currentUser
                    destination = if (current == null) {
                        "login"
                    } else {
                        try {
                            val uid = current.uid
                            val existing = try { repo.getUserProfile(uid) } catch (_: Exception) { null }
                            val isComplete = existing?.let {
                                it.firstName.isNotBlank() && it.lastName.isNotBlank() && it.birthdate.isNotBlank() && it.number.isNotBlank()
                            } ?: false
                            if (isComplete) {
                                // Mantiene el nombre al día en Supabase para los viajes; si falla, no bloquea
                                scope.launch { runCatching { repo.syncProfileToSupabase(uid) } }
                                NavItem.Home.route
                            } else {
                                if (current.phoneNumber.isNullOrBlank()) "phone_link_onboarding" else "profile_completion_phone"
                            }
                        } catch (t: Throwable) {
                            android.widget.Toast.makeText(
                                ctx,
                                t.message ?: "Error al verificar perfil",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            NavItem.Home.route
                        }
                    }
                }
                LaunchedEffect(destination, splashDone) {
                    val next = destination ?: return@LaunchedEffect
                    if (!splashDone) return@LaunchedEffect
                    if (next == NavItem.Home.route) bottomBarVisible = true
                    navController.navigate(next) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
                com.intu.taxi.ui.screens.IntuSplash(onFinished = { splashDone = true })
            }
            // Pantalla de login
            composable("login") {
                bottomBarVisible = false
                LoginScreen(
                    onGoogleClick = {
                        navController.navigate("google_auth")
                    },
                    onPhoneClick = { navController.navigate("phone_auth") },
                    onShowTerms = { showTerms = true }
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
                                    navController.navigate(if (user.phoneNumber.isNullOrBlank()) "phone_link_onboarding" else "profile_completion_phone") {
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
            // Phone sign-in verifies the credential before routing; errors remain in the OTP form.
            composable("phone_auth") {
                bottomBarVisible = false
                PhoneAuthScreen(activity = activity, repo = repo,
                    onCancel = { navController.popBackStack() }, onVerified = { credential ->
                        val user = repo.signInWithPhoneCredential(credential) ?: error("No se pudo iniciar sesión.")
                        val existing = repo.getUserProfile(user.uid)
                        val complete = existing != null && existing.firstName.isNotBlank() && existing.lastName.isNotBlank() &&
                            existing.birthdate.isNotBlank() && existing.number.isNotBlank()
                        bottomBarVisible = complete
                        navController.navigate(if (complete) NavItem.Home.route else "profile_completion_phone") {
                            popUpTo("login") { inclusive = true }
                        }
                    })
            }
            composable("phone_link_onboarding") {
                bottomBarVisible = false
                val expectedUid = remember { auth.currentUser?.uid ?: "" }
                PhoneAuthScreen(activity = activity, repo = repo, linking = true,
                    onCancel = { auth.signOut() }, onVerified = { credential ->
                        repo.linkWithCredential(credential, expectedUid)
                        navController.navigate("profile_completion_phone") {
                            popUpTo("phone_link_onboarding") { inclusive = true }
                        }
                    })
            }
            // Completar el perfil con los datos que ya proporciona Firebase.
            composable("profile_completion_phone") {
                bottomBarVisible = false
                val user = auth.currentUser
                val ctx = LocalContext.current
                var savingProfile by remember { mutableStateOf(false) }
                var initialProfile by remember(user?.uid) { mutableStateOf<UserProfile?>(null) }
                var profileLoaded by remember(user?.uid) { mutableStateOf(false) }
                LaunchedEffect(user?.uid) {
                    val loaded = user?.let { runCatching { repo.getUserProfile(it.uid) }.getOrNull() } ?: UserProfile()
                    initialProfile = loaded.copy(
                        firstName = loaded.firstName.ifBlank { user?.displayName?.substringBefore(" ").orEmpty() },
                        lastName = loaded.lastName.ifBlank { user?.displayName?.substringAfter(" ", "").orEmpty() })
                    profileLoaded = true
                }
                if (!profileLoaded) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else
                ProfileCompletionScreen(
                    prefilledPhoneE164 = user?.phoneNumber,
                    prefilledEmail = user?.email,
                    requireEmail = false,
                    isSaving = savingProfile,
                    initialProfile = initialProfile,
                    onSubmit = { profile: UserProfile ->
                        scope.launch {
                            if (savingProfile) return@launch
                            val uid = auth.currentUser?.uid ?: return@launch
                            if (auth.currentUser?.phoneNumber.isNullOrBlank()) {
                                navController.navigate("phone_link_onboarding")
                                return@launch
                            }
                            // Sin un teléfono válido el perfil quedaría incompleto y volvería a pedirse
                            if (com.intu.taxi.data.SupabaseApi.normalizePhone(profile.number) == null && user?.phoneNumber == null) {
                                android.widget.Toast.makeText(ctx, "Ingresa un teléfono válido, por ejemplo 987 654 321", android.widget.Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            // Si el servidor falla, se avisa y se queda en la pantalla (antes la app se cerraba)
                            savingProfile = true
                            runCatching { repo.saveUserProfile(uid, profile) }
                                .onSuccess {
                                    navController.navigate(NavItem.Home.route) {
                                        popUpTo("login") { inclusive = true }
                                    }
                                    bottomBarVisible = true
                                }
                                .onFailure {
                                    android.widget.Toast.makeText(
                                        ctx,
                                        it.message ?: "No se pudo guardar tu perfil. Intenta de nuevo.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            savingProfile = false
                        }
                    },
                    onVerifyEmail = null
                )
            }
            // Driver data collection screen
            composable("driver_data_collection") {
                bottomBarVisible = false
                val context = LocalContext.current
                var submitting by remember { mutableStateOf(false) }
                
                // Restore bottom bar visibility when leaving this screen
                DisposableEffect(Unit) {
                    onDispose {
                        bottomBarVisible = true
                    }
                }
                
                DriverDataCollectionScreen(
                    isConversion = true,
                    isSubmitting = submitting,
                    onSubmit = submitDriver@{ driverProfile ->
                        if (submitting) return@submitDriver
                        submitting = true
                        // Save driver profile and navigate back to account
                        scope.launch {
                            val uid = auth.currentUser?.uid
                            if (uid != null) {
                                try {
                                    AuthRepository().saveDriverProfile(uid, driverProfile)
                                    // La solicitud queda pendiente; vuelve a Cuenta como pasajero.
                                    AuthRepository().setDriverMode(uid, false)
                                    isDriverMode = false
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
                                } finally {
                                    submitting = false
                                }
                            } else {
                                submitting = false
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
                // Avisos push del viaje: registra este teléfono y, en Android 13+, pide permiso una vez
                val homeContext = LocalContext.current
                val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (!granted) {
                        android.widget.Toast.makeText(
                            homeContext,
                            "Sin notificaciones no sabrás cuándo llega tu conductor con la app minimizada",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
                LaunchedEffect(currentUid) {
                    if (currentUid == null) return@LaunchedEffect
                    runCatching { com.intu.taxi.push.PushNotifications.registerToken() }
                    if (!notificationPermissionAsked &&
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            homeContext, android.Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionAsked = true
                        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
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
                val accountContext = LocalContext.current
                // Fuera del modo conductor (pasajero o sesión cerrada) deja de estar en línea
                fun goOffline() {
                    accountContext.getSharedPreferences("intu_driver", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("online", false).apply()
                    com.intu.taxi.driver.DriverOnlineService.stop(accountContext)
                }
                AccountScreenEnhanced(
                    padding = innerPadding,
                    isDriver = isDriverMode,
                    onDriverChange = { newDriverMode ->
                        if (!newDriverMode) goOffline()
                        isDriverMode = newDriverMode
                    },
                    onLogout = {
                        AdminLocationSimulation.clear()
                        goOffline()
                        scope.launch {
                            // Este teléfono deja de recibir los avisos de la cuenta (sin trabar la salida si no hay internet)
                            kotlinx.coroutines.withTimeoutOrNull(4_000) {
                                runCatching { com.intu.taxi.push.PushNotifications.unregisterToken() }
                            }
                            // Antes solo se volvía al login y la sesión seguía abierta. Cerrar también la de
                            // Google permite elegir otra cuenta al entrar. El listener de sesión navega al login.
                            auth.signOut()
                            com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                                accountContext,
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                            ).signOut()
                        }
                    },
                    onNavigateToDriverDataCollection = {
                        navController.navigate("driver_data_collection")
                    },
                    onOpenAdmin = { navController.navigate("admin") },
                    onCheckUpdates = {
                        updateDialogRequested = true
                        scope.launch { updater.check(force = true) }
                    },
                    availableUpdate = newerRelease,
                )
            }
            // Panel de administración (el servidor rechaza todo si la cuenta no es admin)
            composable("admin") {
                LaunchedEffect(Unit) { bottomBarVisible = false }
                DisposableEffect(Unit) { onDispose { bottomBarVisible = true } }
                val adminContext = LocalContext.current
                com.intu.taxi.ui.screens.AdminScreen(
                    padding = innerPadding,
                    onBack = { navController.popBackStack() },
                    onTestLocationSelected = {
                        navController.navigate(NavItem.Home.route) {
                            popUpTo(NavItem.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onOwnAccountDeleted = {
                        AdminLocationSimulation.clear()
                        // La cuenta ya no existe: cerrar sesión (el listener de sesión lleva al login)
                        adminContext.getSharedPreferences("intu_driver", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("online", false).apply()
                        com.intu.taxi.driver.DriverOnlineService.stop(adminContext)
                        auth.signOut()
                        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                            adminContext,
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                        ).signOut()
                    }
                )
            }
        }
    }
}
