package com.example.jugglingtracker.data.dao

import androidx.room.*
import com.example.jugglingtracker.data.entities.UsageEvent
import com.example.jugglingtracker.data.entities.UsageEventType
import com.example.jugglingtracker.data.backup.UsageEventExport
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageEventDao {
    
    @Insert
    suspend fun insertUsageEvent(event: UsageEvent): Long
    
    @Insert
    suspend fun insertUsageEvents(events: List<UsageEvent>)
    
    @Query("SELECT * FROM usage_events ORDER BY timestamp DESC")
    fun getAllUsageEvents(): Flow<List<UsageEvent>>
    
    @Query("SELECT * FROM usage_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getUsageEventsInRange(startTime: Long, endTime: Long): Flow<List<UsageEvent>>
    
    @Query("SELECT * FROM usage_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getUsageEventsInRangeSync(startTime: Long, endTime: Long): List<UsageEvent>
    
    @Query("SELECT * FROM usage_events WHERE eventType = :eventType ORDER BY timestamp DESC")
    fun getUsageEventsByType(eventType: UsageEventType): Flow<List<UsageEvent>>
    
    @Query("SELECT * FROM usage_events WHERE patternId = :patternId ORDER BY timestamp DESC")
    fun getUsageEventsForPattern(patternId: Long): Flow<List<UsageEvent>>
    
    @Query("SELECT COUNT(*) FROM usage_events WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getEventCountInRange(startTime: Long, endTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM usage_events WHERE eventType = :eventType AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getEventCountByTypeInRange(eventType: UsageEventType, startTime: Long, endTime: Long): Int
    
    @Query("SELECT SUM(duration) FROM usage_events WHERE eventType = :eventType AND timestamp >= :startTime AND timestamp <= :endTime AND duration IS NOT NULL")
    suspend fun getTotalDurationByTypeInRange(eventType: UsageEventType, startTime: Long, endTime: Long): Long?
    
    @Query("SELECT * FROM usage_events WHERE timestamp >= :startTime ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentUsageEvents(startTime: Long, limit: Int = 100): List<UsageEvent>
    
    @Query("DELETE FROM usage_events WHERE timestamp < :cutoffTime")
    suspend fun deleteOldEvents(cutoffTime: Long): Int
    
    @Query("DELETE FROM usage_events")
    suspend fun deleteAllEvents()
    
    // Analytics queries
    @Query("""
        SELECT eventType, COUNT(*) as count 
        FROM usage_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
        GROUP BY eventType 
        ORDER BY count DESC
    """)
    suspend fun getEventTypeStats(startTime: Long, endTime: Long): List<EventTypeStat>
    
    @Query("""
        SELECT DATE(timestamp/1000, 'unixepoch') as date, COUNT(*) as count 
        FROM usage_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
        GROUP BY DATE(timestamp/1000, 'unixepoch') 
        ORDER BY date DESC
    """)
    suspend fun getDailyEventCounts(startTime: Long, endTime: Long): List<DailyEventCount>
    
    // Backup and restore methods
    @Query("SELECT id, eventType, timestamp, patternId, duration, metadata FROM usage_events ORDER BY id ASC")
    suspend fun getAllUsageEventsForExport(): List<UsageEventExport>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageEventsFromBackup(events: List<UsageEvent>)
}

data class EventTypeStat(
    val eventType: UsageEventType,
    val count: Int
)

data class DailyEventCount(
    val date: String,
    val count: Int
)