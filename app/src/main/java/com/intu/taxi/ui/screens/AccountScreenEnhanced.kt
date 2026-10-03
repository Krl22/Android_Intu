package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import com.intu.taxi.ui.theme.LocalAppearanceController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.intu.taxi.auth.AuthRepository
import com.intu.taxi.auth.UserProfile
import com.intu.taxi.data.PaymentPreferences
import com.intu.taxi.ui.nationalPhoneForDisplay
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.intu.taxi.auth.DriverProfile

/**
 * Enhanced Account Screen with modern design principles:
 * - Glassmorphism effects
 * - Smooth animations
 * - Better visual hierarchy
 * - Consistent color scheme
 * - Improved typography
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreenEnhanced(
    padding: PaddingValues,
    isDriver: Boolean,
    onDriverChange: (Boolean) -> Unit,
    onLogout: (() -> Unit)? = null,
    onNavigateToDriverDataCollection: (() -> Unit)? = null,
    onOpenAdmin: (() -> Unit)? = null,
    onCheckUpdates: () -> Unit = {}
) {
    val repo = remember { AuthRepository() }
    val auth = FirebaseAuth.getInstance()
    // El panel de administración solo aparece para cuentas admin (lo decide el servidor)
    var isAdmin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isAdmin = runCatching { com.intu.taxi.repositories.AdminRepository().isAdmin() }.getOrDefault(false)
    }
    val authUser = auth.currentUser
    
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loadingProfile by remember { mutableStateOf(true) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var hasCompleteDriverProfile by remember { mutableStateOf(false) }
    var checkingDriverProfile by remember { mutableStateOf(true) }
    var driverAccess by remember { mutableStateOf(com.intu.taxi.auth.DriverAccess(null, false)) }
    var driverStatusLoaded by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var showSavedPlaces by remember { mutableStateOf(false) }
    var showBugReport by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showAccountDeletion by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = checkNotNull(androidx.activity.compose.LocalActivity.current)
    
    var googleLinked by remember {
        mutableStateOf(
            authUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
        )
    }
    

    var googleLinkingInProgress by rememberSaveable { mutableStateOf(false) }
    var googleLinkUid by rememberSaveable { mutableStateOf<String?>(null) }
    var phoneLinkUid by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Refresh Google account status
    val refreshGoogleStatus: () -> Unit = {
        googleLinked = auth.currentUser?.providerData?.any { 
            it.providerId == GoogleAuthProvider.PROVIDER_ID 
        } == true
    }
    
    // Google Sign-In launcher for account linking
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        googleLinkingInProgress = true
        try {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            
            scope.launch {
                try {
                    val user = repo.linkWithGoogleAccount(account, googleLinkUid ?: error("Tu sesión cambió. Intenta de nuevo."))
                    googleLinked = true
                    val synced = runCatching { repo.syncProfileToSupabase(user.uid) }.isSuccess
                    refreshKey++
                    statusMessage = if (synced) "Google vinculado. Puedes entrar con Google o tu teléfono a esta misma cuenta."
                        else "Google vinculado. Revisa tu conexión para actualizar los datos del perfil."
                    refreshGoogleStatus()
                } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    statusMessage = com.intu.taxi.auth.authErrorMessage(e, linking = true)
                } catch (e: Exception) {
                    statusMessage = com.intu.taxi.auth.authErrorMessage(e, linking = true)
                } finally {
                    googleLinkingInProgress = false
                }
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            googleLinkingInProgress = false
            if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                statusMessage = "Error de Google Sign-In: ${e.message}"
            }
        }
    }
    
    phoneLinkUid?.let { expectedUid ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { phoneLinkUid = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
            Surface(Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(12.dp), shape = RoundedCornerShape(24.dp)) {
                PhoneAuthScreen(activity = activity, repo = repo, linking = true,
                    initialPhone = auth.currentUser?.phoneNumber ?: profile?.number.orEmpty(),
                    onCancel = { phoneLinkUid = null }, onVerified = { credential ->
                        val linkedUser = repo.linkWithCredential(credential, expectedUid)
                        val synced = runCatching { repo.syncProfileToSupabase(linkedUser.uid) }.isSuccess
                        phoneLinkUid = null
                        refreshKey++
                        statusMessage = if (synced) "Número vinculado. Puedes entrar por SMS a esta misma cuenta."
                            else "Número vinculado. Revisa tu conexión para actualizar los datos del perfil."
                    })
            }
        }
    }

    // Animation states
    var headerVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    
    // Payment preferences
    val paymentPreferences = remember { PaymentPreferences(context) }
    var currentPaymentMethod by remember { mutableStateOf("efectivo") }
    
    // Load current payment method
    LaunchedEffect(Unit) {
        paymentPreferences.paymentMethod.collect { method ->
            currentPaymentMethod = method
        }
    }
    
    LaunchedEffect(Unit) {
        headerVisible = true
        kotlinx.coroutines.delay(300)
        contentVisible = true
    }
    
    LaunchedEffect(authUser?.uid, refreshKey) {
        loadingProfile = true
        checkingDriverProfile = true
        driverStatusLoaded = false
        profileError = null
        val uid = authUser?.uid
        try {
            if (uid == null) error("Inicia sesión para continuar.")
            profile = repo.getUserProfile(uid)
            driverAccess = repo.getDriverAccess(uid)
            hasCompleteDriverProfile = driverAccess.completeProfile
            driverStatusLoaded = true
            if ((isDriver || profile?.isDriver == true) && !driverAccess.canDrive) {
                repo.setDriverMode(uid, false)
                onDriverChange(false)
                profile = profile?.copy(isDriver = false)
            }
        } catch (e: Exception) {
            profileError = e.message ?: "No se pudo cargar tu cuenta."
        } finally {
            checkingDriverProfile = false
            loadingProfile = false
            refreshGoogleStatus()
        }
    }

    val canBecomeDriver: () -> Boolean = { driverStatusLoaded && driverAccess.canDrive }
    val handleDriverModeChange: (Boolean) -> Unit = { newMode ->
        if (!checkingDriverProfile) scope.launch {
            checkingDriverProfile = true
            try {
                val uid = authUser?.uid ?: error("Inicia sesión para continuar.")
                repo.setDriverMode(uid, newMode)
                onDriverChange(newMode)
                profile = profile?.copy(isDriver = newMode)
                profileError = null
            } catch (e: Exception) {
                profileError = e.message ?: "No se pudo cambiar de modo."
                refreshKey++
            } finally {
                checkingDriverProfile = false
            }
        }
    }

    if (showSavedPlaces) SavedPlacesDialog(onDismiss = { showSavedPlaces = false })
    if (showBugReport) BugReportDialog(onDismiss = { showBugReport = false })
    if (showTerms) TermsDialog(onDismiss = { showTerms = false })
    if (showAccountDeletion) AccountDeletionDialog(onDismiss = { showAccountDeletion = false })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Animated background gradient
        AnimatedGradientBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // Enhanced Header with glassmorphism
            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
            ) {
                EnhancedHeaderSection(
                    isDriver = isDriver,
                    onDriverChange = onDriverChange,
                    authUser = authUser,
                    profile = profile,
                    loadingProfile = loadingProfile,
                    profileError = profileError,
                    handleDriverModeChange = handleDriverModeChange,
                    checkingDriverProfile = checkingDriverProfile,
                    hasCompleteDriverProfile = hasCompleteDriverProfile,
                    canBecomeDriver = canBecomeDriver,
                    googleLinked = googleLinked
                )
            }
            
            // Enhanced Content Section
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    AccountAppearanceSection()
                    Spacer(modifier = Modifier.height(16.dp))

                    val showDriverStats = isDriver && driverAccess.canDrive
                    if (showDriverStats) {
                        DriverStatsSection()
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (isAdmin && onOpenAdmin != null) {
                        Card(
                            onClick = onOpenAdmin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F47)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Panel de administración", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Aprobar conductores y reiniciar cuentas de prueba",
                                        color = Color.White.copy(alpha = 0.75f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Error message display - moved from header to here
                    if (profileError != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    "Error al cargar perfil",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    profileError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    SettingsSection(
                        authUser = authUser,
                        profile = profile,
                        googleLinked = googleLinked,
                        googleLinkingInProgress = googleLinkingInProgress,
                        onGoogleLinkClick = {
                            statusMessage = null
                            googleLinkUid = auth.currentUser?.uid
                            googleLinkingInProgress = true
                            // Launch Google Sign-In for account linking
                            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                            )
                                .requestIdToken(context.getString(com.intu.taxi.R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        onPhoneLinkClick = { phoneLinkUid = auth.currentUser?.uid },

                        isDriver = isDriver,
                        onLogout = onLogout,
                        canBecomeDriver = canBecomeDriver,
                        onNavigateToDriverDataCollection = onNavigateToDriverDataCollection,
                        onShowError = { errorMessage -> profileError = errorMessage },
                        hasCompleteDriverProfile = hasCompleteDriverProfile,
                        scope = scope,
                        repo = repo,
                        onProfileUpdate = { updatedProfile -> profile = updatedProfile },
                        currentPaymentMethod = currentPaymentMethod,
                        paymentPreferences = paymentPreferences,
                        driverAccess = driverAccess,
                        driverStatusLoaded = driverStatusLoaded,
                        onRefreshDriverStatus = { refreshKey++ },
                        onSavedPlaces = { showSavedPlaces = true },
                        onBugReport = { showBugReport = true },
                        onTerms = { showTerms = true },
                        onAccountDeletion = { showAccountDeletion = true },
                        onCheckUpdates = onCheckUpdates
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountAppearanceSection() {
    val appearance = LocalAppearanceController.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = AppearanceColors.surface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            "Apariencia",
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .testTag("account-dark-mode")
                .toggleable(value = appearance.darkMode, role = Role.Switch,
                    onValueChange = appearance.setDarkMode)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.DarkMode, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text("Modo oscuro", style = MaterialTheme.typography.bodyLarge)
                Text("Usar colores oscuros en Intu", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = appearance.darkMode, onCheckedChange = null)
        }
    }
}

@Composable
private fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientOffset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF08817E), // teal
                        Color(0xFF1E1F47), // indigo
                        Color(0xFF08817E).copy(alpha = 0.6f),
                        Color(0xFF1E1F47).copy(alpha = 0.8f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
            .alpha(0.1f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedHeaderSection(
    isDriver: Boolean,
    onDriverChange: (Boolean) -> Unit,
    authUser: com.google.firebase.auth.FirebaseUser?,
    profile: UserProfile?,
    loadingProfile: Boolean,
    profileError: String?,
    handleDriverModeChange: (Boolean) -> Unit,
    checkingDriverProfile: Boolean,
    hasCompleteDriverProfile: Boolean,
    canBecomeDriver: () -> Boolean,
    googleLinked: Boolean
) {
    // Fixed height since error message will be displayed outside the card
    val totalHeight = 280.dp
    val cardTotalHeight = 240.dp
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // La foto se sube a Storage y queda en la cuenta; así el otro la ve en cada viaje.
    // Solo con la cámara (no galería), para que sea una foto real y actual de la persona.
    var photoUrl by remember { mutableStateOf(authUser?.photoUrl?.toString()) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    val captureUri = remember {
        val dir = java.io.File(context.cacheDir, "profile_photos").apply { mkdirs() }
        androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", java.io.File(dir, "profile.jpg")
        )
    }
    val takePhotoLauncher = rememberLauncherForActivityResult(FrontCameraTakePicture()) { saved ->
        if (!saved || isUploadingPhoto) return@rememberLauncherForActivityResult
        val uri = captureUri
        isUploadingPhoto = true
        scope.launch {
            runCatching { com.intu.taxi.auth.AuthRepository().uploadProfilePhoto(context, uri) }
                .onSuccess {
                    photoUrl = it
                    android.widget.Toast.makeText(context, "Foto actualizada", android.widget.Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    android.widget.Toast.makeText(context, it.message ?: "No se pudo subir la foto", android.widget.Toast.LENGTH_LONG).show()
                }
            isUploadingPhoto = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        // Glassmorphism background
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardTotalHeight)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color.Black.copy(alpha = 0.2f)
                ),
            colors = CardDefaults.cardColors(
                containerColor = AppearanceColors.surface.copy(alpha = 0.2f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f)
                            )
                        )
                    )
            ) {
                // Driver mode toggle - show for existing drivers or users with complete driver profile
                val showDriverToggle = canBecomeDriver()
                if (showDriverToggle) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        DriverModeToggleEnhanced(
                            checked = isDriver,
                            onCheckedChange = { newMode -> handleDriverModeChange(newMode) },
                            enabled = !checkingDriverProfile && (isDriver || canBecomeDriver()),
                            modifier = Modifier
                        )
                        
                        if (checkingDriverProfile) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(12.dp)
                                    .padding(top = 4.dp),
                                strokeWidth = 2.dp,
                                color = AppearanceColors.highlight(Color(0xFF08817E))
                            )
                        }
                    }
                }
                
                // Profile content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Enhanced profile picture
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable { takePhotoLauncher.launch(captureUri) }
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF08817E), // teal
                                        Color(0xFF1E1F47)  // indigo
                                    )
                                )
                            )
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                        if (!photoUrl.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = photoUrl,
                                contentDescription = "Tu foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (isUploadingPhoto) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                            }
                        }
                        
                        // Edit overlay
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AppearanceColors.surface)
                                .align(Alignment.BottomEnd)
                                .border(2.dp, Color(0xFF08817E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppearanceColors.highlight(Color(0xFF08817E))
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // User info
                    when {
                        loadingProfile -> {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        }
                        else -> {
                            val displayName = listOfNotNull(profile?.firstName, profile?.lastName)
                                .joinToString(" ").ifBlank { authUser?.displayName ?: "Mi cuenta" }
                            val phoneNumber = profile?.number?.takeIf { it.isNotBlank() }
                                ?: authUser?.phoneNumber?.takeIf { it.isNotBlank() }
                            val phoneLabel = phoneNumber?.let(::nationalPhoneForDisplay)?.ifBlank { "Sin número" }
                                ?: "Sin número"
                            
                            Text(
                                displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Text(
                                when {
                                    isDriver -> "Conductor • $phoneLabel"
                                    else -> phoneLabel
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverStatsSection() {
    // Ganancias y calificación reales del conductor (antes eran montos de ejemplo)
    val historyRepo = remember { com.intu.taxi.repositories.RideHistoryRepository() }
    var todayTotal by remember { mutableStateOf<Double?>(null) }
    var weekTotal by remember { mutableStateOf<Double?>(null) }
    var rating by remember { mutableStateOf<Pair<Double, Int>?>(null) }
    LaunchedEffect(Unit) {
        runCatching { historyRepo.driverHistory() }.onSuccess { rides ->
            val (today, week, _) = com.intu.taxi.repositories.RideHistoryRepository.earnings(rides)
            todayTotal = today.total
            weekTotal = week.total
        }
        rating = runCatching { historyRepo.driverRating() }.getOrNull()
    }
    val stats = listOf(
        Triple("Hoy", todayTotal?.let { com.intu.taxi.ui.formatSoles(it) } ?: "—", Icons.Default.TrendingUp),
        Triple("7 días", weekTotal?.let { com.intu.taxi.ui.formatSoles(it) } ?: "—", Icons.Default.CalendarToday),
        Triple(
            "Calificación",
            rating?.takeIf { it.second > 0 }?.let { String.format(java.util.Locale.US, "%.1f", it.first) } ?: "—",
            Icons.Default.Star
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppearanceColors.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stats.forEach { (label, value, icon) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppearanceColors.highlight(Color(0xFF08817E)),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppearanceColors.foreground(Color(0xFF1C1C1E))
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppearanceColors.secondary(Color(0xFF6B7280))
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    authUser: com.google.firebase.auth.FirebaseUser?,
    profile: UserProfile?,
    googleLinked: Boolean,
    googleLinkingInProgress: Boolean,
    onGoogleLinkClick: () -> Unit,
    onPhoneLinkClick: () -> Unit,

    isDriver: Boolean,
    onLogout: (() -> Unit)?,
    canBecomeDriver: () -> Boolean,
    onNavigateToDriverDataCollection: (() -> Unit)?,
    onShowError: (String) -> Unit,
    hasCompleteDriverProfile: Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    repo: AuthRepository,
    onProfileUpdate: (UserProfile?) -> Unit,
    currentPaymentMethod: String,
    paymentPreferences: PaymentPreferences,
    driverAccess: com.intu.taxi.auth.DriverAccess,
    driverStatusLoaded: Boolean,
    onRefreshDriverStatus: () -> Unit,
    onSavedPlaces: () -> Unit,
    onBugReport: () -> Unit,
    onTerms: () -> Unit,
    onAccountDeletion: () -> Unit,
    onCheckUpdates: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppearanceColors.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            // Email verification - prioritize Firestore profile email, fallback to Firebase Auth
            // Google account
            SettingsItemEnhanced(
                icon = Icons.Default.Link,
                title = "Acceso con Google",
                subtitle = when {
                    googleLinkingInProgress -> "Vinculando..."
                    googleLinked -> authUser?.email ?: "Vinculado a esta cuenta"
                    else -> "Vincula Google para entrar también con tu correo"
                },
                actionText = when {
                    googleLinkingInProgress -> "Procesando..."
                    googleLinked -> "Vinculado"
                    else -> "Vincular"
                },
                actionColor = when {
                    googleLinkingInProgress -> Color.Gray
                    googleLinked -> Color(0xFF10B981)
                    else -> Color(0xFF667eea)
                },
                onClick = {
                    if (!googleLinked && !googleLinkingInProgress) {
                        onGoogleLinkClick()
                    }
                },
                enabled = !googleLinkingInProgress
            )
            val verifiedPhone = authUser?.phoneNumber?.takeIf { it.isNotBlank() }
            SettingsItemEnhanced(
                icon = Icons.Default.Phone,
                title = "Acceso con teléfono",
                subtitle = verifiedPhone?.let(::nationalPhoneForDisplay)
                    ?: profile?.number?.takeIf { it.isNotBlank() }?.let { "${nationalPhoneForDisplay(it)} · Pendiente de verificar por SMS" }
                    ?: "Verifica tu celular para entrar por SMS",
                actionText = if (verifiedPhone == null) "Vincular" else "Cambiar",
                actionColor = Color(0xFF08817E),
                onClick = onPhoneLinkClick,
                enabled = !googleLinkingInProgress
            )
            
            // Payment methods
            SettingsItemEnhanced(
                icon = Icons.Default.Payment,
                title = "Métodos de pago",
                subtitle = when (currentPaymentMethod) {
                    "efectivo" -> "Efectivo (predeterminado)"
                    "yape_plin" -> "Yape"
                    else -> "Gestionar métodos de pago"
                },
                actionText = "Cambiar",
                onClick = { 
                    // Toggle between payment methods
                    val newMethod = if (currentPaymentMethod == "efectivo") "yape_plin" else "efectivo"
                    scope.launch {
                        paymentPreferences.savePaymentMethod(newMethod)
                    }
                }
            )
            
            // Earn as driver - only show for non-drivers who need to complete driver info
                    val shouldShowDriverOption = driverStatusLoaded && driverAccess.canApply
                    if (shouldShowDriverOption) {
                        SettingsItemEnhanced(
                            icon = Icons.Default.DirectionsCar,
                            title = "Ganar como conductor",
                            subtitle = "Mototaxi o moto lineal para reparto",
                            actionText = "Comenzar",
                            actionColor = Color(0xFF08817E),
                            onClick = {
                                // Check email verification first
                                val displayEmail = authUser?.email
                                if (displayEmail.isNullOrEmpty()) {
                                    onShowError("Debes agregar un correo electrónico primero")
                                } else if (authUser?.isEmailVerified != true) {
                                    onShowError("Por favor verificar correo para convertirse en conductor")
                                } else {
                                    // Navigate to driver data collection
                                    onNavigateToDriverDataCollection?.invoke()
                                    // Note: Profile refresh will happen automatically when returning to this screen
                                }
                            }
                        )
                    }
            
            if (driverStatusLoaded && driverAccess.message != null) {
                SettingsItemEnhanced(
                    icon = Icons.Default.Info,
                    title = if (driverAccess.status == "pending") "Solicitud en revisión"
                        else if (driverAccess.vehicleType == com.intu.taxi.auth.DriverVehicleType.MOTORCYCLE) "Solicitud de repartidor"
                        else "Solicitud de conductor",
                    subtitle = driverAccess.message.orEmpty(),
                    actionText = "Actualizar",
                    onClick = onRefreshDriverStatus
                )
            }

            // Saved addresses
            SettingsItemEnhanced(
                icon = Icons.Default.Place,
                title = "Direcciones guardadas",
                subtitle = "Casa, trabajo y favoritas",
                actionText = "Editar",
                onClick = onSavedPlaces
            )
            
            // Support
            SettingsItemEnhanced(
                icon = Icons.Default.Support,
                title = "Reportar un error",
                subtitle = "Cuéntanos qué falló en Intu",
                actionText = "Reportar",
                onClick = onBugReport
            )
            
            // Terms and privacy
            SettingsItemEnhanced(
                icon = Icons.Outlined.SystemUpdate,
                title = "Actualizaciones",
                subtitle = "Intu ${com.intu.taxi.BuildConfig.VERSION_NAME} · Buscar una nueva versión",
                actionText = "Comprobar",
                actionColor = Color(0xFF08817E),
                onClick = onCheckUpdates
            )

            SettingsItemEnhanced(
                icon = Icons.Default.Description,
                title = "Términos y privacidad",
                subtitle = "Políticas de la app",
                actionText = "Abrir",
                onClick = onTerms
            )

            SettingsItemEnhanced(
                icon = Icons.Default.DeleteOutline,
                title = "Eliminar cuenta",
                subtitle = "Solicita eliminar tu cuenta y tus datos",
                actionText = "Solicitar",
                actionColor = MaterialTheme.colorScheme.error,
                onClick = onAccountDeletion
            )
            
            // Driver approval section removed - approval is now handled server-side
            
            // Logout
            if (onLogout != null) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsItemEnhanced(
                    icon = Icons.Default.Logout,
                    title = "Cerrar sesión",
                    subtitle = "Salir de tu cuenta",
                    actionText = "Cerrar",
                    actionColor = MaterialTheme.colorScheme.error,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
private fun SettingsItemEnhanced(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    actionColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {},
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF08817E).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF667eea),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppearanceColors.foreground(Color(0xFF1C1C1E))
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppearanceColors.secondary(Color(0xFF6B7280)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                actionText?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppearanceColors.highlight(actionColor),
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DriverModeToggleEnhanced(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "toggleScale"
    )
    
    Card(
        modifier = modifier
            .scale(scale)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        colors = CardDefaults.cardColors(
            containerColor = when {
                !enabled -> Color.Gray.copy(alpha = 0.3f)
                checked -> Color(0xFF08817E)
                else -> AppearanceColors.surface.copy(alpha = 0.8f)
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (checked) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (checked) Icons.Default.DirectionsCar else Icons.Default.Person,
                contentDescription = null,
                tint = if (checked && enabled) Color.White else if (!enabled) AppearanceColors.secondary(Color.Gray) else AppearanceColors.muted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                if (checked) "Conductor" else "Pasajero",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (checked && enabled) Color.White else if (!enabled) AppearanceColors.secondary(Color.Gray) else AppearanceColors.muted
            )
        }
    }
}

/**
 * Toma la foto con la cámara pidiendo la frontal (selfie). Estos extras no son oficiales, pero la
 * mayoría de apps de cámara los respetan; si no, se abre la cámara trasera.
 */
private class FrontCameraTakePicture : androidx.activity.result.contract.ActivityResultContracts.TakePicture() {
    override fun createIntent(context: android.content.Context, input: android.net.Uri): android.content.Intent =
        super.createIntent(context, input).apply {
            putExtra("android.intent.extras.CAMERA_FACING", 1)
            putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        }
}
