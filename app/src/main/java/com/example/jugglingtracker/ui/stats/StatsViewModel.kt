package com.example.jugglingtracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jugglingtracker.data.entities.UsageLevel
import com.example.jugglingtracker.data.entities.WeeklyUsage
import com.example.jugglingtracker.data.repository.UsageTrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    val currentWeekPoints: Int = 0,
    val currentUsageLevel: UsageLevel = UsageLevel.LEVELS.first(),
    val progressPercentage: Float = 0f,
    val patternsCreated: Int = 0,
    val testsCompleted: Int = 0,
    val videosRecorded: Int = 0,
    val appOpens: Int = 0,
    val totalTestTime: Long = 0,
    val averageWeeklyPoints: Double = 0.0,
    val weeklyTrends: List<WeeklyTrendItem> = emptyList(),
    val error: String? = null
)

data class WeeklyTrendItem(
    val weekLabel: String,
    val weekDateRange: String,
    val points: Int,
    val level: UsageLevel,
    val isCurrentWeek: Boolean = false
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val usageTrackingRepository: UsageTrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Combine current week usage with weekly trends
                combine(
                    usageTrackingRepository.getCurrentWeekUsage(),
                    usageTrackingRepository.getAllWeeklyUsage()
                ) { currentWeek, allWeeklyUsage ->
                    Pair(currentWeek, allWeeklyUsage)
                }.collect { (currentWeek, allWeeklyUsage) ->
                    updateUiState(currentWeek, allWeeklyUsage)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private suspend fun updateUiState(currentWeek: WeeklyUsage?, allWeeklyUsage: List<WeeklyUsage>) {
        val currentPoints = currentWeek?.totalPoints ?: 0
        val currentLevel = UsageLevel.getLevelForPoints(currentPoints)
        
        // Calculate progress percentage within current level
        val progressPercentage = if (currentLevel.maxPoints == Int.MAX_VALUE) {
            100f // Master level is always 100%
        } else {
            val levelRange = currentLevel.maxPoints - currentLevel.minPoints + 1
            val pointsInLevel = currentPoints - currentLevel.minPoints
            (pointsInLevel.toFloat() / levelRange.toFloat() * 100f).coerceIn(0f, 100f)
        }

        // Calculate total statistics from current week
        val patternsCreated = currentWeek?.patternsCreated ?: 0
        val testsCompleted = currentWeek?.testsCompleted ?: 0
        val videosRecorded = currentWeek?.videosRecorded ?: 0
        val appOpens = currentWeek?.appOpens ?: 0
        val totalTestTime = currentWeek?.totalTestDuration ?: 0L

        // Calculate average weekly points
        val averageWeeklyPoints = usageTrackingRepository.getAverageWeeklyPoints(4)

        // Create weekly trends
        val weeklyTrends = createWeeklyTrends(allWeeklyUsage, currentWeek)

        _uiState.value = StatsUiState(
            isLoading = false,
            currentWeekPoints = currentPoints,
            currentUsageLevel = currentLevel,
            progressPercentage = progressPercentage,
            patternsCreated = patternsCreated,
            testsCompleted = testsCompleted,
            videosRecorded = videosRecorded,
            appOpens = appOpens,
            totalTestTime = totalTestTime,
            averageWeeklyPoints = averageWeeklyPoints,
            weeklyTrends = weeklyTrends,
            error = null
        )
    }

    private fun createWeeklyTrends(allWeeklyUsage: List<WeeklyUsage>, currentWeek: WeeklyUsage?): List<WeeklyTrendItem> {
        val currentWeekStart = usageTrackingRepository.getCurrentWeekStartTimestamp()
        val trends = mutableListOf<WeeklyTrendItem>()

        // Add current week if it exists
        currentWeek?.let { week ->
            trends.add(
                WeeklyTrendItem(
                    weekLabel = "This Week",
                    weekDateRange = formatWeekRange(week.weekStartTimestamp),
                    points = week.totalPoints,
                    level = UsageLevel.getLevelForPoints(week.totalPoints),
                    isCurrentWeek = true
                )
            )
        }

        // Add previous weeks (sorted by most recent first)
        val previousWeeks = allWeeklyUsage
            .filter { it.weekStartTimestamp != currentWeekStart }
            .sortedByDescending { it.weekStartTimestamp }
            .take(6) // Show last 6 weeks plus current week

        previousWeeks.forEachIndexed { index, week ->
            val weekLabel = when (index) {
                0 -> "Last Week"
                else -> "${index + 1} weeks ago"
            }

            trends.add(
                WeeklyTrendItem(
                    weekLabel = weekLabel,
                    weekDateRange = formatWeekRange(week.weekStartTimestamp),
                    points = week.totalPoints,
                    level = UsageLevel.getLevelForPoints(week.totalPoints),
                    isCurrentWeek = false
                )
            )
        }

        return trends
    }

    private fun formatWeekRange(weekStartTimestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = weekStartTimestamp
        val startDate = dateFormatter.format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_WEEK, 6) // Add 6 days to get Sunday
        val endDate = dateFormatter.format(calendar.time)
        
        return "$startDate - $endDate"
    }

    fun formatTestTime(timeInMillis: Long): String {
        val minutes = timeInMillis / (1000 * 60)
        return "${minutes} min"
    }

    fun formatPoints(points: Int): String {
        return "$points points"
    }

    fun formatLevel(level: UsageLevel): String {
        return "Level ${level.level} - ${level.name}"
    }

    fun getLevelColor(level: UsageLevel, isDarkTheme: Boolean = false): String {
        return if (isDarkTheme) level.darkColor else level.color
    }

    fun refreshStats() {
        loadStats()
    }
}