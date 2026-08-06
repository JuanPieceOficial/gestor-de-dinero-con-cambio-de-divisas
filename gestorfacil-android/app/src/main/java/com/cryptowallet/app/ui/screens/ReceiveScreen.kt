@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Surface
import com.cryptowallet.app.ui.components.PrimaryButton
import com.cryptowallet.app.ui.components.QrCode
import com.cryptowallet.app.ui.components.copyToClipboard
import com.cryptowallet.app.ui.components.formatAddress
import com.cryptowallet.app.ui.theme.Green
import com.cryptowallet.app.ui.theme.NavyCard
import com.cryptowallet.app.ui.theme.TextSecondary

@Composable
fun ReceiveScreen(
    address: String,
    chainName: String,
    onBack: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val chains = listOf(
        Triple("Ethereum", "ETH & ERC-20", Green),
        Triple("BNB Smart Chain", "BNB & BEP-20", Green),
        Triple("Polygon", "POL / MATIC", Green),
        Triple("Arbitrum", "Arbitrum One", Green),
        Triple("Optimism", "Optimism Network", Green),
        Triple("Base", "Base L2", Green),
        Triple("Avalanche", "AVAX C-Chain", Green)
    )

    var selectedChain by remember { mutableStateOf(chains[0]) }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Depositar Cripto") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Selecciona la red de depósito (Estilo Binance)", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))

            // Selector de red estilo Binance dropdown card
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NavyCard)
                        .clickable { menuExpanded = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Red de depósito", color = TextSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(selectedChain.first, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedChain.second, color = Green, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                    }
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    chains.forEach { chain ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(chain.first, fontWeight = FontWeight.Bold)
                                    Text(chain.second, color = TextSecondary, fontSize = 11.sp)
                                }
                            },
                            onClick = {
                                selectedChain = chain
                                menuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(androidx.compose.ui.graphics.Color.White)
                    .padding(20.dp)
            ) {
                QrCode(data = address, size = 200.dp)
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavyCard)
                    .clickable { copyToClipboard(clipboard, address) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(10.dp))
                Text(
                    formatAddress(address, 12, 10),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Toca para copiar dirección", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = com.cryptowallet.app.ui.theme.Warning.copy(alpha = 0.12f)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Envía únicamente activos compatibles a través de la red ${selectedChain.first}. Se requiere confirmación de red.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "Comprar con tarjeta (MoonPay / Ramp)",
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://buy.moonpay.com?walletAddress=$address")
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}
