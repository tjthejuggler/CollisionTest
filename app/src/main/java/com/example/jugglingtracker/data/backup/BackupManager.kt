package com.example.jugglingtracker.data.backup

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.example.jugglingtracker.data.database.JugglingDatabase
import com.example.jugglingtracker.data.entities.*
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages backup and restore operations for the juggling tracker app
 */
class BackupManager(
    private val context: Context,
    private val database: JugglingDatabase
) {
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    /**
     * Creates a comprehensive backup of all app data
     */
    suspend fun createBackup(
        outputFile: File,
        progressCallback: BackupProgressCallback? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            progressCallback?.onProgress(0, "Starting backup...")
            
            // Create temporary directory for backup files
            val tempDir = File(context.cacheDir, "backup_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            
            try {
                // Step 1: Export database (30% of progress)
                progressCallback?.onProgress(10, "Exporting database...")
                val databaseExport = exportDatabase()
                val databaseFile = File(tempDir, BackupMetadata.DATABASE_EXPORT_FILE_NAME)
                databaseFile.writeText(json.encodeToString(databaseExport))
                progressCallback?.onProgress(30, "Database exported")
                
                // Step 2: Copy video files (50% of progress)
                progressCallback?.onProgress(30, "Copying video files...")
                val videosDir = File(tempDir, BackupMetadata.VIDEOS_FOLDER_NAME)
                videosDir.mkdirs()
                val videoFileCount = copyVideoFiles(videosDir, databaseExport)
                progressCallback?.onProgress(70, "Video files copied")
                
                // Step 3: Create metadata (10% of progress)
                progressCallback?.onProgress(70, "Creating metadata...")
                val metadata = createMetadata(databaseExport, videoFileCount, tempDir)
                val metadataFile = File(tempDir, BackupMetadata.METADATA_FILE_NAME)
                metadataFile.writeText(json.encodeToString(metadata))
                progressCallback?.onProgress(80, "Metadata created")
                
                // Step 4: Create ZIP file (20% of progress)
                progressCallback?.onProgress(80, "Creating backup archive...")
                createZipFile(tempDir, outputFile)
                progressCallback?.onProgress(100, "Backup completed")
                
                progressCallback?.onComplete()
                BackupResult.Success(outputFile.absolutePath, metadata)
                
            } finally {
                // Clean up temporary directory
                tempDir.deleteRecursively()
            }
            
        } catch (e: Exception) {
            val errorMessage = "Failed to create backup: ${e.message}"
            progressCallback?.onError(errorMessage, e)
            BackupResult.Error(errorMessage, e)
        }
    }
    
    /**
     * Restores app data from a backup file
     */
    suspend fun restoreBackup(
        backupFile: File,
        progressCallback: BackupProgressCallback? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            progressCallback?.onProgress(0, "Starting restore...")
            
            // Create temporary directory for extracted files
            val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            
            try {
                // Step 1: Extract ZIP file (20% of progress)
                progressCallback?.onProgress(10, "Extracting backup archive...")
                extractZipFile(backupFile, tempDir)
                progressCallback?.onProgress(20, "Archive extracted")
                
                // Step 2: Validate backup (10% of progress)
                progressCallback?.onProgress(20, "Validating backup...")
                val metadata = validateBackup(tempDir)
                progressCallback?.onProgress(30, "Backup validated")
                
                // Step 3: Import database (50% of progress)
                progressCallback?.onProgress(30, "Restoring database...")
                val databaseFile = File(tempDir, BackupMetadata.DATABASE_EXPORT_FILE_NAME)
                val databaseExport = json.decodeFromString<DatabaseExport>(databaseFile.readText())
                val restoredItems = importDatabase(databaseExport, progressCallback)
                progressCallback?.onProgress(70, "Database restored")
                
                // Step 4: Restore video files (30% of progress)
                progressCallback?.onProgress(70, "Restoring video files...")
                val videosDir = File(tempDir, BackupMetadata.VIDEOS_FOLDER_NAME)
                val restoredVideoCount = restoreVideoFiles(videosDir)
                progressCallback?.onProgress(100, "Video files restored")
                
                val finalRestoredItems = restoredItems.copy(videoFiles = restoredVideoCount)
                progressCallback?.onComplete()
                RestoreResult.Success(finalRestoredItems)
                
            } finally {
                // Clean up temporary directory
                tempDir.deleteRecursively()
            }
            
        } catch (e: Exception) {
            val errorMessage = "Failed to restore backup: ${e.message}"
            progressCallback?.onError(errorMessage, e)
            RestoreResult.Error(errorMessage, e)
        }
    }
    
    /**
     * Validates a backup file and returns its metadata
     */
    suspend fun validateBackupFile(backupFile: File): BackupMetadata? = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.cacheDir, "validate_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            
            try {
                extractZipFile(backupFile, tempDir)
                validateBackup(tempDir)
            } finally {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun exportDatabase(): DatabaseExport {
        return DatabaseExport(
            patterns = database.patternDao().getAllPatternsForExport(),
            testSessions = database.testSessionDao().getAllTestSessionsForExport(),
            tags = database.tagDao().getAllTagsForExport(),
            patternTagCrossRefs = database.patternDao().getAllPatternTagCrossRefsForExport(),
            patternPrerequisiteCrossRefs = database.patternDao().getAllPatternPrerequisiteCrossRefsForExport(),
            patternDependentCrossRefs = database.patternDao().getAllPatternDependentCrossRefsForExport(),
            patternRelatedCrossRefs = database.patternDao().getAllPatternRelatedCrossRefsForExport(),
            usageEvents = database.usageEventDao().getAllUsageEventsForExport(),
            weeklyUsage = database.weeklyUsageDao().getAllWeeklyUsageForExport()
        )
    }
    
    private fun copyVideoFiles(videosDir: File, databaseExport: DatabaseExport): Int {
        var copiedCount = 0
        val appVideosDir = File(context.filesDir, "videos")
        
        // Collect all video paths from patterns and test sessions
        val videoPaths = mutableSetOf<String>()
        
        databaseExport.patterns.forEach { pattern ->
            pattern.videoUri?.let { videoPaths.add(it) }
        }
        
        databaseExport.testSessions.forEach { session ->
            session.videoPath?.let { videoPaths.add(it) }
        }
        
        // Copy each video file
        videoPaths.forEach { videoPath ->
            try {
                val sourceFile = File(appVideosDir, videoPath)
                if (sourceFile.exists()) {
                    val destFile = File(videosDir, videoPath)
                    destFile.parentFile?.mkdirs()
                    sourceFile.copyTo(destFile, overwrite = true)
                    copiedCount++
                }
            } catch (e: Exception) {
                // Log error but continue with other files
                e.printStackTrace()
            }
        }
        
        return copiedCount
    }
    
    private fun createMetadata(
        databaseExport: DatabaseExport,
        videoFileCount: Int,
        tempDir: File
    ): BackupMetadata {
        val appVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
        
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        val backupSize = calculateDirectorySize(tempDir)
        val checksum = calculateDirectoryChecksum(tempDir)
        
        return BackupMetadata(
            version = BackupMetadata.CURRENT_BACKUP_VERSION,
            appVersion = appVersion,
            databaseVersion = 2, // Current database version
            createdAt = System.currentTimeMillis(),
            deviceInfo = deviceInfo,
            totalPatterns = databaseExport.patterns.size,
            totalTestSessions = databaseExport.testSessions.size,
            totalTags = databaseExport.tags.size,
            totalUsageEvents = databaseExport.usageEvents.size,
            totalWeeklyUsage = databaseExport.weeklyUsage.size,
            totalVideoFiles = videoFileCount,
            backupSizeBytes = backupSize,
            checksum = checksum
        )
    }
    
    private fun createZipFile(sourceDir: File, outputFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = sourceDir.toURI().relativize(file.toURI()).path
                    val zipEntry = ZipEntry(relativePath)
                    zipOut.putNextEntry(zipEntry)
                    
                    BufferedInputStream(FileInputStream(file)).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
        }
    }
    
    private fun extractZipFile(zipFile: File, outputDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val outputFile = File(outputDir, entry.name)
                
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(outputFile)).use { output ->
                        zipIn.copyTo(output)
                    }
                }
                
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }
    
    private fun validateBackup(tempDir: File): BackupMetadata {
        val metadataFile = File(tempDir, BackupMetadata.METADATA_FILE_NAME)
        val databaseFile = File(tempDir, BackupMetadata.DATABASE_EXPORT_FILE_NAME)
        val videosDir = File(tempDir, BackupMetadata.VIDEOS_FOLDER_NAME)
        
        if (!metadataFile.exists()) {
            throw IllegalArgumentException("Invalid backup: metadata file missing")
        }
        
        if (!databaseFile.exists()) {
            throw IllegalArgumentException("Invalid backup: database export missing")
        }
        
        val metadata = json.decodeFromString<BackupMetadata>(metadataFile.readText())
        
        // Validate backup version compatibility
        if (metadata.version != BackupMetadata.CURRENT_BACKUP_VERSION) {
            throw IllegalArgumentException("Incompatible backup version: ${metadata.version}")
        }
        
        // Validate database export structure
        try {
            json.decodeFromString<DatabaseExport>(databaseFile.readText())
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid database export format", e)
        }
        
        return metadata
    }
    
    private suspend fun importDatabase(
        databaseExport: DatabaseExport,
        progressCallback: BackupProgressCallback?
    ): RestoredItems {
        // Clear existing data (with user confirmation in UI)
        database.clearAllTablesForRestore()
        
        var progress = 30
        val progressStep = 40 / 9 // 40% progress divided by 9 tables
        
        // Import in correct order to maintain foreign key relationships
        
        // 1. Tags (no dependencies)
        progressCallback?.onProgress(progress, "Importing tags...")
        database.tagDao().insertTagsFromBackup(databaseExport.tags.map { it.toEntity() })
        progress += progressStep
        
        // 2. Patterns (no dependencies)
        progressCallback?.onProgress(progress, "Importing patterns...")
        database.patternDao().insertPatternsFromBackup(databaseExport.patterns.map { it.toEntity() })
        progress += progressStep
        
        // 3. Test Sessions (depends on patterns)
        progressCallback?.onProgress(progress, "Importing test sessions...")
        database.testSessionDao().insertTestSessionsFromBackup(databaseExport.testSessions.map { it.toEntity() })
        progress += progressStep
        
        // 4. Pattern-Tag cross references
        progressCallback?.onProgress(progress, "Importing pattern-tag relationships...")
        database.patternDao().insertPatternTagCrossRefsFromBackup(databaseExport.patternTagCrossRefs.map { it.toEntity() })
        progress += progressStep
        
        // 5. Pattern prerequisite relationships
        progressCallback?.onProgress(progress, "Importing pattern prerequisites...")
        database.patternDao().insertPatternPrerequisiteCrossRefsFromBackup(databaseExport.patternPrerequisiteCrossRefs.map { it.toEntity() })
        progress += progressStep
        
        // 6. Pattern dependent relationships
        progressCallback?.onProgress(progress, "Importing pattern dependents...")
        database.patternDao().insertPatternDependentCrossRefsFromBackup(databaseExport.patternDependentCrossRefs.map { it.toEntity() })
        progress += progressStep
        
        // 7. Pattern related relationships
        progressCallback?.onProgress(progress, "Importing pattern relationships...")
        database.patternDao().insertPatternRelatedCrossRefsFromBackup(databaseExport.patternRelatedCrossRefs.map { it.toEntity() })
        progress += progressStep
        
        // 8. Usage events
        progressCallback?.onProgress(progress, "Importing usage events...")
        database.usageEventDao().insertUsageEventsFromBackup(databaseExport.usageEvents.map { it.toEntity() })
        progress += progressStep
        
        // 9. Weekly usage
        progressCallback?.onProgress(progress, "Importing weekly usage...")
        database.weeklyUsageDao().insertWeeklyUsageFromBackup(databaseExport.weeklyUsage.map { it.toEntity() })
        
        return RestoredItems(
            patterns = databaseExport.patterns.size,
            testSessions = databaseExport.testSessions.size,
            tags = databaseExport.tags.size,
            crossRefs = databaseExport.patternTagCrossRefs.size + 
                       databaseExport.patternPrerequisiteCrossRefs.size +
                       databaseExport.patternDependentCrossRefs.size +
                       databaseExport.patternRelatedCrossRefs.size,
            usageEvents = databaseExport.usageEvents.size,
            weeklyUsage = databaseExport.weeklyUsage.size,
            videoFiles = 0 // Will be set later
        )
    }
    
    private fun restoreVideoFiles(videosDir: File): Int {
        if (!videosDir.exists()) return 0
        
        val appVideosDir = File(context.filesDir, "videos")
        appVideosDir.mkdirs()
        
        var restoredCount = 0
        
        videosDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                try {
                    val relativePath = videosDir.toURI().relativize(file.toURI()).path
                    val destFile = File(appVideosDir, relativePath)
                    destFile.parentFile?.mkdirs()
                    file.copyTo(destFile, overwrite = true)
                    restoredCount++
                } catch (e: Exception) {
                    // Log error but continue with other files
                    e.printStackTrace()
                }
            }
        }
        
        return restoredCount
    }
    
    private fun calculateDirectorySize(dir: File): Long {
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
    
    private fun calculateDirectoryChecksum(dir: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        
        dir.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.absolutePath }
            .forEach { file ->
                md.update(file.readBytes())
            }
        
        return md.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Generates a default backup filename with timestamp
     */
    fun generateBackupFileName(): String {
        val timestamp = dateFormat.format(Date())
        return "juggling_backup_$timestamp.zip"
    }
}

