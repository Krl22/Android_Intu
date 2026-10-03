package com.intu.taxi.ui.screens

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.intu.taxi.auth.AuthRepository
import com.intu.taxi.auth.PhoneFormatter
import com.intu.taxi.auth.authErrorMessage
import com.intu.taxi.auth.countryCodes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PhoneAuthScreen(
    activity: Activity,
    repo: AuthRepository,
    onVerified: suspend (PhoneAuthCredential) -> Unit,
    onCancel: () -> Unit,
    linking: Boolean = false,
    initialPhone: String = ""
) {
    PhoneAuthForm(initialPhone, linking, onVerified, onCancel, startVerification = { phone, token, callbacks ->
        repo.startPhoneVerification(activity, phone, callbacks = callbacks, forceResendingToken = token)
    })
}

/** Callbacks are replaceable in UI tests; production always uses Firebase above. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthForm(
    initialPhone: String,
    linking: Boolean,
    onVerified: suspend (PhoneAuthCredential) -> Unit,
    onCancel: () -> Unit,
    startVerification: (String, PhoneAuthProvider.ForceResendingToken?, PhoneAuthProvider.OnVerificationStateChangedCallbacks) -> Unit,
    resendDelayMillis: Long = 60_000L
) {
    // Matches the regions currently enabled in Firebase's SMS policy.
    val countries = remember { countryCodes.filter { it.iso2 in setOf("PE", "US") } }
    val initialCountry = countries.firstOrNull { initialPhone.startsWith(it.prefijo) } ?: countries.first()
    var prefix by rememberSaveable { mutableStateOf(initialCountry.prefijo) }
    var number by rememberSaveable { mutableStateOf(PhoneFormatter.nationalDigits(initialCountry.prefijo, initialPhone)) }
    var verificationId by rememberSaveable { mutableStateOf<String?>(null) }
    var sentPhone by rememberSaveable { mutableStateOf("") }
    var resendToken by rememberSaveable { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var resendAt by rememberSaveable { mutableLongStateOf(0L) }
    var sending by rememberSaveable { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var countryMenu by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var active by remember { mutableStateOf(true) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val verifiedCallback by rememberUpdatedState(onVerified)
    val starter by rememberUpdatedState(startVerification)
    BackHandler(enabled = checking) { /* Finish the credential operation before leaving. */ }

    DisposableEffect(Unit) { onDispose { active = false } }
    LaunchedEffect(resendAt) {
        now = SystemClock.elapsedRealtime()
        while (now < resendAt) { delay(250); now = SystemClock.elapsedRealtime() }
    }
    val resendSeconds = ((resendAt - now).coerceAtLeast(0) + 999) / 1000

    fun fail(error: Throwable) {
        if (!active) return
        sending = false
        message = authErrorMessage(error, linking)
    }
    fun complete(credential: PhoneAuthCredential) {
        if (!active || checking) return
        checking = true
        sending = false
        message = null
        focus.clearFocus()
        scope.launch {
            try { verifiedCallback(credential) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { fail(e) }
            finally { checking = false }
        }
    }
    fun send(resend: Boolean = false, reattach: Boolean = false) {
        if (checking || (sending && !reattach) || (resend && SystemClock.elapsedRealtime() < resendAt)) return
        val phone = if (resend) sentPhone else PhoneFormatter.formatE164(prefix, number)
        if (PhoneFormatter.normalizeMobile(phone) == null || !PhoneFormatter.isValid(prefix, number)) {
            message = if (prefix == "+51") "Ingresa un celular de Perú: 9 dígitos y comienza con 9." else "Ingresa un número de 10 dígitos."
            sending = false
            return
        }
        focus.clearFocus()
        sending = true
        message = null
        sentPhone = phone
        otp = ""
        val request = ++attempt
        try {
            starter(phone, if (resend) resendToken else null, object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    if (active && attempt == request) complete(credential)
                }
                override fun onVerificationFailed(e: FirebaseException) { if (active && attempt == request) fail(e) }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    if (!active || attempt != request) return
                    verificationId = id
                    resendToken = token
                    sending = false
                    // Restart for every SMS, even if Firebase reuses the verification ID.
                    resendAt = SystemClock.elapsedRealtime() + resendDelayMillis
                }
                override fun onCodeAutoRetrievalTimeOut(id: String) {
                    if (!active || attempt != request) return
                    sending = false
                    verificationId = id
                }
            })
        } catch (e: Exception) { fail(e) }
    }

    // Reattach activity-scoped callbacks after recreation. Firebase verification is reentrant.
    LaunchedEffect(Unit) { if (sending && sentPhone.isNotEmpty()) send(reattach = true) }
    LaunchedEffect(sending, attempt) {
        if (sending) { delay(125_000); if (sending) fail(IllegalStateException("No recibimos respuesta. Revisa tu conexión y vuelve a solicitar el código.")) }
    }

    MaterialTheme(colorScheme = if (com.intu.taxi.ui.theme.LocalIntuDarkMode.current) MaterialTheme.colorScheme else lightColorScheme(primary = Color(0xFF08817E), onPrimary = Color.White,
        surface = Color.White, onSurface = Color(0xFF1E1F47), onSurfaceVariant = Color(0xFF53696D))) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1E1F47), Color(0xFF08817E))))) {
            Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Surface(Modifier.widthIn(max = 480.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally))
                        Text(if (linking) "Vincula tu número" else "Ingresa con tu número", style = MaterialTheme.typography.headlineSmall)
                        Text(if (linking) "Verifica tu celular por SMS para entrar a esta misma cuenta con Google o teléfono."
                            else "Te enviaremos un código de 6 dígitos por SMS.", style = MaterialTheme.typography.bodyMedium)
                        if (verificationId == null) {
                            ExposedDropdownMenuBox(countryMenu, { if (!sending && !checking) countryMenu = it }) {
                                OutlinedTextField(value = countries.first { it.prefijo == prefix }.let { "${it.nombre} (${it.prefijo})" }, onValueChange = {},
                                    label = { Text("País") }, readOnly = true, enabled = !sending && !checking,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(countryMenu) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, !sending && !checking).fillMaxWidth())
                                ExposedDropdownMenu(countryMenu, { countryMenu = false }) {
                                    countries.forEach { country -> DropdownMenuItem(text = { Text("${country.nombre} (${country.prefijo})") },
                                        onClick = { prefix = country.prefijo; number = ""; message = null; countryMenu = false }) }
                                }
                            }
                            OutlinedTextField(number, {
                                number = PhoneFormatter.nationalDigits(prefix, it).take(15); message = null
                            }, label = { Text("Número de celular") }, placeholder = { Text(if (prefix == "+51") "987 654 321" else "857 123 4567") },
                                supportingText = { Text(if (prefix == "+51") "9 dígitos, comienza con 9. Ya incluimos +51." else "10 dígitos. Ya incluimos +1.") },
                                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), enabled = !sending && !checking,
                                modifier = Modifier.fillMaxWidth().testTag("phone-number"))
                        } else {
                            Text("Código enviado a $sentPhone", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(otp, { otp = PhoneFormatter.sanitizeDigits(it).take(6); message = null }, label = { Text("Código de 6 dígitos") },
                                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !checking && !sending,
                                modifier = Modifier.fillMaxWidth().testTag("phone-otp"))
                        }
                        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("phone-auth-error")) }
                        Button(onClick = {
                            if (verificationId == null) send() else complete(PhoneAuthProvider.getCredential(verificationId!!, otp))
                        }, enabled = !sending && !checking && if (verificationId == null) PhoneFormatter.isValid(prefix, number) else otp.length == 6,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(12.dp)) {
                            if (sending || checking) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(when { checking -> "Verificando…"; sending -> "Enviando…"; verificationId == null -> "Enviar código"; else -> "Verificar código" })
                        }
                        if (verificationId != null) {
                            OutlinedButton(onClick = { send(true) }, enabled = !sending && !checking && resendSeconds == 0L,
                                modifier = Modifier.fillMaxWidth().testTag("phone-resend")) {
                                Text(if (resendSeconds > 0) "Reenviar en ${resendSeconds}s" else "Reenviar código")
                            }
                            TextButton(onClick = { attempt++; verificationId = null; resendToken = null; otp = ""; message = null; sending = false },
                                enabled = !checking && !sending, modifier = Modifier.fillMaxWidth()) { Text("Cambiar número") }
                        }
                        TextButton(onClick = { attempt++; sending = false; onCancel() }, enabled = !checking, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
                    }
                }
            }
        }
    }
}
