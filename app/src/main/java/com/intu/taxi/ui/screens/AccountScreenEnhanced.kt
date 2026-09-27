package com.intu.taxi.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    onNavigateToDriverDataCollection: (() -> Unit)? = null
) {
    val repo = remember { AuthRepository() }
    val auth = FirebaseAuth.getInstance()
    val authUser = auth.currentUser
    
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loadingProfile by remember { mutableStateOf(true) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var hasCompleteDriverProfile by remember { mutableStateOf(false) }
    var checkingDriverProfile by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var googleLinked by remember {
        mutableStateOf(
            authUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
        )
    }
    
    var emailVerificationSent by remember { mutableStateOf(false) }
    var googleLinkingInProgress by remember { mutableStateOf(false) }
    
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
                    val user = repo.linkWithGoogleAccount(account)
                    if (user != null) {
                        googleLinked = true
                        statusMessage = "Cuenta de Google vinculada exitosamente"
                        // Refresh the Google status to ensure UI updates
                        refreshGoogleStatus()
                        // Clear status message after 5 seconds
                        kotlinx.coroutines.delay(5000)
                        statusMessage = null
                    } else {
                        statusMessage = "Error al vincular cuenta de Google"
                        // Clear status message after 3 seconds
                        kotlinx.coroutines.delay(3000)
                        statusMessage = null
                    }
                } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    statusMessage = "Esta cuenta de Google ya está vinculada a otro usuario"
                    // Clear status message after 5 seconds
                    kotlinx.coroutines.delay(5000)
                    statusMessage = null
                } catch (e: Exception) {
                    statusMessage = "Error al vincular cuenta: ${e.message}"
                    // Clear status message after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    statusMessage = null
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
    
    LaunchedEffect(authUser?.uid) {
        loadingProfile = true
        profileError = null
        val uid = authUser?.uid
        if (uid != null) {
            try {
                profile = repo.getUserProfile(uid)
                // Debug: Log the profile data to understand what's happening
                println("DEBUG: Profile loaded - email: ${profile?.email}, firstName: ${profile?.firstName}, lastName: ${profile?.lastName}")
                println("DEBUG: Firebase Auth email: ${authUser?.email}")
                
                // Check if user has complete driver profile
                checkingDriverProfile = true
                hasCompleteDriverProfile = repo.hasCompleteDriverProfile(uid)
                checkingDriverProfile = false
            } catch (e: Exception) {
                profileError = "Error: ${e.message ?: "Error desconocido"}"
                checkingDriverProfile = false
                println("DEBUG: Error loading profile: ${e.message}")
            }
        } else {
            // No user logged in
            profileError = "No hay usuario autenticado"
        }
        googleLinked = auth.currentUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
        loadingProfile = false
        refreshGoogleStatus()
    }

    // Refresh driver profile status when screen becomes visible
    LaunchedEffect(Unit) {
        // Additional refresh for driver profile status when returning to this screen
        val uid = authUser?.uid
        if (uid != null) {
            checkingDriverProfile = true
            try {
                // Refresh complete driver profile status
                hasCompleteDriverProfile = repo.hasCompleteDriverProfile(uid)
                println("DEBUG: Driver profile refresh - hasCompleteDriverProfile: $hasCompleteDriverProfile")
                
                // Also refresh the user profile to ensure latest data
                val updatedProfile = repo.getUserProfile(uid)
                if (updatedProfile != null) {
                    profile = updatedProfile
                    println("DEBUG: Refreshed user profile after returning to screen")
                }
            } catch (e: Exception) {
                println("DEBUG: Error refreshing driver profile: ${e.message}")
            } finally {
                checkingDriverProfile = false
            }
        }
    }

    // Check if user can become a driver
    val canBecomeDriver: () -> Boolean = {
        val displayEmail = profile?.email ?: authUser?.email
        val isEmailVerified = authUser?.isEmailVerified == true
        val hasGoogleAccount = googleLinked
        val hasDriverData = hasCompleteDriverProfile
        !displayEmail.isNullOrEmpty() && isEmailVerified && hasGoogleAccount && hasDriverData
    }

    // Handle driver mode change with validation
    val handleDriverModeChange: (Boolean) -> Unit = { newDriverMode ->
        scope.launch {
            println("DEBUG: Driver mode change triggered - newMode=$newDriverMode")
            if (newDriverMode) {
                val displayEmail = profile?.email ?: authUser?.email
                val isEmailVerified = authUser?.isEmailVerified == true
                val hasGoogleAccount = googleLinked
                val hasDriverData = hasCompleteDriverProfile
                val uid = authUser?.uid
                
                // Fetch fresh profile data from Firestore
                var remoteProfile: UserProfile? = null
                var remoteApproved: Boolean? = null
                var actualIsApproved: Boolean? = null
                if (uid != null) {
                    try { 
                        println("DEBUG: About to fetch user profile from Firestore for UID: $uid")
                        
                        // Primero verificar datos crudos en Firestore
                        println("DEBUG: Checking raw Firestore data...")
                        val rawData = repo.checkRawFirestoreData(uid)
                        
                        // Luego obtener el perfil mapeado
                        remoteProfile = repo.getUserProfile(uid)
                        
                        // Usar la nueva función para obtener el valor correcto de isApproved
                        actualIsApproved = repo.getIsApprovedValue(uid)
                        println("DEBUG: Actual isApproved value from Firestore: $actualIsApproved")
                        
                        remoteApproved = actualIsApproved
                        profile = remoteProfile // Update local profile with fresh data
                        
                        println("DEBUG: Comparison - Raw isApproved: ${rawData?.get("isApproved")} vs Mapped isApproved: $remoteApproved")
                        println("DEBUG: Complete profile data: $remoteProfile")
                        
                        // Verificación adicional - si el perfil trae datos pero isApproved es null
                        if (remoteProfile != null && remoteApproved == null) {
                            println("DEBUG: WARNING - Profile exists but isApproved is null. This might indicate:")
                            println("DEBUG: 1. Field doesn't exist in Firestore")
                            println("DEBUG: 2. Field has different name/capitalization")
                            println("DEBUG: 3. Field has wrong data type")
                            println("DEBUG: 4. Mapping issue between Firestore and data class")
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Firestore fetch failed: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    println("DEBUG: No UID available for Firestore fetch")
                }
                
                // Supabase es la única fuente de verdad para la aprobación manual.
                val finalApproved = actualIsApproved == true
                println("DEBUG: Validation -> emailEmpty=${displayEmail.isNullOrEmpty()}, emailVerified=$isEmailVerified, googleLinked=$hasGoogleAccount, hasDriverData=$hasDriverData")
                println("DEBUG: Approval check -> profile.isApproved: ${profile?.isApproved}, remoteApproved: $remoteApproved, finalApproved: $finalApproved")

                when {
                    displayEmail.isNullOrEmpty() -> {
                        profileError = "Debes tener un correo electrónico para ser conductor"
                    }
                    !isEmailVerified -> {
                        profileError = "Debes verificar tu correo electrónico para ser conductor"
                    }
                    !hasGoogleAccount -> {
                        profileError = "Debes vincular tu cuenta de Google para ser conductor"
                    }
                    !hasDriverData -> {
                        onNavigateToDriverDataCollection?.invoke()
                    }
                    !finalApproved -> {
                        println("DEBUG: APPROVAL ERROR - email=${displayEmail}, verified=$isEmailVerified, google=$hasGoogleAccount, driverData=$hasDriverData, profileApproved=${profile?.isApproved}, remoteApproved=$remoteApproved, actualIsApproved=$actualIsApproved, finalApproved=$finalApproved")
                        
                        profileError = "Tu perfil de conductor está pendiente de aprobación manual. Te avisaremos cuando puedas conectarte."
                    }
                    else -> {
                        val uid2 = authUser?.uid
                        if (uid2 != null) {
                            try {
                                repo.setDriverMode(uid2, true)
                                onDriverChange(newDriverMode)
                                profile = profile?.copy(isDriver = true)
                            } catch (e: Exception) {
                                profileError = "Error al actualizar el perfil: ${e.message}"
                            }
                        } else {
                            onDriverChange(newDriverMode)
                        }
                    }
                }
            } else {
                onDriverChange(newDriverMode)
            }
        }
    }

    // Refresh driver profile status
    val refreshDriverProfile: () -> Unit = {
        val uid = authUser?.uid
        if (uid != null) {
            scope.launch {
                checkingDriverProfile = true
                try {
                    hasCompleteDriverProfile = repo.hasCompleteDriverProfile(uid)
                    println("DEBUG: Refreshed driver profile - hasCompleteDriverProfile: $hasCompleteDriverProfile")
                } catch (e: Exception) {
                    println("DEBUG: Error refreshing driver profile: ${e.message}")
                } finally {
                    checkingDriverProfile = false
                }
            }
        }
    }
    
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
                    val showDriverStats = isDriver || (hasCompleteDriverProfile && profile?.isDriver == true)
                    if (showDriverStats) {
                        DriverStatsSection()
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
                    
                    // Define email verification click handler
                    val onEmailVerificationClick: () -> Unit = {
                        if (authUser?.isEmailVerified == false) {
                            scope.launch {
                                try {
                                    val success = repo.sendEmailVerification()
                                    if (success) {
                                        emailVerificationSent = true
                                        statusMessage = "Correo de verificación enviado. Por favor revisa tu bandeja de entrada."
                                        // Clear status message after 5 seconds
                                        kotlinx.coroutines.delay(5000)
                                        statusMessage = null
                                    } else {
                                        statusMessage = "No se pudo enviar el correo de verificación."
                                        // Clear status message after 3 seconds
                                        kotlinx.coroutines.delay(3000)
                                        statusMessage = null
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "Error al enviar correo: ${e.message}"
                                    // Clear status message after 3 seconds
                                    kotlinx.coroutines.delay(3000)
                                    statusMessage = null
                                }
                            }
                        }
                    }
                    
                    SettingsSection(
                        authUser = authUser,
                        profile = profile,
                        googleLinked = googleLinked,
                        googleLinkingInProgress = googleLinkingInProgress,
                        onGoogleLinkClick = {
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
                        onEmailVerificationClick = onEmailVerificationClick,
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
                        paymentPreferences = paymentPreferences
                    )
                }
            }
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
    var profileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val pickImageLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> profileUri = uri }
    
    val profileBitmap: androidx.compose.ui.graphics.ImageBitmap? = remember(profileUri) {
        profileUri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            } catch (_: Exception) { null }
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
                containerColor = Color.White.copy(alpha = 0.2f)
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
                val showDriverToggle = isDriver || hasCompleteDriverProfile || (profile?.isDriver == true)
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
                                color = Color(0xFF08817E)
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
                            .clickable { pickImageLauncher.launch("image/*") }
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
                        profileBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } ?: run {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                        }
                        
                        // Edit overlay
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .align(Alignment.BottomEnd)
                                .border(2.dp, Color(0xFF08817E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF08817E)
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
                            val phoneLabel = profile?.number ?: authUser?.phoneNumber ?: "Sin número"
                            
                            Text(
                                displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Text(
                                when {
                                    isDriver -> "Conductor • $phoneLabel"
                                    profile?.isDriver == true -> "Conductor • $phoneLabel"
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
    val stats = listOf(
        Triple("Hoy", "$120.00", Icons.Default.TrendingUp),
        Triple("Semana", "$540.50", Icons.Default.CalendarToday),
        Triple("Rating", "4.8", Icons.Default.Star)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
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
                        tint = Color(0xFF08817E),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
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
    onEmailVerificationClick: () -> Unit,
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
    paymentPreferences: PaymentPreferences
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            // Email verification - prioritize Firestore profile email, fallback to Firebase Auth
            val displayEmail = profile?.email ?: authUser?.email
            println("DEBUG UI: Display email - profile.email: ${profile?.email}, authUser.email: ${authUser?.email}, final: $displayEmail")
            SettingsItemEnhanced(
                icon = Icons.Default.Email,
                title = "Correo electrónico",
                subtitle = displayEmail ?: "Sin correo",
                actionText = if (authUser?.isEmailVerified == true) "Verificado" else "Verificar",
                actionColor = if (authUser?.isEmailVerified == true) Color(0xFF10B981) else Color(0xFFF59E0B),
                onClick = onEmailVerificationClick
            )
            
            // Google account
            SettingsItemEnhanced(
                icon = Icons.Default.Link,
                title = "Cuenta de Google",
                subtitle = when {
                    googleLinkingInProgress -> "Vinculando..."
                    googleLinked -> "Vinculado"
                    else -> "No vinculado"
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
                    val shouldShowDriverOption = !isDriver && !hasCompleteDriverProfile && (profile?.isDriver != true)
                    if (shouldShowDriverOption) {
                        SettingsItemEnhanced(
                            icon = Icons.Default.DirectionsCar,
                            title = "Ganar como conductor",
                            subtitle = "Conduce y genera ingresos",
                            actionText = "Comenzar",
                            actionColor = Color(0xFF08817E),
                            onClick = {
                                // Check email verification first
                                val displayEmail = profile?.email ?: authUser?.email
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
            
            // Saved addresses
            SettingsItemEnhanced(
                icon = Icons.Default.Place,
                title = "Direcciones guardadas",
                subtitle = "Casa, trabajo y favoritas",
                actionText = "Editar",
                onClick = { /* Navigate to addresses */ }
            )
            
            // Trip history
            val tripHistoryTitle = when {
                isDriver -> "Historial de viajes (conductor)"
                profile?.isDriver == true -> "Historial de viajes (conductor)"
                else -> "Historial de viajes"
            }
            SettingsItemEnhanced(
                icon = Icons.Default.History,
                title = tripHistoryTitle,
                subtitle = "Ver todos tus viajes",
                actionText = "Ver",
                onClick = { /* Navigate to trip history */ }
            )
            
            // Language
            SettingsItemEnhanced(
                icon = Icons.Default.Language,
                title = "Idioma",
                subtitle = "Español",
                actionText = "Cambiar",
                onClick = { /* Navigate to language settings */ }
            )
            
            // Notifications
            SettingsItemEnhanced(
                icon = Icons.Default.Notifications,
                title = "Notificaciones",
                subtitle = "Configurar alertas",
                actionText = "Configurar",
                onClick = { /* Navigate to notifications */ }
            )
            
            // Support
            SettingsItemEnhanced(
                icon = Icons.Default.Support,
                title = "Soporte",
                subtitle = "Ayuda y contacto",
                actionText = "Contactar",
                onClick = { /* Navigate to support */ }
            )
            
            // Terms and privacy
            SettingsItemEnhanced(
                icon = Icons.Default.Description,
                title = "Términos y privacidad",
                subtitle = "Políticas de la app",
                actionText = "Abrir",
                onClick = { /* Navigate to terms */ }
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
                    color = Color(0xFF1C1C1E)
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
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
                        color = actionColor,
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
                else -> Color.White.copy(alpha = 0.8f)
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
                tint = if (checked && enabled) Color.White else if (!enabled) Color.Gray else Color(0xFF6B7280),
                modifier = Modifier.size(18.dp)
            )
            Text(
                if (checked) "Conductor" else "Pasajero",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (checked && enabled) Color.White else if (!enabled) Color.Gray else Color(0xFF6B7280)
            )
        }
    }
}
