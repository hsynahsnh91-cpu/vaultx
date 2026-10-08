package com.example.duplicate

import android.content.Context
import com.example.model.DuplicateGroup
import com.example.model.VaultItem
import com.example.security.VaultSecurityModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

data class DuplicateFileEntry(
    val item: VaultItem,
    val sha256: String,
    val sizeBytes: Long
)

object DuplicateDetector {

    /**
     * Calculates the SHA-256 hash of an encrypted or raw file on disk using streaming chunks.
     * Prevents loading the whole file into RAM.
     */
    fun calculateFileSha256(file: File): String {
        if (!file.exists() || !file.isFile) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Scans vault file items and identifies exact content duplicates based on SHA-256.
     * Reports progress via [onProgress] (current, total).
     */
    suspend fun scanForDuplicates(
        context: Context,
        fileItems: List<VaultItem>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val vaultDir = VaultSecurityModule.getVaultFilesDir(context)
        val entries = mutableListOf<DuplicateFileEntry>()

        val total = fileItems.size
        fileItems.forEachIndexed { index, item ->
            onProgress(index + 1, total)
            val relPath = item.fileRelativePath
            if (!relPath.isNullOrBlank()) {
                val file = File(vaultDir, relPath)
                if (file.exists()) {
                    val hash = calculateFileSha256(file)
                    if (hash.isNotEmpty()) {
                        entries.add(DuplicateFileEntry(item, hash, file.length()))
                    }
                }
            }
        }

        // Group by hash and filter groups with more than 1 item
        entries.groupBy { it.sha256 }
            .filter { it.value.size > 1 }
            .map { (hash, groupEntries) ->
                val allItems = groupEntries.map { it.item }
                val original = allItems.first()
                val dups = allItems.drop(1)
                com.example.model.DuplicateGroup(
                    id = hash,
                    contentHash = hash,
                    original = original,
                    duplicates = dups,
                    fileSizeBytes = groupEntries.first().sizeBytes
                )
            }
    }
}
