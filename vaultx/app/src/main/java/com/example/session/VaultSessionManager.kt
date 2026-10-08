package com.example.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.crypto.SecretKey

enum class VaultSessionState {
    LOCKED,
    UNLOCKED,
    SETTING_UP
}

object VaultSessionManager {
    private var activeKey: SecretKey? = null
    private var lastActiveTime: Long = System.currentTimeMillis()
    private var autoLockMinutes: Int = 5
    private var lockOnBackground: Boolean = true

    private val lockListeners = mutableListOf<() -> Unit>()

    private val _sessionState = MutableStateFlow(VaultSessionState.LOCKED)
    val sessionState: StateFlow<VaultSessionState> = _sessionState.asStateFlow()

    fun isUnlocked(): Boolean = _sessionState.value == VaultSessionState.UNLOCKED && activeKey != null

    fun getActiveKey(): SecretKey? = activeKey

    fun setAutoLockMinutes(minutes: Int) {
        autoLockMinutes = minutes
    }

    fun getAutoLockMinutes(): Int = autoLockMinutes

    fun setLockOnBackground(enabled: Boolean) {
        lockOnBackground = enabled
    }

    fun isLockOnBackground(): Boolean = lockOnBackground

    fun addLockListener(listener: () -> Unit) {
        synchronized(lockListeners) {
            lockListeners.add(listener)
        }
    }

    fun removeLockListener(listener: () -> Unit) {
        synchronized(lockListeners) {
            lockListeners.remove(listener)
        }
    }

    fun unlock(secretKey: SecretKey) {
        activeKey = secretKey
        lastActiveTime = System.currentTimeMillis()
        _sessionState.value = VaultSessionState.UNLOCKED
    }

    fun lock() {
        activeKey = null
        _sessionState.value = VaultSessionState.LOCKED
        // Notify all active previewers, media players, and sensitive UI components to clear state immediately
        synchronized(lockListeners) {
            lockListeners.forEach { runCatching { it.invoke() } }
        }
    }

    fun onAppBackgrounded(): Boolean {
        if (lockOnBackground && isUnlocked()) {
            lock()
            return true
        }
        return false
    }

    fun recordInteraction() {
        lastActiveTime = System.currentTimeMillis()
    }

    fun checkInactivity(): Boolean {
        if (!isUnlocked()) return false
        if (autoLockMinutes <= 0) return false // "Never" auto-lock option

        val elapsedMillis = System.currentTimeMillis() - lastActiveTime
        val timeoutMillis = autoLockMinutes * 60 * 1000L
        if (elapsedMillis >= timeoutMillis) {
            lock()
            return true
        }
        return false
    }
}
