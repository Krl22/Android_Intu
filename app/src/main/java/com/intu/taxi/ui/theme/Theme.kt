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
    primaryContainer = Color(0xFF214C48), onPrimaryContainer = Color(0xFFCDF7EF),
    secondary = Color(0xFFB3D7D1), onSecondary = Color(0xFF122D2D),
    secondaryContainer = Color(0xFF294341), onSecondaryContainer = Color(0xFFD7EBE7),
    tertiary = Color(0xFFE4C382), onTertiary = Color(0xFF392E14),
    background = Color(0xFF0F1A1E), onBackground = Color(0xFFE7F1EF),
    surface = Color(0xFF19282C), onSurface = Color(0xFFE7F1EF),
    surfaceVariant = Color(0xFF263D40), onSurfaceVariant = Color(0xFFAFC2C3),
    outline = Color(0xFF829B9D), outlineVariant = Color(0xFF3A5054),
    error = Color(0xFFFFB4A9), onError = Color(0xFF5E1710),
    errorContainer = Color(0xFF55261F), onErrorContainer = Color(0xFFFFDAD4),
    surfaceContainerLow = Color(0xFF152328), surfaceContainer = Color(0xFF19282C),
    surfaceContainerHigh = Color(0xFF23383C)
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = Pistachio,
    onPrimaryContainer = Color.Black,
    secondary = Mantis,
    onSecondary = Color.White,
    tertiary = Mindaro,
    onTertiary = Color.Black,
    background = Color(0xFFFDFDFE),
    onBackground = Color(0xFF121314),
    surface = IntuCream,
    onSurface = Color(0xFF121314),
    surfaceVariant = Color(0xFFE9EFE0),
    outline = Color(0xFFBFC7B6)
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