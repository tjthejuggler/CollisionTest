package com.example.jugglingtracker.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "pattern_prerequisite_cross_ref",
    primaryKeys = ["patternId", "prerequisiteId"],
    foreignKeys = [
        ForeignKey(
            entity = Pattern::class,
            parentColumns = ["id"],
            childColumns = ["patternId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Pattern::class,
            parentColumns = ["id"],
            childColumns = ["prerequisiteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("patternId"),
        Index("prerequisiteId")
    ]
)
data class PatternPrerequisiteCrossRef(
    val patternId: Long,
    val prerequisiteId: Long
)