package com.example.jugglingtracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jugglingtracker.data.entities.Tag
import com.example.jugglingtracker.data.repository.TagRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the settings screen.
 * Handles tag management, app settings, and export/import functionality.
 */
class SettingsViewModel(
    private val tagRepository: TagRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // All tags
    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .map { result ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (result.isFailure) result.exceptionOrNull()?.message else null
            )
            result.getOrElse { emptyList() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Tag being edited
    private val _editingTag = MutableStateFlow<Tag?>(null)
    val editingTag: StateFlow<Tag?> = _editingTag.asStateFlow()

    // New tag form fields
    private val _newTagName = MutableStateFlow("")
    val newTagName: StateFlow<String> = _newTagName.asStateFlow()

    private val _newTagColor = MutableStateFlow(DEFAULT_TAG_COLOR)
    val newTagColor: StateFlow<Int> = _newTagColor.asStateFlow()

    // Form validation
    val isNewTagFormValid: StateFlow<Boolean> = _newTagName
        .map { name -> name.isNotBlank() && name.length >= 2 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Available tag colors
    val availableColors = listOf(
        0xFF2196F3.toInt(), // Blue
        0xFF4CAF50.toInt(), // Green
        0xFFFF9800.toInt(), // Orange
        0xFFF44336.toInt(), // Red
        0xFF9C27B0.toInt(), // Purple
        0xFF607D8B.toInt(), // Blue Grey
        0xFF795548.toInt(), // Brown
        0xFF009688.toInt(), // Teal
        0xFFE91E63.toInt(), // Pink
        0xFF673AB7.toInt(), // Deep Purple
        0xFF3F51B5.toInt(), // Indigo
        0xFF00BCD4.toInt(), // Cyan
        0xFF8BC34A.toInt(), // Light Green
        0xFFCDDC39.toInt(), // Lime
        0xFFFFEB3B.toInt(), // Yellow
        0xFFFF5722.toInt()  // Deep Orange
    )

    init {
        _uiState.value = _uiState.value.copy(isLoading = true)
    }

    /**
     * Create a new tag
     */
    fun createTag() {
        if (!isNewTagFormValid.value) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid tag name (at least 2 characters)")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val newTag = Tag(
                name = _newTagName.value.trim(),
                color = _newTagColor.value
            )

            val result = tagRepository.insertTag(newTag)

            if (result.isSuccess) {
                _newTagName.value = ""
                _newTagColor.value = DEFAULT_TAG_COLOR
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Tag created successfully"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to create tag: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Start editing a tag
     */
    fun startEditingTag(tag: Tag) {
        _editingTag.value = tag
        _newTagName.value = tag.name
        _newTagColor.value = tag.color
    }

    /**
     * Update the tag being edited
     */
    fun updateTag() {
        val tagToUpdate = _editingTag.value ?: return
        
        if (!isNewTagFormValid.value) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid tag name (at least 2 characters)")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val updatedTag = tagToUpdate.copy(
                name = _newTagName.value.trim(),
                color = _newTagColor.value
            )

            val result = tagRepository.updateTag(updatedTag)

            if (result.isSuccess) {
                cancelEditingTag()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Tag updated successfully"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update tag: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    /**
     * Cancel editing a tag
     */
    fun cancelEditingTag() {
        _editingTag.value = null
        _newTagName.value = ""
        _newTagColor.value = DEFAULT_TAG_COLOR
    }

    /**
     * Delete a tag
     */
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = tagRepository.deleteTag(tag)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (result.isFailure) {
                    "Failed to delete tag: ${result.exceptionOrNull()?.message}"
                } else null,
                message = if (result.isSuccess) "Tag deleted successfully" else null
            )
        }
    }

    /**
     * Update new tag name
     */
    fun updateNewTagName(name: String) {
        _newTagName.value = name
    }

    /**
     * Update new tag color
     */
    fun updateNewTagColor(color: Int) {
        _newTagColor.value = color
    }

    /**
     * Check if tag name already exists
     */
    fun isTagNameExists(name: String): StateFlow<Boolean> = allTags
        .map { tags ->
            val currentEditingId = _editingTag.value?.id
            tags.any { tag -> 
                tag.name.equals(name.trim(), ignoreCase = true) && 
                tag.id != currentEditingId 
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Get tag usage count (number of patterns using this tag)
     */
    fun getTagUsageCount(tagId: Long): StateFlow<Int> = 
        // This would require a method in TagRepository to count pattern usage
        // For now, return a placeholder
        flowOf(0).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * Export app data (placeholder implementation)
     */
    fun exportData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // TODO: Implement actual export functionality
            // This would involve:
            // 1. Gathering all data from repositories
            // 2. Converting to JSON or other format
            // 3. Writing to file or sharing
            
            kotlinx.coroutines.delay(2000) // Simulate export process
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = "Export functionality will be implemented in a future update"
            )
        }
    }

    /**
     * Import app data (placeholder implementation)
     */
    fun importData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // TODO: Implement actual import functionality
            // This would involve:
            // 1. Reading data from file
            // 2. Parsing JSON or other format
            // 3. Validating data structure
            // 4. Inserting into database
            
            kotlinx.coroutines.delay(2000) // Simulate import process
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = "Import functionality will be implemented in a future update"
            )
        }
    }

    /**
     * Reset all app data (placeholder implementation)
     */
    fun resetAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // TODO: Implement actual reset functionality
            // This would involve clearing all tables in the database
            
            kotlinx.coroutines.delay(1000) // Simulate reset process
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = "Reset functionality will be implemented in a future update"
            )
        }
    }

    /**
     * Get app statistics
     */
    fun getAppStatistics(): StateFlow<AppStatistics> = 
        // This would combine data from all repositories
        // For now, return placeholder data
        flowOf(AppStatistics()).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppStatistics()
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

    companion object {
        private const val DEFAULT_TAG_COLOR = 0xFF2196F3.toInt() // Blue
    }
}

/**
 * UI state for the settings screen
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * App statistics data
 */
data class AppStatistics(
    val totalPatterns: Int = 0,
    val totalTestSessions: Int = 0,
    val totalTags: Int = 0,
    val totalPracticeTime: Long = 0L,
    val averageSuccessRate: Double = 0.0,
    val mostPracticedPattern: String? = null,
    val databaseSize: Long = 0L
)

/**
 * App settings data class
 */
data class AppSettings(
    val darkMode: Boolean = false,
    val defaultTestLength: Int = 5, // minutes
    val showSuccessRate: Boolean = true,
    val autoBackup: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "18:00"
)