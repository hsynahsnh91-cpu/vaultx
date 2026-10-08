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
import com.example.i18n.VaultI18n
import com.example.i18n.VaultStrings
import com.example.model.NotePayload
import com.example.model.VaultItem
import com.example.model.VaultType
import com.example.privacy.VaultPrivacyManager
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(
    items: List<VaultItem>,
    strings: VaultStrings,
    onDecryptNote: (VaultItem) -> NotePayload,
    onSaveNote: (id: String?, title: String, content: String, folder: String, tags: List<String>, isPinned: Boolean, isFavorite: Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onMoveToTrash: (String) -> Unit
) {
    val context = LocalContext.current
    val notes = items.filter { it.type == VaultType.NOTE }
    val langCode = com.example.i18n.LocalLanguageCode.current

    var isEditing by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<VaultItem?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var editFolder by remember { mutableStateOf(strings.defaultFolderNotes) }
    var editPinned by remember { mutableStateOf(false) }
    var editFavorite by remember { mutableStateOf(false) }

    var viewingItem by remember { mutableStateOf<VaultItem?>(null) }
    var viewingPayload by remember { mutableStateOf<NotePayload?>(null) }

    var itemToDelete by remember { mutableStateOf<VaultItem?>(null) }

    fun openNewNote() {
        itemToEdit = null
        editTitle = ""
        editContent = ""
        editFolder = strings.defaultFolderNotes
        editPinned = false
        editFavorite = false
        isEditing = true
    }

    fun openEditNote(item: VaultItem) {
        val payload = onDecryptNote(item)
        itemToEdit = item
        editTitle = item.title
        editContent = payload.content
        editFolder = item.folder
        editPinned = item.isPinned
        editFavorite = item.isFavorite
        isEditing = true
    }

    fun openViewNote(item: VaultItem) {
        val payload = onDecryptNote(item)
        viewingItem = item
        viewingPayload = payload
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
    ) {
        if (notes.isEmpty()) {
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
                        .background(Color(0xFFA855F7).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.noNotesTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { openNewNote() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.newNote, fontWeight = FontWeight.Bold)
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
                            text = strings.notesCount(notes.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Button(
                            onClick = { openNewNote() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.newNote, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(notes, key = { it.id }) { item ->
                    NoteCard(
                        item = item,
                        formattedDate = VaultI18n.formatDate(item.updatedAt, langCode),
                        onClick = { openViewNote(item) },
                        onEdit = { openEditNote(item) },
                        onTogglePin = { onTogglePin(item.id) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }

        // View Note Modal
        if (viewingItem != null && viewingPayload != null) {
            val item = viewingItem!!
            val payload = viewingPayload!!
            var isModalRevealed by remember { mutableStateOf(false) }
            val isPrivacyMode = LocalPrivacyMode.current

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
                        if (isPrivacyMode) {
                            IconButton(onClick = { isModalRevealed = !isModalRevealed }) {
                                Icon(
                                    imageVector = if (isModalRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = strings.reveal,
                                    tint = SecurityGreen
                                )
                            }
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(strings.notes, payload.content))
                            VaultPrivacyManager.showSanitizedToast(
                                context = context,
                                rawMessage = strings.noteCopiedToast,
                                isPrivacyMode = isPrivacyMode,
                                customGenericMessage = strings.noteCopiedGeneric
                            )
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = strings.copyPassword, tint = CyberCyan)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PrivacyMaskedContent(
                            modifier = Modifier.fillMaxWidth(),
                            isMasked = isPrivacyMode,
                            isTemporarilyRevealed = isModalRevealed,
                            placeholderMessage = strings.noteContentMaskedPlaceholder,
                            onTapReveal = { isModalRevealed = true }
                        ) {
                            Text(
                                text = payload.content,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = VaultSurfaceVariant
                            ) {
                                PrivacyMaskedText(
                                    text = item.folder,
                                    isMasked = isPrivacyMode,
                                    isTemporarilyRevealed = isModalRevealed,
                                    maskPlaceholder = strings.notes,
                                    fontSize = 11.sp,
                                    color = CyberCyanLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.updatedAtLabel(VaultI18n.formatDate(item.updatedAt, langCode)),
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewingItem = null
                            viewingPayload = null
                            openEditNote(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
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

        // Edit / Create Note Modal
        if (isEditing) {
            AlertDialog(
                onDismissRequest = { isEditing = false },
                title = {
                    Text(
                        text = if (itemToEdit == null) strings.newNote else strings.editNoteTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .verticalScroll(rememberScrollState())
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

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editFolder,
                            onValueChange = { editFolder = it },
                            label = { Text(strings.folderCategoryLabel) },
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

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editContent,
                            onValueChange = { editContent = it },
                            label = { Text(strings.content) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
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

                        Spacer(modifier = Modifier.height(8.dp))

                        val words = editContent.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                        Text(
                            text = strings.wordsAndCharsCount(words, editContent.length),
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = editPinned,
                                onCheckedChange = { editPinned = it }
                            )
                            Text(strings.pin, fontSize = 13.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Checkbox(
                                checked = editFavorite,
                                onCheckedChange = { editFavorite = it }
                            )
                            Text(strings.favorite, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editTitle.isBlank()) editTitle = strings.untitledNote
                            onSaveNote(
                                itemToEdit?.id,
                                editTitle,
                                editContent,
                                editFolder,
                                emptyList(),
                                editPinned,
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
                message = strings.moveToTrashNoteConfirm(itemToDelete?.title ?: ""),
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
fun NoteCard(
    item: VaultItem,
    formattedDate: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val strings = com.example.i18n.LocalVaultStrings.current
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
                        .background(Color(0xFFA855F7).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
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
                    if (item.isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.PushPin, contentDescription = strings.pinned, tint = SecurityAmber, modifier = Modifier.size(13.dp))
                    }
                    if (item.isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Star, contentDescription = strings.favorite, tint = SecurityAmber, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrivacyMaskedText(
                        text = item.folder,
                        isMasked = isPrivacyMode,
                        maskPlaceholder = strings.notes,
                        fontSize = 11.sp,
                        color = CyberCyanLight
                    )
                    PrivacyMaskedText(
                        text = " • $formattedDate",
                        isMasked = isPrivacyMode,
                        maskPlaceholder = strings.maskPlaceholderProtected,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
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
                        text = { Text(if (item.isPinned) strings.unpin else strings.pin, color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = SecurityAmber) },
                        onClick = {
                            onTogglePin()
                            showMenu = false
                        }
                    )
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
