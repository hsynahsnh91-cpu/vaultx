package com.example.security

import android.os.SystemClock

object RateLimiter {

    @Volatile
    private var consecutiveFailures = 0

    @Volatile
    private var lockoutExpiresRealtime: Long = 0L

    /**
     * Calculates the required progressive delay in milliseconds based on consecutive failures.
     */
    fun getDelayMillisForFailures(failures: Int): Long {
        return when {
            failures <= 1 -> 0L
            failures == 2 -> 3_000L
            failures == 3 -> 8_000L
            failures == 4 -> 15_000L
            failures == 5 -> 30_000L
            else -> minOf(30_000L + (failures - 5) * 10_000L, 60_000L)
        }
    }

    /**
     * Records a failed attempt and triggers progressive lockout if applicable.
     * Returns the lockout duration in seconds (0 if no lockout).
     */
    @Synchronized
    fun recordFailedAttempt(): Long {
        consecutiveFailures++
        val delayMillis = getDelayMillisForFailures(consecutiveFailures)
        if (delayMillis > 0) {
            lockoutExpiresRealtime = SystemClock.elapsedRealtime() + delayMillis
        }
        return delayMillis / 1000
    }

    /**
     * Resets the failure counter upon successful authentication.
     */
    @Synchronized
    fun recordSuccessfulAttempt() {
        consecutiveFailures = 0
        lockoutExpiresRealtime = 0L
    }

    /**
     * Returns the remaining lockout seconds (0 if not locked out).
     */
    fun getRemainingLockoutSeconds(): Long {
        val now = SystemClock.elapsedRealtime()
        val remaining = lockoutExpiresRealtime - now
        return if (remaining > 0) (remaining + 999) / 1000 else 0L
    }

    fun isLockedOut(): Boolean = getRemainingLockoutSeconds() > 0

    fun getConsecutiveFailures(): Int = consecutiveFailures
}
