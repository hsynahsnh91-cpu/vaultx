package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CryptoManager
import com.example.data.EncryptedItem
import com.example.data.EncryptedItemDao
import com.example.data.EncryptedItemType
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
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseAndSecurityTest {

    private lateinit var database: VaultDatabase
    private lateinit var encryptedItemDao: EncryptedItemDao
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            VaultDatabase::class.java
        ).allowMainThreadQueries().build()
        encryptedItemDao = database.encryptedItemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRoomEncryptedItemPersistence() = runBlocking {
        val masterPassword = "TestMasterPassword@2026!"
        val salt = CryptoManager.generateSalt()
        val secretKey = CryptoManager.deriveKey(masterPassword.toCharArray(), salt)

        // 1. Create and persist encrypted note
        val notePlaintext = "Confidential Recovery Phrase: apple banana orange grape"
        val encryptedNotePayload = CryptoManager.encryptString(notePlaintext, secretKey)
        val noteItem = EncryptedItem(
            id = UUID.randomUUID().toString(),
            type = EncryptedItemType.NOTE.name,
            title = "Personal Crypto Recovery",
            folder = "Financial",
            tags = "crypto,recovery",
            isFavorite = true,
            isPinned = true,
            encryptedPayload = encryptedNotePayload
        )
        encryptedItemDao.insertItem(noteItem)

        // 2. Create and persist encrypted password
        val passwordPlaintext = """{"username":"admin","password":"SuperSecretPassword#123"}"""
        val encryptedPasswordPayload = CryptoManager.encryptString(passwordPlaintext, secretKey)
        val passwordItem = EncryptedItem(
            id = UUID.randomUUID().toString(),
            type = EncryptedItemType.PASSWORD.name,
            title = "AWS Production Portal",
            folder = "Cloud",
            encryptedPayload = encryptedPasswordPayload
        )
        encryptedItemDao.insertItem(passwordItem)

        // 3. Create and persist encrypted file item
        val fileItem = EncryptedItem(
            id = UUID.randomUUID().toString(),
            type = EncryptedItemType.FILE.name,
            title = "Passport Scan 2026",
            folder = "Identity",
            encryptedPayload = CryptoManager.encryptString("""{"originalFileName":"passport.pdf"}""", secretKey),
            fileRelativePath = "passport_test.enc",
            fileName = "passport.pdf",
            fileMimeType = "application/pdf",
            fileSizeBytes = 1048576L
        )
        encryptedItemDao.insertItem(fileItem)

        // Query active items
        val activeItems = encryptedItemDao.getActiveItems().first()
        assertEquals(3, activeItems.size)

        // Test filtering by type
        val notes = encryptedItemDao.getItemsByType(EncryptedItemType.NOTE.name).first()
        assertEquals(1, notes.size)
        assertEquals("Personal Crypto Recovery", notes[0].title)

        // Verify payload decryption
        val decryptedNote = CryptoManager.decryptString(notes[0].encryptedPayload, secretKey)
        assertEquals(notePlaintext, decryptedNote)

        // Test trash and restore
        encryptedItemDao.moveToTrash(passwordItem.id)
        val afterTrashActive = encryptedItemDao.getActiveItems().first()
        assertEquals(2, afterTrashActive.size)

        val trashItems = encryptedItemDao.getTrashItems().first()
        assertEquals(1, trashItems.size)
        assertEquals(passwordItem.id, trashItems[0].id)

        encryptedItemDao.restoreFromTrash(passwordItem.id)
        val restoredActive = encryptedItemDao.getActiveItems().first()
        assertEquals(3, restoredActive.size)
    }

    @Test
    fun testVaultSecurityModuleFileEncryption() {
        val testData = "Highly Classified Document: Top Secret Level 5".toByteArray(Charsets.UTF_8)
        val relativeFileName = "test_doc_${System.currentTimeMillis()}.enc"

        // Write using Android Security / AES-256
        val writtenFile = VaultSecurityModule.writeEncryptedFile(
            context = context,
            relativeFileName = relativeFileName,
            data = testData
        )
        assertTrue(writtenFile.exists())

        // Read and verify round-trip integrity
        val readBytes = VaultSecurityModule.readEncryptedFile(
            context = context,
            relativeFileName = relativeFileName
        )
        assertArrayEquals(testData, readBytes)

        // Verify file cleanup
        val deleted = VaultSecurityModule.deleteEncryptedFile(context, relativeFileName)
        assertTrue(deleted)
        assertFalse(writtenFile.exists())
    }

    @Test
    fun testEncryptedSharedPreferencesAccess() {
        val prefs = VaultSecurityModule.getEncryptedSharedPreferences(context)
        prefs.edit().putString("test_vault_key", "test_vault_secret_value").commit()

        val retrieved = prefs.getString("test_vault_key", null)
        assertEquals("test_vault_secret_value", retrieved)
    }
}
