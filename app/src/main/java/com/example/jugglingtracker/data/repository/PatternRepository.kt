package com.example.jugglingtracker.data.repository

import com.example.jugglingtracker.data.dao.PatternDao
import com.example.jugglingtracker.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
class PatternRepository(
    private val patternDao: PatternDao
) {
    
    // Basic CRUD operations with error handling
    fun getAllPatterns(): Flow<Result<List<Pattern>>> {
        return patternDao.getAllPatterns()
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun getPatternById(id: Long): Result<Pattern?> {
        return try {
            val pattern = patternDao.getPatternById(id)
            Result.success(pattern)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getPatternByIdFlow(id: Long): Flow<Result<Pattern?>> {
        return patternDao.getPatternByIdFlow(id)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun insertPattern(pattern: Pattern): Result<Long> {
        return try {
            val id = patternDao.insertPattern(pattern)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePattern(pattern: Pattern): Result<Unit> {
        return try {
            patternDao.updatePattern(pattern)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePattern(pattern: Pattern): Result<Unit> {
        return try {
            patternDao.deletePattern(pattern)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePatternById(id: Long): Result<Unit> {
        return try {
            patternDao.deletePatternById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Complex queries with relationships
    suspend fun getPatternWithRelationships(id: Long): Result<PatternEntity?> {
        return try {
            val patternEntity = patternDao.getPatternWithRelationships(id)
            Result.success(patternEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getPatternWithRelationshipsFlow(id: Long): Flow<Result<PatternEntity?>> {
        return patternDao.getPatternWithRelationshipsFlow(id)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getAllPatternsWithRelationships(): Flow<Result<List<PatternEntity>>> {
        return patternDao.getAllPatternsWithRelationships()
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    // Sorting and filtering
    fun getPatternsByDifficulty(ascending: Boolean = true): Flow<Result<List<Pattern>>> {
        val flow = if (ascending) {
            patternDao.getPatternsByDifficultyAsc()
        } else {
            patternDao.getPatternsByDifficultyDesc()
        }
        return flow
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getPatternsByDifficultyRange(minDifficulty: Int, maxDifficulty: Int): Flow<Result<List<Pattern>>> {
        return patternDao.getPatternsByDifficultyRange(minDifficulty, maxDifficulty)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getPatternsByNumBalls(numBalls: Int): Flow<Result<List<Pattern>>> {
        return patternDao.getPatternsByNumBalls(numBalls)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun searchPatterns(searchQuery: String): Flow<Result<List<Pattern>>> {
        return patternDao.searchPatterns(searchQuery)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getTestedPatterns(): Flow<Result<List<Pattern>>> {
        return patternDao.getTestedPatterns()
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getUntestedPatterns(): Flow<Result<List<Pattern>>> {
        return patternDao.getUntestedPatterns()
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    // Tag-related operations
    fun getPatternsByTag(tagId: Long): Flow<Result<List<Pattern>>> {
        return patternDao.getPatternsByTag(tagId)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getPatternsByTagName(tagName: String): Flow<Result<List<Pattern>>> {
        return patternDao.getPatternsByTagName(tagName)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun addTagToPattern(patternId: Long, tagId: Long): Result<Unit> {
        return try {
            val crossRef = PatternTagCrossRef(patternId, tagId)
            patternDao.insertPatternTagCrossRef(crossRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeTagFromPattern(patternId: Long, tagId: Long): Result<Unit> {
        return try {
            val crossRef = PatternTagCrossRef(patternId, tagId)
            patternDao.deletePatternTagCrossRef(crossRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Relationship management
    fun getPrerequisites(patternId: Long): Flow<Result<List<Pattern>>> {
        return patternDao.getPrerequisites(patternId)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getDependents(patternId: Long): Flow<Result<List<Pattern>>> {
        return patternDao.getDependents(patternId)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getRelatedPatterns(patternId: Long): Flow<Result<List<Pattern>>> {
        return patternDao.getRelatedPatterns(patternId)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun addPrerequisite(patternId: Long, prerequisiteId: Long): Result<Unit> {
        return try {
            val crossRef = PatternPrerequisiteCrossRef(patternId, prerequisiteId)
            patternDao.insertPatternPrerequisiteCrossRef(crossRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removePrerequisite(patternId: Long, prerequisiteId: Long): Result<Unit> {
        return try {
            val crossRef = PatternPrerequisiteCrossRef(patternId, prerequisiteId)
            patternDao.deletePatternPrerequisiteCrossRef(crossRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addDependent(patternId: Long, dependentId: Long): Result<Unit> {
        return try {
            val crossRef = PatternDependentCrossRef(patternId, dependentId)
            patternDao.insertPatternDependentCrossRef(crossRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeDependent(patternId: Long, dependentId: Long): Result<Unit> {
        return try {
            val crossRef = PatternDependentCrossRef(patternId, dependentId)
            patternDao.deletePatternDependentCrossRef(crossRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addRelatedPattern(patternId: Long, relatedId: Long): Result<Unit> {
        return try {
            val crossRef = PatternRelatedCrossRef(patternId, relatedId)
            patternDao.insertPatternRelatedCrossRef(crossRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeRelatedPattern(patternId: Long, relatedId: Long): Result<Unit> {
        return try {
            val crossRef = PatternRelatedCrossRef(patternId, relatedId)
            patternDao.deletePatternRelatedCrossRef(crossRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update operations
    suspend fun updateLastTested(patternId: Long, timestamp: Long): Result<Unit> {
        return try {
            patternDao.updateLastTested(patternId, timestamp)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Statistics
    suspend fun getTotalPatternCount(): Result<Int> {
        return try {
            val count = patternDao.getTotalPatternCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTestedPatternCount(): Result<Int> {
        return try {
            val count = patternDao.getTestedPatternCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAverageDifficulty(): Result<Double?> {
        return try {
            val average = patternDao.getAverageDifficulty()
            Result.success(average)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}