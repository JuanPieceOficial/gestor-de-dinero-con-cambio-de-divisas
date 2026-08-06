package com.cryptowallet.app.data.crypto

import org.bouncycastle.crypto.digests.KeccakDigest

/**
 * Keccak-256 (el hash usado por Ethereum, NO el SHA3-256 estándar).
 */
object Keccak {

    fun digest256(data: ByteArray): ByteArray {
        val digest = KeccakDigest(256)
        digest.update(data, 0, data.size)
        val out = ByteArray(32)
        digest.doFinal(out, 0)
        return out
    }

    fun digest256(vararg parts: ByteArray): ByteArray {
        val digest = KeccakDigest(256)
        for (part in parts) digest.update(part, 0, part.size)
        val out = ByteArray(32)
        digest.doFinal(out, 0)
        return out
    }
}
