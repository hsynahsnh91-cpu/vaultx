package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultAuditLogEntity
import com.example.data.VaultConfigEntity
import com.example.i18n.VaultI18n
import com.example.i18n.VaultStrings
import com.example.model.AutoLockTimeout
import com.example.privacy.VaultPrivacyManager
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.PasswordStrengthIndicator
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    config: VaultConfigEntity?,
    auditLogs: List<VaultAuditLogEntity>,
    strings: VaultStrings,
    onUpdateAutoLock: (Int) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onUpdateLockOnBackground: (Boolean) -> Unit,
    onUpdateReducedMotion: (Boolean) -> Unit = {},
    onTogglePrivacyMode: (Boolean) -> Unit = {},
    onChangeMasterPassword: (newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onWipeVault: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }

    var newMasterPass by remember { mutableStateOf("") }
    var confirmNewMasterPass by remember { mutableStateOf("") }
    var changePasswordError by remember { mutableStateOf<String?>(null) }
    var isChangingPassword by remember { mutableStateOf(false) }

    val isPrivacyMode = config?.isPrivacyModeEnabled == true

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = strings.preferencesAndSecurity,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        // Privacy Mode Toggle Card (Global state: masks filenames, thumbnails, content values & sanitizes notifications)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isPrivacyMode) SecurityGreenContainer else VaultSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = strings.privacyMode,
                                    tint = if (isPrivacyMode) SecurityGreen else CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = strings.privacyMode,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    if (isPrivacyMode) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = SecurityGreenContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = strings.active,
                                                color = SecurityGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isPrivacyMode) strings.privacyModeOn else strings.privacyModeOff,
                                    color = if (isPrivacyMode) SecurityGreen else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isPrivacyMode) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }

                        Switch(
                            checked = isPrivacyMode,
                            onCheckedChange = { newState ->
                                onTogglePrivacyMode(newState)
                                VaultPrivacyManager.showSanitizedToast(
                                    context = context,
                                    rawMessage = if (newState) strings.privacyModeEnabledToast else strings.privacyModeDisabledToast,
                                    isPrivacyMode = newState
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = SecurityGreen
                            )
                        )
                    }
                }
            }
        }

        // Lock when application enters background (Default: Enabled)
        item {
            val isLockOnBg = config?.lockOnBackground != false
            SettingsSwitchRow(
                icon = Icons.Default.Security,
                title = strings.lockOnBackground,
                subtitle = null,
                checked = isLockOnBg,
                onCheckedChange = { onUpdateLockOnBackground(it) },
                warning = null
            )
        }


        // Auto-lock setting card
        item {
            val timeoutLabel = when (config?.autoLockMinutes ?: 5) {
                1 -> strings.timeoutMinutes(1)
                5 -> strings.timeoutMinutes(5)
                15 -> strings.timeoutMinutes(15)
                30 -> strings.timeoutMinutes(30)
                0 -> strings.timeoutNever
                else -> strings.timeoutMinutes(config?.autoLockMinutes ?: 5)
            }
            SettingsItemRow(
                icon = Icons.Default.Timer,
                title = strings.autoLock,
                subtitle = timeoutLabel,
                onClick = { showAutoLockDialog = true }
            )
        }

        // Language setting card
        item {
            val langLabel = if (config?.languageCode == "ar") "العربية (Arabic)" else "English"
            SettingsItemRow(
                icon = Icons.Default.Language,
                title = strings.language,
                subtitle = langLabel,
                onClick = { showLanguageDialog = true }
            )
        }

        item {
            Text(
                text = strings.securityOperations,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        // Change Master Password
        item {
            SettingsItemRow(
                icon = Icons.Default.Password,
                title = strings.changeMasterPassword,
                subtitle = null,
                onClick = {
                    newMasterPass = ""
                    confirmNewMasterPass = ""
                    changePasswordError = null
                    showChangePasswordDialog = true
                }
            )
        }

        // Audit Logs
        item {
            SettingsItemRow(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                title = strings.auditLogsTitle,
                subtitle = strings.auditLogsRecordedCount(auditLogs.size),
                onClick = { showAuditLogsDialog = true }
            )
        }

        item {
            Text(
                text = strings.dangerZoneTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = SecurityRed
            )
        }

        // Wipe Vault Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWipeConfirm = true },
                colors = CardDefaults.cardColors(containerColor = SecurityRedContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SecurityRedContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = SecurityRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.wipeVault, fontWeight = FontWeight.Bold, color = SecurityRed, fontSize = 14.sp)
                    }
                }
            }
        }

        // App Information
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${strings.appName} 1.0.0",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Auto-Lock Timeout Dialog
    if (showAutoLockDialog) {
        val options = listOf(
            AutoLockTimeout.ONE_MINUTE to strings.timeoutMinutes(1),
            AutoLockTimeout.FIVE_MINUTES to strings.timeoutMinutes(5),
            AutoLockTimeout.FIFTEEN_MINUTES to strings.timeoutMinutes(15),
            AutoLockTimeout.THIRTY_MINUTES to strings.timeoutMinutes(30),
            AutoLockTimeout.NEVER to strings.timeoutNever
        )
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = { Text(strings.autoLock, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    options.forEach { (opt, optLabel) ->
                        val isSelected = (config?.autoLockMinutes ?: 5) == opt.minutes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateAutoLock(opt.minutes)
                                    showAutoLockDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onUpdateAutoLock(opt.minutes)
                                    showAutoLockDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(optLabel, color = if (isSelected) CyberCyan else TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAutoLockDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = VaultSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.language, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUpdateLanguage("en")
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config?.languageCode != "ar",
                            onClick = {
                                onUpdateLanguage("en")
                                showLanguageDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("English", color = TextPrimary, fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUpdateLanguage("ar")
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config?.languageCode == "ar",
                            onClick = {
                                onUpdateLanguage("ar")
                                showLanguageDialog = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("العربية (Arabic)", color = TextPrimary, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = VaultSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Change Master Password Dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text(strings.changeMasterPassword, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.changePasswordExplanation,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    if (changePasswordError != null) {
                        Text(changePasswordError!!, color = SecurityRed, fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = newMasterPass,
                        onValueChange = {
                            newMasterPass = it
                            changePasswordError = null
                        },
                        label = { Text(strings.newMasterPasswordLabel) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = VaultOutline,
                            focusedContainerColor = VaultSlateBg,
                            unfocusedContainerColor = VaultSlateBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    if (newMasterPass.isNotEmpty()) {
                        PasswordStrengthIndicator(password = newMasterPass)
                    }

                    OutlinedTextField(
                        value = confirmNewMasterPass,
                        onValueChange = {
                            confirmNewMasterPass = it
                            changePasswordError = null
                        },
                        label = { Text(strings.confirmNewPasswordLabel) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = VaultOutline,
                            focusedContainerColor = VaultSlateBg,
                            unfocusedContainerColor = VaultSlateBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMasterPass.length < 8) {
                            changePasswordError = strings.passwordMinLength
                            return@Button
                        }
                        if (newMasterPass != confirmNewMasterPass) {
                            changePasswordError = strings.passwordMismatch
                            return@Button
                        }
                        isChangingPassword = true
                        onChangeMasterPassword(
                            newMasterPass,
                            {
                                isChangingPassword = false
                                showChangePasswordDialog = false
                                VaultPrivacyManager.showSanitizedToast(
                                    context = context,
                                    rawMessage = strings.passwordChangedSuccess,
                                    isPrivacyMode = isPrivacyMode
                                )
                            },
                            { err ->
                                isChangingPassword = false
                                changePasswordError = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    enabled = !isChangingPassword && newMasterPass.isNotEmpty() && confirmNewMasterPass.isNotEmpty()
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text(strings.save, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = VaultSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Wipe Vault Confirmation Dialog
    if (showWipeConfirm) {
        ConfirmDialog(
            title = strings.wipeVault,
            message = strings.wipeVaultConfirm,
            confirmText = strings.wipeVaultConfirmButton,
            cancelText = strings.cancel,
            isDestructive = true,
            onConfirm = {
                onWipeVault()
                showWipeConfirm = false
                VaultPrivacyManager.showSanitizedToast(
                    context = context,
                    rawMessage = strings.wipeVaultErasedToast,
                    isPrivacyMode = isPrivacyMode
                )
            },
            onDismiss = { showWipeConfirm = false }
        )
    }

    // Security Audit Logs Dialog
    if (showAuditLogsDialog) {
        AlertDialog(
            onDismissRequest = { showAuditLogsDialog = false },
            title = { Text(strings.auditLogsTitle, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (auditLogs.isEmpty()) {
                        Text(strings.noAuditLogs, color = TextSecondary)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(auditLogs, key = { it.logId }) { log ->
                                Surface(
                                    color = VaultSlateBg,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(log.action, fontWeight = FontWeight.Bold, color = CyberCyan, fontSize = 12.sp)
                                            Text(VaultI18n.formatDateTime(log.timestamp, config?.languageCode ?: "en"), color = TextMuted, fontSize = 10.sp)
                                        }
                                        if (log.details.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(log.details, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditLogsDialog = false }) {
                    Text(strings.close, color = CyberCyan)
                }
            },
            containerColor = VaultSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = VaultSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CyberCyanContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                if (subtitle != null) {
                    Text(subtitle, color = TextSecondary, fontSize = 12.sp)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    warning: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberCyanContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    if (subtitle != null) {
                        Text(subtitle, color = TextSecondary, fontSize = 12.sp)
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyanContainer,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = VaultSurfaceVariant
                    )
                )
            }
            if (!checked && warning != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = warning,
                    fontSize = 11.sp,
                    color = SecurityAmber,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(start = 48.dp)
                )
            }
        }
    }
}
