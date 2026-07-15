package com.gestorfacil.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestorfacil.app.data.database.BudgetEntity
import com.gestorfacil.app.data.database.TransactionEntity
import com.gestorfacil.app.data.model.Currency
import com.gestorfacil.app.data.repository.FinanceRepository
import com.gestorfacil.app.ui.theme.OnMuted
import com.gestorfacil.app.ui.theme.Primary
import com.gestorfacil.app.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun AIScreen(repository: FinanceRepository, currency: Currency = Currency.EUR) {
    val transactions by repository.allTransactions.collectAsState(initial = emptyList())
    val budgets by repository.allBudgets.collectAsState(initial = emptyList())

    var loading by remember { mutableStateOf(false) }
    val suggestions = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Asistente Financiero",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Analiza tus finanzas",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnMuted
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        loading = true
                        scope.launch {
                            val result = generateLocalSuggestions(transactions, budgets, currency)
                            suggestions.clear()
                            suggestions.addAll(result)
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !loading && transactions.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Obtener Sugerencias", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            suggestions.forEachIndexed { i, s -> SuggestionCard(i + 1, s) }
        } else if (!loading) {
            EmptyAI(hasTransactions = transactions.isNotEmpty())
        }
    }
}

@Composable
private fun SuggestionCard(index: Int, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Primary.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = "$index",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EmptyAI(hasTransactions: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Lightbulb,
            contentDescription = null,
            tint = OnMuted.copy(alpha = 0.2f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (hasTransactions) "Tocá el botón para generar sugerencias"
                   else "Registrá transacciones para recibir consejos",
            style = MaterialTheme.typography.bodySmall,
            color = OnMuted.copy(alpha = 0.6f)
        )
    }
}

private suspend fun generateLocalSuggestions(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    currency: Currency
): List<String> = withContext(Dispatchers.Default) {
    val totalExpense = transactions.filter { it.type == "expense" }.sumOf { abs(it.amount) }
    val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }
    val byCategory = transactions.filter { it.type == "expense" }
        .groupBy { it.category }
        .mapValues { it.value.sumOf { abs(it.amount) } }

    val s = mutableListOf<String>()

    if (totalExpense > totalIncome) {
        s.add("Tus gastos (${currency.format(totalExpense)}) superan tus ingresos (${currency.format(totalIncome)}). Revisá gastos no esenciales.")
    }

    byCategory.entries.sortedByDescending { it.value }.take(3).forEach { (cat, amount) ->
        val budget = budgets.find { it.category == cat }
        if (budget != null && amount > budget.limit) {
            s.add("Excediste tu presupuesto de $cat (${currency.format(amount)} de ${currency.format(budget.limit)}). Considerá reducir gastos.")
        }
    }

    val savingsRate = if (totalIncome > 0)
        ((totalIncome - totalExpense) / totalIncome * 100).toInt() else 0
    s.add("Tu tasa de ahorro es del $savingsRate%. ${if (savingsRate < 10) "Intentá ahorrar al menos un 10%." else "¡Buen trabajo!"}")
    s.add("Hacer seguimiento semanal de tus gastos te ayuda a mantener el control.")

    s
}
