package com.chandan.beforemidnight.ui.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.chandan.beforemidnight.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    uiState: TodoUiState,
    taskAdded: Flow<Unit>,
    taskUpdated: Flow<Unit>,
    onInputChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onToggle: (Todo) -> Unit,
    onUpdateTask: (Todo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingTask: Todo? = editingTaskId?.let { id -> uiState.tasks.find { it.id == id } }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }

    LaunchedEffect(Unit) {
        taskAdded.collect { showAddSheet = false }
    }

    LaunchedEffect(Unit) {
        taskUpdated.collect { editingTaskId = null }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showHistory) "History" else uiState.currentDate.format(dateFormatter),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(
                            imageVector = if (showHistory) Icons.Default.Close else Icons.Default.DateRange,
                            contentDescription = if (showHistory) "Back to today" else "View history",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!showHistory) {
                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add task")
                }
            }
        },
    ) { innerPadding ->
        if (showHistory) {
            HistoryContent(
                expiredTasks = uiState.expiredTasks,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (uiState.tasks.isEmpty()) {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(uiState.tasks, key = { it.id }) { todo ->
                            TodoItem(
                                todo = todo,
                                nowMillis = uiState.nowMillis,
                                onToggle = { onToggle(todo) },
                                onEdit = { editingTaskId = todo.id },
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddSheet = false
                onInputChange("")
            },
            sheetState = addSheetState,
        ) {
            AddTaskSheetContent(
                inputText = uiState.inputText,
                isInputError = uiState.isInputError,
                isLoading = uiState.isLoading,
                onInputChange = onInputChange,
                onAddTask = onAddTask,
            )
        }
    }

    if (editingTask != null) {
        ModalBottomSheet(
            onDismissRequest = { editingTaskId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            EditTaskSheetContent(
                todo = editingTask,
                currentDate = uiState.currentDate,
                onUpdateTask = onUpdateTask,
            )
        }
    }
}

// ----- Today view -----

@Composable
private fun TodoItem(
    todo: Todo,
    nowMillis: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isExpired = todo.expiresAt != null && todo.expiresAt <= nowMillis
    val targetTextColor = when {
        isExpired -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        todo.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        label = "todoItemTextColor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = if (isExpired) null else { _ ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = todo.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
            color = textColor,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "No tasks for today",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Tap + to add your first task",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ----- History view -----

@Composable
private fun HistoryContent(
    expiredTasks: List<Todo>,
    modifier: Modifier = Modifier,
) {
    if (expiredTasks.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "No past tasks yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Completed days will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        // groupBy preserves insertion order (LinkedHashMap); DAO orders DESC by date so
        // most-recent past day is at the top of the logbook.
        val grouped = remember(expiredTasks) { expiredTasks.groupBy { it.createdDate } }
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            grouped.forEach { (date, tasks) ->
                stickyHeader(key = "header_$date") {
                    DateSectionHeader(date = date)
                }
                items(tasks, key = { it.id }) { todo ->
                    HistoryItem(todo = todo)
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun DateSectionHeader(date: LocalDate) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }
    // Surface gives the header an opaque background so scrolling items don't bleed through.
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = date.format(formatter),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun HistoryItem(todo: Todo) {
    val expiryLabel = remember(todo.expiresAt) {
        todo.expiresAt?.let { millis ->
            val time = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            "Expired ${time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = todo.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
            color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
        )
        if (expiryLabel != null) {
            Text(
                text = expiryLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ----- Add sheet -----

@Composable
private fun AddTaskSheetContent(
    inputText: String,
    isInputError: Boolean,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "New task",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What needs to be done?") },
            isError = isInputError,
            supportingText = {
                Text(
                    text = "${inputText.length}/200",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onAddTask() }),
        )
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAddTask()
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add task")
        }
    }
}

// ----- Edit sheet -----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskSheetContent(
    todo: Todo,
    currentDate: LocalDate,
    onUpdateTask: (Todo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var editTitle by remember(todo.id) { mutableStateOf(todo.title) }
    var hasExpiration by remember(todo.id) { mutableStateOf(todo.expiresAt != null) }

    val (initHour, initMinute) = remember(todo.id) {
        todo.expiresAt?.let { millis ->
            val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
            t.hour to t.minute
        } ?: (23 to 59)
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initHour,
        initialMinute = initMinute,
    )

    val isSaveEnabled = editTitle.trim().isNotBlank() && editTitle.length <= 200

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Edit task",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = editTitle,
            onValueChange = { editTitle = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Task title") },
            supportingText = {
                Text(
                    text = "${editTitle.length}/200",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Set expiration",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = hasExpiration,
                onCheckedChange = { hasExpiration = it },
            )
        }
        AnimatedVisibility(visible = hasExpiration) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(4.dp))
                TimePicker(state = timePickerState)
            }
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val expiresAt = if (hasExpiration) {
                    currentDate
                        .atTime(timePickerState.hour, timePickerState.minute)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } else {
                    null
                }
                onUpdateTask(todo.copy(title = editTitle.trim(), expiresAt = expiresAt))
            },
            enabled = isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}
