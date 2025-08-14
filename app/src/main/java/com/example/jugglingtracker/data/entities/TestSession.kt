package com.example.jugglingtracker.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Pattern::class,
            parentColumns = ["id"],
            childColumns = ["patternId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patternId")]
)
data class TestSession(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val patternId: Long,
    val date: Long,
    val duration: Long, // in milliseconds
    val successCount: Int,
    val attemptCount: Int,
    val notes: String?,
    val videoPath: String?
)