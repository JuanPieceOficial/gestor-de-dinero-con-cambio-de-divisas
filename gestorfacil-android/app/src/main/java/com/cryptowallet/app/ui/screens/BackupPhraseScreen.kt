@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalClipboardManager
import com.cryptowallet.app.ui.components.copyToClipboard
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.ui.components.walletViewModel
import com.cryptowallet.app.ui.theme.Green
import com.cryptowallet.app.ui.theme.TextSecondary
import com.cryptowallet.app.ui.theme.Warning
import com.cryptowallet.app.ui.viewmodel.WalletViewModel

@Composable
fun BackupPhraseScreen(
    onBack: () -> Unit
) {
    val vm = walletViewModel<WalletViewModel> { app -> WalletViewModel(app, app.walletRepository) }
    var phrase by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        vm.getBackupMnemonic { phrase = it }
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
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = Warning.copy(alpha = 0.12f)
            ) {
                androidx.compose.foundation.layout.Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Warning, modifier = Modifier.height(20.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "Nunca compartas esta frase. Cualquiera con ella puede controlar tus fondos.",
                        color = Warning,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            val words = phrase?.split(" ") ?: emptyList()
            if (words.isNotEmpty()) {
                val clipboard = LocalClipboardManager.current
                val formattedPhrase = words.mapIndexed { idx, w -> "${idx + 1}. $w" }.joinToString("\n")

                androidx.compose.material3.OutlinedButton(
                    onClick = { copyToClipboard(clipboard, formattedPhrase) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
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
                        BackUpWord(index + 1, word)
                    }
                }
            } else {
                Text("Cargando...", color = TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun BackUpWord(index: Int, word: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text("$index", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(word, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
