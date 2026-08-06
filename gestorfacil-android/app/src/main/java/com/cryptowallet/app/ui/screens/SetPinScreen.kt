@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.data.model.AccountInfo
import com.cryptowallet.app.ui.components.ErrorBanner
import com.cryptowallet.app.ui.components.PrimaryButton
import com.cryptowallet.app.ui.components.activityViewModel
import com.cryptowallet.app.ui.theme.TextSecondary
import com.cryptowallet.app.ui.viewmodel.OnboardingViewModel

@Composable
fun SetPinScreen(
    mode: String,
    onBack: () -> Unit,
    onDone: (AccountInfo) -> Unit
) {
    val vm = activityViewModel<OnboardingViewModel> { app -> OnboardingViewModel(app, app.walletRepository) }
    val creating by vm.creating.collectAsState()
    val error by vm.error.collectAsState()
    val restorePhrase by vm.restorePhrase.collectAsState()

    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val isRestore = mode == "restore"
    val circleColor = MaterialTheme.colorScheme.primary

    fun submit() {
        if (pin.length < 6) {
            localError = "El PIN debe tener al menos 6 dígitos"
            return
        }
        if (pin != confirm) {
            localError = "Los PIN no coinciden"
            return
        }
        localError = null
        if (isRestore) {
            vm.restoreWallet(restorePhrase ?: "", pin, onDone)
        } else {
            vm.createWallet(pin, onDone)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRestore) "Protege tu billetera" else "Crea tu PIN") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(56.dp)) {
                    drawCircle(color = circleColor)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Elige un PIN para proteger tu billetera.\n" +
                    "Lo necesitarás cada vez que abras la app.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8) },
                label = { Text("PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it.filter { c -> c.isDigit() }.take(8) },
                label = { Text("Confirmar PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(20.dp))
            if (localError != null) {
                ErrorBanner(localError ?: "")
                Spacer(Modifier.height(12.dp))
            }
            if (error != null) {
                ErrorBanner(error ?: "")
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PIN numérico de 6-8 dígitos",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "Crear billetera",
                    onClick = { submit() },
                    loading = creating,
                    enabled = pin.isNotEmpty() && confirm.isNotEmpty(),
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}
