package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.data.api.Chains
import com.cryptowallet.app.data.db.TokenEntity
import com.cryptowallet.app.data.model.Chain
import com.cryptowallet.app.ui.components.showBiometricPrompt
import com.cryptowallet.app.ui.components.walletViewModel
import com.cryptowallet.app.ui.components.copyToClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.cryptowallet.app.ui.viewmodel.WalletViewModel
import androidx.compose.foundation.layout.Box

private data class MainTab(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(
    onSendToken: (Chain, TokenEntity) -> Unit,
    onReceive: () -> Unit,
    onAddToken: () -> Unit,
    onChangePin: () -> Unit,
    onBackupPhrase: () -> Unit,
    onLock: () -> Unit,
    onDeleteWallet: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val vm = walletViewModel<WalletViewModel> { app -> WalletViewModel(app, app.walletRepository) }
    val portfolio by vm.portfolio.collectAsState()
    val account by vm.account.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val history by vm.history.collectAsState()
    val testnetEnabled by vm.testnetEnabled.collectAsState()
    val fiatCurrency by vm.fiatCurrency.collectAsState()
    val biometricEnabled by vm.biometricEnabled.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.loadPreferences()
        vm.loadAccount()
        vm.refresh()
        vm.loadHistory()
    }

    fun toggleBiometric(enable: Boolean) {
        if (enable) {
            vm.enableBiometric { cipher ->
                val activity = context as? androidx.fragment.app.FragmentActivity
                if (activity == null || cipher == null) {
                    onShowMessage("Biometría no disponible en este dispositivo")
                    return@enableBiometric
                }
                showBiometricPrompt(
                    activity = activity,
                    title = "Activar desbloqueo con huella",
                    subtitle = "Confirma tu identidad para guardar el acceso biométrico",
                    cipher = cipher,
                    onSuccess = {
                        vm.completeEnableBiometric(cipher) { ok ->
                            onShowMessage(if (ok) "Huella activada" else "No se pudo activar la huella")
                        }
                    }
                )
            }
        } else {
            vm.disableBiometric()
            onShowMessage("Desbloqueo con huella desactivado")
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(MainTab("Wallet", Icons.Default.AccountBalanceWallet), MainTab("Actividad", Icons.Default.History), MainTab("Ajustes", Icons.Default.Settings))

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> PortfolioScreen(
                    portfolio = portfolio,
                    account = account,
                    onRefresh = { vm.refresh(showSpinner = true) },
                    onSendToken = onSendToken,
                    onReceive = onReceive,
                    onAddToken = onAddToken,
                    onRemoveToken = { vm.removeToken(it) }
                )
                1 -> ActivityScreen(history = history, onRefresh = { vm.loadHistory() })
                2 -> SettingsScreen(
                    account = account,
                    accounts = accounts,
                    chains = if (testnetEnabled) Chains.testnets else Chains.all,
                    testnetEnabled = testnetEnabled,
                    fiatCurrency = fiatCurrency,
                    biometricEnabled = biometricEnabled,
                    biometricAvailable = vm.isBiometricAvailable(),
                    onToggleBiometric = { toggleBiometric(it) },
                    onAddAccount = { vm.addAccount() },
                    onSwitchAccount = { vm.switchAccount(it) },
                    onRenameAccount = { idx, name -> vm.renameAccount(idx, name) },
                    onToggleTestnet = { vm.setTestnetEnabled(it) },
                    onSelectCurrency = { vm.setFiatCurrency(it) },
                    onCopyAddress = {
                        copyToClipboard(clipboard, it)
                        onShowMessage("Dirección copiada")
                    },
                    onChangePin = onChangePin,
                    onBackupPhrase = onBackupPhrase,
                    onLock = onLock,
                    onDeleteWallet = onDeleteWallet
                )
            }
        }
    }
}
