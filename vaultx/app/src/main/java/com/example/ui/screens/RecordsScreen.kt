package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.VaultStrings
import com.example.model.*
import com.example.privacy.VaultPrivacyManager
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun RecordsScreen(
    items: List<VaultItem>,
    strings: VaultStrings,
    onDecryptRecord: (VaultItem) -> SecureRecordPayload,
    onSaveRecord: (id: String?, title: String, payload: SecureRecordPayload, folder: String, tags: List<String>, isFavorite: Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoveToTrash: (String) -> Unit
) {
    val context = LocalContext.current
    val recordItems = items.filter { it.type == VaultType.RECORD }

    var isEditing by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<VaultItem?>(null) }

    var editTitle by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf(RecordCategory.CUSTOM) }
    var editFields by remember { mutableStateOf<List<SecureRecordField>>(emptyList()) }
    var editNotes by remember { mutableStateOf("") }
    var editFolder by remember { mutableStateOf(strings.defaultFolderRecords) }
    var editFavorite by remember { mutableStateOf(false) }

    var viewingItem by remember { mutableStateOf<VaultItem?>(null) }
    var viewingPayload by remember { mutableStateOf<SecureRecordPayload?>(null) }

    var itemToDelete by remember { mutableStateOf<VaultItem?>(null) }

    fun getCategoryDefaultFields(category: RecordCategory): List<SecureRecordField> {
        return when (category) {
            RecordCategory.PASSPORT -> listOf(
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldPassportNumber, "", true),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldCountry, "", false),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldExpirationDate, "", false),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldFullName, "", false)
            )
            RecordCategory.LICENSE -> listOf(
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldLicenseNumber, "", true),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldStateProvince, "", false),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldExpirationDate, "", false)
            )
            RecordCategory.BANK_ACCOUNT -> listOf(
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldBankName, "", false),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldAccountNumber, "", true),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldRoutingSwift, "", true),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldIban, "", true)
            )
            RecordCategory.CREDIT_CARD -> listOf(
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldCardholderName, "", false),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldCardNumber, "", true),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldCvvCvc, "", true),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldExpirationDate, "", false),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldPin, "", true)
            )
            RecordCategory.WIFI -> listOf(
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldNetworkName, "", false),
                SecureRecordField(UUID.randomUUID().toString(), strings.passwordField, "", true)
            )
            RecordCategory.SOFTWARE_LICENSE -> listOf(
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldProductName, "", false),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldLicenseKey, "", true)
            )
            RecordCategory.RECOVERY_SEED -> listOf(
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldRecoverySeed, "", true),
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldDerivationPath, "", false)
            )
            else -> listOf(
                SecureRecordField(UUID.randomUUID().toString(), strings.fieldDetail, "", true)
            )
        }
    }

    fun openNewRecord() {
        itemToEdit = null
        editTitle = ""
        editCategory = RecordCategory.IDENTITY
        editFields = getCategoryDefaultFields(RecordCategory.IDENTITY)
        editNotes = ""
        editFolder = strings.defaultFolderRecords
        editFavorite = false
        isEditing = true
    }

    fun openEditRecord(item: VaultItem) {
        val payload = onDecryptRecord(item)
        itemToEdit = item
        editTitle = item.title
        editCategory = payload.category
        editFields = payload.fields
        editNotes = payload.notes
        editFolder = item.folder
        editFavorite = item.isFavorite
        isEditing = true
    }

    fun openViewRecord(item: VaultItem) {
        val payload = onDecryptRecord(item)
        viewingItem = item
        viewingPayload = payload
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
    ) {
        if (recordItems.isEmpty()) {
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
                        .background(SecurityAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Badge,
                        contentDescription = null,
                        tint = SecurityAmber,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.noRecordsTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { openNewRecord() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.newRecord, fontWeight = FontWeight.Bold)
                }
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
                            text = strings.recordsCount(recordItems.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Button(
                            onClick = { openNewRecord() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.newRecord, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(recordItems, key = { it.id }) { item ->
                    val payload = remember(item) { onDecryptRecord(item) }
                    RecordCard(
                        item = item,
                        payload = payload,
                        strings = strings,
                        onClick = { openViewRecord(item) },
                        onEdit = { openEditRecord(item) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }

        // View Record Modal
        if (viewingItem != null && viewingPayload != null) {
            val item = viewingItem!!
            val payload = viewingPayload!!

            val isPrivacyMode = LocalPrivacyMode.current
            var isModalRevealed by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    viewingItem = null
                    viewingPayload = null
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrivacyMaskedFilename(
                            filename = item.title,
                            isMasked = isPrivacyMode,
                            isTemporarilyRevealed = isModalRevealed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPrivacyMode) {
                                IconButton(onClick = { isModalRevealed = !isModalRevealed }) {
                                    Icon(
                                        imageVector = if (isModalRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = strings.reveal,
                                        tint = SecurityGreen
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SecurityAmber.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = payload.category.displayName,
                                    color = SecurityAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        payload.fields.forEach { field ->
                            var isRevealed by remember { mutableStateOf(!field.isSensitive) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = VaultSlateBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(field.label, fontSize = 11.sp, color = TextMuted)
                                        val showPlain = (isRevealed || isModalRevealed) && (!isPrivacyMode || isModalRevealed)
                                        PrivacyMaskedText(
                                            text = if (showPlain) field.value else "••••••••••••",
                                            isMasked = isPrivacyMode && !isModalRevealed,
                                            maskPlaceholder = "••••••••••••",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontFamily = if (field.isSensitive) FontFamily.Monospace else FontFamily.SansSerif
                                        )
                                    }

                                    Row {
                                        if (field.isSensitive) {
                                            IconButton(
                                                onClick = { isRevealed = !isRevealed },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    if (isRevealed && (!isPrivacyMode || isModalRevealed)) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (isRevealed && (!isPrivacyMode || isModalRevealed)) strings.hide else strings.reveal,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText(field.label, field.value))
                                                VaultPrivacyManager.showSanitizedToast(
                                                    context = context,
                                                    rawMessage = strings.fieldCopiedToast(field.label),
                                                    isPrivacyMode = isPrivacyMode,
                                                    customGenericMessage = strings.recordFieldCopiedGeneric
                                                )
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = strings.copyPassword,
                                                tint = CyberCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (payload.notes.isNotBlank()) {
                            Text(
                                text = "Notes: ${payload.notes}",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewingItem = null
                            viewingPayload = null
                            openEditRecord(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                    ) {
                        Text(strings.edit, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewingItem = null
                        viewingPayload = null
                    }) {
                        Text(strings.cancel, color = TextSecondary)
                    }
                },
                containerColor = VaultSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Add / Edit Record Modal
        if (isEditing) {
            AlertDialog(
                onDismissRequest = { isEditing = false },
                title = {
                    Text(
                        text = if (itemToEdit == null) strings.newRecord else strings.editRecordTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text(strings.title) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = VaultOutline,
                                focusedContainerColor = VaultSlateBg,
                                unfocusedContainerColor = VaultSlateBg,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Category Selection
                        Text(strings.category, fontSize = 12.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(VaultSlateBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = editCategory.displayName,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Dynamic Fields Editor
                        Text(strings.fieldName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        editFields.forEachIndexed { index, field ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = field.label,
                                    onValueChange = { newLbl ->
                                        val updated = editFields.toMutableList()
                                        updated[index] = field.copy(label = newLbl)
                                        editFields = updated
                                    },
                                    label = { Text(strings.fieldName) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = VaultOutline,
                                        focusedContainerColor = VaultSlateBg,
                                        unfocusedContainerColor = VaultSlateBg,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                OutlinedTextField(
                                    value = field.value,
                                    onValueChange = { newVal ->
                                        val updated = editFields.toMutableList()
                                        updated[index] = field.copy(value = newVal)
                                        editFields = updated
                                    },
                                    label = { Text(strings.fieldValue) },
                                    modifier = Modifier.weight(1.2f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = VaultOutline,
                                        focusedContainerColor = VaultSlateBg,
                                        unfocusedContainerColor = VaultSlateBg,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                IconButton(onClick = {
                                    val updated = editFields.toMutableList()
                                    updated.removeAt(index)
                                    editFields = updated
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = SecurityRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Button(
                            onClick = {
                                editFields = editFields + SecureRecordField(UUID.randomUUID().toString(), strings.fieldName, "", true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceVariant, contentColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.addField, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text(strings.privateNotesOptional) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = VaultOutline,
                                focusedContainerColor = VaultSlateBg,
                                unfocusedContainerColor = VaultSlateBg,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editTitle.isBlank()) editTitle = editCategory.displayName
                            onSaveRecord(
                                itemToEdit?.id,
                                editTitle,
                                SecureRecordPayload(
                                    category = editCategory,
                                    fields = editFields,
                                    notes = editNotes
                                ),
                                editFolder,
                                emptyList(),
                                editFavorite
                            )
                            isEditing = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                    ) {
                        Text(strings.save, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isEditing = false }) {
                        Text(strings.cancel, color = TextSecondary)
                    }
                },
                containerColor = VaultSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Delete Confirm Dialog
        if (itemToDelete != null) {
            ConfirmDialog(
                title = strings.moveToTrash,
                message = strings.moveToTrashRecordConfirm(itemToDelete?.title ?: ""),
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

@Composable
fun RecordCard(
    item: VaultItem,
    payload: SecureRecordPayload,
    strings: VaultStrings,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
                size = 42.dp,
                isMasked = isPrivacyMode
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SecurityAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Badge,
                        contentDescription = null,
                        tint = SecurityAmber,
                        modifier = Modifier.size(24.dp)
                    )
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
                    if (item.isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Star, contentDescription = strings.favorite, tint = SecurityAmber, modifier = Modifier.size(13.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                PrivacyMaskedText(
                    text = "${payload.category.displayName} • ${payload.fields.size} field(s)",
                    isMasked = isPrivacyMode,
                    maskPlaceholder = strings.encryptedContentProtected,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = strings.menu, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(VaultSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (item.isFavorite) strings.unfavorite else strings.favorite, color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = SecurityAmber) },
                        onClick = {
                            onToggleFavorite()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.delete, color = SecurityRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SecurityRed) },
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
