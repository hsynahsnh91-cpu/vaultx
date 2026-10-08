package com.example.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.LayoutDirection
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Localized string dictionary supporting English and Arabic.
 * Single source of truth for all user-facing strings across VaultX.
 */
interface VaultStrings {
    val appName: String
    val vaultLocked: String
    val enterMasterPassword: String
    val unlock: String
    val createVault: String
    val setupMasterPassword: String
    val confirmPassword: String
    val passwordMismatch: String
    val passwordMinLength: String
    val invalidPassword: String
    val lockNow: String
    val dashboard: String
    val files: String
    val notes: String
    val passwords: String
    val records: String
    val securityCenter: String
    val privacyCenter: String
    val backupRestore: String
    val trash: String
    val settings: String
    val searchVault: String
    val totalItems: String
    val storageUsed: String
    val recentItems: String
    val quickActions: String
    val newFile: String
    val newNote: String
    val newPassword: String
    val newRecord: String
    val title: String
    val content: String
    val username: String
    val passwordField: String
    val websiteUrl: String
    val generatePassword: String
    val copyPassword: String
    val passwordCopied: String
    val showPassword: String
    val hidePassword: String
    val folder: String
    val tags: String
    val favorite: String
    val pin: String
    val moveToTrash: String
    val restore: String
    val permanentDelete: String
    val emptyTrash: String
    val trashEmptyConfirm: String
    val permanentDeleteConfirm: String
    val save: String
    val cancel: String
    val edit: String
    val preview: String
    val exportFile: String
    val exportBackup: String
    val importBackup: String
    val autoLock: String
    val language: String
    val securityScore: String
    val passwordAudit: String
    val weakPasswordsFound: String
    val reusedPasswordsFound: String
    val allPasswordsStrong: String
    val encryptionStandard: String
    val zeroTelemetryGuarantee: String
    val localFirstNotice: String
    val changeMasterPassword: String
    val wipeVault: String
    val wipeVaultConfirm: String
    val noItemsFound: String
    val all: String
    val category: String
    val addField: String
    val fieldName: String
    val fieldValue: String
    val isSensitive: (Boolean) -> String
    val securitySetupDisclosure: String
    val securityLockedDisclosure: String
    val hardwareCryptoReassurance: String
    val switchLanguage: String
    val auditLogsTitle: String
    val dangerZoneTitle: String
    val privacyArchitectureTitle: String
    val privacyArchitectureSubtitle: String
    val privacyQ1Title: String
    val privacyQ1Answer: String
    val privacyQ2Title: String
    val privacyQ2Answer: String
    val privacyQ3Title: String
    val privacyQ3Answer: String
    val privacyQ4Title: String
    val privacyQ4Answer: String
    val privacyQ5Title: String
    val privacyQ5Answer: String
    val privacyQ6Title: String
    val privacyQ6Answer: String

    // Background Lock & Motion
    val lockOnBackground: String
    val lockOnBackgroundDesc: String
    val lockOnBackgroundWarning: String
    val reducedMotion: String
    val reducedMotionDesc: String

    // File Password Protection
    val protectWithSeparatePassword: String
    val filePassword: String
    val confirmFilePassword: String
    val unlockFile: String
    val protectedFileTitle: String
    val protectedFileSubtitle: String
    val unableToUnlockFile: String
    val changeFilePassword: String
    val removeFilePassword: String
    val addFilePassword: String
    val currentFilePassword: String
    val newFilePassword: String
    val removePasswordConfirmDesc: String
    val filePasswordChanged: String
    val filePasswordRemoved: String
    val filePasswordAdded: String
    val protectedBadge: String

    // Universal Preview
    val previewUnavailable: String
    val previewUnavailableDesc: String
    val downloadSecurely: String
    val zoomIn: String
    val zoomOut: String
    val rotate: String
    val fitToScreen: String
    val pageOf: (Int, Int) -> String
    val archiveContents: String
    val archiveFileCount: (Int) -> String
    val codeViewer: String
    val linesCount: (Int) -> String

    // AI Tools
    val aiTools: String
    val aiFileOrganizer: String
    val aiDocumentUnderstanding: String
    val aiNaturalLanguageSearch: String
    val analyzeAndOrganize: String
    val applyApprovedSuggestions: String
    val aiConsentRequiredTitle: String
    val aiConsentRequiredMessage: String
    val allowCloudAi: String
    val cancelCloudAi: String
    val localIntelligenceMode: String
    val documentSummary: String
    val importantDates: String
    val tasksAndActions: String
    val keyTopics: String
    val outline: String
    val searchQueryPlaceholder: String
    val matchReason: String

    // Duplicate Detector
    val duplicateDetector: String
    val scanDuplicates: String
    val noDuplicatesFound: String
    val duplicatesDetected: (Int) -> String
    val originalFile: String
    val duplicateCopies: String
    val keepOneMoveOthersToTrash: String
    val duplicateScanProgress: (Int, Int) -> String

    // Recovery Kit
    val recoveryKit: String
    val createRecoveryKit: String
    val verifyRecoveryKit: String
    val recoveryKitExplanation: String
    val recoveryKitWarningNotice: String
    val exportRecoveryKitJson: String
    val pasteRecoveryKitJson: String
    val recoveryKitVerifiedSuccess: String
    val recoveryKitVerifiedFailed: String

    // Security Activity & Rate Limiting
    val securityActivity: String
    val accessAttemptsTitle: String
    val consecutiveFailuresCount: (Int) -> String
    val rateLimitLockoutNotice: (Int) -> String
    val filterAll: String
    val filterSuccessful: String
    val filterFailed: String
    val filterUnlocks: String
    val filterRecovery: String
    val filterBackups: String
    val filterProtectedFiles: String
    val filterSecurityChanges: String

    // Privacy Mode
    val privacyMode: String
    val privacyModeOn: String
    val privacyModeOff: String
    val privacyModeDescription: String
    val temporarilyReveal: String
    val sensitiveValueMasked: String
    val previewMaskedNotice: String

    // Smart Collections
    val smartCollections: String
    val collectionAll: String
    val collectionDocuments: String
    val collectionImages: String
    val collectionVideos: String
    val collectionAudio: String
    val collectionProtected: String
    val collectionFavorites: String
    val collectionRecentAdded: String
    val collectionRecentOpened: String

    // --- ADDITIONAL AUDITED KEYS ---
    val confirm: String
    val active: String
    val close: String
    val options: String
    val menu: String
    val unfavorite: String
    val unpin: String
    val pinned: String
    val delete: String
    val clear: String
    val search: String
    val shield: String
    val lockout: String
    val error: String
    val reveal: String
    val hide: String

    // Settings additions
    val preferencesAndSecurity: String
    val securityOperations: String
    val changePasswordDesc: String
    val newMasterPasswordLabel: String
    val confirmNewPasswordLabel: String
    val changePasswordExplanation: String
    val passwordChangedSuccess: String
    val wipeVaultSubtitle: String
    val wipeVaultConfirmButton: String
    val wipeVaultErasedToast: String
    val auditLogsRecordedCount: (Int) -> String
    val noAuditLogs: String
    val privacyModeEnabledToast: String
    val privacyModeDisabledToast: String
    val timeoutMinutes: (Int) -> String
    val timeoutNever: String

    // Common components & Generator
    val passwordGenerator: String
    val regenerate: String
    val lengthWithVal: (Int) -> String
    val uppercaseChars: String
    val lowercaseChars: String
    val numberChars: String
    val symbolChars: String
    val strengthWithEntropy: (String, Int) -> String
    val strengthTip: (String) -> String
    val strengthVeryWeak: String
    val strengthWeak: String
    val strengthFair: String
    val strengthStrong: String
    val strengthVeryStrong: String

    // Dashboard additions
    val vaultProtected: String
    val gcm256: String
    val autoLockLabel: String
    val backupLabel: String
    val backupActive: String
    val backupPending: String
    val storageLabel: String
    val storageLocalOnly: String
    val encryptedItems: String
    val secureNotes: String
    val weakCountLabel: (Int) -> String
    val allStrongLabel: String
    val dashboardEmptyDesc: String
    val maskPlaceholderVault: String
    val maskPlaceholderProtected: String

    // Files additions
    val cannotReadEmptyFile: String
    val errorImportingFile: (String) -> String
    val fileExportedSuccess: String
    val exportError: (String) -> String
    val failedDecryptPreview: String
    val failedDecryptExport: String
    val filesHeaderSubtitle: String
    val duplicateInfoSubtitle: (String, Int) -> String
    val movedDuplicatesToTrash: (Int) -> String
    val storageDuplicatesCleaned: String
    val fileEncryptedToast: String
    val vaultItemSecured: String
    val moveToTrashConfirmMsg: (String) -> String
    val filePasswordMinLength: String
    val encryptAndSave: String

    // Notes additions
    val noNotesTitle: String
    val noNotesDesc: String
    val notesCount: (Int) -> String
    val noteCopiedToast: String
    val noteCopiedGeneric: String
    val noteContentMaskedPlaceholder: String
    val updatedAtLabel: (String) -> String
    val editNoteTitle: String
    val folderCategoryLabel: String
    val wordsAndCharsCount: (Int, Int) -> String
    val untitledNote: String
    val moveToTrashNoteConfirm: (String) -> String

    // Passwords additions
    val noPasswordsTitle: String
    val noPasswordsDesc: String
    val passwordsCount: (Int) -> String
    val editPasswordTitle: String
    val serviceTitlePlaceholder: String
    val privateNotesOptional: String
    val untitledAccount: String
    val moveToTrashPasswordConfirm: (String) -> String
    val editCredentialAction: String
    val credentialCopiedToast: String

    // Records additions
    val noRecordsTitle: String
    val noRecordsDesc: String
    val recordsCount: (Int) -> String
    val fieldsCountSummary: (String, Int) -> String
    val fieldCopiedToast: (String) -> String
    val recordFieldCopiedGeneric: String
    val notesWithVal: (String) -> String
    val editRecordTitle: String
    val fieldsHeader: String
    val fieldLabel: String
    val fieldValueLabel: String
    val fieldDefault: String
    val moveToTrashRecordConfirm: (String) -> String
    val categoryPassport: String
    val categoryDriversLicense: String
    val categoryBankAccount: String
    val categoryCreditCard: String
    val categoryWifiNetwork: String
    val categorySoftwareLicense: String
    val categoryCryptoWallet: String
    val categoryOther: String
    val fieldPassportNumber: String
    val fieldCountry: String
    val fieldExpirationDate: String
    val fieldFullName: String
    val fieldLicenseNumber: String
    val fieldStateProvince: String
    val fieldBankName: String
    val fieldAccountNumber: String
    val fieldRoutingSwift: String
    val fieldIban: String
    val fieldCardholderName: String
    val fieldCardNumber: String
    val fieldCvvCvc: String
    val fieldPin: String
    val fieldNetworkName: String
    val fieldProductName: String
    val fieldLicenseKey: String
    val fieldRecoverySeed: String
    val fieldDerivationPath: String
    val fieldDetail: String

    // Trash additions
    val trashEmptyTitle: String
    val trashEmptyDesc: String
    val trashItemsCount: (Int) -> String
    val deletedDateLabel: (String) -> String

