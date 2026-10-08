package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.privacy.VaultPrivacyManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.VaultStrings
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    items: List<VaultItem>,
    isPrivacyModeEnabled: Boolean = false,
    strings: VaultStrings,
    onUploadFile: (fileName: String, mimeType: String, bytes: ByteArray, filePassword: String?) -> Unit,
    onDecryptFile: suspend (VaultItem, String?) -> ByteArray?,
    onChangeFilePassword: (VaultItem, String, String, () -> Unit, (String) -> Unit) -> Unit,
    onRemoveFilePassword: (VaultItem, String, () -> Unit, (String) -> Unit) -> Unit,
    onAddFilePassword: (VaultItem, String, () -> Unit, (String) -> Unit) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoveToTrash: (String) -> Unit,
    onScanDuplicates: suspend () -> List<DuplicateGroup> = { emptyList() }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    var selectedCollection by remember { mutableStateOf(SmartCollectionType.ALL) }
    var temporarilyRevealedIds by remember { mutableStateOf(setOf<String>()) }

    // Duplicate detector states
    var isScanningDuplicates by remember { mutableStateOf(false) }
    var duplicateGroups by remember { mutableStateOf<List<DuplicateGroup>?>(null) }
    var selectedDuplicatesToTrash by remember { mutableStateOf(setOf<String>()) }

    // Filtered items based on Smart Collection
    val filteredFileItems = remember(items, selectedCollection) {
        val base = items.filter { it.type == VaultType.FILE && !it.isTrash }
        when (selectedCollection) {
            SmartCollectionType.ALL -> base
            SmartCollectionType.DOCUMENTS -> base.filter { item ->
                val ext = (item.fileName ?: item.title).substringAfterLast('.', "").lowercase()
                ext in listOf("pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx")
            }
            SmartCollectionType.IMAGES -> base.filter { item ->
                val ext = (item.fileName ?: item.title).substringAfterLast('.', "").lowercase()
                ext in listOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "svg") || item.fileMimeType?.startsWith("image/") == true
            }
            SmartCollectionType.VIDEOS -> base.filter { item ->
                val ext = (item.fileName ?: item.title).substringAfterLast('.', "").lowercase()
                ext in listOf("mp4", "mkv", "webm", "avi", "mov") || item.fileMimeType?.startsWith("video/") == true
            }
            SmartCollectionType.AUDIO -> base.filter { item ->
                val ext = (item.fileName ?: item.title).substringAfterLast('.', "").lowercase()
                ext in listOf("mp3", "wav", "flac", "aac", "m4a", "ogg") || item.fileMimeType?.startsWith("audio/") == true
            }
            SmartCollectionType.PROTECTED -> base.filter { it.isPasswordProtected }
            SmartCollectionType.FAVORITES -> base.filter { it.isFavorite }
            SmartCollectionType.RECENTLY_ADDED -> {
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 86400000L)
                base.filter { it.createdAt >= sevenDaysAgo }
            }
            SmartCollectionType.RECENTLY_OPENED -> {
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 86400000L)
                base.filter { it.updatedAt >= sevenDaysAgo }
            }
        }
    }

    // Upload dialog state
    var pendingUploadData by remember { mutableStateOf<Triple<String, String, ByteArray>?>(null) }

    // Preview state
    var previewItem by remember { mutableStateOf<VaultItem?>(null) }
    var previewBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Password unlock state for previewing protected file
    var itemToUnlockForPreview by remember { mutableStateOf<VaultItem?>(null) }

    // Password management dialogs state
    var itemForChangePassword by remember { mutableStateOf<VaultItem?>(null) }
    var itemForRemovePassword by remember { mutableStateOf<VaultItem?>(null) }
    var itemForAddPassword by remember { mutableStateOf<VaultItem?>(null) }

    // Exporting state
    var itemToExport by remember { mutableStateOf<VaultItem?>(null) }
    var bytesToExport by remember { mutableStateOf<ByteArray?>(null) }

    // Deletion confirm state
    var itemToDelete by remember { mutableStateOf<VaultItem?>(null) }

    // SAF Document Picker for uploading
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = strings.defaultDocumentName
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    pendingUploadData = Triple(fileName, mimeType, bytes)
                } else {
                    VaultPrivacyManager.showSanitizedToast(context, strings.cannotReadEmptyFile, isPrivacyModeEnabled)
                }
            } catch (e: Exception) {
                VaultPrivacyManager.showSanitizedToast(context, strings.errorImportingFile(e.message ?: ""), isPrivacyModeEnabled)
            }
        }
    }

    // SAF Document Creator for exporting decrypted file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null && bytesToExport != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os: OutputStream ->
                    os.write(bytesToExport!!)
                    os.flush()
                }
                VaultPrivacyManager.showSanitizedToast(context, strings.fileExportedSuccess, isPrivacyModeEnabled)
            } catch (e: Exception) {
                VaultPrivacyManager.showSanitizedToast(context, strings.exportError(e.message ?: ""), isPrivacyModeEnabled)
            } finally {
                itemToExport = null
                bytesToExport = null
            }
        }
    }

    fun formatBytes(bytes: Long): String = strings.formatBytes(bytes)

    fun openPreview(item: VaultItem) {
        if (item.isPasswordProtected) {
            itemToUnlockForPreview = item
        } else {
            coroutineScope.launch {
                val bytes = onDecryptFile(item, null)
                if (bytes != null) {
                    previewItem = item
                    previewBytes = bytes
                } else {
                    VaultPrivacyManager.showSanitizedToast(context, strings.failedDecryptPreview, isPrivacyModeEnabled)
                }
            }
        }
    }

    fun startExport(item: VaultItem) {
        if (item.isPasswordProtected) {
            itemToUnlockForPreview = item
        } else {
            coroutineScope.launch {
                val bytes = onDecryptFile(item, null)
                if (bytes != null) {
                    itemToExport = item
                    bytesToExport = bytes
                    val suggestedName = item.fileName ?: item.title
                    exportLauncher.launch(suggestedName)
                } else {
                    VaultPrivacyManager.showSanitizedToast(context, strings.failedDecryptExport, isPrivacyModeEnabled)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
    ) {
        // Smart Collections & Tools Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val collections = listOf(
                SmartCollectionType.ALL to strings.collectionAll,
                SmartCollectionType.DOCUMENTS to strings.collectionDocuments,
                SmartCollectionType.IMAGES to strings.collectionImages,
                SmartCollectionType.VIDEOS to strings.collectionVideos,
                SmartCollectionType.AUDIO to strings.collectionAudio,
                SmartCollectionType.PROTECTED to strings.collectionProtected,
                SmartCollectionType.FAVORITES to strings.collectionFavorites,
                SmartCollectionType.RECENTLY_ADDED to strings.collectionRecentAdded,
                SmartCollectionType.RECENTLY_OPENED to strings.collectionRecentOpened
            )

            collections.forEach { (type, label) ->
                FilterChip(
                    selected = selectedCollection == type,
                    onClick = { selectedCollection = type },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyanContainer,
                        selectedLabelColor = CyberCyanLight
                    )
                )
            }

            // Duplicate Scan Chip Action
            AssistChip(
                onClick = {
                    isScanningDuplicates = true
                    coroutineScope.launch {
                        val dups = onScanDuplicates()
                        isScanningDuplicates = false
                        if (dups.isEmpty()) {
                            VaultPrivacyManager.showSanitizedToast(context, strings.noDuplicatesFound, isPrivacyModeEnabled)
                        } else {
                            duplicateGroups = dups
                            selectedDuplicatesToTrash = dups.flatMap { it.duplicates.map { d -> d.id } }.toSet()
                        }
                    }
                },
                leadingIcon = {
                    if (isScanningDuplicates) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CyberCyan)
                    } else {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                    }
                },
                label = { Text(strings.duplicateDetector, fontSize = 12.sp, color = CyberCyan) }
            )
        }

        if (filteredFileItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(VaultSurfaceCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = strings.noItemsFound, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.newFile, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.filesCount(filteredFileItems.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.newFile, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(filteredFileItems, key = { it.id }) { item ->
                    val isRevealed = temporarilyRevealedIds.contains(item.id)
                    FileItemCard(
                        item = item,
                        isPrivacyModeEnabled = isPrivacyModeEnabled,
                        isTemporarilyRevealed = isRevealed,
                        onToggleReveal = {
                            temporarilyRevealedIds = if (isRevealed) {
                                temporarilyRevealedIds - item.id
                            } else {
                                temporarilyRevealedIds + item.id
                            }
                        },
                        strings = strings,
                        formattedSize = formatBytes(item.fileSizeBytes ?: 0L),
                        formattedDate = strings.formatDate(item.createdAt),
                        onPreview = { openPreview(item) },
                        onExport = {
                            if (previewBytes != null && previewItem?.id == item.id) {
                                itemToExport = item
                                bytesToExport = previewBytes
                                exportLauncher.launch(item.fileName ?: item.title)
                            } else {
                                startExport(item)
                            }
                        },
                        onChangePassword = { itemForChangePassword = item },
                        onRemovePassword = { itemForRemovePassword = item },
                        onAddPassword = { itemForAddPassword = item },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }

        // Duplicate Review Dialog
        if (duplicateGroups != null) {
            val groups = duplicateGroups!!
            AlertDialog(
                onDismissRequest = { duplicateGroups = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.duplicateDetector, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        Text(
                            text = strings.duplicatesDetected(groups.size),
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(groups) { group ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "${strings.originalFile}: ${group.original.fileName ?: group.original.title}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = SecurityGreen
                                        )
                                        Text(
                                            text = strings.duplicateInfoSubtitle(formatBytes(group.fileSizeBytes), group.duplicates.size),
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        group.duplicates.forEach { dup ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = selectedDuplicatesToTrash.contains(dup.id),
                                                    onCheckedChange = { checked ->
                                                        selectedDuplicatesToTrash = if (checked) {
                                                            selectedDuplicatesToTrash + dup.id
                                                        } else {
                                                            selectedDuplicatesToTrash - dup.id
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = SecurityRed)
                                                )
                                                Text(
                                                    text = dup.fileName ?: dup.title,
                                                    fontSize = 12.sp,
                                                    color = TextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedDuplicatesToTrash.forEach { dupId ->
                                onMoveToTrash(dupId)
                            }
                            VaultPrivacyManager.showSanitizedToast(
                                context = context,
                                rawMessage = strings.movedDuplicatesToTrash(selectedDuplicatesToTrash.size),
                                isPrivacyMode = isPrivacyModeEnabled,
                                customGenericMessage = strings.storageDuplicatesCleaned
                            )
                            duplicateGroups = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecurityRed, contentColor = Color.White),
                        enabled = selectedDuplicatesToTrash.isNotEmpty()
                    ) {
                        Text(strings.keepOneMoveOthersToTrash, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { duplicateGroups = null }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        // Upload File with Optional Password Dialog
        if (pendingUploadData != null) {
            val (fname, fmime, fbytes) = pendingUploadData!!
            UploadFileDialog(
                fileName = fname,
                fileSize = formatBytes(fbytes.size.toLong()),
                strings = strings,
                onDismiss = { pendingUploadData = null },
                onConfirm = { password ->
                    onUploadFile(fname, fmime, fbytes, password)
                    pendingUploadData = null
                    VaultPrivacyManager.showSanitizedToast(
                        context = context,
                        rawMessage = strings.fileEncryptedToast,
                        isPrivacyMode = isPrivacyModeEnabled,
                        customGenericMessage = strings.vaultItemSecured
                    )
                }
            )
        }

        // Protected File Password Unlock Dialog
        if (itemToUnlockForPreview != null) {
            val item = itemToUnlockForPreview!!
            FilePasswordUnlockDialog(
                item = item,
                strings = strings,
                onDismiss = { itemToUnlockForPreview = null },
                onUnlock = { password ->
                    val bytes = onDecryptFile(item, password)
                    bytes != null
                },
                onSuccess = { password ->
                    coroutineScope.launch {
                        val bytes = onDecryptFile(item, password)
                        if (bytes != null) {
                            previewItem = item
                            previewBytes = bytes
                            itemToUnlockForPreview = null
                        }
                    }
                }
            )
        }

        // Universal Preview Dialog
        if (previewItem != null && previewBytes != null) {
            UniversalFilePreviewDialog(
                item = previewItem!!,
                fileBytes = previewBytes!!,
                strings = strings,
                onDismiss = {
                    previewItem = null
                    previewBytes = null
                },
                onExport = {
                    itemToExport = previewItem
                    bytesToExport = previewBytes
                    val suggestedName = previewItem?.fileName ?: previewItem?.title ?: "decrypted_file"
                    exportLauncher.launch(suggestedName)
                }
            )
        }

        // Change File Password Dialog
        if (itemForChangePassword != null) {
            val item = itemForChangePassword!!
            ChangeFilePasswordDialog(
                item = item,
                strings = strings,
                onDismiss = { itemForChangePassword = null },
                onSubmit = { oldPass, newPass ->
                    onChangeFilePassword(
                        item,
                        oldPass,
                        newPass,
                        {
                            itemForChangePassword = null
                            VaultPrivacyManager.showSanitizedToast(context, strings.filePasswordChanged, isPrivacyModeEnabled)
                        },
                        { err ->
                            VaultPrivacyManager.showSanitizedToast(context, err, isPrivacyModeEnabled)
                        }
                    )
                }
            )
        }

        // Remove File Password Dialog
        if (itemForRemovePassword != null) {
            val item = itemForRemovePassword!!
            RemoveFilePasswordDialog(
                item = item,
                strings = strings,
                onDismiss = { itemForRemovePassword = null },
                onSubmit = { pass ->
                    onRemoveFilePassword(
                        item,
                        pass,
                        {
                            itemForRemovePassword = null
                            VaultPrivacyManager.showSanitizedToast(context, strings.filePasswordRemoved, isPrivacyModeEnabled)
                        },
                        { err ->
                            VaultPrivacyManager.showSanitizedToast(context, err, isPrivacyModeEnabled)
                        }
                    )
                }
            )
        }

        // Add File Password Dialog
        if (itemForAddPassword != null) {
            val item = itemForAddPassword!!
            AddFilePasswordDialog(
                item = item,
                strings = strings,
                onDismiss = { itemForAddPassword = null },
                onSubmit = { pass ->
                    onAddFilePassword(
                        item,
                        pass,
                        {
                            itemForAddPassword = null
                            VaultPrivacyManager.showSanitizedToast(context, strings.filePasswordAdded, isPrivacyModeEnabled)
                        },
                        { err ->
                            VaultPrivacyManager.showSanitizedToast(context, err, isPrivacyModeEnabled)
                        }
                    )
                }
            )
        }

        // Delete Confirm Dialog
        if (itemToDelete != null) {
            ConfirmDialog(
                title = strings.moveToTrash,
                message = strings.moveToTrashConfirmMsg(itemToDelete?.title ?: ""),
                confirmText = strings.moveToTrash,
                cancelText = strings.cancel,
                isDestructive = true,
                onConfirm = {
                    onMoveToTrash(itemToDelete!!.id)
                    itemToDelete = null
                },
                onDismiss = { itemToDelete = null }
            )
        }
    }
}

/**
 * Upload Dialog that allows optional file-specific password protection.
 */
@Composable
private fun UploadFileDialog(
    fileName: String,
    fileSize: String,
    strings: VaultStrings,
    onDismiss: () -> Unit,
    onConfirm: (filePassword: String?) -> Unit
) {
    var protectWithPassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.newFile, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = VaultSurfaceCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = fileName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                        Text(text = fileSize, fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { protectWithPassword = !protectWithPassword }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = protectWithPassword,
                        onCheckedChange = { protectWithPassword = it },
                        colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.protectWithSeparatePassword,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                AnimatedVisibility(visible = protectWithPassword) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text(strings.filePassword) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            label = { Text(strings.confirmFilePassword) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = SecurityRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (protectWithPassword) {
                        if (password.length < 6) {
                            errorMessage = strings.passwordMinLength
                            return@Button
                        }
                        if (password != confirmPassword) {
                            errorMessage = strings.passwordMismatch
                            return@Button
                        }
                        onConfirm(password)
                    } else {
                        onConfirm(null)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
            ) {
                Text(strings.encryptAndSave, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun FileItemCard(
    item: VaultItem,
    isPrivacyModeEnabled: Boolean = false,
    isTemporarilyRevealed: Boolean = false,
    onToggleReveal: () -> Unit = {},
    strings: VaultStrings,
    formattedSize: String,
    formattedDate: String,
    onPreview: () -> Unit,
    onExport: () -> Unit,
    onChangePassword: () -> Unit,
    onRemovePassword: () -> Unit,
    onAddPassword: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val icon = when {
        item.fileMimeType?.startsWith("image/") == true -> Icons.Default.Image
        item.fileMimeType?.startsWith("video/") == true -> Icons.Default.VideoFile
        item.fileMimeType?.startsWith("audio/") == true -> Icons.Default.AudioFile
        item.fileMimeType == "application/pdf" -> Icons.Default.PictureAsPdf
        else -> Icons.Default.Description
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() },
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrivacyMaskedThumbnail(
                size = 44.dp,
                isMasked = isPrivacyModeEnabled,
                isTemporarilyRevealed = isTemporarilyRevealed
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberCyanContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isPasswordProtected) {
                        Surface(
                            color = SecurityAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = strings.protectedBadge,
                                    tint = SecurityAmber,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = strings.protectedBadge,
                                    fontSize = 10.sp,
                                    color = SecurityAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    PrivacyMaskedFilename(
                        filename = item.fileName ?: item.title,
                        isMasked = isPrivacyModeEnabled,
                        isTemporarilyRevealed = isTemporarilyRevealed,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (item.isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Star, contentDescription = strings.favorite, tint = SecurityAmber, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrivacyMaskedText(
                        text = formattedSize,
                        isMasked = isPrivacyModeEnabled,
                        isTemporarilyRevealed = isTemporarilyRevealed,
                        maskPlaceholder = strings.encryptedContentProtected,
                        fontSize = 11.sp,
                        color = CyberCyan,
                        fontWeight = FontWeight.Medium
                    )
                    PrivacyMaskedText(
                        text = " • $formattedDate",
                        isMasked = isPrivacyModeEnabled,
                        isTemporarilyRevealed = isTemporarilyRevealed,
                        maskPlaceholder = strings.maskPlaceholderProtected,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            if (isPrivacyModeEnabled) {
                IconButton(onClick = onToggleReveal) {
                    Icon(
                        if (isTemporarilyRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = strings.temporarilyReveal,
                        tint = SecurityGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(onClick = onPreview) {
                    Icon(Icons.Default.Visibility, contentDescription = strings.preview, tint = CyberCyan, modifier = Modifier.size(20.dp))
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = strings.options, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(VaultSurfaceCard)
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.preview, color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = CyberCyan) },
                        onClick = {
                            onPreview()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.exportFile, color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = SecurityGreen) },
                        onClick = {
                            onExport()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.isFavorite) strings.unfavorite else strings.favorite, color = TextPrimary) },
                        leadingIcon = { Icon(if (item.isFavorite) Icons.Default.StarBorder else Icons.Default.Star, contentDescription = null, tint = SecurityAmber) },
                        onClick = {
                            onToggleFavorite()
                            showMenu = false
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = VaultOutline)

                    if (item.isPasswordProtected) {
                        DropdownMenuItem(
                            text = { Text(strings.changeFilePassword, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Password, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                onChangePassword()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.removeFilePassword, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                onRemovePassword()
                                showMenu = false
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(strings.addFilePassword, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                onAddPassword()
                                showMenu = false
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = VaultOutline)

                    DropdownMenuItem(
                        text = { Text(strings.moveToTrash, color = SecurityRed) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = SecurityRed) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
