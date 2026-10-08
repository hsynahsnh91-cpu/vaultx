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
import com.example.crypto.PasswordStrength
import com.example.i18n.VaultStrings
import com.example.model.PasswordPayload
import com.example.model.VaultItem
import com.example.model.VaultType
import com.example.privacy.VaultPrivacyManager
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun PasswordsScreen(
    items: List<VaultItem>,
    strings: VaultStrings,
    onDecryptPassword: (VaultItem) -> PasswordPayload,
    onSavePassword: (id: String?, title: String, payload: PasswordPayload, folder: String, tags: List<String>, isFavorite: Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoveToTrash: (String) -> Unit
) {
    val context = LocalContext.current
    val passwordItems = items.filter { it.type == VaultType.PASSWORD }

    var isEditing by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<VaultItem?>(null) }

    var editTitle by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }
    var editUrl by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }
    var editFolder by remember { mutableStateOf(strings.defaultFolderLogins) }
    var editFavorite by remember { mutableStateOf(false) }

    var showGenerator by remember { mutableStateOf(false) }
    var passwordVisibleInEditor by remember { mutableStateOf(false) }

    var itemToDelete by remember { mutableStateOf<VaultItem?>(null) }

    fun openNewPassword() {
        itemToEdit = null
        editTitle = ""
        editUsername = ""
        editPassword = ""
        editUrl = ""
        editNotes = ""
        editFolder = strings.defaultFolderLogins
        editFavorite = false
        passwordVisibleInEditor = false
        isEditing = true
    }

    fun openEditPassword(item: VaultItem) {
        val payload = onDecryptPassword(item)
        itemToEdit = item
        editTitle = item.title
        editUsername = payload.username
        editPassword = payload.password
        editUrl = payload.url
        editNotes = payload.notes
        editFolder = item.folder
        editFavorite = item.isFavorite
        passwordVisibleInEditor = false
        isEditing = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
    ) {
        if (passwordItems.isEmpty()) {
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
                        .background(CyberCyanContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.noPasswordsTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { openNewPassword() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.newPassword, fontWeight = FontWeight.Bold)
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
                            text = strings.passwordsCount(passwordItems.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Button(
                            onClick = { openNewPassword() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.newPassword, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(passwordItems, key = { it.id }) { item ->
                    val payload = remember(item) { onDecryptPassword(item) }
                    PasswordCard(
                        item = item,
                        payload = payload,
                        strings = strings,
                        onEdit = { openEditPassword(item) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }

        // Add / Edit Password Dialog
        if (isEditing) {
            AlertDialog(
                onDismissRequest = { isEditing = false },
                title = {
                    Text(
                        text = if (itemToEdit == null) strings.newPassword else strings.editPasswordTitle,
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
                            label = { Text(strings.serviceTitlePlaceholder) },
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

                        OutlinedTextField(
                            value = editUsername,
                            onValueChange = { editUsername = it },
                            label = { Text(strings.username) },
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

                        // Password Field with Generator button
                        OutlinedTextField(
                            value = editPassword,
                            onValueChange = { editPassword = it },
                            label = { Text(strings.passwordField) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisibleInEditor) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { passwordVisibleInEditor = !passwordVisibleInEditor }) {
                                        Icon(
                                            if (passwordVisibleInEditor) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisibleInEditor) strings.hidePassword else strings.showPassword,
                                            tint = TextSecondary
                                        )
                                    }
                                    IconButton(onClick = { showGenerator = true }) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = strings.generatePassword, tint = CyberCyan)
                                    }
                                }
                            },
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

                        if (editPassword.isNotEmpty()) {
                            PasswordStrengthIndicator(password = editPassword)
                        }

                        OutlinedTextField(
                            value = editUrl,
                            onValueChange = { editUrl = it },
                            label = { Text(strings.websiteUrl) },
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

                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text(strings.privateNotesOptional) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
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
                            if (editTitle.isBlank()) editTitle = strings.untitledAccount
                            onSavePassword(
                                itemToEdit?.id,
                                editTitle,
                                PasswordPayload(
                                    username = editUsername,
                                    password = editPassword,
                                    url = editUrl,
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

        // Generator Dialog
        if (showGenerator) {
            PasswordGeneratorDialog(
                onPasswordGenerated = { genPass ->
                    editPassword = genPass
                    passwordVisibleInEditor = true
                },
                onDismiss = { showGenerator = false }
            )
        }

        // Delete Confirm Dialog
        if (itemToDelete != null) {
            ConfirmDialog(
                title = strings.moveToTrash,
                message = strings.moveToTrashPasswordConfirm(itemToDelete?.title ?: ""),
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
fun PasswordCard(
    item: VaultItem,
    payload: PasswordPayload,
    strings: VaultStrings,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isPrivacyMode = LocalPrivacyMode.current
    var isPasswordRevealed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val strength = remember(payload.password) {
        PasswordStrength.evaluate(payload.password)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = VaultSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrivacyMaskedThumbnail(
                    size = 40.dp,
                    isMasked = isPrivacyMode
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    PrivacyMaskedFilename(
                        filename = item.title,
                        isMasked = isPrivacyMode,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    if (payload.username.isNotBlank()) {
                        PrivacyMaskedText(
                            text = payload.username,
                            isMasked = isPrivacyMode,
                            maskPlaceholder = "••••••••",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Password strength badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (strength.score) {
                        0, 1 -> SecurityRedContainer
                        2 -> Color(0xFF78350F)
                        else -> SecurityGreenContainer
                    }
                ) {
                    Text(
                        text = strength.level.localized(strings),
                        color = when (strength.score) {
                            0, 1 -> SecurityRed
                            2 -> SecurityAmber
                            else -> SecurityGreen
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
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
                            text = { Text(strings.editCredentialAction, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = CyberCyan) },
                            onClick = {
                                onEdit()
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

            Spacer(modifier = Modifier.height(10.dp))

            // Password row
            Surface(
                color = VaultSlateBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isSecretVisible = isPasswordRevealed && !isPrivacyMode
                    PrivacyMaskedText(
                        text = if (isSecretVisible) payload.password else "••••••••••••",
                        isMasked = isPrivacyMode,
                        maskPlaceholder = "••••••••••••",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = if (isSecretVisible) CyberCyanLight else TextSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    Row {
                        IconButton(
                            onClick = { isPasswordRevealed = !isPasswordRevealed },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isPasswordRevealed && !isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordRevealed && !isPrivacyMode) strings.hidePassword else strings.showPassword,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText(strings.passwordField, payload.password))
                                VaultPrivacyManager.showSanitizedToast(
                                    context = context,
                                    rawMessage = strings.passwordCopied,
                                    isPrivacyMode = isPrivacyMode,
                                    customGenericMessage = strings.passwordCopied
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
    }
}