    // Backup & Restore additions
    val failedCreateBackupPackage: String
    val backupSavedToast: String
    val backupPackageGenerated: String
    val invalidBackupToast: String
    val errorReadingBackup: (String) -> String
    val generateKitButton: String
    val recoveryKitGeneratedToast: String
    val restoreBackupTitle: String
    val restoreBackupMessage: (Int, String) -> String
    val restoreNowButton: String
    val restoredItemsSuccess: (Int) -> String
    val restoreCompletedGeneric: String
    val generatedRecoveryKitJsonTitle: String
    val vaultIdLabel: (String) -> String
    val checksumLabel: (String) -> String
    val recoveryKitCopiedToast: String
    val recoveryDataCopiedGeneric: String
    val copyJsonButton: String
    val exportEncryptedBackupTitle: String
    val exportEncryptedBackupDesc: String
    val importRestoreBackupTitle: String
    val importRestoreBackupDesc: String
    val verifyKitDesc: String
    val invalidJsonError: String
    val verifyIntegrityButton: String
    val recoveryKitValid: String
    val recoveryHistoryTitle: String
    val noRecoveryHistory: String

    // Security Center additions
    val healthExcellent: String
    val healthGood: String
    val healthAttention: String
    val healthCritical: String
    val vaultHealth: (String) -> String
    val securitySubtitle: String
    val rateLimitedStatus: String
    val normalStatus: String
    val zeroFailuresDesc: String
    val weakPasswordsTitle: String
    val noWeakPasswords: String
    val reviewPasswordsAction: String
    val reusedPasswordsTitle: String
    val noReusedPasswords: String
    val resolveDuplicatesAction: String
    val accessProtectionTitle: String
    val autoLockActiveStatus: (Int) -> String
    val autoLockDisabledWarning: String
    val configureAction: String
    val pbkdf2IterationsDesc: String
    val updateKeyAction: String
    val noSecurityEventsFound: String

    // Universal Preview additions
    val play: String
    val pause: String
    val rewind10s: String
    val forward10s: String
    val previousPage: String
    val nextPage: String
    val unableToDecodeText: String
    val mimeTypeLabel: String
    val sizeLabel: String
    val sha256Label: String
    val encryptedContentProtected: String

    // TopBar & Privacy additions
    val privacyOnBadge: String
    val aesGcmBadge: String
    val languageToggleLabel: String
    val sampleFileName: String
    val sampleFileSizeSensitive: String
    val sampleBadge: String
    val maskedBadge: String

    // AI screen additions
    val geminiCloudApproved: String
    val allowGeminiCloudSwitch: String
    val aiFileOrganizerHelp: String
    val aiDocAiHelp: String
    val selectDocumentPlaceholder: String
    val analysisResult: (String) -> String
    val aiSearchHelp: String
    val matchScore: (Int) -> String
    val aiPrivacyPolicyTitle: String
    val aiPrivacyPolicyBody: String
    val filesCount: (Int) -> String
    val duplicateCopiesLabel: (Int) -> String

    // Core default labels & system messages
    val defaultDocumentName: String
    val defaultFolderLogins: String
    val defaultFolderNotes: String
    val defaultFolderRecords: String
    val recoveryKitInvalid: String
    val recoveryKitInvalidJson: String
    val recoveryKitMatchSuccess: String
    val recoveryKitMismatch: String
    val failedUnlockNewVault: String
    val cannotDecodeText: String
    val archiveEntrySize: (String, String) -> String
    val tipAtLeast12Chars: String
    val tipAddLowercase: String
    val tipAddUppercase: String
    val tipAddDigits: String
    val tipAddSymbols: String
    val tipPasswordCannotBeEmpty: String

    fun formatBytes(bytes: Long): String
    fun formatDate(timestamp: Long): String
}

internal class EnglishVaultStrings : VaultStrings {
    override val appName: String = "VaultX"
    override val vaultLocked: String = "Vault is Locked"
    override val enterMasterPassword: String = "Enter Master Password"
    override val unlock: String = "Unlock Vault"
    override val createVault: String = "Create Your Secure Vault"
    override val setupMasterPassword: String = "Create Master Password"
    override val confirmPassword: String = "Confirm Master Password"
    override val passwordMismatch: String = "Passwords do not match"
    override val passwordMinLength: String = "Password must be at least 8 characters"
    override val invalidPassword: String = "Authentication failed. Incorrect password."
    override val lockNow: String = "Lock Now"
    override val dashboard: String = "Dashboard"
    override val files: String = "Files"
    override val notes: String = "Notes"
    override val passwords: String = "Passwords"
    override val records: String = "Records"
    override val securityCenter: String = "Security"
    override val privacyCenter: String = "Privacy"
    override val backupRestore: String = "Backup"
    override val trash: String = "Trash"
    override val settings: String = "Settings"
    override val searchVault: String = "Search encrypted vault..."
    override val totalItems: String = "Total Items"
    override val storageUsed: String = "Storage Used"
    override val recentItems: String = "Recent Activity"
    override val quickActions: String = "Quick Actions"
    override val newFile: String = "Upload File"
    override val newNote: String = "New Note"
    override val newPassword: String = "Add Password"
    override val newRecord: String = "Add Record"
    override val title: String = "Title"
    override val content: String = "Content"
    override val username: String = "Username / Email"
    override val passwordField: String = "Password"
    override val websiteUrl: String = "Website / URL"
    override val generatePassword: String = "Generate Secure Password"
    override val copyPassword: String = "Copy Password"
    override val passwordCopied: String = "Password copied to clipboard"
    override val showPassword: String = "Show Password"
    override val hidePassword: String = "Hide Password"
    override val folder: String = "Folder"
    override val tags: String = "Tags (comma separated)"
    override val favorite: String = "Favorite"
    override val pin: String = "Pin to Top"
    override val moveToTrash: String = "Move to Trash"
    override val restore: String = "Restore Item"
    override val permanentDelete: String = "Destroy Permanently"
    override val emptyTrash: String = "Empty Trash"
    override val trashEmptyConfirm: String = "Empty all items in trash? This cannot be undone."
    override val permanentDeleteConfirm: String = "Permanently destroy this item? It will be cryptographically erased."
    override val save: String = "Save"
    override val cancel: String = "Cancel"
    override val edit: String = "Edit"
    override val preview: String = "Secure Preview"
    override val exportFile: String = "Export Decrypted Copy"
    override val exportBackup: String = "Export Encrypted Backup"
    override val importBackup: String = "Restore from Backup"
    override val autoLock: String = "Auto-Lock Timeout"
    override val language: String = "Application Language"
    override val securityScore: String = "Security Health Score"
    override val passwordAudit: String = "Credential Health Audit"
    override val weakPasswordsFound: String = "weak password(s) found"
    override val reusedPasswordsFound: String = "reused password(s) detected"
    override val allPasswordsStrong: String = "All credentials meet high entropy standards"
    override val encryptionStandard: String = "Hardware-backed AES-256-GCM cipher with PBKDF2 derivation"
    override val zeroTelemetryGuarantee: String = "Zero-knowledge architecture: 0 network calls, 0 third-party trackers, no cloud sync."
    override val localFirstNotice: String = "All encrypted payloads and keys reside strictly in your device's isolated app storage."
    override val changeMasterPassword: String = "Change Master Password"
    override val wipeVault: String = "Wipe All Vault Data"
    override val wipeVaultConfirm: String = "Are you absolutely sure? All encrypted records, notes, and files will be permanently erased and cannot be recovered."
    override val noItemsFound: String = "No vault items match your query"
    override val all: String = "All"
    override val category: String = "Category"
    override val addField: String = "Add Custom Field"
    override val fieldName: String = "Field Label"
    override val fieldValue: String = "Field Value"
    override val isSensitive: (Boolean) -> String = { sensitive -> if (sensitive) "Protected (Sensitive)" else "Standard" }
    override val securitySetupDisclosure: String = "Your Master Password is the sole encryption key for this vault. VaultX does not possess or store any backdoors or recovery servers. If forgotten, your data cannot be retrieved."
    override val securityLockedDisclosure: String = "This vault is protected using AES-256-GCM authenticated encryption. Payloads remain strictly encrypted on disk until unlocked with your master key."
    override val hardwareCryptoReassurance: String = "PBKDF2 key derivation (65,536 rounds) + Hardware Keystore isolation"
    override val switchLanguage: String = "عربي"
    override val auditLogsTitle: String = "Security Audit Logs"
    override val dangerZoneTitle: String = "Danger Zone"
    override val privacyArchitectureTitle: String = "Zero-Knowledge Architecture"
    override val privacyArchitectureSubtitle: String = "Technical verification of our privacy guarantees"
    override val privacyQ1Title: String = "Where is my data stored?"
    override val privacyQ1Answer: String = "100% on your device. VaultX uses an isolated Room SQLite database where payloads, file blobs, and credentials are encrypted prior to writing to disk."
    override val privacyQ2Title: String = "Can the developers or Google see my data?"
    override val privacyQ2Answer: String = "No. VaultX has no analytics SDKs, no crash reporting servers, and no telemetry pings. It operates in pure offline mode."
    override val privacyQ3Title: String = "What encryption algorithms are used?"
    override val privacyQ3Answer: String = "AES-256-GCM with a 128-bit authentication tag, 96-bit random IVs per payload, and key derivation via PBKDF2WithHmacSHA256 across 65,536 rounds."
    override val privacyQ4Title: String = "What happens if I lose my Master Password?"
    override val privacyQ4Answer: String = "Your data cannot be decrypted by anyone without the Master Password or an exported Emergency Recovery Kit. VaultX does not maintain password reset mechanisms."
    override val privacyQ5Title: String = "Are temporary decrypted files leaked to disk?"
    override val privacyQ5Answer: String = "No. Decryption operations occur in memory. Any preview caches created for third-party view intents are cleared when leaving the screen or locking the vault."
    override val privacyQ6Title: String = "How does Privacy Mode protect against shoulder-surfing?"
    override val privacyQ6Answer: String = "Privacy Mode masks text fields, obscures thumbnails, prevents system screenshots via FLAG_SECURE, and suppresses sensitive details in background app previews."

    override val lockOnBackground: String = "Lock on Background"
    override val lockOnBackgroundDesc: String = "Instantly lock vault when app leaves foreground"
    override val lockOnBackgroundWarning: String = "Warning: Leaving this disabled allows the vault to remain unlocked when switching apps."
    override val reducedMotion: String = "Reduced Motion"
    override val reducedMotionDesc: String = "Disable animations for immediate screen transitions"

    override val protectWithSeparatePassword: String = "Protect with separate password"
    override val filePassword: String = "File Password"
    override val confirmFilePassword: String = "Confirm File Password"
    override val unlockFile: String = "Unlock File"
    override val protectedFileTitle: String = "Password Protected File"
    override val protectedFileSubtitle: String = "This file is encrypted with its own separate password. Enter the correct password to decrypt and view."
    override val unableToUnlockFile: String = "Unable to unlock this file. Please verify the password and try again."
    override val changeFilePassword: String = "Change File Password"
    override val removeFilePassword: String = "Remove File Password Protection"
    override val addFilePassword: String = "Add Password Protection"
    override val currentFilePassword: String = "Current File Password"
    override val newFilePassword: String = "New File Password"
    override val removePasswordConfirmDesc: String = "Enter the current file password to remove protection. The file will remain protected by your Master Vault key."
    override val filePasswordChanged: String = "File password changed successfully"
    override val filePasswordRemoved: String = "File password protection removed"
    override val filePasswordAdded: String = "Password protection added to file"
    override val protectedBadge: String = "Protected"

