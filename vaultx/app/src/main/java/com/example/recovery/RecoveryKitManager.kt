package com.example.recovery

import com.example.crypto.CryptoManager
import com.example.data.VaultConfigEntity
import com.example.model.RecoveryKit
import com.example.model.RecoveryKitVerificationResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID

object RecoveryKitManager {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(RecoveryKit::class.java)

    fun generateRecoveryKit(config: VaultConfigEntity): RecoveryKit {
        val vaultId = UUID.nameUUIDFromBytes(config.saltBase64.toByteArray(Charsets.UTF_8)).toString()
        val canaryDigest = CryptoManager.sha256Hex(config.canaryCiphertext.toByteArray(Charsets.UTF_8))
        val saltBytes = android.util.Base64.decode(config.saltBase64, android.util.Base64.NO_WRAP)

        val integrityPayload = "$vaultId|${config.saltBase64}|$canaryDigest|65536".toByteArray(Charsets.UTF_8)
        val hmac = CryptoManager.hmacSha256Hex(integrityPayload, saltBytes)

        val instructions = """
            VaultX Encrypted Recovery Kit (Version 1)
            ------------------------------------------
            This kit contains critical cryptographic parameters (salt, PBKDF2 iterations, integrity verification digest) 
            needed to reconstruct and verify your encrypted vault structure in disaster recovery scenarios.
            
            IMPORTANT SECURITY NOTICES:
            1. This Recovery Kit DOES NOT contain your master password.
            2. This Recovery Kit DOES NOT contain raw encryption keys.
            3. Store this file in a secure, offline location (e.g. encrypted USB drive, safe deposit box).
            4. To recover your vault, you will need BOTH this Recovery Kit AND your master password.
        """.trimIndent()

        return RecoveryKit(
            formatVersion = 1,
            app = "VaultX",
            vaultId = vaultId,
            createdAt = System.currentTimeMillis(),
            saltBase64 = config.saltBase64,
            pbkdf2Iterations = 65536,
            canaryDigestSha256 = canaryDigest,
            integrityHmac = hmac,
            instructions = instructions
        )
    }

    fun exportToJson(kit: RecoveryKit): String {
        return adapter.indent("  ").toJson(kit)
    }

    fun parseFromJson(json: String): RecoveryKit? {
        return try {
            adapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    fun verifyKit(kit: RecoveryKit, currentConfig: VaultConfigEntity?): RecoveryKitVerificationResult {
        if (kit.formatVersion != 1) {
            return RecoveryKitVerificationResult(
                isValid = false,
                version = kit.formatVersion,
                vaultIdMatches = false,
                saltMatches = false,
                integrityVerified = false,
                message = "Unsupported Recovery Kit version (${kit.formatVersion}). Expected version 1."
            )
        }

        val saltBytes = try {
            android.util.Base64.decode(kit.saltBase64, android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            return RecoveryKitVerificationResult(
                isValid = false,
                version = kit.formatVersion,
                vaultIdMatches = false,
                saltMatches = false,
                integrityVerified = false,
                message = "Malformed salt encoding in Recovery Kit."
            )
        }

        val integrityPayload = "${kit.vaultId}|${kit.saltBase64}|${kit.canaryDigestSha256}|${kit.pbkdf2Iterations}".toByteArray(Charsets.UTF_8)
        val computedHmac = CryptoManager.hmacSha256Hex(integrityPayload, saltBytes)
        val integrityOk = computedHmac.equals(kit.integrityHmac, ignoreCase = true)

        if (!integrityOk) {
            return RecoveryKitVerificationResult(
                isValid = false,
                version = kit.formatVersion,
                vaultIdMatches = false,
                saltMatches = false,
                integrityVerified = false,
                message = "Cryptographic integrity check failed! The recovery kit appears corrupted or tampered with."
            )
        }

        var vaultIdMatches = true
        var saltMatches = true

        if (currentConfig != null && currentConfig.isInitialized) {
            saltMatches = currentConfig.saltBase64 == kit.saltBase64
            val currentCanaryDigest = CryptoManager.sha256Hex(currentConfig.canaryCiphertext.toByteArray(Charsets.UTF_8))
            val currentVaultId = UUID.nameUUIDFromBytes(currentConfig.saltBase64.toByteArray(Charsets.UTF_8)).toString()
            vaultIdMatches = currentVaultId == kit.vaultId && currentCanaryDigest == kit.canaryDigestSha256
        }

        val isFullyValid = integrityOk && (currentConfig == null || (saltMatches && vaultIdMatches))

        return RecoveryKitVerificationResult(
            isValid = isFullyValid,
            version = kit.formatVersion,
            vaultIdMatches = vaultIdMatches,
            saltMatches = saltMatches,
            integrityVerified = integrityOk,
            message = if (isFullyValid) {
                "Recovery Kit verified successfully! Cryptographic signature matches current vault parameters."
            } else {
                "Integrity verified, but this Recovery Kit belongs to a different vault or previous configuration."
            }
        )
    }
}
