package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultAuditLogEntity
import com.example.i18n.VaultStrings
import com.example.model.SecurityAuditSummary
import com.example.ui.theme.*
import com.example.ui.viewmodel.VaultNavTab
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(
    audit: SecurityAuditSummary,
    auditLogs: List<VaultAuditLogEntity> = emptyList(),
    consecutiveFailures: Int = 0,
    remainingLockoutSeconds: Int = 0,
    strings: VaultStrings,
    onNavigateTab: (VaultNavTab) -> Unit,
    onChangeMasterPasswordClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Calculate real security score (0 to 100)
    var score = 100
    if (audit.autoLockMinutes <= 0) score -= 25
    if (audit.weakPasswordsCount > 0) score -= (audit.weakPasswordsCount * 10).coerceAtMost(30)
    if (audit.reusedPasswordsCount > 0) score -= (audit.reusedPasswordsCount * 10).coerceAtMost(25)
    if (audit.lastBackupTimestamp == null) score -= 15
    score = score.coerceIn(20, 100)

    val (scoreColor, scoreLabel) = when {
        score >= 90 -> SecurityGreen to strings.healthExcellent
        score >= 70 -> CyberCyan to strings.healthGood
        score >= 50 -> SecurityAmber to strings.healthAttention
        else -> SecurityRed to strings.healthCritical
    }

    val filteredLogs = remember(auditLogs, selectedFilter) {
        when (selectedFilter) {
            "SUCCESSFUL" -> auditLogs.filter { it.severity == "SUCCESS" || it.action.contains("SUCCESS") }
            "FAILED" -> auditLogs.filter { it.severity == "WARNING" || it.severity == "DANGER" || it.action.contains("FAIL") }
            "UNLOCKS" -> auditLogs.filter { it.category == "UNLOCK" || it.action.contains("UNLOCK") }
            "RECOVERY" -> auditLogs.filter { it.category == "RECOVERY" || it.action.contains("RECOVERY") }
            "BACKUPS" -> auditLogs.filter { it.category == "BACKUP" || it.action.contains("BACKUP") }
            "PROTECTED" -> auditLogs.filter { it.action.contains("PASSWORD") || it.action.contains("FILE_PROTECT") }
            "SETTINGS" -> auditLogs.filter { it.category == "SETTINGS" || it.action.contains("CONFIG") }
            else -> auditLogs
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Overall Security Score Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(scoreColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$score%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = scoreColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Vault Health: $scoreLabel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )

                    Text(
                        text = "Real-time cryptographic, credential & storage audit",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Section: Access Attempts & Progressive Delay
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VaultSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = strings.accessAttemptsTitle, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (remainingLockoutSeconds > 0) SecurityRedContainer else SecurityGreenContainer
                        ) {
                            Text(
                                text = if (remainingLockoutSeconds > 0) strings.rateLimitedStatus else strings.normalStatus,
                                color = if (remainingLockoutSeconds > 0) SecurityRed else SecurityGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (consecutiveFailures > 0) {
                            strings.consecutiveFailuresCount(consecutiveFailures)
                        } else {
                            "Zero consecutive failures. Active session is authenticated and secure."
                        },
                        color = if (consecutiveFailures > 0) SecurityAmber else TextSecondary,
                        fontSize = 12.sp
                    )
                    if (remainingLockoutSeconds > 0) {
                        Text(
                            text = strings.rateLimitLockoutNotice(remainingLockoutSeconds),
                            color = SecurityRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Section: Password Health Audit
        item {
            Text(
                text = strings.passwordAudit,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        item {
            SecurityCheckCard(
                icon = Icons.Default.VpnKey,
                title = strings.weakPasswordsTitle,
                status = if (audit.weakPasswordsCount > 0) "${audit.weakPasswordsCount} ${strings.weakPasswordsFound}" else strings.noWeakPasswords,
                isPassed = audit.weakPasswordsCount == 0,
                actionLabel = if (audit.weakPasswordsCount > 0) strings.reviewPasswordsAction else null,
                onAction = { onNavigateTab(VaultNavTab.PASSWORDS) }
            )
        }

        item {
            SecurityCheckCard(
                icon = Icons.Default.CopyAll,
                title = strings.reusedPasswordsTitle,
                status = if (audit.reusedPasswordsCount > 0) "${audit.reusedPasswordsCount} ${strings.reusedPasswordsFound}" else strings.noReusedPasswords,
                isPassed = audit.reusedPasswordsCount == 0,
                actionLabel = if (audit.reusedPasswordsCount > 0) strings.resolveDuplicatesAction else null,
                onAction = { onNavigateTab(VaultNavTab.PASSWORDS) }
            )
        }

        // Section: Session & Access Protection
        item {
            Text(
                text = strings.accessProtectionTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        item {
            SecurityCheckCard(
                icon = Icons.Default.Timer,
                title = strings.autoLock,
                status = if (audit.autoLockMinutes > 0) strings.autoLockActiveStatus(audit.autoLockMinutes) else strings.autoLockDisabledWarning,
                isPassed = audit.autoLockMinutes > 0,
                actionLabel = strings.configureAction,
                onAction = { onNavigateTab(VaultNavTab.SETTINGS) }
            )
        }

        item {
            SecurityCheckCard(
                icon = Icons.Default.Password,
                title = strings.changeMasterPassword,
                status = strings.pbkdf2IterationsDesc,
                isPassed = true,
                actionLabel = strings.updateKeyAction,
                onAction = onChangeMasterPasswordClick
            )
        }

        // Section: Security Activity Log Viewer
        item {
            Column {
                Text(
                    text = strings.securityActivity,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        "ALL" to strings.filterAll,
                        "SUCCESSFUL" to strings.filterSuccessful,
                        "FAILED" to strings.filterFailed,
                        "UNLOCKS" to strings.filterUnlocks,
                        "RECOVERY" to strings.filterRecovery,
                        "BACKUPS" to strings.filterBackups,
                        "PROTECTED" to strings.filterProtectedFiles,
                        "SETTINGS" to strings.filterSecurityChanges
                    )
                    filterOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyanContainer,
                                selectedLabelColor = CyberCyanLight
                            )
                        )
                    }
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = strings.noSecurityEventsFound,
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(filteredLogs.take(30)) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (tint, bg) = when (log.severity) {
                            "DANGER" -> SecurityRed to SecurityRedContainer
                            "WARNING" -> SecurityAmber to SecurityRedContainer.copy(alpha = 0.5f)
                            "SUCCESS" -> SecurityGreen to SecurityGreenContainer
                            else -> CyberCyan to CyberCyanContainer
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when (log.severity) {
                                    "DANGER", "WARNING" -> Icons.Default.Warning
                                    "SUCCESS" -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = log.action, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                                Text(text = dateFormat.format(Date(log.timestamp)), color = TextMuted, fontSize = 10.sp)
                            }
                            Text(text = log.details, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityCheckCard(
    icon: ImageVector,
    title: String,
    status: String,
    isPassed: Boolean,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VaultSurface),
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
                        .background(if (isPassed) SecurityGreenContainer else SecurityRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isPassed) SecurityGreen else SecurityRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text(text = status, color = if (isPassed) TextSecondary else SecurityAmber, fontSize = 12.sp, lineHeight = 16.sp)
                }

                if (actionLabel != null) {
                    TextButton(onClick = onAction) {
                        Text(actionLabel, color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
