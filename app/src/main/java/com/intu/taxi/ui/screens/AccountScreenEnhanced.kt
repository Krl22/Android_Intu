package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import com.intu.taxi.ui.theme.LocalAppearanceController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.intu.taxi.auth.AuthRepository
import com.intu.taxi.auth.UserProfile
import com.intu.taxi.data.PaymentPreferences
import com.intu.taxi.ui.nationalPhoneForDisplay
import kotlinx.coroutines.launch

/** Account actions, profile and appearance using the shared Intu page design. */
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
    var showSupportChat by remember { mutableStateOf(false) }
    var supportChatEnabled by remember { mutableStateOf(false) }
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
    // El asistente de Ayuda solo aparece si un admin lo activó
    LaunchedEffect(refreshKey) {
        supportChatEnabled = runCatching { com.intu.taxi.repositories.SupportChatRepository().isEnabled() }.getOrDefault(false)
    }
    if (showBugReport) BugReportDialog(onDismiss = { showBugReport = false })
    if (showSupportChat) SupportChatDialog(
        onDismiss = { showSupportChat = false },
        onReportProblem = { showSupportChat = false; showBugReport = true }
    )
    if (showTerms) TermsDialog(onDismiss = { showTerms = false })
    if (showAccountDeletion) AccountDeletionDialog(onDismiss = { showAccountDeletion = false })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .intuPageBackground()
    ) {
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding() + 24.dp,
                    bottom = padding.calculateBottomPadding() + 20.dp)
        ) {
            // Profile header follows the same brand hierarchy as Inicio and Viajes.
            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
            ) {
                EnhancedHeaderSection(
                    isDriver = isDriver,
                    authUser = authUser,
                    profile = profile,
                    loadingProfile = loadingProfile,
                    handleDriverModeChange = handleDriverModeChange,
                    checkingDriverProfile = checkingDriverProfile,
                    canBecomeDriver = canBecomeDriver
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
                        .padding(top = 24.dp)
                ) {
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
                                .padding(horizontal = 20.dp).intuCardBackground(),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Panel de administración", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Aprobar conductores y reiniciar cuentas de prueba",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Error message display - moved from header to here
                    if (profileError != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp)
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
                        onSupportChat = if (supportChatEnabled) ({ showSupportChat = true }) else null,
                        onTerms = { showTerms = true },
                        onAccountDeletion = { showAccountDeletion = true },
                        onCheckUpdates = onCheckUpdates
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedHeaderSection(
    isDriver: Boolean,
    authUser: com.google.firebase.auth.FirebaseUser?,
    profile: UserProfile?,
    loadingProfile: Boolean,
    handleDriverModeChange: (Boolean) -> Unit,
    checkingDriverProfile: Boolean,
    canBecomeDriver: () -> Boolean
) {
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

    val colors = MaterialTheme.colorScheme
    val displayName = listOfNotNull(profile?.firstName, profile?.lastName)
        .joinToString(" ").ifBlank { authUser?.displayName ?: "Mi cuenta" }
    val phoneNumber = profile?.number?.takeIf { it.isNotBlank() }
        ?: authUser?.phoneNumber?.takeIf { it.isNotBlank() }
    val phoneLabel = phoneNumber?.let(::nationalPhoneForDisplay)?.ifBlank { "Sin número" } ?: "Sin número"
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        IntuPageHeading("Tu cuenta", "Tus preferencias y todo lo que necesitas de Intu.")
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.intuCardBackground(emphasized = true),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Column(Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(72.dp)) {
                        Box(Modifier.fillMaxSize().clip(CircleShape)
                            .background(colors.primaryContainer)
                            .clickable(enabled = !isUploadingPhoto) { takePhotoLauncher.launch(captureUri) },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, null, tint = colors.primary, modifier = Modifier.size(36.dp))
                            if (!photoUrl.isNullOrBlank()) {
                                coil.compose.AsyncImage(model = photoUrl, contentDescription = "Tu foto de perfil",
                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            if (isUploadingPhoto) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                            }
                        }
                        Surface(onClick = { takePhotoLauncher.launch(captureUri) }, enabled = !isUploadingPhoto,
                            shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary,
                            modifier = Modifier.size(32.dp).align(Alignment.BottomEnd)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.PhotoCamera, "Cambiar foto de perfil", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (loadingProfile) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(displayName, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            Text(phoneLabel, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            Text(if (isDriver) "Conductor" else "Pasajero", style = MaterialTheme.typography.labelMedium,
                                color = colors.primary)
                        }
                    }
                }
                if (canBecomeDriver()) {
                    HorizontalDivider(color = colors.outlineVariant)
                    Row(Modifier.fillMaxWidth().testTag("account-driver-mode")
                        .toggleable(value = isDriver, role = Role.Switch,
                            enabled = !checkingDriverProfile, onValueChange = handleDriverModeChange),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Modo conductor", modifier = Modifier.weight(1f), color = colors.onSurface)
                        Switch(checked = isDriver, onCheckedChange = null, enabled = !checkingDriverProfile)
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
            .padding(horizontal = 20.dp).intuCardBackground(emphasized = true),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp)
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
    onSupportChat: (() -> Unit)?,
    onTerms: () -> Unit,
    onAccountDeletion: () -> Unit,
    onCheckUpdates: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp).intuCardBackground(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.testTag("account-settings").padding(vertical = 8.dp)
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
                    else -> MaterialTheme.colorScheme.primary
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
            
            val appearance = LocalAppearanceController.current
            SettingsItemEnhanced(
                icon = Icons.Outlined.DarkMode,
                title = "Modo oscuro",
                subtitle = "Usar colores oscuros en Intu",
                checked = appearance.darkMode,
                onCheckedChange = appearance.setDarkMode,
                modifier = Modifier.testTag("account-dark-mode")
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
            if (onSupportChat != null) SettingsItemEnhanced(
                icon = Icons.Outlined.QuestionAnswer,
                title = "Ayuda con Intu",
                subtitle = "Resuelve tus dudas al instante",
                actionText = "Preguntar",
                onClick = onSupportChat
            )
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
    enabled: Boolean = true,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val actionModifier = if (checked != null && onCheckedChange != null) {
        modifier.toggleable(value = checked, role = Role.Switch, enabled = enabled,
            onValueChange = onCheckedChange)
    } else modifier.clickable(enabled = enabled, onClick = onClick)
    Row(
        modifier = actionModifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                color = colors.onSurface)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (checked != null) {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                actionText?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium,
                        color = AppearanceColors.highlight(actionColor), fontWeight = FontWeight.Medium)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null,
                    tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
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