    override val previewUnavailable: String = "Preview unavailable for this format"
    override val previewUnavailableDesc: String = "This file type cannot be previewed in-app, but you can securely export it to another application."
    override val downloadSecurely: String = "Export File Securely"
    override val zoomIn: String = "Zoom In"
    override val zoomOut: String = "Zoom Out"
    override val rotate: String = "Rotate"
    override val fitToScreen: String = "Fit to Screen"
    override val pageOf: (Int, Int) -> String = { current, total -> "Page $current of $total" }
    override val archiveContents: String = "Archive Contents"
    override val archiveFileCount: (Int) -> String = { count -> "$count items inside archive" }
    override val codeViewer: String = "Code / Text Viewer"
    override val linesCount: (Int) -> String = { lines -> "$lines lines" }

    override val aiTools: String = "AI Tools"
    override val aiFileOrganizer: String = "Smart File Organizer"
    override val aiDocumentUnderstanding: String = "Document Intelligence"
    override val aiNaturalLanguageSearch: String = "Natural Language Search"
    override val analyzeAndOrganize: String = "Analyze & Suggest Categories"
    override val applyApprovedSuggestions: String = "Apply Approved Changes"
    override val aiConsentRequiredTitle: String = "External AI Service Privacy Notice"
    override val aiConsentRequiredMessage: String = "This feature may send extracted text of selected files to an external AI service (Gemini). The vault encryption key is never shared. Proceed?"
    override val allowCloudAi: String = "Allow Cloud AI"
    override val cancelCloudAi: String = "Use Local Offline Only"
    override val localIntelligenceMode: String = "Local Offline AI Active"
    override val documentSummary: String = "Document Summary"
    override val importantDates: String = "Extracted Dates"
    override val tasksAndActions: String = "Tasks & Action Items"
    override val keyTopics: String = "Key Topics"
    override val outline: String = "Document Outline"
    override val searchQueryPlaceholder: String = "e.g. Where is my medical receipt or tax return?"
    override val matchReason: String = "Match Reason"

    override val duplicateDetector: String = "Duplicate File Detector"
    override val scanDuplicates: String = "Scan for Identical Files"
    override val noDuplicatesFound: String = "No duplicate files found in your vault"
    override val duplicatesDetected: (Int) -> String = { count -> "Found $count sets of duplicate files" }
    override val originalFile: String = "Original File"
    override val duplicateCopies: String = "Duplicates"
    override val keepOneMoveOthersToTrash: String = "Move selected duplicates to Trash"
    override val duplicateScanProgress: (Int, Int) -> String = { current, total -> "Hashing: $current of $total files..." }

    override val recoveryKit: String = "Emergency Recovery Kit"
    override val createRecoveryKit: String = "Generate Recovery Kit"
    override val verifyRecoveryKit: String = "Verify Recovery Kit"
    override val recoveryKitExplanation: String = "The Recovery Kit contains cryptographic metadata and an integrity checksum required to recover access in emergencies. It never contains your Master Password in plaintext."
    override val recoveryKitWarningNotice: String = "Store this Recovery Kit securely in an offline location. You will always need your Master Password to decrypt your records."
    override val exportRecoveryKitJson: String = "Export Recovery Kit (.json)"
    override val pasteRecoveryKitJson: String = "Paste Recovery Kit JSON to verify"
    override val recoveryKitVerifiedSuccess: String = "Recovery Kit verified successfully! Cryptographic integrity matches."
    override val recoveryKitVerifiedFailed: String = "Verification failed: Corrupted payload or mismatched vault ID."

    override val securityActivity: String = "Security Activity"
    override val accessAttemptsTitle: String = "Access Attempts & Throttling"
    override val consecutiveFailuresCount: (Int) -> String = { count -> "$count consecutive failed attempts" }
    override val rateLimitLockoutNotice: (Int) -> String = { sec -> "Too many failed attempts. Mandatory security throttle: please wait $sec seconds." }
    override val filterAll: String = "All"
    override val filterSuccessful: String = "Success"
    override val filterFailed: String = "Failed"
    override val filterUnlocks: String = "Unlocks"
    override val filterRecovery: String = "Recovery"
    override val filterBackups: String = "Backups"
    override val filterProtectedFiles: String = "Protected Files"
    override val filterSecurityChanges: String = "Security Changes"

    override val privacyMode: String = "Privacy Mode"
    override val privacyModeOn: String = "Privacy Mode: ON"
    override val privacyModeOff: String = "Privacy Mode: OFF"
    override val privacyModeDescription: String = "Mask sensitive titles, obscure thumbnails, and redact private values on screen to prevent shoulder-surfing."
    override val temporarilyReveal: String = "Temporarily Reveal"
    override val sensitiveValueMasked: String = "••••••••••••"
    override val previewMaskedNotice: String = "Privacy Mode is ON. Preview content is masked for protection."

    override val smartCollections: String = "Smart Collections"
    override val collectionAll: String = "All Files"
    override val collectionDocuments: String = "Documents"
    override val collectionImages: String = "Images"
    override val collectionVideos: String = "Videos"
    override val collectionAudio: String = "Audio"
    override val collectionProtected: String = "Password Protected"
    override val collectionFavorites: String = "Favorites"
    override val collectionRecentAdded: String = "Recently Added"
    override val collectionRecentOpened: String = "Recently Viewed"

    // Common additions
    override val confirm: String = "Confirm"
    override val active: String = "ACTIVE"
    override val close: String = "Close"
    override val options: String = "Options"
    override val menu: String = "Menu"
    override val unfavorite: String = "Unfavorite"
    override val unpin: String = "Unpin"
    override val pinned: String = "Pinned"
    override val delete: String = "Delete"
    override val clear: String = "Clear"
    override val search: String = "Search"
    override val shield: String = "Security Shield"
    override val lockout: String = "Security Lockout"
    override val error: String = "Error"
    override val reveal: String = "Reveal"
    override val hide: String = "Hide"

    // Settings additions
    override val preferencesAndSecurity: String = "Preferences & Security"
    override val securityOperations: String = "Security Operations"
    override val changePasswordDesc: String = "Re-encrypts all vault items with a new derived key"
    override val newMasterPasswordLabel: String = "New Master Password"
    override val confirmNewPasswordLabel: String = "Confirm New Password"
    override val changePasswordExplanation: String = "Enter a new master password. All existing encrypted items will be immediately decrypted and re-encrypted with the new key."
    override val passwordChangedSuccess: String = "Master password changed & vault re-encrypted"
    override val wipeVaultSubtitle: String = "Irreversibly destroys master key and all encrypted data"
    override val wipeVaultConfirmButton: String = "Yes, Wipe Everything"
    override val wipeVaultErasedToast: String = "Vault has been completely erased."
    override val auditLogsRecordedCount: (Int) -> String = { count -> if (count == 1) "1 item recorded" else "$count items recorded" }
    override val noAuditLogs: String = "No audit logs yet"
    override val privacyModeEnabledToast: String = "Privacy Mode enabled: UI masked & activity sanitized."
    override val privacyModeDisabledToast: String = "Privacy Mode disabled."
    override val timeoutMinutes: (Int) -> String = { min -> "$min minute" + if (min == 1) "" else "s" }
    override val timeoutNever: String = "Never"

    // Generator additions
    override val passwordGenerator: String = "Password Generator"
    override val regenerate: String = "Regenerate"
    override val lengthWithVal: (Int) -> String = { len -> "Length: $len" }
    override val uppercaseChars: String = "Uppercase (A-Z)"
    override val lowercaseChars: String = "Lowercase (a-z)"
    override val numberChars: String = "Numbers (0-9)"
    override val symbolChars: String = "Symbols (!@#$)"
    override val strengthWithEntropy: (String, Int) -> String = { label, entropy -> "$label ($entropy bits entropy)" }
    override val strengthTip: (String) -> String = { tip -> "Tip: $tip" }
    override val strengthVeryWeak: String = "Very Weak"
    override val strengthWeak: String = "Weak"
    override val strengthFair: String = "Fair"
    override val strengthStrong: String = "Strong"
    override val strengthVeryStrong: String = "Very Strong"

    // Dashboard additions
    override val vaultProtected: String = "Vault Protected"
    override val gcm256: String = "256-bit GCM"
    override val autoLockLabel: String = "Auto-Lock"
    override val backupLabel: String = "Backup"
    override val backupActive: String = "Active"
    override val backupPending: String = "Pending"
    override val storageLabel: String = "Storage"
    override val storageLocalOnly: String = "Local-Only"
    override val encryptedItems: String = "Encrypted Items"
    override val secureNotes: String = "Secure Notes"
    override val weakCountLabel: (Int) -> String = { count -> "$count weak" }
    override val allStrongLabel: String = "All Strong"
    override val dashboardEmptyDesc: String = "Add files, notes, passwords, or records using the buttons above"
    override val maskPlaceholderVault: String = "Vault"
    override val maskPlaceholderProtected: String = " • Protected"

    // Files additions
    override val cannotReadEmptyFile: String = "Cannot read empty file"
    override val errorImportingFile: (String) -> String = { err -> "Error importing file: $err" }
    override val fileExportedSuccess: String = "File exported successfully"
    override val exportError: (String) -> String = { err -> "Export error: $err" }
    override val failedDecryptPreview: String = "Failed to decrypt file for preview"
    override val failedDecryptExport: String = "Failed to decrypt file for export"
    override val filesHeaderSubtitle: String = "Store photos, PDFs, documents, audio, and videos securely with zero-knowledge AES-256 encryption."
    override val duplicateInfoSubtitle: (String, Int) -> String = { size, count -> "$size • $count duplicate copies" }
    override val movedDuplicatesToTrash: (Int) -> String = { count -> "Moved $count duplicates to Trash" }
    override val storageDuplicatesCleaned: String = "Storage duplicates cleaned up."
    override val fileEncryptedToast: String = "File encrypted & secured"
    override val vaultItemSecured: String = "Vault item secured."
    override val moveToTrashConfirmMsg: (String) -> String = { title -> "Move '$title' to Trash? You can restore it later." }
    override val filePasswordMinLength: String = "Password must be at least 6 characters"
    override val encryptAndSave: String = "Encrypt & Save"

    // Notes additions
    override val noNotesTitle: String = "No Encrypted Notes"
    override val noNotesDesc: String = "Keep personal thoughts, sensitive memos, recovery instructions, and confidential lists encrypted."
    override val notesCount: (Int) -> String = { count -> "$count Secure Note" + if (count == 1) "" else "s" }
    override val noteCopiedToast: String = "Note copied to clipboard"
    override val noteCopiedGeneric: String = "Note content copied."
    override val noteContentMaskedPlaceholder: String = "🔒 Note content masked (Tap to reveal)"
    override val updatedAtLabel: (String) -> String = { date -> "Updated: $date" }
    override val editNoteTitle: String = "Edit Note"
    override val folderCategoryLabel: String = "Folder / Category"
    override val wordsAndCharsCount: (Int, Int) -> String = { words, chars -> "$words words • $chars chars" }
    override val untitledNote: String = "Untitled Note"
    override val moveToTrashNoteConfirm: (String) -> String = { title -> "Move note '$title' to Trash?" }

