package com.intu.taxi.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.intu.taxi.BuildConfig
import com.intu.taxi.updates.AppUpdateState
import com.intu.taxi.updates.PublishedAppRelease

/** An automatic notice waits for a safe screen and is postponed once per version per session. */
@Composable
internal fun AppUpdateNotice(
    state: AppUpdateState,
    installedCode: Int,
    installedName: String,
    canPrompt: Boolean,
    tripActive: Boolean,
    requested: Boolean,
    onCheck: () -> Unit,
    onDownload: (PublishedAppRelease) -> Boolean,
    onRequestConsumed: () -> Unit,
) {
    var postponedCode by rememberSaveable { mutableIntStateOf(0) }
    val release = state.newerThan(installedCode)
    if (requested || (canPrompt && !tripActive && release != null && release.versionCode > postponedCode)) {
        AppUpdateDialog(state, installedCode, installedName, tripActive,
            onCheck = onCheck,
            onDownload = { selected ->
                if (!tripActive && onDownload(selected)) {
                    postponedCode = maxOf(postponedCode, selected.versionCode)
                    onRequestConsumed()
                }
            },
            onDismiss = {
                postponedCode = maxOf(postponedCode, release?.versionCode ?: 0)
                onRequestConsumed()
            })
    }
}

/** Keep a postponed release discoverable from Account. */
@Composable
internal fun AppUpdateSettingsItem(release: PublishedAppRelease?, onClick: () -> Unit) {
    SettingsItemEnhanced(
        icon = Icons.Outlined.SystemUpdate,
        title = "Actualizaciones",
        subtitle = if (release != null) "${release.title} disponible"
            else "Intu ${BuildConfig.VERSION_NAME} · Buscar una nueva versión",
        actionText = if (release != null) "Actualizar" else "Comprobar",
        onClick = onClick,
        modifier = Modifier.testTag("account-app-updates"),
    )
}
