package com.intu.taxi.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enhanced design system for Intu Taxi app
 * Provides consistent colors, typography, and shapes across all screens
 */

// Enhanced Color Palette
object IntuColors {
    // Primary colors - Modern gradient palette
    val PrimaryGradientStart = Color(0xFF667eea)
    val PrimaryGradientEnd = Color(0xFF764ba2)
    val PrimaryLight = Color(0xFF8B5CF6)
    val PrimaryDark = Color(0xFF5B21B6)
    
    // Secondary colors
    val Secondary = Color(0xFFf093fb)
    val SecondaryLight = Color(0xFFfbbf24)
    val SecondaryDark = Color(0xFFf59e0b)
    
    // Status colors
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)
    
    // Neutral colors
    val Background = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F5F9)
    val OnSurface = Color(0xFF1C1C1E)
    val OnSurfaceVariant = Color(0xFF6B7280)
    val Outline = Color(0xFFE5E7EB)
    
    // Glassmorphism colors
    val GlassSurface = Color.White.copy(alpha = 0.2f)
    val GlassBorder = Color.White.copy(alpha = 0.3f)
    val GlassShadow = Color.Black.copy(alpha = 0.2f)
    
    // Text colors
    val TextPrimary = Color(0xFF1C1C1E)
    val TextSecondary = Color(0xFF6B7280)
    val TextTertiary = Color(0xFF9CA3AF)
    
    // Chart colors
    val ChartPrimary = Color(0xFF4CA1AF)
    val ChartSecondary = Color(0xFF2C3E50)
    val ChartAccent = Color(0xFF667eea)
}

// Enhanced Typography
object IntuTypography {
    // Display styles
    val DisplayLarge = Typography().displayLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )
    val DisplayMedium = Typography().displayMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    )
    
    // Headline styles
    val HeadlineLarge = Typography().headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    )
    val HeadlineMedium = Typography().headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.15).sp
    )
    val HeadlineSmall = Typography().headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp
    )
    
    // Title styles
    val TitleLarge = Typography().titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp
    )
    val TitleMedium = Typography().titleMedium.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.05).sp
    )
    val TitleSmall = Typography().titleSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    )
    
    // Body styles
    val BodyLarge = Typography().bodyLarge.copy(
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    )
    val BodyMedium = Typography().bodyMedium.copy(
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.15.sp,
        lineHeight = 20.sp
    )
    val BodySmall = Typography().bodySmall.copy(
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp,
        lineHeight = 16.sp
    )
    
    // Label styles
    val LabelLarge = Typography().labelLarge.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
    val LabelMedium = Typography().labelMedium.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
    val LabelSmall = Typography().labelSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
}

// Enhanced Shapes
object IntuShapes {
    val ExtraSmall = RoundedCornerShape(4.dp)
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp)
    val Large = RoundedCornerShape(16.dp)
    val ExtraLarge = RoundedCornerShape(20.dp)
    val Full = CircleShape
    
    // Custom shapes
    val Card = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(12.dp)
    val InputField = RoundedCornerShape(12.dp)
    val Avatar = CircleShape
    val Chip = RoundedCornerShape(20.dp)
    val BottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

// Enhanced Elevation
object IntuElevation {
    val Level0 = 0.dp
    val Level1 = 2.dp
    val Level2 = 4.dp
    val Level3 = 8.dp
    val Level4 = 12.dp
    val Level5 = 16.dp
    val Level6 = 24.dp
    
    // Glassmorphism elevation
    val Glass = 20.dp
}

// Enhanced Spacing
object IntuSpacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
    val Huge = 32.dp
    val Massive = 48.dp
}