    // Passwords additions
    override val noPasswordsTitle: String = "No Saved Passwords"
    override val noPasswordsDesc: String = "Secure your online accounts, banking credentials, emails, and generated passwords safely."
    override val passwordsCount: (Int) -> String = { count -> "$count Stored Credential" + if (count == 1) "" else "s" }
    override val editPasswordTitle: String = "Edit Password"
    override val serviceTitlePlaceholder: String = "Service / Title (e.g. Google, GitHub)"
    override val privateNotesOptional: String = "Private Notes (Optional)"
    override val untitledAccount: String = "Untitled Account"
    override val moveToTrashPasswordConfirm: (String) -> String = { title -> "Move credential '$title' to Trash?" }
    override val editCredentialAction: String = "Edit Credential"
    override val credentialCopiedToast: String = "Credential copied."

    // Records additions
    override val noRecordsTitle: String = "No Secure Records"
    override val noRecordsDesc: String = "Store passports, bank accounts, credit cards, Wi-Fi networks, and identity documents securely."
    override val recordsCount: (Int) -> String = { count -> "$count Secure Record" + if (count == 1) "" else "s" }
    override val fieldsCountSummary: (String, Int) -> String = { cat, count -> "$cat • $count field" + if (count == 1) "" else "s" }
    override val fieldCopiedToast: (String) -> String = { label -> "$label copied" }
    override val recordFieldCopiedGeneric: String = "Record field copied."
    override val notesWithVal: (String) -> String = { notes -> "Notes: $notes" }
    override val editRecordTitle: String = "Edit Record"
    override val fieldsHeader: String = "Fields"
    override val fieldLabel: String = "Label"
    override val fieldValueLabel: String = "Value"
    override val fieldDefault: String = "Field"
    override val moveToTrashRecordConfirm: (String) -> String = { title -> "Move record '$title' to Trash?" }
    override val categoryPassport: String = "Passport"
    override val categoryDriversLicense: String = "Driver's License"
    override val categoryBankAccount: String = "Bank Account"
    override val categoryCreditCard: String = "Credit Card"
    override val categoryWifiNetwork: String = "Wi-Fi Network"
    override val categorySoftwareLicense: String = "Software License"
    override val categoryCryptoWallet: String = "Crypto Wallet"
    override val categoryOther: String = "Other"
    override val fieldPassportNumber: String = "Passport Number"
    override val fieldCountry: String = "Country"
    override val fieldExpirationDate: String = "Expiration Date"
    override val fieldFullName: String = "Full Name"
    override val fieldLicenseNumber: String = "License Number"
    override val fieldStateProvince: String = "State / Province"
    override val fieldBankName: String = "Bank Name"
    override val fieldAccountNumber: String = "Account Number"
    override val fieldRoutingSwift: String = "Routing / SWIFT"
    override val fieldIban: String = "IBAN"
    override val fieldCardholderName: String = "Cardholder Name"
    override val fieldCardNumber: String = "Card Number"
    override val fieldCvvCvc: String = "CVV / CVC"
    override val fieldPin: String = "PIN"
    override val fieldNetworkName: String = "Network Name (SSID)"
    override val fieldProductName: String = "Product Name"
    override val fieldLicenseKey: String = "License Key"
    override val fieldRecoverySeed: String = "Recovery Seed / Mnemonic"
    override val fieldDerivationPath: String = "Derivation Path / Notes"
    override val fieldDetail: String = "Detail"

    // Trash additions
    override val trashEmptyTitle: String = "Trash is Empty"
    override val trashEmptyDesc: String = "Items moved to trash will appear here and can be recovered or permanently destroyed."
    override val trashItemsCount: (Int) -> String = { count -> "$count Item" + (if (count == 1) "" else "s") + " in Trash" }
    override val deletedDateLabel: (String) -> String = { date -> "Deleted: $date" }

    // Backup additions
    override val failedCreateBackupPackage: String = "Failed to create backup package"
    override val backupSavedToast: String = "Encrypted backup saved (.vaultx)"
    override val backupPackageGenerated: String = "Vault backup package generated."
    override val invalidBackupToast: String = "Invalid or corrupted .vaultx backup file"
    override val errorReadingBackup: (String) -> String = { err -> "Error reading backup: $err" }
    override val generateKitButton: String = "Generate Kit"
    override val recoveryKitGeneratedToast: String = "Recovery Kit generated successfully"
    override val restoreBackupTitle: String = "Restore Encrypted Backup"
    override val restoreBackupMessage: (Int, String) -> String = { count, date -> "Found backup containing $count encrypted items exported on $date.\n\nRestoring will merge items into your current vault. Continue?" }
    override val restoreNowButton: String = "Restore Now"
    override val restoredItemsSuccess: (Int) -> String = { count -> "Successfully restored $count items" }
    override val restoreCompletedGeneric: String = "Vault restore operation completed."
    override val generatedRecoveryKitJsonTitle: String = "Generated Recovery Kit (JSON)"
    override val vaultIdLabel: (String) -> String = { id -> "Vault ID: $id" }
    override val checksumLabel: (String) -> String = { sum -> "Checksum: $sum" }
    override val recoveryKitCopiedToast: String = "Recovery Kit copied to clipboard"
    override val recoveryDataCopiedGeneric: String = "Recovery data copied."
    override val copyJsonButton: String = "Copy JSON"
    override val exportEncryptedBackupTitle: String = "Export Encrypted Backup"
    override val exportEncryptedBackupDesc: String = "Creates an encrypted .vaultx package containing your items, folders, and tags. Protected with AES-256-GCM using your master key."
    override val importRestoreBackupTitle: String = "Import & Restore Backup"
    override val importRestoreBackupDesc: String = "Restore an existing .vaultx backup file. You will be prompted to confirm before any data is merged into your vault."
    override val verifyKitDesc: String = "Safely test and verify that your Recovery Kit is valid, uncorrupted, and cryptographically compatible with your vault. No data is modified during verification."
    override val invalidJsonError: String = "Invalid JSON syntax. Ensure complete Recovery Kit payload is pasted."
    override val verifyIntegrityButton: String = "Verify Integrity"
    override val recoveryKitValid: String = "Recovery Kit Valid"
    override val recoveryHistoryTitle: String = "Recovery & Security History"
    override val noRecoveryHistory: String = "No recovery events recorded yet."

    // Security Center additions
    override val healthExcellent: String = "Excellent"
    override val healthGood: String = "Good"
    override val healthAttention: String = "Attention Needed"
    override val healthCritical: String = "Critical Issues"
    override val vaultHealth: (String) -> String = { label -> "Vault Health: $label" }
    override val securitySubtitle: String = "Real-time cryptographic, credential & storage audit"
    override val rateLimitedStatus: String = "Rate Limited"
    override val normalStatus: String = "Normal"
    override val zeroFailuresDesc: String = "Zero consecutive failures. Active session is authenticated and secure."
    override val weakPasswordsTitle: String = "Weak Passwords"
    override val noWeakPasswords: String = "No weak passwords found"
    override val reviewPasswordsAction: String = "Review Passwords"
    override val reusedPasswordsTitle: String = "Reused Passwords"
    override val noReusedPasswords: String = "No duplicate credentials found"
    override val resolveDuplicatesAction: String = "Resolve Duplicates"
    override val accessProtectionTitle: String = "Access Protection"
    override val autoLockActiveStatus: (Int) -> String = { min -> "Active: locks after $min minutes of inactivity" }
    override val autoLockDisabledWarning: String = "Warning: Auto-lock is disabled!"
    override val configureAction: String = "Configure"
    override val pbkdf2IterationsDesc: String = "Master key derived with PBKDF2 (65,536 iterations). Change anytime to re-encrypt all records."
    override val updateKeyAction: String = "Update Key"
    override val noSecurityEventsFound: String = "No security events match selected filter."

    // Universal Preview additions
    override val play: String = "Play"
    override val pause: String = "Pause"
    override val rewind10s: String = "Rewind 10s"
    override val forward10s: String = "Forward 10s"
    override val previousPage: String = "Previous Page"
    override val nextPage: String = "Next Page"
    override val unableToDecodeText: String = "Unable to decode text (non-UTF8)."
    override val mimeTypeLabel: String = "MIME Type"
    override val sizeLabel: String = "Size"
    override val sha256Label: String = "SHA-256"
    override val encryptedContentProtected: String = "Encrypted Content • Protected"

    // TopBar & Privacy additions
    override val privacyOnBadge: String = "PRIVACY ON"
    override val aesGcmBadge: String = "AES-GCM"
    override val languageToggleLabel: String = "عربي"
    override val sampleFileName: String = "Bank Statement September.pdf"
    override val sampleFileSizeSensitive: String = "Size: 2.4 MB • Sensitive: Financial"
    override val sampleBadge: String = "Sample"
    override val maskedBadge: String = "Masked"

    // AI screen additions
    override val geminiCloudApproved: String = "Gemini Cloud Intelligence (Authorized)"
    override val allowGeminiCloudSwitch: String = "Allow Gemini Cloud AI"
    override val aiFileOrganizerHelp: String = "Click 'Analyze & Organize' to generate smart suggestions for classifying files based on context."
    override val aiDocAiHelp: String = "Select an item from your vault for smart summarization, dates extraction, action items, or analytical questions."
    override val selectDocumentPlaceholder: String = "Select a document from vault..."
    override val analysisResult: (String) -> String = { op -> "Analysis Result ($op)" }
    override val aiSearchHelp: String = "Search in natural language for warranty papers, exams, invoices, or passports."
    override val matchScore: (Int) -> String = { score -> "Match: $score%" }
    override val aiPrivacyPolicyTitle: String = "VaultX AI Privacy Policy"
    override val aiPrivacyPolicyBody: String = "• Default Local Mode: File classification and semantic search run locally on your device without sending any bytes outside.\n\n• Gemini Cloud: If you wish to analyze long document texts, you can manually enable cloud AI with your explicit consent.\n\n• Zero-Knowledge: Passwords, encryption keys, or private file contents are never sent to third parties."
    override val filesCount: (Int) -> String = { count -> if (count == 1) "1 file" else "$count files" }
    override val duplicateCopiesLabel: (Int) -> String = { count -> if (count == 1) "1 duplicate copy" else "$count duplicate copies" }

