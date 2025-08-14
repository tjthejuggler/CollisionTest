package com.example.jugglingtracker.data.dao

import androidx.room.*
import com.example.jugglingtracker.data.entities.Tag
import com.example.jugglingtracker.data.entities.PatternTagCrossRef
import com.example.jugglingtracker.data.backup.TagExport
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>
    
    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): Tag?
    
    @Query("SELECT * FROM tags WHERE id = :id")
    fun getTagByIdFlow(id: Long): Flow<Tag?>
    
    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getTagByName(name: String): Tag?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long
    
    @Update
    suspend fun updateTag(tag: Tag)
    
    @Delete
    suspend fun deleteTag(tag: Tag)
    
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagById(id: Long)
    
    // Pattern-related tag queries
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN pattern_tag_cross_ref ptcr ON t.id = ptcr.tagId
        WHERE ptcr.patternId = :patternId
        ORDER BY t.name ASC
    """)
    fun getTagsByPattern(patternId: Long): Flow<List<Tag>>
    
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN pattern_tag_cross_ref ptcr ON t.id = ptcr.tagId
        WHERE ptcr.patternId = :patternId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsByPatternSync(patternId: Long): List<Tag>
    
    // Search and filter operations
    @Query("SELECT * FROM tags WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun searchTags(searchQuery: String): Flow<List<Tag>>
    
    @Query("SELECT * FROM tags WHERE color = :color ORDER BY name ASC")
    fun getTagsByColor(color: Int): Flow<List<Tag>>
    
    // Usage statistics
    @Query("""
        SELECT t.*, COUNT(ptcr.patternId) as usage_count
        FROM tags t
        LEFT JOIN pattern_tag_cross_ref ptcr ON t.id = ptcr.tagId
        GROUP BY t.id
        ORDER BY usage_count DESC, t.name ASC
    """)
    fun getTagsWithUsageCount(): Flow<List<TagWithUsageCount>>
    
    @Query("""
        SELECT COUNT(ptcr.patternId) 
        FROM pattern_tag_cross_ref ptcr 
        WHERE ptcr.tagId = :tagId
    """)
    suspend fun getTagUsageCount(tagId: Long): Int
    
    @Query("""
        SELECT t.* FROM tags t
        LEFT JOIN pattern_tag_cross_ref ptcr ON t.id = ptcr.tagId
        WHERE ptcr.tagId IS NULL
        ORDER BY t.name ASC
    """)
    fun getUnusedTags(): Flow<List<Tag>>
    
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN pattern_tag_cross_ref ptcr ON t.id = ptcr.tagId
        GROUP BY t.id
        ORDER BY COUNT(ptcr.patternId) DESC, t.name ASC
    """)
    fun getMostUsedTags(): Flow<List<Tag>>
    
    // Tag management operations
    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getTagCount(): Int
    
    @Query("SELECT COUNT(*) FROM tags WHERE name = :name")
    suspend fun getTagCountByName(name: String): Int
    
    @Query("SELECT DISTINCT color FROM tags ORDER BY color")
    suspend fun getAllUsedColors(): List<Int>
    
    // Bulk operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<Tag>): List<Long>
    
    @Delete
    suspend fun deleteTags(tags: List<Tag>)
    
    // Cross-reference operations (for pattern-tag relationships)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatternTagCrossRef(crossRef: PatternTagCrossRef)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatternTagCrossRefs(crossRefs: List<PatternTagCrossRef>)
    
    @Delete
    suspend fun deletePatternTagCrossRef(crossRef: PatternTagCrossRef)
    
    @Query("DELETE FROM pattern_tag_cross_ref WHERE patternId = :patternId")
    suspend fun deleteAllTagsFromPattern(patternId: Long)
    
    @Query("DELETE FROM pattern_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteTagFromAllPatterns(tagId: Long)
    
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM pattern_tag_cross_ref 
            WHERE patternId = :patternId AND tagId = :tagId
        )
    """)
    suspend fun isPatternTagged(patternId: Long, tagId: Long): Boolean
    
    // Cleanup operations
    @Query("""
        DELETE FROM tags
        WHERE id NOT IN (
            SELECT DISTINCT tagId FROM pattern_tag_cross_ref
        )
    """)
    suspend fun deleteUnusedTags()
    
    // Backup and restore methods
    @Query("SELECT id, name, color FROM tags ORDER BY id ASC")
    suspend fun getAllTagsForExport(): List<TagExport>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagsFromBackup(tags: List<Tag>)
}

// Data class for tag usage statistics
data class TagWithUsageCount(
    @Embedded val tag: Tag,
    val usage_count: Int
)