package com.intu.taxi.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Home : NavItem("home", "Inicio", Icons.Filled.Home)
    data object Trips : NavItem("trips", "Viajes", Icons.Filled.List)
    data object Account : NavItem("account", "Cuenta", Icons.Filled.Person)
}