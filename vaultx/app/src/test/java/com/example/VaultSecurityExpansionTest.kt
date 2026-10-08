package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CryptoManager
import com.example.data.EncryptedItem
import com.example.data.EncryptedItemDao
import com.example.data.VaultConfigEntity
import com.example.data.VaultDatabase
import com.example.security.VaultSecurityModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VaultSecurityExpansionTest {

    private lateinit var database: VaultDatabase
    private lateinit var itemDao: EncryptedItemDao
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            VaultDatabase::class.java
        ).allowMainThreadQueries().build()
        itemDao = database.encryptedItemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testFilePasswordKeyWrappingAndUnwrapping() {
        // 1. Generate random file payload & DEK
        val filePayload = "Confidential Financial Report 2026 Q3: Total Net 50,000,000 USD".toByteArray(Charsets.UTF_8)
        val dekBytes = CryptoManager.generateRandomBytes(32)
        val dek = SecretKeySpec(dekBytes, "AES")

        // 2. Encrypt file payload with DEK using AES-256-GCM
        val relativeFileName = "test_dek_file_${UUID.randomUUID()}.enc"
        VaultSecurityModule.writeEncryptedFile(
            context = context,
            relativeFileName = relativeFileName,
            data = filePayload,
            userSecretKey = dek
        )

        // 3. User sets file-specific password
        val filePassword = "FileSpecificSecret99!"
        val salt = CryptoManager.generateSalt()
        val kek = CryptoManager.deriveKey(filePassword.toCharArray(), salt)

        // 4. Wrap DEK with KEK
        val wrappedDek = CryptoManager.encrypt(dekBytes, kek)
        val wrappedDekBase64 = Base64.getEncoder().encodeToString(wrappedDek)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)

        // 5. Successful unwrap with correct password
        val retrievedSalt = Base64.getDecoder().decode(saltBase64)
        val derivedKek = CryptoManager.deriveKey(filePassword.toCharArray(), retrievedSalt)
        val unwrappedDekBytes = CryptoManager.decrypt(Base64.getDecoder().decode(wrappedDekBase64), derivedKek)
        assertArrayEquals(dekBytes, unwrappedDekBytes)

        val retrievedDek = SecretKeySpec(unwrappedDekBytes, "AES")
        val decryptedFile = VaultSecurityModule.readEncryptedFile(
            context = context,
            relativeFileName = relativeFileName,
            userSecretKey = retrievedDek
        )
        assertArrayEquals(filePayload, decryptedFile)

        // 6. Failed unwrap with incorrect password must fail safely
        val wrongPassword = "WrongPassword99!"
        val wrongKek = CryptoManager.deriveKey(wrongPassword.toCharArray(), retrievedSalt)
        try {
            CryptoManager.decrypt(Base64.getDecoder().decode(wrappedDekBase64), wrongKek)
            fail("Expected decryption failure with wrong password")
        } catch (e: Exception) {
            assertTrue(e is java.security.GeneralSecurityException || e.cause is java.security.GeneralSecurityException || e.message?.contains("tag") == true)
        }

        // Cleanup
        VaultSecurityModule.deleteEncryptedFile(context, relativeFileName)
    }

    @Test
    fun testChangeFilePasswordWithoutReEncryptingFilePayload() {
        val filePayload = "Huge 500MB Video Payload Mock".toByteArray(Charsets.UTF_8)
        val dekBytes = CryptoManager.generateRandomBytes(32)
        val dek = SecretKeySpec(dekBytes, "AES")

        val relativeFileName = "test_change_pw_${UUID.randomUUID()}.enc"
        VaultSecurityModule.writeEncryptedFile(context, relativeFileName, filePayload, dek)

        // Old password
        val oldPass = "OldFilePass123"
        val oldSalt = CryptoManager.generateSalt()
        val oldKek = CryptoManager.deriveKey(oldPass.toCharArray(), oldSalt)
        val wrappedDek = CryptoManager.encrypt(dekBytes, oldKek)

        // Change password to newPass: unwrap DEK, derive new KEK, re-wrap DEK
        val recoveredDekBytes = CryptoManager.decrypt(wrappedDek, oldKek)
        val newPass = "NewFilePass456"
        val newSalt = CryptoManager.generateSalt()
        val newKek = CryptoManager.deriveKey(newPass.toCharArray(), newSalt)
        val reWrappedDek = CryptoManager.encrypt(recoveredDekBytes, newKek)

        // Verify new password successfully decrypts DEK and the file
        val unwrapWithNewPass = CryptoManager.decrypt(reWrappedDek, newKek)
        val newDek = SecretKeySpec(unwrapWithNewPass, "AES")
        val decryptedData = VaultSecurityModule.readEncryptedFile(context, relativeFileName, newDek)
        assertArrayEquals(filePayload, decryptedData)

        VaultSecurityModule.deleteEncryptedFile(context, relativeFileName)
    }

    @Test
    fun testVaultConfigEntityBackgroundLockAndReducedMotion() = runBlocking {
        val initialConfig = VaultConfigEntity(
            id = 1,
            isInitialized = true,
            saltBase64 = "dGVzdHNhbHQ=",
            autoLockMinutes = 5,
            lockOnBackground = true,
            reducedMotion = false,
            languageCode = "en"
        )
        database.vaultDao().saveConfig(initialConfig)

        var config = database.vaultDao().getConfig().first()
        assertNotNull(config)
        assertTrue(config!!.lockOnBackground)
        assertFalse(config.reducedMotion)

        // Update settings
        val updatedConfig = config.copy(lockOnBackground = false, reducedMotion = true)
        database.vaultDao().saveConfig(updatedConfig)

        config = database.vaultDao().getConfig().first()
        assertNotNull(config)
        assertFalse(config!!.lockOnBackground)
        assertTrue(config.reducedMotion)
    }

    @Test
    fun testProtectedFileEntityPersistence() = runBlocking {
        val item = EncryptedItem(
            id = "file_test_id",
            type = "FILE",
            title = "TopSecret.pdf",
            folder = "Documents",
            encryptedPayload = "encrypted_meta",
            fileRelativePath = "file_test_id.enc",
            fileName = "TopSecret.pdf",
            fileMimeType = "application/pdf",
            fileSizeBytes = 2048L,
            isPasswordProtected = true,
            filePasswordSalt = "salt_base64",
            wrappedFileKey = "wrapped_dek_base64"
        )
        itemDao.insertItem(item)

        val retrieved = itemDao.getItemByIdDirect("file_test_id")
        assertNotNull(retrieved)
        assertTrue(retrieved!!.isPasswordProtected)
        assertEquals("salt_base64", retrieved.filePasswordSalt)
        assertEquals("wrapped_dek_base64", retrieved.wrappedFileKey)
    }
}
