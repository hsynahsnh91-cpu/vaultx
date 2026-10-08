package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for EncryptedItem entities.
 * Supports reactive queries via Kotlin Flow and atomic suspend mutations.
 */
@Dao
interface EncryptedItemDao {

    @Query("SELECT * FROM encrypted_items WHERE isTrash = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveItems(): Flow<List<EncryptedItem>>

    @Query("SELECT * FROM encrypted_items WHERE isTrash = 1 ORDER BY trashTimestamp DESC")
    fun getTrashItems(): Flow<List<EncryptedItem>>

    @Query("SELECT * FROM encrypted_items WHERE id = :id LIMIT 1")
    fun getItemById(id: String): Flow<EncryptedItem?>

    @Query("SELECT * FROM encrypted_items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdDirect(id: String): EncryptedItem?

    @Query("SELECT * FROM encrypted_items")
    suspend fun getAllItemsDirect(): List<EncryptedItem>

    @Query("SELECT * FROM encrypted_items WHERE type = :type AND isTrash = 0 ORDER BY updatedAt DESC")
    fun getItemsByType(type: String): Flow<List<EncryptedItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: EncryptedItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<EncryptedItem>)

    @Update
    suspend fun updateItem(item: EncryptedItem)

    @Delete
    suspend fun deleteItem(item: EncryptedItem)

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
    fun searchActiveItems(query: String): Flow<List<EncryptedItem>>
}
