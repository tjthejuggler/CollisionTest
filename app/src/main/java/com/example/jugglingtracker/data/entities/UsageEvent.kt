package com.example.jugglingtracker.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_events",
    foreignKeys = [
        ForeignKey(
            entity = Pattern::class,
            parentColumns = ["id"],
            childColumns = ["patternId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patternId"), Index("timestamp"), Index("eventType")]
)
data class UsageEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: UsageEventType,
    val timestamp: Long,
    val patternId: Long? = null, // Optional - some events may not be pattern-specific
    val duration: Long? = null, // Duration in milliseconds for time-based events
    val metadata: String? = null // JSON string for additional event-specific data
)

enum class UsageEventType(val basePoints: Int, val description: String) {
    PATTERN_CREATED(10, "Created a new pattern"),
    PATTERN_EDITED(5, "Edited an existing pattern"),
    PATTERN_VIEWED(1, "Viewed pattern details"),
    TEST_STARTED(5, "Started a test session"),
    TEST_COMPLETED(15, "Completed a test session"), // Base points, will be modified by duration
    TEST_CANCELLED(2, "Cancelled a test session"),
    VIDEO_RECORDED(8, "Recorded a practice video"),
    VIDEO_TRIMMED(3, "Trimmed a practice video"),
    PATTERN_TAGGED(2, "Added tags to a pattern"),
    PROGRESS_VIEWED(3, "Viewed progress charts"),
    HISTORY_VIEWED(2, "Viewed test history"),
    SETTINGS_ACCESSED(1, "Accessed app settings"),
    APP_OPENED(2, "Opened the app"),
    PATTERN_SEARCHED(1, "Searched for patterns"),
    PATTERN_SORTED(1, "Sorted pattern list")
}