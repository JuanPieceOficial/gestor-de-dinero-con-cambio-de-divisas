package com.gestorfacil.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : Screen("home", "Inicio", Icons.Default.AccountBalanceWallet)
    data object Transactions : Screen("transactions", "Movimientos", Icons.Default.Receipt)
    data object Dolar : Screen("dolar", "Dólar", Icons.Default.AttachMoney)

    companion object {
        val tabs = listOf(Home, Transactions, Dolar)
    }
}
