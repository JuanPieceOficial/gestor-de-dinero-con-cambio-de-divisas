@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.ui.components.ErrorBanner
import com.cryptowallet.app.ui.components.PrimaryButton
import com.cryptowallet.app.ui.components.activityViewModel
import com.cryptowallet.app.ui.components.copyToClipboard
import com.cryptowallet.app.ui.theme.Green
import com.cryptowallet.app.ui.theme.TextSecondary
import com.cryptowallet.app.ui.viewmodel.OnboardingViewModel

@Composable
fun CreateMnemonicScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val vm = activityViewModel<OnboardingViewModel> { app ->
        OnboardingViewModel(app, app.walletRepository)
    }
    val mnemonic by vm.mnemonic.collectAsState()
    val creating by vm.creating.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(Unit) {
        if (mnemonic == null) vm.generateMnemonic()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frase de respaldo") },
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
                "Escribe y guarda esta frase de 24 palabras en un lugar seguro.\n" +
                    "Es la ÚNICA forma de recuperar tus fondos. Nunca la compartas.",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))

            val words = mnemonic?.split(" ") ?: emptyList()
            if (creating) {
                Text("Generando frase segura...", color = TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (words.isNotEmpty()) {
                val clipboard = LocalClipboardManager.current
                val formattedPhrase = words.mapIndexed { idx, w -> "${idx + 1}. $w" }.joinToString("\n")

                androidx.compose.material3.OutlinedButton(
                    onClick = { copyToClipboard(clipboard, formattedPhrase) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Green
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Copiar frase con números (1-24)")
                }
                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(words) { index, word ->
                        WordChip(index + 1, word)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (error != null) {
                ErrorBanner(error ?: "")
                Spacer(Modifier.height(12.dp))
            }
            PrimaryButton(
                text = "Guardé mi frase, continuar",
                onClick = onContinue,
                enabled = words.isNotEmpty()
            )
        }
    }
}

@Composable
private fun WordChip(index: Int, word: String) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text("$index", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(word, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
