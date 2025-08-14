package com.example.jugglingtracker.data.repository

import com.example.jugglingtracker.data.dao.TestSessionDao
import com.example.jugglingtracker.data.entities.TestSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
class TestSessionRepository(
    private val testSessionDao: TestSessionDao
) {
    
    // Basic CRUD operations with error handling
    fun getAllTestSessions(): Flow<Result<List<TestSession>>> {
        return testSessionDao.getAllTestSessions()
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun getTestSessionById(id: Long): Result<TestSession?> {
        return try {
            val testSession = testSessionDao.getTestSessionById(id)
            Result.success(testSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getTestSessionByIdFlow(id: Long): Flow<Result<TestSession?>> {
        return testSessionDao.getTestSessionByIdFlow(id)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun insertTestSession(testSession: TestSession): Result<Long> {
        return try {
            val id = testSessionDao.insertTestSession(testSession)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTestSession(testSession: TestSession): Result<Unit> {
        return try {
            testSessionDao.updateTestSession(testSession)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTestSession(testSession: TestSession): Result<Unit> {
        return try {
            testSessionDao.deleteTestSession(testSession)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTestSessionById(id: Long): Result<Unit> {
        return try {
            testSessionDao.deleteTestSessionById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Pattern-specific operations
    fun getTestSessionsByPattern(patternId: Long): Flow<Result<List<TestSession>>> {
        return testSessionDao.getTestSessionsByPattern(patternId)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getRecentTestSessionsByPattern(patternId: Long, limit: Int = 10): Flow<Result<List<TestSession>>> {
        return testSessionDao.getRecentTestSessionsByPattern(patternId, limit)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun getLatestTestSessionByPattern(patternId: Long): Result<TestSession?> {
        return try {
            val testSession = testSessionDao.getLatestTestSessionByPattern(patternId)
            Result.success(testSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFirstTestSessionByPattern(patternId: Long): Result<TestSession?> {
        return try {
            val testSession = testSessionDao.getFirstTestSessionByPattern(patternId)
            Result.success(testSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Date-based queries
    fun getTestSessionsByDateRange(startDate: Long, endDate: Long): Flow<Result<List<TestSession>>> {
        return testSessionDao.getTestSessionsByDateRange(startDate, endDate)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getTestSessionsSince(startDate: Long): Flow<Result<List<TestSession>>> {
        return testSessionDao.getTestSessionsSince(startDate)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getTodaysTestSessions(todayStart: Long, tomorrowStart: Long): Flow<Result<List<TestSession>>> {
        return testSessionDao.getTodaysTestSessions(todayStart, tomorrowStart)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    // Performance-based queries
    fun getTestSessionsByMinSuccessRate(minSuccessRate: Double): Flow<Result<List<TestSession>>> {
        return testSessionDao.getTestSessionsByMinSuccessRate(minSuccessRate)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getTestSessionsByMinDuration(minDuration: Long): Flow<Result<List<TestSession>>> {
        return testSessionDao.getTestSessionsByMinDuration(minDuration)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getTestSessionsByMinAttempts(minAttempts: Int): Flow<Result<List<TestSession>>> {
        return testSessionDao.getTestSessionsByMinAttempts(minAttempts)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    // Statistics operations
    suspend fun getTotalTestSessionCount(): Result<Int> {
        return try {
            val count = testSessionDao.getTotalTestSessionCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTestSessionCountByPattern(patternId: Long): Result<Int> {
        return try {
            val count = testSessionDao.getTestSessionCountByPattern(patternId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTotalPracticeTimeByPattern(patternId: Long): Result<Long> {
        return try {
            val totalTime = testSessionDao.getTotalPracticeTimeByPattern(patternId) ?: 0L
            Result.success(totalTime)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAverageSuccessRateByPattern(patternId: Long): Result<Double> {
        return try {
            val avgSuccessRate = testSessionDao.getAverageSuccessRateByPattern(patternId) ?: 0.0
            Result.success(avgSuccessRate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTotalSuccessCountByPattern(patternId: Long): Result<Int> {
        return try {
            val totalSuccess = testSessionDao.getTotalSuccessCountByPattern(patternId) ?: 0
            Result.success(totalSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTotalAttemptCountByPattern(patternId: Long): Result<Int> {
        return try {
            val totalAttempts = testSessionDao.getTotalAttemptCountByPattern(patternId) ?: 0
            Result.success(totalAttempts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAverageDurationByPattern(patternId: Long): Result<Double> {
        return try {
            val avgDuration = testSessionDao.getAverageDurationByPattern(patternId) ?: 0.0
            Result.success(avgDuration)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Progress tracking
    fun getTestSessionProgressByPattern(patternId: Long): Flow<Result<List<TestSession>>> {
        return testSessionDao.getTestSessionProgressByPattern(patternId)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun getAverageSuccessRateInPeriod(patternId: Long, startDate: Long): Result<Double> {
        return try {
            val avgSuccessRate = testSessionDao.getAverageSuccessRateInPeriod(patternId, startDate) ?: 0.0
            Result.success(avgSuccessRate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Recent activity
    fun getRecentTestSessions(limit: Int = 20): Flow<Result<List<TestSession>>> {
        return testSessionDao.getRecentTestSessions(limit)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun getRecentlyTestedPatternIds(startDate: Long): Result<List<Long>> {
        return try {
            val patternIds = testSessionDao.getRecentlyTestedPatternIds(startDate)
            Result.success(patternIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Cleanup operations
    suspend fun deleteAllTestSessionsByPattern(patternId: Long): Result<Unit> {
        return try {
            testSessionDao.deleteAllTestSessionsByPattern(patternId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteOldTestSessions(cutoffDate: Long): Result<Unit> {
        return try {
            testSessionDao.deleteOldTestSessions(cutoffDate)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Search operations
    fun searchTestSessionsByNotes(searchQuery: String): Flow<Result<List<TestSession>>> {
        return testSessionDao.searchTestSessionsByNotes(searchQuery)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getTestSessionsWithVideo(): Flow<Result<List<TestSession>>> {
        return testSessionDao.getTestSessionsWithVideo()
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    // Convenience methods for creating test sessions
    suspend fun createTestSession(
        patternId: Long,
        date: Long = System.currentTimeMillis(),
        duration: Long,
        successCount: Int,
        attemptCount: Int,
        notes: String? = null,
        videoPath: String? = null
    ): Result<Long> {
        val testSession = TestSession(
            patternId = patternId,
            date = date,
            duration = duration,
            successCount = successCount,
            attemptCount = attemptCount,
            notes = notes,
            videoPath = videoPath
        )
        return insertTestSession(testSession)
    }
    
    // Calculate success rate for a test session
    fun calculateSuccessRate(successCount: Int, attemptCount: Int): Double {
        return if (attemptCount > 0) {
            (successCount.toDouble() / attemptCount.toDouble()) * 100.0
        } else {
            0.0
        }
    }
}