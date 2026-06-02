package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TaskDao {
    @Query("SELECT * FROM setup_tasks ORDER BY id ASC")
    fun getAllTasksFlow(): kotlinx.coroutines.flow.Flow<List<TaskEntity>>

    @Query("SELECT * FROM setup_tasks ORDER BY id ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("UPDATE setup_tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskCompletion(id: Int, isCompleted: Boolean)

    @Query("UPDATE setup_tasks SET title = :title, description = :description, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskDetails(id: Int, title: String, description: String, isCompleted: Boolean)

    @Query("UPDATE setup_tasks SET isCompleted = 0")
    suspend fun resetAllTasks()
}
