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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.ui.components.ErrorBanner
import com.cryptowallet.app.ui.components.PrimaryButton
import com.cryptowallet.app.ui.components.activityViewModel
import com.cryptowallet.app.ui.theme.TextSecondary
import com.cryptowallet.app.ui.viewmodel.OnboardingViewModel

@Composable
fun VerifyMnemonicScreen(
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    val vm = activityViewModel<OnboardingViewModel> { app -> OnboardingViewModel(app, app.walletRepository) }
    val mnemonic by vm.mnemonic.collectAsState()

    val words = remember(mnemonic) { (mnemonic ?: "").split(" ").filter { it.isNotEmpty() } }
    val quizIndices = remember(words) {
        if (words.size >= 3) {
            words.indices.shuffled().take(3).sorted()
        } else {
            emptyList()
        }
    }
    val answers = remember { mutableStateOf(listOf("", "", "")) }
    val error = remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verifica tu frase") },
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
                "Escribe las palabras que te pedimos para confirmar que guardaste bien tu frase.",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(20.dp))
            quizIndices.forEachIndexed { pos, wordIndex ->
                val word = words.getOrNull(wordIndex) ?: return@forEachIndexed
                OutlinedTextField(
                    value = answers.value[pos],
                    onValueChange = { new ->
                        answers.value = answers.value.toMutableList().also { it[pos] = new.trim() }
                    },
                    label = { Text("Palabra #${wordIndex + 1}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
            }
            if (error.value != null) {
                ErrorBanner(error.value ?: "")
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = "Verificar",
                enabled = quizIndices.isNotEmpty(),
                onClick = {
                    val correct = quizIndices.map { words[it] }
                    if (answers.value == correct) {
                        error.value = null
                        onVerified()
                    } else {
                        error.value = "Alguna palabra no coincide. Revisa tu frase guardada."
                    }
                }
            )
        }
    }
}
