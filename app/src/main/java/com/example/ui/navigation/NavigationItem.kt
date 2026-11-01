package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainDestination(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Default.Home, "nav_home"),
    STOCK("Stock", Icons.Default.Inventory2, "nav_stock"),
    PROJECTS("Projects", Icons.Default.PrecisionManufacturing, "nav_projects"),
    CART("Cart", Icons.Default.ShoppingCart, "nav_cart"),
    ASK_AI("Ask AI", Icons.Default.AutoAwesome, "nav_ask_ai")
}

sealed class ScreenRoute {
    data object Main : ScreenRoute()
    data class PartDetail(val partId: Long) : ScreenRoute()
    data object AddPart : ScreenRoute()
    data object LocationManager : ScreenRoute()
    data class ProjectDetail(val projectId: Long) : ScreenRoute()
}
