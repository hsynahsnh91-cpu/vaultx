package com.example.security

import android.os.SystemClock
import com.example.data.VaultAuditLogEntity
import com.example.data.VaultDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VaultAccessMonitor {

    private var consecutiveFailures = 0
    private var lockoutUntilMonotonicMs = 0L

    /**
     * Checks if the user is currently rate-limited.
     * Returns remaining lockout seconds (0 if not rate limited).
     */
    fun getRemainingLockoutSeconds(): Int {
        return RateLimiter.getRemainingLockoutSeconds().toInt()
    }

    fun getConsecutiveFailures(): Int = RateLimiter.getConsecutiveFailures()

    suspend fun recordAttemptResult(
        vaultDao: VaultDao,
        isSuccess: Boolean,
        failureReason: String? = null
    ): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()

        if (isSuccess) {
            RateLimiter.recordSuccessfulAttempt()

            vaultDao.insertLog(
                VaultAuditLogEntity(
                    action = "SUCCESSFUL_VAULT_UNLOCK",
                    timestamp = timestamp,
                    details = "Master authentication succeeded. Ephemeral key mounted in memory.",
                    severity = "SUCCESS",
                    category = "UNLOCK"
                )
            )
            0
        } else {
            consecutiveFailures++

            // Progressive delay calculation
            val delaySeconds = when (consecutiveFailures) {
                1 -> 0
                2 -> 3
                3 -> 10
                4 -> 30
                else -> (60 * (consecutiveFailures - 4)).coerceIn(60, 300)
            }

            val nowMonotonic = SystemClock.elapsedRealtime()
            if (delaySeconds > 0) {
                lockoutUntilMonotonicMs = nowMonotonic + (delaySeconds * 1000L)
            }
            RateLimiter.recordFailedAttempt()

            val severity = if (consecutiveFailures >= 3) "DANGER" else "WARNING"
            vaultDao.insertLog(
                VaultAuditLogEntity(
                    action = "FAILED_VAULT_UNLOCK",
                    timestamp = timestamp,
                    details = "${failureReason ?: "Invalid credentials"}. Consecutive failures: $consecutiveFailures. Enforced delay: ${delaySeconds}s.",
                    severity = severity,
                    category = "FAILED"
                )
            )

            if (consecutiveFailures >= 3) {
                vaultDao.insertLog(
                    VaultAuditLogEntity(
                        action = "SUSPICIOUS_REPEATED_FAILURES",
                        timestamp = timestamp,
                        details = "Threshold warning: $consecutiveFailures consecutive failed unlock attempts detected within active session window.",
                        severity = "DANGER",
                        category = "SECURITY"
                    )
                )
            }

            delaySeconds
        }
    }

    suspend fun logEvent(
        vaultDao: VaultDao,
        action: String,
        details: String,
        severity: String = "INFO",
        category: String = "SECURITY"
    ) = withContext(Dispatchers.IO) {
        vaultDao.insertLog(
            VaultAuditLogEntity(
                action = action,
                timestamp = System.currentTimeMillis(),
                details = details,
                severity = severity,
                category = category
            )
        )
    }
}