    override val defaultDocumentName: String = "document"
    override val defaultFolderLogins: String = "Logins"
    override val defaultFolderNotes: String = "Notes"
    override val defaultFolderRecords: String = "Records"
    override val recoveryKitInvalid: String = "Verification Failed"
    override val recoveryKitInvalidJson: String = "Invalid format. Ensure complete Recovery Kit payload is pasted."
    override val recoveryKitMatchSuccess: String = "Cryptographic signature matches current vault."
    override val recoveryKitMismatch: String = "Recovery kit belongs to a different vault or configuration."
    override val failedUnlockNewVault: String = "Failed to unlock newly created vault"
    override val cannotDecodeText: String = "Unable to decode text (non-UTF8)."
    override val archiveEntrySize: (String, String) -> String = { size, comp -> "Size: $size (Compressed: $comp)" }
    override val tipAtLeast12Chars: String = "Use at least 12 characters"
    override val tipAddLowercase: String = "Add lowercase letters"
    override val tipAddUppercase: String = "Add uppercase letters"
    override val tipAddDigits: String = "Add numeric digits"
    override val tipAddSymbols: String = "Add special symbols"
    override val tipPasswordCannotBeEmpty: String = "Password cannot be empty"
    override fun formatBytes(bytes: Long): String = VaultI18n.formatBytes(bytes, "en")
    override fun formatDate(timestamp: Long): String = VaultI18n.formatDate(timestamp, "en")
}

internal class ArabicVaultStrings : VaultStrings {
    override val appName: String = "VaultX"
    override val vaultLocked: String = "الخزنة مقفلة"
    override val enterMasterPassword: String = "أدخل كلمة المرور الرئيسية"
    override val unlock: String = "فتح الخزنة"
    override val createVault: String = "إنشاء خزنتك الآمنة"
    override val setupMasterPassword: String = "تعيين كلمة المرور الرئيسية"
    override val confirmPassword: String = "تأكيد كلمة المرور الرئيسية"
    override val passwordMismatch: String = "كلمتا المرور غير متطابقتين"
    override val passwordMinLength: String = "يجب ألا تقل كلمة المرور عن 8 أحرف"
    override val invalidPassword: String = "فشل التحقق. كلمة المرور الرئيسية غير صحيحة."
    override val lockNow: String = "قفل الخزنة الآن"
    override val dashboard: String = "لوحة التحكم"
    override val files: String = "الملفات"
    override val notes: String = "الملاحظات"
    override val passwords: String = "كلمات المرور"
    override val records: String = "السجلات"
    override val securityCenter: String = "مركز الأمان"
    override val privacyCenter: String = "مركز الخصوصية"
    override val backupRestore: String = "النسخ الاحتياطي"
    override val trash: String = "المهملات"
    override val settings: String = "الإعدادات"
    override val searchVault: String = "البحث في الخزنة المشفرة..."
    override val totalItems: String = "إجمالي العناصر"
    override val storageUsed: String = "المساحة المستخدمة"
    override val recentItems: String = "أحدث الأنشطة"
    override val quickActions: String = "إجراءات سريعة"
    override val newFile: String = "رفع ملف مشفر"
    override val newNote: String = "ملاحظة جديدة"
    override val newPassword: String = "حساب جديد"
    override val newRecord: String = "سجل جديد"
    override val title: String = "العنوان"
    override val content: String = "المحتوى"
    override val username: String = "اسم المستخدم / البريد"
    override val passwordField: String = "كلمة المرور"
    override val websiteUrl: String = "الموقع الإلكتروني / الرابط"
    override val generatePassword: String = "توليد كلمة مرور قوية"
    override val copyPassword: String = "نسخ كلمة المرور"
    override val passwordCopied: String = "تم نسخ كلمة المرور إلى الحافظة"
    override val showPassword: String = "إظهار كلمة المرور"
    override val hidePassword: String = "إخفاء كلمة المرور"
    override val folder: String = "المجلد"
    override val tags: String = "الوسوم (مفصولة بفواصل)"
    override val favorite: String = "إضافة للمفضلة"
    override val pin: String = "تثبيت في الأعلى"
    override val moveToTrash: String = "نقل إلى المهملات"
    override val restore: String = "استعادة العنصر"
    override val permanentDelete: String = "حذف نهائي فوري"
    override val emptyTrash: String = "إفراغ سلة المهملات"
    override val trashEmptyConfirm: String = "هل أنت متأكد من رغبتك في إفراغ سلة المهملات؟ لا يمكن التراجع عن هذا الإجراء."
    override val permanentDeleteConfirm: String = "تدمير هذا العنصر نهائياً؟ سيتم محوه تشفيرياً بلا رجعة."
    override val save: String = "حفظ"
    override val cancel: String = "إلغاء"
    override val edit: String = "تعديل"
    override val preview: String = "معاينة آمنة"
    override val exportFile: String = "تصدير نسخة مفكوكة التشفير"
    override val exportBackup: String = "تصدير نسخة احتياطية مشفرة"
    override val importBackup: String = "استعادة من نسخة احتياطية"
    override val autoLock: String = "مهلة القفل التلقائي"
    override val language: String = "لغة التطبيق"
    override val securityScore: String = "مستوى الأمان التشفيري"
    override val passwordAudit: String = "تدقيق سلامة كلمات المرور"
    override val weakPasswordsFound: String = "كلمات مرور ضعيفة مكتشفة"
    override val reusedPasswordsFound: String = "كلمات مرور مكررة مكتشفة"
    override val allPasswordsStrong: String = "كافة كلمات المرور تحقق أعلى معايير الأمان والإنتروبيا"
    override val encryptionStandard: String = "تشفير عتادي متقدم AES-256-GCM مع اشتقاق PBKDF2"
    override val zeroTelemetryGuarantee: String = "معمارية المعرفة الصفرية: لا توجد اتصالات شبكية، ولا أدوات تتبع، ولا مزامنة سحابية."
    override val localFirstNotice: String = "جميع الحزم المشفرة والمفاتيح محفوظة حصرياً ومحلياً في التخزين المعزول لجهازك."
    override val changeMasterPassword: String = "تغيير كلمة المرور الرئيسية"
    override val wipeVault: String = "مسح جميع بيانات الخزنة"
    override val wipeVaultConfirm: String = "تحذير حاسم: سيتم محو كافة الملفات والملاحظات وكلمات المرور نهائياً وتدمير مفاتيح التشفير تماماً. لن تتمكن من استعادتها أبداً."
    override val noItemsFound: String = "لم يتم العثور على عناصر تطابق بحثك"
    override val all: String = "الكل"
    override val category: String = "التصنيف"
    override val addField: String = "إضافة حقل مخصص"
    override val fieldName: String = "اسم الحقل"
    override val fieldValue: String = "قيمة الحقل"
    override val isSensitive: (Boolean) -> String = { sensitive -> if (sensitive) "محمي (حساس)" else "قياسي" }
    override val securitySetupDisclosure: String = "كلمة المرور الرئيسية هي المفتاح الوحيد لفك تشفير هذه الخزنة. لا يمتلك فولت إكس أي خوادم أو أبواب خلفية لاستعادة البيانات. في حال نسيانها، لا يمكن استرجاع بياناتك نهائياً."
    override val securityLockedDisclosure: String = "هذه الخزنة مؤمنة بالكامل بتشفير AES-256-GCM المقيد. تظل جميع البيانات مشفرة على وحدة التخزين حتى إدخال المفتاح الرئيسي."
    override val hardwareCryptoReassurance: String = "اشتقاق مفاتيح PBKDF2 (بـ 65,536 دورة) + عزل عتادي كامل"
    override val switchLanguage: String = "English"
    override val auditLogsTitle: String = "سجلات التدقيق الأمني"
    override val dangerZoneTitle: String = "منطقة العمليات الحرجة"
    override val privacyArchitectureTitle: String = "معمارية المعرفة الصفرية"
    override val privacyArchitectureSubtitle: String = "التحقق التقني من ضمانات الخصوصية والأمان"
    override val privacyQ1Title: String = "أين تُخزّن بياناتي وملفاتي؟"
    override val privacyQ1Answer: String = "100% محلياً على جهازك. يستخدم فولت إكس قاعدة بيانات SQLite مشفرة ومعزولة حيث تُشفر كافة النصوص والملفات والبيانات الحساسة قبل كتابتها إلى التخزين."
    override val privacyQ2Title: String = "هل يمكن للمطورين أو لجوجل الاطلاع على بياناتي؟"
    override val privacyQ2Answer: String = "كلا نهائياً. لا يحتوي فولت إكس على أي حزم تتبع أو أدوات تحليل أو خوادم لتسجيل الأخطاء. يعمل التطبيق محلياً وبشكل منفصل تماماً."
    override val privacyQ3Title: String = "ما هي الخوارزميات التشفيرية المستخدمة؟"
    override val privacyQ3Answer: String = "تشفير AES-256-GCM مع وسم توثيق 128-بت وقيم IV عشوائية 96-بت لكل عنصر، بالإضافة لاشتقاق المفاتيح عبر PBKDF2WithHmacSHA256 بـ 65,536 دورة."
    override val privacyQ4Title: String = "ماذا يحدث في حال نسيت كلمة المرور الرئيسية؟"
    override val privacyQ4Answer: String = "لا يمكن لأي طرف فك تشفير البيانات دون كلمة المرور الرئيسية أو حقيبة الاستعادة المصدرة مسبقاً. لا يدعم فولت إكس ميزة إعادة تعيين كلمة المرور حمايةً لأمنك."
    override val privacyQ5Title: String = "هل يتم تسريب ملفات مؤقتة غير مشفرة على وحدة التخزين؟"
    override val privacyQ5Answer: String = "كلا. تتم عمليات فك التشفير في الذاكرة العشوائية الحية. أي ملفات مؤقتة تُنشأ للمعاينة يتم محوها وتطهيرها فور مغادرة الشاشة أو قفل الخزنة."
    override val privacyQ6Title: String = "كيف يحمي وضع الخصوصية من التلصص البصري؟"
    override val privacyQ6Answer: String = "يموه وضع الخصوصية أسماء الملفات، ويخفي المعاينات، ويمنع لقطات الشاشة عبر FLAG_SECURE، ويحجب المحتوى أثناء التبديل بين التطبيقات."

    override val lockOnBackground: String = "القفل التلقائي عند مغادرة الواجهة"
    override val lockOnBackgroundDesc: String = "قفل الخزنة فوراً عند الانتقال إلى تطبيق آخر أو إغلاق الشاشة"
    override val lockOnBackgroundWarning: String = "تحذير: تعطيل هذا الخيار يبقي الخزنة مفتوحة ومتاحة عند التبديل بين التطبيقات."
    override val reducedMotion: String = "تقليل الحركة والانتقالات"
    override val reducedMotionDesc: String = "تعطيل الحركات البصرية لتسريع الانتقال المباشر بين الشاشات"

