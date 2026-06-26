package com.chandan.beforemidnight.ui.todo

import com.chandan.beforemidnight.domain.model.Todo
import java.time.LocalDate

data class TodoUiState(
    val tasks: List<Todo> = emptyList(),
    val inputText: String = "",
    val isInputError: Boolean = false,
    val isLoading: Boolean = false,
    val currentDate: LocalDate = LocalDate.now(),
)
