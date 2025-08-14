package com.example.jugglingtracker.data.repository

import com.example.jugglingtracker.data.dao.TagDao
import com.example.jugglingtracker.data.dao.TagWithUsageCount
import com.example.jugglingtracker.data.entities.Tag
import com.example.jugglingtracker.data.entities.PatternTagCrossRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
class TagRepository(
    private val tagDao: TagDao
) {
    
    // Basic CRUD operations with error handling
    fun getAllTags(): Flow<Result<List<Tag>>> {
        return tagDao.getAllTags()
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun getTagById(id: Long): Result<Tag?> {
        return try {
            val tag = tagDao.getTagById(id)
            Result.success(tag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getTagByIdFlow(id: Long): Flow<Result<Tag?>> {
        return tagDao.getTagByIdFlow(id)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun getTagByName(name: String): Result<Tag?> {
        return try {
            val tag = tagDao.getTagByName(name)
            Result.success(tag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun insertTag(tag: Tag): Result<Long> {
        return try {
            val id = tagDao.insertTag(tag)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTag(tag: Tag): Result<Unit> {
        return try {
            tagDao.updateTag(tag)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTag(tag: Tag): Result<Unit> {
        return try {
            tagDao.deleteTag(tag)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTagById(id: Long): Result<Unit> {
        return try {
            tagDao.deleteTagById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Pattern-related tag operations
    fun getTagsByPattern(patternId: Long): Flow<Result<List<Tag>>> {
        return tagDao.getTagsByPattern(patternId)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun getTagsByPatternSync(patternId: Long): Result<List<Tag>> {
        return try {
            val tags = tagDao.getTagsByPatternSync(patternId)
            Result.success(tags)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Search and filter operations
    fun searchTags(searchQuery: String): Flow<Result<List<Tag>>> {
        return tagDao.searchTags(searchQuery)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getTagsByColor(color: Int): Flow<Result<List<Tag>>> {
        return tagDao.getTagsByColor(color)
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    // Usage statistics
    fun getTagsWithUsageCount(): Flow<Result<List<TagWithUsageCount>>> {
        return tagDao.getTagsWithUsageCount()
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    suspend fun getTagUsageCount(tagId: Long): Result<Int> {
        return try {
            val count = tagDao.getTagUsageCount(tagId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getUnusedTags(): Flow<Result<List<Tag>>> {
        return tagDao.getUnusedTags()
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    fun getMostUsedTags(): Flow<Result<List<Tag>>> {
        return tagDao.getMostUsedTags()
            .map { Result.success(it) }
            .catch { e -> emit(Result.failure(e)) }
    }
    
    // Tag management operations
    suspend fun getTagCount(): Result<Int> {
        return try {
            val count = tagDao.getTagCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isTagNameExists(name: String): Result<Boolean> {
        return try {
            val count = tagDao.getTagCountByName(name)
            Result.success(count > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllUsedColors(): Result<List<Int>> {
        return try {
            val colors = tagDao.getAllUsedColors()
            Result.success(colors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Bulk operations
    suspend fun insertTags(tags: List<Tag>): Result<List<Long>> {
        return try {
            val ids = tagDao.insertTags(tags)
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTags(tags: List<Tag>): Result<Unit> {
        return try {
            tagDao.deleteTags(tags)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Pattern-tag relationship management
    suspend fun addTagToPattern(patternId: Long, tagId: Long): Result<Unit> {
        return try {
            val crossRef = PatternTagCrossRef(patternId, tagId)
            tagDao.insertPatternTagCrossRef(crossRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addTagsToPattern(patternId: Long, tagIds: List<Long>): Result<Unit> {
        return try {
            val crossRefs = tagIds.map { PatternTagCrossRef(patternId, it) }
            tagDao.insertPatternTagCrossRefs(crossRefs)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeTagFromPattern(patternId: Long, tagId: Long): Result<Unit> {
        return try {
            val crossRef = PatternTagCrossRef(patternId, tagId)
            tagDao.deletePatternTagCrossRef(crossRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeAllTagsFromPattern(patternId: Long): Result<Unit> {
        return try {
            tagDao.deleteAllTagsFromPattern(patternId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeTagFromAllPatterns(tagId: Long): Result<Unit> {
        return try {
            tagDao.deleteTagFromAllPatterns(tagId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isPatternTagged(patternId: Long, tagId: Long): Result<Boolean> {
        return try {
            val isTagged = tagDao.isPatternTagged(patternId, tagId)
            Result.success(isTagged)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Cleanup operations
    suspend fun deleteUnusedTags(): Result<Unit> {
        return try {
            tagDao.deleteUnusedTags()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Convenience methods for creating and managing tags
    suspend fun createTag(name: String, color: Int): Result<Long> {
        // Check if tag with this name already exists
        val existingTagResult = getTagByName(name)
        return when {
            existingTagResult.isSuccess -> {
                if (existingTagResult.getOrNull() != null) {
                    Result.failure(IllegalArgumentException("Tag with name '$name' already exists"))
                } else {
                    val tag = Tag(name = name, color = color)
                    insertTag(tag)
                }
            }
            existingTagResult.isFailure -> {
                // If there was an error checking, try to create anyway
                val tag = Tag(name = name, color = color)
                insertTag(tag)
            }
            else -> {
                // This should never happen, but adding for exhaustiveness
                val tag = Tag(name = name, color = color)
                insertTag(tag)
            }
        }
    }
    
    suspend fun updateTagName(tagId: Long, newName: String): Result<Unit> {
        val tagResult = getTagById(tagId)
        return when {
            tagResult.isSuccess -> {
                val tag = tagResult.getOrNull()
                if (tag != null) {
                    val updatedTag = tag.copy(name = newName)
                    updateTag(updatedTag)
                } else {
                    Result.failure(IllegalArgumentException("Tag with id $tagId not found"))
                }
            }
            tagResult.isFailure -> Result.failure(tagResult.exceptionOrNull() ?: Exception("Unknown error"))
            else -> {
                // This should never happen, but adding for exhaustiveness
                Result.failure(IllegalArgumentException("Unexpected result state"))
            }
        }
    }
    
    suspend fun updateTagColor(tagId: Long, newColor: Int): Result<Unit> {
        val tagResult = getTagById(tagId)
        return when {
            tagResult.isSuccess -> {
                val tag = tagResult.getOrNull()
                if (tag != null) {
                    val updatedTag = tag.copy(color = newColor)
                    updateTag(updatedTag)
                } else {
                    Result.failure(IllegalArgumentException("Tag with id $tagId not found"))
                }
            }
            tagResult.isFailure -> Result.failure(tagResult.exceptionOrNull() ?: Exception("Unknown error"))
            else -> {
                // This should never happen, but adding for exhaustiveness
                Result.failure(IllegalArgumentException("Unexpected result state"))
            }
        }
    }
    
    // Helper method to get suggested colors (avoiding already used colors)
    suspend fun getSuggestedColors(availableColors: List<Int>): Result<List<Int>> {
        val usedColorsResult = getAllUsedColors()
        return when {
            usedColorsResult.isSuccess -> {
                val usedColors = usedColorsResult.getOrNull() ?: emptyList()
                val suggestedColors = availableColors.filter { it !in usedColors }
                Result.success(suggestedColors.ifEmpty { availableColors })
            }
            usedColorsResult.isFailure -> Result.failure(usedColorsResult.exceptionOrNull() ?: Exception("Unknown error"))
            else -> {
                // This should never happen, but adding for exhaustiveness
                Result.failure(IllegalArgumentException("Unexpected result state"))
            }
        }
    }
}