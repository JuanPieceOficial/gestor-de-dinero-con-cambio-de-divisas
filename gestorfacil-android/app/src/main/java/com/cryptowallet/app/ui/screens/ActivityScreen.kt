package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.cryptowallet.app.data.api.Chains
import com.cryptowallet.app.data.db.TxRecordEntity
import com.cryptowallet.app.ui.components.CoinIcon
import com.cryptowallet.app.ui.components.formatAddress
import com.cryptowallet.app.ui.components.formatTxTime
import com.cryptowallet.app.ui.theme.Green
import com.cryptowallet.app.ui.theme.NavyCard
import com.cryptowallet.app.ui.theme.TextSecondary

@Composable
fun ActivityScreen(
    history: List<TxRecordEntity>,
    onRefresh: () -> Unit
) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text("Sin movimientos todavía", color = TextSecondary)
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(history, key = { it.id }) { tx ->
            TxRow(tx)
        }
    }
}

@Composable
private fun TxRow(tx: TxRecordEntity) {
    val chain = Chains.byId(tx.chainId)
    val isSend = tx.type == "send"
    val icon: ImageVector = if (isSend) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    val iconColor = if (isSend) MaterialTheme.colorScheme.error else Green
    val statusColor = when (tx.status) {
        "success" -> Green
        "failed" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NavyCard)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${if (isSend) "Enviado" else "Recibido"} · ${tx.tokenSymbol}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${chain?.name ?: "Red ${tx.chainId}"} · ${formatTxTime(tx.timestamp)}",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
            Text(
                "a ${formatAddress(tx.to)}",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (isSend) "-" else "+") + tx.amount + " " + tx.tokenSymbol,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isSend) Color.White else Green
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                Spacer(Modifier.size(3.dp))
                Text(
                    tx.status.replaceFirstChar { it.uppercase() },
                    color = statusColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}
