package com.example.jugglingtracker.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val name: String,
    val color: Int
)