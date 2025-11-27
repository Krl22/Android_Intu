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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3E9559),
    onPrimaryContainer = Color.White,
    secondary = Mantis,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3A7E4B),
    onSecondaryContainer = Color.White,
    tertiary = Mindaro,
    onTertiary = Color.Black,
    background = IntuGrey,
    onBackground = Color(0xFFEDEFF1),
    surface = Color(0xFF232527),
    onSurface = Color(0xFFEDEFF1),
    surfaceVariant = Color(0xFF2B2D2F),
    outline = Color(0xFF3D3F42)
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}