// Enhanced Theme
@Composable
fun IntuTaxiTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = IntuColors.PrimaryGradientStart,
            onPrimary = Color.White,
            primaryContainer = IntuColors.PrimaryLight,
            onPrimaryContainer = Color.White,
            secondary = IntuColors.Secondary,
            onSecondary = Color.White,
            secondaryContainer = IntuColors.SecondaryLight,
            onSecondaryContainer = Color.White,
            tertiary = IntuColors.Info,
            onTertiary = Color.White,
            tertiaryContainer = IntuColors.Info.copy(alpha = 0.1f),
            onTertiaryContainer = IntuColors.Info,
            background = Color(0xFF0F172A),
            onBackground = Color.White,
            surface = Color(0xFF1E293B),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF334155),
            onSurfaceVariant = Color(0xFFCBD5E1),
            surfaceTint = IntuColors.PrimaryGradientStart,
            inverseSurface = Color(0xFFF8FAFC),
            inverseOnSurface = Color(0xFF1C1C1E),
            error = IntuColors.Error,
            onError = Color.White,
            errorContainer = IntuColors.Error.copy(alpha = 0.1f),
            onErrorContainer = IntuColors.Error,
            outline = Color(0xFF475569),
            outlineVariant = Color(0xFF64748B),
            scrim = Color.Black.copy(alpha = 0.5f)
        )
    } else {
        lightColorScheme(
            primary = IntuColors.PrimaryGradientStart,
            onPrimary = Color.White,
            primaryContainer = IntuColors.PrimaryLight,
            onPrimaryContainer = Color.White,
            secondary = IntuColors.Secondary,
            onSecondary = Color.White,
            secondaryContainer = IntuColors.SecondaryLight,
            onSecondaryContainer = Color.White,
            tertiary = IntuColors.Info,
            onTertiary = Color.White,
            tertiaryContainer = IntuColors.Info.copy(alpha = 0.1f),
            onTertiaryContainer = IntuColors.Info,
            background = IntuColors.Background,
            onBackground = IntuColors.TextPrimary,
            surface = IntuColors.Surface,
            onSurface = IntuColors.TextPrimary,
            surfaceVariant = IntuColors.SurfaceVariant,
            onSurfaceVariant = IntuColors.TextSecondary,
            surfaceTint = IntuColors.PrimaryGradientStart,
            inverseSurface = Color(0xFF1C1C1E),
            inverseOnSurface = Color.White,
            error = IntuColors.Error,
            onError = Color.White,
            errorContainer = IntuColors.Error.copy(alpha = 0.1f),
            onErrorContainer = IntuColors.Error,
            outline = IntuColors.Outline,
            outlineVariant = IntuColors.Outline.copy(alpha = 0.5f),
            scrim = Color.Black.copy(alpha = 0.5f)
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = IntuTypography.DisplayLarge,
            displayMedium = IntuTypography.DisplayMedium,
            headlineLarge = IntuTypography.HeadlineLarge,
            headlineMedium = IntuTypography.HeadlineMedium,
            headlineSmall = IntuTypography.HeadlineSmall,
            titleLarge = IntuTypography.TitleLarge,
            titleMedium = IntuTypography.TitleMedium,
            titleSmall = IntuTypography.TitleSmall,
            bodyLarge = IntuTypography.BodyLarge,
            bodyMedium = IntuTypography.BodyMedium,
            bodySmall = IntuTypography.BodySmall,
            labelLarge = IntuTypography.LabelLarge,
            labelMedium = IntuTypography.LabelMedium,
            labelSmall = IntuTypography.LabelSmall
        ),
        shapes = Shapes(
            extraSmall = IntuShapes.ExtraSmall,
            small = IntuShapes.Small,
            medium = IntuShapes.Medium,
            large = IntuShapes.Large,
            extraLarge = IntuShapes.ExtraLarge
        ),
        content = content
    )
}

// Enhanced Gradient Composables
@Composable
fun IntuGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        IntuColors.PrimaryGradientStart.copy(alpha = 0.1f),
                        IntuColors.PrimaryGradientEnd.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun IntuGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = IntuElevation.Glass,
                shape = IntuShapes.Card,
                ambientColor = IntuColors.GlassShadow
            ),
        colors = CardDefaults.cardColors(
            containerColor = IntuColors.GlassSurface
        ),
        border = BorderStroke(1.dp, IntuColors.GlassBorder),
        shape = IntuShapes.Card,
        elevation = CardDefaults.cardElevation(
            defaultElevation = IntuElevation.Level3
        )
    ) {
        content()
    }
}

@Composable
fun IntuPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = IntuColors.PrimaryGradientStart,
            contentColor = Color.White,
            disabledContainerColor = IntuColors.PrimaryGradientStart.copy(alpha = 0.3f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        ),
        shape = IntuShapes.Button,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = IntuElevation.Level2,
            pressedElevation = IntuElevation.Level1,
            disabledElevation = IntuElevation.Level0
        )
    ) {
        content()
    }
}

@Composable
fun IntuSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = IntuColors.PrimaryGradientStart,
            disabledContentColor = IntuColors.PrimaryGradientStart.copy(alpha = 0.3f)
        ),
        shape = IntuShapes.Button,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = SolidColor(IntuColors.PrimaryGradientStart)
        )
    ) {
        content()
    }
}