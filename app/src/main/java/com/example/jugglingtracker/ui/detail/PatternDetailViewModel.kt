package com.example.jugglingtracker.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jugglingtracker.data.entities.Pattern
import com.example.jugglingtracker.data.entities.PatternEntity
import com.example.jugglingtracker.data.entities.TestSession
import com.example.jugglingtracker.data.repository.PatternRepository
import com.example.jugglingtracker.data.repository.TestSessionRepository
import com.example.jugglingtracker.utils.VideoManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for the pattern detail screen.
 * Handles pattern details loading, test session creation, and pattern cloning.
 */
class PatternDetailViewModel(
    private val patternRepository: PatternRepository,
    private val testSessionRepository: TestSessionRepository,
    private val context: Context
) : ViewModel() {

    private val videoManager = VideoManager(context)

    // UI State
    private val _uiState = MutableStateFlow(PatternDetailUiState())
    val uiState: StateFlow<PatternDetailUiState> = _uiState.asStateFlow()

    // Current pattern ID
    private val _patternId = MutableStateFlow<Long?>(null)

    // Pattern with all relationships
    val patternEntity: StateFlow<PatternEntity?> = _patternId
        .filterNotNull()
        .flatMapLatest { id ->
            patternRepository.getPatternWithRelationshipsFlow(id)
        }
        .map { result ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (result.isFailure) result.exceptionOrNull()?.message else null
            )
            result.getOrNull()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Recent test sessions for this pattern
    val recentTestSessions: StateFlow<List<TestSession>> = _patternId
        .filterNotNull()
        .flatMapLatest { id ->
            testSessionRepository.getTestSessionsByPattern(id)
        }
        .map { result ->
            result.getOrElse { emptyList() }
                .sortedByDescending { it.date }
                .take(5) // Show only 5 most recent
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Best test session statistics
    val bestTestSession: StateFlow<TestSession?> = _patternId
        .filterNotNull()
        .flatMapLatest { id ->
            testSessionRepository.getTestSessionsByPattern(id)
        }
        .map { result ->
            result.getOrElse { emptyList() }
                .maxByOrNull { session ->
                    if (session.attemptCount > 0) {
                        session.successCount.toDouble() / session.attemptCount.toDouble()
                    } else 0.0
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Video-related state flows
    val videoFile: StateFlow<File?> = patternEntity
        .map { entity ->
            entity?.pattern?.videoUri?.let { uri ->
                android.util.Log.d("PatternDetailVM", "Getting video file from URI: $uri")
                val file = videoManager.getVideoFileFromUri(uri)
                android.util.Log.d("PatternDetailVM", "Video file: ${file?.absolutePath}, exists: ${file?.exists()}")
                file
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val videoDuration: StateFlow<Long> = videoFile
        .filterNotNull()
        .flatMapLatest { file ->
            flow {
                val durationResult = videoManager.getVideoDuration(file)
                emit(durationResult.getOrNull() ?: 0L)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val videoFileSize: StateFlow<Long> = videoFile
        .map { file ->
            file?.let { videoManager.getVideoFileSize(it) } ?: 0L
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    // Video playback state
    private val _videoPlaybackState = MutableStateFlow(VideoPlaybackState())
    val videoPlaybackState: StateFlow<VideoPlaybackState> = _videoPlaybackState.asStateFlow()

    /**
     * Load pattern details by ID
     */
    fun loadPattern(patternId: Long) {
        _patternId.value = patternId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
    }

    /**
     * Create a new test session for the current pattern
     */
    fun createTestSession(
        durationMinutes: Int,
        successCount: Int,
        dropsCount: Int,
        notes: String? = null
    ) {
        val currentPatternId = _patternId.value ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val testSession = TestSession(
                patternId = currentPatternId,
                date = System.currentTimeMillis(),
                duration = durationMinutes * 60 * 1000L, // Convert to milliseconds
                successCount = successCount,
                attemptCount = successCount + dropsCount, // Total attempts = successful + drops
                notes = notes,
                videoPath = null // Will be implemented later
            )
            
            val result = testSessionRepository.insertTestSession(testSession)
            
            if (result.isSuccess) {
                // Update the pattern's lastTested timestamp
                patternRepository.updateLastTested(currentPatternId, System.currentTimeMillis())
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Test session recorded successfully!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to record test session: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Clone the current pattern with a new name
     */
    fun clonePattern(newName: String) {
        val currentPattern = patternEntity.value?.pattern ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Create new pattern with modified name
            val clonedPattern = currentPattern.copy(
                id = 0, // Reset ID for new pattern
                name = newName,
                lastTested = null // Reset test history
            )
            
            val result = patternRepository.insertPattern(clonedPattern)
            
            if (result.isSuccess) {
                val newPatternId = result.getOrNull() ?: return@launch
                
                // Copy tags from original pattern
                val originalEntity = patternEntity.value
                originalEntity?.tags?.forEach { tag ->
                    patternRepository.addTagToPattern(newPatternId, tag.id)
                }
                
                // Copy relationships (prerequisites, dependents, related patterns)
                originalEntity?.prerequisites?.forEach { prerequisite ->
                    patternRepository.addPrerequisite(newPatternId, prerequisite.id)
                }
                
                originalEntity?.dependents?.forEach { dependent ->
                    patternRepository.addDependent(newPatternId, dependent.id)
                }
                
                originalEntity?.relatedPatterns?.forEach { related ->
                    patternRepository.addRelatedPattern(newPatternId, related.id)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Pattern cloned successfully!",
                    clonedPatternId = newPatternId
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to clone pattern: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Delete the current pattern
     */
    fun deletePattern() {
        val currentPattern = patternEntity.value?.pattern ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = patternRepository.deletePattern(currentPattern)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (result.isFailure) {
                    "Failed to delete pattern: ${result.exceptionOrNull()?.message}"
                } else null,
                patternDeleted = result.isSuccess
            )
        }
    }

    /**
     * Calculate success rate for the pattern
     */
    fun getSuccessRate(): StateFlow<Double> = _patternId
        .filterNotNull()
        .flatMapLatest { id ->
            testSessionRepository.getTestSessionsByPattern(id)
        }
        .map { result ->
            val sessions = result.getOrElse { emptyList() }
            if (sessions.isEmpty()) {
                0.0
            } else {
                val totalSuccess = sessions.sumOf { it.successCount }
                val totalAttempts = sessions.sumOf { it.attemptCount }
                if (totalAttempts > 0) {
                    (totalSuccess.toDouble() / totalAttempts.toDouble()) * 100
                } else 0.0
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /**
     * Get total practice time for the pattern
     */
    fun getTotalPracticeTime(): StateFlow<Long> = _patternId
        .filterNotNull()
        .flatMapLatest { id ->
            testSessionRepository.getTestSessionsByPattern(id)
        }
        .map { result ->
            result.getOrElse { emptyList() }
                .sumOf { it.duration }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /**
     * Clear cloned pattern ID (after navigation)
     */
    fun clearClonedPatternId() {
        _uiState.value = _uiState.value.copy(clonedPatternId = null)
    }

    /**
     * Clear pattern deleted flag (after navigation)
     */
    fun clearPatternDeleted() {
        _uiState.value = _uiState.value.copy(patternDeleted = false)
    }

    /**
     * Video playback control methods
     */
    fun playVideo() {
        _videoPlaybackState.value = _videoPlaybackState.value.copy(
            isPlaying = true,
            isPaused = false
        )
    }

    fun pauseVideo() {
        _videoPlaybackState.value = _videoPlaybackState.value.copy(
            isPlaying = false,
            isPaused = true
        )
    }

    fun stopVideo() {
        _videoPlaybackState.value = _videoPlaybackState.value.copy(
            isPlaying = false,
            isPaused = false,
            currentPosition = 0L
        )
    }

    fun seekTo(position: Long) {
        _videoPlaybackState.value = _videoPlaybackState.value.copy(
            currentPosition = position
        )
    }

    fun updateVideoPosition(position: Long) {
        _videoPlaybackState.value = _videoPlaybackState.value.copy(
            currentPosition = position
        )
    }

    fun setVideoLoading(isLoading: Boolean) {
        _videoPlaybackState.value = _videoPlaybackState.value.copy(
            isLoading = isLoading
        )
    }

    fun setVideoError(error: String?) {
        _videoPlaybackState.value = _videoPlaybackState.value.copy(
            error = error
        )
    }

    /**
     * Share video functionality
     */
    fun shareVideo(): Intent? {
        val currentVideoFile = videoFile.value ?: return null
        
        if (!videoManager.videoExists(currentVideoFile)) {
            _uiState.value = _uiState.value.copy(error = "Video file not found")
            return null
        }

        return try {
            val shareUri = videoManager.getShareableUri(currentVideoFile)
            val patternName = patternEntity.value?.pattern?.name ?: "Pattern"
            
            Intent().apply {
                action = Intent.ACTION_SEND
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, "Juggling Pattern: $patternName")
                putExtra(Intent.EXTRA_TEXT, "Check out this juggling pattern video!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to share video: ${e.message}")
            null
        }
    }

    /**
     * Get formatted video duration
     */
    fun getFormattedVideoDuration(): String {
        return videoManager.formatDuration(videoDuration.value)
    }

    /**
     * Get formatted video file size
     */
    fun getFormattedVideoFileSize(): String {
        return videoManager.formatFileSize(videoFileSize.value)
    }

    /**
     * Check if pattern has video
     */
    fun hasVideo(): Boolean {
        val file = videoFile.value
        return file != null && videoManager.videoExists(file)
    }

    /**
     * Get video URI for playback
     */
    fun getVideoUri(): Uri? {
        val file = videoFile.value ?: return null
        return if (videoManager.videoExists(file)) {
            Uri.fromFile(file)
        } else null
    }

    /**
     * Clear video playback error
     */
    fun clearVideoError() {
        _videoPlaybackState.value = _videoPlaybackState.value.copy(error = null)
    }
}

/**
 * UI state for the pattern detail screen
 */
data class PatternDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val clonedPatternId: Long? = null,
    val patternDeleted: Boolean = false
)

/**
 * Video playback state
 */
data class VideoPlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isLoading: Boolean = false,
    val currentPosition: Long = 0L,
    val error: String? = null
)