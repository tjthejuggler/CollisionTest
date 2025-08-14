package com.example.jugglingtracker.data.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PatternEntity(
    @Embedded val pattern: Pattern,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "patternId"
    )
    val testSessions: List<TestSession>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PatternTagCrossRef::class,
            parentColumn = "patternId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PatternPrerequisiteCrossRef::class,
            parentColumn = "patternId",
            entityColumn = "prerequisiteId"
        )
    )
    val prerequisites: List<Pattern>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PatternDependentCrossRef::class,
            parentColumn = "patternId",
            entityColumn = "dependentId"
        )
    )
    val dependents: List<Pattern>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PatternRelatedCrossRef::class,
            parentColumn = "patternId",
            entityColumn = "relatedId"
        )
    )
    val relatedPatterns: List<Pattern>
)