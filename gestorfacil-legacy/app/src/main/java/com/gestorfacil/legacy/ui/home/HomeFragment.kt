package com.gestorfacil.legacy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.cardview.widget.CardView
import com.gestorfacil.legacy.GestorFacilLegacyApp
import com.gestorfacil.legacy.R
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val app = requireContext().applicationContext as GestorFacilLegacyApp
        val repo = app.repository
        val currency = app.settingsManager.selectedCurrency

        val nf = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val balanceText: TextView = view.findViewById(R.id.balance_text)
        val incomeText: TextView = view.findViewById(R.id.income_text)
        val expenseText: TextView = view.findViewById(R.id.expense_text)
        val tipText: TextView = view.findViewById(R.id.tip_text)
        val pieCard: CardView = view.findViewById(R.id.pie_card)
        val pieChart: PieChartView = view.findViewById(R.id.pie_chart)
        val pieLegend: LinearLayout = view.findViewById(R.id.pie_legend)

        val tips = listOf(
            "Ahorra al menos el 20% de tus ingresos cada mes.",
            "Lleva un registro de cada gasto, por pequeño que sea.",
            "Usa la regla 50/30/20: 50% necesidades, 30% deseos, 20% ahorro.",
            "Revisa tus suscripciones mensuales y cancela las que no uses.",
            "Establece un presupuesto para cada categoría de gasto.",
            "Compra con lista y evita las compras impulsivas.",
            "El mejor momento para empezar a ahorrar fue ayer. El segundo mejor es hoy."
        )
        tipText.text = "💡 ${tips.random()}"

        val pieColors = intArrayOf(
            0xFF2275C0.toInt(), 0xFF29996E.toInt(), 0xFFF59E0B.toInt(),
            0xFF8B5CF6.toInt(), 0xFFEF4444.toInt(), 0xFFEC4899.toInt(),
            0xFF06B6D4.toInt(), 0xFF84CC16.toInt()
        )

        viewLifecycleOwner.lifecycleScope.launch {
            combine(repo.totalIncome, repo.totalExpense) { income, expense ->
                Pair(income, expense)
            }.collect { (income, expense) ->
                val balance = income - expense
                balanceText.text = currency.format(balance)
                incomeText.text = currency.format(income)
                expenseText.text = currency.format(expense)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repo.allTransactions.collect { txs ->
                val expenseMap = mutableMapOf<String, Float>()
                for (tx in txs) {
                    if (tx.type == "expense") {
                        val amt = kotlin.math.abs(tx.amount).toFloat()
                        expenseMap[tx.category] = (expenseMap[tx.category] ?: 0f) + amt
                    }
                }

                if (expenseMap.isEmpty()) {
                    pieCard.visibility = View.GONE
                    return@collect
                }

                val sorted = expenseMap.entries
                    .sortedByDescending { it.value }
                    .take(8)

                pieCard.visibility = View.VISIBLE

                val slices = sorted.mapIndexed { i, (cat, amt) ->
                    PieChartView.Slice(cat, amt, pieColors[i % pieColors.size])
                }
                pieChart.slices = slices

                pieLegend.removeAllViews()
                for (i in sorted.indices) {
                    val (cat, amt) = sorted[i]
                    val row = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_legend_row, pieLegend, false)

                    row.findViewById<View>(R.id.legend_dot).apply {
                        setBackgroundColor(pieColors[i % pieColors.size])
                    }
                    row.findViewById<TextView>(R.id.legend_label).text = cat
                    row.findViewById<TextView>(R.id.legend_value).text = currency.format(amt.toDouble())

                    pieLegend.addView(row)
                }
            }
        }
    }
}
