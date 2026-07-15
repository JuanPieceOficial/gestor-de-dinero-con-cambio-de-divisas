package com.gestorfacil.legacy.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.gestorfacil.legacy.GestorFacilLegacyApp
import com.gestorfacil.legacy.R
import com.gestorfacil.legacy.data.model.Currency

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val app = requireContext().applicationContext as GestorFacilLegacyApp

        val currencySpinner: Spinner = view.findViewById(R.id.currency_spinner)
        val darkModeSwitch: SwitchCompat = view.findViewById(R.id.dark_mode_switch)

        val currencies = Currency.entries.toList()
        val labels = currencies.map { "${it.code} (${it.symbol})" }

        currencySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, labels)

        val currentIdx = currencies.indexOf(app.settingsManager.selectedCurrency)
        if (currentIdx >= 0) currencySpinner.setSelection(currentIdx)

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                app.settingsManager.selectedCurrency = currencies[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        darkModeSwitch.isChecked = app.settingsManager.useDarkMode
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            app.settingsManager.useDarkMode = isChecked
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            requireActivity().recreate()
        }
    }
}
