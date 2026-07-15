package com.gestorfacil.app.ui.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gestorfacil.app.data.database.TransactionEntity
import com.gestorfacil.app.data.model.Currency
import com.gestorfacil.app.data.repository.FinanceRepository
import com.gestorfacil.app.ui.theme.Accent
import com.gestorfacil.app.ui.theme.OnMuted
import com.gestorfacil.app.ui.theme.Primary
import com.gestorfacil.app.ui.theme.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    repository: FinanceRepository,
    currency: Currency = Currency.EUR,
    onEdit: ((TransactionEntity) -> Unit)? = null
) {
    val transactions by repository.allTransactions.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(transactions, searchQuery) {
        if (searchQuery.isBlank()) transactions
        else transactions.filter { t ->
            t.description.contains(searchQuery, ignoreCase = true) ||
            t.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar movimientos...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = OnMuted)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Cancel, contentDescription = "Limpiar", tint = OnMuted)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        if (filtered.isEmpty()) {
            EmptyState(hasFilter = searchQuery.isNotBlank())
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filtered, key = { it.id }) { t ->
                    TransactionItem(
                        transaction = t,
                        currency = currency,
                        onDelete = {
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.deleteTransaction(t.id)
                            }
                        },
                        onEdit = { onEdit?.invoke(t) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    currency: Currency,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val icon = getCategoryIcon(transaction.category)
    val isIncome = transaction.type == "income"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.combinedClickable(
            onClick = { },
            onLongClick = onEdit
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isIncome) Accent.copy(alpha = 0.1f)
                        else Primary.copy(alpha = 0.08f)
                )
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isIncome) Accent else Primary,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnMuted
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnMuted.copy(alpha = 0.3f)
                    )
                    Text(
                        text = formatDate(transaction.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnMuted
                    )
                }
            }
            val sign = if (isIncome) "+" else "-"
            val amt = if (isIncome) transaction.amount else Math.abs(transaction.amount)
            Text(
                text = "$sign${currency.format(amt)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) Accent else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = OnMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(hasFilter: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Receipt,
            contentDescription = null,
            tint = OnMuted.copy(alpha = 0.25f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (hasFilter) "Sin resultados" else "No hay movimientos",
            style = MaterialTheme.typography.titleMedium,
            color = OnMuted
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (hasFilter) "Probá con otro término de búsqueda"
                   else "Tocá + para registrar tu primer movimiento",
            style = MaterialTheme.typography.bodySmall,
            color = OnMuted.copy(alpha = 0.6f)
        )
    }
}

private fun getCategoryIcon(category: String): ImageVector = when (category) {
    "Alimentación" -> Icons.Default.Restaurant
    "Transporte" -> Icons.Default.DirectionsCar
    "Ocio" -> Icons.Default.ShoppingCart
    "Hogar" -> Icons.Default.Home
    "Salud" -> Icons.Default.Favorite
    "Educación" -> Icons.Default.School
    else -> Icons.Default.Category
}

private fun formatDate(date: String): String = try {
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val output = SimpleDateFormat("dd MMM", Locale("es", "ES"))
    output.format(input.parse(date)!!)
} catch (_: Exception) { date }
