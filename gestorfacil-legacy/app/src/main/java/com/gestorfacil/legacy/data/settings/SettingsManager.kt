package com.gestorfacil.legacy.data.settings

import android.content.Context
import com.gestorfacil.legacy.data.model.BolivarRate
import com.gestorfacil.legacy.data.model.Currency
import com.google.gson.Gson

class SettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences("gestorfacil_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var selectedCurrency: Currency
        get() = Currency.fromCode(prefs.getString(KEY_CURRENCY, "EUR") ?: "EUR")
        set(value) = prefs.edit().putString(KEY_CURRENCY, value.code).apply()

    var useDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var cachedRate: BolivarRate?
        get() {
            val json = prefs.getString(KEY_CACHED_RATE, null) ?: return null
            return try { gson.fromJson(json, BolivarRate::class.java) } catch (_: Exception) { null }
        }
        set(value) {
            if (value != null) {
                prefs.edit().putString(KEY_CACHED_RATE, gson.toJson(value)).apply()
            } else {
                prefs.edit().remove(KEY_CACHED_RATE).apply()
            }
        }

    companion object {
        const val KEY_CURRENCY = "selected_currency"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_CACHED_RATE = "cached_dolar_rate"
    }
}
