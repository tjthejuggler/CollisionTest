package com.example.jugglingtracker.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patterns")
data class Pattern(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val name: String,
    val difficulty: Int, // 1-10 scale
    val numBalls: Int,
    val videoUri: String?,
    val lastTested: Long? // timestamp of last test session
)