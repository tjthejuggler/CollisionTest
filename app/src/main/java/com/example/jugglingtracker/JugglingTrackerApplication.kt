package com.example.jugglingtracker

import android.app.Application
import com.example.jugglingtracker.data.database.JugglingDatabase
import com.example.jugglingtracker.data.repository.PatternRepository
import com.example.jugglingtracker.data.repository.TestSessionRepository
import com.example.jugglingtracker.data.repository.TagRepository

/**
 * Application class for the Juggling Tracker app.
 * Initializes the database and repositories for dependency injection.
 */
class JugglingTrackerApplication : Application() {
    
    // Database instance
    val database by lazy { JugglingDatabase.getDatabase(this) }
    
    // Repository instances
    val patternRepository by lazy { PatternRepository(database.patternDao()) }
    val testSessionRepository by lazy { TestSessionRepository(database.testSessionDao()) }
    val tagRepository by lazy { TagRepository(database.tagDao()) }
    
    override fun onCreate() {
        super.onCreate()
        // Any additional initialization can be done here
    }
}