    override val protectWithSeparatePassword: String = "حماية بكلمة مرور منفصلة"
    override val filePassword: String = "كلمة مرور الملف"
    override val confirmFilePassword: String = "تأكيد كلمة مرور الملف"
    override val unlockFile: String = "فتح قفل الملف"
    override val protectedFileTitle: String = "ملف محمي بكلمة مرور خاصة"
    override val protectedFileSubtitle: String = "هذا الملف مؤمن بمفتاح تشفير إضافي ويتطلب إدخال كلمة مروره الخاصة لفتحه ومعاينته."
    override val unableToUnlockFile: String = "تعذر فك قفل هذا الملف. يرجى التحقق من صحة كلمة المرور والمحاولة مجدداً."
    override val changeFilePassword: String = "تغيير كلمة مرور الملف"
    override val removeFilePassword: String = "إلغاء حماية كلمة مرور الملف"
    override val addFilePassword: String = "إضافة حماية بكلمة مرور"
    override val currentFilePassword: String = "كلمة المرور الحالية للملف"
    override val newFilePassword: String = "كلمة المرور الجديدة للملف"
    override val removePasswordConfirmDesc: String = "أدخل كلمة المرور الحالية للملف لإلغاء الحماية المنفصلة. سيظل الملف محمياً بالمفتاح الرئيسي للخزنة."
    override val filePasswordChanged: String = "تم تغيير كلمة مرور الملف بنجاح"
    override val filePasswordRemoved: String = "تمت إزالة الحماية المنفصلة للملف بنجاح"
    override val filePasswordAdded: String = "تمت إضافة حماية بكلمة مرور للملف بنجاح"
    override val protectedBadge: String = "محمي"

    override val previewUnavailable: String = "المعاينة غير متاحة لهذا النوع من الملفات"
    override val previewUnavailableDesc: String = "لا يمكن عرض هذا التنسيق مباشرة داخل التطبيق، ولكن يمكنك تصديره بأمان إلى تطبيق خارجي."
    override val downloadSecurely: String = "تصدير الملف بأمان"
    override val zoomIn: String = "تكبير"
    override val zoomOut: String = "تصغير"
    override val rotate: String = "تدوير"
    override val fitToScreen: String = "ملاءمة الشاشة"
    override val pageOf: (Int, Int) -> String = { current, total -> "صفحة $current من $total" }
    override val archiveContents: String = "محتويات الأرشيف المضغوط"
    override val archiveFileCount: (Int) -> String = { count -> "$count عناصر داخل الأرشيف" }
    override val codeViewer: String = "عارض الأكواد والنصوص"
    override val linesCount: (Int) -> String = { lines -> "$lines أسطر" }

    override val aiTools: String = "أدوات الذكاء الاصطناعي"
    override val aiFileOrganizer: String = "منظّم الملفات الذكي"
    override val aiDocumentUnderstanding: String = "تحليل المستندات الذكي"
    override val aiNaturalLanguageSearch: String = "البحث باللغة الطبيعية"
    override val analyzeAndOrganize: String = "تحليل واقتراح التصنيفات"
    override val applyApprovedSuggestions: String = "تطبيق الاقتراحات المعتمدة"
    override val aiConsentRequiredTitle: String = "إشعار خصوصية الذكاء الاصطناعي الخارجي"
    override val aiConsentRequiredMessage: String = "قد ترسل هذه العملية النص المستخرج من الملف المحدد إلى مزود خارجي (Gemini). لا يتم إرسال مفتاح تشفير الخزنة إطلاقاً. هل ترغب في المتابعة؟"
    override val allowCloudAi: String = "السماح بالذكاء الاصطناعي الخارجي"
    override val cancelCloudAi: String = "استخدام المعالجة المحلية بدون إنترنت فقط"
    override val localIntelligenceMode: String = "الذكاء الاصطناعي المحلي (بدون إنترنت) نشط"
    override val documentSummary: String = "ملخص المستند"
    override val importantDates: String = "التواريخ المستخرجة"
    override val tasksAndActions: String = "المهام والإجراءات"
    override val keyTopics: String = "الموضوعات الرئيسية"
    override val outline: String = "الهيكل والمخطط"
    override val searchQueryPlaceholder: String = "مثال: أين فاتورة الهاتف أو عقد الإيجار؟"
    override val matchReason: String = "سبب التطابق"

    override val duplicateDetector: String = "كاشف الملفات المكررة"
    override val scanDuplicates: String = "فحص الملفات المتطابقة تماماً"
    override val noDuplicatesFound: String = "لم يتم العثور على أي ملفات مكررة في الخزنة"
    override val duplicatesDetected: (Int) -> String = { count -> "تم اكتشاف $count مجموعات لملفات مكررة" }
    override val originalFile: String = "الملف الأصلي"
    override val duplicateCopies: String = "النسخ المكررة"
    override val keepOneMoveOthersToTrash: String = "نقل النسخ المكررة المحددة إلى المهملات"
    override val duplicateScanProgress: (Int, Int) -> String = { current, total -> "حساب البصمات التشفيرية: $current من $total ملفات..." }

    override val recoveryKit: String = "حقيبة استعادة الطوارئ"
    override val createRecoveryKit: String = "إنشاء حقيبة الاستعادة"
    override val verifyRecoveryKit: String = "التحقق من حقيبة الاستعادة"
    override val recoveryKitExplanation: String = "تحتوي حقيبة الاستعادة على المعلمات التشفيرية وبصمة التحقق اللازمة لإعادة بناء هيكل الخزنة في حالات الطوارئ. وهي لا تحتوي إطلاقاً على كلمة المرور الرئيسية."
    override val recoveryKitWarningNotice: String = "احفظ حقيبة الاستعادة هذه في مكان آمن ومنفصل بدون إنترنت. ستحتاج دوماً إلى كلمة المرور الرئيسية لفك تشفير بياناتك."
    override val exportRecoveryKitJson: String = "تصدير حقيبة الاستعادة (.json)"
    override val pasteRecoveryKitJson: String = "الصق كود حقيبة الاستعادة للتحقق"
    override val recoveryKitVerifiedSuccess: String = "تم التحقق من حقيبة الاستعادة بنجاح! التكامل التشفيري متطابق وسليم."
    override val recoveryKitVerifiedFailed: String = "فشل التحقق من حقيبة الاستعادة: بيانات تالفة أو معرّف خزنة غير متطابق."

    override val securityActivity: String = "سجل النشاط الأمني"
    override val accessAttemptsTitle: String = "محاولات الوصول والتأخير التدريجي"
    override val consecutiveFailuresCount: (Int) -> String = { count -> "$count محاولات فاشلة متتالية" }
    override val rateLimitLockoutNotice: (Int) -> String = { sec -> "تم تجاوز عدد المحاولات المسموح بها. تأخير أمني إجباري: يرجى الانتظار $sec ثانية." }
    override val filterAll: String = "الكل"
    override val filterSuccessful: String = "الناجحة"
    override val filterFailed: String = "الفاشلة"
    override val filterUnlocks: String = "فتح الخزنة"
    override val filterRecovery: String = "الاستعادة"
    override val filterBackups: String = "النسخ الاحتياطي"
    override val filterProtectedFiles: String = "الملفات المحمية"
    override val filterSecurityChanges: String = "تغييرات الأمان"

    override val privacyMode: String = "وضع الخصوصية"
    override val privacyModeOn: String = "وضع الخصوصية: مشغّل"
    override val privacyModeOff: String = "وضع الخصوصية: معطّل"
    override val privacyModeDescription: String = "إخفاء أسماء الملفات، تحييد الصور المصغرة، وتمويه القيم الحساسة على الشاشة للحماية من التلصص."
    override val temporarilyReveal: String = "كشف مؤقت"
    override val sensitiveValueMasked: String = "••••••••••••"
    override val previewMaskedNotice: String = "وضع الخصوصية مشغّل. تمت حماية محتوى المعاينة لمنع التلصص."

    override val smartCollections: String = "المجموعات الذكية"
    override val collectionAll: String = "كافة الملفات"
    override val collectionDocuments: String = "المستندات"
    override val collectionImages: String = "الصور"
    override val collectionVideos: String = "الفيديو"
    override val collectionAudio: String = "الصوتيات"
    override val collectionProtected: String = "المحمية بكلمة مرور"
    override val collectionFavorites: String = "المفضلة"
    override val collectionRecentAdded: String = "المضافة حديثاً"
    override val collectionRecentOpened: String = "المعروضة حديثاً"

    // Common additions
    override val confirm: String = "تأكيد"
    override val active: String = "نشط"
    override val close: String = "إغلاق"
    override val options: String = "خيارات"
    override val menu: String = "القائمة"
    override val unfavorite: String = "إزالة من المفضلة"
    override val unpin: String = "إلغاء التثبيت"
    override val pinned: String = "مثبّت"
    override val delete: String = "حذف"
    override val clear: String = "مسح"
    override val search: String = "بحث"
    override val shield: String = "درع الأمان"
    override val lockout: String = "إغلاق أمني مؤقت"
    override val error: String = "خطأ"
    override val reveal: String = "كشف"
    override val hide: String = "إخفاء"

    // Settings additions
    override val preferencesAndSecurity: String = "التفضيلات والأمان"
    override val securityOperations: String = "عمليات الأمان"
    override val changePasswordDesc: String = "إعادة تشفير كافة عناصر الخزنة بمفتاح مشتق جديد"
    override val newMasterPasswordLabel: String = "كلمة المرور الرئيسية الجديدة"
    override val confirmNewPasswordLabel: String = "تأكيد كلمة المرور الجديدة"
    override val changePasswordExplanation: String = "أدخل كلمة مرور رئيسية جديدة. سيتم فوراً فك تشفير كافة العناصر وإعادة تشفيرها بالمفتاح الجديد."
    override val passwordChangedSuccess: String = "تم تغيير كلمة المرور الرئيسية وإعادة تشفير الخزنة بنجاح"
    override val wipeVaultSubtitle: String = "يدمر المفتاح الرئيسي وجميع البيانات المشفرة نهائياً وبلا رجعة"
    override val wipeVaultConfirmButton: String = "نعم، امسح كل شيء"
    override val wipeVaultErasedToast: String = "تم مسح بيانات الخزنة بالكامل."
    override val auditLogsRecordedCount: (Int) -> String = { count ->
        when {
            count == 1 -> "تم تسجيل حدث أمان وتشفير واحد"
            count == 2 -> "تم تسجيل حدثي أمان وتشفير"
            count in 3..10 -> "تم تسجيل $count أحداث أمان وتشفير"
            else -> "تم تسجيل $count حدث أمان وتشفير"
        }
    }
    override val noAuditLogs: String = "لا توجد سجلات تدقيق أمني حتى الآن"
    override val privacyModeEnabledToast: String = "تم تشغيل وضع الخصوصية: تمويه الواجهة وتطهير الأنشطة."
    override val privacyModeDisabledToast: String = "تم تعطيل وضع الخصوصية."
    override val timeoutMinutes: (Int) -> String = { min ->
        when {
            min == 1 -> "دقيقة واحدة"
            min == 2 -> "دقيقتان"
            min in 3..10 -> "$min دقائق"
            else -> "$min دقيقة"
        }
    }
    override val timeoutNever: String = "أبداً"

    // Generator additions
    override val passwordGenerator: String = "مولد كلمات المرور"
    override val regenerate: String = "إعادة التوليد"
    override val lengthWithVal: (Int) -> String = { len -> "الطول: $len" }
    override val uppercaseChars: String = "أحرف كبيرة (A-Z)"
    override val lowercaseChars: String = "أحرف صغيرة (a-z)"
    override val numberChars: String = "أرقام (0-9)"
    override val symbolChars: String = "رموز خاصة (!@#$)"
    override val strengthWithEntropy: (String, Int) -> String = { label, entropy -> "$label ($entropy بت إنتروبيا)" }
    override val strengthTip: (String) -> String = { tip -> "نصيحة: $tip" }
    override val strengthVeryWeak: String = "ضعيف جداً"
    override val strengthWeak: String = "ضعيف"
    override val strengthFair: String = "مقبول"
    override val strengthStrong: String = "قوي"
    override val strengthVeryStrong: String = "قوي جداً"

