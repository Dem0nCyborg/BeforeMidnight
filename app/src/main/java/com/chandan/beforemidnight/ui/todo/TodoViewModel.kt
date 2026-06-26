package com.chandan.beforemidnight.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chandan.beforemidnight.di.AppContainer
import com.chandan.beforemidnight.domain.model.Todo
import com.chandan.beforemidnight.domain.repository.TodoRepository
import com.chandan.beforemidnight.domain.usecase.AddTaskUseCase
import com.chandan.beforemidnight.util.DateProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class TodoViewModel(
    private val repository: TodoRepository,
    private val addTaskUseCase: AddTaskUseCase,
    private val dateProvider: DateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoUiState(currentDate = dateProvider.today()))
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    private var taskObserverJob: Job? = null

    private val _taskAdded = Channel<Unit>(Channel.BUFFERED)

    val taskAdded = _taskAdded.receiveAsFlow()

    init {
        observeTodayTasks()
        scheduleMidnightReset()
    }

    // Called by the UI on every ON_RESUME. Refreshes the task list if the date has
    // rolled over while the app was in the background, without the ViewModel ever
    // holding a Lifecycle reference.
    fun onResume() {
        if (dateProvider.today() != _uiState.value.currentDate) {
            observeTodayTasks()
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text, isInputError = false) }
    }

    fun onAddTask() {
        if (_uiState.value.isLoading) return
        val title = _uiState.value.inputText
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                addTaskUseCase(title)
                _uiState.update { it.copy(inputText = "", isInputError = false) }
                _taskAdded.send(Unit)
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(isInputError = true) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onToggle(todo: Todo) {
        viewModelScope.launch {
            repository.toggleCompletion(todo)
        }
    }

    private fun observeTodayTasks() {
        taskObserverJob?.cancel()
        taskObserverJob = viewModelScope.launch {
            repository.getTasksForDay(dateProvider.today())
                .collect { tasks ->
                    _uiState.update { it.copy(tasks = tasks, currentDate = dateProvider.today()) }
                }
        }
    }

    private fun scheduleMidnightReset() {
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                delay(ChronoUnit.MILLIS.between(now, nextMidnight))
                observeTodayTasks()
            }
        }
    }
}

class TodoViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TodoViewModel(
            repository = container.repository,
            addTaskUseCase = container.addTaskUseCase,
            dateProvider = container.dateProvider,
        ) as T
}
