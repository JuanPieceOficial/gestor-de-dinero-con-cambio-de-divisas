package com.cryptowallet.app.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class EthCryptoTest {

    @Test
    fun keyPairFromPrivateKeyOne_derivesKnownAddress() {
        val pair = EthCrypto.keyPairFromPrivateKey(BigInteger.ONE)
        assertEquals("0x7E5F4552091A69125d5DfCb7b8C2659029395Bdf", pair.checksumAddress)
        assertEquals(65, pair.publicKey.size)
        assertEquals(0x04, pair.publicKey[0].toInt() and 0xff)
    }

    @Test
    fun keccak_matchesKnownVectors() {
        assertEquals(
            "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470",
            TestUtil.toHex(Keccak.digest256(ByteArray(0)))
        )
        assertEquals(
            "4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45",
            TestUtil.toHex(Keccak.digest256("abc".toByteArray(Charsets.US_ASCII)))
        )
    }

    @Test
    fun checksumAddress_matchesEip55Vector() {
        val lower = "0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed"
        assertEquals("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed", EthCrypto.toChecksumAddress(lower))
        assertTrue(EthCrypto.isChecksumAddress("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"))
        assertFalse(EthCrypto.isChecksumAddress(lower))
    }

    @Test
    fun signDigest_recoversCorrectRecidAndKey() {
        // Test de regresión: findRecid comparaba clave comprimida contra sin comprimir.
        val pair = EthCrypto.keyPairFromPrivateKey(BigInteger.ONE)
        val digest = TestUtil.hexToBytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")

        val signature = EthCrypto.signDigest(BigInteger.ONE, digest)

        val recovered = EthCrypto.recoverPublicKey(digest, signature.r, signature.s, signature.recid)
        assertNotNull("No se recuperó la clave pública", recovered)

        // Comprimir la clave pública original y comparar con la recuperada
        val expected = ByteArray(33)
        val y = BigInteger(1, pair.publicKey.copyOfRange(33, 65))
        expected[0] = if (y.testBit(0)) 0x03 else 0x02
        pair.publicKey.copyOfRange(1, 33).copyInto(expected, 1)
        assertEquals(TestUtil.toHex(expected), TestUtil.toHex(recovered!!))
    }

    @Test
    fun isValidAddress_acceptsOnlyValidHex() {
        assertTrue(EthCrypto.isValidAddress("0x9858EfFD232B4033E47d90003D41EC34EcaEda94"))
        assertFalse(EthCrypto.isValidAddress("0x9858"))
        assertFalse(EthCrypto.isValidAddress("9858EfFD232B4033E47d90003D41EC34EcaEda94"))
    }
}
