package com.intu.taxi.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF78D9D0), onPrimary = Color(0xFF063A36),
    primaryContainer = Color(0xFF125B63), onPrimaryContainer = Color(0xFFD6F5F0),
    secondary = Color(0xFFB6C4EE), onSecondary = IntuIndigo,
    secondaryContainer = Color(0xFF303660), onSecondaryContainer = Color(0xFFE1E6FF),
    tertiary = Color(0xFFE4C382), onTertiary = Color(0xFF392E14),
    background = Color(0xFF10132C), onBackground = Color(0xFFF0F7FA),
    surface = IntuIndigo, onSurface = Color(0xFFF0F7FA),
    surfaceVariant = Color(0xFF28375C), onSurfaceVariant = Color(0xFFD6E9EF),
    outline = Color(0xFF91BFC8), outlineVariant = Color(0xFF487B89),
    error = Color(0xFFFFB4A9), onError = Color(0xFF5E1710),
    errorContainer = Color(0xFF55261F), onErrorContainer = Color(0xFFFFDAD4),
    surfaceContainerLowest = Color(0xFF0C1025), surfaceContainerLow = Color(0xFF171D3C),
    surfaceContainer = IntuIndigo, surfaceContainerHigh = Color(0xFF28375C),
    surfaceContainerHighest = Color(0xFF30466C)
)

private val LightColorScheme = lightColorScheme(
    primary = IntuTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDAF3EE),
    onPrimaryContainer = Color(0xFF063A36),
    secondary = IntuIndigo,
    onSecondary = Color.White,
    tertiary = Mindaro,
    onTertiary = Color.Black,
    background = Color(0xFFFDFDFE),
    onBackground = Color(0xFF121314),
    surface = Color(0xFFF7FCFB),
    onSurface = Color(0xFF121314),
    surfaceVariant = Color(0xFFE4EFEE), onSurfaceVariant = Color(0xFF425C65),
    outline = Color(0xFF7D9CA3), outlineVariant = Color(0xFFBDD5D9),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF5FAFA),
    surfaceContainer = Color(0xFFEDF5F5), surfaceContainerHigh = Color(0xFFE6EEF4),
    surfaceContainerHighest = Color(0xFFDDE8F0)
)

@Composable
fun IntuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Usamos colores de marca, sin colores dinámicos
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIntuDarkMode provides darkTheme, androidx.compose.material3.LocalContentColor provides colorScheme.onBackground) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
