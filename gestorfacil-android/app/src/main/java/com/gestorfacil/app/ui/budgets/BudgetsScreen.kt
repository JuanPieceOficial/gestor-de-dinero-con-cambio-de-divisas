package com.gestorfacil.app.ui.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestorfacil.app.data.model.Currency
import com.gestorfacil.app.data.repository.FinanceRepository
import com.gestorfacil.app.ui.theme.Accent
import com.gestorfacil.app.ui.theme.OnMuted
import com.gestorfacil.app.ui.theme.Primary
import com.gestorfacil.app.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BudgetsScreen(repository: FinanceRepository, currency: Currency = Currency.EUR) {
    val budgets by repository.allBudgets.collectAsState(initial = emptyList())
    val spentMap = remember { mutableStateMapOf<String, Double>() }

    LaunchedEffect(budgets) {
        budgets.forEach { budget ->
            val spent = withContext(Dispatchers.IO) {
                repository.getSpentByCategory(budget.category)
            }
            spentMap[budget.category] = spent
        }
    }

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
                        Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Control de Presupuestos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(18.dp))
                budgets.forEach { budget ->
                    val spent = spentMap[budget.category] ?: 0.0
                    val percentage = if (budget.limit > 0)
                        (spent / budget.limit).toFloat().coerceAtMost(1f) else 0f
                    val isOver = spent > budget.limit
                    val isNear = percentage > 0.85f && !isOver
                    val barColor = when {
                        isOver -> MaterialTheme.colorScheme.error
                        isNear -> Color(0xFFF59E0B)
                        else -> Primary
                    }

                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = budget.category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            val labelColor = when {
                                isOver -> MaterialTheme.colorScheme.error
                                isNear -> Color(0xFFF59E0B)
                                else -> OnMuted
                            }
                            Text(
                                text = "${currency.format(spent)} / ${currency.format(budget.limit)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = labelColor
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                        )
                    }
                    if (budget != budgets.last()) Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
