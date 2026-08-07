package com.cryptowallet.app.data.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra datos con AES-256-GCM usando una clave no exportable guardada en el
 * Android Keystore (soporte hardware en la mayoría de dispositivos).
 *
 * Dos claves:
 *  - Clave maestra: sin autenticación (legado y migraciones).
 *  - Clave biométrica: requiere autenticación biométrica en cada uso, se
 *    invalida si el usuario da de alta una huella nueva.
 */
class KeyStoreManager {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encryptedText: String): String {
        val parts = encryptedText.split(":")
        check(parts.size == 2) { "Texto cifrado inválido" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    // ---------- Clave biométrica ----------

    fun isBiometricKeyAvailable(): Boolean = keyStore.containsAlias(BIOMETRIC_ALIAS)

    /**
     * Cipher en modo cifrado con la clave biométrica. Se debe pasar como
     * CryptoObject al BiometricPrompt; tras autenticarse, úsalo con
     * [encryptWithCipher].
     */
    fun createBiometricEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateBiometricKey())
        return cipher
    }

    /**
     * Cipher en modo descifrado con la clave biométrica y el IV del blob
     * almacenado. Pásalo al BiometricPrompt y luego usa [decryptWithCipher].
     */
    fun createBiometricDecryptCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateBiometricKey(), GCMParameterSpec(128, iv))
        return cipher
    }

    fun encryptWithCipher(cipher: Cipher, plainText: String): String {
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decryptWithCipher(cipher: Cipher, encryptedText: String): String {
        val parts = encryptedText.split(":")
        check(parts.size == 2) { "Texto cifrado inválido" }
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun deleteBiometricKey() {
        if (keyStore.containsAlias(BIOMETRIC_ALIAS)) {
            keyStore.deleteEntry(BIOMETRIC_ALIAS)
        }
    }

    private fun getOrCreateBiometricKey(): SecretKey {
        val existing = keyStore.getKey(BIOMETRIC_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                BIOMETRIC_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        return generator.generateKey()
    }

    // ---------- Clave maestra ----------

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "cryptowallet_master_key"
        const val BIOMETRIC_ALIAS = "cryptowallet_biometric_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
