package com.example.jugglingtracker.services

import android.content.Context
import com.example.jugglingtracker.data.entities.UsageEventType
import com.example.jugglingtracker.data.repository.UsageTrackingRepository
import com.example.jugglingtracker.ui.theme.DynamicThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageTrackingService @Inject constructor(
    private val context: Context,
    private val usageTrackingRepository: UsageTrackingRepository,
    private val dynamicThemeManager: DynamicThemeManager
) {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    // Pattern-related tracking
    fun trackPatternCreated(patternId: Long) {
        trackEvent(UsageEventType.PATTERN_CREATED, patternId)
    }
    
    fun trackPatternEdited(patternId: Long) {
        trackEvent(UsageEventType.PATTERN_EDITED, patternId)
    }
    
    fun trackPatternViewed(patternId: Long) {
        trackEvent(UsageEventType.PATTERN_VIEWED, patternId)
    }
    
    fun trackPatternTagged(patternId: Long, tagCount: Int) {
        trackEvent(UsageEventType.PATTERN_TAGGED, patternId, metadata = "{\"tagCount\":$tagCount}")
    }
    
    // Test session tracking
    fun trackTestStarted(patternId: Long) {
        trackEvent(UsageEventType.TEST_STARTED, patternId)
    }
    
    fun trackTestCompleted(patternId: Long, duration: Long, successCount: Int, attemptCount: Int) {
        val metadata = "{\"successCount\":$successCount,\"attemptCount\":$attemptCount,\"successRate\":${successCount.toFloat()/attemptCount}}"
        trackEvent(UsageEventType.TEST_COMPLETED, patternId, duration, metadata)
    }
    
    fun trackTestCancelled(patternId: Long, duration: Long) {
        trackEvent(UsageEventType.TEST_CANCELLED, patternId, duration)
    }
    
    // Video recording tracking
    fun trackVideoRecorded(patternId: Long?, duration: Long) {
        trackEvent(UsageEventType.VIDEO_RECORDED, patternId, duration)
    }
    
    fun trackVideoTrimmed(patternId: Long?, originalDuration: Long, newDuration: Long) {
        val metadata = "{\"originalDuration\":$originalDuration,\"newDuration\":$newDuration}"
        trackEvent(UsageEventType.VIDEO_TRIMMED, patternId, newDuration, metadata)
    }
    
    // Navigation and UI tracking
    fun trackAppOpened() {
        trackEvent(UsageEventType.APP_OPENED)
    }
    
    fun trackProgressViewed() {
        trackEvent(UsageEventType.PROGRESS_VIEWED)
    }
    
    fun trackHistoryViewed() {
        trackEvent(UsageEventType.HISTORY_VIEWED)
    }
    
    fun trackSettingsAccessed() {
        trackEvent(UsageEventType.SETTINGS_ACCESSED)
    }
    
    fun trackPatternSearched(query: String, resultCount: Int) {
        val metadata = "{\"query\":\"$query\",\"resultCount\":$resultCount}"
        trackEvent(UsageEventType.PATTERN_SEARCHED, metadata = metadata)
    }
    
    fun trackPatternSorted(sortType: String) {
        val metadata = "{\"sortType\":\"$sortType\"}"
        trackEvent(UsageEventType.PATTERN_SORTED, metadata = metadata)
    }
    
    // Batch tracking for multiple events
    fun trackMultipleEvents(events: List<Pair<UsageEventType, Long?>>) {
        serviceScope.launch {
            val usageEvents = events.map { (eventType, patternId) ->
                com.example.jugglingtracker.data.entities.UsageEvent(
                    eventType = eventType,
                    timestamp = System.currentTimeMillis(),
                    patternId = patternId
                )
            }
            usageTrackingRepository.trackEvents(usageEvents)
            dynamicThemeManager.onUsageEventTracked()
        }
    }
    
    // Session-based tracking
    private var sessionStartTime: Long = 0
    private var currentSessionEvents = mutableListOf<UsageEventType>()
    
    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        currentSessionEvents.clear()
        trackAppOpened()
    }
    
    fun endSession() {
        if (sessionStartTime > 0) {
            val sessionDuration = System.currentTimeMillis() - sessionStartTime
            val metadata = "{\"sessionDuration\":$sessionDuration,\"eventsInSession\":${currentSessionEvents.size}}"
            
            // Track session summary if it was meaningful (more than 30 seconds)
            if (sessionDuration > 30000) {
                serviceScope.launch {
                    usageTrackingRepository.trackEvent(
                        eventType = UsageEventType.APP_OPENED, // Reuse for session end
                        duration = sessionDuration,
                        metadata = metadata
                    )
                }
            }
            
            sessionStartTime = 0
            currentSessionEvents.clear()
        }
    }
    
    // Analytics and insights
    suspend fun getWeeklyInsights(): WeeklyInsights {
        val currentWeekUsage = usageTrackingRepository.getCurrentWeekUsageSync()
        val usageLevel = usageTrackingRepository.getCurrentUsageLevel()
        val weeklyTrends = usageTrackingRepository.getWeeklyTrends(4)
        
        return WeeklyInsights(
            currentPoints = currentWeekUsage?.totalPoints ?: 0,
            usageLevel = usageLevel,
            patternsCreated = currentWeekUsage?.patternsCreated ?: 0,
            testsCompleted = currentWeekUsage?.testsCompleted ?: 0,
            totalTestDuration = currentWeekUsage?.totalTestDuration ?: 0,
            videosRecorded = currentWeekUsage?.videosRecorded ?: 0,
            appOpens = currentWeekUsage?.appOpens ?: 0,
            weeklyTrends = weeklyTrends,
            averageWeeklyPoints = usageTrackingRepository.getAverageWeeklyPoints()
        )
    }
    
    // Maintenance
    fun performMaintenance() {
        serviceScope.launch {
            usageTrackingRepository.cleanupOldData()
        }
    }
    
    // Private helper method
    private fun trackEvent(
        eventType: UsageEventType,
        patternId: Long? = null,
        duration: Long? = null,
        metadata: String? = null
    ) {
        currentSessionEvents.add(eventType)
        
        serviceScope.launch {
            usageTrackingRepository.trackEvent(eventType, patternId, duration, metadata)
            dynamicThemeManager.onUsageEventTracked()
        }
    }
}

data class WeeklyInsights(
    val currentPoints: Int,
    val usageLevel: com.example.jugglingtracker.data.entities.UsageLevel,
    val patternsCreated: Int,
    val testsCompleted: Int,
    val totalTestDuration: Long,
    val videosRecorded: Int,
    val appOpens: Int,
    val weeklyTrends: List<com.example.jugglingtracker.data.entities.WeeklyUsage>,
    val averageWeeklyPoints: Double
) {
    val averageTestDuration: Long
        get() = if (testsCompleted > 0) totalTestDuration / testsCompleted else 0
    
    val isActiveWeek: Boolean
        get() = currentPoints >= 50 // Casual level threshold
    
    val progressToNextLevel: Float
        get() {
            val nextLevel = com.example.jugglingtracker.data.entities.UsageLevel.LEVELS
                .find { it.level > usageLevel.level }
            return if (nextLevel != null) {
                val progress = (currentPoints - usageLevel.minPoints).toFloat()
                val range = (nextLevel.minPoints - usageLevel.minPoints).toFloat()
                (progress / range).coerceIn(0f, 1f)
            } else 1f
        }
}