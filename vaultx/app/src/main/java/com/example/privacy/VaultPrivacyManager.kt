package com.example.privacy

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VaultPrivacyManager {

    private val _isPrivacyMode = MutableStateFlow(false)
    val isPrivacyMode: StateFlow<Boolean> = _isPrivacyMode.asStateFlow()

    private val _revealedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val revealedItemIds: StateFlow<Set<String>> = _revealedItemIds.asStateFlow()

    fun setPrivacyMode(enabled: Boolean) {
        _isPrivacyMode.value = enabled
        if (!enabled) {
            clearAllRevealed()
        }
    }

    fun maskTitle(title: String, isPrivacyMode: Boolean, itemId: String? = null): String {
        if (!isPrivacyMode) return title
        if (itemId != null && _revealedItemIds.value.contains(itemId)) return title
        return "🔒 ••••••••••••••••"
    }

    fun maskValue(value: String, isPrivacyMode: Boolean, isExplicitlyRevealed: Boolean = false): String {
        if (!isPrivacyMode) return value
        if (isExplicitlyRevealed) return value
        return "••••••••••••"
    }

    /**
     * Sanitizes activity notifications and messages to completely generic alerts when Privacy Mode is active.
     */
    fun sanitizeNotification(message: String, isPrivacyMode: Boolean): String {
        if (!isPrivacyMode) return message

        val lower = message.lowercase()
        return when {
            lower.contains("copied") || lower.contains("clipboard") -> "Item copied to clipboard."
            lower.contains("export") -> "Vault export operation completed."
            lower.contains("import") -> "Vault import operation completed."
            lower.contains("backup") -> "Vault backup package generated."
            lower.contains("restore") -> "Vault restore operation completed."
            lower.contains("trash") || lower.contains("delete") -> "Vault storage updated."
            lower.contains("encrypt") || lower.contains("save") -> "Vault item secured."
            lower.contains("password") -> "Security credential updated."
            lower.contains("duplicate") -> "Storage scan completed."
            lower.contains("recovery") -> "Recovery verification completed."
            lower.contains("wipe") -> "Vault operation completed."
            else -> "Security event recorded in VaultX."
        }
    }

    /**
     * Centralized helper to show user-facing activity notifications, automatically
     * sanitized to generic safe messages when Privacy Mode is active.
     */
    fun showSanitizedToast(
        context: Context,
        rawMessage: String,
        isPrivacyMode: Boolean = _isPrivacyMode.value,
        customGenericMessage: String? = null,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        val messageToShow = if (isPrivacyMode) {
            customGenericMessage ?: sanitizeNotification(rawMessage, true)
        } else {
            rawMessage
        }
        Toast.makeText(context, messageToShow, duration).show()
    }

    fun temporarilyReveal(itemId: String) {
        _revealedItemIds.value = _revealedItemIds.value + itemId
    }

    fun remaskItem(itemId: String) {
        _revealedItemIds.value = _revealedItemIds.value - itemId
    }

    fun clearAllRevealed() {
        _revealedItemIds.value = emptySet()
    }
}
