@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.data.api.Chains
import com.cryptowallet.app.ui.components.ErrorBanner
import com.cryptowallet.app.ui.components.PrimaryButton
import com.cryptowallet.app.ui.components.walletViewModel
import com.cryptowallet.app.ui.theme.NavyCard
import com.cryptowallet.app.ui.theme.TextSecondary
import com.cryptowallet.app.ui.viewmodel.AddTokenViewModel

@Composable
fun AddTokenScreen(
    onBack: () -> Unit,
    onAdded: () -> Unit
) {
    val vm = walletViewModel<AddTokenViewModel> { app -> AddTokenViewModel(app, app.walletRepository) }
    val adding by vm.adding.collectAsState()
    val error by vm.error.collectAsState()

    var selectedChain by remember { mutableStateOf(Chains.all.first()) }
    var contract by remember { mutableStateOf("") }
    var chainMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar token") },
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
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp)
        ) {
            Text("Pega la dirección del contrato del token.", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavyCard)
                    .clickable { chainMenu = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Red", color = TextSecondary, fontSize = 11.sp)
                    Text(selectedChain.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
            }
            DropdownMenu(expanded = chainMenu, onDismissRequest = { chainMenu = false }) {
                (Chains.all + Chains.testnets).forEach { chain ->
                    DropdownMenuItem(
                        text = { Text(chain.name) },
                        onClick = {
                            selectedChain = chain
                            chainMenu = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = contract,
                onValueChange = { contract = it },
                label = { Text("Dirección del contrato (0x...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            if (error != null) {
                ErrorBanner(error ?: "")
                Spacer(Modifier.height(12.dp))
            }
            Text(
                "El nombre, símbolo y decimales se obtienen automáticamente del contrato.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "Agregar token",
                loading = adding,
                enabled = contract.trim().startsWith("0x") && contract.trim().length == 42,
                onClick = {
                    vm.addToken(selectedChain, contract) { onAdded() }
                }
            )
        }
    }
}
