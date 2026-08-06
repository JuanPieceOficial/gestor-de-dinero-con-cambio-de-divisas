package com.cryptowallet.app.data.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Precios en USD vía CoinGecko (endpoints públicos sin API key).
 */
class PriceClient {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun nativePrices(coinGeckoIds: Map<String, String>, vsCurrency: String = "usd"): Map<String, Double> {
        if (coinGeckoIds.isEmpty()) return emptyMap()
        val ids = coinGeckoIds.values.distinct().joinToString(",")
        val url = "https://api.coingecko.com/api/v3/simple/price?ids=$ids&vs_currencies=$vsCurrency"
        return try {
            val root = json.parseToJsonElement(fetch(url)).jsonObject
            root.mapValues { (_, v) ->
                (v as? JsonObject)?.get(vsCurrency)?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun tokenPrices(coinGeckoChain: String, addresses: List<String>, vsCurrency: String = "usd"): Map<String, Double> {
        if (addresses.isEmpty()) return emptyMap()
        val url = "https://api.coingecko.com/api/v3/simple/token_price/$coinGeckoChain" +
            "?contract_addresses=${addresses.joinToString(",")}&vs_currencies=$vsCurrency"
        return try {
            val root = json.parseToJsonElement(fetch(url)).jsonObject
            root.mapValues { (_, v) ->
                (v as? JsonObject)?.get(vsCurrency)?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun fetch(url: String): String {
        val response = ApiHttpClient.client.get(url)
        return response.bodyAsText()
    }
}
