package com.example

import com.example.crypto.CryptoManager
import com.example.crypto.PasswordStrength
import com.example.crypto.StrengthLevel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.GeneralSecurityException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VaultCryptoUnitTest {

    @Test
    fun testKeyDerivationAndEncryptionRoundTrip() {
        val masterPassword = "TestMasterPassword123!@#"
        val salt = CryptoManager.generateSalt()
        assertEquals(32, salt.size)

        val key = CryptoManager.deriveKey(masterPassword.toCharArray(), salt)
        assertNotNull(key)

        val sampleText = "VaultX Confidential Personal Note: 4928-3921-9923"
        val ciphertextBase64 = CryptoManager.encryptString(sampleText, key)
        assertNotEquals(sampleText, ciphertextBase64)

        val decryptedText = CryptoManager.decryptString(ciphertextBase64, key)
        assertEquals(sampleText, decryptedText)
    }

    @Test
    fun testAuthenticationTagTamperDetection() {
        val password = "SuperSecretPassword123!"
        val salt = CryptoManager.generateSalt()
        val key = CryptoManager.deriveKey(password.toCharArray(), salt)

        val payload = "TopSecretBankPIN: 9876"
        val encrypted = CryptoManager.encrypt(payload.toByteArray(Charsets.UTF_8), key)

        // Tamper with the last byte (authentication tag)
        val tampered = encrypted.copyOf()
        tampered[tampered.size - 1] = (tampered[tampered.size - 1] + 1).toByte()

        try {
            CryptoManager.decrypt(tampered, key)
            fail("Expected decryption to fail on tampered ciphertext/tag")
        } catch (e: Exception) {
            assertTrue(e is GeneralSecurityException || e.message?.contains("tag", ignoreCase = true) == true || e.cause is GeneralSecurityException)
        }
    }

    @Test
    fun testPasswordGenerator() {
        val pass1 = CryptoManager.generateSecurePassword(24, true, true, true, true)
        assertEquals(24, pass1.length)

        val pass2 = CryptoManager.generateSecurePassword(24, true, true, true, true)
        assertNotEquals(pass1, pass2) // Must be cryptographically random and unique

        val digitsOnly = CryptoManager.generateSecurePassword(6, false, false, true, false)
        assertEquals(6, digitsOnly.length)
        assertTrue(digitsOnly.all { it.isDigit() })
    }

    @Test
    fun testPasswordStrengthEvaluator() {
        val weak = PasswordStrength.evaluate("123456")
        assertTrue(weak.score <= 1)
        assertEquals(StrengthLevel.VERY_WEAK, weak.level)

        val strong = PasswordStrength.evaluate("X8#kL9@pQ2!vW4\$zM")
        assertTrue(strong.score >= 3)
        assertTrue(strong.entropyBits > 60)
    }
}
