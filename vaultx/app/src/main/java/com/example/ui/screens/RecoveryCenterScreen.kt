package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.VaultAuditLogEntity
import com.example.i18n.VaultStrings
import com.example.recovery.RecoveryKitPackage
import com.example.recovery.RecoveryVerificationResult
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecoveryCenterScreen(
    auditLogs: List<VaultAuditLogEntity>,
    strings: VaultStrings,
    onCreateRecoveryKit: suspend () -> Pair<RecoveryKitPackage, String>?,
    onVerifyRecoveryKit: (String) -> RecoveryVerificationResult
) {
    val context = LocalContext.current
    val langCode = com.example.i18n.LocalLanguageCode.current
    val coroutineScope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var createdKitPackage by remember { mutableStateOf<RecoveryKitPackage?>(null) }
    var createdKitJson by remember { mutableStateOf<String?>(null) }
    var verificationResult by remember { mutableStateOf<RecoveryVerificationResult?>(null) }
    var verifyInputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val recoveryLogs = remember(auditLogs) {
        auditLogs.filter { it.category == "RECOVERY" || it.action.contains("RECOVERY") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultSlateBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VaultSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberCyan.copy(alpha = 0.3f)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(CyberCyanContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.recoveryKit,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isGenerating = true
                                coroutineScope.launch {
                                    val res = onCreateRecoveryKit()
                                    if (res != null) {
                                        createdKitPackage = res.first
                                        createdKitJson = res.second
                                        showCreateDialog = true
                                    }
                                    isGenerating = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            enabled = !isGenerating
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AddModerator, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.createRecoveryKit, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                verificationResult = null
                                verifyInputText = ""
                                showVerifyDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.verifyRecoveryKit, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section Title: Recovery History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.recoveryHistoryTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        if (recoveryLogs.isEmpty()) {
            item {
                Surface(
                    color = VaultSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.noRecoveryHistory,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recoveryLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = log.action,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (log.severity == "WARNING") SecurityAmber else CyberCyan
                            )
                            val dateStr = com.example.i18n.VaultI18n.formatDateTime(log.timestamp, langCode)
                            Text(text = dateStr, fontSize = 11.sp, color = TextMuted)
                        }
                        if (log.details.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = log.details, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }

    // Modal: Create Recovery Kit Display
    if (showCreateDialog && createdKitPackage != null && createdKitJson != null) {
        val pkg = createdKitPackage!!
        val json = createdKitJson!!
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VaultSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = SecurityGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.recoveryKit,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = strings.checksumLabel(pkg.integrityChecksum),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SecurityGreen
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = strings.vaultIdLabel(pkg.vaultId),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = json,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VaultSlateBg,
                            unfocusedContainerColor = VaultSlateBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(strings.recoveryKit, json)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, strings.recoveryKitCopiedToast, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.copyJsonButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showCreateDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(strings.cancel, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal: Verify Recovery Kit
    if (showVerifyDialog) {
        Dialog(onDismissRequest = { showVerifyDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VaultSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = strings.verifyRecoveryKit,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = strings.pasteRecoveryKitJson,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = verifyInputText,
                        onValueChange = {
                            verifyInputText = it
                            verificationResult = null
                        },
                        placeholder = { Text("{ \"format\": \"VaultX-Recovery-Kit\", ... }", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VaultSlateBg,
                            unfocusedContainerColor = VaultSlateBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (verificationResult != null) {
                        val res = verificationResult!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = if (res.isValid) SecurityGreenContainer else SecurityRedContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (res.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (res.isValid) SecurityGreen else SecurityRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = res.message,
                                    color = if (res.isValid) Color(0xFFA7F3D0) else Color(0xFFFECACA),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (verifyInputText.isNotBlank()) {
                                    verificationResult = onVerifyRecoveryKit(verifyInputText)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            enabled = verifyInputText.isNotBlank()
                        ) {
                            Text(strings.verifyRecoveryKit, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showVerifyDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(strings.cancel, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
