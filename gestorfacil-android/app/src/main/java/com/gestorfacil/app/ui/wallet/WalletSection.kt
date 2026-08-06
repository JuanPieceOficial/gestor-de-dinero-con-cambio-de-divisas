package com.gestorfacil.app.ui.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.cryptowallet.app.ui.navigation.AppNavigation
import com.cryptowallet.app.ui.navigation.Routes
import com.cryptowallet.app.ui.theme.Green
import com.cryptowallet.app.ui.theme.TextSecondary
import com.gestorfacil.app.GestorFacilApp
import kotlinx.coroutines.launch

/**
 * Sección "Cartera" dentro de GestorFácil: aloja el flujo completo de la wallet
 * (onboarding, bloqueo por PIN, cartera, enviar, recibir, actividad, ajustes).
 * Al pasar la app a segundo plano la wallet se bloquea automáticamente.
 */
@Composable
fun WalletSection() {
    val app = LocalContext.current.applicationContext as GestorFacilApp
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var ready by remember { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf(Routes.WELCOME) }

    LaunchedEffect(Unit) {
        startDestination = if (app.walletRepository.hasWallet()) Routes.LOCK else Routes.WELCOME
        ready = true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    scope.launch { app.walletRepository.lock() }
                }
                Lifecycle.Event.ON_START -> {
                    scope.launch {
                        if (app.walletRepository.hasWallet() && !app.walletRepository.isUnlocked()) {
                            try {
                                val currentRoute = navController.currentBackStackEntry?.destination?.route
                                if (currentRoute != Routes.LOCK && currentRoute != Routes.WELCOME) {
                                    navController.navigate(Routes.LOCK) {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            } catch (e: Exception) {
                                // NavController aún no listo
                            }
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Green)
                Spacer(Modifier.height(16.dp))
                Text("CryptoWallet", color = TextSecondary)
            }
        }
        return
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        AppNavigation(
            navController = navController,
            startDestination = startDestination,
            verifyPin = { pin -> app.walletRepository.unlock(pin) }
        )
    }
}