    // Dashboard additions
    override val vaultProtected: String = "الخزنة محمية ومؤمنة"
    override val gcm256: String = "تشفير 256-بت GCM"
    override val autoLockLabel: String = "القفل التلقائي"
    override val backupLabel: String = "النسخ الاحتياطي"
    override val backupActive: String = "نشط"
    override val backupPending: String = "معلق"
    override val storageLabel: String = "التخزين"
    override val storageLocalOnly: String = "محلي فقط"
    override val encryptedItems: String = "عناصر مشفرة"
    override val secureNotes: String = "ملاحظات سرية"
    override val weakCountLabel: (Int) -> String = { count -> "$count ضعيفة" }
    override val allStrongLabel: String = "جميعها قوية"
    override val dashboardEmptyDesc: String = "أضف ملفات، ملاحظات، كلمات مرور، أو سجلات باستخدام الأزرار أعلاه"
    override val maskPlaceholderVault: String = "خزنة"
    override val maskPlaceholderProtected: String = " • محمي"

    // Files additions
    override val cannotReadEmptyFile: String = "لا يمكن قراءة ملف فارغ"
    override val errorImportingFile: (String) -> String = { err -> "خطأ أثناء استيراد الملف: $err" }
    override val fileExportedSuccess: String = "تم تصدير الملف بأمان بنجاح"
    override val exportError: (String) -> String = { err -> "خطأ أثناء التصدير: $err" }
    override val failedDecryptPreview: String = "فشل فك تشفير الملف للمعاينة"
    override val failedDecryptExport: String = "فشل فك تشفير الملف للتصدير"
    override val filesHeaderSubtitle: String = "احفظ الصور ومستندات PDF والمقاطع الصوتية والمرئية بأمان عبر تشفير AES-256 بمعرفة صفرية."
    override val duplicateInfoSubtitle: (String, Int) -> String = { size, count -> "$size • $count نسخ مكررة" }
    override val movedDuplicatesToTrash: (Int) -> String = { count -> "تم نقل $count ملفات مكررة إلى المهملات" }
    override val storageDuplicatesCleaned: String = "تم تنظيف وتفريغ النسخ المكررة."
    override val fileEncryptedToast: String = "تم تشفير الملف وتأمينه بنجاح"
    override val vaultItemSecured: String = "تم تأمين عنصر الخزنة."
    override val moveToTrashConfirmMsg: (String) -> String = { title -> "نقل '$title' إلى المهملات؟ يمكنك استعادته لاحقاً." }
    override val filePasswordMinLength: String = "يجب ألا تقل كلمة المرور عن 6 أحرف"
    override val encryptAndSave: String = "تشفير وحفظ"

    // Notes additions
    override val noNotesTitle: String = "لا توجد ملاحظات مشفرة"
    override val noNotesDesc: String = "احتفظ بخواطرك ومذكراتك الحساسة وتعليمات الاستعادة والقوائم السرية مشفرة بأمان."
    override val notesCount: (Int) -> String = { count -> "$count ملاحظة مشفرة" }
    override val noteCopiedToast: String = "تم نسخ الملاحظة إلى الحافظة"
    override val noteCopiedGeneric: String = "تم نسخ محتوى الملاحظة."
    override val noteContentMaskedPlaceholder: String = "🔒 محتوى الملاحظة مموّه (انقر للكشف)"
    override val updatedAtLabel: (String) -> String = { date -> "آخر تحديث: $date" }
    override val editNoteTitle: String = "تعديل الملاحظة"
    override val folderCategoryLabel: String = "المجلد / التصنيف"
    override val wordsAndCharsCount: (Int, Int) -> String = { words, chars -> "$words كلمة • $chars حرف" }
    override val untitledNote: String = "ملاحظة بدون عنوان"
    override val moveToTrashNoteConfirm: (String) -> String = { title -> "نقل الملاحظة '$title' إلى المهملات؟" }

    // Passwords additions
    override val noPasswordsTitle: String = "لا توجد كلمات مرور محفوظة"
    override val noPasswordsDesc: String = "احفظ حساباتك على الإنترنت وبياناتك المصرفية وبريدك الإلكتروني بأمان تام."
    override val passwordsCount: (Int) -> String = { count -> "$count بيانات اعتماد محفوظة" }
    override val editPasswordTitle: String = "تعديل كلمة المرور"
    override val serviceTitlePlaceholder: String = "الخدمة / العنوان (مثال: Google، GitHub)"
    override val privateNotesOptional: String = "ملاحظات سرية (اختياري)"
    override val untitledAccount: String = "حساب بدون عنوان"
    override val moveToTrashPasswordConfirm: (String) -> String = { title -> "نقل بيانات الاعتماد '$title' إلى المهملات؟" }
    override val editCredentialAction: String = "تعديل الحساب"
    override val credentialCopiedToast: String = "تم نسخ بيانات الاعتماد."

    // Records additions
    override val noRecordsTitle: String = "لا توجد سجلات آمنة"
    override val noRecordsDesc: String = "احفظ جوازات السفر والحسابات والبطاقات المصرفية وشبكات Wi-Fi والوثائق الشخصية بسرية."
    override val recordsCount: (Int) -> String = { count -> "$count سجل آمن" }
    override val fieldsCountSummary: (String, Int) -> String = { cat, count -> "$cat • $count حقول" }
    override val fieldCopiedToast: (String) -> String = { label -> "تم نسخ $label" }
    override val recordFieldCopiedGeneric: String = "تم نسخ حقل السجل."
    override val notesWithVal: (String) -> String = { notes -> "ملاحظات: $notes" }
    override val editRecordTitle: String = "تعديل السجل"
    override val fieldsHeader: String = "الحقول"
    override val fieldLabel: String = "اسم الحقل"
    override val fieldValueLabel: String = "قيمة الحقل"
    override val fieldDefault: String = "حقل"
    override val moveToTrashRecordConfirm: (String) -> String = { title -> "نقل السجل '$title' إلى المهملات؟" }
    override val categoryPassport: String = "جواز سفر"
    override val categoryDriversLicense: String = "رخصة قيادة"
    override val categoryBankAccount: String = "حساب مصرفي"
    override val categoryCreditCard: String = "بطاقة ائتمان"
    override val categoryWifiNetwork: String = "شبكة واي فاي"
    override val categorySoftwareLicense: String = "ترخيص برمجيات"
    override val categoryCryptoWallet: String = "محفظة عملات رقمية"
    override val categoryOther: String = "أخرى"
    override val fieldPassportNumber: String = "رقم جواز السفر"
    override val fieldCountry: String = "الدولة"
    override val fieldExpirationDate: String = "تاريخ الانتهاء"
    override val fieldFullName: String = "الاسم الكامل"
    override val fieldLicenseNumber: String = "رقم الرخصة"
    override val fieldStateProvince: String = "المنطقة / المحافظة"
    override val fieldBankName: String = "اسم المصرف"
    override val fieldAccountNumber: String = "رقم الحساب"
    override val fieldRoutingSwift: String = "رمز التحويل / SWIFT"
    override val fieldIban: String = "الآيبان (IBAN)"
    override val fieldCardholderName: String = "اسم حامل البطاقة"
    override val fieldCardNumber: String = "رقم البطاقة"
    override val fieldCvvCvc: String = "رمز الأمان CVV/CVC"
    override val fieldPin: String = "الرمز السري (PIN)"
    override val fieldNetworkName: String = "اسم الشبكة (SSID)"
    override val fieldProductName: String = "اسم المنتج"
    override val fieldLicenseKey: String = "مفتاح الترخيص"
    override val fieldRecoverySeed: String = "كلمات استعادة المحفظة (Seed)"
    override val fieldDerivationPath: String = "مسار الاشتقاق / ملاحظات"
    override val fieldDetail: String = "التفاصيل"

    // Trash additions
    override val trashEmptyTitle: String = "سلة المهملات فارغة"
    override val trashEmptyDesc: String = "العناصر المنقولة إلى سلة المهملات ستظهر هنا ويمكنك استعادتها أو حذفها نهائياً."
    override val trashItemsCount: (Int) -> String = { count -> "$count عناصر في المهملات" }
    override val deletedDateLabel: (String) -> String = { date -> "تاريخ الحذف: $date" }

    // Backup additions
    override val failedCreateBackupPackage: String = "فشل إنشاء حزمة النسخة الاحتياطية"
    override val backupSavedToast: String = "تم حفظ النسخة الاحتياطية المشفرة (.vaultx)"
    override val backupPackageGenerated: String = "تم إنشاء حزمة النسخة الاحتياطية بنجاح."
    override val invalidBackupToast: String = "ملف نسخة احتياطية غير صالح أو تالف (.vaultx)"
    override val errorReadingBackup: (String) -> String = { err -> "خطأ أثناء قراءة النسخة الاحتياطية: $err" }
    override val generateKitButton: String = "إنشاء الحقيبة"
    override val recoveryKitGeneratedToast: String = "تم إنشاء حقيبة الاستعادة بنجاح"
    override val restoreBackupTitle: String = "استعادة نسخة احتياطية مشفرة"
    override val restoreBackupMessage: (Int, String) -> String = { count, date -> "تم العثور على نسخة احتياطية تحتوي على $count عناصر مشفرة تم تصديرها في $date.\n\nستؤدي الاستعادة إلى دمج هذه العناصر في خزنتك الحالية. هل ترغب بالمتابعة؟" }
    override val restoreNowButton: String = "استعادة الآن"
    override val restoredItemsSuccess: (Int) -> String = { count -> "تمت استعادة $count عناصر بنجاح" }
    override val restoreCompletedGeneric: String = "اكتملت عملية استعادة الخزنة بنجاح."
    override val generatedRecoveryKitJsonTitle: String = "حقيبة الاستعادة المُنشأة (JSON)"
    override val vaultIdLabel: (String) -> String = { id -> "معرّف الخزنة: $id" }
    override val checksumLabel: (String) -> String = { sum -> "بصمة التحقق: $sum" }
    override val recoveryKitCopiedToast: String = "تم نسخ كود حقيبة الاستعادة إلى الحافظة"
    override val recoveryDataCopiedGeneric: String = "تم نسخ بيانات الاستعادة."
    override val copyJsonButton: String = "نسخ كود JSON"
    override val exportEncryptedBackupTitle: String = "تصدير نسخة احتياطية مشفرة"
    override val exportEncryptedBackupDesc: String = "ينشئ حزمة مشفرة (.vaultx) تتضمن ملفاتك ومجلداتك ووسومك، محمية بخوارزمية AES-256-GCM باستخدام مفتاحك الرئيسي."
    override val importRestoreBackupTitle: String = "استيراد واستعادة نسخة احتياطية"
    override val importRestoreBackupDesc: String = "استعادة ملف نسخة احتياطية (.vaultx). ستتم مطالبتك بالتأكيد قبل دمج أي بيانات في خزنتك."
    override val verifyKitDesc: String = "اختبر وتأكد بأمان من أن حقيبة الاستعادة صالحة وسليمة ومتوافقة تشفيرياً مع خزنتك. لا يتم تعديل أي بيانات أثناء الفحص."
    override val invalidJsonError: String = "صيغة JSON غير صحيحة. يرجى التأكد من لصق محتوى حقيبة الاستعادة كاملاً."
    override val verifyIntegrityButton: String = "التحقق من السلامة التشفيرية"
    override val recoveryKitValid: String = "حقيبة الاستعادة صالحة ومتوافقة"
    override val recoveryHistoryTitle: String = "سجل الاستعادة والأمان"
    override val noRecoveryHistory: String = "لا توجد أحداث استعادة مسجلة حتى الآن."

