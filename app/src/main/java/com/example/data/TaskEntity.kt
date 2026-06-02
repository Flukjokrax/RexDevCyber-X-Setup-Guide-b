package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "setup_tasks")
data class TaskEntity(
    @PrimaryKey val id: Int,
    val category: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false
)
