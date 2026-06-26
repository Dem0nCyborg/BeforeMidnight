package com.chandan.beforemidnight.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chandan.beforemidnight.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos WHERE createdAtEpochDay = :epochDay ORDER BY id ASC")
    fun getTasksForDay(epochDay: Long): Flow<List<TodoEntity>>

    // Reserved for the future read-only Expired view; never shown on the Today screen.
    @Query("SELECT * FROM todos WHERE createdAtEpochDay < :epochDay ORDER BY createdAtEpochDay DESC, id ASC")
    fun getTasksBeforeDay(epochDay: Long): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)
}
