package com.example.jugglingtracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jugglingtracker.data.entities.TestSession
import com.example.jugglingtracker.data.repository.TestSessionRepository
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for the progress chart screen.
 * Handles test session data loading, filtering, and statistics calculation.
 */
class ProgressChartViewModel(
    private val testSessionRepository: TestSessionRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(ProgressChartUiState())
    val uiState: StateFlow<ProgressChartUiState> = _uiState.asStateFlow()

    // Current pattern ID
    private val _patternId = MutableStateFlow<Long?>(null)

    // Filter options
    private val _timeframeFilter = MutableStateFlow(TimeframeFilter.ALL_TIME)
    val timeframeFilter: StateFlow<TimeframeFilter> = _timeframeFilter.asStateFlow()

    private val _testLengthFilter = MutableStateFlow<TestLengthFilter?>(null)
    val testLengthFilter: StateFlow<TestLengthFilter?> = _testLengthFilter.asStateFlow()

    // Raw test sessions for the pattern
    private val rawTestSessions: StateFlow<List<TestSession>> = _patternId
        .filterNotNull()
        .flatMapLatest { id ->
            testSessionRepository.getTestSessionsByPattern(id)
        }
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

    // Filtered test sessions based on current filters
    val filteredTestSessions: StateFlow<List<TestSession>> = combine(
        rawTestSessions,
        _timeframeFilter,
        _testLengthFilter
    ) { sessions, timeframe, lengthFilter ->
        var filtered = sessions

        // Apply timeframe filter
        val cutoffTime = when (timeframe) {
            TimeframeFilter.LAST_WEEK -> System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            TimeframeFilter.LAST_MONTH -> System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            TimeframeFilter.LAST_3_MONTHS -> System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
            TimeframeFilter.LAST_YEAR -> System.currentTimeMillis() - (365 * 24 * 60 * 60 * 1000L)
            TimeframeFilter.ALL_TIME -> 0L
        }
        
        if (cutoffTime > 0) {
            filtered = filtered.filter { it.date >= cutoffTime }
        }

        // Apply test length filter
        lengthFilter?.let { filter ->
            filtered = when (filter) {
                TestLengthFilter.SHORT -> filtered.filter { it.duration <= 5 * 60 * 1000L } // <= 5 minutes
                TestLengthFilter.MEDIUM -> filtered.filter { it.duration in (5 * 60 * 1000L)..(15 * 60 * 1000L) } // 5-15 minutes
                TestLengthFilter.LONG -> filtered.filter { it.duration > 15 * 60 * 1000L } // > 15 minutes
            }
        }

        filtered.sortedBy { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Chart data points for success rate over time
    val chartDataPoints: StateFlow<List<ChartDataPoint>> = filteredTestSessions
        .map { sessions ->
            sessions.map { session ->
                val successRate = if (session.attemptCount > 0) {
                    (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                } else 0.0
                
                ChartDataPoint(
                    date = session.date,
                    successRate = successRate,
                    sessionDuration = session.duration,
                    successCount = session.successCount,
                    attemptCount = session.attemptCount
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Statistics
    val statistics: StateFlow<ProgressStatistics> = filteredTestSessions
        .map { sessions ->
            calculateStatistics(sessions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProgressStatistics()
        )

    /**
     * Load progress data for a specific pattern
     */
    fun loadProgressData(patternId: Long) {
        _patternId.value = patternId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
    }

    /**
     * Update timeframe filter
     */
    fun updateTimeframeFilter(filter: TimeframeFilter) {
        _timeframeFilter.value = filter
    }

    /**
     * Update test length filter
     */
    fun updateTestLengthFilter(filter: TestLengthFilter?) {
        _testLengthFilter.value = filter
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _timeframeFilter.value = TimeframeFilter.ALL_TIME
        _testLengthFilter.value = null
    }

    /**
     * Calculate statistics from test sessions
     */
    private fun calculateStatistics(sessions: List<TestSession>): ProgressStatistics {
        if (sessions.isEmpty()) {
            return ProgressStatistics()
        }

        val totalSessions = sessions.size
        val totalPracticeTime = sessions.sumOf { it.duration }
        val totalSuccesses = sessions.sumOf { it.successCount }
        val totalAttempts = sessions.sumOf { it.attemptCount }
        
        val overallSuccessRate = if (totalAttempts > 0) {
            (totalSuccesses.toDouble() / totalAttempts.toDouble()) * 100
        } else 0.0

        // Best session (highest success rate)
        val bestSession = sessions.maxByOrNull { session ->
            if (session.attemptCount > 0) {
                session.successCount.toDouble() / session.attemptCount.toDouble()
            } else 0.0
        }

        val bestSuccessRate = bestSession?.let { session ->
            if (session.attemptCount > 0) {
                (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
            } else 0.0
        } ?: 0.0

        // Recent trend (last 5 sessions vs previous 5)
        val trend = if (sessions.size >= 10) {
            val recent5 = sessions.takeLast(5)
            val previous5 = sessions.drop(sessions.size - 10).take(5)
            
            val recentAvg = recent5.map { session ->
                if (session.attemptCount > 0) {
                    (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                } else 0.0
            }.average()
            
            val previousAvg = previous5.map { session ->
                if (session.attemptCount > 0) {
                    (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                } else 0.0
            }.average()
            
            when {
                recentAvg > previousAvg + 5 -> ProgressTrend.IMPROVING
                recentAvg < previousAvg - 5 -> ProgressTrend.DECLINING
                else -> ProgressTrend.STABLE
            }
        } else {
            ProgressTrend.INSUFFICIENT_DATA
        }

        // Average session length
        val averageSessionLength = totalPracticeTime / totalSessions

        // Sessions per week (if we have enough data)
        val sessionsPerWeek = if (sessions.size >= 2) {
            val firstSession = sessions.first()
            val lastSession = sessions.last()
            val timeSpanWeeks = (lastSession.date - firstSession.date) / (7 * 24 * 60 * 60 * 1000.0)
            if (timeSpanWeeks > 0) {
                totalSessions / timeSpanWeeks
            } else 0.0
        } else 0.0

        return ProgressStatistics(
            totalSessions = totalSessions,
            totalPracticeTime = totalPracticeTime,
            overallSuccessRate = overallSuccessRate,
            bestSuccessRate = bestSuccessRate,
            bestSessionDate = bestSession?.date,
            trend = trend,
            averageSessionLength = averageSessionLength,
            sessionsPerWeek = sessionsPerWeek
        )
    }

    /**
     * Get success rate trend for the last N sessions
     */
    fun getSuccessRateTrend(sessionCount: Int = 10): StateFlow<List<Double>> = filteredTestSessions
        .map { sessions ->
            sessions.takeLast(sessionCount).map { session ->
                if (session.attemptCount > 0) {
                    (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                } else 0.0
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Get chart entries for MPAndroidChart LineChart
     */
    fun getChartEntries(): StateFlow<List<Entry>> = chartDataPoints
        .map { dataPoints ->
            dataPoints.mapIndexed { index, point ->
                Entry(point.date.toFloat(), point.successRate.toFloat(), point)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Get chart entries grouped by test length for multiple line chart
     */
    fun getChartEntriesByTestLength(): StateFlow<Map<TestLengthFilter, List<Entry>>> = filteredTestSessions
        .map { sessions ->
            val grouped = sessions.groupBy { session ->
                when {
                    session.duration <= 5 * 60 * 1000L -> TestLengthFilter.SHORT
                    session.duration <= 15 * 60 * 1000L -> TestLengthFilter.MEDIUM
                    else -> TestLengthFilter.LONG
                }
            }
            
            grouped.mapValues { (_, sessions) ->
                sessions.sortedBy { it.date }.mapIndexed { index, session ->
                    val successRate = if (session.attemptCount > 0) {
                        (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                    } else 0.0
                    
                    Entry(session.date.toFloat(), successRate.toFloat(),
                        ChartDataPoint(
                            date = session.date,
                            successRate = successRate,
                            sessionDuration = session.duration,
                            successCount = session.successCount,
                            attemptCount = session.attemptCount
                        )
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    /**
     * Get moving average data for trend analysis
     */
    fun getMovingAverageEntries(windowSize: Int = 5): StateFlow<List<Entry>> = chartDataPoints
        .map { dataPoints ->
            if (dataPoints.size < windowSize) return@map emptyList<Entry>()
            
            dataPoints.windowed(windowSize, 1) { window ->
                val avgSuccessRate = window.map { it.successRate }.average()
                val centerDate = window[window.size / 2].date
                Entry(centerDate.toFloat(), avgSuccessRate.toFloat())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Get goal line entries (target success rate)
     */
    fun getGoalLineEntries(targetSuccessRate: Double = 80.0): StateFlow<List<Entry>> = chartDataPoints
        .map { dataPoints ->
            if (dataPoints.isEmpty()) return@map emptyList<Entry>()
            
            val firstDate = dataPoints.first().date
            val lastDate = dataPoints.last().date
            
            listOf(
                Entry(firstDate.toFloat(), targetSuccessRate.toFloat()),
                Entry(lastDate.toFloat(), targetSuccessRate.toFloat())
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Get chart data for different metrics
     */
    fun getMetricEntries(metric: ChartMetric): StateFlow<List<Entry>> = filteredTestSessions
        .map { sessions ->
            sessions.sortedBy { it.date }.map { session ->
                val value = when (metric) {
                    ChartMetric.SUCCESS_RATE -> {
                        if (session.attemptCount > 0) {
                            (session.successCount.toDouble() / session.attemptCount.toDouble()) * 100
                        } else 0.0
                    }
                    ChartMetric.SUCCESS_COUNT -> session.successCount.toDouble()
                    ChartMetric.ATTEMPT_COUNT -> session.attemptCount.toDouble()
                    ChartMetric.DURATION_MINUTES -> session.duration.toDouble() / (1000 * 60)
                }
                
                Entry(session.date.toFloat(), value.toFloat(), session)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * Factory for creating ProgressChartViewModel with dependencies
 */
class ProgressChartViewModelFactory(
    private val testSessionRepository: TestSessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressChartViewModel::class.java)) {
            return ProgressChartViewModel(testSessionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * UI state for the progress chart screen
 */
data class ProgressChartUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Data point for chart visualization
 */
data class ChartDataPoint(
    val date: Long,
    val successRate: Double,
    val sessionDuration: Long,
    val successCount: Int,
    val attemptCount: Int
)

/**
 * Progress statistics
 */
data class ProgressStatistics(
    val totalSessions: Int = 0,
    val totalPracticeTime: Long = 0L,
    val overallSuccessRate: Double = 0.0,
    val bestSuccessRate: Double = 0.0,
    val bestSessionDate: Long? = null,
    val trend: ProgressTrend = ProgressTrend.INSUFFICIENT_DATA,
    val averageSessionLength: Long = 0L,
    val sessionsPerWeek: Double = 0.0
)

/**
 * Timeframe filter options
 */
enum class TimeframeFilter {
    LAST_WEEK,
    LAST_MONTH,
    LAST_3_MONTHS,
    LAST_YEAR,
    ALL_TIME
}

/**
 * Test length filter options
 */
enum class TestLengthFilter {
    SHORT,   // <= 5 minutes
    MEDIUM,  // 5-15 minutes
    LONG     // > 15 minutes
}

/**
 * Progress trend indicators
 */
enum class ProgressTrend {
    IMPROVING,
    STABLE,
    DECLINING,
    INSUFFICIENT_DATA
}

/**
 * Chart metrics for different visualization options
 */
enum class ChartMetric {
    SUCCESS_RATE,
    SUCCESS_COUNT,
    ATTEMPT_COUNT,
    DURATION_MINUTES
}