package com.intu.taxi.ui.theme

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.maps.Style

val LocalIntuDarkMode = staticCompositionLocalOf { false }
data class AppearanceController(val darkMode: Boolean = false, val managed: Boolean = false, val setDarkMode: (Boolean) -> Unit = {})
val LocalAppearanceController = staticCompositionLocalOf { AppearanceController() }

/** Device-local appearance, isolated by signed-in UID; signed-out screens default to light. */
class AppearancePreferences(context: Context, name: String = "intu_appearance") {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    fun isDark(uid: String?): Boolean = !uid.isNullOrBlank() && preferences.getBoolean("dark_$uid", false)
    fun setDark(uid: String?, enabled: Boolean) {
        require(!uid.isNullOrBlank()) { "Inicia sesión para cambiar la apariencia." }
        preferences.edit().putBoolean("dark_$uid", enabled).apply()
    }
}

@Composable
fun IntuAppearanceHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { AppearancePreferences(context) }
    val auth = remember { FirebaseAuth.getInstance() }
    var uid by remember { mutableStateOf(auth.currentUser?.uid) }
    var dark by remember(uid) { mutableStateOf(preferences.isDark(uid)) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { uid = it.currentUser?.uid }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    val controller = AppearanceController(dark, managed = true) { enabled ->
        if (uid != null && uid == auth.currentUser?.uid) {
            preferences.setDark(uid, enabled)
            dark = enabled
        }
    }
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        (view.context as? android.app.Activity)?.window?.let { window ->
            val bars = androidx.core.view.WindowCompat.getInsetsController(window, view)
            bars.isAppearanceLightStatusBars = uid != null && !dark
            bars.isAppearanceLightNavigationBars = uid != null && !dark
        }
    }
    CompositionLocalProvider(LocalAppearanceController provides controller) {
        IntuTheme(darkTheme = dark, content = content)
    }
}

@Composable
fun intuMapStyle(): String = if (LocalIntuDarkMode.current) Style.DARK else Style.MAPBOX_STREETS

object AppearanceColors {
    val surface: Color @Composable get() = if (LocalIntuDarkMode.current) Color(0xFF19282C) else Color.White
    val ink: Color @Composable get() = if (LocalIntuDarkMode.current) Color(0xFFE7F1EF) else Color(0xFF1E1F47)
    val muted: Color @Composable get() = if (LocalIntuDarkMode.current) Color(0xFFAFC2C3) else Color(0xFF5F6570)
    val accent: Color @Composable get() = if (LocalIntuDarkMode.current) Color(0xFF78D9D0) else Color(0xFF08817E)
    val outline: Color @Composable get() = if (LocalIntuDarkMode.current) Color(0xFF3A5054) else Color(0xFFDCE8E7)
    @Composable fun foreground(light: Color): Color = if (LocalIntuDarkMode.current) ink else light
    @Composable fun secondary(light: Color): Color = if (LocalIntuDarkMode.current) muted else light
    @Composable fun highlight(light: Color): Color = if (!LocalIntuDarkMode.current) light else when (light.toArgb()) {
        0xFFB42318.toInt(), 0xFFDC2626.toInt() -> Color(0xFFFFB4A9)
        0xFFB45309.toInt(), 0xFF99641C.toInt() -> Color(0xFFFFD291)
        0xFF067647.toInt(), 0xFF16A34A.toInt() -> Color(0xFF85DCA4)
        else -> accent
    }
    @Composable fun tint(light: Color): Color = if (LocalIntuDarkMode.current) Color(0xFF263D40) else light
}
