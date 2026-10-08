package com.example.model

enum class VaultType {
    FILE,
    NOTE,
    PASSWORD,
    RECORD
}

enum class RecordCategory(val displayName: String) {
    IDENTITY("Identity / Personal"),
    PASSPORT("Passport"),
    LICENSE("Driver's License"),
    BANK_ACCOUNT("Bank Account"),
    CREDIT_CARD("Credit / Debit Card"),
    WIFI("Wi-Fi Credentials"),
    SOFTWARE_LICENSE("Software License"),
    RECOVERY_SEED("Recovery Seed / 2FA"),
    CUSTOM("Custom Secure Record")
}

data class SecureRecordField(
    val id: String,
    val label: String,
    val value: String,
    val isSensitive: Boolean = true
)

data class PasswordPayload(
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = ""
)

data class NotePayload(
    val content: String = ""
)

data class SecureRecordPayload(
    val category: RecordCategory = RecordCategory.CUSTOM,
    val fields: List<SecureRecordField> = emptyList(),
    val notes: String = ""
)

data class FileMetadataPayload(
    val originalFileName: String = "",
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0L,
    val isPasswordProtected: Boolean = false,
    val filePasswordSalt: String? = null,
    val wrappedFileKey: String? = null
)

enum class AutoLockTimeout(val minutes: Int, val label: String) {
    ONE_MINUTE(1, "1 minute"),
    FIVE_MINUTES(5, "5 minutes"),
    FIFTEEN_MINUTES(15, "15 minutes"),
    THIRTY_MINUTES(30, "30 minutes"),
    NEVER(0, "Never")
}

data class VaultItem(
    val id: String,
    val type: VaultType,
    val title: String,
    val folder: String = "Default",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isTrash: Boolean = false,
    val trashTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val encryptedPayload: String, // Base64 ciphertext
    val fileRelativePath: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null,
    val fileSizeBytes: Long? = null,
    val isPasswordProtected: Boolean = false,
    val filePasswordSalt: String? = null,
    val wrappedFileKey: String? = null
)

data class BackupPackage(
    val formatVersion: Int = 1,
    val app: String = "VaultX",
    val exportedAt: Long = System.currentTimeMillis(),
    val saltBase64: String,
    val verificationTokenCiphertext: String,
    val items: List<BackupItem>
)

data class BackupItem(
    val id: String,
    val type: String,
    val title: String,
    val folder: String,
    val tags: String,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val encryptedPayload: String,
    val fileName: String?,
    val fileMimeType: String?,
    val fileSizeBytes: Long?,
    val encryptedFileBytesBase64: String? = null
)

data class SecurityAuditSummary(
    val totalItems: Int,
    val totalFiles: Int,
    val totalNotes: Int,
    val totalPasswords: Int,
    val totalRecords: Int,
    val weakPasswordsCount: Int,
    val reusedPasswordsCount: Int,
    val autoLockMinutes: Int,
    val lastBackupTimestamp: Long?,
    val totalStorageBytes: Long
)

enum class SmartCollectionType {
    ALL,
    DOCUMENTS,
    IMAGES,
    VIDEOS,
    AUDIO,
    PROTECTED,
    FAVORITES,
    RECENTLY_ADDED,
    RECENTLY_OPENED
}

data class DuplicateGroup(
    val id: String,
    val contentHash: String,
    val original: VaultItem,
    val duplicates: List<VaultItem>,
    val fileSizeBytes: Long
)

data class RecoveryKit(
    val formatVersion: Int = 1,
    val app: String = "VaultX",
    val vaultId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val saltBase64: String,
    val pbkdf2Iterations: Int = 65536,
    val canaryDigestSha256: String,
    val integrityHmac: String,
    val instructions: String
)

data class RecoveryKitVerificationResult(
    val isValid: Boolean,
    val version: Int,
    val vaultIdMatches: Boolean,
    val saltMatches: Boolean,
    val integrityVerified: Boolean,
    val message: String
)

data class AiOrganizeSuggestion(
    val itemId: String,
    val fileName: String,
    val currentFolder: String,
    val suggestedCategory: String, // Study, Work, Documents, Finance, Personal
    val confidence: String, // High, Medium, Low
    val reason: String,
    var isApproved: Boolean = true
)

data class AiDocumentSummary(
    val summary: String,
    val importantDates: List<String> = emptyList(),
    val tasks: List<String> = emptyList(),
    val keyTopics: List<String> = emptyList(),
    val outline: List<String> = emptyList()
)

data class NaturalLanguageSearchResult(
    val item: VaultItem,
    val matchExplanation: String,
    val relevanceScore: Float
)

sealed class UnlockResult {
    data class Success(val key: javax.crypto.SecretKey) : UnlockResult()
    data class InvalidPassword(val remainingLockoutSeconds: Long, val consecutiveFailures: Int) : UnlockResult()
    data class RateLimited(val remainingLockoutSeconds: Long) : UnlockResult()
    object NotInitialized : UnlockResult()
}

