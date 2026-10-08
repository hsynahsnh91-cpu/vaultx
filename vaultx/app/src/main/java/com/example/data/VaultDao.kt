package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Unified DAO interface combining Vault configuration, audit logs, and encrypted items.
 */
@Dao
interface VaultDao {

    // Config
    @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<VaultConfigEntity?>

    @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): VaultConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: VaultConfigEntity)

    // Items
    @Query("SELECT * FROM encrypted_items WHERE isTrash = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM encrypted_items WHERE isTrash = 1 ORDER BY trashTimestamp DESC")
    fun getTrashItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM encrypted_items WHERE id = :id LIMIT 1")
    fun getItemById(id: String): Flow<VaultItemEntity?>

    @Query("SELECT * FROM encrypted_items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdDirect(id: String): VaultItemEntity?

    @Query("SELECT * FROM encrypted_items")
    suspend fun getAllItemsDirect(): List<VaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VaultItemEntity>)

    @Update
    suspend fun updateItem(item: VaultItemEntity)

    @Query("UPDATE encrypted_items SET isTrash = 1, trashTimestamp = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE encrypted_items SET isTrash = 0, trashTimestamp = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("DELETE FROM encrypted_items WHERE id = :id")
    suspend fun permanentlyDeleteItem(id: String)

    @Query("DELETE FROM encrypted_items WHERE isTrash = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM encrypted_items")
    suspend fun deleteAllItems()

    @Query("SELECT * FROM encrypted_items WHERE isTrash = 0 AND (title LIKE '%' || :query || '%' OR folder LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC")
    fun searchActiveItems(query: String): Flow<List<VaultItemEntity>>

    // Audit logs
    @Query("SELECT * FROM vault_audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<VaultAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VaultAuditLogEntity)

    @Query("DELETE FROM vault_audit_logs")
    suspend fun clearLogs()
}
