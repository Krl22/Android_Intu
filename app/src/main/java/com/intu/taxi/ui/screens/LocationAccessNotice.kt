package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.intu.taxi.permissions.AppPermissions

@Composable
internal fun LocationAccessNotice(permissions: AppPermissions, modifier: Modifier = Modifier) {
    if (permissions.requestInProgress || permissions.locationGranted && permissions.locationEnabled) return
    Surface(modifier.fillMaxWidth().testTag("location-access-notice"), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp)) {
            Text(if (!permissions.locationGranted) "Activa tu ubicación para encontrar el punto de recojo."
                else "Enciende la ubicación del dispositivo para usar el GPS.",
                style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = {
                if (!permissions.locationGranted) permissions.requestLocation {} else permissions.enableLocation()
            }, modifier = Modifier.testTag("location-access-action")) {
                Text(if (!permissions.locationGranted) "Activar ubicación" else "Activar GPS")
            }
        }
    }
}
