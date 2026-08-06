@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.ui.components.ErrorBanner
import com.cryptowallet.app.ui.components.PrimaryButton
import com.cryptowallet.app.ui.components.walletViewModel
import com.cryptowallet.app.ui.theme.TextSecondary
import com.cryptowallet.app.ui.viewmodel.WalletViewModel

@Composable
fun ChangePinScreen(
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    val vm = walletViewModel<WalletViewModel> { app -> WalletViewModel(app, app.walletRepository) }

    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    fun submit() {
        if (newPin.length < 6) {
            localError = "El PIN debe tener al menos 6 dígitos"
            return
        }
        if (newPin != confirmPin) {
            localError = "Los PIN no coinciden"
            return
        }
        localError = null
        checking = true
        vm.changePin(oldPin, newPin) { ok ->
            checking = false
            if (ok) {
                onChanged()
            } else {
                localError = "El PIN actual no es correcto"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cambiar PIN") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Text("Para tu seguridad necesitamos tu PIN actual.", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            PinField("PIN actual", oldPin, { oldPin = it })
            Spacer(Modifier.height(12.dp))
            PinField("Nuevo PIN", newPin, { newPin = it })
            Spacer(Modifier.height(12.dp))
            PinField("Confirmar nuevo PIN", confirmPin, { confirmPin = it })
            Spacer(Modifier.height(20.dp))
            if (localError != null) {
                ErrorBanner(localError ?: "")
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = "Guardar",
                loading = checking,
                enabled = oldPin.isNotEmpty() && newPin.isNotEmpty() && confirmPin.isNotEmpty(),
                onClick = { submit() }
            )
        }
    }
}

@Composable
private fun PinField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(8)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
