package com.cryptowallet.app.data.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class RlpTest {

    private fun bytes(hex: String) = TestUtil.hexToBytes(hex)

    @Test
    fun encodeString_dog() {
        assertArrayEquals(bytes("83646f67"), Rlp.encode(Rlp.Item.Bytes("dog".toByteArray())))
    }

    @Test
    fun encodeString_empty() {
        assertArrayEquals(bytes("80"), Rlp.encode(Rlp.Item.Bytes(ByteArray(0))))
    }

    @Test
    fun encodeString_singleByteInRange() {
        // 0x00..0x7f se codifica como el propio byte
        assertArrayEquals(bytes("00"), Rlp.encode(Rlp.Item.Bytes(bytes("00"))))
        assertArrayEquals(bytes("7f"), Rlp.encode(Rlp.Item.Bytes(bytes("7f"))))
    }

    @Test
    fun encodeList_catDog() {
        val list = Rlp.Item.ItemList(
            listOf(
                Rlp.Item.Bytes("cat".toByteArray()),
                Rlp.Item.Bytes("dog".toByteArray())
            )
        )
        assertArrayEquals(bytes("c88363617483646f67"), Rlp.encode(list))
    }

    @Test
    fun encodeBigInteger_zero_isEmpty() {
        assertArrayEquals(ByteArray(0), Rlp.encodeBigInteger(BigInteger.ZERO))
    }

    @Test
    fun encodeBigInteger_1024() {
        // Bytes mínimos sin prefijo RLP (el prefijo lo añade encode(Item.Bytes(...)))
        assertArrayEquals(bytes("0400"), Rlp.encodeBigInteger(BigInteger.valueOf(1024)))
    }

    @Test
    fun encodeBigInteger_1024_fullRlp() {
        // Codificación RLP completa de un entero (como string corto 0x82 + 2 bytes)
        val item = Rlp.Item.Bytes(Rlp.encodeBigInteger(BigInteger.valueOf(1024)))
        assertArrayEquals(bytes("820400"), Rlp.encode(item))
    }
}
