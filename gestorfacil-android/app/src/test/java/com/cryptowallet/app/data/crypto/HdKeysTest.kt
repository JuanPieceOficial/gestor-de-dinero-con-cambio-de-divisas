package com.cryptowallet.app.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class HdKeysTest {

    private val seed = TestUtil.hexToBytes("000102030405060708090a0b0c0d0e0f")

    @Test
    fun masterFromKnownSeed_matchesBip32Vector1() {
        val master = HdKeys.masterFromSeed(seed)
        assertEquals(
            "e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35",
            TestUtil.toHex(master.privateKeyBytes)
        )
        assertEquals(
            "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508",
            TestUtil.toHex(master.chainCode)
        )
    }

    @Test
    fun hardenedChild_matchesBip32Vector() {
        val master = HdKeys.masterFromSeed(seed)
        val child = HdKeys.deriveChild(master, 0x80000000L) // m/0'
        assertEquals(
            "edb2e14f9ee77d26dd93b4ecede8d16ed408ce149b6cd80b0715a2d911a0afea",
            TestUtil.toHex(child.privateKeyBytes)
        )
    }

    @Test
    fun nonHardenedChild_matchesBip32Vector() {
        val master = HdKeys.masterFromSeed(seed)
        val hardened0 = HdKeys.deriveChild(master, 0x80000000L) // m/0'
        val child1 = HdKeys.deriveChild(hardened0, 1L)          // m/0'/1
        assertEquals(
            "3c6cb8d0f6a264c91ea8b5030fadaa8e538b020f0a387421a12de9319dc93368",
            TestUtil.toHex(child1.privateKeyBytes)
        )
    }

    @Test
    fun evmAccount_usesStandardBip44Path() {
        // Frase BIP-39 conocida; la dirección m/44'/60'/0'/0/0 es un vector estándar.
        val seed2 = Bip39.mnemonicToSeed(
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        )
        val account = HdKeys.evmAccount(seed2, 0)
        val pair = EthCrypto.keyPairFromPrivateBytes(account.privateKeyBytes)
        assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", pair.checksumAddress)
    }
}
