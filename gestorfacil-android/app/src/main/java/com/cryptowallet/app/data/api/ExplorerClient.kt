package com.cryptowallet.app.data.api

import com.cryptowallet.app.data.db.TxRecordEntity
import com.cryptowallet.app.data.model.Chain
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Historial on-chain vía Blockscout API v2 (público, sin API key).
 * Endpoints: /api/v2/addresses/{address}/transactions y .../token-transfers
 */
class ExplorerClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val hosts: Map<Long, String> = mapOf(
        1L to "https://eth.blockscout.com",
        56L to "https://bsc.blockscout.com",
        137L to "https://polygon.blockscout.com",
        42161L to "https://arbitrum.blockscout.com",
        10L to "https://optimism.blockscout.com",
        8453L to "https://base.blockscout.com",
        43114L to "https://avalanche.blockscout.com",
        11155111L to "https://eth-sepolia.blockscout.com",
        97L to "https://bsc-testnet.blockscout.com",
        80002L to "https://amoy-testnet.blockscout.com"
    )

    suspend fun fetchHistory(chain: Chain, address: String): List<TxRecordEntity> {
        val host = hosts[chain.id] ?: return emptyList()
        val records = mutableListOf<TxRecordEntity>()
        try {
            records += fetchNativeTxs(host, chain, address)
        } catch (e: Exception) {
            // cadena sin explorador disponible: se omite
        }
        try {
            records += fetchTokenTransfers(host, chain, address)
        } catch (e: Exception) {
            // igual
        }
        return records
    }

    private suspend fun fetchNativeTxs(host: String, chain: Chain, address: String): List<TxRecordEntity> {
        val url = "$host/api/v2/addresses/$address/transactions"
        val root = parseItems(url) ?: return emptyList()
        return root.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val hash = obj["hash"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val from = obj["from"]?.jsonObject?.get("hash")?.jsonPrimitive?.contentOrNull ?: ""
            val to = obj["to"]?.jsonObject?.get("hash")?.jsonPrimitive?.contentOrNull ?: ""
            val valueWei = obj["value"]?.jsonPrimitive?.contentOrNull ?: "0"
            val feeWei = obj["fee"]?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull ?: ""
            val status = when (obj["status"]?.jsonPrimitive?.contentOrNull) {
                "ok" -> "success"
                "error" -> "failed"
                else -> "success"
            }
            val timestamp = parseTimestamp(obj["timestamp"]?.jsonPrimitive?.contentOrNull)
            val isSend = from.equals(address, ignoreCase = true)
            TxRecordEntity(
                id = "${chain.id}:$hash",
                chainId = chain.id,
                hash = hash,
                from = from,
                to = to,
                tokenAddress = null,
                tokenSymbol = chain.nativeSymbol,
                amount = formatAmount(valueWei, chain.nativeDecimals),
                amountRaw = valueWei,
                feeWei = feeWei,
                status = status,
                timestamp = timestamp,
                type = if (isSend) "send" else "receive"
            )
        }
    }

    private suspend fun fetchTokenTransfers(host: String, chain: Chain, address: String): List<TxRecordEntity> {
        val url = "$host/api/v2/addresses/$address/token-transfers"
        val root = parseItems(url) ?: return emptyList()
        return root.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val hash = obj["transaction_hash"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val from = obj["from"]?.jsonObject?.get("hash")?.jsonPrimitive?.contentOrNull ?: ""
            val to = obj["to"]?.jsonObject?.get("hash")?.jsonPrimitive?.contentOrNull ?: ""
            val token = obj["token"]?.jsonObject
            val symbol = token?.get("symbol")?.jsonPrimitive?.contentOrNull ?: "TOKEN"
            val decimals = token?.get("decimals")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 18
            val total = obj["total"]?.jsonObject
            val rawValue = total?.get("value")?.jsonPrimitive?.contentOrNull ?: "0"
            val tokenAddress = token?.get("address")?.jsonPrimitive?.contentOrNull
            val timestamp = parseTimestamp(obj["timestamp"]?.jsonPrimitive?.contentOrNull)
            val isSend = from.equals(address, ignoreCase = true)
            TxRecordEntity(
                id = "${chain.id}:$hash:$tokenAddress",
                chainId = chain.id,
                hash = hash,
                from = from,
                to = to,
                tokenAddress = tokenAddress,
                tokenSymbol = symbol,
                amount = formatAmount(rawValue, decimals),
                amountRaw = rawValue,
                feeWei = "",
                status = "success",
                timestamp = timestamp,
                type = if (isSend) "send" else "receive"
            )
        }
    }

    private suspend fun parseItems(url: String): JsonArray? {
        val response = ApiHttpClient.client.get(url)
        if (!response.status.isSuccess()) return null
        val body = response.bodyAsText()
        if (body.isBlank()) return null
        return try {
            (json.parseToJsonElement(body) as JsonObject)["items"] as? JsonArray
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTimestamp(raw: String?): Long {
        if (raw == null) return 0L
        return try {
            java.time.Instant.parse(raw).toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }

    private fun formatAmount(raw: String, decimals: Int): String {
        return try {
            java.math.BigDecimal(raw).movePointLeft(decimals).stripTrailingZeros().toPlainString()
        } catch (e: Exception) {
            "0"
        }
    }
}
