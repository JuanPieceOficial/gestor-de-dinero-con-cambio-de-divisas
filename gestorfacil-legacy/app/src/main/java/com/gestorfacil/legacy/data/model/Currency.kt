package com.gestorfacil.legacy.data.model

import java.util.Currency as JavaCurrency
import java.util.Locale

enum class Currency(
    val code: String,
    val symbol: String,
    val locale: Locale
) {
    EUR("EUR", "€", Locale("es", "ES")),
    USD("USD", "$", Locale("en", "US")),
    VES("VES", "Bs.", Locale("es", "VE")),
    COP("COP", "\$", Locale("es", "CO")),
    ARS("ARS", "\$", Locale("es", "AR")),
    MXN("MXN", "\$", Locale("es", "MX")),
    BRL("BRL", "R\$", Locale("pt", "BR"));

    fun format(amount: Double): String {
        val nf = java.text.NumberFormat.getCurrencyInstance(locale)
        try {
            val jCurrency = JavaCurrency.getInstance(code)
            nf.currency = jCurrency
        } catch (_: Exception) {}
        return nf.format(amount)
    }

    companion object {
        fun fromCode(code: String): Currency {
            return entries.find { it.code == code } ?: USD
        }
    }
}
