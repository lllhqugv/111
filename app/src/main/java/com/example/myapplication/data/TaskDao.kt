package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY completed ASC, createdAt DESC")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<Task?>

    /** 截止时间落在 (now, threshold] 且尚未提醒、未完成的任务，按截止时间升序。 */
    @Query(
        "SELECT * FROM tasks WHERE dueAt > :now AND dueAt <= :threshold AND notifiedAt IS NULL " +
            "AND completed = 0 ORDER BY dueAt ASC"
    )
    suspend fun unnotifiedTasksDueWithin(now: Long, threshold: Long): List<Task>

    @Query("UPDATE tasks SET notifiedAt = :at WHERE id = :id")
    suspend fun markNotified(id: Long, at: Long)

    @Query("UPDATE tasks SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}
