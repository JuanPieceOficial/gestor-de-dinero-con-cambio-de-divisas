package com.cryptowallet.app.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class AbiTest {

    @Test
    fun functionSelector_transfer() {
        val selector = Abi.functionSelector("transfer(address,uint256)")
        assertEquals("a9059cbb", TestUtil.toHex(selector))
    }

    @Test
    fun functionSelector_balanceOf() {
        val selector = Abi.functionSelector("balanceOf(address)")
        assertEquals("70a08231", TestUtil.toHex(selector))
    }

    @Test
    fun encodeTransfer_builds68Bytes() {
        val data = Abi.encodeTransfer(
            "0x0000000000000000000000000000000000000001",
            BigInteger.ONE
        )
        assertEquals(68, data.size)
        assertEquals("a9059cbb", TestUtil.toHex(data.copyOfRange(0, 4)))
        // address padding: 12 bytes de ceros + 0x...01 al final
        assertEquals("01", TestUtil.toHex(data.copyOfRange(67, 68)))
    }

    @Test
    fun decodeUintFromHex() {
        val hex = "0x" + "ff".repeat(32)
        assertEquals(BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16), Abi.decodeUintFromHex(hex))
    }

    @Test
    fun decodeStringFromHex_symbol() {
        // Respuesta típica de symbol(): offset(0x20) + length(3) + "ETH" + padding
        val offset = "0".repeat(62) + "20"
        val length = "0".repeat(62) + "03"
        val value = "455448" + "0".repeat(58)
        val result = Abi.decodeStringFromHex("0x$offset$length$value")
        assertEquals("ETH", result)
    }

    @Test
    fun encodeAddress_padsTo32Bytes() {
        val encoded = Abi.encodeAddress("0x0000000000000000000000000000000000000001")
        assertEquals(32, encoded.size)
        assertEquals(0, encoded[0].toInt())
        assertEquals(1, encoded[31].toInt())
        assertTrue(encoded.copyOfRange(0, 12).all { it == 0.toByte() })
    }
}
