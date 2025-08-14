package com.example.jugglingtracker.ui.addedit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jugglingtracker.data.entities.Pattern
import com.example.jugglingtracker.data.entities.PatternEntity
import com.example.jugglingtracker.data.entities.Tag
import com.example.jugglingtracker.data.repository.PatternRepository
import com.example.jugglingtracker.data.repository.TagRepository
import com.example.jugglingtracker.utils.VideoManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for adding and editing patterns.
 * Handles form validation, tag selection, and pattern relationships.
 */
class AddEditPatternViewModel(
    private val patternRepository: PatternRepository,
    private val tagRepository: TagRepository,
    private val context: Context
) : ViewModel() {

    private val videoManager = VideoManager(context)

    // UI State
    private val _uiState = MutableStateFlow(AddEditPatternUiState())
    val uiState: StateFlow<AddEditPatternUiState> = _uiState.asStateFlow()

    // Form fields
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _difficulty = MutableStateFlow(1)
    val difficulty: StateFlow<Int> = _difficulty.asStateFlow()

    private val _numBalls = MutableStateFlow(3)
    val numBalls: StateFlow<Int> = _numBalls.asStateFlow()

    private val _videoUri = MutableStateFlow<String?>(null)
    val videoUri: StateFlow<String?> = _videoUri.asStateFlow()

    private val _videoFile = MutableStateFlow<File?>(null)
    val videoFile: StateFlow<File?> = _videoFile.asStateFlow()

    private val _thumbnailFile = MutableStateFlow<File?>(null)
    val thumbnailFile: StateFlow<File?> = _thumbnailFile.asStateFlow()

    private val _videoDuration = MutableStateFlow<Long>(0L)
    val videoDuration: StateFlow<Long> = _videoDuration.asStateFlow()

    private val _videoFileSize = MutableStateFlow<Long>(0L)
    val videoFileSize: StateFlow<Long> = _videoFileSize.asStateFlow()

    // Selected tags
    private val _selectedTags = MutableStateFlow<Set<Tag>>(emptySet())
    val selectedTags: StateFlow<Set<Tag>> = _selectedTags.asStateFlow()

    // Selected relationships
    private val _selectedPrerequisites = MutableStateFlow<Set<Pattern>>(emptySet())
    val selectedPrerequisites: StateFlow<Set<Pattern>> = _selectedPrerequisites.asStateFlow()

    private val _selectedDependents = MutableStateFlow<Set<Pattern>>(emptySet())
    val selectedDependents: StateFlow<Set<Pattern>> = _selectedDependents.asStateFlow()

    private val _selectedRelatedPatterns = MutableStateFlow<Set<Pattern>>(emptySet())
    val selectedRelatedPatterns: StateFlow<Set<Pattern>> = _selectedRelatedPatterns.asStateFlow()

    // Current pattern being edited (null for new pattern)
    private var currentPatternId: Long? = null

    // All available tags
    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .map { result -> result.getOrElse { emptyList() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All available patterns for relationships (excluding current pattern)
    val availablePatterns: StateFlow<List<Pattern>> = patternRepository.getAllPatterns()
        .map { result ->
            result.getOrElse { emptyList() }
                .filter { it.id != currentPatternId }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form validation
    val isFormValid: StateFlow<Boolean> = combine(
        _name,
        _difficulty,
        _numBalls
    ) { name, difficulty, numBalls ->
        name.isNotBlank() && 
        difficulty in 1..10 && 
        numBalls >= 1
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /**
     * Initialize for editing an existing pattern
     */
    fun initializeForEdit(patternId: Long) {
        currentPatternId = patternId
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            val result = patternRepository.getPatternWithRelationships(patternId)
            
            if (result.isSuccess) {
                val patternEntity = result.getOrNull()
                if (patternEntity != null) {
                    loadPatternData(patternEntity)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load pattern: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Load pattern data into form fields
     */
    private fun loadPatternData(patternEntity: PatternEntity) {
        _name.value = patternEntity.pattern.name
        _difficulty.value = patternEntity.pattern.difficulty
        _numBalls.value = patternEntity.pattern.numBalls
        _videoUri.value = patternEntity.pattern.videoUri
        _selectedTags.value = patternEntity.tags.toSet()
        _selectedPrerequisites.value = patternEntity.prerequisites.toSet()
        _selectedDependents.value = patternEntity.dependents.toSet()
        _selectedRelatedPatterns.value = patternEntity.relatedPatterns.toSet()
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isEditMode = true
        )
    }

    /**
     * Update form fields
     */
    fun updateName(name: String) {
        _name.value = name
        validateForm()
    }

    fun updateDifficulty(difficulty: Int) {
        _difficulty.value = difficulty.coerceIn(1, 10)
        validateForm()
    }

    fun updateNumBalls(numBalls: Int) {
        _numBalls.value = numBalls.coerceAtLeast(1)
        validateForm()
    }

    fun updateVideoUri(uri: String?) {
        _videoUri.value = uri
        updateVideoFileInfo(uri)
    }

    /**
     * Update video file information when URI changes
     */
    private fun updateVideoFileInfo(uri: String?) {
        viewModelScope.launch {
            if (uri.isNullOrBlank()) {
                _videoFile.value = null
                _thumbnailFile.value = null
                _videoDuration.value = 0L
                _videoFileSize.value = 0L
                return@launch
            }

            val videoFile = videoManager.getVideoFileFromUri(uri)
            _videoFile.value = videoFile

            videoFile?.let { file ->
                if (videoManager.videoExists(file)) {
                    _videoFileSize.value = videoManager.getVideoFileSize(file)
                    
                    // Get video duration
                    val durationResult = videoManager.getVideoDuration(file)
                    if (durationResult.isSuccess) {
                        _videoDuration.value = durationResult.getOrNull() ?: 0L
                    }
                }
            }
        }
    }

    /**
     * Set video from recorded file
     */
    fun setVideoFromRecording(videoFile: File) {
        viewModelScope.launch {
            _videoFile.value = videoFile
            _videoUri.value = videoFile.toURI().toString()
            _videoFileSize.value = videoManager.getVideoFileSize(videoFile)
            
            // Generate thumbnail
            val thumbnailResult = videoManager.generateThumbnail(videoFile)
            if (thumbnailResult.isSuccess) {
                _thumbnailFile.value = thumbnailResult.getOrNull()
            }
            
            // Get video duration
            val durationResult = videoManager.getVideoDuration(videoFile)
            if (durationResult.isSuccess) {
                _videoDuration.value = durationResult.getOrNull() ?: 0L
            }
        }
    }

    /**
     * Set video from imported URI
     */
    fun setVideoFromImport(sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Copy video to app storage
            val copyResult = videoManager.copyVideoToAppStorage(sourceUri)
            if (copyResult.isSuccess) {
                val videoFile = copyResult.getOrNull()!!
                _videoFile.value = videoFile
                _videoUri.value = videoFile.toURI().toString()
                _videoFileSize.value = videoManager.getVideoFileSize(videoFile)
                
                // Generate thumbnail
                val thumbnailResult = videoManager.generateThumbnail(videoFile)
                if (thumbnailResult.isSuccess) {
                    _thumbnailFile.value = thumbnailResult.getOrNull()
                }
                
                // Get video duration
                val durationResult = videoManager.getVideoDuration(videoFile)
                if (durationResult.isSuccess) {
                    _videoDuration.value = durationResult.getOrNull() ?: 0L
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to import video: ${copyResult.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Remove current video
     */
    fun removeVideo() {
        viewModelScope.launch {
            val videoFile = _videoFile.value
            val thumbnailFile = _thumbnailFile.value
            
            if (videoFile != null) {
                videoManager.deleteVideo(videoFile, thumbnailFile)
            }
            
            _videoFile.value = null
            _videoUri.value = null
            _thumbnailFile.value = null
            _videoDuration.value = 0L
            _videoFileSize.value = 0L
        }
    }

    /**
     * Get formatted video duration
     */
    fun getFormattedVideoDuration(): String {
        return videoManager.formatDuration(_videoDuration.value)
    }

    /**
     * Get formatted video file size
     */
    fun getFormattedVideoFileSize(): String {
        return videoManager.formatFileSize(_videoFileSize.value)
    }

    /**
     * Get shareable video URI
     */
    fun getShareableVideoUri(): Uri? {
        val videoFile = _videoFile.value ?: return null
        return if (videoManager.videoExists(videoFile)) {
            videoManager.getShareableUri(videoFile)
        } else null
    }

    /**
     * Tag selection methods
     */
    fun toggleTag(tag: Tag) {
        val currentTags = _selectedTags.value.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _selectedTags.value = currentTags
    }

    fun addTag(tag: Tag) {
        _selectedTags.value = _selectedTags.value + tag
    }

    fun removeTag(tag: Tag) {
        _selectedTags.value = _selectedTags.value - tag
    }

    /**
     * Relationship selection methods
     */
    fun togglePrerequisite(pattern: Pattern) {
        val current = _selectedPrerequisites.value.toMutableSet()
        if (current.contains(pattern)) {
            current.remove(pattern)
        } else {
            current.add(pattern)
        }
        _selectedPrerequisites.value = current
    }

    fun toggleDependent(pattern: Pattern) {
        val current = _selectedDependents.value.toMutableSet()
        if (current.contains(pattern)) {
            current.remove(pattern)
        } else {
            current.add(pattern)
        }
        _selectedDependents.value = current
    }

    fun toggleRelatedPattern(pattern: Pattern) {
        val current = _selectedRelatedPatterns.value.toMutableSet()
        if (current.contains(pattern)) {
            current.remove(pattern)
        } else {
            current.add(pattern)
        }
        _selectedRelatedPatterns.value = current
    }

    /**
     * Save the pattern
     */
    fun savePattern() {
        if (!isFormValid.value) {
            _uiState.value = _uiState.value.copy(error = "Please fill in all required fields correctly")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val pattern = Pattern(
                id = currentPatternId ?: 0,
                name = _name.value.trim(),
                difficulty = _difficulty.value,
                numBalls = _numBalls.value,
                videoUri = _videoUri.value?.takeIf { it.isNotBlank() },
                lastTested = if (currentPatternId != null) {
                    // Keep existing lastTested for edits
                    patternRepository.getPatternById(currentPatternId!!).getOrNull()?.lastTested
                } else null
            )

            val result = if (currentPatternId != null) {
                patternRepository.updatePattern(pattern)
            } else {
                patternRepository.insertPattern(pattern)
            }

            if (result.isSuccess) {
                val patternId = if (currentPatternId != null) {
                    currentPatternId!!
                } else {
                    result.getOrNull() as? Long ?: return@launch
                }

                // Save relationships
                saveRelationships(patternId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save pattern: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Save pattern relationships
     */
    private suspend fun saveRelationships(patternId: Long) {
        try {
            // If editing, we need to clear existing relationships first
            if (currentPatternId != null) {
                // This would require additional DAO methods to clear relationships
                // For now, we'll assume the relationships are managed properly
            }

            // Save tags
            _selectedTags.value.forEach { tag ->
                patternRepository.addTagToPattern(patternId, tag.id)
            }

            // Save prerequisites
            _selectedPrerequisites.value.forEach { prerequisite ->
                patternRepository.addPrerequisite(patternId, prerequisite.id)
            }

            // Save dependents
            _selectedDependents.value.forEach { dependent ->
                patternRepository.addDependent(patternId, dependent.id)
            }

            // Save related patterns
            _selectedRelatedPatterns.value.forEach { related ->
                patternRepository.addRelatedPattern(patternId, related.id)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                patternSaved = true,
                savedPatternId = patternId
            )

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to save relationships: ${e.message}"
            )
        }
    }

    /**
     * Validate form
     */
    private fun validateForm() {
        val nameError = when {
            _name.value.isBlank() -> "Pattern name is required"
            _name.value.length < 2 -> "Pattern name must be at least 2 characters"
            else -> null
        }

        val difficultyError = when {
            _difficulty.value !in 1..10 -> "Difficulty must be between 1 and 10"
            else -> null
        }

        val numBallsError = when {
            _numBalls.value < 1 -> "Number of balls must be at least 1"
            else -> null
        }

        _uiState.value = _uiState.value.copy(
            nameError = nameError,
            difficultyError = difficultyError,
            numBallsError = numBallsError
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear saved pattern state (after navigation)
     */
    fun clearSavedPattern() {
        _uiState.value = _uiState.value.copy(
            patternSaved = false,
            savedPatternId = null
        )
    }

    /**
     * Reset form for new pattern
     */
    fun resetForm() {
        currentPatternId = null
        _name.value = ""
        _difficulty.value = 1
        _numBalls.value = 3
        _videoUri.value = null
        _selectedTags.value = emptySet()
        _selectedPrerequisites.value = emptySet()
        _selectedDependents.value = emptySet()
        _selectedRelatedPatterns.value = emptySet()
        _uiState.value = AddEditPatternUiState()
    }
}

/**
 * UI state for the add/edit pattern screen
 */
data class AddEditPatternUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val difficultyError: String? = null,
    val numBallsError: String? = null,
    val patternSaved: Boolean = false,
    val savedPatternId: Long? = null,
    val isRecordingVideo: Boolean = false,
    val recordingDuration: Long = 0L
)