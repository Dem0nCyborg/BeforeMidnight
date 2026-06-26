package com.chandan.beforemidnight.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chandan.beforemidnight.data.local.entity.TodoEntity

@Database(
    entities = [TodoEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}
