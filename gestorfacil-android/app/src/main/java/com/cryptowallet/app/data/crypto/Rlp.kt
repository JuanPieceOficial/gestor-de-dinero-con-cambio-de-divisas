package com.cryptowallet.app.data.crypto

import java.math.BigInteger

/**
 * Codificación RLP (Recursive Length Prefix) para transacciones Ethereum.
 */
object Rlp {

    sealed class Item {
        class Bytes(val data: ByteArray) : Item()
        class ItemList(val items: List<Item>) : Item()
    }

    fun encode(item: Item): ByteArray = when (item) {
        is Item.Bytes -> encodeString(item.data)
        is Item.ItemList -> {
            val payload = item.items.map { encode(it) }.toByteArray2()
            encodeLength(payload.size, 0xc0).plus(payload)
        }
    }

    fun encodeList(vararg items: Item): Item.ItemList = Item.ItemList(items.toList())

    fun encodeString(data: ByteArray): ByteArray = when {
        data.size == 1 && data[0].toInt() in 0x00..0x7f -> data
        data.size < 56 -> encodeLength(data.size, 0x80).plus(data)
        else -> encodeLength(data.size, 0xb7).plus(data)
    }

    fun encodeBigInteger(value: BigInteger): ByteArray {
        if (value == BigInteger.ZERO) return ByteArray(0)
        return stripLeadingZeroes(value.toByteArray())
    }

    private fun stripLeadingZeroes(bytes: ByteArray): ByteArray {
        var start = 0
        while (start < bytes.size - 1 && bytes[start].toInt() == 0) start++
        return if (start == 0) bytes else bytes.copyOfRange(start, bytes.size)
    }

    private fun encodeLength(length: Int, offset: Int): ByteArray {
        if (length < 56) {
            return byteArrayOf((offset + length).toByte())
        }
        val lengthBytes = BigInteger.valueOf(length.toLong()).toByteArray().let { stripLeadingZeroes(it) }
        val prefix = (offset + 55 + lengthBytes.size).toByte()
        return byteArrayOf(prefix) + lengthBytes
    }

    private fun List<ByteArray>.toByteArray2(): ByteArray {
        val size = sumOf { it.size }
        val out = ByteArray(size)
        var pos = 0
        for (arr in this) {
            arr.copyInto(out, pos)
            pos += arr.size
        }
        return out
    }

    private fun ByteArray.plus(other: ByteArray): ByteArray {
        val out = ByteArray(size + other.size)
        copyInto(out)
        other.copyInto(out, size)
        return out
    }
}
