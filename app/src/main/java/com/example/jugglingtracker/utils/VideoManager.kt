package com.example.jugglingtracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for managing video operations including storage, thumbnails, and file management.
 */
class VideoManager(private val context: Context) {

    companion object {
        private const val VIDEO_DIRECTORY = "pattern_videos"
        private const val THUMBNAIL_DIRECTORY = "video_thumbnails"
        private const val VIDEO_EXTENSION = ".mp4"
        private const val THUMBNAIL_EXTENSION = ".jpg"
        private const val THUMBNAIL_QUALITY = 85
        private const val THUMBNAIL_WIDTH = 320
        private const val THUMBNAIL_HEIGHT = 240
    }

    private val videoDirectory: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), VIDEO_DIRECTORY).apply {
            if (!exists()) mkdirs()
        }
    }

    private val thumbnailDirectory: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), THUMBNAIL_DIRECTORY).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Generate a unique filename for a video
     */
    fun generateVideoFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "pattern_video_${timestamp}$VIDEO_EXTENSION"
    }

    /**
     * Generate a unique filename for a thumbnail
     */
    fun generateThumbnailFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "pattern_thumbnail_${timestamp}$THUMBNAIL_EXTENSION"
    }


    /**
     * Create a new video file
     */
    fun createVideoFile(fileName: String? = null): File {
        val name = fileName ?: generateVideoFileName()
        return File(videoDirectory, name)
    }

    /**
     * Create a new thumbnail file
     */
    fun createThumbnailFile(fileName: String? = null): File {
        val name = fileName ?: generateThumbnailFileName()
        return File(thumbnailDirectory, name)
    }

    /**
     * Copy a video from external source to app storage
     */
    suspend fun copyVideoToAppStorage(sourceUri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destinationFile = createVideoFile()
            
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate a thumbnail from a video file
     */
    suspend fun generateThumbnail(videoFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val thumbnailFile = createThumbnailFile()
            val retriever = MediaMetadataRetriever()
            
            retriever.use {
                it.setDataSource(videoFile.absolutePath)
                val bitmap = it.getFrameAtTime(1000000) // Get frame at 1 second
                
                bitmap?.let { bmp ->
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        bmp, 
                        THUMBNAIL_WIDTH, 
                        THUMBNAIL_HEIGHT, 
                        true
                    )
                    
                    FileOutputStream(thumbnailFile).use { outputStream ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outputStream)
                    }
                    
                    scaledBitmap.recycle()
                    bmp.recycle()
                }
            }
            
            Result.success(thumbnailFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate a thumbnail from a video URI
     */
    suspend fun generateThumbnail(videoUri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val thumbnailFile = createThumbnailFile()
            val retriever = MediaMetadataRetriever()
            
            retriever.use {
                it.setDataSource(context, videoUri)
                val bitmap = it.getFrameAtTime(1000000) // Get frame at 1 second
                
                bitmap?.let { bmp ->
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        bmp, 
                        THUMBNAIL_WIDTH, 
                        THUMBNAIL_HEIGHT, 
                        true
                    )
                    
                    FileOutputStream(thumbnailFile).use { outputStream ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outputStream)
                    }
                    
                    scaledBitmap.recycle()
                    bmp.recycle()
                }
            }
            
            Result.success(thumbnailFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get video duration in milliseconds
     */
    suspend fun getVideoDuration(videoFile: File): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.use {
                it.setDataSource(videoFile.absolutePath)
                val duration = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                Result.success(duration)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get video duration from URI
     */
    suspend fun getVideoDuration(videoUri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.use {
                it.setDataSource(context, videoUri)
                val duration = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                Result.success(duration)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get video file size in bytes
     */
    fun getVideoFileSize(videoFile: File): Long {
        return if (videoFile.exists()) videoFile.length() else 0L
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Format duration for display
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * Delete a video file and its associated thumbnail
     */
    suspend fun deleteVideo(videoFile: File, thumbnailFile: File? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var deleted = true
            
            if (videoFile.exists()) {
                deleted = videoFile.delete() && deleted
            }
            
            thumbnailFile?.let { thumbnail ->
                if (thumbnail.exists()) {
                    deleted = thumbnail.delete() && deleted
                }
            }
            
            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete video files"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a shareable URI for a video file using FileProvider
     */
    fun getShareableUri(videoFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            videoFile
        )
    }

    /**
     * Check if a video file exists
     */
    fun videoExists(videoFile: File): Boolean {
        return videoFile.exists() && videoFile.isFile() && videoFile.length() > 0
    }

    /**
     * Get video file from URI string
     */
    fun getVideoFileFromUri(uriString: String?): File? {
        if (uriString.isNullOrBlank()) return null
        
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                File(uri.path ?: return null)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clean up old video files (optional maintenance function)
     */
    suspend fun cleanupOldVideos(maxAgeMs: Long = 30L * 24 * 60 * 60 * 1000): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            var deletedCount = 0
            
            videoDirectory.listFiles()?.forEach { file ->
                if (file.isFile() && (currentTime - file.lastModified()) > maxAgeMs) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            thumbnailDirectory.listFiles()?.forEach { file ->
                if (file.isFile() && (currentTime - file.lastModified()) > maxAgeMs) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}