@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.ui.components.ErrorBanner
import com.cryptowallet.app.ui.components.PrimaryButton
import com.cryptowallet.app.ui.components.activityViewModel
import com.cryptowallet.app.ui.theme.TextSecondary
import com.cryptowallet.app.ui.viewmodel.OnboardingViewModel

@Composable
fun RestoreWalletScreen(
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    val vm = activityViewModel<OnboardingViewModel> { app -> OnboardingViewModel(app, app.walletRepository) }
    val creating by vm.creating.collectAsState()
    val vmError by vm.error.collectAsState()

    var phrase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restaurar billetera") },
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
            Text(
                "Introduce tu frase de recuperación de 12 o 24 palabras en orden, separadas por espacios.",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))
            var visible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = phrase,
                onValueChange = { phrase = it },
                label = { Text("Frase de recuperación") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 6,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mostrar frase"
                        )
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            if (error != null) {
                ErrorBanner(error ?: "")
                Spacer(Modifier.height(12.dp))
            }
            if (vmError != null) {
                ErrorBanner(vmError ?: "")
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = "Continuar",
                loading = creating,
                enabled = phrase.isNotBlank(),
                onClick = {
                    if (vm.validateMnemonic(phrase)) {
                        error = null
                        vm.setRestorePhrase(phrase.trim().lowercase())
                        onContinue(phrase.trim().lowercase())
                    } else {
                        error = "La frase no es válida. Verifica palabras y orden."
                    }
                }
            )
        }
    }
}
