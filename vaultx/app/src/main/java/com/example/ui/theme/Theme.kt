package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val VaultDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    primaryContainer = CyberCyanContainer,
    onPrimaryContainer = CyberCyanLight,
    secondary = CyberCyanLight,
    onSecondary = Color.Black,
    secondaryContainer = VaultSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = SecurityGreen,
    onTertiary = Color.Black,
    tertiaryContainer = SecurityGreenContainer,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = VaultSlateBg,
    onBackground = TextPrimary,
    surface = VaultSurface,
    onSurface = TextPrimary,
    surfaceVariant = VaultSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = VaultOutline,
    error = SecurityRed,
    onError = Color.White,
    errorContainer = SecurityRedContainer,
    onErrorContainer = Color(0xFFFECACA)
)

private val VaultLightColorScheme = lightColorScheme(
    primary = CyberCyanDim,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    tertiary = SecurityGreen,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = SecurityRed
)

@Composable
fun VaultXTheme(
    darkTheme: Boolean = true, // Default to deep security dark theme
    dynamicColor: Boolean = false, // Keep brand security styling
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VaultDarkColorScheme else VaultLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
