package com.example.jugglingtracker.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.jugglingtracker.data.dao.PatternDao
import com.example.jugglingtracker.data.dao.TestSessionDao
import com.example.jugglingtracker.data.dao.TagDao
import com.example.jugglingtracker.data.dao.UsageEventDao
import com.example.jugglingtracker.data.dao.WeeklyUsageDao
import com.example.jugglingtracker.data.entities.*

@Database(
    entities = [
        Pattern::class,
        TestSession::class,
        Tag::class,
        PatternTagCrossRef::class,
        PatternPrerequisiteCrossRef::class,
        PatternDependentCrossRef::class,
        PatternRelatedCrossRef::class,
        UsageEvent::class,
        WeeklyUsage::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class JugglingDatabase : RoomDatabase() {
    
    abstract fun patternDao(): PatternDao
    abstract fun testSessionDao(): TestSessionDao
    abstract fun tagDao(): TagDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun weeklyUsageDao(): WeeklyUsageDao
    
    companion object {
        @Volatile
        private var INSTANCE: JugglingDatabase? = null
        
        private const val DATABASE_NAME = "juggling_database"
        
        fun getDatabase(context: Context): JugglingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JugglingDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(DatabaseCallback())
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3
                )
                .fallbackToDestructiveMigration() // Remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Migration from version 1 to 2: Add usage tracking tables
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create usage_events table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `usage_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `patternId` INTEGER,
                        `duration` INTEGER,
                        `metadata` TEXT,
                        FOREIGN KEY(`patternId`) REFERENCES `patterns`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                
                // Create indices for usage_events
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_events_patternId` ON `usage_events` (`patternId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_events_timestamp` ON `usage_events` (`timestamp`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_events_eventType` ON `usage_events` (`eventType`)")
                
                // Create weekly_usage table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `weekly_usage` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `weekStartTimestamp` INTEGER NOT NULL,
                        `totalPoints` INTEGER NOT NULL DEFAULT 0,
                        `patternsCreated` INTEGER NOT NULL DEFAULT 0,
                        `testsCompleted` INTEGER NOT NULL DEFAULT 0,
                        `totalTestDuration` INTEGER NOT NULL DEFAULT 0,
                        `videosRecorded` INTEGER NOT NULL DEFAULT 0,
                        `appOpens` INTEGER NOT NULL DEFAULT 0,
                        `lastUpdated` INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create unique index for weekly_usage
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_weekly_usage_weekStartTimestamp` ON `weekly_usage` (`weekStartTimestamp`)")
            }
        }
        
        // Migration from version 2 to 3: Update foreign key constraints
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Update the foreign key constraint for usage_events table
                // Since SQLite doesn't support ALTER TABLE for foreign keys,
                // we need to recreate the table with the updated constraint
                
                // Create new table with updated foreign key constraint
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `usage_events_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `patternId` INTEGER,
                        `duration` INTEGER,
                        `metadata` TEXT,
                        FOREIGN KEY(`patternId`) REFERENCES `patterns`(`id`) ON UPDATE CASCADE ON DELETE SET NULL
                    )
                """.trimIndent())
                
                // Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO `usage_events_new` (`id`, `eventType`, `timestamp`, `patternId`, `duration`, `metadata`)
                    SELECT `id`, `eventType`, `timestamp`, `patternId`, `duration`, `metadata` FROM `usage_events`
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE `usage_events`")
                
                // Rename new table to original name
                database.execSQL("ALTER TABLE `usage_events_new` RENAME TO `usage_events`")
                
                // Recreate indices
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_events_patternId` ON `usage_events` (`patternId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_events_timestamp` ON `usage_events` (`timestamp`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_events_eventType` ON `usage_events` (`eventType`)")
            }
        }
        
        // Database callback for initial setup
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Perform any initial setup here
                // For example, insert default data
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }
        
        // Method to close database (for testing or cleanup)
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
    
    /**
     * Clear all tables for backup restore
     */
    suspend fun clearAllTablesForRestore() {
        // Use Room's built-in clearAllTables method
        clearAllTables()
    }
}

// Type converters for Room database
class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): java.util.Date? {
        return value?.let { java.util.Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: java.util.Date?): Long? {
        return date?.time
    }
    
    // Add more type converters as needed
    // For example, for lists, enums, etc.
}