// Extension functions to convert export data classes to entities
private fun PatternExport.toEntity() = Pattern(
    id = id,
    name = name,
    difficulty = difficulty,
    numBalls = numBalls,
    videoUri = videoUri,
    lastTested = lastTested
)

private fun TestSessionExport.toEntity() = TestSession(
    id = id,
    patternId = patternId,
    date = date,
    duration = duration,
    successCount = successCount,
    attemptCount = attemptCount,
    notes = notes,
    videoPath = videoPath
)

private fun TagExport.toEntity() = Tag(
    id = id,
    name = name,
    color = color
)

private fun PatternTagCrossRefExport.toEntity() = PatternTagCrossRef(patternId, tagId)
private fun PatternPrerequisiteCrossRefExport.toEntity() = PatternPrerequisiteCrossRef(patternId, prerequisiteId)
private fun PatternDependentCrossRefExport.toEntity() = PatternDependentCrossRef(patternId, dependentId)
private fun PatternRelatedCrossRefExport.toEntity() = PatternRelatedCrossRef(patternId, relatedId)

private fun UsageEventExport.toEntity() = UsageEvent(
    id = id,
    eventType = UsageEventType.valueOf(eventType),
    timestamp = timestamp,
    patternId = patternId,
    duration = duration?.toLong(),
    metadata = metadata
)

private fun WeeklyUsageExport.toEntity() = WeeklyUsage(
    id = id,
    weekStartTimestamp = weekStartTimestamp,
    totalPoints = totalPoints,
    patternsCreated = patternsCreated,
    testsCompleted = testsCompleted,
    totalTestDuration = totalTestDuration,
    videosRecorded = videosRecorded,
    appOpens = appOpens,
    lastUpdated = lastUpdated
)