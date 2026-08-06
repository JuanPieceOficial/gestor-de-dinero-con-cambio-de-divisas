package com.cryptowallet.app.data.api

import com.cryptowallet.app.data.model.Chain
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Cliente JSON-RPC 2.0 para redes EVM, con fallback entre nodos públicos.
 */
class RpcClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val requestCounter = java.util.concurrent.atomic.AtomicLong(0)

    suspend fun call(chain: Chain, method: String, params: List<JsonElement>): JsonElement {
        val errors = StringBuilder()
        for (url in chain.rpcUrls) {
            try {
                val id = requestCounter.incrementAndGet()
                val body = buildString {
                    append("{\"jsonrpc\":\"2.0\",\"id\":")
                    append(id)
                    append(",\"method\":")
                    append(JsonPrimitive(method).toString())
                    append(",\"params\":[")
                    append(params.joinToString(",") { it.toString() })
                    append("]}")
                }
                val response = ApiHttpClient.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                val text = response.body<String>()
                val root = json.parseToJsonElement(text).jsonObject
                val error = root["error"]
                if (error != null && error != JsonNull) {
                    errors.append("$url: ").append(error.toString()).append('\n')
                    continue
                }
                val result = root["result"]
                if (result != null) return result
                errors.append("$url: respuesta sin resultado\n")
            } catch (e: Exception) {
                errors.append("$url: ").append(e.message).append('\n')
            }
        }
        throw RuntimeException("Todos los nodos de ${chain.name} fallaron:\n$errors")
    }

    suspend fun chainId(chain: Chain): Long {
        val result = call(chain, "eth_chainId", emptyList()) as JsonPrimitive
        return hexToLong(result.content)
    }

    suspend fun getBalance(chain: Chain, address: String): java.math.BigInteger {
        val result = call(chain, "eth_getBalance", listOf(JsonPrimitive(address), JsonPrimitive("latest")))
        return hexToBigInt(asHex(result))
    }

    suspend fun getTransactionCount(chain: Chain, address: String): Long {
        // "pending" evita colisiones de nonce al encolar envíos consecutivos
        val result = call(chain, "eth_getTransactionCount", listOf(JsonPrimitive(address), JsonPrimitive("pending")))
        return hexToLong(asHex(result))
    }

    suspend fun callContract(chain: Chain, to: String, data: String, from: String? = null): String {
        val tx = buildJsonObject(from = from, to = to, data = data)
        val result = call(chain, "eth_call", listOf(tx, JsonPrimitive("latest")))
        return result.toString().removeSurrounding("\"")
    }

    suspend fun gasPrice(chain: Chain): java.math.BigInteger {
        val result = call(chain, "eth_gasPrice", emptyList())
        return hexToBigInt(asHex(result))
    }

    suspend fun maxPriorityFeePerGas(chain: Chain): java.math.BigInteger? {
        return try {
            val result = call(chain, "eth_maxPriorityFeePerGas", emptyList())
            hexToBigInt(asHex(result))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun latestBlock(chain: Chain): JsonObject {
        val result = call(chain, "eth_getBlockByNumber", listOf(JsonPrimitive("latest"), JsonPrimitive(false)))
        return result.jsonObject
    }

    suspend fun estimateGas(
        chain: Chain,
        from: String,
        to: String,
        value: String = "0x0",
        data: String = "0x"
    ): Long {
        val tx = buildJsonObject(from = from, to = to, value = value, data = data)
        val result = call(chain, "eth_estimateGas", listOf(tx))
        return hexToLong(asHex(result))
    }

    suspend fun sendRawTransaction(chain: Chain, rawHex: String): String {
        val result = call(chain, "eth_sendRawTransaction", listOf(JsonPrimitive(rawHex)))
        return result.toString().removeSurrounding("\"")
    }

    suspend fun getTransactionReceipt(chain: Chain, hash: String): JsonObject? {
        return try {
            val result = call(chain, "eth_getTransactionReceipt", listOf(JsonPrimitive(hash)))
            if (result == JsonNull) null else result.jsonObject
        } catch (e: Exception) {
            null
        }
    }

    private fun buildJsonObject(from: String? = null, to: String, value: String? = null, data: String = "0x"): JsonElement {
        val obj = JsonObject(
            buildMap {
                from?.let { put("from", JsonPrimitive(it)) }
                put("to", JsonPrimitive(to))
                value?.let { put("value", JsonPrimitive(it)) }
                put("data", JsonPrimitive(data))
            }
        )
        return obj
    }

    /**
     * Extrae el valor hex de un primitivo JSON (evita las comillas que
     * incluye JsonElement.toString() para strings).
     */
    private fun asHex(element: JsonElement): String =
        (element as JsonPrimitive).content

    private fun hexToLong(hex: String): Long = hexToBigInt(hex).toLong()

    private fun hexToBigInt(hex: String): java.math.BigInteger {
        val clean = hex.removePrefix("0x")
        if (clean.isEmpty()) return java.math.BigInteger.ZERO
        return java.math.BigInteger(clean, 16)
    }
}
