package com.cryptowallet.app.data.crypto

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * BIP-39: generación, validación y derivación de seeds desde frases mnemotécnicas.
 */
object Bip39 {

    private const val PBKDF2_ITERATIONS = 2048
    private const val PBKDF2_KEY_BITS = 512

    @Volatile
    private var cachedWords: List<String>? = null

    fun wordList(context: Context): List<String> {
        cachedWords?.let { return it }
        val words = context.assets.open("bip39_english.txt")
            .bufferedReader(Charsets.UTF_8)
            .useLines { lines -> lines.map { it.trim() }.filter { it.isNotEmpty() }.toList() }
        check(words.size == 2048) { "Wordlist BIP-39 inválida: ${words.size}" }
        cachedWords = words
        return words
    }

    fun generateMnemonic(context: Context, entropyBytes: Int = 16): String {
        require(entropyBytes == 16 || entropyBytes == 32) { "Entropía debe ser 16 (12 palabras) o 32 (24 palabras)" }
        val entropy = ByteArray(entropyBytes)
        SecureRandom().nextBytes(entropy)
        return entropyToMnemonic(context, entropy)
    }

    fun entropyToMnemonic(context: Context, entropy: ByteArray): String {
        val words = wordList(context)
        val checksumBits = entropy.size * 8 / 32
        val hash = MessageDigest.getInstance("SHA-256").digest(entropy)

        val totalBits = entropy.size * 8 + checksumBits
        val indices = ArrayList<Int>(totalBits / 11)
        var bitIndex = 0
        var current = 0
        var bitsInCurrent = 0
        while (bitIndex < totalBits) {
            val sourceByte = if (bitIndex < entropy.size * 8) {
                entropy[bitIndex / 8]
            } else {
                hash[(bitIndex - entropy.size * 8) / 8]
            }
            val shift = 7 - (bitIndex % 8)
            current = (current shl 1) or ((sourceByte.toInt() ushr shift) and 1)
            bitsInCurrent++
            if (bitsInCurrent == 11) {
                indices.add(current)
                current = 0
                bitsInCurrent = 0
            }
            bitIndex++
        }
        return indices.joinToString(" ") { words[it] }
    }

    fun validateMnemonic(context: Context, phrase: String): Boolean {
        val words = wordList(context)
        val parts = phrase.trim().lowercase().split(Regex("\\s+"))
        if (parts.size !in listOf(12, 15, 18, 21, 24)) return false
        val indices = parts.map { words.indexOf(it) }
        if (indices.any { it < 0 }) return false

        val entropyBits = parts.size * 11 / 33 * 32
        val checksumBits = parts.size * 11 - entropyBits
        val bytes = ByteArray(entropyBits / 8)
        var bitIndex = 0
        for (bytePos in bytes.indices) {
            var byte = 0
            for (b in 0 until 8) {
                val wordIndex = indices[(bitIndex + b) / 11]
                val bitInWord = (bitIndex + b) % 11
                byte = (byte shl 1) or ((wordIndex ushr (10 - bitInWord)) and 1)
            }
            bytes[bytePos] = byte.toByte()
            bitIndex += 8
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        var expectedChecksum = 0
        for (b in 0 until checksumBits) {
            val bit = (hash[b / 8].toInt() ushr (7 - (b % 8))) and 1
            expectedChecksum = (expectedChecksum shl 1) or bit
        }
        var actualChecksum = 0
        for (b in 0 until checksumBits) {
            val wordIndex = indices[(entropyBits + b) / 11]
            val bitInWord = (entropyBits + b) % 11
            val bit = (wordIndex ushr (10 - bitInWord)) and 1
            actualChecksum = (actualChecksum shl 1) or bit
        }
        return expectedChecksum == actualChecksum
    }

    fun mnemonicToSeed(phrase: String, passphrase: String = ""): ByteArray {
        val normalized = phrase.trim().lowercase()
        val salt = "mnemonic$passphrase"
        val spec = PBEKeySpec(normalized.toCharArray(), salt.toByteArray(Charsets.UTF_8), PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        return factory.generateSecret(spec).encoded
    }
}
