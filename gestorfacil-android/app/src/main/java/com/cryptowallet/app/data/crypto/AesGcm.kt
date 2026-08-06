package com.cryptowallet.app.data.crypto

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifrado AES-256-GCM con claves en memoria (por ejemplo, la clave derivada del PIN).
 */
object AesGcm {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128

    fun encrypt(key: ByteArray, plainText: String): String {
        require(key.size == 32) { "La clave debe ser AES-256 (32 bytes)" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv) + ":" +
            Base64.getEncoder().encodeToString(encrypted)
    }

    fun decrypt(key: ByteArray, encryptedText: String): String {
        require(key.size == 32) { "La clave debe ser AES-256 (32 bytes)" }
        val parts = encryptedText.split(":")
        check(parts.size == 2) { "Texto cifrado inválido" }
        val iv = Base64.getDecoder().decode(parts[0])
        val encrypted = Base64.getDecoder().decode(parts[1])
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
