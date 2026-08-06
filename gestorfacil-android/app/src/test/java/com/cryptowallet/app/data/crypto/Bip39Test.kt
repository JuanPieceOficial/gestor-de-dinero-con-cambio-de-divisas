package com.cryptowallet.app.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class Bip39Test {

    private val mnemonic =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

    @Test
    fun mnemonicToSeed_matchesBip39VectorWithoutPassphrase() {
        val seed = Bip39.mnemonicToSeed(mnemonic, "")
        assertEquals(
            "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a" +
                "5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4",
            TestUtil.toHex(seed)
        )
    }

    @Test
    fun mnemonicToSeed_withPassphrase_differs() {
        val seed = Bip39.mnemonicToSeed(mnemonic, "TREZOR")
        val seedPlain = Bip39.mnemonicToSeed(mnemonic, "")
        assertEquals(64, seed.size)
        assertEquals(64, seedPlain.size)
        // Distintas passphrases -> distintos seeds
        org.junit.Assert.assertFalse(seed.contentEquals(seedPlain))
    }
}
