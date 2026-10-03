package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal val AccountTeal = Color(0xFF087F80)
private val AccountIndigo = Color(0xFF25254D)
private val AccountDialogColors = lightColorScheme(
    primary = AccountTeal, onPrimary = Color.White,
    background = Color(0xFFF3F7F8), surface = Color.White,
    onSurface = AccountIndigo, onBackground = AccountIndigo,
    onSurfaceVariant = Color(0xFF586772), outline = Color(0xFF84979C),
    outlineVariant = Color(0xFFDDE7E9)
)

/** Shared chrome for the two reading/form dialogs in Account. */
@Composable
internal fun AccountDialogLayout(
    eyebrow: String,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    dismissEnabled: Boolean = true,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = { if (dismissEnabled) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        MaterialTheme(colorScheme = if (com.intu.taxi.ui.theme.LocalIntuDarkMode.current) MaterialTheme.colorScheme else AccountDialogColors) {
            BoxWithConstraints(Modifier.fillMaxWidth().systemBarsPadding().imePadding().padding(16.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight * 0.94f),
                    shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.background) {
                    Column {
                        Row(Modifier.fillMaxWidth()
                            .background(Brush.linearGradient(listOf(AccountTeal, AccountIndigo)))
                            .padding(start = 22.dp, top = 18.dp, end = 8.dp, bottom = 20.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(eyebrow, color = Color.White.copy(alpha = .78f),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold)
                                Text(subtitle, color = Color.White.copy(alpha = .9f),
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = onDismiss, enabled = dismissEnabled) {
                                Icon(Icons.Outlined.Close, contentDescription = "Cerrar $title",
                                    tint = Color.White.copy(alpha = if (dismissEnabled) 1f else .4f))
                            }
                        }
                        Column(Modifier.weight(1f, fill = false), content = content)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(Modifier.fillMaxWidth().background(AppearanceColors.surface)
                            .padding(horizontal = 20.dp, vertical = 14.dp), content = footer)
                    }
                }
            }
        }
    }
}
