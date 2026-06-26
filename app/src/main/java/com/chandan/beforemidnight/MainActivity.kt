package com.chandan.beforemidnight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chandan.beforemidnight.ui.theme.BeforeMidnightTheme
import com.chandan.beforemidnight.ui.todo.TodoScreen
import com.chandan.beforemidnight.ui.todo.TodoViewModel
import com.chandan.beforemidnight.ui.todo.TodoViewModelFactory
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BeforeMidnightApp

        setContent {
            BeforeMidnightTheme {
                val vm: TodoViewModel = viewModel(factory = TodoViewModelFactory(app.container))
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                // Fires every time the app returns to the foreground. If the calendar
                // date has rolled over while the app was backgrounded, onResume()
                // cancels the old Room query and opens a fresh one for the new day.
                LifecycleResumeEffect(Unit) {
                    vm.onResume()
                    onPauseOrDispose { }
                }

                TodoScreen(
                    uiState = uiState,
                    taskAdded = vm.taskAdded,
                    taskUpdated = vm.taskUpdated,
                    onInputChange = vm::onInputChange,
                    onAddTask = vm::onAddTask,
                    onToggle = vm::onToggle,
                    onUpdateTask = vm::onUpdateTask,
                )
            }
        }
    }
}
