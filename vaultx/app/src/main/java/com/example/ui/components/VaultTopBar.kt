package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.i18n.VaultStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.VaultNavTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    strings: VaultStrings,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onLockClick: () -> Unit,
    onSelectTab: (VaultNavTab) -> Unit,
    onToggleLanguage: () -> Unit,
    currentLanguage: String,
    isPrivacyMode: Boolean = false,
    onTogglePrivacyMode: () -> Unit = {}
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultSurface)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isPrivacyMode) SecurityGreenContainer else CyberCyanContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Shield,
                            contentDescription = strings.shield,
                            tint = if (isPrivacyMode) SecurityGreen else CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = strings.appName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isPrivacyMode) SecurityGreenContainer else SecurityGreenContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = if (isPrivacyMode) strings.privacyOnBadge else strings.aesGcmBadge,
                                    color = SecurityGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            },
            actions = {
                // Privacy Mode Quick Toggle
                IconButton(onClick = onTogglePrivacyMode) {
                    Icon(
                        imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = strings.privacyMode,
                        tint = if (isPrivacyMode) SecurityGreen else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Search toggle
                IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                    Icon(
                        if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = strings.search,
                        tint = if (isSearchExpanded) CyberCyan else TextSecondary
                    )
                }

                // Language switch
                TextButton(onClick = onToggleLanguage) {
                    Text(
                        text = if (currentLanguage == "ar") "EN" else "عربي",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // More menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = strings.menu, tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(VaultSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.securityCenter, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = SecurityGreen) },
                            onClick = {
                                onSelectTab(VaultNavTab.SECURITY)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.privacyCenter, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = CyberCyan) },
                            onClick = {
                                onSelectTab(VaultNavTab.PRIVACY)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.backupRestore, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                onSelectTab(VaultNavTab.BACKUP)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.trash, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                onSelectTab(VaultNavTab.TRASH)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.settings, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                onSelectTab(VaultNavTab.SETTINGS)
                                showMenu = false
                            }
                        )
                    }
                }

                // Lock Now button
                IconButton(
                    onClick = onLockClick,
                    modifier = Modifier
                        .padding(start = 2.dp, end = 6.dp)
                        .clip(CircleShape)
                        .background(SecurityRedContainer)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = strings.lockNow,
                        tint = SecurityRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = VaultSurface
            )
        )

        AnimatedVisibility(visible = isSearchExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.searchVault, color = TextMuted, fontSize = 14.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = strings.clear, tint = TextSecondary)
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
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}
