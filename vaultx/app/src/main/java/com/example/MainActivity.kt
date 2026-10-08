package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.VaultI18n
import com.example.model.VaultType
import com.example.session.VaultSessionState
import com.example.ui.components.LocalPrivacyMode
import com.example.ui.components.VaultNavigationBar
import com.example.ui.components.VaultTopBar
import com.example.ui.screens.*
import com.example.ui.theme.VaultXTheme
import com.example.ui.viewmodel.VaultNavTab
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#090D16"))
        enableEdgeToEdge()

        setContent {
            VaultXTheme {
                val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
                val config by viewModel.config.collectAsStateWithLifecycle()
                val isPrivacyMode by viewModel.isPrivacyMode.collectAsStateWithLifecycle()
                val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()
                val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
                val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
                val securityAudit by viewModel.securityAudit.collectAsStateWithLifecycle()
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val aiSuggestions by viewModel.aiSuggestions.collectAsStateWithLifecycle()
                val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
                val aiDocumentResult by viewModel.aiDocumentResult.collectAsStateWithLifecycle()

                val defaultLang = remember {
                    if (java.util.Locale.getDefault().language.startsWith("ar")) "ar" else "en"
                }
                val activeLang = config?.languageCode ?: defaultLang

                val (strings, layoutDirection) = remember(activeLang) {
                    VaultI18n.forLanguage(activeLang)
                }

                val isReducedMotion = config?.reducedMotion != false

                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    LocalPrivacyMode provides isPrivacyMode,
                    com.example.i18n.LocalVaultStrings provides strings,
                    com.example.i18n.LocalLanguageCode provides activeLang
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { viewModel.recordInteraction() })
                            },
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AnimatedContent(
                            targetState = sessionState,
                            transitionSpec = {
                                if (isReducedMotion) {
                                    EnterTransition.None togetherWith ExitTransition.None
                                } else {
                                    fadeIn(animationSpec = tween(220)).togetherWith(fadeOut(animationSpec = tween(160)))
                                }
                            },
                            label = "SessionLockTransition"
                        ) { state ->
                            if (state == VaultSessionState.LOCKED) {
                                LockScreen(
                                    isInitialized = config?.isInitialized == true,
                                    strings = strings,
                                    onUnlock = { password, onError ->
                                        viewModel.unlockVault(
                                            password = password,
                                            onSuccess = {},
                                            onError = onError
                                        )
                                    },
                                    onCreateVault = { password, confirm, onError ->
                                        viewModel.createVault(
                                            password = password,
                                            confirm = confirm,
                                            onSuccess = {},
                                            onError = onError
                                        )
                                    },
                                    onToggleLanguage = {
                                        val nextLang = if (activeLang == "ar") "en" else "ar"
                                        viewModel.updateLanguage(nextLang)
                                    }
                                )
                            } else {
                                // Unlocked Vault Interface
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    topBar = {
                                        VaultTopBar(
                                            strings = strings,
                                            searchQuery = searchQuery,
                                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                            onLockClick = { viewModel.lockVault() },
                                            onSelectTab = { viewModel.setTab(it) },
                                            onToggleLanguage = {
                                                val nextLang = if (activeLang == "ar") "en" else "ar"
                                                viewModel.updateLanguage(nextLang)
                                            },
                                            currentLanguage = activeLang,
                                            isPrivacyMode = isPrivacyMode,
                                            onTogglePrivacyMode = { viewModel.togglePrivacyMode() }
                                        )
                                    },
                                    bottomBar = {
                                        VaultNavigationBar(
                                            currentTab = currentTab,
                                            onTabSelected = { viewModel.setTab(it) },
                                            strings = strings
                                        )
                                    }
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        AnimatedContent(
                                            targetState = currentTab,
                                            transitionSpec = {
                                                if (isReducedMotion) {
                                                    EnterTransition.None togetherWith ExitTransition.None
                                                } else {
                                                    (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { it / 8 })
                                                        .togetherWith(fadeOut(animationSpec = tween(160)))
                                                }
                                            },
                                            label = "TabTransition"
                                        ) { targetTab ->
                                            when (targetTab) {
                                                VaultNavTab.DASHBOARD -> {
                                                    DashboardScreen(
                                                        items = filteredItems,
                                                        audit = securityAudit,
                                                        strings = strings,
                                                        onNavigateTab = { viewModel.setTab(it) },
                                                        onItemClick = { item ->
                                                            when (item.type) {
                                                                VaultType.FILE -> viewModel.setTab(VaultNavTab.FILES)
                                                                VaultType.NOTE -> viewModel.setTab(VaultNavTab.NOTES)
                                                                VaultType.PASSWORD -> viewModel.setTab(VaultNavTab.PASSWORDS)
                                                                VaultType.RECORD -> viewModel.setTab(VaultNavTab.RECORDS)
                                                            }
                                                        },
                                                        onNewFileClick = { viewModel.setTab(VaultNavTab.FILES) },
                                                        onNewNoteClick = { viewModel.setTab(VaultNavTab.NOTES) },
                                                        onNewPasswordClick = { viewModel.setTab(VaultNavTab.PASSWORDS) },
                                                        onNewRecordClick = { viewModel.setTab(VaultNavTab.RECORDS) }
                                                    )
                                                }

                                                VaultNavTab.FILES -> {
                                                    FilesScreen(
                                                        items = filteredItems,
                                                        isPrivacyModeEnabled = isPrivacyMode,
                                                        strings = strings,
                                                        onUploadFile = { fileName, mimeType, bytes, filePassword ->
                                                            viewModel.uploadEncryptedFile(
                                                                originalFileName = fileName,
                                                                mimeType = mimeType,
                                                                fileBytes = bytes,
                                                                filePassword = filePassword,
                                                                onSuccess = {},
                                                                onError = {}
                                                            )
                                                        },
                                                        onDecryptFile = { item, filePassword -> viewModel.decryptFile(item, filePassword) },
                                                        onChangeFilePassword = { item, oldP, newP, onS, onE ->
                                                            viewModel.changeFilePassword(item, oldP, newP, onS, onE)
                                                        },
                                                        onRemoveFilePassword = { item, pass, onS, onE ->
                                                            viewModel.removeFilePassword(item, pass, onS, onE)
                                                        },
                                                        onAddFilePassword = { item, pass, onS, onE ->
                                                            viewModel.addFilePassword(item, pass, onS, onE)
                                                        },
                                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                        onMoveToTrash = { viewModel.moveToTrash(it) }
                                                    )
                                                }

                                                VaultNavTab.NOTES -> {
                                                    NotesScreen(
                                                        items = filteredItems,
                                                        strings = strings,
                                                        onDecryptNote = { viewModel.decryptNote(it) },
                                                        onSaveNote = { id, title, content, folder, tags, isPinned, isFav ->
                                                            viewModel.saveNote(
                                                                id = id,
                                                                title = title,
                                                                content = content,
                                                                folder = folder,
                                                                tags = tags,
                                                                isPinned = isPinned,
                                                                isFavorite = isFav,
                                                                onSuccess = {},
                                                                onError = {}
                                                            )
                                                        },
                                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                        onTogglePin = { viewModel.togglePin(it) },
                                                        onMoveToTrash = { viewModel.moveToTrash(it) }
                                                    )
                                                }

                                                VaultNavTab.PASSWORDS -> {
                                                    PasswordsScreen(
                                                        items = filteredItems,
                                                        strings = strings,
                                                        onDecryptPassword = { viewModel.decryptPassword(it) },
                                                        onSavePassword = { id, title, payload, folder, tags, isFav ->
                                                            viewModel.savePassword(
                                                                id = id,
                                                                title = title,
                                                                payload = payload,
                                                                folder = folder,
                                                                tags = tags,
                                                                isFavorite = isFav,
                                                                onSuccess = {},
                                                                onError = {}
                                                            )
                                                        },
                                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                        onMoveToTrash = { viewModel.moveToTrash(it) }
                                                    )
                                                }

                                                VaultNavTab.RECORDS -> {
                                                    RecordsScreen(
                                                        items = filteredItems,
                                                        strings = strings,
                                                        onDecryptRecord = { viewModel.decryptRecord(it) },
                                                        onSaveRecord = { id, title, payload, folder, tags, isFav ->
                                                            viewModel.saveRecord(
                                                                id = id,
                                                                title = title,
                                                                payload = payload,
                                                                folder = folder,
                                                                tags = tags,
                                                                isFavorite = isFav,
                                                                onSuccess = {},
                                                                onError = {}
                                                            )
                                                        },
                                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                        onMoveToTrash = { viewModel.moveToTrash(it) }
                                                    )
                                                }

                                                VaultNavTab.SECURITY -> {
                                                    SecurityCenterScreen(
                                                        audit = securityAudit,
                                                        strings = strings,
                                                        onNavigateTab = { viewModel.setTab(it) },
                                                        onChangeMasterPasswordClick = {
                                                            viewModel.setTab(VaultNavTab.SETTINGS)
                                                        }
                                                    )
                                                }

                                                VaultNavTab.RECOVERY -> {
                                                    RecoveryCenterScreen(
                                                        auditLogs = auditLogs,
                                                        strings = strings,
                                                        onCreateRecoveryKit = { viewModel.createRecoveryKit() },
                                                        onVerifyRecoveryKit = { viewModel.verifyRecoveryKit(it) }
                                                    )
                                                }

                                                VaultNavTab.AI_TOOLS -> {
                                                    AiToolsScreen(
                                                        items = filteredItems,
                                                        strings = strings,
                                                        aiSuggestions = aiSuggestions,
                                                        isAiProcessing = isAiProcessing,
                                                        aiDocumentResult = aiDocumentResult,
                                                        onGenerateSuggestions = { viewModel.generateAiOrganization(it) },
                                                        onToggleSuggestionApproval = { viewModel.toggleAiSuggestionApproval(it) },
                                                        onApplyApprovedSuggestions = { viewModel.applyAiFolderSuggestions(it) },
                                                        onAnalyzeDocument = { text, op, q, cloud -> viewModel.analyzeDocument(text, op, q, cloud) },
                                                        onClearDocumentResult = { viewModel.clearAiDocumentResult() },
                                                        onPerformNlSearch = { viewModel.performNaturalLanguageSearch(it) }
                                                    )
                                                }

                                                VaultNavTab.PRIVACY -> {
                                                    PrivacyCenterScreen(
                                                        isPrivacyModeEnabled = isPrivacyMode,
                                                        onTogglePrivacyMode = { viewModel.updatePrivacyMode(it) },
                                                        strings = strings
                                                    )
                                                }

                                                VaultNavTab.BACKUP -> {
                                                    BackupRestoreScreen(
                                                        config = config,
                                                        strings = strings,
                                                        onExportBackupJson = { viewModel.createBackupJson() },
                                                        onParseBackupJson = { viewModel.parseBackup(it) },
                                                        onRestoreBackup = { viewModel.restoreBackup(it) },
                                                        onSaveRecoveryKitMetadata = { viewModel.saveRecoveryKitMetadata(it) }
                                                    )
                                                }

                                                VaultNavTab.TRASH -> {
                                                    TrashScreen(
                                                        items = trashItems,
                                                        strings = strings,
                                                        onRestore = { viewModel.restoreFromTrash(it) },
                                                        onPermanentDelete = { viewModel.permanentlyDelete(it) },
                                                        onEmptyTrash = { viewModel.emptyTrash() }
                                                    )
                                                }

                                                VaultNavTab.SETTINGS -> {
                                                    SettingsScreen(
                                                        config = config,
                                                        auditLogs = auditLogs,
                                                        strings = strings,
                                                        onUpdateAutoLock = { viewModel.updateAutoLock(it) },
                                                        onUpdateLanguage = { viewModel.updateLanguage(it) },
                                                        onUpdateLockOnBackground = { viewModel.updateLockOnBackground(it) },
                                                        onUpdateReducedMotion = { viewModel.updateReducedMotion(it) },
                                                        onTogglePrivacyMode = { viewModel.updatePrivacyMode(it) },
                                                        onChangeMasterPassword = { newPass, onSuccess, onError ->
                                                            viewModel.changeMasterPassword(newPass, onSuccess, onError)
                                                        },
                                                        onWipeVault = { viewModel.wipeVault {} }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkInactivity()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onApplicationBackground()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.recordInteraction()
    }
}
