package com.example.jugglingtracker.data.dao

import androidx.room.*
import com.example.jugglingtracker.data.entities.TestSession
import kotlinx.coroutines.flow.Flow

@Dao
interface TestSessionDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM test_sessions ORDER BY date DESC")
    fun getAllTestSessions(): Flow<List<TestSession>>
    
    @Query("SELECT * FROM test_sessions WHERE id = :id")
    suspend fun getTestSessionById(id: Long): TestSession?
    
    @Query("SELECT * FROM test_sessions WHERE id = :id")
    fun getTestSessionByIdFlow(id: Long): Flow<TestSession?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestSession(testSession: TestSession): Long
    
    @Update
    suspend fun updateTestSession(testSession: TestSession)
    
    @Delete
    suspend fun deleteTestSession(testSession: TestSession)
    
    @Query("DELETE FROM test_sessions WHERE id = :id")
    suspend fun deleteTestSessionById(id: Long)
    
    // Pattern-specific queries
    @Query("SELECT * FROM test_sessions WHERE patternId = :patternId ORDER BY date DESC")
    fun getTestSessionsByPattern(patternId: Long): Flow<List<TestSession>>
    
    @Query("SELECT * FROM test_sessions WHERE patternId = :patternId ORDER BY date DESC LIMIT :limit")
    fun getRecentTestSessionsByPattern(patternId: Long, limit: Int): Flow<List<TestSession>>
    
    @Query("SELECT * FROM test_sessions WHERE patternId = :patternId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestTestSessionByPattern(patternId: Long): TestSession?
    
    @Query("SELECT * FROM test_sessions WHERE patternId = :patternId ORDER BY date ASC LIMIT 1")
    suspend fun getFirstTestSessionByPattern(patternId: Long): TestSession?
    
    // Date-based queries
    @Query("SELECT * FROM test_sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTestSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<TestSession>>
    
    @Query("SELECT * FROM test_sessions WHERE date >= :startDate ORDER BY date DESC")
    fun getTestSessionsSince(startDate: Long): Flow<List<TestSession>>
    
    @Query("SELECT * FROM test_sessions WHERE date >= :todayStart AND date < :tomorrowStart ORDER BY date DESC")
    fun getTodaysTestSessions(todayStart: Long, tomorrowStart: Long): Flow<List<TestSession>>
    
    // Performance-based queries
    @Query("SELECT * FROM test_sessions WHERE (successCount * 100.0 / attemptCount) >= :minSuccessRate ORDER BY date DESC")
    fun getTestSessionsByMinSuccessRate(minSuccessRate: Double): Flow<List<TestSession>>
    
    @Query("SELECT * FROM test_sessions WHERE duration >= :minDuration ORDER BY date DESC")
    fun getTestSessionsByMinDuration(minDuration: Long): Flow<List<TestSession>>
    
    @Query("SELECT * FROM test_sessions WHERE attemptCount >= :minAttempts ORDER BY date DESC")
    fun getTestSessionsByMinAttempts(minAttempts: Int): Flow<List<TestSession>>
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM test_sessions")
    suspend fun getTotalTestSessionCount(): Int
    
    @Query("SELECT COUNT(*) FROM test_sessions WHERE patternId = :patternId")
    suspend fun getTestSessionCountByPattern(patternId: Long): Int
    
    @Query("SELECT SUM(duration) FROM test_sessions WHERE patternId = :patternId")
    suspend fun getTotalPracticeTimeByPattern(patternId: Long): Long?
    
    @Query("SELECT AVG(successCount * 100.0 / attemptCount) FROM test_sessions WHERE patternId = :patternId AND attemptCount > 0")
    suspend fun getAverageSuccessRateByPattern(patternId: Long): Double?
    
    @Query("SELECT SUM(successCount) FROM test_sessions WHERE patternId = :patternId")
    suspend fun getTotalSuccessCountByPattern(patternId: Long): Int?
    
    @Query("SELECT SUM(attemptCount) FROM test_sessions WHERE patternId = :patternId")
    suspend fun getTotalAttemptCountByPattern(patternId: Long): Int?
    
    @Query("SELECT AVG(duration) FROM test_sessions WHERE patternId = :patternId")
    suspend fun getAverageDurationByPattern(patternId: Long): Double?
    
    // Progress tracking queries
    @Query("""
        SELECT * FROM test_sessions 
        WHERE patternId = :patternId 
        ORDER BY date ASC
    """)
    fun getTestSessionProgressByPattern(patternId: Long): Flow<List<TestSession>>
    
    @Query("""
        SELECT AVG(successCount * 100.0 / attemptCount) as avgSuccessRate
        FROM test_sessions 
        WHERE patternId = :patternId 
        AND date >= :startDate 
        AND attemptCount > 0
    """)
    suspend fun getAverageSuccessRateInPeriod(patternId: Long, startDate: Long): Double?
    
    // Recent activity queries
    @Query("SELECT * FROM test_sessions ORDER BY date DESC LIMIT :limit")
    fun getRecentTestSessions(limit: Int): Flow<List<TestSession>>
    
    @Query("""
        SELECT DISTINCT patternId FROM test_sessions 
        WHERE date >= :startDate 
        ORDER BY date DESC
    """)
    suspend fun getRecentlyTestedPatternIds(startDate: Long): List<Long>
    
    // Cleanup operations
    @Query("DELETE FROM test_sessions WHERE patternId = :patternId")
    suspend fun deleteAllTestSessionsByPattern(patternId: Long)
    
    @Query("DELETE FROM test_sessions WHERE date < :cutoffDate")
    suspend fun deleteOldTestSessions(cutoffDate: Long)
    
    // Search operations
    @Query("SELECT * FROM test_sessions WHERE notes LIKE '%' || :searchQuery || '%' ORDER BY date DESC")
    fun searchTestSessionsByNotes(searchQuery: String): Flow<List<TestSession>>
    
    @Query("SELECT * FROM test_sessions WHERE videoPath IS NOT NULL ORDER BY date DESC")
    fun getTestSessionsWithVideo(): Flow<List<TestSession>>
}