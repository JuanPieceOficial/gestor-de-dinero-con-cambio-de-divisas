package com.gestorfacil.legacy.data.api

import com.gestorfacil.legacy.data.model.AlCambioResponse
import com.gestorfacil.legacy.data.model.BinanceRate
import com.gestorfacil.legacy.data.model.BolivarRate
import com.gestorfacil.legacy.data.model.DolarResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class DolarApi {

    private val gson = Gson()

    suspend fun fetchRates(): BolivarRate = withContext(Dispatchers.IO) {
        try {
            fetchAlCambioRates()
        } catch (_: Exception) {
            fetchDolarApiRates()
        }
    }

    private suspend fun fetchAlCambioRates(): BolivarRate = withContext(Dispatchers.IO) {
        val query = """{"query":"query { getBinanceP2PAverages { sellAverage buyAverage } getCountryConversions(payload: {countryCode: \"VE\"}) { conversionRates { type official baseValue rateCurrency { code } } } }"}"""
        val json = postJson("https://api.alcambio.app/graphql", query)
        val resp = gson.fromJson(json, AlCambioResponse::class.java)

        val conversions = resp?.data?.getCountryConversions?.conversionRates ?: emptyList()

        val bcvRate = conversions
            .find { it.type == "SECONDARY" && it.official && it.rateCurrency.code == "USD" }
            ?.baseValue
            ?: conversions
                .find { it.type == "OTHER" && it.official && it.rateCurrency.code == "USD" }
                ?.baseValue ?: 0.0

        val euroRate = conversions
            .find { it.type == "OTHER" && it.official && it.rateCurrency.code == "EUR" }
            ?.baseValue ?: 0.0

        val binanceAvg = resp?.data?.getBinanceP2PAverages
        val binance = if (binanceAvg != null) {
            val prom = (binanceAvg.buyAverage + binanceAvg.sellAverage) / 2
            BinanceRate(
                compra = binanceAvg.buyAverage,
                venta = binanceAvg.sellAverage,
                promedio = prom
            )
        } else null

        val usdt = binance?.promedio ?: bcvRate

        BolivarRate(
            oficial = bcvRate,
            paralelo = (bcvRate + usdt) / 2,
            binance = binance,
            euro = euroRate,
            lastUpdated = ""
        )
    }

    private suspend fun fetchDolarApiRates(): BolivarRate = withContext(Dispatchers.IO) {
        val json = URL("https://ve.dolarapi.com/v1/dolares").readText()
        val listType = object : TypeToken<List<DolarResponse>>() {}.type
        val list: List<DolarResponse> = gson.fromJson(json, listType)

        val oficial = list.find { it.fuente == "oficial" }?.promedio ?: 0.0
        val paralelo = list.find { it.fuente == "paralelo" }?.promedio ?: 0.0
        val lastUpdated = list.firstOrNull()?.fechaActualizacion ?: ""

        val binance = fetchBinanceRates()

        BolivarRate(
            oficial = oficial,
            paralelo = paralelo,
            binance = binance,
            lastUpdated = lastUpdated
        )
    }

    private fun postJson(url: String, body: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        OutputStreamWriter(conn.outputStream).use { it.write(body) }
        return conn.inputStream.bufferedReader().readText()
    }

    private fun fetchBinanceRates(): BinanceRate? {
        return try {
            val body = """{"asset":"USDT","fiat":"VES","tradeType":"SELL","page":1,"rows":5}"""
            val json = postJson("https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search", body)
            val resp = gson.fromJson(json, com.gestorfacil.legacy.data.model.BinanceResponse::class.java)
            val prices = resp.data.map { it.adv.price.toDouble() }
            val compra = prices.average()

            val body2 = """{"asset":"USDT","fiat":"VES","tradeType":"BUY","page":1,"rows":5}"""
            val json2 = postJson("https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search", body2)
            val resp2 = gson.fromJson(json2, com.gestorfacil.legacy.data.model.BinanceResponse::class.java)
            val prices2 = resp2.data.map { it.adv.price.toDouble() }
            val venta = prices2.average()

            BinanceRate(compra = compra, venta = venta, promedio = (compra + venta) / 2)
        } catch (_: Exception) {
            null
        }
    }
}
