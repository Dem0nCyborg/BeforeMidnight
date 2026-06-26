package com.chandan.beforemidnight.domain.repository

import com.chandan.beforemidnight.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TodoRepository {
    fun getTasksForDay(date: LocalDate): Flow<List<Todo>>
    fun getTasksBeforeDay(date: LocalDate): Flow<List<Todo>>
    suspend fun addTask(title: String, date: LocalDate): Long
    suspend fun toggleCompletion(todo: Todo)
    suspend fun updateTask(todo: Todo)
}
