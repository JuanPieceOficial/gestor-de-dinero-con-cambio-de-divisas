package com.cryptowallet.app.data.crypto

import java.math.BigInteger

/**
 * Codificación ABI (interfaces ERC-20 y eth_call).
 */
object Abi {

    fun functionSelector(signature: String): ByteArray {
        return Keccak.digest256(signature.toByteArray(Charsets.US_ASCII)).copyOfRange(0, 4)
    }

    fun encodeUint(value: BigInteger): ByteArray {
        return HdKeys.toBytes32(value)
    }

    fun encodeAddress(address: String): ByteArray {
        val clean = address.removePrefix("0x")
        require(clean.length == 40) { "Dirección inválida: $address" }
        val out = ByteArray(32)
        val bytes = clean.hexToBytes()
        bytes.copyInto(out, 12)
        return out
    }

    fun encodeUint256Address(address: String): ByteArray = encodeAddress(address)

    /**
     * data para transfer(token, amount): transfer(address,uint256)
     */
    fun encodeTransfer(toAddress: String, amountWei: BigInteger): ByteArray {
        return functionSelector("transfer(address,uint256)") +
            encodeAddress(toAddress) +
            encodeUint(amountWei)
    }

    /**
     * data para balanceOf(address)
     */
    fun encodeBalanceOf(address: String): ByteArray {
        return functionSelector("balanceOf(address)") + encodeAddress(address)
    }

    fun encodeNoArgs(signature: String): ByteArray = functionSelector(signature)

    fun decodeUintFromHex(hex: String): BigInteger {
        val bytes = hex.removePrefix("0x").hexToBytes()
        return decodeUint(bytes)
    }

    fun decodeUint(data: ByteArray): BigInteger {
        val start = if (data.size <= 32) 0 else data.size - 32
        return BigInteger(1, data.copyOfRange(start, data.size))
    }

    /**
     * Decodifica un string dinámico (symbol/name) de una respuesta eth_call.
     */
    fun decodeString(data: ByteArray): String {
        if (data.size < 64) return ""
        val offset = decodeUint(data.copyOfRange(0, 32)).toInt()
        if (offset + 32 > data.size) return ""
        val length = decodeUint(data.copyOfRange(offset, offset + 32)).toInt()
        if (offset + 32 + length > data.size) return ""
        return String(data.copyOfRange(offset + 32, offset + 32 + length), Charsets.UTF_8)
    }

    fun decodeStringFromHex(hex: String): String {
        return decodeString(hex.removePrefix("0x").hexToBytes())
    }

    fun toHex(data: ByteArray): String {
        val sb = StringBuilder(data.size * 2)
        for (b in data) sb.append("%02x".format(b))
        return sb.toString()
    }

    private fun String.hexToBytes(): ByteArray {
        val clean = if (startsWith("0x")) substring(2) else this
        if (clean.length % 2 != 0) throw IllegalArgumentException("Hex de longitud impar")
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }
}