    // Security Center additions
    override val healthExcellent: String = "ممتاز"
    override val healthGood: String = "جيد"
    override val healthAttention: String = "بحاجة لانتباه"
    override val healthCritical: String = "مشكلات حرجة"
    override val vaultHealth: (String) -> String = { label -> "حالة أمان الخزنة: $label" }
    override val securitySubtitle: String = "تدقيق تشفيري وحسابي وتخزيني في الوقت الفعلي"
    override val rateLimitedStatus: String = "مقيد بمعدل محاولات"
    override val normalStatus: String = "طبيعي"
    override val zeroFailuresDesc: String = "صفر محاولات فاشلة متتالية. الجلسة الحالية موثقة وآمنة تماماً."
    override val weakPasswordsTitle: String = "كلمات مرور ضعيفة"
    override val noWeakPasswords: String = "لم يتم العثور على كلمات مرور ضعيفة"
    override val reviewPasswordsAction: String = "مراجعة كلمات المرور"
    override val reusedPasswordsTitle: String = "كلمات مرور مكررة"
    override val noReusedPasswords: String = "لا توجد بيانات اعتماد مكررة"
    override val resolveDuplicatesAction: String = "معالجة التكرار"
    override val accessProtectionTitle: String = "حماية الوصول"
    override val autoLockActiveStatus: (Int) -> String = { min -> "نشط: تقفل الخزنة بعد $min دقيقة من الخمول" }
    override val autoLockDisabledWarning: String = "تحذير: القفل التلقائي معطل!"
    override val configureAction: String = "تهيئة"
    override val pbkdf2IterationsDesc: String = "المفتاح الرئيسي مشتق باستخدام PBKDF2 (بـ 65,536 دورة). يمكنك تغييره متى شئت لإعادة تشفير كل السجلات."
    override val updateKeyAction: String = "تحديث المفتاح"
    override val noSecurityEventsFound: String = "لا توجد أحداث أمنية تطابق التصنيف المختار."

    // Universal Preview additions
    override val play: String = "تشغيل"
    override val pause: String = "إيقاف مؤقت"
    override val rewind10s: String = "إرجاع 10 ثوانٍ"
    override val forward10s: String = "تقديم 10 ثوانٍ"
    override val previousPage: String = "الصفحة السابقة"
    override val nextPage: String = "الصفحة التالية"
    override val unableToDecodeText: String = "تعذر فك ترميز النص (غير متوافق مع UTF-8)."
    override val mimeTypeLabel: String = "نوع الملف (MIME)"
    override val sizeLabel: String = "الحجم"
    override val sha256Label: String = "بصمة SHA-256"
    override val encryptedContentProtected: String = "محتوى مشفر • محمي"

    // TopBar & Privacy additions
    override val privacyOnBadge: String = "الخصوصية مشغّلة"
    override val aesGcmBadge: String = "AES-GCM"
    override val languageToggleLabel: String = "EN"
    override val sampleFileName: String = "كشف حساب بنكي - سبتمبر.pdf"
    override val sampleFileSizeSensitive: String = "الحجم: 2.4 م.ب • تصنيف حساس: مالي"
    override val sampleBadge: String = "عينة تجريبية"
    override val maskedBadge: String = "مموه"

    // AI screen additions
    override val geminiCloudApproved: String = "ذكاء Gemini السحابي (مصرّح به)"
    override val aiFileOrganizerHelp: String = "انقر فوق 'تحليل واقتراح التصنيفات' لتوليد مقترحات ذكية لتصنيف الملفات بناءً على محتواها وسياقها."
    override val aiDocAiHelp: String = "اختر عنصراً من خزنتك للتلخيص الذكي، أو استخراج التواريخ، أو حصر بنود العمل، أو الإجابة التحليلية."
    override val selectDocumentPlaceholder: String = "اختر مستنداً من الخزنة..."
    override val analysisResult: (String) -> String = { op -> "نتيجة التحليل ($op)" }
    override val aiSearchHelp: String = "ابحث باللغة الطبيعية عن أوراق الضمان، أو الاختبارات، أو الفواتير، أو جوازات السفر."
    override val matchScore: (Int) -> String = { score -> "نسبة التطابق: $score%" }
    override val aiPrivacyPolicyTitle: String = "سياسة خصوصية الذكاء الاصطناعي في VaultX"
    override val aiPrivacyPolicyBody: String = "• الوضع المحلي الافتراضي: يعمل تصنيف الملفات والبحث الدلالي محلياً بالكامل على جهازك دون إرسال أي بايت للخارج.\n\n• سحابة Gemini: إذا رغبت في تحليل نصوص مستندات طويلة، يمكنك تفعيل الذكاء السحابي يدوياً بموافقتك الصريحة.\n\n• المعرفة الصفرية: كلمات المرور ومفاتيح التشفير والمحتويات الخاصة للملفات لا تُرسل مطلقاً لأي طرف ثالث."
    override val allowGeminiCloudSwitch: String = "السماح بالذكاء الاصطناعي السحابي (Gemini)"
    override val filesCount: (Int) -> String = { count ->
        when {
            count == 1 -> "ملف واحد"
            count == 2 -> "ملفان"
            count in 3..10 -> "$count ملفات"
            else -> "$count ملفاً"
        }
    }
    override val duplicateCopiesLabel: (Int) -> String = { count ->
        when {
            count == 1 -> "نسخة مكررة واحدة"
            count == 2 -> "نسختان مكررتان"
            count in 3..10 -> "$count نسخ مكررة"
            else -> "$count نسخة مكررة"
        }
    }

    override val defaultDocumentName: String = "مستند"
    override val defaultFolderLogins: String = "تسجيلات الدخول"
    override val defaultFolderNotes: String = "ملاحظات"
    override val defaultFolderRecords: String = "سجلات"
    override val recoveryKitInvalid: String = "فشل التحقق"
    override val recoveryKitInvalidJson: String = "صيغة غير صالحة. تأكد من لصق البيانات كاملة."
    override val recoveryKitMatchSuccess: String = "التوقيع التشفيري يطابق الخزنة الحالية."
    override val recoveryKitMismatch: String = "حزمة الاسترداد تتبع لخزنة أو إعدادات مختلفة."
    override val failedUnlockNewVault: String = "فشل فتح الخزنة المنشأة حديثاً"
    override val cannotDecodeText: String = "تعذر فك ترميز النص (غير متوافق مع UTF-8)."
    override val archiveEntrySize: (String, String) -> String = { size, comp -> "الحجم: $size (المضغوط: $comp)" }
    override val tipAtLeast12Chars: String = "استخدم 12 حرفاً على الأقل"
    override val tipAddLowercase: String = "أضف أحرفاً صغيرة"
    override val tipAddUppercase: String = "أضف أحرفاً كبيرة"
    override val tipAddDigits: String = "أضف أرقاماً"
    override val tipAddSymbols: String = "أضف رموزاً خاصة"
    override val tipPasswordCannotBeEmpty: String = "لا يمكن أن تكون كلمة المرور فارغة"
    override fun formatBytes(bytes: Long): String = VaultI18n.formatBytes(bytes, "ar")
    override fun formatDate(timestamp: Long): String = VaultI18n.formatDate(timestamp, "ar")
}

val LocalVaultStrings = staticCompositionLocalOf { VaultI18n.English }
val LocalLanguageCode = staticCompositionLocalOf { "en" }

object VaultI18n {
    val English: VaultStrings = EnglishVaultStrings()
    val Arabic: VaultStrings = ArabicVaultStrings()

    fun forLanguage(langCode: String): Pair<VaultStrings, LayoutDirection> {
        return if (langCode.equals("ar", ignoreCase = true)) {
            Arabic to LayoutDirection.Rtl
        } else {
            English to LayoutDirection.Ltr
        }
    }

    /**
     * Locale-aware byte size formatter (e.g. "2.4 MB" vs "٢٫٤ م.ب").
     */
    fun formatBytes(bytes: Long, langCode: String): String {
        val isAr = langCode.equals("ar", ignoreCase = true)
        val locale = if (isAr) Locale.forLanguageTag("ar") else Locale.US
        return when {
            bytes >= 1024L * 1024L * 1024L -> {
                val value = bytes / (1024f * 1024f * 1024f)
                val unit = if (isAr) "ج.ب" else "GB"
                String.format(locale, "%.1f %s", value, unit)
            }
            bytes >= 1024L * 1024L -> {
                val value = bytes / (1024f * 1024f)
                val unit = if (isAr) "م.ب" else "MB"
                String.format(locale, "%.1f %s", value, unit)
            }
            bytes >= 1024L -> {
                val value = bytes / 1024f
                val unit = if (isAr) "ك.ب" else "KB"
                String.format(locale, "%.1f %s", value, unit)
            }
            else -> {
                val unit = if (isAr) "بايت" else "B"
                String.format(locale, "%d %s", bytes, unit)
            }
        }
    }

    /**
     * Locale-aware date formatter.
     */
    fun formatDate(timestamp: Long, langCode: String): String {
        val locale = if (langCode.equals("ar", ignoreCase = true)) Locale.forLanguageTag("ar") else Locale.US
        val formatter = SimpleDateFormat("MMM dd, yyyy", locale)
        return formatter.format(Date(timestamp))
    }

    /**
     * Locale-aware date & time formatter.
     */
    fun formatDateTime(timestamp: Long, langCode: String): String {
        val locale = if (langCode.equals("ar", ignoreCase = true)) Locale.forLanguageTag("ar") else Locale.US
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", locale)
        return formatter.format(Date(timestamp))
    }

    /**
     * Locale-aware number formatter.
     */
    fun formatNumber(number: Number, langCode: String): String {
        val locale = if (langCode.equals("ar", ignoreCase = true)) Locale.forLanguageTag("ar") else Locale.US
        return NumberFormat.getInstance(locale).format(number)
    }

    /**
     * Locale-aware percentage formatter.
     */
    fun formatPercentage(percentage: Int, langCode: String): String {
        val isAr = langCode.equals("ar", ignoreCase = true)
        val formattedNumber = formatNumber(percentage, langCode)
        return if (isAr) "%$formattedNumber" else "$formattedNumber%"
    }

    /**
     * Locale-aware duration mm:ss formatter.
     */
    fun formatDuration(minutes: Int, seconds: Int, langCode: String): String {
        val locale = if (langCode.equals("ar", ignoreCase = true)) Locale.forLanguageTag("ar") else Locale.US
        return String.format(locale, "%02d:%02d", minutes, seconds)
    }
}
