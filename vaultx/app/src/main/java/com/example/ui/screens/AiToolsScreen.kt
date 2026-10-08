package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiDocumentAnalysisResult
import com.example.ai.AiFolderSuggestion
import com.example.ai.AiSearchResult
import com.example.i18n.VaultStrings
import com.example.model.VaultItem
import com.example.model.VaultType
import com.example.ui.theme.*

enum class AiSubTab {
    ORGANIZER,
    DOCUMENT_AI,
    NL_SEARCH,
    PRIVACY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsScreen(
    items: List<VaultItem>,
    strings: VaultStrings,
    aiSuggestions: List<AiFolderSuggestion>,
    isAiProcessing: Boolean,
    aiDocumentResult: AiDocumentAnalysisResult?,
    onGenerateSuggestions: (Boolean) -> Unit,
    onToggleSuggestionApproval: (String) -> Unit,
    onApplyApprovedSuggestions: (() -> Unit) -> Unit,
    onAnalyzeDocument: (String, String, String?, Boolean) -> Unit,
    onClearDocumentResult: () -> Unit,
    onPerformNlSearch: (String) -> List<AiSearchResult>
) {
    var selectedSubTab by remember { mutableStateOf(AiSubTab.ORGANIZER) }
    var useCloudAi by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }

    // State for Document Understanding
    var selectedItemForDocAi by remember { mutableStateOf<VaultItem?>(null) }
    var customQuestionText by remember { mutableStateOf("") }

    // State for NL Search
    var nlSearchQuery by remember { mutableStateOf("") }
    var nlSearchResults by remember { mutableStateOf<List<AiSearchResult>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
            .padding(16.dp)
    ) {
        // SubTabs
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab.ordinal,
            containerColor = VaultSurface,
            contentColor = CyberCyan,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedSubTab == AiSubTab.ORGANIZER,
                onClick = { selectedSubTab = AiSubTab.ORGANIZER },
                text = { Text(strings.aiFileOrganizer, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == AiSubTab.DOCUMENT_AI,
                onClick = { selectedSubTab = AiSubTab.DOCUMENT_AI },
                text = { Text(strings.aiDocumentUnderstanding, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == AiSubTab.NL_SEARCH,
                onClick = { selectedSubTab = AiSubTab.NL_SEARCH },
                text = { Text(strings.aiNaturalLanguageSearch, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == AiSubTab.PRIVACY,
                onClick = { selectedSubTab = AiSubTab.PRIVACY },
                text = { Text(strings.privacyMode, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedSubTab) {
            AiSubTab.ORGANIZER -> {
                // File Organizer View
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(strings.aiFileOrganizer, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(
                                    text = if (useCloudAi) strings.geminiCloudApproved else strings.localIntelligenceMode,
                                    fontSize = 11.sp,
                                    color = if (useCloudAi) CyberCyanLight else SecurityGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (useCloudAi) {
                                        showConsentDialog = true
                                    } else {
                                        onGenerateSuggestions(false)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                enabled = !isAiProcessing && items.isNotEmpty(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isAiProcessing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                } else {
                                    Text(strings.analyzeAndOrganize, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (aiSuggestions.any { it.isApproved }) {
                                Button(
                                    onClick = { onApplyApprovedSuggestions {} },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(strings.applyApprovedSuggestions, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (aiSuggestions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.aiFileOrganizerHelp,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(aiSuggestions) { suggestion ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = VaultSurfaceVariant),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = suggestion.isApproved,
                                        onCheckedChange = { onToggleSuggestionApproval(suggestion.itemId) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion.itemTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${suggestion.currentFolder} → ",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                            Surface(
                                                color = CyberCyanContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = suggestion.suggestedFolder,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyberCyanLight,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = suggestion.reason,
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AiSubTab.DOCUMENT_AI -> {
                // Document Understanding View
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(strings.aiDocumentUnderstanding, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.aiDocAiHelp,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Item Picker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(VaultSlateBg, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedItemForDocAi?.title ?: strings.selectDocumentPlaceholder,
                                fontSize = 13.sp,
                                color = if (selectedItemForDocAi != null) TextPrimary else TextMuted
                            )
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        }

                        // Horizontal list of selectable items
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .padding(top = 8.dp)
                        ) {
                            items(items.take(8)) { item ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedItemForDocAi = item }
                                        .padding(vertical = 2.dp),
                                    color = if (selectedItemForDocAi?.id == item.id) CyberCyanContainer else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "• ${item.title} (${item.folder})",
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    val item = selectedItemForDocAi ?: return@Button
                                    onAnalyzeDocument(item.title, "SUMMARIZE", null, useCloudAi)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = selectedItemForDocAi != null && !isAiProcessing,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(strings.documentSummary, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val item = selectedItemForDocAi ?: return@Button
                                    onAnalyzeDocument(item.title, "DATES", null, useCloudAi)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = selectedItemForDocAi != null && !isAiProcessing,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(strings.importantDates, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val item = selectedItemForDocAi ?: return@Button
                                    onAnalyzeDocument(item.title, "TASKS", null, useCloudAi)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = selectedItemForDocAi != null && !isAiProcessing,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(strings.tasksAndActions, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Result Card
                if (aiDocumentResult != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = VaultSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.analysisResult(aiDocumentResult.operation),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                                IconButton(onClick = onClearDocumentResult, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = strings.close, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiDocumentResult.content,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            AiSubTab.NL_SEARCH -> {
                // Natural Language Semantic Search View
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(strings.aiNaturalLanguageSearch, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.aiSearchHelp,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = nlSearchQuery,
                            onValueChange = {
                                nlSearchQuery = it
                                if (it.isNotBlank()) {
                                    nlSearchResults = onPerformNlSearch(it)
                                } else {
                                    nlSearchResults = emptyList()
                                }
                            },
                            placeholder = { Text(strings.searchQueryPlaceholder, fontSize = 12.sp, color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (nlSearchResults.isEmpty() && nlSearchQuery.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(strings.noItemsFound, fontSize = 13.sp, color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(nlSearchResults) { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = VaultSurfaceVariant),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = result.item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                        Surface(color = SecurityGreenContainer, shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = strings.matchScore(result.score),
                                                color = SecurityGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${strings.matchReason}: ${result.reason}",
                                        fontSize = 11.sp,
                                        color = CyberCyanLight
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AiSubTab.PRIVACY -> {
                // AI Privacy & Consent Settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = SecurityGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.aiPrivacyPolicyTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.allowGeminiCloudSwitch, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = useCloudAi,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        showConsentDialog = true
                                    } else {
                                        useCloudAi = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Explicit Cloud AI Consent Dialog
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = {
                Text(strings.aiConsentRequiredTitle, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(strings.aiConsentRequiredMessage, fontSize = 13.sp, lineHeight = 18.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        useCloudAi = true
                        showConsentDialog = false
                        onGenerateSuggestions(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                ) {
                    Text(strings.allowCloudAi, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text(strings.cancelCloudAi)
                }
            }
        )
    }
}
