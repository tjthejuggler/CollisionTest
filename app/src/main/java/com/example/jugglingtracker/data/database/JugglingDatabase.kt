package com.example.jugglingtracker.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.jugglingtracker.data.dao.PatternDao
import com.example.jugglingtracker.data.dao.TestSessionDao
import com.example.jugglingtracker.data.dao.TagDao
import com.example.jugglingtracker.data.entities.*

@Database(
    entities = [
        Pattern::class,
        TestSession::class,
        Tag::class,
        PatternTagCrossRef::class,
        PatternPrerequisiteCrossRef::class,
        PatternDependentCrossRef::class,
        PatternRelatedCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class JugglingDatabase : RoomDatabase() {
    
    abstract fun patternDao(): PatternDao
    abstract fun testSessionDao(): TestSessionDao
    abstract fun tagDao(): TagDao
    
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
                    // Add future migrations here
                    // MIGRATION_1_2,
                    // MIGRATION_2_3
                )
                .fallbackToDestructiveMigration() // Remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Example migration for future use
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: Add a new column to patterns table
                // database.execSQL("ALTER TABLE patterns ADD COLUMN new_column TEXT")
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