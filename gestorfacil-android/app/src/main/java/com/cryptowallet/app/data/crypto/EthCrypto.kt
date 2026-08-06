package com.cryptowallet.app.data.crypto

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import java.math.BigInteger

/**
 * Criptografía Ethereum: claves secp256k1, direcciones, EIP-55 y firma ECDSA (RFC6979).
 */
object EthCrypto {

    data class Signature(val r: BigInteger, val s: BigInteger, val recid: Int) {
        val yParity: Int get() = recid and 1
        fun v(chainId: Long): Long = chainId * 2 + 35 + recid
    }

    data class KeyPair(val privateKey: BigInteger, val publicKey: ByteArray) {
        val address: String by lazy { addressFromPublicKey(publicKey) }
        val checksumAddress: String by lazy { toChecksumAddress(address) }
    }

    private val params = SECNamedCurves.getByName("secp256k1")
    private val curve = params.curve
    private val g = params.g
    private val n = params.n
    private val domain = ECDomainParameters(curve, g, n, params.h)

    fun keyPairFromPrivateKey(privateKey: BigInteger): KeyPair {
        require(privateKey > BigInteger.ZERO && privateKey < n) { "Clave privada fuera de rango" }
        val point = g.multiply(privateKey).normalize()
        val x = point.affineXCoord.toBigInteger()
        val y = point.affineYCoord.toBigInteger()
        val publicKey = ByteArray(65)
        publicKey[0] = 0x04
        val xBytes = HdKeys.toBytes32(x)
        val yBytes = HdKeys.toBytes32(y)
        xBytes.copyInto(publicKey, 1)
        yBytes.copyInto(publicKey, 33)
        return KeyPair(privateKey, publicKey)
    }

    fun keyPairFromPrivateBytes(privateKey: ByteArray): KeyPair {
        return keyPairFromPrivateKey(BigInteger(1, privateKey))
    }

    fun addressFromPublicKey(publicKey: ByteArray): String {
        val hash = Keccak.digest256(publicKey.copyOfRange(1, publicKey.size))
        val hex = hash.copyOfRange(12, 32).toHex()
        return "0x$hex"
    }

    fun toChecksumAddress(address: String): String {
        val clean = address.removePrefix("0x").lowercase()
        val hash = Keccak.digest256(clean.toByteArray(Charsets.US_ASCII)).toHex()
        val sb = StringBuilder(clean.length)
        for (i in clean.indices) {
            val c = clean[i]
            val nibble = Character.digit(hash[i], 16)
            sb.append(if (c in 'a'..'f' && nibble >= 8) c.uppercaseChar() else c)
        }
        return "0x$sb"
    }

    fun isChecksumAddress(address: String): Boolean {
        return toChecksumAddress(address) == address
    }

    /**
     * Firma determinista (RFC6979/SHA-256) de un digest de 32 bytes.
     * Normaliza la firma a low-s y recupera el recovery id correcto.
     */
    fun signDigest(privateKey: BigInteger, digest: ByteArray): Signature {
        require(digest.size == 32) { "El digest debe tener 32 bytes" }
        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        signer.init(true, ECPrivateKeyParameters(privateKey, domain))
        val signature = signer.generateSignature(digest)
        var r = signature[0]
        var s = signature[1]
        require(r.signum() != 0 && s.signum() != 0) { "Firma inválida" }
        val halfN = n.shiftRight(1)
        if (s > halfN) {
            s = n.subtract(s)
        }
        val publicKey = keyPairFromPrivateKey(privateKey).publicKey
        val recid = findRecid(digest, r, s, publicKey)
            ?: error("No se pudo recuperar el recovery id")
        return Signature(r, s, recid)
    }

    private fun findRecid(digest: ByteArray, r: BigInteger, s: BigInteger, publicKey: ByteArray): Int? {
        // recoverPublicKey devuelve la clave comprimida (33 bytes); la clave original
        // es sin comprimir (65 bytes). Hay que comparar en el mismo formato.
        val expected = compressPublicKey(publicKey).toHex()
        for (recid in 0 until 4) {
            val recovered = recoverPublicKey(digest, r, s, recid)
            if (recovered != null && recovered.toHex() == expected) {
                return recid
            }
        }
        return null
    }

    /**
     * Convierte una clave pública sin comprimir (65 bytes, prefijo 0x04) a su
     * forma comprimida (33 bytes, prefijo 0x02/0x03).
     */
    private fun compressPublicKey(publicKey: ByteArray): ByteArray {
        check(publicKey.size == 65 && publicKey[0] == 0x04.toByte()) { "Clave pública sin comprimir inválida" }
        val x = BigInteger(1, publicKey.copyOfRange(1, 33))
        val y = BigInteger(1, publicKey.copyOfRange(33, 65))
        val out = ByteArray(33)
        out[0] = if (y.testBit(0)) 0x03 else 0x02
        HdKeys.toBytes32(x).copyInto(out, 1)
        return out
    }

    /**
     * Recupera la clave pública comprimida (33 bytes) a partir de la firma.
     */
    fun recoverPublicKey(digest: ByteArray, r: BigInteger, s: BigInteger, recid: Int): ByteArray? {
        if (r.signum() <= 0 || s.signum() <= 0 || r >= n || s >= n) return null
        val x = if (recid and 2 == 0) r else r.add(n)
        if (x >= curve.field.characteristic) return null
        val rPoint = try {
            decompressPoint(recid and 1, x)
        } catch (e: Exception) {
            return null
        }
        if (rPoint.multiply(n).isInfinity.not()) return null

        val e = BigInteger(1, digest)
        val rInv = r.modInverse(n)
        val u1 = e.negate().multiply(rInv).mod(n)
        val u2 = s.multiply(rInv).mod(n)
        val q = g.multiply(u1).add(rPoint.multiply(u2)).normalize()
        if (q.isInfinity) return null
        val xb = HdKeys.toBytes32(q.affineXCoord.toBigInteger())
        val out = ByteArray(33)
        out[0] = if (q.affineYCoord.toBigInteger().testBit(0)) 0x03 else 0x02
        xb.copyInto(out, 1)
        return out
    }

    fun isValidAddress(address: String): Boolean {
        return address.matches(Regex("^0x[a-fA-F0-9]{40}$"))
    }

    private fun decompressPoint(yTilde: Int, x: BigInteger): org.bouncycastle.math.ec.ECPoint {
        val p = curve.field.characteristic
        val ySquared = x.modPow(BigInteger.valueOf(3), p).add(BigInteger.valueOf(7)).mod(p)
        var y = ySquared.modPow(p.add(BigInteger.ONE).shiftRight(2), p)
        if (y.multiply(y).mod(p) != ySquared) {
            error("Punto no válido")
        }
        if (y.testBit(0) != (yTilde == 1)) {
            y = p.subtract(y)
        }
        return curve.createPoint(x, y)
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) sb.append("%02x".format(b))
        return sb.toString()
    }
}
