package com.tesis.potatodiseaseai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.History
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Scanner : Screen("scanner", "Escanear", Icons.Outlined.Camera)
    data object History : Screen("history", "Historial", Icons.Outlined.History)
    data object Help : Screen("help", "Ayuda", Icons.Outlined.Help)
    data object Result : Screen("result", "Resultado", Icons.Outlined.Help)

    companion object {
        val bottomTabs = listOf(Scanner, History, Help)
    }
}