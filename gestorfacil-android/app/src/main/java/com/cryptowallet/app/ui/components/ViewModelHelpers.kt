package com.cryptowallet.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gestorfacil.app.GestorFacilApp

@Composable
inline fun <reified VM : ViewModel> walletViewModel(
    owner: ViewModelStoreOwner? = LocalViewModelStoreOwner.current,
    noinline create: (GestorFacilApp) -> VM
): VM {
    val app = LocalContext.current.applicationContext as GestorFacilApp
    val effectiveOwner = owner ?: checkNotNull(LocalViewModelStoreOwner.current) { "Sin ViewModelStoreOwner" }
    return viewModel(
        viewModelStoreOwner = effectiveOwner,
        factory = viewModelFactory { initializer { create(app) } }
    )
}

/**
 * ViewModel compartido a nivel de actividad (para el flujo de onboarding,
 * donde varias pantallas comparten la frase mnemotécnica).
 */
@Composable
inline fun <reified VM : ViewModel> activityViewModel(
    noinline create: (GestorFacilApp) -> VM
): VM {
    val app = LocalContext.current.applicationContext as GestorFacilApp
    val activityOwner = LocalContext.current as? ViewModelStoreOwner
    return walletViewModel(owner = activityOwner, create = create)
}
