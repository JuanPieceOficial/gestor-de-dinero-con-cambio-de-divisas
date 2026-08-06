package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.data.db.TokenEntity
import com.cryptowallet.app.data.model.AccountInfo
import com.cryptowallet.app.data.model.Chain
import com.cryptowallet.app.data.model.TokenBalance
import com.cryptowallet.app.ui.components.CoinIcon
import com.cryptowallet.app.ui.components.LoadingBox
import com.cryptowallet.app.ui.components.formatAddress
import com.cryptowallet.app.ui.components.formatCryptoAmount
import com.cryptowallet.app.ui.components.formatFiat
import com.cryptowallet.app.ui.theme.Green
import com.cryptowallet.app.ui.theme.NavyCard
import com.cryptowallet.app.ui.theme.TextSecondary
import com.cryptowallet.app.ui.viewmodel.ChainGroup
import com.cryptowallet.app.ui.viewmodel.PortfolioUiState

@Composable
fun PortfolioScreen(
    portfolio: PortfolioUiState,
    account: AccountInfo?,
    onRefresh: (Boolean) -> Unit,
    onSendToken: (Chain, TokenEntity) -> Unit,
    onReceive: () -> Unit,
    onAddToken: () -> Unit,
    onRemoveToken: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TotalBalanceCard(
                totalFiat = portfolio.totalFiat,
                address = account?.address,
                refreshing = portfolio.refreshing,
                fiatCurrency = portfolio.fiatCurrency,
                onRefresh = { onRefresh(true) },
                onReceive = onReceive
            )
        }

        if (portfolio.loading && !portfolio.hasLoadedOnce) {
            item { LoadingBox() }
        }

        if (portfolio.error != null && !portfolio.hasLoadedOnce) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(onClick = { onRefresh(true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Reintentar")
                    }
                }
            }
        }

        if (portfolio.hasLoadedOnce && portfolio.byChain.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text("Sin activos todavía", color = TextSecondary)
                }
            }
        }

        portfolio.byChain.forEach { group ->
            item(key = "header_${group.chain.id}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        group.chain.name,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (group.items.isNotEmpty()) {
                        Text(formatFiat(group.chainFiat, portfolio.fiatCurrency), color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
            items(group.items, key = { "${it.token.chainId}:${it.token.address.lowercase()}" }) { balance ->
                TokenRow(
                    balance = balance,
                    fiatCurrency = portfolio.fiatCurrency,
                    onClick = { onSendToken(group.chain, balance.token.toEntity()) }
                )
            }
        }

        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Button(onClick = onAddToken) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Agregar token")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TotalBalanceCard(
    totalFiat: Double,
    address: String?,
    refreshing: Boolean,
    fiatCurrency: String,
    onRefresh: () -> Unit,
    onReceive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Green)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Saldo total", color = Color.Black.copy(alpha = 0.7f), fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            if (refreshing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(formatFiat(totalFiat, fiatCurrency), color = Color.Black, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (address != null) {
                Text(formatAddress(address), color = Color.Black.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            SendButton(onReceive)
        }
    }
}

@Composable
private fun SendButton(onReceive: () -> Unit) {
    androidx.compose.material3.FilledTonalButton(
        onClick = onReceive,
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.Black.copy(alpha = 0.12f),
            contentColor = Color.Black
        )
    ) {
        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.size(4.dp))
        Text("Recibir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TokenRow(
    balance: TokenBalance,
    fiatCurrency: String = "USD",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NavyCard)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinIcon(balance.token.symbol, size = 40)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(balance.token.symbol, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
            Text(
                balance.token.name,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatCryptoAmount(balance.balance, balance.token.symbol), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(formatFiat(balance.fiatValue, fiatCurrency), color = TextSecondary, fontSize = 12.sp)
        }
    }
}

private fun com.cryptowallet.app.data.model.TokenInfo.toEntity(): TokenEntity {
    return TokenEntity(
        id = "$chainId:${address.lowercase()}",
        chainId = chainId,
        address = address,
        symbol = symbol,
        name = name,
        decimals = decimals,
        isNative = isNative,
        custom = custom
    )
}
