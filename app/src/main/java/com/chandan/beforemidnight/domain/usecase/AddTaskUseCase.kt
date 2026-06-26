package com.chandan.beforemidnight.domain.usecase

import com.chandan.beforemidnight.domain.repository.TodoRepository
import com.chandan.beforemidnight.util.DateProvider

class AddTaskUseCase(
    private val repository: TodoRepository,
    private val dateProvider: DateProvider,
) {
    suspend operator fun invoke(title: String) {
        val trimmed = title.trim()
        require(trimmed.isNotBlank()) { "Title must not be blank" }
        require(trimmed.length <= 200) { "Title too long" }
        repository.addTask(trimmed, dateProvider.today())
    }
}
