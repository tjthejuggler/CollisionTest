package com.example.jugglingtracker.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "pattern_related_cross_ref",
    primaryKeys = ["patternId", "relatedId"],
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
            childColumns = ["relatedId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("patternId"),
        Index("relatedId")
    ]
)
data class PatternRelatedCrossRef(
    val patternId: Long,
    val relatedId: Long
)