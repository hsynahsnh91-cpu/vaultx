package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoManager
import com.example.crypto.PasswordStrength
import com.example.crypto.StrengthLevel
import com.example.i18n.LocalLanguageCode
import com.example.i18n.LocalVaultStrings
import com.example.ui.theme.*

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String? = null,
    cancelText: String? = null,
    isDestructive: Boolean = false,
    confirmColor: Color? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalVaultStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Text(text = message, color = TextSecondary, fontSize = 14.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor ?: if (isDestructive) SecurityRed else CyberCyan,
                    contentColor = if (isDestructive) Color.White else Color.Black
                )
            ) {
                Text(confirmText ?: strings.confirm, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText ?: strings.cancel, color = TextSecondary)
            }
        },
        containerColor = VaultSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PasswordStrengthIndicator(password: String) {
    val strings = LocalVaultStrings.current
    val langCode = LocalLanguageCode.current
    val result = remember(password) { PasswordStrength.evaluate(password) }

    val barColor = when (result.score) {
        0 -> SecurityRed
        1 -> Color(0xFFF97316) // Orange
        2 -> SecurityAmber
        3 -> CyberCyan
        else -> SecurityGreen
    }

    val localizedLevelLabel = when (result.level) {
        StrengthLevel.VERY_WEAK -> strings.strengthVeryWeak
        StrengthLevel.WEAK -> strings.strengthWeak
        StrengthLevel.FAIR -> strings.strengthFair
        StrengthLevel.STRONG -> strings.strengthStrong
        StrengthLevel.VERY_STRONG -> strings.strengthVeryStrong
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.strengthWithEntropy(localizedLevelLabel, result.entropyBits),
                color = barColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0..4) {
                val filled = i <= result.score
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (filled) barColor else VaultOutline)
                )
            }
        }
        val tip = result.structuredSuggestions.firstOrNull()?.localized(strings)
            ?: result.suggestions.firstOrNull()
        if (tip != null && password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.strengthTip(tip),
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun PasswordGeneratorDialog(
    onPasswordGenerated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalVaultStrings.current
    var length by remember { mutableStateOf(16f) }
    var includeUpper by remember { mutableStateOf(true) }
    var includeLower by remember { mutableStateOf(true) }
    var includeDigits by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }

    var generatedPassword by remember {
        mutableStateOf(
            CryptoManager.generateSecurePassword(16, true, true, true, true)
        )
    }

    fun regenerate() {
        generatedPassword = CryptoManager.generateSecurePassword(
            length.toInt(),
            includeUpper,
            includeLower,
            includeDigits,
            includeSymbols
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(strings.passwordGenerator, fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Display Generated Password
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VaultSlateBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = generatedPassword,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CyberCyanLight,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { regenerate() }) {
                            Icon(Icons.Default.Refresh, contentDescription = strings.regenerate, tint = CyberCyan)
                        }
                    }
                }

                PasswordStrengthIndicator(password = generatedPassword)

                Spacer(modifier = Modifier.height(12.dp))

                // Length slider
                Text(
                    text = strings.lengthWithVal(length.toInt()),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = length,
                    onValueChange = {
                        length = it
                        regenerate()
                    },
                    valueRange = 8f..48f,
                    steps = 39,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan
                    )
                )

                // Character sets checkboxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.uppercaseChars, fontSize = 13.sp, color = TextPrimary)
                    Checkbox(
                        checked = includeUpper,
                        onCheckedChange = {
                            includeUpper = it
                            regenerate()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.lowercaseChars, fontSize = 13.sp, color = TextPrimary)
                    Checkbox(
                        checked = includeLower,
                        onCheckedChange = {
                            includeLower = it
                            regenerate()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.numberChars, fontSize = 13.sp, color = TextPrimary)
                    Checkbox(
                        checked = includeDigits,
                        onCheckedChange = {
                            includeDigits = it
                            regenerate()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.symbolChars, fontSize = 13.sp, color = TextPrimary)
                    Checkbox(
                        checked = includeSymbols,
                        onCheckedChange = {
                            includeSymbols = it
                            regenerate()
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPasswordGenerated(generatedPassword)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
            ) {
                Text(strings.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = TextSecondary)
            }
        },
        containerColor = VaultSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
