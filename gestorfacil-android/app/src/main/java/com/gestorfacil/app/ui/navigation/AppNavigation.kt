package com.gestorfacil.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    // La wallet es la pestaña principal: es el espacio donde se guarda el dinero.
    // Los movimientos (ingresos/gastos) son un registro aparte, no tocan el saldo.
    data object Wallet : Screen("wallet", "Cartera", Icons.Default.CurrencyBitcoin)
    data object Transactions : Screen("transactions", "Movimientos", Icons.Default.Receipt)
    data object Dolar : Screen("dolar", "Dólar", Icons.Default.AttachMoney)

    companion object {
        val tabs = listOf(Wallet, Transactions, Dolar)
    }
}
