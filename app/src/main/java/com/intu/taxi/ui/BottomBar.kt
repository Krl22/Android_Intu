package com.intu.taxi.ui

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import com.intu.taxi.ui.theme.OxfordBlue
import com.intu.taxi.ui.theme.YinmnBlue
import com.intu.taxi.ui.theme.Verdigris
import androidx.compose.material3.NavigationBarItemDefaults
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
        NavigationBar {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = Verdigris
                    )
                )
            }
        }
    }
}