package com.example.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Production-grade cryptographic operations for VaultX.
 * Uses AES-256-GCM authenticated encryption and PBKDF2WithHmacSHA256 key derivation.
 */
object CryptoManager {
    private const val ALGORITHM_KEY_DERIVATION = "PBKDF2WithHmacSHA256"
    private const val ALGORITHM_CIPHER = "AES/GCM/NoPadding"
    private const val KEY_LENGTH_BITS = 256
    private const val ITERATIONS = 65536
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val SALT_LENGTH_BYTES = 32

    private val secureRandom = SecureRandom()

    const val VERIFICATION_CANARY = "VAULTX_SECURE_TOKEN_V1_VALIDATED"

    /**
     * Generates a cryptographically secure random salt.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Generates a unique initialization vector for GCM.
     */
    fun generateIv(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        return iv
    }

    /**
     * Generates cryptographically secure random bytes of given length.
     */
    fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    /**
     * Derives a 256-bit AES SecretKey from the user's master password using PBKDF2.
     */
    fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM_KEY_DERIVATION)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts arbitrary bytes using AES-256-GCM with a freshly generated IV.
     * Output format: [12 bytes IV] + [Ciphertext + 16 bytes GCM Auth Tag]
     */
    fun encrypt(plaintext: ByteArray, secretKey: SecretKey): ByteArray {
        val iv = generateIv()
        val cipher = Cipher.getInstance(ALGORITHM_CIPHER)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext)

        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return combined
    }

    /**
     * Decrypts combined payload [12 bytes IV + ciphertext/tag] using AES-256-GCM.
     */
    fun decrypt(combined: ByteArray, secretKey: SecretKey): ByteArray {
        require(combined.size > GCM_IV_LENGTH_BYTES) { "Invalid encrypted payload length" }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        val ciphertextLength = combined.size - GCM_IV_LENGTH_BYTES
        val ciphertext = ByteArray(ciphertextLength)

        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertextLength)

        val cipher = Cipher.getInstance(ALGORITHM_CIPHER)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(ciphertext)
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (e: Throwable) {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(str)
        } catch (e: Throwable) {
            Base64.decode(str, Base64.NO_WRAP)
        }
    }

    /**
     * Encrypts a UTF-8 string into a Base64-encoded string.
     */
    fun encryptString(plaintext: String, secretKey: SecretKey): String {
        val encryptedBytes = encrypt(plaintext.toByteArray(Charsets.UTF_8), secretKey)
        return encodeBase64(encryptedBytes)
    }

    /**
     * Decrypts a Base64-encoded encrypted string back to UTF-8 plaintext.
     */
    fun decryptString(base64Payload: String, secretKey: SecretKey): String {
        val encryptedBytes = decodeBase64(base64Payload)
        val decryptedBytes = decrypt(encryptedBytes, secretKey)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Generates a cryptographically strong random password.
     */
    fun generateSecurePassword(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val upperChars = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lowerChars = "abcdefghijkmnopqrstuvwxyz"
        val digitChars = "23456789"
        val symbolChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        val charPool = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        if (includeUpper) {
            charPool.append(upperChars)
            guaranteedChars.add(upperChars[secureRandom.nextInt(upperChars.length)])
        }
        if (includeLower) {
            charPool.append(lowerChars)
            guaranteedChars.add(lowerChars[secureRandom.nextInt(lowerChars.length)])
        }
        if (includeDigits) {
            charPool.append(digitChars)
            guaranteedChars.add(digitChars[secureRandom.nextInt(digitChars.length)])
        }
        if (includeSymbols) {
            charPool.append(symbolChars)
            guaranteedChars.add(symbolChars[secureRandom.nextInt(symbolChars.length)])
        }

        if (charPool.isEmpty()) {
            charPool.append(lowerChars).append(digitChars)
            guaranteedChars.add(lowerChars[secureRandom.nextInt(lowerChars.length)])
            guaranteedChars.add(digitChars[secureRandom.nextInt(digitChars.length)])
        }

        val poolString = charPool.toString()
        val actualLength = length.coerceIn(4, 64)
        val passwordChars = ArrayList<Char>(actualLength)
        passwordChars.addAll(guaranteedChars)

        while (passwordChars.size < actualLength) {
            passwordChars.add(poolString[secureRandom.nextInt(poolString.length)])
        }

        // Shuffle cryptographically
        for (i in passwordChars.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val temp = passwordChars[i]
            passwordChars[i] = passwordChars[j]
            passwordChars[j] = temp
        }

        return passwordChars.joinToString("")
    }

    /**
     * Computes the SHA-256 hex string for binary data (used for exact duplicate detection and integrity hashes).
     */
    fun sha256Hex(data: ByteArray): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes HMAC-SHA256 hex string for authenticated integrity verification of Recovery Kits.
     */
    fun hmacSha256Hex(data: ByteArray, key: ByteArray): String {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKey)
        val hmac = mac.doFinal(data)
        return hmac.joinToString("") { "%02x".format(it) }
    }
}
