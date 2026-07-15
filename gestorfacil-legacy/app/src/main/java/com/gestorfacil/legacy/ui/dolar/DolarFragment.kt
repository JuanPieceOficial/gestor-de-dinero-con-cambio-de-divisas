package com.gestorfacil.legacy.ui.dolar

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gestorfacil.legacy.GestorFacilLegacyApp
import com.gestorfacil.legacy.R
import com.gestorfacil.legacy.data.api.DolarApi
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DolarFragment : Fragment() {

    private var selectedRateIdx = 1
    private val rateValues = mutableListOf<Double>()
    private var isUpdating = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_dolar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val app = requireContext().applicationContext as GestorFacilLegacyApp
        val api = DolarApi()
        val nf = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val ratesCard: View = view.findViewById(R.id.rates_card)
        val ratesContainer: LinearLayout = view.findViewById(R.id.rates_container)
        val converterCard: View = view.findViewById(R.id.converter_card)
        val rateSelector: LinearLayout = view.findViewById(R.id.rate_selector)
        val usdInput: EditText = view.findViewById(R.id.usd_input)
        val vesInput: EditText = view.findViewById(R.id.ves_input)
        val refreshBtn: View = view.findViewById(R.id.refresh_btn)
        val errorText: TextView = view.findViewById(R.id.error_text)
        val loadingSpinner: ProgressBar = view.findViewById(R.id.loading_spinner)

        fun convertUsdToVes(usd: Double): Double? {
            if (rateValues.isEmpty() || selectedRateIdx >= rateValues.size) return null
            return usd * rateValues[selectedRateIdx]
        }

        fun convertVesToUsd(ves: Double): Double? {
            if (rateValues.isEmpty() || selectedRateIdx >= rateValues.size) return null
            val rate = rateValues[selectedRateIdx]
            return if (rate > 0) ves / rate else null
        }

        fun updateConverter(source: EditText) {
            if (isUpdating) return
            isUpdating = true
            try {
                val raw = source.text.toString().replace(",", "")
                val value = raw.toDoubleOrNull() ?: return

                if (source === usdInput) {
                    val ves = convertUsdToVes(value)
                    if (ves != null) {
                        val fmt = nf.format(ves)
                        vesInput.setText(fmt)
                        vesInput.setSelection(fmt.length)
                    }
                } else {
                    val usd = convertVesToUsd(value)
                    if (usd != null) {
                        val fmt = nf.format(usd)
                        usdInput.setText(fmt)
                        usdInput.setSelection(fmt.length)
                    }
                }
            } finally {
                isUpdating = false
            }
        }

        usdInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateConverter(usdInput) }
        })

        vesInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateConverter(vesInput) }
        })

        fun buildChips() {
            val colors = listOf(R.color.primary, R.color.usdt, R.color.promedio, R.color.euro)
            rateSelector.removeAllViews()
            rateValues.forEachIndexed { i, value ->
                val chip = Button(requireContext(), null, android.R.attr.buttonBarButtonStyle).apply {
                    text = "Bs. ${nf.format(value)}"
                    setOnClickListener {
                        selectedRateIdx = i
                        val tmp = isUpdating
                        isUpdating = true
                        try {
                            usdInput.setText("")
                            vesInput.setText("")
                        } finally {
                            isUpdating = tmp
                        }
                        updateChips(rateSelector, selectedRateIdx, colors)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                rateSelector.addView(chip)
            }
            updateChips(rateSelector, selectedRateIdx, colors)
        }

        fun showCached(cached: com.gestorfacil.legacy.data.model.BolivarRate) {
            ratesCard.visibility = View.VISIBLE
            converterCard.visibility = View.VISIBLE
            ratesContainer.removeAllViews()
            rateSelector.removeAllViews()
            rateValues.clear()

            fun addRow(label: String, value: Double, color: Int) {
                val row = layoutInflater.inflate(R.layout.item_rate_row, null)
                row.findViewById<TextView>(R.id.rate_label).text = label
                row.findViewById<TextView>(R.id.rate_value).text = "Bs. ${nf.format(value)}"
                row.findViewById<TextView>(R.id.rate_value).setTextColor(requireContext().getColor(color))
                ratesContainer.addView(row)
                rateValues.add(value)
            }

            addRow("Dólar BCV", cached.oficial, R.color.primary)
            cached.binance?.let { addRow("Dólar USDT", it.promedio, R.color.usdt) }
            addRow("Dólar Promedio", cached.paralelo, R.color.promedio)
            if (cached.euro > 0) addRow("Euro", cached.euro, R.color.euro)

            cached.binance?.let { b ->
                val v = View(requireContext()).apply {
                    setBackgroundColor(requireContext().getColor(R.color.on_muted))
                    alpha = 0.15f
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                }
                ratesContainer.addView(v)
                val diff = b.promedio - cached.oficial
                addRow("Brecha BCV ↔ USDT", diff, if (diff > 0) R.color.income else R.color.expense)
            }

            buildChips()
            val src = TextView(requireContext()).apply {
                text = "Fuente: Al Cambio"
                textSize = 11f
                setTextColor(requireContext().getColor(R.color.on_muted))
                alpha = 0.5f
            }
            ratesContainer.addView(src)
        }

        fun loadRates() {
            loadingSpinner.visibility = View.VISIBLE
            errorText.visibility = View.GONE
            lifecycleScope.launch {
                try {
                    val rate = api.fetchRates()
                    app.settingsManager.cachedRate = rate
                    showCached(rate)
                } catch (_: Exception) {
                    app.settingsManager.cachedRate?.let { showCached(it) }
                        ?: run { errorText.visibility = View.VISIBLE }
                }
                loadingSpinner.visibility = View.GONE
            }
        }

        refreshBtn.setOnClickListener { loadRates() }

        app.settingsManager.cachedRate?.let { showCached(it) }
        loadRates()
    }

    private fun updateChips(container: LinearLayout, selected: Int, colors: List<Int>) {
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? Button ?: continue
            chip.isSelected = i == selected
            chip.setBackgroundColor(
                if (i == selected) requireContext().getColor(colors.getOrElse(i) { R.color.primary })
                else requireContext().getColor(android.R.color.transparent)
            )
            chip.setTextColor(
                if (i == selected) -0x1
                else requireContext().getColor(R.color.on_muted)
            )
        }
    }
}
