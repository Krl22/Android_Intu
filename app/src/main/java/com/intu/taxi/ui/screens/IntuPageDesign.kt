package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intu.taxi.ui.theme.IntuTeal
import com.intu.taxi.ui.theme.IntuIndigo
import com.intu.taxi.ui.theme.LocalIntuDarkMode

/** Shared brand treatment for the three main passenger tabs, in either appearance. */
@Composable
internal fun Modifier.intuPageBackground(): Modifier {
    val dark = LocalIntuDarkMode.current
    val start = if (dark) lerp(IntuTeal, IntuIndigo, 0.25f) else lerp(IntuTeal, Color.White, 0.86f)
    val end = if (dark) IntuIndigo else lerp(IntuIndigo, Color.White, 0.93f)
    return background(Brush.linearGradient(listOf(start, end)))
}

/** Colored surfaces retain the original two-color identity instead of neutral grey. */
@Composable
internal fun Modifier.intuCardBackground(emphasized: Boolean = false): Modifier {
    val dark = LocalIntuDarkMode.current
    val start = if (dark) lerp(IntuTeal, IntuIndigo, if (emphasized) 0.20f else 0.42f)
        else lerp(IntuTeal, Color.White, if (emphasized) 0.86f else 0.95f)
    val end = if (dark) lerp(IntuIndigo, IntuTeal, 0.08f)
        else lerp(IntuIndigo, Color.White, if (emphasized) 0.90f else 0.96f)
    val shape = RoundedCornerShape(24.dp)
    return clip(shape).background(Brush.linearGradient(listOf(start, end)))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
}

@Composable
internal fun IntuPageHeading(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text("intu", color = colors.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), color = colors.onBackground,
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            action?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
