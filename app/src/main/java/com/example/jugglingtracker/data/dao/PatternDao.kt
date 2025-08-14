package com.example.jugglingtracker.data.dao

import androidx.room.*
import com.example.jugglingtracker.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM patterns ORDER BY name ASC")
    fun getAllPatterns(): Flow<List<Pattern>>
    
    @Query("SELECT * FROM patterns WHERE id = :id")
    suspend fun getPatternById(id: Long): Pattern?
    
    @Query("SELECT * FROM patterns WHERE id = :id")
    fun getPatternByIdFlow(id: Long): Flow<Pattern?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: Pattern): Long
    
    @Update
    suspend fun updatePattern(pattern: Pattern)
    
    @Delete
    suspend fun deletePattern(pattern: Pattern)
    
    @Query("DELETE FROM patterns WHERE id = :id")
    suspend fun deletePatternById(id: Long)
    
    // Complex queries with relationships
    @Transaction
    @Query("SELECT * FROM patterns WHERE id = :id")
    suspend fun getPatternWithRelationships(id: Long): PatternEntity?
    
    @Transaction
    @Query("SELECT * FROM patterns WHERE id = :id")
    fun getPatternWithRelationshipsFlow(id: Long): Flow<PatternEntity?>
    
    @Transaction
    @Query("SELECT * FROM patterns ORDER BY name ASC")
    fun getAllPatternsWithRelationships(): Flow<List<PatternEntity>>
    
    // Sorting and filtering queries
    @Query("SELECT * FROM patterns ORDER BY difficulty ASC")
    fun getPatternsByDifficultyAsc(): Flow<List<Pattern>>
    
    @Query("SELECT * FROM patterns ORDER BY difficulty DESC")
    fun getPatternsByDifficultyDesc(): Flow<List<Pattern>>
    
    @Query("SELECT * FROM patterns WHERE difficulty BETWEEN :minDifficulty AND :maxDifficulty ORDER BY name ASC")
    fun getPatternsByDifficultyRange(minDifficulty: Int, maxDifficulty: Int): Flow<List<Pattern>>
    
    @Query("SELECT * FROM patterns WHERE numBalls = :numBalls ORDER BY name ASC")
    fun getPatternsByNumBalls(numBalls: Int): Flow<List<Pattern>>
    
    @Query("SELECT * FROM patterns WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun searchPatterns(searchQuery: String): Flow<List<Pattern>>
    
    @Query("SELECT * FROM patterns WHERE lastTested IS NOT NULL ORDER BY lastTested DESC")
    fun getTestedPatterns(): Flow<List<Pattern>>
    
    @Query("SELECT * FROM patterns WHERE lastTested IS NULL ORDER BY name ASC")
    fun getUntestedPatterns(): Flow<List<Pattern>>
    
    // Tag-related queries
    @Query("""
        SELECT p.* FROM patterns p
        INNER JOIN pattern_tag_cross_ref ptcr ON p.id = ptcr.patternId
        WHERE ptcr.tagId = :tagId
        ORDER BY p.name ASC
    """)
    fun getPatternsByTag(tagId: Long): Flow<List<Pattern>>
    
    @Query("""
        SELECT p.* FROM patterns p
        INNER JOIN pattern_tag_cross_ref ptcr ON p.id = ptcr.patternId
        INNER JOIN tags t ON ptcr.tagId = t.id
        WHERE t.name = :tagName
        ORDER BY p.name ASC
    """)
    fun getPatternsByTagName(tagName: String): Flow<List<Pattern>>
    
    // Prerequisite and dependency queries
    @Query("""
        SELECT p.* FROM patterns p
        INNER JOIN pattern_prerequisite_cross_ref ppcr ON p.id = ppcr.prerequisiteId
        WHERE ppcr.patternId = :patternId
        ORDER BY p.name ASC
    """)
    fun getPrerequisites(patternId: Long): Flow<List<Pattern>>
    
    @Query("""
        SELECT p.* FROM patterns p
        INNER JOIN pattern_dependent_cross_ref pdcr ON p.id = pdcr.dependentId
        WHERE pdcr.patternId = :patternId
        ORDER BY p.name ASC
    """)
    fun getDependents(patternId: Long): Flow<List<Pattern>>
    
    @Query("""
        SELECT p.* FROM patterns p
        INNER JOIN pattern_related_cross_ref prcr ON p.id = prcr.relatedId
        WHERE prcr.patternId = :patternId
        ORDER BY p.name ASC
    """)
    fun getRelatedPatterns(patternId: Long): Flow<List<Pattern>>
    
    // Cross-reference table operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatternTagCrossRef(crossRef: PatternTagCrossRef)
    
    @Delete
    suspend fun deletePatternTagCrossRef(crossRef: PatternTagCrossRef)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatternPrerequisiteCrossRef(crossRef: PatternPrerequisiteCrossRef)
    
    @Delete
    suspend fun deletePatternPrerequisiteCrossRef(crossRef: PatternPrerequisiteCrossRef)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatternDependentCrossRef(crossRef: PatternDependentCrossRef)
    
    @Delete
    suspend fun deletePatternDependentCrossRef(crossRef: PatternDependentCrossRef)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatternRelatedCrossRef(crossRef: PatternRelatedCrossRef)
    
    @Delete
    suspend fun deletePatternRelatedCrossRef(crossRef: PatternRelatedCrossRef)
    
    // Update last tested timestamp
    @Query("UPDATE patterns SET lastTested = :timestamp WHERE id = :patternId")
    suspend fun updateLastTested(patternId: Long, timestamp: Long)
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM patterns")
    suspend fun getTotalPatternCount(): Int
    
    @Query("SELECT COUNT(*) FROM patterns WHERE lastTested IS NOT NULL")
    suspend fun getTestedPatternCount(): Int
    
    @Query("SELECT AVG(difficulty) FROM patterns")
    suspend fun getAverageDifficulty(): Double?
}