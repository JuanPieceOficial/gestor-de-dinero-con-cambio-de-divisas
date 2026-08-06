package com.cryptowallet.app.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AesGcmTest {

    private val key = TestUtil.hexToBytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")

    @Test
    fun encryptDecrypt_roundtrip() {
        val plain = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val cipher = AesGcm.encrypt(key, plain)
        assertEquals(plain, AesGcm.decrypt(key, cipher))
    }

    @Test
    fun samePlaintextDifferentIv() {
        val a = AesGcm.encrypt(key, "hola")
        val b = AesGcm.encrypt(key, "hola")
        assertNotEquals(a, b) // IV aleatorio
    }

    @Test
    fun wrongKeyFails() {
        val cipher = AesGcm.encrypt(key, "secreto")
        val wrongKey = TestUtil.hexToBytes("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
        assertThrows(javax.crypto.AEADBadTagException::class.java) {
            AesGcm.decrypt(wrongKey, cipher)
        }
    }
}
