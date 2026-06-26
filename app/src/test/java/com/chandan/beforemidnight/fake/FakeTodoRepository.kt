package com.chandan.beforemidnight.fake

import com.chandan.beforemidnight.domain.model.Todo
import com.chandan.beforemidnight.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeTodoRepository : TodoRepository {

    private val tasks = mutableListOf<Todo>()
    private val _flow = MutableStateFlow<List<Todo>>(emptyList())

    override fun getTasksForDay(date: LocalDate): Flow<List<Todo>> =
        _flow.map { all -> all.filter { it.createdDate == date } }

    override fun getTasksBeforeDay(date: LocalDate): Flow<List<Todo>> =
        _flow.map { all -> all.filter { it.createdDate < date } }

    override suspend fun addTask(title: String, date: LocalDate): Long {
        val id = (tasks.size + 1).toLong()
        tasks.add(Todo(id = id, title = title, isCompleted = false, createdDate = date, expiresAt = null))
        _flow.value = tasks.toList()
        return id
    }

    // Mirrors the real impl: !todo.isCompleted (the argument's value), not the stored value.
    // This ensures the fake and the real impl agree on what "toggle" means.
    override suspend fun toggleCompletion(todo: Todo) {
        val idx = tasks.indexOfFirst { it.id == todo.id }
        if (idx != -1) {
            tasks[idx] = tasks[idx].copy(isCompleted = !todo.isCompleted)
            _flow.value = tasks.toList()
        }
    }

    fun allTasks(): List<Todo> = tasks.toList()
}
