package com.gestorfacil.app.ui.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestorfacil.app.data.model.Currency
import com.gestorfacil.app.data.repository.FinanceRepository
import com.gestorfacil.app.ui.theme.Accent
import com.gestorfacil.app.ui.theme.ChartColors
import com.gestorfacil.app.ui.theme.Destructive
import com.gestorfacil.app.ui.theme.OnMuted
import com.gestorfacil.app.ui.theme.Primary
import com.gestorfacil.app.ui.theme.Surface
import kotlin.math.abs

@Composable
fun HomeScreen(repository: FinanceRepository, currency: Currency = Currency.EUR) {
    val income by repository.totalIncome.collectAsState(0.0)
    val expense by repository.totalExpense.collectAsState(0.0)
    val balance = income - expense
    val allTransactions by repository.allTransactions.collectAsState(initial = emptyList())

    val expenseByCategory = remember(allTransactions) {
        allTransactions
            .filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { abs(it.amount) } }
            .entries
            .sortedByDescending { it.value }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Balance card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Primary)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "Balance Total",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currency.format(balance),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStat(
                        modifier = Modifier.weight(1f),
                        label = "Ingresos",
                        amount = currency.format(income),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        tint = Accent
                    )
                    MiniStat(
                        modifier = Modifier.weight(1f),
                        label = "Gastos",
                        amount = currency.format(expense),
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Ingresos",
                amount = currency.format(income),
                color = Accent
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Gastos",
                amount = currency.format(expense),
                color = Destructive
            )
        }

        // Pie chart card
        if (expenseByCategory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Gastos por categoría",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PieChart(
                            data = expenseByCategory.map { it.value },
                            colors = ChartColors,
                            modifier = Modifier.size(140.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            expenseByCategory.take(5).forEachIndexed { i, entry ->
                                val cat = entry.key
                                val amt = entry.value
                                val color = ChartColors[i % ChartColors.size]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .padding(0.dp)
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(color = color)
                                        }
                                    }
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = currency.format(amt),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tip card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Consejo del día",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Registrar cada gasto te ayuda a identificar oportunidades de ahorro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnMuted,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PieChart(
    data: List<Double>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = data.sum()
    if (total <= 0) return

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.15f
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        var startAngle = -90f
        data.forEachIndexed { i, value ->
            val sweepAngle = ((value / total) * 360f).toFloat()
            drawArc(
                color = colors[i % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun MiniStat(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Icon(
                imageVector = if (label == "Ingresos") Icons.AutoMirrored.Filled.TrendingUp
                    else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = OnMuted
            )
        }
    }
}
