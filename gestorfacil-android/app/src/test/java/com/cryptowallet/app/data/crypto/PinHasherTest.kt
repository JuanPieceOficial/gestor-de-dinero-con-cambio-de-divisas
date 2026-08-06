package com.cryptowallet.app.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun hashIsDeterministic() {
        val salt = PinHasher.newSalt()
        val a = PinHasher.hash("123456", salt)
        val b = PinHasher.hash("123456", salt)
        assertTrue(a.contentEquals(b))
        assertFalse(a.contentEquals(PinHasher.hash("654321", salt)))
    }

    @Test
    fun hashDiffersWithDifferentSalt() {
        val a = PinHasher.hash("123456", PinHasher.newSalt())
        val b = PinHasher.hash("123456", PinHasher.newSalt())
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun deriveKey_is32BytesAndDeterministic() {
        val salt = PinHasher.newSalt()
        val key = PinHasher.deriveKey("123456", salt)
        assertEquals(32, key.size)
        assertTrue(key.contentEquals(PinHasher.deriveKey("123456", salt)))
        assertFalse(key.contentEquals(PinHasher.deriveKey("123457", salt)))
    }

    @Test
    fun base64Roundtrip() {
        val salt = PinHasher.newSalt()
        assertEquals(
            TestUtil.toHex(salt),
            TestUtil.toHex(PinHasher.decodeSalt(PinHasher.encodeSalt(salt)))
        )
    }
}
