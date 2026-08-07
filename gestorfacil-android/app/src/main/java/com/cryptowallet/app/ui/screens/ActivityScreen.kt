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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.cryptowallet.app.data.api.Chains
import com.cryptowallet.app.data.db.TxRecordEntity
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
    var selected by remember { mutableStateOf<TxRecordEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(history, key = { it.id }) { tx ->
            TxRow(tx, onClick = { selected = tx })
        }
    }

    selected?.let { tx ->
        TxDetailDialog(tx = tx, onDismiss = { selected = null })
    }
}

@Composable
private fun TxRow(tx: TxRecordEntity, onClick: () -> Unit) {
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
            .clickable(onClick = onClick)
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

@Composable
private fun TxDetailDialog(tx: TxRecordEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val chain = Chains.byId(tx.chainId)
    val isSend = tx.type == "send"

    fun openExplorer() {
        val base = chain?.explorerUrl ?: "https://etherscan.io"
        val url = "$base/tx/${tx.hash}"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "${if (isSend) "Envío" else "Recepción"} · ${tx.tokenSymbol}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow("Red", chain?.name ?: "Red ${tx.chainId}")
                DetailRow("Cantidad", "${if (isSend) "-" else "+"}${tx.amount} ${tx.tokenSymbol}")
                DetailRow("Estado", tx.status.replaceFirstChar { it.uppercase() })
                DetailRow("Fecha", formatTxTime(tx.timestamp))
                if (tx.feeWei.isNotBlank() && tx.feeWei != "0") {
                    DetailRow("Comisión (red)", formatFee(tx.feeWei))
                }
                HashRow("Desde", tx.from)
                HashRow("Hacia", tx.to)
                HashRow("Hash", tx.hash)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Explorador", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { clipboard.setText(AnnotatedString(tx.hash)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar hash", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { openExplorer() }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Abrir en explorador", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun HashRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            formatAddress(value, 8, 6),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatFee(feeWei: String): String {
    return try {
        val wei = java.math.BigDecimal(feeWei)
        val eth = wei.movePointLeft(18).stripTrailingZeros().toPlainString()
        val num = eth.toDoubleOrNull() ?: 0.0
        if (num >= 0.00001) "$eth ETH" else "${num * 1_000_000_000} Gwei"
    } catch (e: Exception) {
        feeWei
    }
}
