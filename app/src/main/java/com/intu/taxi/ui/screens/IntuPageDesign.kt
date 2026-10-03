package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared brand treatment for the three main passenger tabs, in either appearance. */
@Composable
internal fun Modifier.intuPageBackground(): Modifier {
    val colors = MaterialTheme.colorScheme
    val glowHeight = with(LocalDensity.current) { 420.dp.toPx() }
    return background(Brush.verticalGradient(
        listOf(colors.primaryContainer, colors.background), endY = glowHeight))
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
