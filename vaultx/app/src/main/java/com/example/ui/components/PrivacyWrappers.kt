package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.LocalVaultStrings
import com.example.ui.theme.*

/**
 * CompositionLocal providing global Privacy Mode state across the entire Composable tree.
 */
val LocalPrivacyMode = compositionLocalOf { false }

/**
 * Composable wrapper that conditionally blurs and masks any arbitrary content (thumbnails, previews, cards).
 * Uses both visual Gaussian blur and an opaque/frosted privacy shield layer for 100% data protection.
 */
@Composable
fun PrivacyBlurMask(
    modifier: Modifier = Modifier,
    isMasked: Boolean = LocalPrivacyMode.current,
    isTemporarilyRevealed: Boolean = false,
    blurRadius: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    maskBadgeIcon: ImageVector? = Icons.Default.Security,
    maskBadgeText: String? = null,
    onTapReveal: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val activeMask = isMasked && !isTemporarilyRevealed

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (activeMask && onTapReveal != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTapReveal
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Base Content Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (activeMask) {
                        Modifier.blur(blurRadius)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
            content = content
        )

        // Frosted Privacy Mask Overlay
        if (activeMask) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(VaultSurface.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(4.dp)
                ) {
                    if (maskBadgeIcon != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SecurityGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = maskBadgeIcon,
                                contentDescription = null,
                                tint = SecurityGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (maskBadgeText != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = maskBadgeText,
                            color = SecurityGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable wrapper specifically designed to blur, mask, and protect sensitive filenames & titles.
 */
@Composable
fun PrivacyMaskedFilename(
    filename: String,
    modifier: Modifier = Modifier,
    isMasked: Boolean = LocalPrivacyMode.current,
    isTemporarilyRevealed: Boolean = false,
    maskText: String = "🔒 ••••••••••••••••",
    color: Color = TextPrimary,
    fontWeight: FontWeight? = FontWeight.Bold,
    fontSize: TextUnit = 14.sp,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val activeMask = isMasked && !isTemporarilyRevealed

    AnimatedContent(
        targetState = activeMask,
        label = "PrivacyFilenameAnimation"
    ) { masked ->
        if (masked) {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = maskText,
                    color = TextSecondary,
                    fontWeight = fontWeight,
                    fontSize = fontSize,
                    maxLines = maxLines,
                    overflow = overflow,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Text(
                text = filename,
                modifier = modifier,
                color = color,
                fontWeight = fontWeight,
                fontSize = fontSize,
                maxLines = maxLines,
                overflow = overflow
            )
        }
    }
}

/**
 * Composable wrapper specifically designed to protect media icons, thumbnails, and preview chips.
 */
@Composable
fun PrivacyMaskedThumbnail(
    modifier: Modifier = Modifier,
    isMasked: Boolean = LocalPrivacyMode.current,
    isTemporarilyRevealed: Boolean = false,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    blurRadius: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val activeMask = isMasked && !isTemporarilyRevealed

    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(if (activeMask) Modifier.blur(blurRadius) else Modifier),
            contentAlignment = Alignment.Center,
            content = content
        )

        if (activeMask) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(VaultSurface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = SecurityGreen,
                    modifier = Modifier.size(size * 0.45f)
                )
            }
        }
    }
}

/**
 * Composable wrapper for text content values (notes, records, password fields, details).
 */
@Composable
fun PrivacyMaskedText(
    text: String,
    modifier: Modifier = Modifier,
    isMasked: Boolean = LocalPrivacyMode.current,
    isTemporarilyRevealed: Boolean = false,
    maskPlaceholder: String = "••••••••••••",
    color: Color = TextSecondary,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = 12.sp,
    fontFamily: FontFamily? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val activeMask = isMasked && !isTemporarilyRevealed

    if (activeMask) {
        Text(
            text = maskPlaceholder,
            modifier = modifier,
            color = TextMuted,
            fontWeight = fontWeight,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            maxLines = maxLines,
            overflow = overflow
        )
    } else {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontWeight = fontWeight,
            fontSize = fontSize,
            fontFamily = fontFamily,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}

/**
 * Composable wrapper for large content blocks (e.g. note body, decrypted preview panels, custom cards).
 */
@Composable
fun PrivacyMaskedContent(
    modifier: Modifier = Modifier,
    isMasked: Boolean = LocalPrivacyMode.current,
    isTemporarilyRevealed: Boolean = false,
    blurRadius: Dp = 10.dp,
    placeholderMessage: String? = null,
    onTapReveal: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val activeMask = isMasked && !isTemporarilyRevealed
    val displayMessage = placeholderMessage ?: LocalVaultStrings.current.previewMaskedNotice

    Box(
        modifier = modifier
            .then(
                if (activeMask && onTapReveal != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTapReveal
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (activeMask) Modifier.blur(blurRadius) else Modifier),
            content = content
        )

        if (activeMask) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(VaultSurface.copy(alpha = 0.90f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = VaultSurfaceCard,
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SecurityGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = displayMessage,
                            color = SecurityGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
