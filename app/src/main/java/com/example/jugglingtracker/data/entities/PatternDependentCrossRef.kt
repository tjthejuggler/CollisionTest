package com.example.jugglingtracker.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "pattern_dependent_cross_ref",
    primaryKeys = ["patternId", "dependentId"],
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
            childColumns = ["dependentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("patternId"),
        Index("dependentId")
    ]
)
data class PatternDependentCrossRef(
    val patternId: Long,
    val dependentId: Long
)