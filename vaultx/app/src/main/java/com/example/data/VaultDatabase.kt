package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VaultConfigEntity::class,
        EncryptedItem::class,
        VaultAuditLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun vaultDao(): VaultDao
    abstract fun encryptedItemDao(): EncryptedItemDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vault_config ADD COLUMN isPrivacyModeEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vault_config ADD COLUMN failedAttemptsCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vault_config ADD COLUMN lockoutUntilTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vault_config ADD COLUMN lastFailedAttemptTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vault_config ADD COLUMN recoveryKitCreatedAt INTEGER")
                db.execSQL("ALTER TABLE vault_config ADD COLUMN recoveryKitHash TEXT")
                db.execSQL("ALTER TABLE vault_audit_logs ADD COLUMN severity TEXT NOT NULL DEFAULT 'INFO'")
                db.execSQL("ALTER TABLE vault_audit_logs ADD COLUMN category TEXT NOT NULL DEFAULT 'SECURITY'")
            }
        }

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vaultx_local.db"
                ).addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
