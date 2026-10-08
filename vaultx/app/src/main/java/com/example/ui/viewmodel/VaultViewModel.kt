package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoManager
import com.example.crypto.PasswordStrength
import com.example.crypto.PasswordStrengthResult
import com.example.data.VaultAuditLogEntity
import com.example.data.VaultConfigEntity
import com.example.data.VaultDatabase
import com.example.data.VaultRepository
import com.example.ai.AiDocumentAnalysisResult
import com.example.ai.AiFolderSuggestion
import com.example.ai.VaultAiManager
import com.example.duplicate.DuplicateDetector
import com.example.model.*
import com.example.privacy.VaultPrivacyManager
import com.example.recovery.RecoveryKitPackage
import com.example.recovery.RecoveryVerificationResult
import com.example.recovery.VaultRecoveryManager
import com.example.i18n.VaultI18n
import com.example.i18n.VaultStrings
import com.example.session.VaultSessionManager
import com.example.session.VaultSessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VaultNavTab {
    DASHBOARD,
    FILES,
    NOTES,
    PASSWORDS,
    RECORDS,
    SECURITY,
    RECOVERY,
    AI_TOOLS,
    PRIVACY,
    BACKUP,
    TRASH,
    SETTINGS
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VaultDatabase.getInstance(application)
    val repository = VaultRepository(application, database.vaultDao())

    val sessionState: StateFlow<VaultSessionState> = VaultSessionManager.sessionState

    val config: StateFlow<VaultConfigEntity?> = repository.configFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isPrivacyMode: StateFlow<Boolean> = config
        .map { it?.isPrivacyModeEnabled == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val activeItems: StateFlow<List<VaultItem>> = repository.getActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashItems: StateFlow<List<VaultItem>> = repository.getTrashItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<VaultAuditLogEntity>> = repository.getAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(VaultNavTab.DASHBOARD)
    val currentTab: StateFlow<VaultNavTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<VaultType?>(null)
    val typeFilter: StateFlow<VaultType?> = _typeFilter.asStateFlow()

    private val _favoriteOnlyFilter = MutableStateFlow(false)
    val favoriteOnlyFilter: StateFlow<Boolean> = _favoriteOnlyFilter.asStateFlow()

    private val _smartCollectionFilter = MutableStateFlow(SmartCollectionType.ALL)
    val smartCollectionFilter: StateFlow<SmartCollectionType> = _smartCollectionFilter.asStateFlow()

    val revealedItemIds: StateFlow<Set<String>> = VaultPrivacyManager.revealedItemIds

    init {
        viewModelScope.launch {
            repository.ensureDefaultConfig()
        }
        viewModelScope.launch {
            isPrivacyMode.collect { enabled ->
                VaultPrivacyManager.setPrivacyMode(enabled)
            }
        }
    }

    val filteredItems: StateFlow<List<VaultItem>> = combine(
        activeItems,
        _searchQuery,
        _typeFilter,
        _favoriteOnlyFilter,
        _smartCollectionFilter
    ) { items, query, type, favOnly, collection ->
        val filtered = items.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.folder.contains(query, ignoreCase = true) ||
                    item.tags.any { it.contains(query, ignoreCase = true) }
            val matchesType = type == null || item.type == type
            val matchesFav = !favOnly || item.isFavorite
            val matchesCollection = when (collection) {
                SmartCollectionType.ALL -> true
                SmartCollectionType.DOCUMENTS -> item.fileMimeType?.contains("pdf") == true ||
                        item.fileMimeType?.contains("document") == true ||
                        item.fileMimeType?.contains("text") == true ||
                        item.type == VaultType.NOTE
                SmartCollectionType.IMAGES -> item.fileMimeType?.startsWith("image/") == true
                SmartCollectionType.VIDEOS -> item.fileMimeType?.startsWith("video/") == true
                SmartCollectionType.AUDIO -> item.fileMimeType?.startsWith("audio/") == true
                SmartCollectionType.PROTECTED -> item.isPasswordProtected
                SmartCollectionType.FAVORITES -> item.isFavorite
                SmartCollectionType.RECENTLY_ADDED -> true
                SmartCollectionType.RECENTLY_OPENED -> true
            }
            matchesQuery && matchesType && matchesFav && matchesCollection
        }
        if (collection == SmartCollectionType.RECENTLY_ADDED) {
            filtered.sortedByDescending { it.createdAt }
        } else {
            filtered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Security audit summary
    val securityAudit: StateFlow<SecurityAuditSummary> = combine(
        activeItems,
        config
    ) { items, cfg ->
        val files = items.filter { it.type == VaultType.FILE }
        val notes = items.filter { it.type == VaultType.NOTE }
        val passwords = items.filter { it.type == VaultType.PASSWORD }
        val records = items.filter { it.type == VaultType.RECORD }

        val totalStorage = files.sumOf { it.fileSizeBytes ?: 0L }

        // Analyze passwords if key is available
        var weakCount = 0
        var reusedCount = 0
        val key = VaultSessionManager.getActiveKey()
        if (key != null && passwords.isNotEmpty()) {
            val passList = mutableListOf<String>()
            for (pItem in passwords) {
                val p = repository.decryptPassword(pItem, key)
                if (p.password.isNotEmpty()) {
                    val strength = PasswordStrength.evaluate(p.password)
                    if (strength.score < 3) {
                        weakCount++
                    }
                    passList.add(p.password)
                }
            }
            val duplicates = passList.groupBy { it }.filter { it.value.size > 1 }
            reusedCount = duplicates.values.sumOf { it.size }
        }

        SecurityAuditSummary(
            totalItems = items.size,
            totalFiles = files.size,
            totalNotes = notes.size,
            totalPasswords = passwords.size,
            totalRecords = records.size,
            weakPasswordsCount = weakCount,
            reusedPasswordsCount = reusedCount,
            autoLockMinutes = cfg?.autoLockMinutes ?: 5,
            lastBackupTimestamp = cfg?.lastBackupAt,
            totalStorageBytes = totalStorage
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SecurityAuditSummary(0, 0, 0, 0, 0, 0, 0, 5, null, 0L)
    )

    fun setTab(tab: VaultNavTab) {
        _currentTab.value = tab
        recordInteraction()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        recordInteraction()
    }

    fun setTypeFilter(type: VaultType?) {
        _typeFilter.value = type
        recordInteraction()
    }

    fun toggleFavoriteFilter() {
        _favoriteOnlyFilter.value = !_favoriteOnlyFilter.value
        recordInteraction()
    }

    fun recordInteraction() {
        VaultSessionManager.recordInteraction()
    }

    fun checkInactivity() {
        VaultSessionManager.checkInactivity()
    }

    private val currentStrings: VaultStrings
        get() = VaultI18n.forLanguage(config.value?.languageCode ?: "en").first

    // Authentication actions
    fun createVault(
        password: String,
        confirm: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (password.length < 8) {
            onError(currentStrings.passwordMinLength)
            return
        }
        if (password != confirm) {
            onError(currentStrings.passwordMismatch)
            return
        }

        viewModelScope.launch {
            try {
                repository.initializeVault(password)
                val key = repository.verifyAndUnlock(password)
                if (key != null) {
                    VaultSessionManager.unlock(key)
                    _currentTab.value = VaultNavTab.DASHBOARD
                    onSuccess()
                } else {
                    onError(currentStrings.failedUnlockNewVault)
                }
            } catch (e: Exception) {
                onError("Vault initialization error: ${e.message}")
            }
        }
    }

    fun unlockVault(
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                when (val result = repository.verifyAndUnlockWithStatus(password)) {
                    is UnlockResult.Success -> {
                        val cfg = repository.configFlow.firstOrNull()
                        if (cfg != null) {
                            VaultSessionManager.setAutoLockMinutes(cfg.autoLockMinutes)
                        }
                        VaultSessionManager.unlock(result.key)
                        _currentTab.value = VaultNavTab.DASHBOARD
                        onSuccess()
                    }
                    is UnlockResult.RateLimited -> {
                        onError("Too many failed attempts. Please wait ${result.remainingLockoutSeconds}s before trying again.")
                    }
                    is UnlockResult.InvalidPassword -> {
                        if (result.remainingLockoutSeconds > 0) {
                            onError("Invalid master password. Too many failed attempts: please wait ${result.remainingLockoutSeconds}s before trying again.")
                        } else {
                            onError("Invalid master password. Access denied.")
                        }
                    }
                    is UnlockResult.NotInitialized -> {
                        onError("Vault is not initialized.")
                    }
                }
            } catch (e: Exception) {
                onError("Authentication error: ${e.message}")
            }
        }
    }

    fun lockVault() {
        VaultSessionManager.lock()
        VaultPrivacyManager.clearAllRevealed()
        _searchQuery.value = ""
        _typeFilter.value = null
        _favoriteOnlyFilter.value = false
        _smartCollectionFilter.value = SmartCollectionType.ALL
        viewModelScope.launch {
            repository.logSecurityEvent(
                action = "MANUAL_LOCK",
                details = "Vault locked manually by user.",
                severity = "INFO",
                category = "LOCK"
            )
        }
    }

    // Item Actions
    fun saveNote(
        id: String?,
        title: String,
        content: String,
        folder: String,
        tags: List<String>,
        isPinned: Boolean,
        isFavorite: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val key = VaultSessionManager.getActiveKey() ?: run {
            onError(currentStrings.vaultLocked)
            return
        }
        viewModelScope.launch {
            try {
                repository.saveNote(
                    id = id ?: java.util.UUID.randomUUID().toString(),
                    title = title,
                    content = content,
                    folder = folder,
                    tags = tags,
                    isPinned = isPinned,
                    isFavorite = isFavorite,
                    secretKey = key
                )
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to save note: ${e.message}")
            }
        }
    }

    fun savePassword(
        id: String?,
        title: String,
        payload: PasswordPayload,
        folder: String,
        tags: List<String>,
        isFavorite: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val key = VaultSessionManager.getActiveKey() ?: run {
            onError(currentStrings.vaultLocked)
            return
        }
        viewModelScope.launch {
            try {
                repository.savePassword(
                    id = id ?: java.util.UUID.randomUUID().toString(),
                    title = title,
                    payload = payload,
                    folder = folder,
                    tags = tags,
                    isFavorite = isFavorite,
                    secretKey = key
                )
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to save password: ${e.message}")
            }
        }
    }

    fun saveRecord(
        id: String?,
        title: String,
        payload: SecureRecordPayload,
        folder: String,
        tags: List<String>,
        isFavorite: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val key = VaultSessionManager.getActiveKey() ?: run {
            onError(currentStrings.vaultLocked)
            return
        }
        viewModelScope.launch {
            try {
                repository.saveRecord(
                    id = id ?: java.util.UUID.randomUUID().toString(),
                    title = title,
                    payload = payload,
                    folder = folder,
                    tags = tags,
                    isFavorite = isFavorite,
                    secretKey = key
                )
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to save record: ${e.message}")
            }
        }
    }

    fun uploadEncryptedFile(
        originalFileName: String,
        mimeType: String,
        fileBytes: ByteArray,
        folder: String = "Documents",
        tags: List<String> = emptyList(),
        filePassword: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val key = VaultSessionManager.getActiveKey() ?: run {
            onError(currentStrings.vaultLocked)
            return
        }
        viewModelScope.launch {
            try {
                repository.saveEncryptedFile(
                    title = originalFileName,
                    originalFileName = originalFileName,
                    mimeType = mimeType,
                    fileBytes = fileBytes,
                    folder = folder,
                    tags = tags,
                    secretKey = key,
                    filePassword = filePassword
                )
                onSuccess()
            } catch (e: Exception) {
                onError("File encryption failed: ${e.message}")
            }
        }
    }

    suspend fun decryptFile(item: VaultItem, filePassword: String? = null): ByteArray? {
        val key = VaultSessionManager.getActiveKey() ?: return null
        return repository.decryptFileBytes(item, key, filePassword)
    }

    fun changeFilePassword(
        item: VaultItem,
        oldPass: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val key = VaultSessionManager.getActiveKey() ?: run {
            onError(currentStrings.vaultLocked)
            return
        }
        viewModelScope.launch {
            val ok = repository.changeFilePassword(item, oldPass, newPass, key)
            if (ok) onSuccess() else onError("Unable to unlock this file. Please check the password and try again.")
        }
    }

    fun removeFilePassword(
        item: VaultItem,
        currentPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val key = VaultSessionManager.getActiveKey() ?: run {
            onError(currentStrings.vaultLocked)
            return
        }
        viewModelScope.launch {
            val ok = repository.removeFilePassword(item, currentPass, key)
            if (ok) onSuccess() else onError("Unable to remove password protection. Invalid password.")
        }
    }

    fun addFilePassword(
        item: VaultItem,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val key = VaultSessionManager.getActiveKey() ?: run {
            onError(currentStrings.vaultLocked)
            return
        }
        viewModelScope.launch {
            val ok = repository.addFilePassword(item, newPass, key)
            if (ok) onSuccess() else onError("Failed to add file password protection.")
        }
    }

    fun updateLockOnBackground(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateLockOnBackground(enabled)
        }
    }

    fun updateReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateReducedMotion(enabled)
        }
    }

    fun onApplicationBackground() {
        val shouldLock = config.value?.lockOnBackground != false
        if (shouldLock && sessionState.value == VaultSessionState.UNLOCKED) {
            lockVault()
        }
    }

    fun decryptNote(item: VaultItem): NotePayload {
        val key = VaultSessionManager.getActiveKey() ?: return NotePayload("Locked")
        return repository.decryptNote(item, key)
    }

    fun decryptPassword(item: VaultItem): PasswordPayload {
        val key = VaultSessionManager.getActiveKey() ?: return PasswordPayload(notes = "Locked")
        return repository.decryptPassword(item, key)
    }

    fun decryptRecord(item: VaultItem): SecureRecordPayload {
        val key = VaultSessionManager.getActiveKey() ?: return SecureRecordPayload(notes = "Locked")
        return repository.decryptRecord(item, key)
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun togglePin(id: String) {
        viewModelScope.launch {
            repository.togglePin(id)
        }
    }

    fun moveToTrash(id: String) {
        viewModelScope.launch {
            repository.moveToTrash(id)
        }
    }

    fun restoreFromTrash(id: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(id)
        }
    }

    fun permanentlyDelete(id: String) {
        viewModelScope.launch {
            repository.permanentlyDelete(id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun updateAutoLock(minutes: Int) {
        viewModelScope.launch {
            VaultSessionManager.setAutoLockMinutes(minutes)
            repository.updateAutoLock(minutes)
        }
    }

    fun updateLanguage(langCode: String) {
        viewModelScope.launch {
            repository.updateLanguage(langCode)
        }
    }

    fun changeMasterPassword(
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val oldKey = VaultSessionManager.getActiveKey() ?: run {
            onError(currentStrings.vaultLocked)
            return
        }
        if (newPassword.length < 8) {
            onError("New password must be at least 8 characters")
            return
        }
        viewModelScope.launch {
            try {
                val success = repository.changeMasterPassword(oldKey, newPassword)
                if (success) {
                    val newKey = repository.verifyAndUnlock(newPassword)
                    if (newKey != null) {
                        VaultSessionManager.unlock(newKey)
                    }
                    onSuccess()
                } else {
                    onError("Failed to re-encrypt vault with new key")
                }
            } catch (e: Exception) {
                onError("Error changing master password: ${e.message}")
            }
        }
    }

    fun wipeVault(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.wipeEntireVault()
            lockVault()
            onSuccess()
        }
    }

    suspend fun createBackupJson(): String? {
        val key = VaultSessionManager.getActiveKey() ?: return null
        return try {
            repository.createEncryptedBackup(key)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun parseBackup(json: String): BackupPackage? {
        return repository.parseBackup(json)
    }

    suspend fun restoreBackup(pkg: BackupPackage): Int {
        return repository.restoreBackup(pkg)
    }

    // Privacy Mode Controls
    fun updatePrivacyMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePrivacyMode(enabled)
        }
    }

    fun togglePrivacyMode() {
        val current = isPrivacyMode.value
        updatePrivacyMode(!current)
    }

    fun temporarilyRevealItem(itemId: String) {
        VaultPrivacyManager.temporarilyReveal(itemId)
    }

    fun remaskItem(itemId: String) {
        VaultPrivacyManager.remaskItem(itemId)
    }

    // Recovery Kit Subsystem
    suspend fun createRecoveryKit(): Pair<RecoveryKitPackage, String>? {
        val cfg = repository.getConfigDirect() ?: return null
        val (pkg, json) = VaultRecoveryManager.generateRecoveryKit(cfg)
        repository.saveRecoveryKitMetadata(pkg.integrityChecksum)
        return pkg to json
    }

    fun saveRecoveryKitMetadata(checksum: String) {
        viewModelScope.launch {
            repository.saveRecoveryKitMetadata(checksum)
        }
    }

    fun verifyRecoveryKit(json: String): RecoveryVerificationResult {
        val cfg = config.value
        val res = VaultRecoveryManager.verifyRecoveryKit(json, cfg)
        viewModelScope.launch {
            repository.logSecurityEvent(
                action = if (res.isValid) "RECOVERY_KIT_VERIFIED" else "RECOVERY_KIT_INVALID",
                details = "Recovery verification: ${res.message}",
                severity = if (res.isValid) "INFO" else "WARNING",
                category = "RECOVERY"
            )
        }
        return res
    }

    // Duplicate File Detection
    private val _duplicateScanProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val duplicateScanProgress: StateFlow<Pair<Int, Int>?> = _duplicateScanProgress.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    fun scanForDuplicates() {
        val files = activeItems.value.filter { it.type == VaultType.FILE }
        viewModelScope.launch {
            val groups = DuplicateDetector.scanForDuplicates(
                context = getApplication(),
                fileItems = files,
                onProgress = { cur, tot -> _duplicateScanProgress.value = cur to tot }
            )
            _duplicateGroups.value = groups
            _duplicateScanProgress.value = null
        }
    }

    fun clearDuplicateGroups() {
        _duplicateGroups.value = emptyList()
    }

    fun deleteDuplicateFiles(itemsToDelete: List<VaultItem>, onComplete: () -> Unit) {
        viewModelScope.launch {
            itemsToDelete.forEach { item ->
                repository.moveToTrash(item.id)
            }
            repository.logSecurityEvent(
                action = "DUPLICATE_FILES_REMOVED",
                details = "Moved ${itemsToDelete.size} redundant duplicate copies to Trash.",
                severity = "INFO",
                category = "FILES"
            )
            val remainingFiles = repository.getActiveItems().first().filter { it.type == VaultType.FILE }
            val groups = DuplicateDetector.scanForDuplicates(
                context = getApplication(),
                fileItems = remainingFiles
            )
            _duplicateGroups.value = groups
            onComplete()
        }
    }

    // AI Tools Subsystem
    private val _aiSuggestions = MutableStateFlow<List<AiFolderSuggestion>>(emptyList())
    val aiSuggestions: StateFlow<List<AiFolderSuggestion>> = _aiSuggestions.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _aiDocumentResult = MutableStateFlow<AiDocumentAnalysisResult?>(null)
    val aiDocumentResult: StateFlow<AiDocumentAnalysisResult?> = _aiDocumentResult.asStateFlow()

    fun generateAiOrganization(useCloudAi: Boolean = false) {
        _isAiProcessing.value = true
        viewModelScope.launch {
            val suggestions = VaultAiManager.generateOrganizationSuggestions(
                items = activeItems.value,
                useCloudAi = useCloudAi
            )
            _aiSuggestions.value = suggestions
            _isAiProcessing.value = false
        }
    }

    fun applyAiFolderSuggestions(onComplete: () -> Unit) {
        viewModelScope.launch {
            val approved = _aiSuggestions.value.filter { it.isApproved }
            approved.forEach { suggestion ->
                repository.updateItemFolder(suggestion.itemId, suggestion.suggestedFolder)
            }
            repository.logSecurityEvent(
                action = "AI_ORGANIZATION_APPLIED",
                details = "Applied AI folder organization to ${approved.size} items.",
                severity = "INFO",
                category = "SETTINGS"
            )
            _aiSuggestions.value = emptyList()
            onComplete()
        }
    }

    fun toggleAiSuggestionApproval(itemId: String) {
        _aiSuggestions.value = _aiSuggestions.value.map {
            if (it.itemId == itemId) it.copy(isApproved = !it.isApproved) else it
        }
    }

    fun analyzeDocument(
        text: String,
        operation: String,
        question: String? = null,
        useCloudAi: Boolean = false
    ) {
        _isAiProcessing.value = true
        viewModelScope.launch {
            val result = VaultAiManager.analyzeDocument(
                textSample = text,
                operation = operation,
                question = question,
                useCloudAi = useCloudAi
            )
            _aiDocumentResult.value = result
            _isAiProcessing.value = false
        }
    }

    fun clearAiDocumentResult() {
        _aiDocumentResult.value = null
    }

    fun performNaturalLanguageSearch(query: String): List<com.example.ai.AiSearchResult> {
        return VaultAiManager.naturalLanguageSearch(query, activeItems.value)
    }
}
