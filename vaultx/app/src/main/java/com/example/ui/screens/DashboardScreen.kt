package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.VaultI18n
import com.example.i18n.VaultStrings
import com.example.model.SecurityAuditSummary
import com.example.model.VaultItem
import com.example.model.VaultType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.VaultNavTab
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    items: List<VaultItem>,
    audit: SecurityAuditSummary,
    strings: VaultStrings,
    onNavigateTab: (VaultNavTab) -> Unit,
    onItemClick: (VaultItem) -> Unit,
    onNewFileClick: () -> Unit,
    onNewNoteClick: () -> Unit,
    onNewPasswordClick: () -> Unit,
    onNewRecordClick: () -> Unit
) {
    val langCode = com.example.i18n.LocalLanguageCode.current
    val recentItems = items.take(6)

    fun formatBytes(bytes: Long): String = VaultI18n.formatBytes(bytes, langCode)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Security Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SecurityGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.vaultProtected,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyberCyanContainer
                        ) {
                            Text(
                                text = strings.gcm256,
                                color = CyberCyanLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SecurityBadge(
                            icon = Icons.Default.LockClock,
                            label = strings.autoLockLabel,
                            value = "${audit.autoLockMinutes}m",
                            color = CyberCyan
                        )
                        SecurityBadge(
                            icon = Icons.Default.Backup,
                            label = strings.backupLabel,
                            value = if (audit.lastBackupTimestamp != null) strings.backupActive else strings.backupPending,
                            color = if (audit.lastBackupTimestamp != null) SecurityGreen else SecurityAmber
                        )
                        SecurityBadge(
                            icon = Icons.Default.PhonelinkLock,
                            label = strings.storageLabel,
                            value = strings.storageLocalOnly,
                            color = CyberCyanLight
                        )
                    }
                }
            }
        }

        // Stats Overview Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = strings.totalItems,
                        count = audit.totalItems.toString(),
                        subtitle = strings.encryptedItems,
                        icon = Icons.Default.Inventory2,
                        color = CyberCyan,
                        onClick = { onNavigateTab(VaultNavTab.FILES) }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = strings.files,
                        count = audit.totalFiles.toString(),
                        subtitle = formatBytes(audit.totalStorageBytes),
                        icon = Icons.Default.AttachFile,
                        color = SecurityGreen,
                        onClick = { onNavigateTab(VaultNavTab.FILES) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = strings.notes,
                        count = audit.totalNotes.toString(),
                        subtitle = strings.secureNotes,
                        icon = Icons.Default.EditNote,
                        color = Color(0xFFA855F7), // Purple
                        onClick = { onNavigateTab(VaultNavTab.NOTES) }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = strings.passwords,
                        count = audit.totalPasswords.toString(),
                        subtitle = if (audit.weakPasswordsCount > 0) strings.weakCountLabel(audit.weakPasswordsCount) else strings.allStrongLabel,
                        icon = Icons.Default.VpnKey,
                        color = if (audit.weakPasswordsCount > 0) SecurityAmber else CyberCyan,
                        onClick = { onNavigateTab(VaultNavTab.PASSWORDS) }
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            Text(
                text = strings.quickActions,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    label = strings.newFile,
                    icon = Icons.Default.UploadFile,
                    color = CyberCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNewFileClick
                )
                QuickActionButton(
                    label = strings.newNote,
                    icon = Icons.AutoMirrored.Filled.NoteAdd,
                    color = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f),
                    onClick = onNewNoteClick
                )
                QuickActionButton(
                    label = strings.newPassword,
                    icon = Icons.Default.Key,
                    color = SecurityGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNewPasswordClick
                )
                QuickActionButton(
                    label = strings.newRecord,
                    icon = Icons.Default.PostAdd,
                    color = SecurityAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onNewRecordClick
                )
            }
        }

        // Recent Items Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.recentItems,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextSecondary
                )
                if (items.isNotEmpty()) {
                    TextButton(onClick = { onNavigateTab(VaultNavTab.FILES) }) {
                        Text(strings.all, color = CyberCyan, fontSize = 13.sp)
                    }
                }
            }
        }

        if (recentItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.noItemsFound,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = strings.dashboardEmptyDesc,
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(recentItems, key = { it.id }) { item ->
                RecentItemRow(item = item, onClick = { onItemClick(item) }, langCode = langCode, strings = strings)
            }
        }
    }
}

@Composable
fun SecurityBadge(icon: ImageVector, label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(label, fontSize = 10.sp, color = TextMuted)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    count: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = VaultSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = count, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = VaultSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RecentItemRow(item: VaultItem, onClick: () -> Unit, langCode: String, strings: com.example.i18n.VaultStrings) {
    val (icon, color) = when (item.type) {
        VaultType.FILE -> Icons.Default.Description to SecurityGreen
        VaultType.NOTE -> Icons.Default.EditNote to Color(0xFFA855F7)
        VaultType.PASSWORD -> Icons.Default.VpnKey to CyberCyan
        VaultType.RECORD -> Icons.Default.Badge to SecurityAmber
    }

    val isPrivacyMode = LocalPrivacyMode.current

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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrivacyMaskedThumbnail(
                size = 40.dp,
                isMasked = isPrivacyMode
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrivacyMaskedFilename(
                        filename = item.title,
                        isMasked = isPrivacyMode,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = strings.pinned,
                            tint = SecurityAmber,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrivacyMaskedText(
                        text = item.folder,
                        isMasked = isPrivacyMode,
                        maskPlaceholder = strings.maskPlaceholderVault,
                        color = CyberCyanLight,
                        fontSize = 11.sp
                    )
                    PrivacyMaskedText(
                        text = " • ${VaultI18n.formatDateTime(item.updatedAt, langCode)}",
                        isMasked = isPrivacyMode,
                        maskPlaceholder = strings.maskPlaceholderProtected,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
