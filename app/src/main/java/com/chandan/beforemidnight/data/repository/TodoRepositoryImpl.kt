package com.chandan.beforemidnight.data.repository

import com.chandan.beforemidnight.data.local.TodoDao
import com.chandan.beforemidnight.data.local.entity.TodoEntity
import com.chandan.beforemidnight.domain.model.Todo
import com.chandan.beforemidnight.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TodoRepositoryImpl(private val dao: TodoDao) : TodoRepository {

    override fun getTasksForDay(date: LocalDate): Flow<List<Todo>> =
        dao.getTasksForDay(date.toEpochDay())
            .map { entities -> entities.map { it.toDomain() } }

    override fun getTasksBeforeDay(date: LocalDate): Flow<List<Todo>> =
        dao.getTasksBeforeDay(date.toEpochDay())
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun addTask(title: String, date: LocalDate): Long =
        dao.insert(TodoEntity(title = title, createdAtEpochDay = date.toEpochDay()))

    override suspend fun toggleCompletion(todo: Todo) =
        dao.update(todo.toEntity().copy(isCompleted = !todo.isCompleted))

    override suspend fun updateTask(todo: Todo) =
        dao.update(todo.toEntity())
}

private fun TodoEntity.toDomain(): Todo = Todo(
    id = id,
    title = title,
    isCompleted = isCompleted,
    createdDate = LocalDate.ofEpochDay(createdAtEpochDay),
    expiresAt = expiresAt,
)

private fun Todo.toEntity(): TodoEntity = TodoEntity(
    id = id,
    title = title,
    isCompleted = isCompleted,
    createdAtEpochDay = createdDate.toEpochDay(),
    expiresAt = expiresAt,
)
