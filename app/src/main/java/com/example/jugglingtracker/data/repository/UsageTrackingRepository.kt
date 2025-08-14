package com.example.jugglingtracker.data.repository

import com.example.jugglingtracker.data.dao.UsageEventDao
import com.example.jugglingtracker.data.dao.WeeklyUsageDao
import com.example.jugglingtracker.data.entities.UsageEvent
import com.example.jugglingtracker.data.entities.UsageEventType
import com.example.jugglingtracker.data.entities.WeeklyUsage
import com.example.jugglingtracker.data.entities.UsageLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageTrackingRepository @Inject constructor(
    private val usageEventDao: UsageEventDao,
    private val weeklyUsageDao: WeeklyUsageDao
) {
    
    // Usage Event operations
    suspend fun trackEvent(
        eventType: UsageEventType,
        patternId: Long? = null,
        duration: Long? = null,
        metadata: String? = null
    ) {
        val event = UsageEvent(
            eventType = eventType,
            timestamp = System.currentTimeMillis(),
            patternId = patternId,
            duration = duration,
            metadata = metadata
        )
        
        try {
            usageEventDao.insertUsageEvent(event)
            updateWeeklyUsage(event)
        } catch (e: Exception) {
            // If foreign key constraint fails, insert without patternId
            if (e.message?.contains("FOREIGN KEY constraint failed") == true && patternId != null) {
                val eventWithoutPattern = event.copy(patternId = null)
                usageEventDao.insertUsageEvent(eventWithoutPattern)
                updateWeeklyUsage(eventWithoutPattern)
            } else {
                throw e
            }
        }
    }
    
    suspend fun trackEvents(events: List<UsageEvent>) {
        try {
            usageEventDao.insertUsageEvents(events)
            events.forEach { updateWeeklyUsage(it) }
        } catch (e: Exception) {
            // If foreign key constraint fails, insert events without invalid patternIds
            if (e.message?.contains("FOREIGN KEY constraint failed") == true) {
                val eventsWithoutInvalidPatterns = events.map { event ->
                    if (event.patternId != null) event.copy(patternId = null) else event
                }
                usageEventDao.insertUsageEvents(eventsWithoutInvalidPatterns)
                eventsWithoutInvalidPatterns.forEach { updateWeeklyUsage(it) }
            } else {
                throw e
            }
        }
    }
    
    fun getAllUsageEvents(): Flow<List<UsageEvent>> = usageEventDao.getAllUsageEvents()
    
    fun getUsageEventsInRange(startTime: Long, endTime: Long): Flow<List<UsageEvent>> =
        usageEventDao.getUsageEventsInRange(startTime, endTime)
    
    fun getUsageEventsForPattern(patternId: Long): Flow<List<UsageEvent>> =
        usageEventDao.getUsageEventsForPattern(patternId)
    
    // Weekly Usage operations
    fun getCurrentWeekUsage(): Flow<WeeklyUsage?> = weeklyUsageDao.getCurrentWeekUsageFlow()
    
    suspend fun getCurrentWeekUsageSync(): WeeklyUsage? = weeklyUsageDao.getCurrentWeekUsage()
    
    fun getAllWeeklyUsage(): Flow<List<WeeklyUsage>> = weeklyUsageDao.getAllWeeklyUsage()
    
    suspend fun getWeeklyUsageInRange(startTime: Long, endTime: Long): List<WeeklyUsage> =
        weeklyUsageDao.getWeeklyUsageInRange(startTime, endTime)
    
    // Scoring and calculation methods
    suspend fun calculateEventPoints(event: UsageEvent): Int {
        var points = event.eventType.basePoints
        
        // Apply duration-based multipliers for certain events
        when (event.eventType) {
            UsageEventType.TEST_COMPLETED -> {
                event.duration?.let { duration ->
                    // Bonus points for longer tests (every 30 seconds adds 1 point, max 10 bonus)
                    val durationBonus = minOf((duration / 30000).toInt(), 10)
                    points += durationBonus
                }
            }
            UsageEventType.VIDEO_RECORDED -> {
                event.duration?.let { duration ->
                    // Bonus points for longer videos (every 10 seconds adds 1 point, max 5 bonus)
                    val durationBonus = minOf((duration / 10000).toInt(), 5)
                    points += durationBonus
                }
            }
            else -> { /* No duration bonus for other events */ }
        }
        
        return points
    }
    
    suspend fun getCurrentWeekPoints(): Int {
        val currentWeek = getCurrentWeekUsageSync()
        return currentWeek?.totalPoints ?: 0
    }
    
    suspend fun getCurrentUsageLevel(): UsageLevel {
        val points = getCurrentWeekPoints()
        return UsageLevel.getLevelForPoints(points)
    }
    
    suspend fun getCurrentAccentColor(isDarkTheme: Boolean = false): String {
        val points = getCurrentWeekPoints()
        return UsageLevel.getColorForPoints(points, isDarkTheme)
    }
    
    // Weekly usage update logic
    private suspend fun updateWeeklyUsage(event: UsageEvent) {
        val weekStart = getWeekStartTimestamp(event.timestamp)
        val existingWeekly = weeklyUsageDao.getWeeklyUsage(weekStart)
        val points = calculateEventPoints(event)
        
        val updatedWeekly = if (existingWeekly != null) {
            existingWeekly.copy(
                totalPoints = existingWeekly.totalPoints + points,
                patternsCreated = existingWeekly.patternsCreated + if (event.eventType == UsageEventType.PATTERN_CREATED) 1 else 0,
                testsCompleted = existingWeekly.testsCompleted + if (event.eventType == UsageEventType.TEST_COMPLETED) 1 else 0,
                totalTestDuration = existingWeekly.totalTestDuration + (if (event.eventType == UsageEventType.TEST_COMPLETED) event.duration ?: 0 else 0),
                videosRecorded = existingWeekly.videosRecorded + if (event.eventType == UsageEventType.VIDEO_RECORDED) 1 else 0,
                appOpens = existingWeekly.appOpens + if (event.eventType == UsageEventType.APP_OPENED) 1 else 0,
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            WeeklyUsage(
                weekStartTimestamp = weekStart,
                totalPoints = points,
                patternsCreated = if (event.eventType == UsageEventType.PATTERN_CREATED) 1 else 0,
                testsCompleted = if (event.eventType == UsageEventType.TEST_COMPLETED) 1 else 0,
                totalTestDuration = if (event.eventType == UsageEventType.TEST_COMPLETED) event.duration ?: 0 else 0,
                videosRecorded = if (event.eventType == UsageEventType.VIDEO_RECORDED) 1 else 0,
                appOpens = if (event.eventType == UsageEventType.APP_OPENED) 1 else 0,
                lastUpdated = System.currentTimeMillis()
            )
        }
        
        weeklyUsageDao.upsertWeeklyUsage(updatedWeekly)
    }
    
    // Utility methods
    private fun getWeekStartTimestamp(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    fun getCurrentWeekStartTimestamp(): Long {
        return getWeekStartTimestamp(System.currentTimeMillis())
    }
    
    // Analytics methods
    suspend fun getWeeklyTrends(weeksBack: Int = 12): List<WeeklyUsage> {
        val startTime = getCurrentWeekStartTimestamp() - (weeksBack * 7 * 24 * 60 * 60 * 1000L)
        return weeklyUsageDao.getWeeklyUsageInRange(startTime, System.currentTimeMillis())
    }
    
    suspend fun getTotalPointsSince(startTime: Long): Int {
        return weeklyUsageDao.getTotalPointsSince(startTime) ?: 0
    }
    
    suspend fun getAverageWeeklyPoints(weeksBack: Int = 4): Double {
        val startTime = getCurrentWeekStartTimestamp() - (weeksBack * 7 * 24 * 60 * 60 * 1000L)
        return weeklyUsageDao.getAveragePointsSince(startTime) ?: 0.0
    }
    
    // Cleanup methods
    suspend fun cleanupOldData(daysToKeep: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        usageEventDao.deleteOldEvents(cutoffTime)
        
        // Keep weekly data for longer (1 year)
        val weeklyCutoffTime = System.currentTimeMillis() - (365 * 24 * 60 * 60 * 1000L)
        weeklyUsageDao.deleteOldWeeklyUsage(weeklyCutoffTime)
    }
}