package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import com.example.privacy.VaultPrivacyManager
import com.example.ui.components.LocalPrivacyMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultConfigEntity
import com.example.i18n.VaultStrings
import com.example.model.BackupPackage
import com.example.model.RecoveryKit
import com.example.model.RecoveryKitVerificationResult
import com.example.recovery.RecoveryKitManager
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupRestoreScreen(
    config: VaultConfigEntity?,
    strings: VaultStrings,
    onExportBackupJson: suspend () -> String?,
    onParseBackupJson: suspend (String) -> BackupPackage?,
    onRestoreBackup: suspend (BackupPackage) -> Int,
    onSaveRecoveryKitMetadata: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val langCode = com.example.i18n.LocalLanguageCode.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Recovery Kit, 1: Backups, 2: Verify Recovery

    // Recovery Kit State
    var showCreateKitConfirm by remember { mutableStateOf(false) }
    var generatedKit by remember { mutableStateOf<RecoveryKit?>(null) }
    var generatedKitJson by remember { mutableStateOf<String?>(null) }

    // Verification State
    var kitJsonToVerify by remember { mutableStateOf("") }
    var verificationResult by remember { mutableStateOf<RecoveryKitVerificationResult?>(null) }

    // Backup state
    var isExporting by remember { mutableStateOf(false) }
    var backupJsonToSave by remember { mutableStateOf<String?>(null) }
    var parsedPackageForRestore by remember { mutableStateOf<BackupPackage?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    val isPrivacyMode = LocalPrivacyMode.current

    // SAF Document Creator for saving .vaultx file
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && backupJsonToSave != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os: OutputStream ->
                    os.write(backupJsonToSave!!.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                VaultPrivacyManager.showSanitizedToast(
                    context = context,
                    rawMessage = strings.backupSavedToast,
                    isPrivacyMode = isPrivacyMode,
                    customGenericMessage = strings.backupPackageGenerated
                )
            } catch (e: Exception) {
                VaultPrivacyManager.showSanitizedToast(
                    context = context,
                    rawMessage = strings.exportError(e.message ?: ""),
                    isPrivacyMode = isPrivacyMode,
                    customGenericMessage = strings.exportError("")
                )
            } finally {
                backupJsonToSave = null
                isExporting = false
            }
        } else {
            isExporting = false
        }
    }

    // SAF Document Picker for opening .vaultx file
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use {
                        String(it.readBytes(), Charsets.UTF_8)
                    }
                    if (json != null) {
                        val pkg = onParseBackupJson(json)
                        if (pkg != null) {
                            parsedPackageForRestore = pkg
                        } else {
                            VaultPrivacyManager.showSanitizedToast(
                                context = context,
                                rawMessage = strings.invalidBackupToast,
                                isPrivacyMode = isPrivacyMode
                            )
                        }
                    }
                } catch (e: Exception) {
                    VaultPrivacyManager.showSanitizedToast(
                        context = context,
                        rawMessage = strings.errorReadingBackup(e.message ?: ""),
                        isPrivacyMode = isPrivacyMode
                    )
                }
            }
        }
    }

    // Confirm Create Recovery Kit Dialog
    if (showCreateKitConfirm) {
        ConfirmDialog(
            title = strings.createRecoveryKit,
            message = "${strings.recoveryKitExplanation}\n\n${strings.recoveryKitWarningNotice}",
            confirmText = strings.generateKitButton,
            cancelText = strings.cancel,
            confirmColor = CyberCyan,
            onConfirm = {
                showCreateKitConfirm = false
                if (config != null) {
                    val kit = RecoveryKitManager.generateRecoveryKit(config)
                    val json = RecoveryKitManager.exportToJson(kit)
                    generatedKit = kit
                    generatedKitJson = json
                    onSaveRecoveryKitMetadata(kit.integrityHmac)
                    VaultPrivacyManager.showSanitizedToast(
                        context = context,
                        rawMessage = strings.recoveryKitGeneratedToast,
                        isPrivacyMode = isPrivacyMode
                    )
                }
            },
            onDismiss = { showCreateKitConfirm = false }
        )
    }

    // Confirm Restore Dialog
    if (parsedPackageForRestore != null) {
        val pkg = parsedPackageForRestore!!
        val dateStr = com.example.i18n.VaultI18n.formatDateTime(pkg.exportedAt, langCode)
        ConfirmDialog(
            title = strings.restoreBackupTitle,
            message = strings.restoreBackupMessage(pkg.items.size, dateStr),
            confirmText = strings.restoreNowButton,
            cancelText = strings.cancel,
            confirmColor = SecurityGreen,
            onConfirm = {
                val toRestore = parsedPackageForRestore
                parsedPackageForRestore = null
                if (toRestore != null) {
                    isRestoring = true
                    coroutineScope.launch {
                        val count = onRestoreBackup(toRestore)
                        isRestoring = false
                        VaultPrivacyManager.showSanitizedToast(
                            context = context,
                            rawMessage = strings.restoredItemsSuccess(count),
                            isPrivacyMode = isPrivacyMode,
                            customGenericMessage = strings.restoreCompletedGeneric
                        )
                    }
                }
            },
            onDismiss = { parsedPackageForRestore = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
            .statusBarsPadding()
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = VaultSurface,
            contentColor = CyberCyan
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(strings.recoveryKit, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(strings.backupRestore, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(strings.verifyRecoveryKit, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Recovery Kit Tab
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = CyberCyan)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = strings.recoveryKit, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = strings.recoveryKitExplanation,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 17.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SecurityAmber.copy(alpha = 0.15f)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = SecurityAmber, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = strings.recoveryKitWarningNotice, color = SecurityAmber, fontSize = 11.sp, lineHeight = 15.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { showCreateKitConfirm = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddModerator, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = strings.createRecoveryKit, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (generatedKit != null && generatedKitJson != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = VaultSurface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "Generated Recovery Kit (JSON)", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "Vault ID: ${generatedKit!!.vaultId}", color = CyberCyanLight, fontSize = 11.sp)
                                    Text(text = "Checksum: ${generatedKit!!.integrityHmac.take(16)}...", color = TextMuted, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(generatedKitJson!!))
                                                VaultPrivacyManager.showSanitizedToast(
                                                    context = context,
                                                    rawMessage = strings.recoveryKitCopiedToast,
                                                    isPrivacyMode = isPrivacyMode,
                                                    customGenericMessage = strings.recoveryKitCopiedToast
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyanContainer, contentColor = CyberCyanLight),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(strings.copyJsonButton, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Encrypted Backups Tab
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = strings.exportBackup, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        isExporting = true
                                        coroutineScope.launch {
                                            val json = onExportBackupJson()
                                            if (json != null) {
                                                backupJsonToSave = json
                                                exportFileLauncher.launch("vaultx_backup_${System.currentTimeMillis()}.vaultx")
                                            } else {
                                                isExporting = false
                                                VaultPrivacyManager.showSanitizedToast(
                                                    context = context,
                                                    rawMessage = strings.failedCreateBackupPackage,
                                                    isPrivacyMode = isPrivacyMode
                                                )
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isExporting
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = strings.exportBackup, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = strings.importBackup, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = { importFileLauncher.launch(arrayOf("*/*")) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecurityGreen),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = strings.importBackup, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Verify Recovery Kit Tab
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = strings.verifyRecoveryKit, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = kitJsonToVerify,
                                    onValueChange = {
                                        kitJsonToVerify = it
                                        verificationResult = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    placeholder = { Text(strings.pasteRecoveryKitJson, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val kit = RecoveryKitManager.parseFromJson(kitJsonToVerify)
                                        if (kit != null) {
                                            verificationResult = RecoveryKitManager.verifyKit(kit, config)
                                        } else {
                                            verificationResult = RecoveryKitVerificationResult(
                                                isValid = false,
                                                version = 0,
                                                vaultIdMatches = false,
                                                saltMatches = false,
                                                integrityVerified = false,
                                                message = strings.recoveryKitInvalidJson
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = kitJsonToVerify.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.verifyRecoveryKit, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (verificationResult != null) {
                        val res = verificationResult!!
                        val displayMsg = when {
                            res.isValid -> strings.recoveryKitMatchSuccess
                            res.message == strings.recoveryKitInvalidJson -> strings.recoveryKitInvalidJson
                            else -> strings.recoveryKitMismatch
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (res.isValid) SecurityGreenContainer else SecurityRedContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (res.isValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (res.isValid) SecurityGreen else SecurityRed
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (res.isValid) strings.recoveryKitValid else strings.recoveryKitInvalid,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(text = displayMsg, color = Color(0xFFE2E8F0), fontSize = 12.sp, lineHeight = 16.sp)
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
