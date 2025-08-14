package com.example.jugglingtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jugglingtracker.data.entities.Pattern
import com.example.jugglingtracker.data.entities.TestSession
import com.example.jugglingtracker.data.repository.PatternRepository
import com.example.jugglingtracker.data.repository.TestSessionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the test history screen.
 * Handles test session listing, filtering, searching, and deletion.
 */
class TestHistoryViewModel(
    private val testSessionRepository: TestSessionRepository,
    private val patternRepository: PatternRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(TestHistoryUiState())
    val uiState: StateFlow<TestHistoryUiState> = _uiState.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter options
    private val _selectedPattern = MutableStateFlow<Pattern?>(null)
    val selectedPattern: StateFlow<Pattern?> = _selectedPattern.asStateFlow()

    private val _dateRangeFilter = MutableStateFlow(DateRangeFilter.ALL_TIME)
    val dateRangeFilter: StateFlow<DateRangeFilter> = _dateRangeFilter.asStateFlow()

    private val _successRateFilter = MutableStateFlow<SuccessRateFilter?>(null)
    val successRateFilter: StateFlow<SuccessRateFilter?> = _successRateFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(HistorySortOption.DATE_DESC)
    val sortOption: StateFlow<HistorySortOption> = _sortOption.asStateFlow()

    // All available patterns for filtering
    val allPatterns: StateFlow<List<Pattern>> = patternRepository.getAllPatterns()
        .map { result -> result.getOrElse { emptyList() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Simple test sessions flow - avoiding complex combine for now
    val filteredTestSessions: StateFlow<List<TestSessionWithPattern>> = 
        testSessionRepository.getAllTestSessions()
            .map { sessionsResult ->
                val sessions = sessionsResult.getOrElse { emptyList() }
                sessions.map { session ->
                    TestSessionWithPattern(
                        testSession = session,
                        pattern = null // Simplified for now
                    )
                }
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Summary statistics
    val summaryStatistics: StateFlow<HistorySummaryStatistics> = filteredTestSessions
        .map { sessions ->
            calculateSummaryStatistics(sessions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistorySummaryStatistics()
        )

    init {
        _uiState.value = _uiState.value.copy(isLoading = true)
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Set pattern filter
     */
    fun setPatternFilter(pattern: Pattern?) {
        _selectedPattern.value = pattern
    }

    /**
     * Update date range filter
     */
    fun updateDateRangeFilter(filter: DateRangeFilter) {
        _dateRangeFilter.value = filter
    }

    /**
     * Update success rate filter
     */
    fun updateSuccessRateFilter(filter: SuccessRateFilter?) {
        _successRateFilter.value = filter
    }

    /**
     * Update sort option
     */
    fun updateSortOption(option: HistorySortOption) {
        _sortOption.value = option
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedPattern.value = null
        _dateRangeFilter.value = DateRangeFilter.ALL_TIME
        _successRateFilter.value = null
        _sortOption.value = HistorySortOption.DATE_DESC
    }

    /**
     * Delete a test session
     */
    fun deleteTestSession(testSession: TestSession) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = testSessionRepository.deleteTestSession(testSession)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (result.isFailure) {
                    "Failed to delete test session: ${result.exceptionOrNull()?.message}"
                } else null,
                message = if (result.isSuccess) "Test session deleted successfully" else null
            )
        }
    }

    /**
     * Calculate summary statistics
     */
    private fun calculateSummaryStatistics(sessions: List<TestSessionWithPattern>): HistorySummaryStatistics {
        if (sessions.isEmpty()) {
            return HistorySummaryStatistics()
        }

        val totalSessions = sessions.size
        val totalPracticeTime = sessions.sumOf { it.testSession.duration }
        val totalSuccesses = sessions.sumOf { it.testSession.successCount }
        val totalAttempts = sessions.sumOf { it.testSession.attemptCount }
        
        val averageSuccessRate = if (totalAttempts > 0) {
            (totalSuccesses.toDouble() / totalAttempts.toDouble()) * 100
        } else 0.0

        val uniquePatterns = sessions.mapNotNull { it.pattern }.distinctBy { it.id }.size

        val averageSessionLength = if (totalSessions > 0) totalPracticeTime / totalSessions else 0L

        // Most practiced pattern
        val mostPracticedPattern = sessions
            .groupBy { it.pattern?.id }
            .maxByOrNull { it.value.size }
            ?.value?.firstOrNull()?.pattern

        return HistorySummaryStatistics(
            totalSessions = totalSessions,
            totalPracticeTime = totalPracticeTime,
            averageSuccessRate = averageSuccessRate,
            uniquePatterns = uniquePatterns,
            averageSessionLength = averageSessionLength,
            mostPracticedPattern = mostPracticedPattern
        )
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
 * Test session with associated pattern information
 */
data class TestSessionWithPattern(
    val testSession: TestSession,
    val pattern: Pattern?
)

/**
 * UI state for the test history screen
 */
data class TestHistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * Summary statistics for test history
 */
data class HistorySummaryStatistics(
    val totalSessions: Int = 0,
    val totalPracticeTime: Long = 0L,
    val averageSuccessRate: Double = 0.0,
    val uniquePatterns: Int = 0,
    val averageSessionLength: Long = 0L,
    val mostPracticedPattern: Pattern? = null
)

/**
 * Date range filter options
 */
enum class DateRangeFilter {
    TODAY,
    LAST_WEEK,
    LAST_MONTH,
    LAST_3_MONTHS,
    ALL_TIME
}

/**
 * Success rate filter options
 */
enum class SuccessRateFilter {
    EXCELLENT, // >= 90%
    GOOD,      // 70-89%
    FAIR,      // 50-69%
    POOR       // < 50%
}

/**
 * Sort options for test history
 */
enum class HistorySortOption {
    DATE_DESC,
    DATE_ASC,
    PATTERN_NAME,
    SUCCESS_RATE_DESC,
    SUCCESS_RATE_ASC,
    DURATION_DESC,
    DURATION_ASC
}