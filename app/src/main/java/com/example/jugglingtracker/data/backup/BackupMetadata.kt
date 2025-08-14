package com.example.jugglingtracker.data.backup

import kotlinx.serialization.Serializable
import java.util.Date

/**
 * Metadata for backup files containing information about the backup
 */
@Serializable
data class BackupMetadata(
    val version: String,
    val appVersion: String,
    val databaseVersion: Int,
    val createdAt: Long,
    val deviceInfo: String,
    val totalPatterns: Int,
    val totalTestSessions: Int,
    val totalTags: Int,
    val totalUsageEvents: Int,
    val totalWeeklyUsage: Int,
    val totalVideoFiles: Int,
    val backupSizeBytes: Long,
    val checksum: String
) {
    companion object {
        const val CURRENT_BACKUP_VERSION = "1.0"
        const val METADATA_FILE_NAME = "backup_metadata.json"
        const val DATABASE_EXPORT_FILE_NAME = "database_export.json"
        const val VIDEOS_FOLDER_NAME = "videos"
    }
}

/**
 * Database export structure containing all tables data
 */
@Serializable
data class DatabaseExport(
    val patterns: List<PatternExport>,
    val testSessions: List<TestSessionExport>,
    val tags: List<TagExport>,
    val patternTagCrossRefs: List<PatternTagCrossRefExport>,
    val patternPrerequisiteCrossRefs: List<PatternPrerequisiteCrossRefExport>,
    val patternDependentCrossRefs: List<PatternDependentCrossRefExport>,
    val patternRelatedCrossRefs: List<PatternRelatedCrossRefExport>,
    val usageEvents: List<UsageEventExport>,
    val weeklyUsage: List<WeeklyUsageExport>
)

@Serializable
data class PatternExport(
    val id: Long,
    val name: String,
    val difficulty: Int,
    val numBalls: Int,
    val videoUri: String?,
    val lastTested: Long?
)

@Serializable
data class TestSessionExport(
    val id: Long,
    val patternId: Long,
    val date: Long,
    val duration: Long,
    val successCount: Int,
    val attemptCount: Int,
    val notes: String?,
    val videoPath: String?
)

@Serializable
data class TagExport(
    val id: Long,
    val name: String,
    val color: Int
)

@Serializable
data class PatternTagCrossRefExport(
    val patternId: Long,
    val tagId: Long
)

@Serializable
data class PatternPrerequisiteCrossRefExport(
    val patternId: Long,
    val prerequisiteId: Long
)

@Serializable
data class PatternDependentCrossRefExport(
    val patternId: Long,
    val dependentId: Long
)

@Serializable
data class PatternRelatedCrossRefExport(
    val patternId: Long,
    val relatedId: Long
)

@Serializable
data class UsageEventExport(
    val id: Long,
    val eventType: String,
    val timestamp: Long,
    val patternId: Long?,
    val duration: Int?,
    val metadata: String?
)

@Serializable
data class WeeklyUsageExport(
    val id: Long,
    val weekStartTimestamp: Long,
    val totalPoints: Int,
    val patternsCreated: Int,
    val testsCompleted: Int,
    val totalTestDuration: Long,
    val videosRecorded: Int,
    val appOpens: Int,
    val lastUpdated: Long
)

/**
 * Result of backup operation
 */
sealed class BackupResult {
    data class Success(val filePath: String, val metadata: BackupMetadata) : BackupResult()
    data class Error(val message: String, val exception: Throwable? = null) : BackupResult()
}

/**
 * Result of restore operation
 */
sealed class RestoreResult {
    data class Success(val restoredItems: RestoredItems) : RestoreResult()
    data class Error(val message: String, val exception: Throwable? = null) : RestoreResult()
}

/**
 * Information about restored items
 */
data class RestoredItems(
    val patterns: Int,
    val testSessions: Int,
    val tags: Int,
    val crossRefs: Int,
    val usageEvents: Int,
    val weeklyUsage: Int,
    val videoFiles: Int
)

/**
 * Progress callback for backup/restore operations
 */
interface BackupProgressCallback {
    fun onProgress(progress: Int, message: String)
    fun onComplete()
    fun onError(message: String, exception: Throwable? = null)
}