package com.example.data

import android.content.Context
import android.util.Base64
import com.example.crypto.CryptoManager
import com.example.model.*
import com.example.security.RateLimiter
import com.example.security.VaultSecurityModule
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class VaultRepository(
    private val context: Context,
    private val vaultDao: VaultDao
) {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val passwordAdapter = moshi.adapter(PasswordPayload::class.java)
    private val noteAdapter = moshi.adapter(NotePayload::class.java)
    private val recordAdapter = moshi.adapter(SecureRecordPayload::class.java)
    private val backupAdapter = moshi.adapter(BackupPackage::class.java)

    val encryptedItemDao: EncryptedItemDao by lazy {
        VaultDatabase.getInstance(context).encryptedItemDao()
    }

    private val filesDir: File
        get() {
            val dir = File(context.filesDir, "vault_files")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun getEncryptedFileHandle(relativePath: String): File {
        return File(filesDir, relativePath)
    }

    // Config & Authentication
    val configFlow: Flow<VaultConfigEntity?> = vaultDao.getConfig()

    suspend fun getConfigDirect(): VaultConfigEntity? = withContext(Dispatchers.IO) {
        vaultDao.getConfigDirect()
    }

    suspend fun isVaultInitialized(): Boolean = withContext(Dispatchers.IO) {
        val config = vaultDao.getConfigDirect()
        config != null && config.isInitialized
    }

    suspend fun initializeVault(password: String): Boolean = withContext(Dispatchers.IO) {
        val existingConfig = vaultDao.getConfigDirect()
        val defaultLang = if (java.util.Locale.getDefault().language.startsWith("ar")) "ar" else "en"
        val lang = existingConfig?.languageCode ?: defaultLang
        val salt = CryptoManager.generateSalt()
        val derivedKey = CryptoManager.deriveKey(password.toCharArray(), salt)
        val canaryCiphertext = CryptoManager.encryptString(CryptoManager.VERIFICATION_CANARY, derivedKey)

        val config = (existingConfig ?: VaultConfigEntity()).copy(
            id = 1,
            isInitialized = true,
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            canaryCiphertext = canaryCiphertext,
            autoLockMinutes = existingConfig?.autoLockMinutes ?: 5,
            languageCode = lang,
            createdAt = existingConfig?.createdAt ?: System.currentTimeMillis(),
            lastUnlockedAt = System.currentTimeMillis()
        )
        vaultDao.saveConfig(config)
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = "VAULT_INITIALIZED",
                details = "Vault created with AES-256-GCM and PBKDF2-HMAC-SHA256"
            )
        )
        true
    }

    suspend fun verifyAndUnlockWithStatus(password: String): UnlockResult = withContext(Dispatchers.IO) {
        val remaining = RateLimiter.getRemainingLockoutSeconds()
        if (remaining > 0) {
            return@withContext UnlockResult.RateLimited(remaining)
        }

        val config = vaultDao.getConfigDirect() ?: return@withContext UnlockResult.NotInitialized
        if (!config.isInitialized || config.saltBase64.isEmpty()) return@withContext UnlockResult.NotInitialized

        try {
            val salt = Base64.decode(config.saltBase64, Base64.NO_WRAP)
            val derivedKey = CryptoManager.deriveKey(password.toCharArray(), salt)
            val decryptedCanary = CryptoManager.decryptString(config.canaryCiphertext, derivedKey)

            if (decryptedCanary == CryptoManager.VERIFICATION_CANARY) {
                RateLimiter.recordSuccessfulAttempt()
                vaultDao.saveConfig(
                    config.copy(
                        lastUnlockedAt = System.currentTimeMillis(),
                        failedAttemptsCount = 0,
                        lockoutUntilTimestamp = 0L
                    )
                )
                vaultDao.insertLog(
                    VaultAuditLogEntity(
                        action = "VAULT_UNLOCKED",
                        details = "Master authentication succeeded",
                        severity = "INFO",
                        category = "UNLOCK"
                    )
                )
                return@withContext UnlockResult.Success(derivedKey)
            }
        } catch (_: Exception) {
            // Decryption failure
        }

        val lockoutSeconds = RateLimiter.recordFailedAttempt()
        val consecutive = RateLimiter.getConsecutiveFailures()
        val severity = if (consecutive >= 3) "DANGER" else "WARNING"
        val lockoutMsg = if (lockoutSeconds > 0) " Progressive rate-limit applied ($lockoutSeconds s delay)." else ""

        vaultDao.saveConfig(
            config.copy(
                failedAttemptsCount = consecutive,
                lastFailedAttemptTimestamp = System.currentTimeMillis(),
                lockoutUntilTimestamp = if (lockoutSeconds > 0) System.currentTimeMillis() + lockoutSeconds * 1000 else 0L
            )
        )

        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = "FAILED_VAULT_UNLOCK",
                details = "Consecutive failed unlock attempt #$consecutive.$lockoutMsg",
                severity = severity,
                category = "FAILED"
            )
        )
        UnlockResult.InvalidPassword(lockoutSeconds, consecutive)
    }

    suspend fun verifyAndUnlock(password: String): SecretKey? {
        return when (val res = verifyAndUnlockWithStatus(password)) {
            is UnlockResult.Success -> res.key
            else -> null
        }
    }

    suspend fun changeMasterPassword(oldKey: SecretKey, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val config = vaultDao.getConfigDirect() ?: return@withContext false

        // Generate new salt and new derived key
        val newSalt = CryptoManager.generateSalt()
        val newKey = CryptoManager.deriveKey(newPassword.toCharArray(), newSalt)

        // Re-encrypt all items
        val allItems = vaultDao.getAllItemsDirect()
        val updatedItems = mutableListOf<VaultItemEntity>()

        for (item in allItems) {
            val decryptedPayloadString = CryptoManager.decryptString(item.encryptedPayload, oldKey)
            val reEncryptedPayload = CryptoManager.encryptString(decryptedPayloadString, newKey)

            var newRelativePath = item.fileRelativePath
            if (item.type == VaultType.FILE.name && item.fileRelativePath != null) {
                val oldFile = File(filesDir, item.fileRelativePath)
                if (oldFile.exists()) {
                    val encryptedBytes = oldFile.readBytes()
                    val plainBytes = CryptoManager.decrypt(encryptedBytes, oldKey)
                    val reEncryptedBytes = CryptoManager.encrypt(plainBytes, newKey)
                    oldFile.writeBytes(reEncryptedBytes)
                }
            }

            updatedItems.add(
                item.copy(
                    encryptedPayload = reEncryptedPayload,
                    fileRelativePath = newRelativePath,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        vaultDao.insertAll(updatedItems)

        val newCanaryCiphertext = CryptoManager.encryptString(CryptoManager.VERIFICATION_CANARY, newKey)
        vaultDao.saveConfig(
            config.copy(
                saltBase64 = Base64.encodeToString(newSalt, Base64.NO_WRAP),
                canaryCiphertext = newCanaryCiphertext,
                lastUnlockedAt = System.currentTimeMillis()
            )
        )

        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = "MASTER_PASSWORD_CHANGED",
                details = "Re-encrypted ${allItems.size} vault items with new derived key"
            )
        )
        true
    }

    suspend fun updateAutoLock(minutes: Int) = withContext(Dispatchers.IO) {
        val config = vaultDao.getConfigDirect() ?: return@withContext
        vaultDao.saveConfig(config.copy(autoLockMinutes = minutes))
    }

    suspend fun updateLockOnBackground(enabled: Boolean) = withContext(Dispatchers.IO) {
        val config = vaultDao.getConfigDirect() ?: return@withContext
        vaultDao.saveConfig(config.copy(lockOnBackground = enabled))
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = "SECURITY_CONFIG_CHANGED",
                details = "Lock on background set to $enabled"
            )
        )
    }

    suspend fun updateReducedMotion(enabled: Boolean) = withContext(Dispatchers.IO) {
        val config = vaultDao.getConfigDirect() ?: return@withContext
        vaultDao.saveConfig(config.copy(reducedMotion = enabled))
    }

    suspend fun updateLanguage(langCode: String) = withContext(Dispatchers.IO) {
        val config = vaultDao.getConfigDirect()
        if (config != null) {
            vaultDao.saveConfig(config.copy(languageCode = langCode))
        } else {
            vaultDao.saveConfig(
                VaultConfigEntity(
                    id = 1,
                    isInitialized = false,
                    languageCode = langCode,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun ensureDefaultConfig() = withContext(Dispatchers.IO) {
        if (vaultDao.getConfigDirect() == null) {
            val defaultLang = if (java.util.Locale.getDefault().language.startsWith("ar")) "ar" else "en"
            vaultDao.saveConfig(
                VaultConfigEntity(
                    id = 1,
                    isInitialized = false,
                    languageCode = defaultLang,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun logSecurityEvent(
        action: String,
        details: String,
        severity: String = "INFO",
        category: String = "SECURITY"
    ) = withContext(Dispatchers.IO) {
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = action,
                details = details,
                severity = severity,
                category = category
            )
        )
    }

    suspend fun updatePrivacyMode(enabled: Boolean) = withContext(Dispatchers.IO) {
        val config = vaultDao.getConfigDirect() ?: return@withContext
        vaultDao.saveConfig(config.copy(isPrivacyModeEnabled = enabled))
        logSecurityEvent(
            action = if (enabled) "PRIVACY_MODE_ENABLED" else "PRIVACY_MODE_DISABLED",
            details = "Privacy mode was switched to ${if (enabled) "active" else "disabled"}.",
            severity = "INFO",
            category = "PRIVACY"
        )
    }

    suspend fun saveRecoveryKitMetadata(checksum: String) = withContext(Dispatchers.IO) {
        val config = vaultDao.getConfigDirect() ?: return@withContext
        vaultDao.saveConfig(
            config.copy(
                recoveryKitCreatedAt = System.currentTimeMillis(),
                recoveryKitHash = checksum
            )
        )
        logSecurityEvent(
            action = "RECOVERY_KIT_CREATED",
            details = "Recovery Kit generated with SHA-256 integrity checksum ${checksum.take(12)}...",
            severity = "INFO",
            category = "RECOVERY"
        )
    }

    suspend fun updateItemFolder(id: String, newFolder: String) = withContext(Dispatchers.IO) {
        val item = vaultDao.getItemByIdDirect(id) ?: return@withContext
        vaultDao.updateItem(item.copy(folder = newFolder, updatedAt = System.currentTimeMillis()))
    }

    // Items Flow
    fun getActiveItems(): Flow<List<VaultItem>> = vaultDao.getActiveItems().map { list ->
        list.map { it.toDomain() }
    }

    fun getTrashItems(): Flow<List<VaultItem>> = vaultDao.getTrashItems().map { list ->
        list.map { it.toDomain() }
    }

    fun getAuditLogs(): Flow<List<VaultAuditLogEntity>> = vaultDao.getRecentLogs()

    // Note operations
    suspend fun saveNote(
        id: String = UUID.randomUUID().toString(),
        title: String,
        content: String,
        folder: String = "Notes",
        tags: List<String> = emptyList(),
        isPinned: Boolean = false,
        isFavorite: Boolean = false,
        secretKey: SecretKey
    ): VaultItem = withContext(Dispatchers.IO) {
        val payload = NotePayload(content = content)
        val json = noteAdapter.toJson(payload)
        val encryptedPayload = CryptoManager.encryptString(json, secretKey)

        val existing = vaultDao.getItemByIdDirect(id)
        val entity = VaultItemEntity(
            id = id,
            type = VaultType.NOTE.name,
            title = title.ifBlank { "Untitled Note" },
            folder = folder.ifBlank { "Notes" },
            tags = tags.joinToString(","),
            isFavorite = isFavorite,
            isPinned = isPinned,
            isTrash = false,
            trashTimestamp = null,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            encryptedPayload = encryptedPayload
        )
        vaultDao.insertItem(entity)
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = if (existing == null) "NOTE_CREATED" else "NOTE_UPDATED",
                details = "Note '${entity.title}' encrypted and stored"
            )
        )
        entity.toDomain()
    }

    // Password operations
    suspend fun savePassword(
        id: String = UUID.randomUUID().toString(),
        title: String,
        payload: PasswordPayload,
        folder: String = "Logins",
        tags: List<String> = emptyList(),
        isFavorite: Boolean = false,
        isPinned: Boolean = false,
        secretKey: SecretKey
    ): VaultItem = withContext(Dispatchers.IO) {
        val json = passwordAdapter.toJson(payload)
        val encryptedPayload = CryptoManager.encryptString(json, secretKey)

        val existing = vaultDao.getItemByIdDirect(id)
        val entity = VaultItemEntity(
            id = id,
            type = VaultType.PASSWORD.name,
            title = title.ifBlank { "Untitled Password" },
            folder = folder.ifBlank { "Logins" },
            tags = tags.joinToString(","),
            isFavorite = isFavorite,
            isPinned = isPinned,
            isTrash = false,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            encryptedPayload = encryptedPayload
        )
        vaultDao.insertItem(entity)
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = if (existing == null) "PASSWORD_CREATED" else "PASSWORD_UPDATED",
                details = "Password credential for '${entity.title}' encrypted"
            )
        )
        entity.toDomain()
    }

    // Record operations
    suspend fun saveRecord(
        id: String = UUID.randomUUID().toString(),
        title: String,
        payload: SecureRecordPayload,
        folder: String = "Records",
        tags: List<String> = emptyList(),
        isFavorite: Boolean = false,
        isPinned: Boolean = false,
        secretKey: SecretKey
    ): VaultItem = withContext(Dispatchers.IO) {
        val json = recordAdapter.toJson(payload)
        val encryptedPayload = CryptoManager.encryptString(json, secretKey)

        val existing = vaultDao.getItemByIdDirect(id)
        val entity = VaultItemEntity(
            id = id,
            type = VaultType.RECORD.name,
            title = title.ifBlank { "Untitled Record" },
            folder = folder.ifBlank { "Records" },
            tags = tags.joinToString(","),
            isFavorite = isFavorite,
            isPinned = isPinned,
            isTrash = false,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            encryptedPayload = encryptedPayload
        )
        vaultDao.insertItem(entity)
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = if (existing == null) "RECORD_CREATED" else "RECORD_UPDATED",
                details = "Record '${entity.title}' (${payload.category.displayName}) encrypted"
            )
        )
        entity.toDomain()
    }

    // File operations
    suspend fun saveEncryptedFile(
        id: String = UUID.randomUUID().toString(),
        title: String,
        originalFileName: String,
        mimeType: String,
        fileBytes: ByteArray,
        folder: String = "Documents",
        tags: List<String> = emptyList(),
        isFavorite: Boolean = false,
        secretKey: SecretKey,
        filePassword: String? = null
    ): VaultItem = withContext(Dispatchers.IO) {
        // 1. Generate random AES-256 Data Encryption Key (DEK)
        val dekBytes = CryptoManager.generateRandomBytes(32)
        val dek = SecretKeySpec(dekBytes, "AES")

        // 2. Encrypt file payload using AES-256-GCM via VaultSecurityModule
        val relativeFileName = "$id.enc"
        VaultSecurityModule.writeEncryptedFile(
            context = context,
            relativeFileName = relativeFileName,
            data = fileBytes,
            userSecretKey = dek
        )

        // 3. Wrap DEK: with KEK derived from filePassword, or with vault's secretKey
        val isProtected = !filePassword.isNullOrBlank()
        val (wrappedKeyStr, saltStr) = if (isProtected) {
            val salt = CryptoManager.generateSalt()
            val kek = CryptoManager.deriveKey(filePassword!!.toCharArray(), salt)
            val wrapped = CryptoManager.encrypt(dekBytes, kek)
            Pair(Base64.encodeToString(wrapped, Base64.NO_WRAP), Base64.encodeToString(salt, Base64.NO_WRAP))
        } else {
            val wrapped = CryptoManager.encrypt(dekBytes, secretKey)
            Pair(Base64.encodeToString(wrapped, Base64.NO_WRAP), null)
        }

        val meta = FileMetadataPayload(
            originalFileName = originalFileName,
            mimeType = mimeType,
            sizeBytes = fileBytes.size.toLong(),
            isPasswordProtected = isProtected,
            filePasswordSalt = saltStr,
            wrappedFileKey = wrappedKeyStr
        )
        val json = moshi.adapter(FileMetadataPayload::class.java).toJson(meta)
        val encryptedMeta = CryptoManager.encryptString(json, secretKey)

        val entity = VaultItemEntity(
            id = id,
            type = VaultType.FILE.name,
            title = title.ifBlank { originalFileName },
            folder = folder.ifBlank { "Documents" },
            tags = tags.joinToString(","),
            isFavorite = isFavorite,
            isPinned = false,
            isTrash = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            encryptedPayload = encryptedMeta,
            fileRelativePath = relativeFileName,
            fileName = originalFileName,
            fileMimeType = mimeType,
            fileSizeBytes = fileBytes.size.toLong(),
            isPasswordProtected = isProtected,
            filePasswordSalt = saltStr,
            wrappedFileKey = wrappedKeyStr
        )
        vaultDao.insertItem(entity)
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = if (isProtected) "FILE_ENCRYPTED_PASSWORD_PROTECTED" else "FILE_ENCRYPTED",
                details = "Encrypted file '$originalFileName' (${fileBytes.size} bytes)${if (isProtected) " with separate password" else ""}"
            )
        )
        entity.toDomain()
    }

    suspend fun decryptFileBytes(
        item: VaultItem,
        secretKey: SecretKey,
        filePassword: String? = null
    ): ByteArray? = withContext(Dispatchers.IO) {
        val relativePath = item.fileRelativePath ?: return@withContext null
        try {
            if (item.wrappedFileKey != null) {
                val wrappedBytes = Base64.decode(item.wrappedFileKey, Base64.NO_WRAP)
                val dekBytes = if (item.isPasswordProtected) {
                    if (filePassword.isNullOrBlank()) {
                        return@withContext null
                    }
                    val saltStr = item.filePasswordSalt ?: return@withContext null
                    val salt = Base64.decode(saltStr, Base64.NO_WRAP)
                    val kek = CryptoManager.deriveKey(filePassword.toCharArray(), salt)
                    CryptoManager.decrypt(wrappedBytes, kek)
                } else {
                    CryptoManager.decrypt(wrappedBytes, secretKey)
                }
                val dek = SecretKeySpec(dekBytes, "AES")
                VaultSecurityModule.readEncryptedFile(
                    context = context,
                    relativeFileName = relativePath,
                    userSecretKey = dek
                )
            } else {
                VaultSecurityModule.readEncryptedFile(
                    context = context,
                    relativeFileName = relativePath,
                    userSecretKey = secretKey
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun changeFilePassword(
        item: VaultItem,
        oldPassword: String,
        newPassword: String,
        secretKey: SecretKey
    ): Boolean = withContext(Dispatchers.IO) {
        val wrappedKeyStr = item.wrappedFileKey ?: return@withContext false
        val saltStr = item.filePasswordSalt ?: return@withContext false
        try {
            val oldSalt = Base64.decode(saltStr, Base64.NO_WRAP)
            val oldKek = CryptoManager.deriveKey(oldPassword.toCharArray(), oldSalt)
            val wrappedBytes = Base64.decode(wrappedKeyStr, Base64.NO_WRAP)
            val dekBytes = CryptoManager.decrypt(wrappedBytes, oldKek)

            val newSalt = CryptoManager.generateSalt()
            val newKek = CryptoManager.deriveKey(newPassword.toCharArray(), newSalt)
            val newWrapped = CryptoManager.encrypt(dekBytes, newKek)
            val newWrappedStr = Base64.encodeToString(newWrapped, Base64.NO_WRAP)
            val newSaltStr = Base64.encodeToString(newSalt, Base64.NO_WRAP)

            val existing = vaultDao.getItemByIdDirect(item.id) ?: return@withContext false
            val updated = existing.copy(
                isPasswordProtected = true,
                filePasswordSalt = newSaltStr,
                wrappedFileKey = newWrappedStr,
                updatedAt = System.currentTimeMillis()
            )
            vaultDao.updateItem(updated)
            vaultDao.insertLog(
                VaultAuditLogEntity(
                    action = "FILE_PASSWORD_CHANGED",
                    details = "File password changed for '${item.title}'"
                )
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun removeFilePassword(
        item: VaultItem,
        currentPassword: String,
        secretKey: SecretKey
    ): Boolean = withContext(Dispatchers.IO) {
        val wrappedKeyStr = item.wrappedFileKey ?: return@withContext false
        val saltStr = item.filePasswordSalt ?: return@withContext false
        try {
            val salt = Base64.decode(saltStr, Base64.NO_WRAP)
            val kek = CryptoManager.deriveKey(currentPassword.toCharArray(), salt)
            val wrappedBytes = Base64.decode(wrappedKeyStr, Base64.NO_WRAP)
            val dekBytes = CryptoManager.decrypt(wrappedBytes, kek)

            val newWrapped = CryptoManager.encrypt(dekBytes, secretKey)
            val newWrappedStr = Base64.encodeToString(newWrapped, Base64.NO_WRAP)

            val existing = vaultDao.getItemByIdDirect(item.id) ?: return@withContext false
            val updated = existing.copy(
                isPasswordProtected = false,
                filePasswordSalt = null,
                wrappedFileKey = newWrappedStr,
                updatedAt = System.currentTimeMillis()
            )
            vaultDao.updateItem(updated)
            vaultDao.insertLog(
                VaultAuditLogEntity(
                    action = "FILE_PASSWORD_REMOVED",
                    details = "Removed file password protection for '${item.title}'"
                )
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun addFilePassword(
        item: VaultItem,
        newPassword: String,
        secretKey: SecretKey
    ): Boolean = withContext(Dispatchers.IO) {
        val wrappedKeyStr = item.wrappedFileKey ?: return@withContext false
        try {
            val wrappedBytes = Base64.decode(wrappedKeyStr, Base64.NO_WRAP)
            val dekBytes = CryptoManager.decrypt(wrappedBytes, secretKey)

            val newSalt = CryptoManager.generateSalt()
            val newKek = CryptoManager.deriveKey(newPassword.toCharArray(), newSalt)
            val newWrapped = CryptoManager.encrypt(dekBytes, newKek)
            val newWrappedStr = Base64.encodeToString(newWrapped, Base64.NO_WRAP)
            val newSaltStr = Base64.encodeToString(newSalt, Base64.NO_WRAP)

            val existing = vaultDao.getItemByIdDirect(item.id) ?: return@withContext false
            val updated = existing.copy(
                isPasswordProtected = true,
                filePasswordSalt = newSaltStr,
                wrappedFileKey = newWrappedStr,
                updatedAt = System.currentTimeMillis()
            )
            vaultDao.updateItem(updated)
            vaultDao.insertLog(
                VaultAuditLogEntity(
                    action = "FILE_PASSWORD_ADDED",
                    details = "Added password protection to '${item.title}'"
                )
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    // Decryption helpers
    fun decryptNote(item: VaultItem, secretKey: SecretKey): NotePayload {
        return try {
            val json = CryptoManager.decryptString(item.encryptedPayload, secretKey)
            noteAdapter.fromJson(json) ?: NotePayload()
        } catch (_: Exception) {
            NotePayload("Error: Decryption failed")
        }
    }

    fun decryptPassword(item: VaultItem, secretKey: SecretKey): PasswordPayload {
        return try {
            val json = CryptoManager.decryptString(item.encryptedPayload, secretKey)
            passwordAdapter.fromJson(json) ?: PasswordPayload()
        } catch (_: Exception) {
            PasswordPayload(notes = "Error: Decryption failed")
        }
    }

    fun decryptRecord(item: VaultItem, secretKey: SecretKey): SecureRecordPayload {
        return try {
            val json = CryptoManager.decryptString(item.encryptedPayload, secretKey)
            recordAdapter.fromJson(json) ?: SecureRecordPayload()
        } catch (_: Exception) {
            SecureRecordPayload(notes = "Error: Decryption failed")
        }
    }

    // Item management
    suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        val item = vaultDao.getItemByIdDirect(id) ?: return@withContext
        vaultDao.updateItem(item.copy(isFavorite = !item.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun togglePin(id: String) = withContext(Dispatchers.IO) {
        val item = vaultDao.getItemByIdDirect(id) ?: return@withContext
        vaultDao.updateItem(item.copy(isPinned = !item.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveToTrash(id: String) = withContext(Dispatchers.IO) {
        vaultDao.moveToTrash(id)
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = "ITEM_TRASHED",
                details = "Moved item $id to trash"
            )
        )
    }

    suspend fun restoreFromTrash(id: String) = withContext(Dispatchers.IO) {
        vaultDao.restoreFromTrash(id)
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = "ITEM_RESTORED",
                details = "Restored item $id from trash"
            )
        )
    }

    suspend fun permanentlyDelete(id: String) = withContext(Dispatchers.IO) {
        val item = vaultDao.getItemByIdDirect(id)
        if (item?.fileRelativePath != null) {
            VaultSecurityModule.deleteEncryptedFile(context, item.fileRelativePath)
        }
        vaultDao.permanentlyDeleteItem(id)
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = "ITEM_PERMANENTLY_DELETED",
                details = "Permanently deleted item $id"
            )
        )
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val trashItems = vaultDao.getAllItemsDirect().filter { it.isTrash }
        for (item in trashItems) {
            if (item.fileRelativePath != null) {
                VaultSecurityModule.deleteEncryptedFile(context, item.fileRelativePath)
            }
        }
        vaultDao.emptyTrash()
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = "TRASH_EMPTIED",
                details = "Permanently removed ${trashItems.size} items from trash"
            )
        )
    }

    suspend fun wipeEntireVault() = withContext(Dispatchers.IO) {
        val files = filesDir.listFiles()
        files?.forEach { it.delete() }
        vaultDao.deleteAllItems()
        vaultDao.clearLogs()
        val config = vaultDao.getConfigDirect()
        if (config != null) {
            vaultDao.saveConfig(
                config.copy(
                    isInitialized = false,
                    saltBase64 = "",
                    canaryCiphertext = ""
                )
            )
        }
    }

    // Encrypted Backup & Restore (.vaultx)
    suspend fun createEncryptedBackup(secretKey: SecretKey): String = withContext(Dispatchers.IO) {
        val config = vaultDao.getConfigDirect() ?: throw IllegalStateException("Vault not configured")
        val allItems = vaultDao.getAllItemsDirect()

        val backupItems = allItems.map { entity ->
            var fileBytesBase64: String? = null
            if (entity.fileRelativePath != null) {
                val file = File(filesDir, entity.fileRelativePath)
                if (file.exists()) {
                    fileBytesBase64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                }
            }
            BackupItem(
                id = entity.id,
                type = entity.type,
                title = entity.title,
                folder = entity.folder,
                tags = entity.tags,
                isFavorite = entity.isFavorite,
                isPinned = entity.isPinned,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                encryptedPayload = entity.encryptedPayload,
                fileName = entity.fileName,
                fileMimeType = entity.fileMimeType,
                fileSizeBytes = entity.fileSizeBytes,
                encryptedFileBytesBase64 = fileBytesBase64
            )
        }

        val pkg = BackupPackage(
            formatVersion = 1,
            app = "VaultX",
            exportedAt = System.currentTimeMillis(),
            saltBase64 = config.saltBase64,
            verificationTokenCiphertext = config.canaryCiphertext,
            items = backupItems
        )

        vaultDao.saveConfig(config.copy(lastBackupAt = System.currentTimeMillis()))
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = "BACKUP_EXPORTED",
                details = "Encrypted backup (.vaultx) generated with ${backupItems.size} items"
            )
        )

        backupAdapter.toJson(pkg)
    }

    suspend fun parseBackup(backupJson: String): BackupPackage? = withContext(Dispatchers.IO) {
        try {
            backupAdapter.fromJson(backupJson)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun restoreBackup(backup: BackupPackage): Int = withContext(Dispatchers.IO) {
        val restoredEntities = mutableListOf<VaultItemEntity>()

        for (item in backup.items) {
            var restoredRelativePath: String? = null
            if (!item.encryptedFileBytesBase64.isNullOrEmpty()) {
                val fileBytes = Base64.decode(item.encryptedFileBytesBase64, Base64.NO_WRAP)
                val targetFileName = "${item.id}.enc"
                val file = File(filesDir, targetFileName)
                file.writeBytes(fileBytes)
                restoredRelativePath = targetFileName
            }

            restoredEntities.add(
                VaultItemEntity(
                    id = item.id,
                    type = item.type,
                    title = item.title,
                    folder = item.folder,
                    tags = item.tags,
                    isFavorite = item.isFavorite,
                    isPinned = item.isPinned,
                    isTrash = false,
                    trashTimestamp = null,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    encryptedPayload = item.encryptedPayload,
                    fileRelativePath = restoredRelativePath,
                    fileName = item.fileName,
                    fileMimeType = item.fileMimeType,
                    fileSizeBytes = item.fileSizeBytes
                )
            )
        }

        vaultDao.insertAll(restoredEntities)
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = "BACKUP_RESTORED",
                details = "Successfully restored ${restoredEntities.size} items from encrypted backup"
            )
        )
        restoredEntities.size
    }

    private fun VaultItemEntity.toDomain(): VaultItem {
        return VaultItem(
            id = this.id,
            type = VaultType.valueOf(this.type),
            title = this.title,
            folder = this.folder,
            tags = if (this.tags.isBlank()) emptyList() else this.tags.split(",").map { it.trim() },
            isFavorite = this.isFavorite,
            isPinned = this.isPinned,
            isTrash = this.isTrash,
            trashTimestamp = this.trashTimestamp,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            encryptedPayload = this.encryptedPayload,
            fileRelativePath = this.fileRelativePath,
            fileName = this.fileName,
            fileMimeType = this.fileMimeType,
            fileSizeBytes = this.fileSizeBytes,
            isPasswordProtected = this.isPasswordProtected,
            filePasswordSalt = this.filePasswordSalt,
            wrappedFileKey = this.wrappedFileKey
        )
    }
}
