package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Types of secure items managed within VaultX.
 */
enum class EncryptedItemType {
    FILE,
    NOTE,
    PASSWORD,
    RECORD
}

/**
 * Room entity representing an encrypted item (files, notes, passwords, and custom records).
 * All sensitive payloads and file paths are secured with AES-256 encryption.
 */
@Entity(tableName = "encrypted_items")
data class EncryptedItem(
    @PrimaryKey val id: String,
    val type: String, // EncryptedItemType: FILE, NOTE, PASSWORD, RECORD
    val title: String,
    val folder: String = "Default",
    val tags: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isTrash: Boolean = false,
    val trashTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val encryptedPayload: String, // Base64 AES-256-GCM ciphertext
    val fileRelativePath: String? = null, // Relative path to EncryptedFile in internal storage
    val fileName: String? = null,
    val fileMimeType: String? = null,
    val fileSizeBytes: Long? = null,
    val initializationVector: String = "", // Explicit IV metadata (also packed in GCM ciphertext)
    val isPasswordProtected: Boolean = false,
    val filePasswordSalt: String? = null,
    val wrappedFileKey: String? = null
)

/**
 * Alias for backward compatibility across existing repositories and ViewModels.
 */
typealias VaultItemEntity = EncryptedItem

/**
 * Configuration and cryptographic parameters for the vault.
 */
@Entity(tableName = "vault_config")
data class VaultConfigEntity(
    @PrimaryKey val id: Int = 1,
    val isInitialized: Boolean = false,
    val saltBase64: String = "",
    val canaryCiphertext: String = "",
    val autoLockMinutes: Int = 5,
    val lockOnBackground: Boolean = true,
    val reducedMotion: Boolean = true,
    val languageCode: String = "en",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUnlockedAt: Long = 0L,
    val lastBackupAt: Long = 0L,
    val isPrivacyModeEnabled: Boolean = false,
    val failedAttemptsCount: Int = 0,
    val lockoutUntilTimestamp: Long = 0L,
    val lastFailedAttemptTimestamp: Long = 0L,
    val recoveryKitCreatedAt: Long? = null,
    val recoveryKitHash: String? = null
)

/**
 * Audit log recording security operations and key lifecycle events.
 */
@Entity(tableName = "vault_audit_logs")
data class VaultAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0L,
    val action: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = "",
    val severity: String = "INFO", // "INFO", "WARNING", "DANGER"
    val category: String = "SECURITY" // "ALL", "UNLOCK", "FAILED", "LOCK", "RECOVERY", "BACKUP", "PROTECTED_FILE", "SETTINGS", "PRIVACY"
)
