package com.cryptowallet.app.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gestorfacil.app.GestorFacilApp
import com.cryptowallet.app.ui.screens.AddTokenScreen
import com.cryptowallet.app.ui.screens.BackupPhraseScreen
import com.cryptowallet.app.ui.screens.ChangePinScreen
import com.cryptowallet.app.ui.screens.CreateMnemonicScreen
import com.cryptowallet.app.ui.screens.LockScreen
import com.cryptowallet.app.ui.screens.MainScreen
import com.cryptowallet.app.ui.screens.ReceiveScreen
import com.cryptowallet.app.ui.screens.RestoreWalletScreen
import com.cryptowallet.app.ui.screens.SendScreen
import com.cryptowallet.app.ui.screens.SetPinScreen
import com.cryptowallet.app.ui.screens.VerifyMnemonicScreen
import com.cryptowallet.app.ui.screens.WelcomeScreen
import kotlinx.coroutines.launch

object Routes {
    const val WELCOME = "welcome"
    const val CREATE = "create"
    const val VERIFY = "verify"
    const val RESTORE = "restore"
    const val SETPIN = "setpin?mode={mode}"
    const val LOCK = "lock"
    const val MAIN = "main"
    const val SEND = "send?chainId={chainId}&tokenId={tokenId}"
    const val RECEIVE = "receive"
    const val ADD_TOKEN = "addtoken"
    const val CHANGE_PIN = "changepin"
    const val BACKUP = "backup"

    fun send(chainId: Long, tokenId: String): String =
        "send?chainId=$chainId&tokenId=${Uri.encode(tokenId)}"

    fun setPin(mode: String): String = "setpin?mode=$mode"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    verifyPin: suspend (String) -> Boolean
) {
    val app = LocalContext.current.applicationContext as GestorFacilApp

    fun goMain() {
        navController.navigate(Routes.MAIN) {
            popUpTo(Routes.WELCOME) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onCreate = { navController.navigate(Routes.CREATE) },
                onRestore = { navController.navigate(Routes.RESTORE) }
            )
        }
        composable(Routes.CREATE) {
            CreateMnemonicScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.VERIFY) }
            )
        }
        composable(Routes.VERIFY) {
            VerifyMnemonicScreen(
                onBack = { navController.popBackStack() },
                onVerified = { navController.navigate(Routes.setPin("create")) }
            )
        }
        composable(Routes.RESTORE) {
            RestoreWalletScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.setPin("restore")) }
            )
        }
        composable(
            route = Routes.SETPIN,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { entry ->
            val mode = entry.arguments?.getString("mode") ?: "create"
            SetPinScreen(
                mode = mode,
                onBack = { navController.popBackStack() },
                onDone = { goMain() }
            )
        }
        composable(Routes.LOCK) {
            val scope = rememberCoroutineScope()
            LockScreen(
                verifyPin = verifyPin,
                lockoutRemaining = { app.walletRepository.lockoutRemainingMs() },
                onUnlocked = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOCK) { inclusive = true }
                    }
                },
                onDeleteWallet = {
                    scope.launch {
                        app.walletRepository.deleteWallet()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.LOCK) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            val context = LocalContext.current
            val mainScope = rememberCoroutineScope()
            MainScreen(
                onSendToken = { chain, token ->
                    navController.navigate(Routes.send(chain.id, token.id))
                },
                onReceive = { navController.navigate(Routes.RECEIVE) },
                onAddToken = { navController.navigate(Routes.ADD_TOKEN) },
                onChangePin = { navController.navigate(Routes.CHANGE_PIN) },
                onBackupPhrase = { navController.navigate(Routes.BACKUP) },
                onLock = {
                    app.walletRepository.lock()
                    navController.navigate(Routes.LOCK) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onDeleteWallet = {
                    mainScope.launch {
                        app.walletRepository.deleteWallet()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    }
                },
                onShowMessage = { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
        composable(
            route = Routes.SEND,
            arguments = listOf(
                navArgument("chainId") { type = NavType.LongType },
                navArgument("tokenId") { type = NavType.StringType }
            )
        ) { entry ->
            val chainId = entry.arguments?.getLong("chainId") ?: 1L
            val tokenId = entry.arguments?.getString("tokenId") ?: ""
            SendScreen(
                chainId = chainId,
                tokenId = tokenId,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }
        composable(Routes.RECEIVE) {
            var address by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                address = app.walletRepository.getActiveAddress()
            }
            val value = address ?: ""
            ReceiveScreen(
                address = value,
                chainName = "EVM",
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ADD_TOKEN) {
            AddTokenScreen(
                onBack = { navController.popBackStack() },
                onAdded = { navController.popBackStack() }
            )
        }
        composable(Routes.CHANGE_PIN) {
            ChangePinScreen(
                onBack = { navController.popBackStack() },
                onChanged = { navController.popBackStack() }
            )
        }
        composable(Routes.BACKUP) {
            BackupPhraseScreen(onBack = { navController.popBackStack() })
        }
    }
}
