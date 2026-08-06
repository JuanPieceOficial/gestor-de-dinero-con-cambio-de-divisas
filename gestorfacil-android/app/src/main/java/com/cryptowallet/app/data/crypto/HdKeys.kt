package com.cryptowallet.app.data.crypto

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter
import java.math.BigInteger

/**
 * BIP-32 (HD wallets) sobre la curva secp256k1.
 */
object HdKeys {

    data class ExtendedKey(val key: BigInteger, val chainCode: ByteArray) {
        val privateKeyBytes: ByteArray
            get() = toBytes32(key)
    }

    private val params = SECNamedCurves.getByName("secp256k1")
    private val curve = params.curve
    private val g = params.g
    private val n = params.n
    private val HARDENED = 0x80000000.toLong()

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val mac = HMac(SHA512Digest())
        mac.init(KeyParameter(key))
        mac.update(data, 0, data.size)
        val out = ByteArray(64)
        mac.doFinal(out, 0)
        return out
    }

    fun masterFromSeed(seed: ByteArray): ExtendedKey {
        val i = hmacSha512("Bitcoin seed".toByteArray(Charsets.UTF_8), seed)
        val key = BigInteger(1, i.copyOfRange(0, 32))
        require(key < n && key.signum() > 0) { "Clave maestra inválida" }
        return ExtendedKey(key, i.copyOfRange(32, 64))
    }

    fun deriveChild(parent: ExtendedKey, index: Long): ExtendedKey {
        require(index >= 0 && index <= (HARDENED + 0x7FFFFFFF)) { "Índice BIP-32 fuera de rango" }
        val hardened = index and HARDENED != 0L
        val data = if (hardened) {
            ByteArray(1 + 32 + 4)
        } else {
            ByteArray(33 + 4)
        }
        if (hardened) {
            data[0] = 0
            parent.privateKeyBytes.copyInto(data, 1)
            writeUInt32BE(index.toInt(), data, 33)
        } else {
            val point = g.multiply(parent.key).normalize()
            val x = point.affineXCoord.toBigInteger()
            data[0] = if (point.affineYCoord.toBigInteger().testBit(0)) 0x03 else 0x02
            val xBytes = toBytes32(x)
            xBytes.copyInto(data, 1)
            writeUInt32BE(index.toInt(), data, 33)
        }
        val i = hmacSha512(parent.chainCode, data)
        val il = BigInteger(1, i.copyOfRange(0, 32))
        val childKey = il.add(parent.key).mod(n)
        require(childKey.signum() != 0) { "Derivación inválida (clave cero)" }
        return ExtendedKey(childKey, i.copyOfRange(32, 64))
    }

    fun derivePath(seed: ByteArray, path: List<Int>): ExtendedKey {
        var key = masterFromSeed(seed)
        for (index in path) {
            key = deriveChild(key, if (index < 0) (HARDENED + (-index.toLong())) else index.toLong())
        }
        return key
    }

    /**
     * Ruta EVM estándar BIP-44: m/44'/60'/0'/0/accountIndex.
     * Nota: el nivel de cuenta (0') debe ser hardened; se deriva manualmente
     * porque la convención de índices negativos no permite expresar -0.
     */
    fun evmAccount(seed: ByteArray, accountIndex: Int): ExtendedKey {
        var key = masterFromSeed(seed)
        key = deriveChild(key, HARDENED + 44)
        key = deriveChild(key, HARDENED + 60)
        key = deriveChild(key, HARDENED) // 0' (cuenta)
        key = deriveChild(key, 0)        // cadena externa
        key = deriveChild(key, accountIndex.toLong())
        return key
    }

    fun toBytes32(value: BigInteger): ByteArray {
        val out = ByteArray(32)
        val raw = value.toByteArray()
        val src = if (raw.size > 32) raw.copyOfRange(raw.size - 32, raw.size) else raw
        src.copyInto(out, 32 - src.size)
        return out
    }

    private fun writeUInt32BE(value: Int, dst: ByteArray, offset: Int) {
        dst[offset] = (value ushr 24).toByte()
        dst[offset + 1] = (value ushr 16).toByte()
        dst[offset + 2] = (value ushr 8).toByte()
        dst[offset + 3] = value.toByte()
    }
}
