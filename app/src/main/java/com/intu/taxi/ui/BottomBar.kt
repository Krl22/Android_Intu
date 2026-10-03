package com.intu.taxi.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BottomBar(
    navController: NavController,
    items: List<NavItem>,
    visible: Boolean = true
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (visible) {
        val darkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val accent = if (darkSurface) Color(0xFF76D7CF) else Color(0xFF08817E)
        val selectedSurface = if (darkSurface) Color(0xFF244B48) else Color(0xFFE3F3EF)
        val muted = if (darkSurface) Color(0xFFB5C6C8) else Color(0xFF647479)
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                color = if (darkSurface) MaterialTheme.colorScheme.surface else Color.White,
                border = BorderStroke(1.dp, if (darkSurface) Color(0xFF3D5157) else Color(0xFFE8EFED)),
                shadowElevation = 10.dp) {
                Row(Modifier.selectableGroup().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEach { item ->
                        val selected = currentRoute == item.route
                        val background by animateColorAsState(if (selected) selectedSurface else Color.Transparent, label = "nav-background")
                        val foreground by animateColorAsState(if (selected) accent else muted, label = "nav-foreground")
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(background)
                            .selectable(selected = selected, role = Role.Tab, onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }).heightIn(min = 56.dp).padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                            val icon = when (item.route) {
                                NavItem.Home.route -> Icons.Outlined.Home
                                NavItem.Trips.route -> Icons.Outlined.Route
                                NavItem.Account.route -> Icons.Outlined.PersonOutline
                                else -> item.icon
                            }
                            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(22.dp))
                            Text(item.label, style = MaterialTheme.typography.labelMedium, color = foreground,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
