package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.VaultI18n
import com.example.i18n.VaultStrings
import com.example.model.VaultItem
import com.example.model.VaultType
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrashScreen(
    items: List<VaultItem>,
    strings: VaultStrings,
    onRestore: (String) -> Unit,
    onPermanentDelete: (String) -> Unit,
    onEmptyTrash: () -> Unit
) {
    val langCode = com.example.i18n.LocalLanguageCode.current

    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var itemToPermanentlyDelete by remember { mutableStateOf<VaultItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
    ) {
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(VaultSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.trashEmptyTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.trashEmptyDesc,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.trashItemsCount(items.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        TextButton(
                            onClick = { showEmptyTrashConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = SecurityRed)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.emptyTrash, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(items, key = { it.id }) { item ->
                    val (icon, color) = when (item.type) {
                        VaultType.FILE -> Icons.Default.Description to SecurityGreen
                        VaultType.NOTE -> Icons.Default.EditNote to Color(0xFFA855F7)
                        VaultType.PASSWORD -> Icons.Default.VpnKey to CyberCyan
                        VaultType.RECORD -> Icons.Default.Badge to SecurityAmber
                    }

                    val isPrivacyMode = LocalPrivacyMode.current

                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                PrivacyMaskedFilename(
                                    filename = item.title,
                                    isMasked = isPrivacyMode,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                PrivacyMaskedText(
                                    text = strings.deletedDateLabel(VaultI18n.formatDate(item.trashTimestamp ?: item.updatedAt, langCode)),
                                    isMasked = isPrivacyMode,
                                    maskPlaceholder = strings.maskPlaceholderProtected,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            // Restore button
                            IconButton(onClick = { onRestore(item.id) }) {
                                Icon(
                                    Icons.Default.Restore,
                                    contentDescription = strings.restore,
                                    tint = SecurityGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Permanent Delete button
                            IconButton(onClick = { itemToPermanentlyDelete = item }) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = strings.permanentDelete,
                                    tint = SecurityRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Empty Trash Dialog
        if (showEmptyTrashConfirm) {
            ConfirmDialog(
                title = strings.emptyTrash,
                message = strings.trashEmptyConfirm,
                confirmText = strings.emptyTrash,
                cancelText = strings.cancel,
                isDestructive = true,
                onConfirm = {
                    onEmptyTrash()
                    showEmptyTrashConfirm = false
                },
                onDismiss = { showEmptyTrashConfirm = false }
            )
        }

        // Permanent Delete Dialog
        if (itemToPermanentlyDelete != null) {
            ConfirmDialog(
                title = strings.permanentDelete,
                message = strings.permanentDeleteConfirm,
                confirmText = strings.permanentDelete,
                cancelText = strings.cancel,
                isDestructive = true,
                onConfirm = {
                    onPermanentDelete(itemToPermanentlyDelete!!.id)
                    itemToPermanentlyDelete = null
                },
                onDismiss = { itemToPermanentlyDelete = null }
            )
        }
    }
}
