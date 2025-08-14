package com.example.jugglingtracker.ui.patterns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jugglingtracker.data.entities.Pattern
import com.example.jugglingtracker.data.entities.Tag
import com.example.jugglingtracker.data.repository.PatternRepository
import com.example.jugglingtracker.data.repository.TagRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the patterns list screen.
 * Handles pattern listing, sorting, filtering, and deletion.
 */
class PatternsListViewModel(
    private val patternRepository: PatternRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(PatternsListUiState())
    val uiState: StateFlow<PatternsListUiState> = _uiState.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sort option
    private val _sortOption = MutableStateFlow(SortOption.NAME)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Selected tag filter
    private val _selectedTagFilter = MutableStateFlow<Tag?>(null)
    val selectedTagFilter: StateFlow<Tag?> = _selectedTagFilter.asStateFlow()

    // All available tags for filtering
    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .map { result ->
            result.getOrElse { emptyList() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Patterns flow that combines search, sort, and filter
    val patterns: StateFlow<List<Pattern>> = combine(
        _searchQuery,
        _sortOption,
        _selectedTagFilter
    ) { query, sort, tagFilter ->
        Triple(query, sort, tagFilter)
    }.flatMapLatest { (query, sort, tagFilter) ->
        getFilteredAndSortedPatterns(query, sort, tagFilter)
    }.map { result ->
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = if (result.isFailure) result.exceptionOrNull()?.message else null
        )
        result.getOrElse { emptyList() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Set initial loading state
        _uiState.value = _uiState.value.copy(isLoading = true)
    }

    /**
     * Get filtered and sorted patterns based on current criteria
     */
    private fun getFilteredAndSortedPatterns(
        query: String,
        sort: SortOption,
        tagFilter: Tag?
    ): Flow<Result<List<Pattern>>> {
        return when {
            // Filter by tag first if selected
            tagFilter != null -> {
                patternRepository.getPatternsByTag(tagFilter.id)
            }
            // Search if query is not empty
            query.isNotBlank() -> {
                patternRepository.searchPatterns(query)
            }
            // Sort patterns
            sort == SortOption.DIFFICULTY_ASC -> {
                patternRepository.getPatternsByDifficulty(ascending = true)
            }
            sort == SortOption.DIFFICULTY_DESC -> {
                patternRepository.getPatternsByDifficulty(ascending = false)
            }
            sort == SortOption.NUM_BALLS -> {
                patternRepository.getAllPatterns().map { result ->
                    result.map { patterns ->
                        patterns.sortedBy { it.numBalls }
                    }
                }
            }
            sort == SortOption.RECENT -> {
                patternRepository.getAllPatterns().map { result ->
                    result.map { patterns ->
                        patterns.sortedByDescending { it.lastTested ?: 0 }
                    }
                }
            }
            else -> {
                // Default: sort by name
                patternRepository.getAllPatterns().map { result ->
                    result.map { patterns ->
                        patterns.sortedBy { it.name }
                    }
                }
            }
        }.map { result ->
            // Apply additional filtering if needed
            result.map { patterns ->
                var filteredPatterns = patterns
                
                // Apply search filter if we didn't already search
                if (query.isNotBlank() && tagFilter != null) {
                    filteredPatterns = filteredPatterns.filter { pattern ->
                        pattern.name.contains(query, ignoreCase = true)
                    }
                }
                
                // Apply sorting if we filtered by tag or searched
                if (tagFilter != null || query.isNotBlank()) {
                    filteredPatterns = when (sort) {
                        SortOption.NAME -> filteredPatterns.sortedBy { it.name }
                        SortOption.DIFFICULTY_ASC -> filteredPatterns.sortedBy { it.difficulty }
                        SortOption.DIFFICULTY_DESC -> filteredPatterns.sortedByDescending { it.difficulty }
                        SortOption.NUM_BALLS -> filteredPatterns.sortedBy { it.numBalls }
                        SortOption.RECENT -> filteredPatterns.sortedByDescending { it.lastTested ?: 0 }
                    }
                }
                
                filteredPatterns
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Update sort option
     */
    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    /**
     * Set tag filter
     */
    fun setTagFilter(tag: Tag?) {
        _selectedTagFilter.value = tag
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedTagFilter.value = null
        _sortOption.value = SortOption.NAME
    }

    /**
     * Delete a pattern
     */
    fun deletePattern(pattern: Pattern) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = patternRepository.deletePattern(pattern)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (result.isFailure) {
                    "Failed to delete pattern: ${result.exceptionOrNull()?.message}"
                } else null,
                message = if (result.isSuccess) "Pattern deleted successfully" else null
            )
        }
    }

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
}

/**
 * UI state for the patterns list screen
 */
data class PatternsListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * Sort options for patterns
 */
enum class SortOption {
    NAME,
    DIFFICULTY_ASC,
    DIFFICULTY_DESC,
    NUM_BALLS,
    RECENT
}