package com.example.jugglingtracker

import android.app.Application
import androidx.lifecycle.lifecycleScope
import com.example.jugglingtracker.data.database.JugglingDatabase
import com.example.jugglingtracker.data.repository.PatternRepository
import com.example.jugglingtracker.data.repository.TestSessionRepository
import com.example.jugglingtracker.data.repository.TagRepository
import com.example.jugglingtracker.data.repository.BackupRepository
import com.example.jugglingtracker.services.UsageTrackingService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for the Juggling Tracker app.
 * Initializes the database and repositories for dependency injection.
 */
@HiltAndroidApp
class JugglingTrackerApplication : Application() {
    
    // Database instance
    val database by lazy { JugglingDatabase.getDatabase(this) }
    
    // Repository instances
    val patternRepository by lazy { PatternRepository(database.patternDao()) }
    val testSessionRepository by lazy { TestSessionRepository(database.testSessionDao()) }
    val tagRepository by lazy { TagRepository(database.tagDao()) }
    val backupRepository by lazy { BackupRepository(this, database) }
    
    @Inject
    lateinit var usageTrackingService: UsageTrackingService
    
    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize usage tracking maintenance
        applicationScope.launch {
            // Perform periodic maintenance (cleanup old data)
            usageTrackingService.performMaintenance()
        }
    }
}