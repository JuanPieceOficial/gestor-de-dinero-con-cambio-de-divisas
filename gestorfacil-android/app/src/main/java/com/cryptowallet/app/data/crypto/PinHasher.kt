package com.cryptowallet.app.data.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash seguro de PIN con PBKDF2-HMAC-SHA256 y derivación de claves de cifrado.
 */
object PinHasher {

    private const val ITERATIONS = 100_000
    private const val KEY_BITS = 256

    /** Iteraciones para derivar la clave de cifrado del seed (más costosas que el hash). */
    const val DERIVE_ITERATIONS = 250_000

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /**
     * Deriva una clave AES-256 a partir del PIN. Se usa para cifrar el seed:
     * sin el PIN es imposible descifrarlo, incluso con acceso al almacenamiento.
     */
    fun deriveKey(pin: String, salt: ByteArray, iterations: Int = DERIVE_ITERATIONS): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    fun newSalt(): ByteArray {
        return ByteArray(16).also { SecureRandom().nextBytes(it) }
    }

    fun encodeSalt(salt: ByteArray): String = Base64.getEncoder().encodeToString(salt)

    fun decodeSalt(encoded: String): ByteArray = Base64.getDecoder().decode(encoded)

    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun decode(encoded: String): ByteArray = Base64.getDecoder().decode(encoded)
}
