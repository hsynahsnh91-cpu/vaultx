package com.example.recovery

import android.util.Base64
import com.example.data.VaultConfigEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

data class RecoveryKitPackage(
    val format: String = "VaultX-Recovery-Kit",
    val version: Int = 1,
    val vaultId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val createdDateFormatted: String,
    val saltBase64: String,
    val canaryCiphertext: String,
    val encryptionStandard: String = "AES-256-GCM / PBKDF2-HMAC-SHA256 (65,536 iterations)",
    val integrityChecksum: String,
    val instructions: String
)

data class RecoveryVerificationResult(
    val isValid: Boolean,
    val message: String,
    val version: Int = 1,
    val vaultId: String = "",
    val createdDate: String = "",
    val matchesCurrentVault: Boolean = false
)

object VaultRecoveryManager {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(RecoveryKitPackage::class.java)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Computes a deterministic SHA-256 integrity checksum for a recovery kit payload.
     */
    fun computeChecksum(vaultId: String, saltBase64: String, canaryCiphertext: String): String {
        val payload = "VAULTX_KIT_V1:$vaultId:$saltBase64:$canaryCiphertext"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a new Recovery Kit from active vault configuration.
     * Contains verification parameters and instructions; NEVER stores raw master password or raw keys.
     */
    fun generateRecoveryKit(
        config: VaultConfigEntity,
        vaultId: String = UUID.randomUUID().toString()
    ): Pair<RecoveryKitPackage, String> {
        val now = System.currentTimeMillis()
        val checksum = computeChecksum(vaultId, config.saltBase64, config.canaryCiphertext)
        val instructions = "Store this recovery kit securely in an offline location, secure hardware token, or printed safe-deposit copy. " +
                "It contains vault cryptographic verification parameters and integrity metadata required to verify and reconstruct your vault configuration."

        val pkg = RecoveryKitPackage(
            format = "VaultX-Recovery-Kit",
            version = 1,
            vaultId = vaultId,
            createdAt = now,
            createdDateFormatted = dateFormat.format(Date(now)),
            saltBase64 = config.saltBase64,
            canaryCiphertext = config.canaryCiphertext,
            encryptionStandard = "AES-256-GCM / PBKDF2-HMAC-SHA256 (65,536 iterations)",
            integrityChecksum = checksum,
            instructions = instructions
        )

        val json = adapter.indent("  ").toJson(pkg)
        return pkg to json
    }

    /**
     * Parses and safely verifies a Recovery Kit without modifying vault data.
     */
    fun verifyRecoveryKit(jsonText: String, activeConfig: VaultConfigEntity?): RecoveryVerificationResult {
        return try {
            val pkg = adapter.fromJson(jsonText.trim())
                ?: return RecoveryVerificationResult(false, "Failed to parse Recovery Kit format. Invalid JSON.")

            if (pkg.format != "VaultX-Recovery-Kit") {
                return RecoveryVerificationResult(false, "Unrecognized format: ${pkg.format}. Expected VaultX-Recovery-Kit.")
            }

            if (pkg.version > 1) {
                return RecoveryVerificationResult(false, "Incompatible Recovery Kit version (${pkg.version}). Maximum supported version is 1.")
            }

            if (pkg.saltBase64.isBlank() || pkg.canaryCiphertext.isBlank()) {
                return RecoveryVerificationResult(false, "Corrupted Recovery Kit: Missing cryptographic salt or canary parameters.")
            }

            // Verify integrity checksum
            val expectedChecksum = computeChecksum(pkg.vaultId, pkg.saltBase64, pkg.canaryCiphertext)
            if (pkg.integrityChecksum != expectedChecksum) {
                return RecoveryVerificationResult(false, "Integrity Checksum mismatch! The Recovery Kit may be tampered with or corrupted.")
            }

            // Check if matches active vault
            val matchesActive = activeConfig != null &&
                    activeConfig.saltBase64 == pkg.saltBase64 &&
                    activeConfig.canaryCiphertext == pkg.canaryCiphertext

            RecoveryVerificationResult(
                isValid = true,
                message = if (matchesActive) {
                    "Recovery Kit verified successfully. Matches your current active vault."
                } else {
                    "Recovery Kit format and cryptographic checksum are valid."
                },
                version = pkg.version,
                vaultId = pkg.vaultId,
                createdDate = pkg.createdDateFormatted,
                matchesCurrentVault = matchesActive
            )
        } catch (e: Exception) {
            RecoveryVerificationResult(false, "Verification error: ${e.message ?: "Invalid file structure"}")
        }
    }
}
