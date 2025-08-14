package com.example.jugglingtracker.data.dao

import androidx.room.*
import com.example.jugglingtracker.data.entities.WeeklyUsage
import com.example.jugglingtracker.data.backup.WeeklyUsageExport
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyUsageDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyUsage(weeklyUsage: WeeklyUsage): Long
    
    @Update
    suspend fun updateWeeklyUsage(weeklyUsage: WeeklyUsage)
    
    @Query("SELECT * FROM weekly_usage WHERE weekStartTimestamp = :weekStart")
    suspend fun getWeeklyUsage(weekStart: Long): WeeklyUsage?
    
    @Query("SELECT * FROM weekly_usage WHERE weekStartTimestamp = :weekStart")
    fun getWeeklyUsageFlow(weekStart: Long): Flow<WeeklyUsage?>
    
    @Query("SELECT * FROM weekly_usage ORDER BY weekStartTimestamp DESC")
    fun getAllWeeklyUsage(): Flow<List<WeeklyUsage>>
    
    @Query("SELECT * FROM weekly_usage ORDER BY weekStartTimestamp DESC LIMIT :limit")
    suspend fun getRecentWeeklyUsage(limit: Int = 12): List<WeeklyUsage>
    
    @Query("SELECT * FROM weekly_usage WHERE weekStartTimestamp >= :startTime AND weekStartTimestamp <= :endTime ORDER BY weekStartTimestamp DESC")
    suspend fun getWeeklyUsageInRange(startTime: Long, endTime: Long): List<WeeklyUsage>
    
    @Query("SELECT * FROM weekly_usage ORDER BY weekStartTimestamp DESC LIMIT 1")
    suspend fun getCurrentWeekUsage(): WeeklyUsage?
    
    @Query("SELECT * FROM weekly_usage ORDER BY weekStartTimestamp DESC LIMIT 1")
    fun getCurrentWeekUsageFlow(): Flow<WeeklyUsage?>
    
    @Query("SELECT SUM(totalPoints) FROM weekly_usage WHERE weekStartTimestamp >= :startTime")
    suspend fun getTotalPointsSince(startTime: Long): Int?
    
    @Query("SELECT AVG(totalPoints) FROM weekly_usage WHERE weekStartTimestamp >= :startTime")
    suspend fun getAveragePointsSince(startTime: Long): Double?
    
    @Query("SELECT MAX(totalPoints) FROM weekly_usage")
    suspend fun getMaxWeeklyPoints(): Int?
    
    @Query("DELETE FROM weekly_usage WHERE weekStartTimestamp < :cutoffTime")
    suspend fun deleteOldWeeklyUsage(cutoffTime: Long): Int
    
    @Query("DELETE FROM weekly_usage")
    suspend fun deleteAllWeeklyUsage()
    
    // Analytics queries
    @Query("""
        SELECT 
            weekStartTimestamp,
            totalPoints,
            patternsCreated,
            testsCompleted,
            totalTestDuration,
            videosRecorded,
            appOpens
        FROM weekly_usage 
        WHERE weekStartTimestamp >= :startTime 
        ORDER BY weekStartTimestamp ASC
    """)
    suspend fun getWeeklyTrends(startTime: Long): List<WeeklyTrend>
    
    @Query("""
        SELECT COUNT(*) 
        FROM weekly_usage 
        WHERE totalPoints >= :minPoints AND weekStartTimestamp >= :startTime
    """)
    suspend fun getActiveWeeksCount(minPoints: Int = 10, startTime: Long): Int
    
    @Transaction
    suspend fun upsertWeeklyUsage(weeklyUsage: WeeklyUsage) {
        val existing = getWeeklyUsage(weeklyUsage.weekStartTimestamp)
        if (existing != null) {
            updateWeeklyUsage(weeklyUsage.copy(id = existing.id))
        } else {
            insertWeeklyUsage(weeklyUsage)
        }
    }
    
    // Backup and restore methods
    @Query("""
        SELECT id, weekStartTimestamp, totalPoints, patternsCreated, testsCompleted,
               totalTestDuration, videosRecorded, appOpens, lastUpdated
        FROM weekly_usage ORDER BY id ASC
    """)
    suspend fun getAllWeeklyUsageForExport(): List<WeeklyUsageExport>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyUsageFromBackup(weeklyUsage: List<WeeklyUsage>)
}

data class WeeklyTrend(
    val weekStartTimestamp: Long,
    val totalPoints: Int,
    val patternsCreated: Int,
    val testsCompleted: Int,
    val totalTestDuration: Long,
    val videosRecorded: Int,
    val appOpens: Int
)