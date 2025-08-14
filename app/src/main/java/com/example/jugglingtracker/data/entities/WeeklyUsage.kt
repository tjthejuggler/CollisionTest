package com.example.jugglingtracker.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_usage",
    indices = [Index("weekStartTimestamp", unique = true)]
)
data class WeeklyUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weekStartTimestamp: Long, // Monday 00:00:00 of the week
    val totalPoints: Int = 0,
    val patternsCreated: Int = 0,
    val testsCompleted: Int = 0,
    val totalTestDuration: Long = 0, // in milliseconds
    val videosRecorded: Int = 0,
    val appOpens: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class UsageLevel(
    val level: Int,
    val name: String,
    val minPoints: Int,
    val maxPoints: Int,
    val color: String,
    val darkColor: String
) {
    companion object {
        val LEVELS = listOf(
            UsageLevel(0, "Inactive", 0, 9, "#FFA0AEC0", "#FF78909C"), // Gray
            UsageLevel(1, "Beginner", 10, 49, "#FF68D391", "#FF66BB6A"), // Green
            UsageLevel(2, "Casual", 50, 99, "#FFECC94B", "#FFFFC107"), // Yellow
            UsageLevel(3, "Regular", 100, 199, "#FFED8936", "#FFFF9800"), // Orange
            UsageLevel(4, "Active", 200, 349, "#FF3182CE", "#FF42A5F5"), // Blue
            UsageLevel(5, "Dedicated", 350, 549, "#FF805AD5", "#FFAB47BC"), // Purple
            UsageLevel(6, "Expert", 550, 799, "#FFE53E3E", "#FFEF5350"), // Red
            UsageLevel(7, "Master", 800, Int.MAX_VALUE, "#FF9F7AEA", "#FF9C27B0") // Deep Purple
        )
        
        fun getLevelForPoints(points: Int): UsageLevel {
            return LEVELS.findLast { points >= it.minPoints } ?: LEVELS.first()
        }
        
        fun getColorForPoints(points: Int, isDarkTheme: Boolean = false): String {
            val level = getLevelForPoints(points)
            return if (isDarkTheme) level.darkColor else level.color
        }
    }
}