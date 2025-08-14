package com.example.jugglingtracker.data.repository

import android.content.Context
import com.example.jugglingtracker.data.backup.*
import com.example.jugglingtracker.data.database.JugglingDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for backup and restore operations
 */
@Singleton
class BackupRepository @Inject constructor(
    private val context: Context,
    private val database: JugglingDatabase
) {
    private val backupManager = BackupManager(context, database)
    
    /**
     * Creates a backup of all app data
     */
    suspend fun createBackup(
        outputFile: File,
        progressCallback: BackupProgressCallback? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        backupManager.createBackup(outputFile, progressCallback)
    }
    
    /**
     * Restores app data from a backup file
     */
    suspend fun restoreBackup(
        backupFile: File,
        progressCallback: BackupProgressCallback? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        backupManager.restoreBackup(backupFile, progressCallback)
    }
    
    /**
     * Validates a backup file and returns its metadata
     */
    suspend fun validateBackupFile(backupFile: File): BackupMetadata? = withContext(Dispatchers.IO) {
        backupManager.validateBackupFile(backupFile)
    }
    
    /**
     * Generates a default backup filename with timestamp
     */
    fun generateBackupFileName(): String {
        return backupManager.generateBackupFileName()
    }
    
    /**
     * Gets the default backup directory
     */
    fun getDefaultBackupDirectory(): File {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir
    }
    
    /**
     * Gets available backup files in the default directory
     */
    suspend fun getAvailableBackups(): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        val backupDir = getDefaultBackupDirectory()
        val backupFiles = backupDir.listFiles { file ->
            file.isFile && file.name.endsWith(".zip")
        } ?: emptyArray()
        
        backupFiles.mapNotNull { file ->
            try {
                val metadata = backupManager.validateBackupFile(file)
                metadata?.let {
                    BackupFileInfo(
                        file = file,
                        metadata = it,
                        sizeBytes = file.length()
                    )
                }
            } catch (e: Exception) {
                null // Invalid backup file
            }
        }.sortedByDescending { it.metadata.createdAt }
    }
    
    /**
     * Deletes a backup file
     */
    suspend fun deleteBackup(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            backupFile.delete()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets the total size of all backup files
     */
    suspend fun getTotalBackupSize(): Long = withContext(Dispatchers.IO) {
        val backupDir = getDefaultBackupDirectory()
        backupDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    /**
     * Cleans up old backup files, keeping only the most recent ones
     */
    suspend fun cleanupOldBackups(keepCount: Int = 5): Int = withContext(Dispatchers.IO) {
        val backups = getAvailableBackups()
        val toDelete = backups.drop(keepCount)
        
        var deletedCount = 0
        toDelete.forEach { backupInfo ->
            if (deleteBackup(backupInfo.file)) {
                deletedCount++
            }
        }
        
        deletedCount
    }
}

/**
 * Information about a backup file
 */
data class BackupFileInfo(
    val file: File,
    val metadata: BackupMetadata,
    val sizeBytes: Long
) {
    val name: String get() = file.name
    val formattedSize: String get() = formatFileSize(sizeBytes)
    
    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> "%.1f GB".format(gb)
            mb >= 1 -> "%.1f MB".format(mb)
            kb >= 1 -> "%.1f KB".format(kb)
            else -> "$bytes B"
        }
    }
}